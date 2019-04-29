package com.jadyn.mediakit.video.decode

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import com.jadyn.mediakit.function.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

/**
 *@version:
 *@FileDescription: 解码指定帧
 *@Author:jing
 *@Since:2019/2/18
 *@ChangeList:
 */
class VideoDecoder2(dataSource: String) {

    private val TAG = this.javaClass.name

    private val videoAnalyze by lazy {
        VideoAnalyze(dataSource)
    }
    private val mediaFormat by lazy {
        videoAnalyze.mediaFormat
    }
    private val decodeCore by lazy {
        GLCore()
    }
    private val frameCache by lazy {
        FrameCache(dataSource)
    }

    private val thread by lazy {
        Executors.newSingleThreadScheduledExecutor()
    }
    private val compositeDisposable by lazy {
        CompositeDisposable()
    }

    private var curFrame = 0L

    private val DEF_TIME_OUT = 2000L
    private var decoder: MediaCodec
    private val defDecoderColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
    private var isStart = false

    init {
        val mime = mediaFormat.mime
        decoder = MediaCodec.createDecoderByType(mime)
        debugShowSupportColorFormat(decoder.codecInfo.getCapabilitiesForType(mime))
        // 指定帧格式COLOR_FormatYUV420Flexible,几乎所有的解码器都支持
        if (decoder.codecInfo.getCapabilitiesForType(mediaFormat.mime).isSupportColorFormat(defDecoderColorFormat)) {
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, defDecoderColorFormat)
        } else {
            throw RuntimeException("this mobile not support YUV 420 Color Format")
            TODO("soft decode")
        }
    }

    /**
     * 得到指定时间的帧，秒级别，异步回调
     *
     * @param second 秒
     *
     * @param success 成功回调
     *
     * @param failed 失败回调
     */
    fun getFrame(second: Float, success: (Bitmap) -> Unit, failed: (Throwable) -> Unit,
                 isNeedCache: Boolean = true) {
        val target = videoAnalyze.getValidSampleTime(mediaFormat.getSafeTimeUS(second))
        Log.d(TAG, "getFrame second $second sampleTime $target: ")
        getInternalFrame(target, success, failed, isNeedCache)
    }

    /**
     * 得到指定时间的帧，ms,毫秒级别，异步回调
     *
     * @param second ms
     *
     * @param success 成功回调
     *
     * @param failed 失败回调
     */
    fun getFrame(ms: Long, success: (Bitmap) -> Unit, failed: (Throwable) -> Unit,
                 isNeedCache: Boolean = true) {
        val target = videoAnalyze.getValidSampleTime(mediaFormat.getSafeTimeUS(ms))
        Log.d(TAG, "getFrame ms $ms sampleTime $target: ")
        getInternalFrame(target, success, failed, isNeedCache)
    }

    private fun getInternalFrame(target: Long, success: (Bitmap) -> Unit,
                                 failed: (Throwable) -> Unit, isNeedCache: Boolean = true) {
        fun codecDecode() {
            // 如果此时正在取这一帧，就不作任何处理
            val isSameFrame = curFrame != 0L && curFrame == target
            if (!isSameFrame) {
                curFrame = target
                val decoder2 = Decoder2(target, success, failed, if (isNeedCache) 1 else 0)
                decoder2.disposable?.apply {
                    compositeDisposable.add(this)
                }
                thread.execute(decoder2)
            }
        }
        // 是否需要缓存，不需要的话就直接解码。需要缓存则优先从缓存中读取
        if (isNeedCache) {
            frameCache.asyncGetTarget(target, { time, bitmap ->
                if (target == time) {
                    success.invoke(bitmap)
                }
            }, {
                codecDecode()
            })
        } else {
            codecDecode()
        }

    }

    private fun prepareCodeC() {
        if (!isStart) {
            decoder.configure(mediaFormat, decodeCore.fkOutputSurface(mediaFormat.width, mediaFormat.height),
                    null, 0)
            decoder.start()
            isStart = true
        }
    }

    fun release() {
        videoAnalyze.release()
        decodeCore.release()
        decoder.release()
        frameCache.release()
        thread.shutdown()
        compositeDisposable.clear()
    }

    /**
     * @param extraNum 额外解码帧数量
     * */
    inner class Decoder2(val target: Long, success: (Bitmap) -> Unit,
                         failed: (Throwable) -> Unit, private val extraNum: Int = 1) : Runnable {

        var disposable: Disposable? = null
            private set

        private val handler = PublishSubject.create<Bitmap>()

        init {
            disposable = handler.observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        success.invoke(it)
                    }, {
                        failed.invoke(it)
                    }, {
                        disposable?.dispose()
                    })
        }

        override fun run() {
            prepareCodeC()
            Log.d(TAG, "decoder2 thread is ${Thread.currentThread().name}")
            val c = measureTimeMillis {
                val info = MediaCodec.BufferInfo()
                //处理目标时间帧
                try {
                    handleFrame(info)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.d(TAG, "decoder2 frame failed : $e")
                    handler.onError(Throwable(e))
                    handler.onComplete()
                }
            }
            Log.d(TAG, "decoder2 frame time ms is : $c")
        }

        private fun handleFrame(info: MediaCodec.BufferInfo) {
            var outputDone = false
            var inputDone = false
            videoAnalyze.mediaExtractor.seekTo(target, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            var curExtraNum = -1
            while (!outputDone) {
                if (!inputDone) {
                    decoder.dequeueValidInputBuffer(DEF_TIME_OUT) { inputBufferId, inputBuffer ->
                        val sampleSize = videoAnalyze.mediaExtractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inputBufferId, 0, 0, 0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            // 将数据压入到输入队列
                            val presentationTimeUs = videoAnalyze.mediaExtractor.sampleTime
                            decoder.queueInputBuffer(inputBufferId, 0,
                                    sampleSize, presentationTimeUs, 0)
                            videoAnalyze.mediaExtractor.advance()
                        }
                    }
                }

                decoder.disposeOutput(info, DEF_TIME_OUT, {
                    outputDone = true
                }) { id ->
                    Log.d(TAG, "out time ${info.presentationTimeUs} ")
                    if (decodeCore.updateTexture(info, id, decoder)) {
                        if (info.presentationTimeUs == target) {
                            // 遇到目标时间帧，才生产Bitmap
                            curExtraNum = 0
                        }
                        if (curExtraNum >= 0) {
                            outputDone = curExtraNum == extraNum
                            curExtraNum++
                            val bitmap = decodeCore.generateFrame()
                            if (curFrame == target) {
                                handler.onNext(bitmap)
                            }
                            frameCache.cacheFrame(info.presentationTimeUs, bitmap)
                            Log.d(TAG, "target cache $target time is ${info.presentationTimeUs}")
                        }
                    }
                }
            }
        }
    }
}