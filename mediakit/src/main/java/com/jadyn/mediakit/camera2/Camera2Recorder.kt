package com.jadyn.mediakit.camera2

import android.os.Handler
import android.os.HandlerThread
import com.jadyn.mediakit.audio.AudioPacket
import com.jadyn.mediakit.audio.AudioRecorder
import com.jadyn.mediakit.video.encode.VideoRecorder
import java.util.concurrent.ConcurrentLinkedDeque

/**
 *@version:
 *@FileDescription: 视频录制类
 *@Author:Jing
 *@Since:2019-05-07
 *@ChangeList:
 */


/**
 *
 *  isRecording 使用数组来判定是否需要停止
 * */
class Camera2Recorder {

    //PCM 队列
    private val audioQueue by lazy {
        ConcurrentLinkedDeque<AudioPacket>()
    }
    // 视频帧队列
    private val videoQueue by lazy {
        ConcurrentLinkedDeque<VideoPacket>()
    }

    private val isRecording = arrayListOf<Int>()
    //-----video-------
    private val videoThread by lazy {
        val handlerThread = HandlerThread("videoRecorder")
        handlerThread.start()
        handlerThread
    }
    private val videoHandler by lazy {
        Handler(videoThread.looper)
    }
    //-----audio-------
    private val audioThread by lazy {
        val handlerThread = HandlerThread("audioRecorder")
        handlerThread.start()
        handlerThread
    }
    private val audioHandler by lazy {
        Handler(audioThread.looper)
    }
    //-----mux-------
    

    /**
     * 设置输出视频的宽高、比特率、帧率、帧间隔。输出视频路径
     *
     * 音频的参数设置全部使用音频录制类的默认参数，此处不设外参
     * */
    fun start(width: Int, height: Int, bitRate: Int, frameRate: Int = 24,
              frameInterval: Int = 5, outputPath: String?) {
        isRecording.add(1)
        videoHandler.post(VideoRecorder(width, height, bitRate, frameRate,
                frameInterval, isRecording, {}, { frame, timeStamp, bufferInfo, data ->
            data.position(bufferInfo.offset)
            data.limit(bufferInfo.offset + bufferInfo.size)
            val byteArray = ByteArray(data.remaining())
            data.get(byteArray, 0, byteArray.size)
            val videoPacket = VideoPacket(byteArray, byteArray.size, timeStamp, frame)
            videoQueue.push(videoPacket)
        }))
        audioHandler.post(AudioRecorder(isRecoding = isRecording, dataCallBack = {
            val audioPacket = AudioPacket(it, it.size)
            audioQueue.push(audioPacket)
        }))
        
        
    }

    fun reset() {
        videoQueue.clear()
        audioQueue.clear()
        videoHandler.removeCallbacksAndMessages(null)
    }
} 