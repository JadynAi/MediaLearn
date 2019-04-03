package com.jadyn.mediakit.video.encode

import android.graphics.Bitmap
import android.util.Size
import android.view.Surface
import com.jadyn.mediakit.gl.EglEnv
import com.jadyn.mediakit.gl.Texture2dProgram

/**
 *@version:
 *@FileDescription: OpenGL编码核心
 *@Author:Jing
 *@Since:2019/3/29
 *@ChangeList:
 */
class GLEncodeCore(private val width: Int, private val height: Int) {

    private val texture2dProgram by lazy {
        Texture2dProgram()
    }

    private var textureId: Int = 0

    private val eglEnv by lazy {
        EglEnv(width, height)
    }

    fun buildEGLSurface(surface: Surface) {
        // 构建EGL环境
        eglEnv.setUpEnv().buildWindowSurface(surface)
        textureId = texture2dProgram.genTextureId()
    }

    fun drainFrame(b: Bitmap) {
        texture2dProgram.uploadBitmapToTexture(b, Size(width, height))
        eglEnv.setPresentationTime(0)
        eglEnv.swapBuffers()
    }

}