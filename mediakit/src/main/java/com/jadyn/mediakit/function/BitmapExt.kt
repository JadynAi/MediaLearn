package com.jadyn.mediakit.function

import android.graphics.Bitmap
import android.util.Log
import android.util.Size
import java.io.*
import java.nio.ByteBuffer
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.system.measureTimeMillis

/**
 *@version:
 *@FileDescription:
 *@Author:Jing
 *@Since:2019-06-28
 *@ChangeList:
 */

/**
 * save bitmap RGB byteArray.
 * @param dir : the file will saved parent folder
 * @param key : To identify the uniqueness of this Bitmap
 * */

fun Bitmap.byteArray(): ByteArray {
    val byteBuffer = ByteBuffer.allocate(byteCount)
    copyPixelsToBuffer(byteBuffer)
    return byteBuffer.array()
}

class BitmapLru(val size: Size, val data: ByteArray)

fun Bitmap.lruCache(): BitmapLru {
    val array = byteArray()

    val out = ByteArrayOutputStream()
    val zip = GZIPOutputStream(out)
    zip.write(array)
    zip.close()
    // 在这里zip要及时关闭，否则读取压缩数据时会出现异常
    val data = out.toByteArray()
    out.close()
    return BitmapLru(Size(width, height), data)
}

fun BitmapLru?.lruToBitmap(): Bitmap? {
    this?.apply {
        val s = System.currentTimeMillis()
        val inb = ByteArrayInputStream(data)
        val zip = GZIPInputStream(inb)

        val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(zip.readBytes()))

        zip.close()
        inb.close()
        Log.d("lruToBitmap", "lru to bitmap cost :${System.currentTimeMillis() - s} ")
        return bitmap
    }
    return null
}

fun Bitmap.saveAll(dir: String) {
    val file = File(dir)
    if (file.exists()) {
        return
    }
    val byteArray = byteArray()
    if (!file.parentFile.exists()) {
        file.parentFile.mkdirs()
    }
    file.writeBytes(byteArray)
}

fun getBitmapAll(dir: String, size: Size): Bitmap? {
    val f = File(dir)
    if (!f.exists()) {
        return null
    }
    val bytes = f.readBytes()
    val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
    bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(bytes))
    return bitmap
}

fun Bitmap.saveUndamaged(dir: String) {
    val file = File(dir)
    if (file.exists()) {
        return
    }
    val byteArray = byteArray()
    if (!file.parentFile.exists()) {
        file.parentFile.mkdirs()
    }
    val fileOut = FileOutputStream(file)
    val zipOutputStream = GZIPOutputStream(fileOut)
    zipOutputStream.write(byteArray)
    zipOutputStream.close()
    fileOut.close()
}

fun getUndamagedBitmap(dir: String, size: Size): Bitmap? {
    var bitmap: Bitmap? = null
    val cost = measureTimeMillis {
        val f = File(dir)
        if (!f.exists()) {
            return null
        }
        val file = FileInputStream(f)
        val zip = GZIPInputStream(file)
        bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        val s = System.currentTimeMillis()
        val readBytes = zip.readBytes()
        Log.d("Cost", "zip read bytes cost :${System.currentTimeMillis() - s}")
        bitmap!!.copyPixelsFromBuffer(ByteBuffer.wrap(readBytes))
        Log.d("Cost", "bitmap copy cost :${System.currentTimeMillis() - s}")
        zip.close()
        file.close()
    }
    Log.d("getUndamagedBitmap", "getUndamagedBitmap dir $dir cost $cost")
    return bitmap
}