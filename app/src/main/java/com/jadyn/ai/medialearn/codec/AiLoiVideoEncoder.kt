package com.jadyn.ai.medialearn.codec

import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaMuxer
import android.os.Environment
import android.util.Log
import android.util.Size
import android.view.Surface
import com.jadyn.mediakit.function.createVideoFormat
import com.jadyn.mediakit.gl.CodecInputSurface
import java.io.File
import java.io.IOException
import java.util.*

/**
 *@version:
 *@FileDescription:
 *@Author:jing
 *@Since:2018/12/5
 *@ChangeList:
 */
class AiLoiVideoEncoder(private val width: Int, private val height: Int,
                        bitRate: Int,
                        frameRate: Int = 30,
                        frameInterval: Int = 5) {

    private val MIME_TYPE = "video/avc"
    private lateinit var encoder: MediaCodec
    private lateinit var inputSurface: CodecInputSurface
    private var mediaMuxer: MediaMuxer? = null

    private val TAG = "encoder"

    private var trackIndex: Int = 0
    private var muxerStarted: Boolean = false

    private val format by lazy {
        createVideoFormat(Size(width, height), bitRate = bitRate, frameRate = frameRate,
                iFrameInterval = frameInterval)
    }

    private val outPath: String
        get() {
            val instance = java.util.Calendar.getInstance()
            val file = File(Environment.getExternalStorageDirectory(),
                    "test${width}x$height${instance.get(java.util.Calendar.HOUR_OF_DAY)}" +
                            ": ${instance.get(Calendar.MINUTE)}.mp4")
            return file.toString()
        }

    init {
        try {
            encoder = MediaCodec.createEncoderByType(MIME_TYPE)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private val bufferInfo by lazy {
        MediaCodec.BufferInfo()
    }

    private var stMgr: TextureFunction? = null

    /*
    * 将encoderSurface作为渲染目标。这一步必须在SurfaceManager初始化之前执行。否则会无法创建vertex shader
    * 
    * MediaCodeC重置后，需要重新配置，重新生成inputSurface
    * */
    private fun makeCurrent() {
        Log.d(TAG, "makeCurrent ")
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = CodecInputSurface(encoder.createInputSurface())
        inputSurface.makeCurrent()
    }

    fun createSurface(width: Int, height: Int): Surface {
        makeCurrent()
        val texture = createTexture(width, height)

        try {
            Log.d(TAG, "outputPath is : $outPath")
            mediaMuxer = MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        trackIndex = -1
        muxerStarted = false
        return Surface(texture)
    }

    fun createTexture(width: Int, height: Int): SurfaceTexture {
        if (stMgr == null) {
            stMgr = TextureFunction()
        }
        val texture = stMgr!!.surfaceTexture?.apply {
            setDefaultBufferSize(width, height)
        }
        return texture!!
    }

    private var isStart = false

    fun start() {
        if (!::encoder.isInitialized) {
            Log.wtf(TAG, "encoder initial failed ")
            return
        }
        isStart = true
        encoder.start()
        Log.d(TAG, "codec started thread${Thread.currentThread().name}")

        val surfaceTexture = stMgr!!.surfaceTexture
        var frameCount = 0
        val startWhen = System.nanoTime()
        while (isStart) {
            drainEncoder(false)
            frameCount++

            stMgr!!.awaitNewImage()
            stMgr!!.drawImage()
            Log.d(TAG, "present: " + (surfaceTexture!!.timestamp - startWhen) / 1000000.0 + "ms")
            inputSurface.setPresentationTime(surfaceTexture.timestamp)
            inputSurface.swapBuffers()
        }
        drainEncoder(true)
        stopEncoder()
    }

    private fun drainEncoder(endOfStream: Boolean) {
        val timeout_usec = 10000
        if (endOfStream) {
            encoder.signalEndOfInputStream()
        }
        loop@ while (true) {
            val outputBufferId = encoder.dequeueOutputBuffer(bufferInfo, timeout_usec.toLong())
            Log.d(TAG, "drainEncoder outputBufferId : $outputBufferId")
            if (outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    break@loop
                }
            } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (muxerStarted) {
                    throw RuntimeException("format changed twice")
                }
                val newFormat = encoder.outputFormat
                Log.d(TAG, "encoder output format changed: $newFormat")
                trackIndex = mediaMuxer!!.addTrack(newFormat)
                mediaMuxer!!.start()
                muxerStarted = true
            } else if (outputBufferId < 0) {
                Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: $outputBufferId")
            } else {
                val encodedData = encoder.getOutputBuffer(outputBufferId)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    bufferInfo.size = 0
                }
                if (bufferInfo.size != 0) {
                    if (!muxerStarted) {
                        throw RuntimeException("muxer hasn't started")
                    }
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)

                    mediaMuxer!!.writeSampleData(trackIndex, encodedData, bufferInfo)
                    Log.d(TAG, "sent " + bufferInfo.size + " bytes to muxer")
                }

                encoder.releaseOutputBuffer(outputBufferId, false)

                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    if (!endOfStream) {
                        Log.w(TAG, "reached end of stream unexpectedly")
                    }
                    break@loop
                }
            }
        }
    }

    fun stop() {
        isStart = false
    }

    private fun stopEncoder() {
        encoder.stop()
        mediaMuxer!!.stop()
        mediaMuxer!!.release()
        mediaMuxer = null

        // 2019/1/3-18:07 MediaCodeC状态重置
        encoder.reset()
        // 2019/1/3-18:20 重置SurfaceTexture
        stMgr!!.release()
        stMgr = null
    }

    fun release() {
        encoder.release()
        mediaMuxer?.release()
    }
}