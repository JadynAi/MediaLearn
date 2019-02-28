package com.jadyn.mediakit.video.decode

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import com.jadyn.mediakit.function.*
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*

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
    private val decoderScheduler by lazy {
        Schedulers.io()
    }
    private val frameCache by lazy {
        FrameCache(dataSource)
    }

    private val queueTask by lazy {
        Collections.synchronizedList(arrayListOf<DecodeFrame>())
    }

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
     * 得到指定时间的帧，异步回调
     *
     * @param second 秒
     *
     * @param success 成功回调
     *
     * @param failed 失败回调
     */
    fun getFrame(second: Float, success: (Bitmap) -> Unit, failed: (Throwable) -> Unit) {
        val target = videoAnalyze.getValidSampleTime(mediaFormat.getSafeTimeUS(second))
        frameCache.asyncGetTarget(target, success, {
            // 如果此时任务栈里正在取这一帧，就不作任何处理
            val isSameFrame = queueTask.isNotEmpty() && queueTask[0].sampleTime == target
            if (!isSameFrame) {
                Log.d(TAG, "getFrame second $second sampleTime ${videoAnalyze.getValidSampleTime(mediaFormat.getSafeTimeUS(second))}: ")
                DecodeFrame(second, success, failed).execute()
            }
        })
    }

    fun release() {
        videoAnalyze.release()
        if (queueTask.isNotEmpty()) {
            queueTask.forEach {
                it.release()
            }
        }
        decodeCore.release()
        decoder.release()
    }

    inner class DecodeFrame(target: Float,
                            private val success: (Bitmap) -> Unit,
                            private val failed: (Throwable) -> Unit) {

        val sampleTime: Long = videoAnalyze.getValidSampleTime(when {
            target < 0L -> 0L
            target * 1000000 > mediaFormat.duration -> mediaFormat.duration
            else -> (target * 1000000).toLong()
        })
        private val core: Observable<Bitmap>
        private var flint: Disposable? = null

        init {
            core = Observable.create<Bitmap> { emitter ->
                if (!isStart) {
                    decoder.configure(mediaFormat, decodeCore.fkOutputSurface(mediaFormat.width, mediaFormat.height),
                            null, 0)
                    decoder.start()
                    isStart = true
                }
                val info = MediaCodec.BufferInfo()
                //处理目标时间帧
                handleFrame(sampleTime, info, emitter)
                emitter.onComplete()
            }.subscribeOn(decoderScheduler)
                    .doOnComplete {
                        schedulerNext()
                    }
                    .observeOn(AndroidSchedulers.mainThread())
        }

        private fun handleFrame(time: Long, info: MediaCodec.BufferInfo, emitter: ObservableEmitter<Bitmap>? = null) {
            var outputDone = false
            var inputDone = false
            videoAnalyze.mediaExtractor.seekTo(time, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
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
                            Log.d(TAG, "${if (emitter != null) "main time" else "fuck time"} dequeue time is $presentationTimeUs ")
                            decoder.queueInputBuffer(inputBufferId, 0,
                                    sampleSize, presentationTimeUs, 0)
                            videoAnalyze.mediaExtractor.advance()
                        }
                    }
                }

                decoder.disposeOutput(info, DEF_TIME_OUT, {
                    outputDone = true
                }, { id ->
                    Log.d(TAG, "out time ${info.presentationTimeUs} ")
                    if (decodeCore.updateTexture(info, id, decoder)) {
                        if (info.presentationTimeUs == time) {
                            outputDone = true
                            val bitmap = decodeCore.generateFrame()
                            emitter?.onNext(bitmap)
                            frameCache.cacheFrame(time, bitmap)
                        }
                    }
                })
            }
            decoder.flush()
        }

        private fun endCore() {
            if (queueTask.size > 1) {
                //队列还有任务.抛出异常用来终止上游继续运行
                throw EndSignal()
            }
        }

        fun execute() {
            if (queueTask.isEmpty()) {
                run()
                queueTask.add(this)
            } else {
                if (queueTask.contains(this)) {
                    //提升优先级,此时位于0index的不可能为本身，所以和1交换
                    Collections.swap(queueTask, queueTask.indexOf(this), 1)
                } else {
                    // 入列
                    queueTask.add(this)
                }
            }
        }

        fun release() {
            flint?.apply {
                if (!isDisposed) {
                    endCore()
                }
            }
        }

        private fun run() {
            if (flint != null) {
                return
            }
            Log.d(TAG, "run sample $sampleTime ")
            flint = core.subscribe({
                success.invoke(it)
            }, {
                if (it is EndSignal) {
                    schedulerNext()
                } else {
                    failed.invoke(it)
                }
            })
        }

        /*
        * 火石熄灭，执行队列下一个任务
        * */
        private fun schedulerNext() {
            Log.d(TAG, "schedulerNext ")
            decoder.flush()
            flint?.dispose()
            queueTask.remove(this)
            if (queueTask.isNotEmpty()) {
                queueTask[0].run()
            }
        }

        override fun hashCode(): Int {
            return sampleTime.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            other?.apply {
                if (other is DecodeFrame) {
                    return this@DecodeFrame.sampleTime == other.sampleTime
                }
            }
            return super.equals(other)
        }

        //用来停止上游继续运行
        inner class EndSignal : Throwable()
    }
}