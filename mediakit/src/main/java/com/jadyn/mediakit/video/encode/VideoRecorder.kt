package com.jadyn.mediakit.video.encode

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.util.Size
import android.view.Surface
import com.jadyn.mediakit.function.createVideoFormat
import com.jadyn.mediakit.function.handleOutputBuffer
import java.io.IOException
import java.nio.ByteBuffer

/**
 *@version:
 *@FileDescription: 视频录制类，从相机获取视频帧数据，并回调
 *@Author:Jing
 *@Since:2019-05-21
 *@ChangeList:
 */

/**
 * @param readySurface 对外提供的录入Surface准备好
 *
 * @param isRecording 使用外部数组来判断是否需要停止
 *
 * */
class VideoRecorder(private val width: Int, private val height: Int,
                    bitRate: Int,
                    frameRate: Int = 25,
                    frameInterval: Int = 5,
                    private val isRecording: List<Any>,
                    private val readySurface: (Surface) -> Unit,
                    private val dataCallback: (frame: Int, timeStamp: Long, bufferInfo: MediaCodec.BufferInfo,
                                               data: ByteBuffer) -> Unit,
                    private val outputFormatChanged: (MediaFormat) -> Unit = {}) : Runnable {
    private val TAG = "VideoRecorder"

    private val mediaFormat = createVideoFormat(Size(width, height), bitRate = bitRate, frameRate = frameRate,
            iFrameInterval = frameInterval)

    private val encodeCore by lazy {
        SurfaceEncodeCore(width, height)
    }

    private val bufferInfo by lazy {
        MediaCodec.BufferInfo()
    }

    private lateinit var codec: MediaCodec
    private lateinit var inputSurface: Surface
    private var frameCount = 0
    private var isFormatChanged = false

    override fun run() {
        Log.d(TAG, "runnable init thread: ${Thread.currentThread().name} ")
        try {
            // Mime 决定输出数据格式，这里的AVC代表H264
            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        } catch (e: IOException) {
            throw RuntimeException("code c init failed $e")
        }
        codec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val s = codec.createInputSurface()
        val surfaceTexture = encodeCore.buildEGLSurface(s)
        inputSurface = Surface(surfaceTexture)
        readySurface.invoke(inputSurface)
        codec.start()

        val startTime = System.nanoTime()
        while (isRecording.isNotEmpty()) {
            drainEncoder(false)
            frameCount++
            encodeCore.draw()
            val curFrameTime = System.nanoTime() - startTime
            encodeCore.swapData(curFrameTime)
        }
        drainEncoder(true)
        codec.release()
        encodeCore.release()
        inputSurface.release()
    }

    private fun drainEncoder(isEnd: Boolean = false) {
        if (isEnd) {
            codec.signalEndOfInputStream()
        }
        codec.handleOutputBuffer(bufferInfo, 0, {
            if (!isFormatChanged) {
                outputFormatChanged.invoke(codec.outputFormat)
                isFormatChanged = true
            }
        }, {
            //            手动设置帧间隔
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            val bundle = Bundle()
//            bundle.putLong(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
//            codec.setParameters(bundle)
//            }

            val encodedData = codec.getOutputBuffer(it)
            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                bufferInfo.size = 0
            }
            if (bufferInfo.size != 0) {
                Log.d(TAG, "buffer info offset ${bufferInfo.offset} time is ${bufferInfo.presentationTimeUs} ")
                encodedData?.apply {
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    Log.d(TAG, "sent " + bufferInfo.size + " bytes to muxer")
                    dataCallback.invoke(frameCount, bufferInfo.presentationTimeUs, bufferInfo, encodedData)
                }
            }
            codec.releaseOutputBuffer(it, false)
        }, !isEnd)
    }
}
