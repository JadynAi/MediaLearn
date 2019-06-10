package com.jadyn.mediakit.audio

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import com.jadyn.mediakit.function.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.Executors

/**
 *@version:
 *@FileDescription: 将 PCM 数据编码为 AAC 数据
 *@Author:Jing
 *@Since:2019-05-20
 *@ChangeList:
 */

/**
 * @param format 编码的AAC文件的数据参数:采样率、声道、比特率等
 * */
class AudioEncoder(private val format: MediaFormat) {

    private val TAG = "AudioEncoder"

    private val audioThreads by lazy {
        Executors.newFixedThreadPool(2)
    }

    //PCM 队列
    private val audioQueue by lazy {
        ConcurrentLinkedDeque<ByteArray>()
    }

    private var isFormatChanged = false

    var formatChanged: (MediaFormat) -> Unit = {}

    fun start(isRecording: List<Any>, dataCallback: (data: AudioPacket, frameCount: Int) -> Unit) {
        // 录音并回调PCM数据
        audioThreads.execute(AudioRecorder(isRecoding = isRecording, dataCallBack = {
            Log.d(TAG, "audio pcm ${Thread.currentThread().name} size :${it.size}: ")
            audioQueue.add(it)
        }))
        // 从PCM 队列中获取数据并编码为AAC 数据格式
        audioThreads.execute {
            var frameCount = 0
            val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            val bufferInfo = MediaCodec.BufferInfo()
            // 循环的拿取PCM数据，编码为AAC数据
            while (isRecording.isNotEmpty()) {
                Log.d(TAG, "audio encoder ${Thread.currentThread().name}: queue ${audioQueue.isEmpty()} ")
                if (audioQueue.isNotEmpty()) {
                    Log.d(TAG, " audio queue encoder")
                    val bytes = audioQueue.pop()
                    var isEnd = false
                    while (!isEnd) {
                        codec.dequeueValidInputBuffer(1000) { inputBufferId, inputBuffer ->
                            inputBuffer.clear()
                            inputBuffer.put(bytes)
                            inputBuffer.limit(bytes.size)
                            codec.queueInputBuffer(inputBufferId, 0, bytes.size
                                    , frameCount * format.aacPerFrameTime, 0)
                        }

                        codec.disposeOutput(bufferInfo, 1000, {
                            isEnd = true
                        }, {
                            // audio format changed
                            if (!isFormatChanged) {
                                formatChanged.invoke(codec.outputFormat)
                                isFormatChanged = true
                            }
                        }, {
                            val outputBuffer = codec.getOutputBuffer(it)
                            val outDataSize = bufferInfo.size + 7
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            val data = ByteArray(outDataSize)
                            data.addADTS(outDataSize)
                            outputBuffer.get(data, 7, bufferInfo.size)
                            outputBuffer.position(bufferInfo.size)
                            frameCount++
                            val audioPacket = AudioPacket(data, data.size, bufferInfo.copy())
                            dataCallback.invoke(audioPacket, frameCount)
                            codec.releaseOutputBuffer(it, false)
                        })
                    }
                }
            }

            codec.release()
        }
    }

    fun release() {
        audioThreads.shutdownNow()
    }
} 