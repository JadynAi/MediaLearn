package com.jadyn.mediakit.video.decode

import android.content.ContentValues.TAG
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.util.Log
import android.util.Size
import android.view.Surface
import com.jadyn.mediakit.gl.EglEnv
import com.jadyn.mediakit.gl.Texture2dProgram
import com.jadyn.mediakit.gl.checkGlError
import com.jadyn.mediakit.gl.isSupportPBO
import com.jadyn.mediakit.video.decode.pixg.FBOPixelsGen
import com.jadyn.mediakit.video.decode.pixg.PixelsGen
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 *@version:
 *@FileDescription:
 *@Author:jing
 *@Since:2019/2/18
 *@ChangeList:
 */
class GLCore {

    private lateinit var surface: Surface
    private lateinit var surfaceTexture: SurfaceTexture
    private lateinit var pixelGen: PixelsGen
    private lateinit var size: Size
    private lateinit var eglEnv: EglEnv

    private val texture2dProgram by lazy {
        Texture2dProgram()
    }

    private val semaphore by lazy {
        Semaphore(0)
    }

    fun fkOutputSurface(width: Int, height: Int): Surface? {
        if (::surface.isInitialized) {
            return surface
        }
        size = Size(width, height)
        eglEnv = EglEnv(width, height).setUpEnv().buildOffScreenSurface()
        pixelGen = FBOPixelsGen(size, isSupportPBO())
        surfaceTexture = SurfaceTexture(texture2dProgram.genTextureId())
        surfaceTexture.setOnFrameAvailableListener {
            Log.d(TAG, "new frame available")
            semaphore.release()
        }
        surface = Surface(surfaceTexture)
        return surface
    }

    fun updateTexture(bufferInfo: MediaCodec.BufferInfo, outputBufferId: Int,
                      decoder: MediaCodec): Boolean {
        val doRender = bufferInfo.size != 0
        // CodeC搭配输出Surface时，调用此方法将数据及时渲染到Surface上
        decoder.releaseOutputBuffer(outputBufferId, doRender)
        if (doRender) {
            // 2019/2/14-15:24 必须和surface创建时保持统一线程
            awaitNewImage()
            drawImage()
            return true
        }
        return false
    }

    /*
    * must call after updateTexture return true
    * */
    fun generateFrame() = produceBitmap()

    private fun awaitNewImage() {
        try {
            semaphore.tryAcquire(500, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        checkGlError("before updateTexImage")
        surfaceTexture.updateTexImage()
    }

    /**
     * Draws the data from SurfaceTexture onto the current EGL surface.
     */
    private fun drawImage() {
        texture2dProgram.drawFrame(surfaceTexture)
    }

    private fun produceBitmap(): Bitmap {
        val s = System.currentTimeMillis()
        val bitmap = pixelGen.produceBitmap()
        Log.d(TAG, "produce bitmap cost : ${System.currentTimeMillis() - s}")
        return bitmap
    }

    fun release() {
        if (::pixelGen.isInitialized) {
            pixelGen.release()
        }
        if (::surface.isInitialized) {
            surface.release()
            surfaceTexture.release()
        }
        if (::eglEnv.isInitialized) {
            eglEnv.release()
        }
    }
}