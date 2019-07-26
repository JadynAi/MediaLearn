package com.jadyn.mediakit.gl

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLExt
import android.util.Log
import android.view.Surface

/**
 *@version:
 *@FileDescription: Android平台下，EGL环境搭建，java层代码实现
 *@Author:Jing
 *@Since:2019/3/26
 *@ChangeList:
 */
class EglEnv(private val width: Int, private val height: Int) {
//    private val EGL_RECORDABLE_ANDROID = 0x3142

    private var eglDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface = EGL14.EGL_NO_SURFACE

    private var eglConfig: EGLConfig? = null

    /**
     * 搭建一个EGL环境
     * */
    fun setUpEnv(): EglEnv {
        // 构建一个显示设备
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            checkEglError("can't load EGL display")
        }
        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            checkEglError("EGL initialize failed")
        }
        val attribs = intArrayOf(
                EGL14.EGL_BUFFER_SIZE, 32,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE,
                EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE,
                EGL14.EGL_WINDOW_BIT,
                EGL14.EGL_NONE)
//        val attribs = intArrayOf(EGL14.EGL_RED_SIZE, 8,
//                EGL14.EGL_GREEN_SIZE, 8,
//                EGL14.EGL_BLUE_SIZE, 8,
//                EGL14.EGL_ALPHA_SIZE, 8,
//                EGL14.EGL_RENDERABLE_TYPE, 
//                EGL14.EGL_OPENGL_ES2_BIT,
//                EGL_RECORDABLE_ANDROID, 
//                1,
//                EGL14.EGL_NONE)
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(eglDisplay, attribs, 0, configs,
                        0, configs.size, numConfigs, 0)) {
            checkEglError("EGL choose config failed")
        }
        eglConfig = configs[0]
        // 构建上下文环境
        val attributes = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        // share_context 是否与其他上下文共享OpenGL资源
        eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig,
                EGL14.EGL_NO_CONTEXT, attributes, 0)
        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            checkEglError("EGL create context failed ")
        }
        return this
    }

    fun buildEGLSurface(surface: Surface? = null): EglEnv {
        surface?.apply {
            Log.d("EGLSurface", "build window surface")
            return buildWindowSurface(this)
        }
        Log.d("EGLSurface", "build off screen surface")
        return buildOffScreenSurface()
    }

    /**
     * 创建离线Surface
     * */
    fun buildOffScreenSurface(): EglEnv {
        // EGL 和 OpenGL ES环境搭建完毕，OpenGL输出可以获得。接着是EGL和设备连接
        // 连接工具是：EGLSurface，这是一个FrameBuffer
        val pbufferAttributes = intArrayOf(EGL14.EGL_WIDTH, width, EGL14.EGL_HEIGHT,
                height, EGL14.EGL_NONE)
        eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfig, pbufferAttributes, 0)
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            checkEglError("EGL create Pbuffer surface failed")
        }
        makeCurrent()
        return this
    }

    /**
     * 创建一个可实际显示的windowSurface
     *
     * @param surface 本地设备屏幕
     * */
    fun buildWindowSurface(surface: Surface): EglEnv {
        val format = IntArray(1)
        if (!EGL14.eglGetConfigAttrib(eglDisplay, eglConfig, EGL14.EGL_NATIVE_VISUAL_ID, format, 0)) {
            checkEglError("EGL getConfig attrib failed ")
        }
        if (eglSurface != EGL14.EGL_NO_SURFACE) {
            throw RuntimeException("EGL already config surface")
        }
        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig,
                surface, surfaceAttribs, 0)
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            checkEglError("EGL create window surface failed")
        }
        makeCurrent()
        return this
    }

    /**
     * 为此线程绑定上下文
     * */
    private fun makeCurrent() {
        Log.d(this.javaClass.name, " egl make current ")
        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            checkEglError("EGL make current failed")
        }
    }

    /**
     * @param nsecs 纳秒 10^-9
     * */
    fun setPresentationTime(nsecs: Long) {
        EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, nsecs)
        checkEglError("eglPresentationTimeANDROID")
    }

    /**
     * EGL是双缓冲机制，Back Frame Buffer和Front Frame Buffer，正常绘制目标都是Back Frame Buffer
     * 将绘制完毕的FrameBuffer交换到Front Frame Buffer 并显示出来
     * */
    fun swapBuffers(): Boolean {
        val result = EGL14.eglSwapBuffers(eglDisplay, eglSurface)
        checkEglError("eglSwapBuffers")
        return result
    }

    fun release() {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eglDisplay)
        }
        eglSurface = EGL14.EGL_NO_SURFACE
        eglContext = EGL14.EGL_NO_CONTEXT
        eglDisplay = EGL14.EGL_NO_DISPLAY
    }
}