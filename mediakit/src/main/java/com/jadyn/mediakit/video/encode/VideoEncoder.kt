package com.jadyn.mediakit.video.encode

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import android.util.Size
import android.view.Surface
import com.jadyn.mediakit.function.createVideoFormat
import com.jadyn.mediakit.function.handleOutputBuffer
import com.jadyn.mediakit.function.perFrameTime
import java.io.File
import java.io.IOException

/**
 *@version:
 *@FileDescription: 视频编码
 *@Author:Jing
 *@Since:2019/3/29
 *@ChangeList:
 */
class VideoEncoder(private val width: Int, private val height: Int,
                   val bitRate: Int,
                   frameRate: Int = 30,
                   frameInterval: Int = 5) {

    private val TAG = "VideoEncoder"

    private val mediaFormat by lazy {
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
    private var mediaMuxer: MediaMuxer? = null
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
        val file = File(outputPath)
        if (file?.exists()) {
            file.delete()
        }
        codec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = codec.createInputSurface()
        try {
            mediaMuxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (e: IOException) {
            throw RuntimeException("create media muxer failed $e")
        }
        codec.start()
        encodeCore.buildEGLSurface(inputSurface)
    }

    /**
     *
     * @b : draw bitmap to texture
     *
     * @presentTime: frame current time
     * */
    fun drainFrame(b: Bitmap, presentTime: Long) {
        encodeCore.drainFrame(b, presentTime)
        drainCoder(false)
    }

    fun drainFrame(b: Bitmap, index: Int) {
        drainFrame(b, index * mediaFormat.perFrameTime * 1000)
    }

    fun drainEnd() {
        drainCoder(true)
        encodeCore.release()
        codec.stop()
        codec.reset()
        mediaMuxer?.stop()
        mediaMuxer?.release()
        mediaMuxer = null
    }

    private fun drainCoder(endOfSteams: Boolean) {
        if (endOfSteams) {
            codec.signalEndOfInputStream()
        }
        val defTimeOut: Long = 1000
        codec.handleOutputBuffer(bufferInfo, defTimeOut, {
            if (muxerStarted) {
                throw RuntimeException("already muxer started!!!")
            }
            Log.d(TAG, "format changed ${codec.outputFormat} ")
            trackIndex = mediaMuxer!!.addTrack(codec.outputFormat)
            mediaMuxer!!.start()
            muxerStarted = true
        }, {
            val encodedData = codec.getOutputBuffer(it)
            Log.d(TAG, "buffer info flag ${bufferInfo.flags}: ")
            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                bufferInfo.size = 0
            }
            if (bufferInfo.size != 0) {
                if (!muxerStarted) {
                    throw RuntimeException("muxer hasn't started")
                }
                Log.d(TAG, "buffer info offset ${bufferInfo.offset} time is ${bufferInfo.presentationTimeUs} ")
                encodedData.position(bufferInfo.offset)
                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                mediaMuxer!!.writeSampleData(trackIndex, encodedData, bufferInfo)
                Log.d(TAG, "sent " + bufferInfo.size + " bytes to muxer")
            }
            codec.releaseOutputBuffer(it, false)
        }, !endOfSteams)
    }
}