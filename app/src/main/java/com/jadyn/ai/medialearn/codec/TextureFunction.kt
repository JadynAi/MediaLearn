package com.jadyn.ai.medialearn.codec

import android.graphics.SurfaceTexture
import com.jadyn.mediakit.function.checkGlError
import com.jadyn.mediakit.gl.STextureRender

/**
 *@version:
 *@FileDescription:
 *@Author:jing
 *@Since:2018/12/5
 *@ChangeList:
 */
class TextureFunction : SurfaceTexture.OnFrameAvailableListener {

    private val TAG = "SurfaceTextureManager"
    var surfaceTexture: SurfaceTexture? = null
        private set
    
    private var textureRender: STextureRender? = null

    private val frameSyncObject = java.lang.Object()
    private var frameAvailable: Boolean = false

    init {
        textureRender = STextureRender()
        textureRender!!.surfaceCreated()
        val textureId = textureRender!!.textureId
        surfaceTexture = SurfaceTexture(textureId)
        surfaceTexture!!.setOnFrameAvailableListener(this)
    }

    fun release() {
        textureRender = null
        surfaceTexture?.release()
        surfaceTexture = null
    }


    fun awaitNewImage() {
        val timeout_ms: Long = 2500

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
        surfaceTexture!!.updateTexImage()
    }

    fun drawImage() {
        textureRender!!.drawFrame(surfaceTexture!!)
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        synchronized(frameSyncObject) {
            if (frameAvailable) {
                throw RuntimeException("mFrameAvailable already set, frame could be dropped")
            }
            frameAvailable = true
            frameSyncObject.notifyAll()
        }
    }
}