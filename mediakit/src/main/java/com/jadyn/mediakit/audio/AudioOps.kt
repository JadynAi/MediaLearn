package com.jadyn.mediakit.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log

/**
 *@version:
 *@FileDescription:
 *@Author:Jing
 *@Since:2019-05-06
 *@ChangeList:
 */
class AudioOps(private val sampleRate: Int = 44100,
               private val channel: Int = AudioFormat.CHANNEL_IN_MONO) {
    private val TAG = "AudioOps"

    private var minBufferInByte = 0

    private val audioRecord by lazy {
        val bit = AudioFormat.ENCODING_PCM_16BIT
        minBufferInByte = AudioRecord.getMinBufferSize(sampleRate, channel,
                bit)
        Log.d(TAG, "min buffer size $minBufferInByte")
        AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRate, channel, bit, minBufferInByte)
    }

    var isRecording = false
        private set

    fun start() {
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            throw RuntimeException("AudioRecord not initialized")
        }
        isRecording = true
        audioRecord.startRecording()
    }

    fun read(dataCallback: (size: Int, sampleData: ByteArray) -> Unit) {
        val sampleData = ByteArray(minBufferInByte)
        val size = audioRecord.read(sampleData, 0, minBufferInByte)
        Log.d(TAG, "call back size $size sampleData ${sampleData.size}")
        dataCallback.invoke(size, sampleData)
    }

    fun stop() {
        isRecording = false
        audioRecord.stop()
    }

    fun release() {
        stop()
        audioRecord.release()
    }
} 