package com.jadyn.mediakit.video.decode

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Environment
import android.util.Log
import com.jadyn.mediakit.function.*
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
class VideoDecoder private constructor(file: File, private val decodeCore: DecodeCore = YuvCore()) {

    private val TAG = this.javaClass.name

    private val videoAnalyze by lazy {
        VideoAnalyze(file.toString())
    }

    private val mediaFormat by lazy {
        videoAnalyze.mediaFormat
    }

    private val DEF_TIME_OUT = 10000L
    private var decoder: MediaCodec
    private val defDecoderColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
    
    private var outputDirectory: String = Environment.getExternalStorageDirectory().path + "/"
        private set(value) {
            decodeCore.configure(value)
            field = value
        }

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
        videoAnalyze.release()
        decodeCore.release()
    }

    private fun decodeToFrames() {
        // 指定帧格式COLOR_FormatYUV420Flexible,几乎所有的解码器都支持
        if (decoder.codecInfo.getCapabilitiesForType(mediaFormat.mime).isSupportColorFormat(defDecoderColorFormat)) {
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, defDecoderColorFormat)
            decoder.configure(mediaFormat, decodeCore.fkOutputSurface(mediaFormat.width, mediaFormat.height),
                    null, 0)
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
                // 获得可用输入队列，并填充数据
                decoder.dequeueValidInputBuffer(DEF_TIME_OUT) { inputBufferId, inputBuffer ->
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
                //视频帧编码为图片
                val index = decodeCore.codeToFrame(bufferInfo, it, outputFrameCount, decoder)
                if (index > 0) {
                    decoding.invoke(index)
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

        private var decoderCore: DecodeCore = YuvCore()

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
            decoderCore = if (use) GLCore() else YuvCore()
            return this
        }

        fun build(): VideoDecoder {
            return VideoDecoder(file, decoderCore).apply {
                outputDirectory = this@DecoderBuilder.dir
            }
        }
    }
}