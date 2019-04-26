package com.jadyn.mediakit.video.decode

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import com.jadyn.mediakit.function.*
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

/**
 *@version:
 *@FileDescription: 视频解码类
 *@Author:jing
 *@Since:2019/1/28
 *@ChangeList:
 */
class VideoDecoder2Compat(private val success: (Bitmap) -> Unit) {

    private val TAG = this.javaClass.name

    private lateinit var videoAnalyze: VideoAnalyze

    private lateinit var mediaFormat: MediaFormat

    private lateinit var decodeCore: GLCore

    private val DEF_TIME_OUT = 10000L
    private lateinit var decoder: MediaCodec

    private val defDecoderColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible

    private val scheduler by lazy {
        Schedulers.io()
    }

    private val publish by lazy {
        val create = PublishSubject.create<Bitmap>()
        create.subscribeOn(scheduler)
        create.observeOn(AndroidSchedulers.mainThread()).subscribe(success)
        create
    }


    //-------decode----------
    private var decoderDisposable: Disposable? = null

    private var isStart = false

    fun setDataSource(dataSource: String) {
        if (decoderDisposable != null) {
            stop()
        }
        val subscribe = Single.just("")
                .subscribeOn(scheduler)
                .delay(2, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    decodeCore = GLCore()
                    videoAnalyze = VideoAnalyze(dataSource)
                    mediaFormat = videoAnalyze.mediaFormat
                    val mime = mediaFormat.mime
                    decoder = MediaCodec.createDecoderByType(mime)
                    start()
                }, {

                })
    }

    fun start(success: () -> Unit = {}, failed: () -> Unit = {}) {
        decoderDisposable = Observable.fromCallable {
            decodeToFrames()
        }.subscribeOn(scheduler).subscribe({
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
    }

    private fun decodeToFrames() {
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, defDecoderColorFormat)
        decoder.configure(mediaFormat, decodeCore.fkOutputSurface(mediaFormat.width, mediaFormat.height),
                null, 0)

        Log.d(TAG, "start decode frames")
        isStart = true
        val bufferInfo = MediaCodec.BufferInfo()
        // 是否输入完毕
        var inputEnd = false
        // 是否输出完毕
        var outputEnd = false
        decoder.start()

        var advanceCount = 0

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
                outputEnd = true
            }) {
                Log.d("TimeTest", "out info ${bufferInfo.presentationTimeUs}")
                if (decodeCore.updateTexture(bufferInfo, it, decoder)) {
                    val bitmap = decodeCore.generateFrame()
                    publish.onNext(bitmap)
                }
            }
        }
    }
}