package com.jadyn.mediakit.video.decode

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.media.Image
import android.media.MediaCodec
import android.util.Log
import com.jadyn.mediakit.function.getDataByte
import java.io.FileNotFoundException
import java.io.FileOutputStream

/**
 *@version:
 *@FileDescription:
 *@Author:jing
 *@Since:2019/2/18
 *@ChangeList:
 */
class YuvCore : DecodeCore() {

    override fun codeFrameBitmap(bufferInfo: MediaCodec.BufferInfo, outputBufferId: Int,
                                 decoder: MediaCodec, ob: (Bitmap) -> Unit) {
    }

    override fun codeToFrame(bufferInfo: MediaCodec.BufferInfo, outputBufferId: Int, outputFrameCount: Int,
                             decoder: MediaCodec): Int {
        if (bufferInfo.size != 0) {
            // YUV输出JPEG。使用Image时，先拿到image数据再releaseOutputBuffer
            decoder.getOutputBuffer(outputBufferId)
            val image = decoder.getOutputImage(outputBufferId)
            if (outputFrameCount <= 1) {
                Log.d(TAG, "output Image format ${image.format}: ")
            }
            val fileName = DecoderFormat.JPG.outputFrameFileName(outputDir,
                    outputFrameCount)
            val toJpgSuccess = DecoderFormat.JPG.compressCorrespondingFile(fileName, image)
            image?.close()
            decoder.releaseOutputBuffer(outputBufferId, true)
            return if (toJpgSuccess) outputFrameCount else -1
        }
        decoder.releaseOutputBuffer(outputBufferId, true)
        return -1
    }

    override fun release() {

    }
}

fun compressToJpeg(fileName: String, image: Image): Boolean {
    val fileOutputStream: FileOutputStream
    try {
        fileOutputStream = FileOutputStream(fileName)
    } catch (e: FileNotFoundException) {
        throw RuntimeException("compress JPG can not create available file ")
    }
    val rect = image.cropRect
    val yuvImage = YuvImage(image.getDataByte(), ImageFormat.NV21, rect.width(), rect.height(), null)
    val success = yuvImage.compressToJpeg(rect, 100, fileOutputStream)
    fileOutputStream.close()
    return success
}