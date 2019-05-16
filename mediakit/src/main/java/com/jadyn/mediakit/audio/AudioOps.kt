package com.jadyn.mediakit.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

/**
 *@version:
 *@FileDescription:
 *@Author:Jing
 *@Since:2019-05-06
 *@ChangeList:
 */
class AudioOps(private val sampleRate: Int = 44100,
               private val channel: Int = AudioFormat.CHANNEL_IN_MONO) {

    private val audioRecord by lazy {
        val bit = AudioFormat.ENCODING_PCM_16BIT
        val minB = AudioRecord.getMinBufferSize(sampleRate, channel,
                bit)
        AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRate, channel, bit, minB)
    }

    private var isStart = false

    fun start() {
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            throw RuntimeException("AudioRecord not initialized")
        }
        isStart = true
        audioRecord.startRecording()
    }
} 