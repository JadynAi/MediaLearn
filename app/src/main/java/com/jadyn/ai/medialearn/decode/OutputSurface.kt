package com.jadyn.ai.medialearn.decode

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.util.Log
import android.view.Surface
import com.jadyn.ai.medialearn.codec.checkEglError
import com.jadyn.ai.medialearn.codec.checkGlError
import com.jadyn.ai.medialearn.gl.STextureRender
import javax.microedition.khronos.egl.*

/**
 *@version:
 *@FileDescription:
 *@Author:jing
 *@Since:2019/2/12
 *@ChangeList:
 */
internal class OutputSurface(width: Int = 0, height: Int = 0) : SurfaceTexture.OnFrameAvailableListener {

    private val TAG = "OutputSurface"
    private val VERBOSE = false
    private val EGL_OPENGL_ES2_BIT = 4

    private var mEGL: EGL10? = null
    private var mEGLDisplay: EGLDisplay? = null
    private var mEGLContext: EGLContext? = null
    private var mEGLSurface: EGLSurface? = null
    private var surfaceTexture: SurfaceTexture? = null


    var surface: Surface? = null
        private set

    private val frameSyncObject = java.lang.Object()
    private var frameAvailable: Boolean = false
    private var mTextureRender: STextureRender? = null

    init {
        if (width <= 0 || height <= 0) {
            setup()
        } else {
            eglSetup(width, height)
            makeCurrent()
            setup()
        }
    }

    private fun setup() {
        mTextureRender = STextureRender()
        mTextureRender!!.surfaceCreated()
        
        if (VERBOSE) Log.d(TAG, "textureID=" + mTextureRender!!.textureId)
        surfaceTexture = SurfaceTexture(mTextureRender!!.textureId)
        
        surfaceTexture!!.setOnFrameAvailableListener(this)
        surface = Surface(surfaceTexture)
    }

    /**
     * Prepares EGL.  We want a GLES 2.0 context and a surface that supports pbuffer.
     */
    private fun eglSetup(width: Int, height: Int) {
        mEGL = EGLContext.getEGL() as EGL10
        mEGLDisplay = mEGL!!.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        if (!mEGL!!.eglInitialize(mEGLDisplay, null)) {
            throw RuntimeException("unable to initialize EGL10")
        }
        // Configure EGL for pbuffer and OpenGL ES 2.0.  We want enough RGB bits
        // to be able to tell if the frame is reasonable.
        val attribList = intArrayOf(EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8, EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT, EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL10.EGL_NONE)
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!mEGL!!.eglChooseConfig(mEGLDisplay, attribList, configs, 1, numConfigs)) {
            throw RuntimeException("unable to find RGB888+pbuffer EGL config")
        }
        // Configure context for OpenGL ES 2.0.
        val attrib_list = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE)
        mEGLContext = mEGL!!.eglCreateContext(mEGLDisplay, configs[0], EGL10.EGL_NO_CONTEXT,
                attrib_list)
        checkEglError("eglCreateContext")
        if (mEGLContext == null) {
            throw RuntimeException("null context")
        }
        // Create a pbuffer surface.  By using this for output, we can use glReadPixels
        // to test values in the output.
        val surfaceAttribs = intArrayOf(EGL10.EGL_WIDTH, width, EGL10.EGL_HEIGHT, height, EGL10.EGL_NONE)
        mEGLSurface = mEGL!!.eglCreatePbufferSurface(mEGLDisplay, configs[0], surfaceAttribs)
        checkEglError("eglCreatePbufferSurface")
        if (mEGLSurface == null) {
            throw RuntimeException("surface was null")
        }
    }

    /**
     * Discard all resources held by this class, notably the EGL context.
     */
    fun release() {
        if (mEGL != null) {
            if (mEGL!!.eglGetCurrentContext() == mEGLContext) {
                // Clear the current context and surface to ensure they are discarded immediately.
                mEGL!!.eglMakeCurrent(mEGLDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE,
                        EGL10.EGL_NO_CONTEXT)
            }
            mEGL!!.eglDestroySurface(mEGLDisplay, mEGLSurface)
            mEGL!!.eglDestroyContext(mEGLDisplay, mEGLContext)
            //mEGL.eglTerminate(mEGLDisplay);
        }
        surface!!.release()
        mEGLDisplay = null
        mEGLContext = null
        mEGLSurface = null
        mEGL = null
        mTextureRender = null
        surface = null
        surfaceTexture = null
    }

    /**
     * Makes our EGL context and surface current.
     */
    fun makeCurrent() {
        if (mEGL == null) {
            throw RuntimeException("not configured for makeCurrent")
        }
        checkEglError("before makeCurrent")
        if (!mEGL!!.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
            throw RuntimeException("eglMakeCurrent failed")
        }
    }

    /**
     * Replaces the fragment shader.
     */
    fun changeFragmentShader(fragmentShader: String) {
        mTextureRender!!.changeFragmentShader(fragmentShader)
    }

    /**
     * Latches the next buffer into the texture.  Must be called from the thread that created
     * the OutputSurface object, after the onFrameAvailable callback has signaled that new
     * data is available.
     */
    fun awaitNewImage() {
        val TIMEOUT_MS = 500L
        
        synchronized(frameSyncObject) {
            while (!frameAvailable) {
                try {
                    frameSyncObject.wait(TIMEOUT_MS)
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

    /**
     * Draws the data from SurfaceTexture onto the current EGL surface.
     */
    fun drawImage() {
        mTextureRender!!.drawFrame(surfaceTexture!!)
    }

    override fun onFrameAvailable(st: SurfaceTexture) {
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