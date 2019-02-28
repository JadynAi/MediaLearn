package com.jadyn.mediakit.function

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.Image
import android.media.MediaCodecInfo
import android.opengl.EGL14
import android.opengl.GLES20
import android.os.Build
import android.support.annotation.IntRange
import android.util.Log
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest


/**
 *@version:
 *@FileDescription: 常见编解码辅助工具类
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

fun debugShowSupportColorFormat(caps: MediaCodecInfo.CodecCapabilities) {
    caps.colorFormats.forEach {
        Log.d("colorFormat", "$it : \t")
    }
}

fun MediaCodecInfo.CodecCapabilities.isSupportColorFormat(colorForamt: Int): Boolean {
    return this.colorFormats.contains(colorForamt)
}

//------------------Decode Video----------------------
fun Image.isSupportFormat(): Boolean {
    return when (format) {
        ImageFormat.YUV_420_888, ImageFormat.NV21, ImageFormat.YV12 -> true
        else -> false
    }
}

fun Image.getDataByte(): ByteArray {
    val format = format
    if (!isSupportFormat()) {
        throw RuntimeException("image can not support format is $format")
    }
    // 指定了图片的有效区域，只有这个Rect内的像素才是有效的
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
                channelOffset = width * height + 1
                outputStride = 2
            }
            2 -> {
                channelOffset = width * height
                outputStride = 2
            }
        }

        // 此时得到的ByteBuffer的position指向末端
        val buffer = planes[i].buffer
        //  行跨距
        val rowStride = planes[i].rowStride
        // 行内颜色值间隔，真实间隔值为此值减一
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

fun Bitmap.saveFrame(fileName: String, @IntRange(from = 1, to = 100) quality: Int = 100) {
    var bos: BufferedOutputStream? = null
    try {
        File(fileName).makeParent()
        bos = BufferedOutputStream(FileOutputStream(fileName))
        compress(Bitmap.CompressFormat.JPEG, quality, bos)
        recycle()
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        bos?.close()
    }
}

fun md5(str: String): String {
    val digest = MessageDigest.getInstance("MD5")
    val result = digest.digest(str.toByteArray())
    //没转16进制之前是16位
    println("result${result.size}")
    //转成16进制后是32字节
    return toHex(result)
}

fun toHex(byteArray: ByteArray): String {
    //转成16进制后是32字节
    return with(StringBuilder()) {
        byteArray.forEach {
            val hex = it.toInt() and (0xFF)
            val hexStr = Integer.toHexString(hex)
            if (hexStr.length == 1) {
                append("0").append(hexStr)
            } else {
                append(hexStr)
            }
        }
        toString()
    }
}

fun File.makeParent() {
    parentFile?.apply {
        if (!exists()) {
            this.mkdirs()
        }
    }
}

fun Bitmap.getByteSize(): Int {
    if (isRecycled) {
        return 0
    }
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        try {
            allocationByteCount
        } catch (e: Exception) {
            0
        }
    } else {
        height * rowBytes
    }
}