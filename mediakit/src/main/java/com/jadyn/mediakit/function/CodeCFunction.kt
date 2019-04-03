package com.jadyn.mediakit.function

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import android.util.Size
import java.nio.ByteBuffer

/**
 *@version:
 *@FileDescription:硬编码相关辅助类
 *@Author:jing
 *@Since:2019/2/12
 *@ChangeList:
 */

/*
* 处理MediaCodeC输出队列数据
* */
fun MediaCodec.disposeOutput(bufferInfo: MediaCodec.BufferInfo, defTimeOut: Long,
                             endStream: () -> Unit = {},
                             render: (outputBufferId: Int) -> Unit,
                             formatChanged: () -> Unit = {}) {
    //  获取可用的输出缓存队列
    val outputBufferId = dequeueOutputBuffer(bufferInfo, defTimeOut)
    Log.d("disposeOutput", "output buffer id : $outputBufferId ")
    if (outputBufferId >= 0) {
        // 2019/2/12-22:55 and是位运算 &，转换为二进制进行“与”运算.位数不匹配则都为0
        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            // 2019/2/12-22:59 bufferInfo无可用缓存
            endStream.invoke()
        }
        render.invoke(outputBufferId)
    } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
        formatChanged.invoke()
    }
}

/*
* 硬编码获得可用的输入队列
* */
fun MediaCodec.dequeueValidInputBuffer(timeOutUs: Long, input: (inputBufferId: Int, inputBuffer: ByteBuffer) -> Unit): Boolean {
    val inputBufferId = dequeueInputBuffer(timeOutUs)
    if (inputBufferId >= 0) {
        input.invoke(inputBufferId, getInputBuffer(inputBufferId))
        return true
    }
    return false
}

fun MediaCodec.queueEndSteam(timeOutUs: Long) {
    dequeueValidInputBuffer(timeOutUs) { inputBufferId, inputBuffer ->
        queueInputBuffer(inputBufferId, 0, 0, 0L,
                MediaCodec.BUFFER_FLAG_END_OF_STREAM)
    }
}


fun createVideoFormat(size: Size, colorFormat: Int = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
                      bitRate: Int, frameRate: Int, iFrameInterval: Int): MediaFormat {
    return MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, size.width, size.height)
            .apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)
            }
}