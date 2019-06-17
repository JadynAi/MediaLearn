package com.jadyn.mediakit.audio

/**
 *@version:
 *@FileDescription: 录音记录，回调声音帧
 *@Author:Jing
 *@Since:2019-05-17
 *@ChangeList:
 */

/**
 *
 * @param use an external array to judge if a stop is required
 * */
class AudioRecorder(private val sampleRate: Int = 44100,
                    private val isRecoding: List<Any>,
                    private val dataCallBack: (size: Int, data: ByteArray) -> Unit) : Runnable {

    private val audioOps by lazy {
        AudioOps(sampleRate)
    }

    override fun run() {
        audioOps.start()
        while (isRecoding.isNotEmpty()) {
            audioOps.read { size, sampleData ->
                if (size > 0) {
                    dataCallBack.invoke(size, sampleData)
                }
            }
        }
        dataCallBack.invoke(-1, byteArrayOf())
        audioOps.release()
    }
} 