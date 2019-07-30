package com.jadyn.mediakit.video.encode

import android.graphics.Bitmap
import android.util.Size
import android.view.Surface
import com.jadyn.mediakit.gl.EglEnv

/**
 *@version:
 *@FileDescription: OpenGL编码核心
 *@Author:Jing
 *@Since:2019/3/29
 *@ChangeList:
 */
class GLEncodeCore(private val width: Int, private val height: Int) {

    private val eglEnv by lazy {
        EglEnv(width, height)
    }

    private val encodeProgram by lazy {
        EncodeProgram(Size(width, height))
    }

    fun buildEGLSurface(surface: Surface) {
        // 构建EGL环境
        eglEnv.setUpEnv().buildWindowSurface(surface)
        encodeProgram.build()
    }
    
    /**
     * this function must call by after buildEGLSurface
     * */
    fun getTextureId() = encodeProgram.textureID

    /**
     *
     * @param presentTime 纳秒，当前帧时间
     * */
    fun drainFrame(b: Bitmap, presentTime: Long) {
        encodeProgram.renderBitmap(b)
        // 给渲染的这一帧设置一个时间戳
        eglEnv.setPresentationTime(presentTime)
        eglEnv.swapBuffers()
    }

    fun release() {
        eglEnv.release()
        encodeProgram.release()
    }
}