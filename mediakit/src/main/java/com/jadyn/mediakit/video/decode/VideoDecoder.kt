package com.jadyn.mediakit.video.decode

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import com.jadyn.ai.kotlind.utils.getReal
import com.jadyn.mediakit.function.*
import java.io.File
import java.util.concurrent.Executors

/**
 *@version:
 *@FileDescription: 视频解码类
 *@Author:jing
 *@Since:2019/1/28
 *@ChangeList:
 */
class VideoDecoder(private val dataSource: String?,
                   private val outputFiles: String) {
    private val executors by lazy {
        Executors.newCachedThreadPool()
    }

    fun start(success: () -> Unit, failed: (Exception) -> Unit,
              process: (index: Int) -> Unit) {
        executors.execute(VideoDecoderRunnable(arrayListOf(), dataSource, observer = {
            if (it is Exception) {
                failed.invoke(it)
            } else {
                success.invoke()
            }
        }) { index, bmp, videoInfo ->
            process.invoke(index)
            val file = File(outputFiles, String.format("frame-%02d.jpg", index))
            bmp.invoke().saveFrame(file.toString())
        })
    }

    fun release() {
        executors.shutdown()
    }
}

/**
 * @param dataSource :the raw video path
 * @param observer : the result callback, return Excepetion is failed, return other is Success
 * @param callBack : decode video frame callbacks in call thread
 * */
class VideoDecoderRunnable(
        private val isRunning: ArrayList<Any>,
        private val dataSource: String?,
        private var maxDecodeNum: Int = -1,
        private val observer: (Any) -> Unit = {},
        private val callBack: (index: Int, bmp: () -> Bitmap, videoInfo: MediaFormat) -> Unit) : Runnable {
    private val TAG = this.javaClass.name


    override fun run() {
        val decodeCore = GLCore()
        try {
            isRunning.add(1)
            val startTime = System.currentTimeMillis()
            val DEF_TIME_OUT = 1000L
            val defDecoderColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
            val videoAnalyze = VideoAnalyze(dataSource.getReal())

            val mediaFormat = videoAnalyze.mediaFormat
            val decoder = MediaCodec.createDecoderByType(mediaFormat.mime)


            val width = mediaFormat.width
            val height = mediaFormat.height
            // 指定帧格式COLOR_FormatYUV420Flexible,几乎所有的解码器都支持
            if (decoder.codecInfo.getCapabilitiesForType(mediaFormat.mime).colorFormats.contains(defDecoderColorFormat)) {
                mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, defDecoderColorFormat)
                decoder.configure(mediaFormat, decodeCore.fkOutputSurface(width, height),
                        null, 0)
            } else {
                throw RuntimeException("this mobile not support YUV 420 Color Format")
            }
            var outputDone = false
            val bufferInfo = MediaCodec.BufferInfo()
            // 是否输入完毕
            var inputEnd = false
            // 是否输出完毕
            var outputFrameCount = 0
            var advanceCount = 0

            decoder.start()

            fun genBitmap(): Bitmap {
                return decodeCore.generateFrame()
            }

            while (!outputDone) {
                if (isRunning.isEmpty()) {
                    outputDone = true
                }
                if (!inputEnd) {
                    // 获得可用输入队列，并填充数据
                    val (inputBufferId, inputBuffer) = decoder.dequeueValidInputBuffer(DEF_TIME_OUT)
                    inputBuffer?.apply {
                        // 使用MediaExtractor读取数据
                        val sampleSize = videoAnalyze.mediaExtractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            // 2019/2/8-19:15 没有数据
                            decoder.queueInputBuffer(inputBufferId, 0, 0, 0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEnd = true
                            Log.d(TAG, "queueInputBuffer end stream :$advanceCount  time is ${System.currentTimeMillis()}")
                        } else {
                            // 将数据压入到输入队列
                            val presentationTimeUs = videoAnalyze.mediaExtractor.sampleTime
                            Log.d("TimeTest", "input time $presentationTimeUs")
                            decoder.queueInputBuffer(inputBufferId, 0,
                                    sampleSize, presentationTimeUs, 0)
                            videoAnalyze.mediaExtractor.advance()
                            advanceCount++
                            Log.d(TAG, "queueInputBuffer count :$advanceCount  time is ${System.currentTimeMillis()}")
                        }
                    }
                }
                // 2019/2/9-22:20 获取可用的输出缓存队列
                decoder.disposeOutput(bufferInfo, DEF_TIME_OUT, {
                    outputDone = true
                }) {
                    Log.d("TimeTest", "out info ${bufferInfo.presentationTimeUs}")
                    //视频帧编码为图片
                    val update = decodeCore.updateTexture(bufferInfo, it, decoder)
                    if (update) {
                        outputFrameCount++
                        callBack.invoke(outputFrameCount, ::genBitmap, mediaFormat)
                        //如果达到最大解码数量，就停止解码
                        if (maxDecodeNum >= 0 && outputFrameCount == maxDecodeNum) {
                            outputDone = true
                        }
                    }
                }
            }
            decoder.release()
            decodeCore.release()
            videoAnalyze.release()
            observer.invoke(outputFrameCount)
            Log.d(TAG, "total cost time ${System.currentTimeMillis() - startTime}")
        } catch (e: Exception) {
            decodeCore.release()
            observer.invoke(e)
        }
    }
}