package com.jadyn.ai.medialearn.decode

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Environment
import android.util.Log
import com.jadyn.ai.medialearn.codec.debugShowSupportColorFormat
import com.jadyn.ai.medialearn.codec.isSupportColorFormat
import com.jadyn.ai.medialearn.utils.disposeOutput
import com.jadyn.ai.medialearn.utils.height
import com.jadyn.ai.medialearn.utils.mime
import com.jadyn.ai.medialearn.utils.width
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.File

/**
 *@version:
 *@FileDescription: 视频解码类
 *@Author:jing
 *@Since:2019/1/28
 *@ChangeList:
 */
class VideoDecoder private constructor(file: File, private val isSurface: Boolean = false) {

    private val TAG = this.javaClass.name

    private val videoAnalyze by lazy {
        VideoAnalyze(file.toString())
    }

    private val mediaFormat by lazy {
        videoAnalyze.mediaFormat
    }

    private var outputSurface: OutputSurface? = null

    private var decoder: MediaCodec

    private val defDecoderColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible

    private val DEF_TIME_OUT = 10000L

    private var outputDirectory: String = Environment.getExternalStorageDirectory().path + "/"

    init {
        val mime = mediaFormat.mime

        decoder = MediaCodec.createDecoderByType(mime)
        debugShowSupportColorFormat(decoder.codecInfo.getCapabilitiesForType(mime))
    }

    //-------decode----------
    private var decoderDisposable: Disposable? = null

    private var isStart = false

    var decoding: (Int) -> Unit = {}

    fun start(success: () -> Unit = {}, failed: () -> Unit = {}) {
        if (decoderDisposable != null) {
            Log.d(TAG, "decoder already started ")
            return
        }
        decoderDisposable = Observable.fromCallable {
            decodeToFrames()
        }.subscribeOn(Schedulers.io()).subscribe({
            decoderDisposable?.dispose()
            success.invoke()
        }, {
            Log.d(TAG, "decode failed :${it.message} ")
            decoderDisposable?.dispose()
            failed.invoke()
        })
    }

    fun stop() {
        isStart = false
    }

    fun release() {
        stop()
        decoderDisposable?.dispose()
        outputSurface?.release()
    }

    private fun decodeToFrames() {
        outputSurface = if (isSurface) OutputSurface(mediaFormat.width, mediaFormat.height) else null

        // 指定帧格式COLOR_FormatYUV420Flexible,几乎所有的解码器都支持
        if (decoder.codecInfo.getCapabilitiesForType(mediaFormat.mime).isSupportColorFormat(defDecoderColorFormat)) {
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, defDecoderColorFormat)
            decoder.configure(mediaFormat, outputSurface?.surface, null, 0)
        } else {
            throw RuntimeException("this mobile not support YUV 420 Color Format")
            TODO("soft decode")
        }

        val startTime = System.currentTimeMillis()
        Log.d(TAG, "start decode frames")
        isStart = true
        val bufferInfo = MediaCodec.BufferInfo()
        // 是否输入完毕
        var inputEnd = false
        // 是否输出完毕
        var outputEnd = false
        decoder.start()
        var outputFrameCount = 0

        while (!outputEnd && isStart) {
            if (!inputEnd) {
                val inputBufferId = decoder.dequeueInputBuffer(DEF_TIME_OUT)
                if (inputBufferId >= 0) {
                    // 获得一个可写的输入缓存对象
                    val inputBuffer = decoder.getInputBuffer(inputBufferId)
                    // 使用MediaExtractor读取数据
                    val sampleSize = videoAnalyze.mediaExtractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        // 2019/2/8-19:15 没有数据
                        decoder.queueInputBuffer(inputBufferId, 0, 0, 0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputEnd = true
                    } else {
                        // 将数据压入到输入队列
                        val presentationTimeUs = videoAnalyze.mediaExtractor.sampleTime
                        decoder.queueInputBuffer(inputBufferId, 0,
                                sampleSize, presentationTimeUs, 0)
                        videoAnalyze.mediaExtractor.advance()
                    }
                }
            }

            // 2019/2/9-22:20 获取可用的输出缓存队列
            decoder.disposeOutput(bufferInfo, DEF_TIME_OUT, {
                outputEnd = true
            }) {
                outputFrameCount++

                if (outputSurface != null) {
                    val doRender = bufferInfo.size != 0
                    // CodeC搭配输出Surface时，调用此方法将数据及时渲染到Surface上
                    decoder.releaseOutputBuffer(it, doRender)
                    if (doRender) {
                        // 2019/2/14-15:24 必须和surface创建时保持统一线程
                        outputSurface!!.awaitNewImage()
                        outputSurface!!.drawImage(true)

                        val file = File(outputDirectory, String.format("frame-%02d.jpg", outputFrameCount))
                        decoding.invoke(outputFrameCount)
                        outputSurface!!.saveFrame(file.toString())
                    }
                } else {
                    if (bufferInfo.size != 0) {
                        // YUV输出JPEG。使用Image时，先拿到image数据再releaseOutputBuffer
                        val image = decoder.getOutputImage(it)
                        if (outputFrameCount <= 1) {
                            Log.d(TAG, "output Image format ${image.format}: ")
                        }
                        val fileName = DecoderFormat.JPG.outputFrameFileName(outputDirectory,
                                outputFrameCount)
                        DecoderFormat.JPG.compressCorrespondingFile(fileName, image)
                        decoding.invoke(outputFrameCount)
                        image.close()
                    }
                    decoder.releaseOutputBuffer(it, true)
                }
            }
        }
        Log.d(TAG, "decode frames end ${(System.currentTimeMillis() - startTime) / 1000}")
    }

    //------build----------
    class DecoderBuilder {

        private lateinit var file: File

        private var dir = ""

        private var surfaceOutput = false

        fun makeFile(path: String?): DecoderBuilder {
            if (path.isNullOrBlank()) {
                throw IllegalArgumentException("file path is wrong!!!!!")
            }
            file = File(path)
            if (!file.isFile) {
                throw IllegalArgumentException("this path is not file")
            }
            return this
        }

        fun makeFile(f: File?): DecoderBuilder {
            if (f == null || f.exists()) {
                throw IllegalArgumentException("file path is wrong!!!!!")
            }
            this.file = f
            return this
        }

        fun saveDirectory(dir: String): DecoderBuilder {
            val d = File(dir)
            if (!d.exists()) {
                d.mkdirs()
            }
            if (!d.isDirectory) {
                throw IllegalArgumentException("dir must directory")
            }
            this.dir = d.absolutePath + "/"
            return this
        }

        /*
        * 使用surface作为输出
        * */
        fun setUseSurfaceOutput(use: Boolean): DecoderBuilder {
            surfaceOutput = use
            return this
        }

        fun build(): VideoDecoder {
            return VideoDecoder(file, surfaceOutput).apply {
                outputDirectory = this@DecoderBuilder.dir
            }
        }
    }
}