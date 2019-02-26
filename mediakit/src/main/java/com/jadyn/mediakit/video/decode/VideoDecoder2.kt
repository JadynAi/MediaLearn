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
import java.util.concurrent.Executors

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
    private val decoderExecutor by lazy {
        Schedulers.computation()
        Executors.newSingleThreadExecutor()
    }
    private val frameCache by lazy {
        FrameCache(3, dataSource)
    }

    private val queuqTask by lazy {
        arrayListOf<DecodeFrame>()
    }

    private val DEF_TIME_OUT = 10000L
    private var decoder: MediaCodec
    private val defDecoderColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible

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


    fun getFrame(second: Float, success: (Bitmap) -> Unit, failed: (Throwable) -> Unit) {
        val target = videoAnalyze.getValidSampleTime(mediaFormat.getSafeTimeUS(second))
        frameCache.asyncGetTarget(target, success, {
            // 如果正在取这一帧，就不作任何处理
            if (queuqTask.isNotEmpty() && queuqTask[0].sampleTime ==
                    target) {
            } else {
                Log.d(TAG, "getFrame second $second sampleTime ${videoAnalyze.getValidSampleTime(mediaFormat.getSafeTimeUS(second))}: ")
                DecodeFrame(second, success, failed).start()
            }
        })
    }

    fun release() {
        videoAnalyze.release()
        if (queuqTask.isNotEmpty()) {
            queuqTask.forEach {
                it.release()
            }
        }
        decoder.release()
    }

    inner class DecodeFrame(target: Float,
                            private val success: (Bitmap) -> Unit,
                            private val failed: (Throwable) -> Unit) {

        val sampleTime: Long
        private val core: Observable<Bitmap>
        private var flint: Disposable? = null

        init {
            sampleTime = videoAnalyze.getValidSampleTime(when {
                target < 0L -> 0L
                target * 1000000 > mediaFormat.duration -> mediaFormat.duration
                else -> (target * 1000000).toLong()
            })

            core = Observable.create<Bitmap> { emitter ->
                decoder.configure(mediaFormat, decodeCore.fkOutputSurface(mediaFormat.width, mediaFormat.height),
                        null, 0)
                decoder.start()
                //处理目标时间帧
                handleFrame(sampleTime, emitter)

                //如果队列没有紧急任务，那么就抽空往下渲染几帧,
                val timeList = videoAnalyze.getLaterTime(sampleTime, 5)
                if (timeList.isNotEmpty()) {
                    timeList.forEach {
                        handleFrame(it, null)
                    }
                } else {
                    emitter.onComplete()
                }
            }.subscribeOn(Schedulers.from(decoderExecutor))
                    .doOnComplete {
                        schedulerNext()
                    }
                    .observeOn(AndroidSchedulers.mainThread())
        }

        private fun handleFrame(time: Long, emitter: ObservableEmitter<Bitmap>? = null) {
            // 缓存移到外部
//            frameCache.getLruBitmap(time)?.apply {
//                emitter?.onNext(this)
//                endCore()
//            }
//            var diskBitmap: Bitmap? = null
//            val diskCost = measureTimeMillis {
//                diskBitmap = frameCache.blockingGetDiskCache(time)
//            }
//
//            Log.d(TAG, "disk cache time $time cost $diskCost: ")
//            diskBitmap?.apply {
//                emitter?.onNext(this)
//                endCore()
//            }
            val info = MediaCodec.BufferInfo()
            decoder.dequeueValidInputBuffer(DEF_TIME_OUT) { inputBufferId, inputBuffer ->
                videoAnalyze.mediaExtractor.seekTo(time, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                val sampleSize = videoAnalyze.mediaExtractor.readSampleData(inputBuffer, 0)
                // 将数据压入到输入队列
                val presentationTimeUs = videoAnalyze.mediaExtractor.sampleTime
                Log.d(TAG, "${if (emitter != null) "main time" else "fuck time"} dequeue time is $time ")
                decoder.queueInputBuffer(inputBufferId, 0,
                        sampleSize, presentationTimeUs, MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
            }

            //如果不给codec标记结束印记的话，就拿不到输出的数据，dequeueOutputBuffer会一直返回-1
            decoder.dequeueValidInputBuffer(DEF_TIME_OUT) { inputBufferId, inputBuffer ->
                decoder.queueInputBuffer(inputBufferId, 0, 0, 0L,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }

            var notGetResult = true
            val start = System.currentTimeMillis()
            while (notGetResult) {
                // outputBuffer不会即刻得到数据，必须循环
                val id = decoder.dequeueOutputBuffer(info, DEF_TIME_OUT)
                Log.d(TAG, "output $time id is $id: ")
                if (id >= 0) {
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        // 2019/2/12-22:59 bufferInfo无可用缓存
                        notGetResult = false
                    }
                    decodeCore.codeFrameBitmap(info, id, decoder) {
                        emitter?.onNext(it)
                        //缓存这一帧
                        frameCache.cacheFrame(time, it)
                        //解码每一帧的时候都要观察队列是否有紧急任务
                        endCore()
                        notGetResult = false
                    }
                }
                if (System.currentTimeMillis() - start >= 1000L) {
                    notGetResult = false
                }
            }
            decoder.flush()
        }

        private fun endCore() {
            if (queuqTask.size > 1) {
                //队列还有任务.抛出异常用来终止上游继续运行
                throw EndSignal()
            }
        }

        fun start() {
            if (queuqTask.isEmpty()) {
                run()
            } else {
                if (queuqTask.contains(this)) {
                    //提升优先级,此时位于0index的不可能为本身，所以和1交换
                    Collections.swap(queuqTask, queuqTask.indexOf(this), 1)
                } else {
                    // 入列
                    queuqTask.add(this)
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
            queuqTask.add(this)
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
            decoder.reset()
            flint?.dispose()
            queuqTask.remove(this)
            if (queuqTask.isNotEmpty()) {
                queuqTask.get(0).start()
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