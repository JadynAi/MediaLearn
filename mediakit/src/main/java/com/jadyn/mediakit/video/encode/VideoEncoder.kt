package com.jadyn.mediakit.video.encode

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Size
import com.jadyn.mediakit.function.createVideoFormat
import java.io.IOException

/**
 *@version:
 *@FileDescription: 视频编码
 *@Author:Jing
 *@Since:2019/3/29
 *@ChangeList:
 */
class VideoEncoder(private val width: Int, private val height: Int,
                   bitRate: Int,
                   frameRate: Int = 30,
                   frameInterval: Int = 5) {

    private val mediaformat by lazy {
        createVideoFormat(Size(width, height), bitRate = bitRate, frameRate = frameRate,
                iFrameInterval = frameInterval)
    }

    private val encodeCore by lazy {
        GLEncodeCore(width, height)
    }

    private lateinit var codec: MediaCodec

    init {
        try {
            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}