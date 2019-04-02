package com.jadyn.mediakit.video.decode

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.opengl.GLES20
import android.util.Log
import android.util.Size
import android.view.Surface
import com.jadyn.mediakit.function.checkGlError
import com.jadyn.mediakit.function.saveFrame
import com.jadyn.mediakit.gl.EglEnv
import com.jadyn.mediakit.gl.Texture2dProgram
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 *@version:
 *@FileDescription:
 *@Author:jing
 *@Since:2019/2/18
 *@ChangeList:
 */
class GLCore : DecodeCore() {

    private lateinit var surface: Surface
    private lateinit var surfaceTexture: SurfaceTexture
    private lateinit var pixelBuf: ByteBuffer

    private lateinit var size: Size

    private val texture2dProgram by lazy {
        Texture2dProgram()
    }

    private val frameSyncObject = java.lang.Object()
    private var frameAvailable: Boolean = false

    override fun fkOutputSurface(width: Int, height: Int): Surface? {
        if (::surface.isInitialized) {
            return surface
        }
        size = Size(width, height)
        EglEnv(width, height).setUpEnv().buildBackgroundSurface()
        surfaceTexture = SurfaceTexture(texture2dProgram.genTextureId())
        surfaceTexture.setOnFrameAvailableListener {
            Log.d(TAG, "new frame available")
            synchronized(frameSyncObject) {
                if (frameAvailable) {
                    throw RuntimeException("mFrameAvailable already set, frame could be dropped")
                }
                frameAvailable = true
                frameSyncObject.notifyAll()
            }
        }
        surface = Surface(surfaceTexture)
        pixelBuf = ByteBuffer.allocate(width * height * 4)
        pixelBuf.order(ByteOrder.LITTLE_ENDIAN)
        return surface
    }

    override fun codeToFrame(bufferInfo: MediaCodec.BufferInfo, outputBufferId: Int, outputFrameCount: Int,
                             decoder: MediaCodec): Int {
        val doRender = bufferInfo.size != 0
        // CodeC搭配输出Surface时，调用此方法将数据及时渲染到Surface上
        decoder.releaseOutputBuffer(outputBufferId, doRender)
        if (doRender) {
            // 2019/2/14-15:24 必须和surface创建时保持统一线程
            awaitNewImage()
            drawImage(true)

            val file = File(outputDir, String.format("frame-%02d.jpg", outputFrameCount))
            saveFrame(file.toString())
            return outputFrameCount
        }
        return -1
    }

    override fun codeFrameBitmap(bufferInfo: MediaCodec.BufferInfo, outputBufferId: Int,
                                 decoder: MediaCodec, ob: (Bitmap) -> Unit) {
        if (updateTexture(bufferInfo, outputBufferId, decoder)) {
            ob.invoke(produceBitmap())
        }
    }

    fun updateTexture(bufferInfo: MediaCodec.BufferInfo, outputBufferId: Int,
                      decoder: MediaCodec): Boolean {
        val doRender = bufferInfo.size != 0
        // CodeC搭配输出Surface时，调用此方法将数据及时渲染到Surface上
        decoder.releaseOutputBuffer(outputBufferId, doRender)
        if (doRender) {
            // 2019/2/14-15:24 必须和surface创建时保持统一线程
            awaitNewImage()
            drawImage(true)
            return true
        }
        return false
    }

    /*
    * must call after updateTexture return true
    * */
    fun generateFrame() = produceBitmap()

    private fun awaitNewImage() {
        val timeout_ms = 500L
        synchronized(frameSyncObject) {
            while (!frameAvailable) {
                try {
                    frameSyncObject.wait(timeout_ms)
                    if (!frameAvailable) {
                        throw RuntimeException("Camera frame wait timed out")
                    }
                } catch (e: InterruptedException) {
                    throw RuntimeException(e)
                }
            }
            frameAvailable = false
        }
        checkGlError("before updateTexImage")
        surfaceTexture.updateTexImage()
    }

    /**
     * Draws the data from SurfaceTexture onto the current EGL surface.
     */
    private fun drawImage(invert: Boolean = false) {
        texture2dProgram.drawFrame(surfaceTexture, invert)
    }

    private fun produceBitmap(): Bitmap {
        pixelBuf.rewind()
        GLES20.glReadPixels(0, 0, size.width, size.height, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, pixelBuf)
        val bmp = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        pixelBuf.rewind()
        bmp.copyPixelsFromBuffer(pixelBuf)
        return bmp
    }

    private fun saveFrame(fileName: String) {
        produceBitmap().saveFrame(fileName)
    }

    override fun release() {
        if (::surface.isInitialized) {
            surface.release()
            surfaceTexture.release()
        }
    }
}