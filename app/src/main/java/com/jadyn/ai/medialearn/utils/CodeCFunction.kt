package com.jadyn.ai.medialearn.utils

import android.media.MediaCodec

/**
 *@version:
 *@FileDescription:
 *@Author:jing
 *@Since:2019/2/12
 *@ChangeList:
 */

/*
* 处理MediaCodeC输出队列数据
* */
fun MediaCodec.disposeOutput(bufferInfo: MediaCodec.BufferInfo, defTimeOut: Long,
                             endStream: () -> Unit = {},
                             render: (Int) -> Unit) {
    // 2019/2/9-22:20 获取可用的输出缓存队列
    val outputBufferId = dequeueOutputBuffer(bufferInfo, defTimeOut)
    if (outputBufferId >= 0) {
        // 2019/2/12-22:55 and是位运算 &，转换为二进制进行“与”运算.位数不匹配则都为0
        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            // 2019/2/12-22:59 bufferInfo无可用缓存
            endStream.invoke()
        }
        val doRender = bufferInfo.size != 0
        if (doRender) {
            render.invoke(outputBufferId)
        }
        releaseOutputBuffer(outputBufferId, true)
    }
}