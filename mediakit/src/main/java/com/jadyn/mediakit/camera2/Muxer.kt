package com.jadyn.mediakit.camera2

import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import com.jadyn.mediakit.audio.AudioPacket
import com.jadyn.mediakit.function.popSafe
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedDeque

/**
 *@version:
 *@FileDescription: mux模块，从H264队列和AAC队列拿去数据，编码为视频文件
 *@Author:Jing
 *@Since:2019-05-07
 *@ChangeList:
 */
class Muxer {

    //AAC 音频帧队列
    private val audioQueue by lazy {
        ConcurrentLinkedDeque<AudioPacket>()
    }
    // 视频帧队列
    private val videoQueue by lazy {
        ConcurrentLinkedDeque<VideoPacket>()
    }

    private val thread by lazy {
        val handlerThread = HandlerThread("Camera2 Muxer")
        handlerThread.start()
        handlerThread
    }

    private val handler by lazy {
        Handler(thread.looper)
    }

    private lateinit var mediaMuxer: MediaMuxer

    fun start(isRecording: List<Any>, outputPath: String?,
              videoTracks: List<MediaFormat>,
              audioTracks: List<MediaFormat>) {
        handler.post {
            // 循环直到拿到可用的 输出视频轨和音频轨
            loop@ while (true) {
                if (videoTracks.isNotEmpty() && audioTracks.isNotEmpty()) {
                    start(isRecording, outputPath, videoTracks[0], audioTracks[0])
                    break@loop
                }
            }
        }
    }

    fun start(isRecording: List<Any>, outputPath: String?,
              videoTrack: MediaFormat? = null,
              audioTrack: MediaFormat? = null) {
        handler.post {
            if (::mediaMuxer.isInitialized) {
                throw RuntimeException("MediaMuxer already init")
            }
            val defP = Environment.getExternalStorageDirectory().toString() + "/${System.currentTimeMillis()}.mp4"
            val p = if (outputPath.isNullOrBlank()) defP else outputPath.trim()
            mediaMuxer = MediaMuxer(p, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val videoTrackId = mediaMuxer.addTrack(videoTrack)
            val audioTrackId = mediaMuxer.addTrack(audioTrack)
            mediaMuxer.start()
            while (isRecording.isNotEmpty()) {
                val videoFrame = videoQueue.popSafe()
                val audioFrame = audioQueue.popSafe()
                videoFrame?.apply {
                    mediaMuxer.writeSampleData(videoTrackId, ByteBuffer.wrap(videoFrame.buffer), videoFrame.bufferInfo)
                }
                audioFrame?.apply {
                    mediaMuxer.writeSampleData(audioTrackId, ByteBuffer.wrap(audioFrame.buffer), audioFrame.bufferInfo)
                }
            }
            mediaMuxer.stop()
            mediaMuxer.release()
        }
    }

    fun pushVideo(videoPacket: VideoPacket) {
        videoQueue.offer(videoPacket)
    }

    fun pushAudio(audioPacket: AudioPacket) {
        audioQueue.offer(audioPacket)
    }

    fun release() {
        videoQueue.clear()
        audioQueue.clear()
        handler.removeCallbacksAndMessages(null)
        thread.quitSafely()
    }
}

