package com.jadyn.ai.medialearn.decode

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Environment
import android.util.Log
import android.util.Size
import com.jadyn.ai.medialearn.codec.debugShowSupportColorFormat
import com.jadyn.ai.medialearn.codec.isSupportColorFormat
import com.jadyn.ai.medialearn.codec.selectVideoTrack
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
class VideoDecoder private constructor(file: File) {

    private val TAG = this.javaClass.name

    private val extractor by lazy {
        MediaExtractor()
    }

    private var decoder: MediaCodec

    private val defDecoderColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible

    private val DEF_TIME_OUT = 10000L

    private var size: Size

    var outputFormat = DecoderFormat.JPG

    private var outputDirectory: String = Environment.getExternalStorageDirectory().path + "/"

    init {
        extractor.setDataSource(file.toString())
        val trackIndex = extractor.selectVideoTrack()
        if (trackIndex < 0) {
            throw RuntimeException("can not find video track in $file")
        }
        extractor.selectTrack(trackIndex)
        val mediaFormat = extractor.getTrackFormat(trackIndex)
        val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
        // 2019/2/8-16:21 视频宽高
        size = Size(mediaFormat.getInteger(MediaFormat.KEY_WIDTH),
                mediaFormat.getInteger(MediaFormat.KEY_HEIGHT))
        decoder = MediaCodec.createDecoderByType(mime)
        debugShowSupportColorFormat(decoder.codecInfo.getCapabilitiesForType(mime))

        //---decoder--

        // 指定帧格式COLOR_FormatYUV420Flexible,几乎所有的解码器都支持
        if (decoder.codecInfo.getCapabilitiesForType(mime).isSupportColorFormat(defDecoderColorFormat)) {
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, defDecoderColorFormat)
            decoder.configure(mediaFormat, null, null, 0)
        } else {
            throw RuntimeException("this mobile not support YUV 420 Color Format")
            TODO("soft decode ffmepeg")
        }
    }

    private var decoderDisposable: Disposable? = null

    private var isStart = false

    fun start() {
        if (decoderDisposable != null) {
            Log.d(TAG, "decoder already started ")
            return
        }
        decoderDisposable = Observable.fromCallable {
            decodeToFrames()
        }.subscribeOn(Schedulers.io()).subscribe {
            decoderDisposable?.dispose()
        }
    }

    fun stop() {
        isStart = false
    }

    fun release() {
        stop()
        decoderDisposable?.dispose()
    }

    private fun decodeToFrames() {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "start decode frames")
        isStart = true
        val bufferInfo = MediaCodec.BufferInfo()
        var sawInputEOS = false
        var sawOutputEOS = false
        decoder.start()
        var outputFrameCount = 0

        while (!sawOutputEOS && isStart) {
            if (!sawInputEOS) {
                val inputBufferId = decoder.dequeueInputBuffer(DEF_TIME_OUT)
                if (inputBufferId >= 0) {
                    // 2019/2/9-21:38 获得可使用缓冲区位置索引
                    val inputBuffer = decoder.getInputBuffer(inputBufferId)
                    // 2019/2/8-22:14 检索当前编码的样本，并存储到inputBuffer
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        // 2019/2/8-19:15 没有数据
                        decoder.queueInputBuffer(inputBufferId, 0, 0, 0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        sawInputEOS = true
                    } else {
                        // 2019/2/9-21:46 将数据压入到输入队列
                        val presentationTimeUs = extractor.sampleTime
                        decoder.queueInputBuffer(inputBufferId, 0,
                                sampleSize, presentationTimeUs, 0)
                        extractor.advance()
                    }
                }
            }

            // 2019/2/9-22:20 获取可用的输出缓存队列
            val outputBufferId = decoder.dequeueOutputBuffer(bufferInfo, DEF_TIME_OUT)
            if (outputBufferId >= 0) {
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    sawOutputEOS = true
                }
                val doRender = bufferInfo.size != 0
                if (doRender) {
                    outputFrameCount++
                    val image = decoder.getOutputImage(outputBufferId)
                    if (outputFrameCount <= 3) {
                        Log.d(TAG, "output Image format ${image.format}: ")
                    }
                    
                    val fileName = outputFormat.outputFrameFileName(outputDirectory, outputFrameCount)
                    outputFormat.compressCorrespondingFile(fileName, image)

                    image.close()
                    decoder.releaseOutputBuffer(outputBufferId, true)
                }
            }
        }
        Log.d(TAG, "decode frames end ${(System.currentTimeMillis() - startTime) / 1000}")
    }

    //------build----------
    class DecoderBuilder {

        private lateinit var file: File

        private var dir = ""

        fun makeFile(path: String?): DecoderBuilder {
            if (path.isNullOrBlank()) {
                throw IllegalArgumentException("file path is wrong!!!!!")
            }
            file = File(path)
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

        fun build(): VideoDecoder {
            return VideoDecoder(file).apply {
                outputDirectory = this@DecoderBuilder.dir
            }
        }
    }
}