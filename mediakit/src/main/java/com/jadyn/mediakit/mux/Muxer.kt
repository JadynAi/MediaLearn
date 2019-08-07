package com.jadyn.mediakit.mux

import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import com.jadyn.mediakit.audio.AudioPacket
import com.jadyn.mediakit.camera2.VideoPacket
import com.jadyn.mediakit.function.toS
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 *@version:
 *@FileDescription: mux模块，从H264队列和AAC队列拿去数据，编码为视频文件
 *@Author:Jing
 *@Since:2019-05-07
 *@ChangeList:
 */
class Muxer {
    private val TAG = "videoMuxer"

    //AAC 音频帧队列
    private val audioQueue by lazy {
        ConcurrentLinkedQueue<AudioPacket>()
    }
    // 视频帧队列
    private val videoQueue by lazy {
        ConcurrentLinkedQueue<VideoPacket>()
    }

    private val thread by lazy {
        val handlerThread = HandlerThread("Camera2-Mux")
        handlerThread.start()
        handlerThread
    }

    private var handler: Handler? = null

    private var mediaMuxer: MediaMuxer? = null

    private var loggerStream: FileOutputStream? = null

    fun start(isRecording: List<Any>, outputPath: String?,
              videoTracks: List<MediaFormat>,
              audioTracks: List<MediaFormat>) {
        if (handler == null) {
            handler = Handler(thread.looper)
        }
        handler!!.post {
            // 循环直到拿到可用的 输出视频轨和音频轨
            loop@ while (true) {
                if (videoTracks.isNotEmpty() && audioTracks.isNotEmpty()) {
                    break@loop
                }
            }
            start(isRecording, outputPath, videoTracks[0], audioTracks[0])
        }
    }

    fun start(isRecording: List<Any>, outputPath: String?,
              videoTrack: MediaFormat,
              audioTrack: MediaFormat) {
        if (mediaMuxer != null) {
            throw RuntimeException("MediaMuxer already init")
        }
        val defP = Environment.getExternalStorageDirectory().toString() + "/music${System.currentTimeMillis()}.aac"
        val p = if (outputPath.isNullOrBlank()) defP else outputPath.trim()

        val instance = Calendar.getInstance()
        val log = File(Environment.getExternalStorageDirectory().toString()
                + "/log:${instance.get(Calendar.HOUR_OF_DAY)}" +
                ":${instance.get(Calendar.MINUTE)}.txt")
        log.setWritable(true)
        log.createNewFile()
        loggerStream = FileOutputStream(log)

        mediaMuxer = MediaMuxer(p, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val videoTrackId = mediaMuxer!!.addTrack(videoTrack)
        val audioTrackId = mediaMuxer!!.addTrack(audioTrack)
        mediaMuxer!!.start()

        while (isRecording.isNotEmpty()) {
            val audioFrame = audioQueue.peek()
            val videoFrame = videoQueue.peek()
            if (audioFrame == null || videoFrame == null) {
                continue
            }
            // 先判断队首的时间戳大小，先写入小的数据
            val audioTime = audioFrame.bufferInfo.presentationTimeUs
            val videoTime = videoFrame.bufferInfo.presentationTimeUs
            if (audioTime != -1L && videoTime != -1L) {
                // 先写小一点的时间戳的数据
                if (audioTime < videoTime) {
                    writeAudio(audioTrackId, audioQueue.poll())
                } else {
                    writeVideo(videoTrackId, videoQueue.poll())
                }
            }
        }
        loggerStream?.close()
        mediaMuxer!!.stop()
        mediaMuxer!!.release()
        mediaMuxer = null
    }

    private fun writeVideo(id: Int, videoFrame: VideoPacket) {
        videoFrame.apply {
            try {
                loggerStream?.write("video frame : ${bufferInfo?.toS()} \r\n".toByteArray())
            } catch (e: Exception) {

            }
            mediaMuxer!!.writeSampleData(id, ByteBuffer.wrap(buffer), bufferInfo)
        }
    }

    private fun writeAudio(id: Int, audioFrame: AudioPacket) {
        audioFrame.apply {
            try {
                loggerStream?.write("audio frame : ${bufferInfo?.toS()} \r\n".toByteArray())
            } catch (e: Exception) {

            }
            mediaMuxer!!.writeSampleData(id, ByteBuffer.wrap(buffer), bufferInfo)
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
        handler?.removeCallbacksAndMessages(null)
        thread.interrupt()
        thread.quitSafely()
        handler = null
        mediaMuxer = null
    }
}


