package com.jadyn.mediakit.camera2

import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import com.jadyn.ai.kotlind.utils.safeList
import com.jadyn.mediakit.audio.AudioEncoder
import com.jadyn.mediakit.audio.AudioPacket
import com.jadyn.mediakit.audio.AudioRecorder
import com.jadyn.mediakit.function.copy
import com.jadyn.mediakit.function.createAACFormat
import com.jadyn.mediakit.function.genData
import com.jadyn.mediakit.mux.Muxer
import com.jadyn.mediakit.video.encode.VideoRecorder
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors

/**
 *@version:
 *@FileDescription: 视频录制类
 *@Author:Jing
 *@Since:2019-05-07
 *@ChangeList:
 */


class VideoGen {

    private val TAG = "Camera2Recorder"
    private val isRecording = safeList<Int>()
    //-----video-------
    private val recorderThread by lazy {
        Executors.newFixedThreadPool(3)
    }
    //-----audio-------
    //PCM 队列
    private val audioQueue by lazy {
        ArrayBlockingQueue<ByteArray>(20)
    }

    //-----mux-------
    private val mux by lazy {
        Muxer()
    }

    /**
     * 设置输出视频的宽高、比特率、帧率、帧间隔。输出视频路径
     *
     * 音频的参数设置全部使用音频录制类的默认参数，此处不设外参
     * */
    fun start(width: Int, height: Int, bitRate: Int, frameRate: Int = 24,
              frameInterval: Int = 5,
              surfaceCallback: (surface: Surface) -> Unit,
              outputPath: String?) {
        isRecording.add(1)
        val videoFormats = safeList<MediaFormat>()
        val audioFormats = safeList<MediaFormat>()

        val videoRecorder = VideoRecorder(width, height, bitRate, frameRate,
                frameInterval, isRecording, surfaceCallback, { frame, timeStamp, bufferInfo, data ->
            val byteArray = data.genData()
            val videoPacket = VideoPacket(byteArray, data.remaining(), timeStamp, frame, bufferInfo.copy())
            mux.pushVideo(videoPacket)
        }) {
            // 得到输出video format
            videoFormats.add(it)
        }
        // 执行视频录制
        recorderThread.execute(videoRecorder)
        // 执行音频录制，回调PCM数据
        recorderThread.execute(AudioRecorder(isRecoding = isRecording, dataCallBack = { size, data ->
            Log.d(TAG, "audio pcm size : $size data :${data.size}: ")
            audioQueue.offer(data)
        }))
        // 执行音频编码，将PCM数据编码为AAC数据
        recorderThread.execute(AudioEncoder(isRecording, createAACFormat(128000),
                audioQueue, { byteBuffer, bufferInfo ->
            val data = ByteArray(byteBuffer.remaining())
            byteBuffer.get(data, 0, data.size)
            val audioPacket = AudioPacket(data, data.size, bufferInfo.copy())
            mux.pushAudio(audioPacket)
        }) {
            // 得到输出的audio format
            audioFormats.add(it)
        })

        mux.start(isRecording, outputPath, videoFormats, audioFormats)
    }

    fun stop() {
        isRecording.clear()
    }

    fun release() {
        stop()
        recorderThread.shutdown()
        mux.release()
    }
} 