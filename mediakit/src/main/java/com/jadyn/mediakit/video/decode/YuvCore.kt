package com.jadyn.mediakit.video.decode

import android.media.MediaCodec
import android.util.Log

/**
 *@version:
 *@FileDescription:
 *@Author:jing
 *@Since:2019/2/18
 *@ChangeList:
 */
class YuvCore : DecodeCore() {

    override fun codeToFrame(bufferInfo: MediaCodec.BufferInfo, outputBufferId: Int, outputFrameCount: Int,
                             decoder: MediaCodec): Int {
        if (bufferInfo.size != 0) {
            // YUV输出JPEG。使用Image时，先拿到image数据再releaseOutputBuffer
            val image = decoder.getOutputImage(outputBufferId)
            if (outputFrameCount <= 1) {
                Log.d(TAG, "output Image format ${image.format}: ")
            }
            val fileName = DecoderFormat.JPG.outputFrameFileName(outputDir,
                    outputFrameCount)
            val toJpgSuccess = DecoderFormat.JPG.compressCorrespondingFile(fileName, image)
            image.close()
            decoder.releaseOutputBuffer(outputBufferId, true)
            return if (toJpgSuccess) outputFrameCount else -1
        }
        decoder.releaseOutputBuffer(outputBufferId, true)
        return -1
    }

    override fun release() {

    }
}