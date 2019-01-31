package com.jadyn.ai.medialearn.camera.audio

import android.media.*
import android.util.Log

/**
 *@version:
 *@FileDescription:
 *@Author:jing
 *@Since:2019/1/4
 *@ChangeList:
 */
class AiLoiAudioEncoder(sampleRate: Int = 44100, channel: Int = AudioFormat.CHANNEL_IN_MONO,
                        audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT) {

    private val TAG = "AudioEncoder"

    private var minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channel, audioFormat)

    val audioData by lazy {
        ByteArray(minBufferSize)
    }

    private lateinit var audioRecord: AudioRecord

    private lateinit var encoder: MediaCodec

    private val bufferInfo by lazy {
        MediaCodec.BufferInfo()
    }

    init {
        try {
            audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channel,
                    audioFormat, minBufferSize)

            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC,
                    sampleRate, channel)
            format.setInteger(MediaFormat.KEY_BIT_RATE, 128000)
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0)
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    fun start() {
        audioRecord.startRecording()
        encoder.start()
    }

    private var trackIndex: Int = 0

    fun startMuxer(muxer: MediaMuxer) {
        val read = audioRecord.read(audioData, 0, minBufferSize)
        if (read < 0) {
            return
        }
        val inputBufferIndex = encoder.dequeueInputBuffer(-1)
        if (inputBufferIndex >= 0) {
            val byteBuffer = encoder.getInputBuffer(inputBufferIndex)
            byteBuffer.clear()
            // 2019/1/5-16:30 数据添加到输入缓存
            byteBuffer.put(audioData)
            encoder.queueInputBuffer(inputBufferIndex, 0, minBufferSize, 0, 0)
        }

        loop@ while (true) {
            val outputBufferId = encoder.dequeueOutputBuffer(bufferInfo, 1000)
            Log.d(TAG, "drainEncoder outputBufferId : $outputBufferId")
            if (outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break@loop
            } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat = encoder.outputFormat
                Log.d(TAG, "encoder output format changed: $newFormat")
                trackIndex = muxer.addTrack(newFormat)
                muxer.start()
            } else if (outputBufferId < 0) {
                Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: $outputBufferId")
            } else {
                val encodedData = encoder.getOutputBuffer(outputBufferId)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    bufferInfo.size = 0
                }
                if (bufferInfo.size != 0) {
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)

                    muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    Log.d(TAG, "sent " + bufferInfo.size + " bytes to muxer")
                }
                encoder.releaseOutputBuffer(outputBufferId, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    break@loop
                }
            }
        }
    }

}