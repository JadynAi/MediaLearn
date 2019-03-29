package com.jadyn.mediakit.gl

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.util.Log
import android.util.Size
import android.view.Surface
import com.jadyn.mediakit.function.checkEglError
import com.jadyn.mediakit.function.checkGlError
import com.jadyn.mediakit.function.saveFrame
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 *@version:
 *@FileDescription:
 *@Author:jing
 *@Since:2019/2/12
 *@ChangeList:
 */
class OutputSurface(private val width: Int, private val height: Int) {

    constructor(size: Size) : this(size.width, size.height)

    private val TAG = "OutputSurface"
    private val VERBOSE = true

    private var mEGLDisplay = EGL14.EGL_NO_DISPLAY
    private var mEGLContext = EGL14.EGL_NO_CONTEXT
    private var mEGLSurface = EGL14.EGL_NO_SURFACE

    private lateinit var surfaceTexture: SurfaceTexture

    val surface by lazy {
        Surface(surfaceTexture)
    }

    private val frameSyncObject = java.lang.Object()
    private var frameAvailable: Boolean = false

    private lateinit var textureRender: STextureRender

    private var pixelBuf: ByteBuffer

    init {
        if (width <= 0 || height <= 0) {
            throw IllegalArgumentException("width and height must not zero")
        }

        eglSetup()
        makeCurrent()
        setup()
        pixelBuf = ByteBuffer.allocate(width * height * 4)
        pixelBuf.order(ByteOrder.LITTLE_ENDIAN)
    }

    private fun setup() {
        textureRender = STextureRender()
        textureRender.surfaceCreated()

        if (VERBOSE) Log.d(TAG, "textureID=" + textureRender.textureId)
        surfaceTexture = SurfaceTexture(textureRender.textureId)

        surfaceTexture.setOnFrameAvailableListener {
            if (VERBOSE) Log.d(TAG, "new frame available")
            synchronized(frameSyncObject) {
                if (frameAvailable) {
                    throw RuntimeException("mFrameAvailable already set, frame could be dropped")
                }
                frameAvailable = true
                frameSyncObject.notifyAll()
            }
        }
    }

    /**
     * 创建一个EGL上下文
     * */
    private fun eglSetup() {
        // 封装系统物理屏幕的对象，作为OpenGl Es 渲染的目标
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (mEGLDisplay === EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("unable to get EGL14 display")
        }
        val version = IntArray(2)
        // 初始化显示设备
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            mEGLDisplay = null
            throw RuntimeException("unable to initialize EGL14")
        }
        
        // 此时可以将OpenGl Es的输出和设备屏幕桥接起来了，但是需要设置如下配置
        val attributeList = intArrayOf(EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE,
                EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE,
                EGL14.EGL_PBUFFER_BIT, EGL14.EGL_NONE)
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(mEGLDisplay, attributeList, 0,
                        configs, 0, configs.size,
                        numConfigs, 0)) {
            throw RuntimeException("unable to find RGB888+recordable ES2 EGL config")
        }

        // Configure context for OpenGL ES 2.0.构建上下文环境
        val attribute_list = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        // 第三个参数是，和其他上下文EGLContext共享OpenGL资源
        mEGLContext = EGL14.eglCreateContext(mEGLDisplay, configs[0], EGL14.EGL_NO_CONTEXT,
                attribute_list, 0)
        checkEglError("eglCreateContext")
        if (mEGLContext == null) {
            throw RuntimeException("null context")
        }

        // Create a pbuffer surface。将EGL和设备屏幕连接，让OpenGL的输出可以渲染到设备屏幕
        val surfaceAttribs = intArrayOf(EGL14.EGL_WIDTH, width, EGL14.EGL_HEIGHT,
                height, EGL14.EGL_NONE)
        // eglCreateWindowSurface可实际显示，eglCreatePbufferSurface创建一个OffScreen(画面外，后台)的Surface
        mEGLSurface = EGL14.eglCreatePbufferSurface(mEGLDisplay, configs[0], surfaceAttribs, 0)
        checkEglError("eglCreatePbufferSurface")
        if (mEGLSurface == null) {
            throw RuntimeException("surface was null")
        }
    }

    fun release() {
        if (mEGLDisplay !== EGL14.EGL_NO_DISPLAY) {
            // 销毁也在该线程，先销毁设备，再销毁上下文
            EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface)
            EGL14.eglDestroyContext(mEGLDisplay, mEGLContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(mEGLDisplay)
        }
        mEGLDisplay = EGL14.EGL_NO_DISPLAY
        mEGLContext = EGL14.EGL_NO_CONTEXT
        mEGLSurface = EGL14.EGL_NO_SURFACE

        surface.release()
        surfaceTexture.release()
    }

    /**
     * 为该线程绑定Surface和Context
     * */
    fun makeCurrent() {
        if (!EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
            throw RuntimeException("eglMakeCurrent failed")
        }
    }

    fun awaitNewImage() {
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
    fun drawImage(invert: Boolean = false) {
        textureRender.drawFrame(surfaceTexture, invert)
    }

    fun saveFrame(fileName: String) {
        produceBitmap().saveFrame(fileName)
    }

    fun produceBitmap(): Bitmap {
        pixelBuf.rewind()
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuf)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        pixelBuf.rewind()
        bmp.copyPixelsFromBuffer(pixelBuf)
        return bmp
    }

}