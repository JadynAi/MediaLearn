package com.jadyn.mediakit.camera2

import android.media.MediaMuxer
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread

/**
 *@version:
 *@FileDescription: mux模块，从H264队列和AAC队列拿去数据，编码为视频文件
 *@Author:Jing
 *@Since:2019-05-07
 *@ChangeList:
 */
class Muxer {

    private val thread by lazy {
        val handlerThread = HandlerThread("Camera2 Muxer")
        handlerThread.start()
        handlerThread
    }

    private val handler by lazy {
        Handler(thread.looper)
    }

    private lateinit var mediaMuxer: MediaMuxer

    fun start(isRecording: List<Any>, outputPath: String?) {
        handler.post {
            if (::mediaMuxer.isInitialized) {
                throw RuntimeException("MediaMuxer already init")
            }
            val defP = Environment.getExternalStorageDirectory().toString() + "/${System.currentTimeMillis()}.mp4"
            val p = if (outputPath.isNullOrBlank()) defP else outputPath.trim()
            mediaMuxer = MediaMuxer(p, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            while (isRecording.isNotEmpty()) {
                
            }
        }
    }
}

