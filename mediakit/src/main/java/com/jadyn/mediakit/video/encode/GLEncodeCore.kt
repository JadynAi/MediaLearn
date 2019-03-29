package com.jadyn.mediakit.video.encode

import android.graphics.SurfaceTexture
import android.view.Surface
import com.jadyn.mediakit.gl.EglEnv
import com.jadyn.mediakit.gl.Texture2dProgram

/**
 *@version:
 *@FileDescription:OpenGL编码核心
 *@Author:Jing
 *@Since:2019/3/29
 *@ChangeList:
 */
class GLEncodeCore(private val width: Int, private val height: Int) {

    init {
        // 构建EGL环境
        EglEnv(width, height)
                .setUpEnv()
                .buildBackgroundSurface()
    }

    private val texture2dProgram by lazy {
        Texture2dProgram()
    }

    private var surfaceTexture: SurfaceTexture? = null

    fun generateTextureSurface(): Surface {
        surfaceTexture = SurfaceTexture(texture2dProgram.genTextureId())
        return Surface(surfaceTexture)
    }

    fun release() {
        surfaceTexture?.release()
    }
}