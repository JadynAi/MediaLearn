package com.jadyn.ai.medialearn.codec

import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.media.Image
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.opengl.EGL14
import android.opengl.GLES20
import android.util.Log
import com.jadyn.ai.medialearn.decode.ColorFormat
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.nio.ByteBuffer


/**
 *@version:
 *@FileDescription: MediaCodeC辅助工具类
 *@Author:jing
 *@Since:2018/12/5
 *@ChangeList:
 */
fun checkEglError(msg: String) {
    val error: Int = EGL14.eglGetError()
    if (error != EGL14.EGL_SUCCESS) {
        throw RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error))
    }
}

fun checkGlError(op: String) {
    var error: Int = GLES20.glGetError()
    while (error != GLES20.GL_NO_ERROR) {
        error = GLES20.glGetError()
        throw RuntimeException("$op: glError $error")
    }
}

fun computePresentationTimeNsec(frameIndex: Int, frameRate: Int): Long {
    val ONE_BILLION: Long = 1000000000
    return frameIndex * ONE_BILLION / frameRate
}

fun checkLocation(location: Int, label: String) {
    if (location < 0) {
        throw RuntimeException("Unable to locate '$label' in program")
    }
}

fun MediaExtractor.selectVideoTrack(): Int {
    val numTracks = trackCount
    for (i in 0 until numTracks) {
        val format = getTrackFormat(i)
        val mime = format.getString(MediaFormat.KEY_MIME)
        if (mime.startsWith("video/")) {
            return i
        }
    }
    return -1
}

fun debugShowSupportColorFormat(caps: MediaCodecInfo.CodecCapabilities) {
    caps.colorFormats.forEach {
        Log.d("colorFormat", "$it : \t")
    }
}

fun MediaCodecInfo.CodecCapabilities.isSupportColorFormat(colorForamt: Int): Boolean {
    return this.colorFormats.contains(colorForamt)
}

//------------------Decode Video----------------------


fun compressToJpeg(fileName: String, image: Image) {
    val fileOutputStream: FileOutputStream
    try {
        fileOutputStream = FileOutputStream(fileName)
    } catch (e: FileNotFoundException) {
        throw RuntimeException("compress JPG can not create available file ")
    }
    val rect = image.cropRect
    val yuvImage = YuvImage(image.getDataByte(), ImageFormat.NV21, rect.width(), rect.height(), null)
    yuvImage.compressToJpeg(rect, 100, fileOutputStream)
    fileOutputStream.close()
}

fun Image.isSupportFormat(): Boolean {
    return when (format) {
        ImageFormat.YUV_420_888, ImageFormat.NV21, ImageFormat.YV12 -> true
        else -> false
    }
}

fun Image.getDataByte(colorForamt: ColorFormat = ColorFormat.NV21): ByteArray {
    val format = format
    if (!isSupportFormat()) {
        throw RuntimeException("image can not support format is $format")
    }
    // 2019/2/10-15:40 指定了图片的有效区域，只有这个Rect内的像素才是有效的
    val rect = cropRect
    val width = rect.width()
    val height = rect.height()
    val planes = planes
    val data = ByteArray(width * height * ImageFormat.getBitsPerPixel(format) / 8)
    val rowData = ByteArray(planes[0].rowStride)

    var channelOffset = 0
    var outputStride = 1
    for (i in 0 until planes.size) {
        when (i) {
            0 -> {
                channelOffset = 0
                outputStride = 1
            }
            1 -> {
                if (colorForamt == ColorFormat.I420) {
                    channelOffset = width * height
                    outputStride = 1
                } else if (colorForamt == ColorFormat.NV21) {
                    channelOffset = width * height + 1
                    outputStride = 2
                }
            }
            2 -> {
                if (colorForamt == ColorFormat.I420) {
                    channelOffset = (width * height * 1.25f).toInt()
                    outputStride = 1
                } else if (colorForamt == ColorFormat.NV21) {
                    channelOffset = width * height
                    outputStride = 2
                }
            }
        }

        // 2019/2/11-23:14 此时得到的ByteBuffer的position指向末端
        val buffer = planes[i].buffer
        // 2019/2/11-23:17 这一行的byte数，像素数
        val rowStride = planes[i].rowStride
        // 2019/2/11-23:17 行内颜色值间隔，真实间隔值为此值减一
        val pixelStride = planes[i].pixelStride

        val TAG = "getDataByte"

        Log.d(TAG, "planes index is  $i")
        Log.d(TAG, "pixelStride $pixelStride")
        Log.d(TAG, "rowStride $rowStride")
        Log.d(TAG, "width $width")
        Log.d(TAG, "height $height")
        Log.d(TAG, "buffer size " + buffer.remaining())

        val shift = if (i == 0) 0 else 1
        val w = width.shr(shift)
        val h = height.shr(shift)
        buffer.position(rowStride * (rect.top.shr(shift)) + pixelStride +
                (rect.left.shr(shift)))
        for (row in 0 until h) {
            var length: Int
            if (pixelStride == 1 && outputStride == 1) {
                length = w
                // 2019/2/11-23:05 buffer有时候遗留的长度，小于length就会报错
                buffer.getNoException(data, channelOffset, length)
                channelOffset += length
            } else {
                length = (w - 1) * pixelStride + 1
                buffer.getNoException(rowData, 0, length)
                for (col in 0 until w) {
                    data[channelOffset] = rowData[col * pixelStride]
                    channelOffset += outputStride
                }
            }

            if (row < h - 1) {
                buffer.position(buffer.position() + rowStride - length)
            }
        }
    }
    return data
}

/*
* 当输入length大于remaining时候，不抛出异常
* */
fun ByteBuffer.getNoException(dst: ByteArray, offset: Int, length: Int): ByteBuffer? {
    val realLength = if (length > remaining()) remaining() else length
    return get(dst, offset, realLength)
}