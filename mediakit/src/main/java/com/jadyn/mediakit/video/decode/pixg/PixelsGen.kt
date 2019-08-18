package com.jadyn.mediakit.video.decode.pixg

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.ImageReader
import android.opengl.GLES20
import android.opengl.GLES30
import android.util.Log
import android.util.Size
import android.view.Surface
import com.jadyn.mediakit.gl.GLJni
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 *@version:
 *@FileDescription: 像素拷贝
 *@Author:Jing
 *@Since:2019-07-16
 *@ChangeList:
 */
interface PixelsGen {

    fun inputSurface(): Surface? {
        return null
    }

    fun produceBitmap(): Bitmap

    fun release()
}

class ImageReaderPixelsGen(private val size: Size) : PixelsGen {
    private val imageReader by lazy {
        ImageReader.newInstance(size.width, size.height, PixelFormat.RGBA_8888, 3)
    }

    override fun inputSurface(): Surface? {
        return imageReader.surface
    }

    override fun produceBitmap(): Bitmap {
        return Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
    }

    override fun release() {
        imageReader.close()
    }
}

/**
 *
 * */
class FBOPixelsGen(private val size: Size,
                   private val usePbo: Boolean = true) : PixelsGen {
    private val TAG = "FBOPixelsGen"

    private val pixelBuf by lazy {
        // ARGB——8888，Each pixel is stored on 4 bytes
        val b = ByteBuffer.allocate(
                size.width * size.height * 4).order(ByteOrder.LITTLE_ENDIAN)
        b
    }

    private val PBO_COUNT = 2
    private val pboIds by lazy {
        IntArray(PBO_COUNT)
    }
    private var index = 0
    private var nextIndex = 1

    init {
        if (usePbo) {
            val s = size.width * size.height * 4
            GLES30.glGenBuffers(PBO_COUNT, pboIds, 0)
            GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, pboIds[0])
            // 传null就是分配空间
            GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, s, null, GLES30.GL_STATIC_READ)

            GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, pboIds[1])
            GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, s, null, GLES30.GL_STATIC_READ)

            GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0)
        }
    }

    override fun produceBitmap(): Bitmap {
        if (usePbo) {
            return pboReadPixels()
        }
        return fboReadPixels()
    }

    /**
     * FBO模式，默认缓冲区
     * */
    private fun fboReadPixels(): Bitmap {
        Log.d(TAG, "read FBO: ")
        pixelBuf.rewind()
        GLES20.glReadPixels(0, 0, size.width, size.height, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, pixelBuf)
        val bmp = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        pixelBuf.rewind()
        bmp.copyPixelsFromBuffer(pixelBuf)
        return bmp
    }

    private fun pboReadPixels(): Bitmap {
        Log.d(TAG, "PBO read: ")
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, pboIds[index])
        // 2019-08-18-14:29 数据复制到缓冲区
        GLJni.glReadPixels(0, 0, size.width, size.height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE)

        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, pboIds[nextIndex])

        val data = GLES30.glMapBufferRange(GLES30.GL_PIXEL_PACK_BUFFER,
                0, size.width * size.height * 4, GLES30.GL_MAP_READ_BIT) as ByteBuffer
        //解除映射
        GLES30.glUnmapBuffer(GLES30.GL_PIXEL_PACK_BUFFER)
        //解除绑定PBO
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, GLES30.GL_NONE)

        //交换索引
        index = (index + 1) % PBO_COUNT
        nextIndex = (nextIndex + 1) % PBO_COUNT
        val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(data)
        return bitmap
    }

    override fun release() {
        if (usePbo) {
            GLES30.glDeleteBuffers(pboIds.size, pboIds, 0)
        }
    }
}