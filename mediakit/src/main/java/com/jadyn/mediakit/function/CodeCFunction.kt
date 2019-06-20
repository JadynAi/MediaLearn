package com.jadyn.mediakit.function

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.support.annotation.IntRange
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
                             formatChanged: () -> Unit = {},
                             render: (outputBufferId: Int) -> Unit) {
    //  获取可用的输出缓存队列
    val outputBufferId = dequeueOutputBuffer(bufferInfo, defTimeOut)
    Log.d("disposeOutput", "output buffer id : $outputBufferId ")
    when {
        outputBufferId >= 0 -> {
            // 2019/2/12-22:55 and是位运算 &，转换为二进制进行“与”运算.位数不匹配则都为0
            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                // 2019/2/12-22:59 bufferInfo无可用缓存
                endStream.invoke()
            }
            render.invoke(outputBufferId)
        }
        outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> formatChanged.invoke()
        outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER -> endStream.invoke()
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

fun MediaCodec.dequeueValidInputBuffer(timeOutUs: Long): InputCodeCData {
    val inputBufferId = dequeueInputBuffer(timeOutUs)
    if (inputBufferId >= 0) {
        return InputCodeCData(inputBufferId, getInputBuffer(inputBufferId))
    }
    return InputCodeCData(inputBufferId, null)
}

data class InputCodeCData(val id: Int, val inputBuffer: ByteBuffer?)

/**
 *
 * @param needEnd when bufferId is INFO_TRY_AGAIN_LATER, is need to break loop
 * */
fun MediaCodec.handleOutputBuffer(bufferInfo: MediaCodec.BufferInfo, defTimeOut: Long,
                                  formatChanged: () -> Unit = {},
                                  render: (bufferId: Int) -> Unit,
                                  needEnd: Boolean = true) {
    loopOut@ while (true) {
        //  获取可用的输出缓存队列
        val outputBufferId = dequeueOutputBuffer(bufferInfo, defTimeOut)
        Log.d("handleOutputBuffer", "output buffer id : $outputBufferId ")
        if (outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
            if (needEnd) {
                break@loopOut
            }
        } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            formatChanged.invoke()
        } else if (outputBufferId >= 0) {
            render.invoke(outputBufferId)
            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                break@loopOut
            }
        }
    }
}


fun createVideoFormat(size: Size, colorFormat: Int = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
                      bitRate: Int, frameRate: Int, iFrameInterval: Int): MediaFormat {
    return MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, size.width, size.height)
            .apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
//                setInteger(MediaFormat.KEY_BITRATE_MODE, BITRATE_MODE_CQ)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)
            }
}

/**
 * MPEG-4 AAC LC 低复杂度规格（Low Complexity），现在的手机比较常见的 MP4 文件中的音频部份就包括了该规格音频文件
 * MPEG-4 AAC Main 主规格
 * MPEG-4 AAC SSR 可变采样率规格（Scaleable Sample Rate）
 * MPEG-4 AAC LTP 长时期预测规格（Long Term Predicition）
 * MPEG-4 AAC LD 低延迟规格（Low Delay）
 * MPEG-4 AAC HE 高效率规格（High Efficiency）
 * */
fun createAACFormat(bitRate: Int = 128000, sampleRate: Int = 44100,
                    @IntRange(from = 1, to = 2) channel: Int = 1): MediaFormat {
    return MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate,
            channel).apply {
        setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
        // 默认使用LC底规格
        setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
    }
}

fun MediaCodec.BufferInfo.copy(): MediaCodec.BufferInfo {
    val copy = MediaCodec.BufferInfo()
    copy.set(offset, size, presentationTimeUs, flags)
    return copy
}