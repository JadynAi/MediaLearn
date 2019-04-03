package com.jadyn.mediakit.video.encode

import android.content.ContentValues
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import android.util.Size
import android.view.Surface
import com.jadyn.mediakit.function.createVideoFormat
import com.jadyn.mediakit.function.disposeOutput
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

    private val bufferInfo by lazy {
        MediaCodec.BufferInfo()
    }

    private var codec: MediaCodec
    private lateinit var mediaMuxer: MediaMuxer
    private lateinit var inputSurface: Surface

    private var trackIndex: Int = 0
    private var muxerStarted: Boolean = false

    init {
        try {
            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        } catch (e: IOException) {
            throw RuntimeException("code c init failed $e")
        }
    }

    fun start(outputPath: String) {
        codec.configure(mediaformat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = codec.createInputSurface()

        try {
            mediaMuxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (e: IOException) {
            throw RuntimeException("create media muxer failed $e")
        }
        codec.start()
        encodeCore.buildEGLSurface(inputSurface)
    }

    fun drainFrame(b: Bitmap) {
        drainCoder(false)
        encodeCore.drainFrame(b)
    }

    fun drainCoder(endOfSteams: Boolean) {
        if (endOfSteams) {
            codec.signalEndOfInputStream()
        }
        val defTimeOut: Long = 1000
        var outputDone = false
        while (!outputDone) {
            codec.disposeOutput(bufferInfo, defTimeOut, {
                outputDone = true
            }, {
                val encodedData = codec.getOutputBuffer(it)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    bufferInfo.size = 0
                }
                if (bufferInfo.size != 0) {
                    if (!muxerStarted) {
                        throw RuntimeException("muxer hasn't started")
                    }
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    mediaMuxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    Log.d(ContentValues.TAG, "sent " + bufferInfo.size + " bytes to muxer")
                }
                codec.releaseOutputBuffer(it, false)
            }, {
                trackIndex = mediaMuxer.addTrack(codec.outputFormat)
                mediaMuxer.start()
                muxerStarted = true
            })
        }
    }
}