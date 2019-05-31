package com.jadyn.mediakit.video.encode

import android.graphics.SurfaceTexture
import android.util.Log
import android.view.Surface
import com.jadyn.mediakit.gl.*

/**
 *@version:
 *@FileDescription: Surface OpenGL 编码视频帧核心类
 *@Author:Jing
 *@Since:2019-05-21
 *@ChangeList:
 */
class SurfaceEncodeCore(private val width: Int, private val height: Int) {
    private val TAG = "SurfaceEncodeCore"
    private val eglEnv by lazy {
        EglEnv(width, height)
    }

    private var surfaceTexture: SurfaceTexture? = null

    private val encodeProgram by lazy {
        SurfaceProgram()
    }

    private val frameSyncObject = Object()
    private var frameAvailable: Boolean = false

    fun buildEGLSurface(surface: Surface): SurfaceTexture {
        Log.d(TAG, "build egl thread: ${Thread.currentThread().name}")
        // 构建EGL环境
        eglEnv.setUpEnv().buildWindowSurface(surface)
        val textureId = encodeProgram.genTextureId()
        surfaceTexture = SurfaceTexture(textureId)
        surfaceTexture!!.setDefaultBufferSize(width, height)
        // 监听获取新的图像帧
        surfaceTexture!!.setOnFrameAvailableListener {
            synchronized(frameSyncObject) {
                if (frameAvailable) {
                    throw RuntimeException("mFrameAvailable already set, frame could be dropped")
                }
                frameAvailable = true
                frameSyncObject.notifyAll()
            }
        }
        return surfaceTexture!!
    }

    fun draw() {
        awaitNewImage()
        encodeProgram.drawFrame(surfaceTexture!!)
    }

    fun swapData(nesc: Long) {
        eglEnv.setPresentationTime(nesc)
        eglEnv.swapBuffers()
    }

    private fun awaitNewImage() {
        val timeoutMs: Long = 2500
        synchronized(frameSyncObject) {
            while (!frameAvailable) {
                try {
                    frameSyncObject.wait(timeoutMs)
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
        surfaceTexture!!.updateTexImage()
    }

    fun release() {
        eglEnv.release()
        surfaceTexture?.release()
        surfaceTexture = null
    }
}

/**
 * 使用 作为数据传递介质 Surface 绘制
 *
 * 每一帧的数据从SurfaceTexture 中获取
 * */
class SurfaceProgram {
    private var textureId: Int = 0

    private val textureDraw: TextureDraw

    init {
        Log.d(this.javaClass.name, " create texture 2d program ")
        val program = createCommoneProgram()
        textureDraw = TextureDraw(program)
    }

    fun genTextureId(): Int {
        textureId = buildTextureId()
        return textureId
    }

    fun drawFrame(st: SurfaceTexture) {
        textureDraw.drawFromSurfaceTexture(st, textureId)
    }
}