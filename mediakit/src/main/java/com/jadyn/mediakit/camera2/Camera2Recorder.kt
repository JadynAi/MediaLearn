package com.jadyn.mediakit.camera2

import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import com.jadyn.mediakit.audio.AudioEncoder
import com.jadyn.mediakit.function.copy
import com.jadyn.mediakit.function.createAACFormat
import com.jadyn.mediakit.video.encode.VideoRecorder
import java.util.concurrent.Executors

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

    private val TAG = "Camera2Recorder"
    private val isRecording = arrayListOf<Int>()
    //-----video-------
    private val videoThread by lazy {
        Executors.newSingleThreadExecutor()
    }
    //-----audio-------
    private val audioEncoder by lazy {
        AudioEncoder(createAACFormat(1411000))
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
        val videoFormats = arrayListOf<MediaFormat>()
        val audioFormats = arrayListOf<MediaFormat>()
        val videoRecorder = VideoRecorder(width, height, bitRate, frameRate,
                frameInterval, isRecording, surfaceCallback, { frame, timeStamp, bufferInfo, data ->
            val byteArray = ByteArray(data.remaining())
            data.get(byteArray, 0, byteArray.size)
            Log.d(TAG, "video callback present: ${bufferInfo.presentationTimeUs}")
            Log.d(TAG, "video1 recorder data: $data")
            val videoPacket = VideoPacket(byteArray, data.remaining(), timeStamp, frame, bufferInfo.copy())
            mux.pushVideo(videoPacket)
        }) {
            // 得到输出video format
            videoFormats.add(it)
        }
        videoThread.execute(videoRecorder)
        audioEncoder.formatChanged = {
            audioFormats.add(it)
        }
        audioEncoder.start(isRecording) { data, frameCount ->
            // 音频录音，并编码为AAC数据，再封装到音频帧数据回调.
            Log.d(TAG, "audio  ${data.size}: ")
            mux.pushAudio(data)
        }
        mux.start(isRecording, outputPath, videoFormats, audioFormats)
    }

    fun stop() {
        isRecording.clear()
    }

    fun release() {
        videoThread.shutdownNow()
        audioEncoder.release()
        mux.release()
    }
} 