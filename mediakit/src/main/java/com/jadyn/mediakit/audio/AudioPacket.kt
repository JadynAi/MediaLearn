package com.jadyn.mediakit.audio

import android.media.MediaCodec

/**
 *@version:
 *@FileDescription: 编码后的 AAC 音频数据帧对象
 *@Author:Jing
 *@Since:2019-05-20
 *@ChangeList:
 */
class AudioPacket(val buffer: ByteArray, val size: Int, val bufferInfo: MediaCodec.BufferInfo) {

    override fun toString(): String {
        bufferInfo?.apply {
            return presentationTimeUs.toString()
        }
        return ""
    }
} 