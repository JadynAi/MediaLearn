package com.jadyn.mediakit.gl

import android.graphics.SurfaceTexture
import android.opengl.GLES20

/**
 *@version:
 *@FileDescription: 创建显卡可执行程序Program
 *@Author:Jing
 *@Since:2019/3/27
 *@ChangeList:
 */
class Texture2dProgram {

    //顶点着色器
    private val VERTEX_SHADER =
            """
                attribute vec4 position;
                attribute vec4 aTexCoord;
                uniform mat4 texMatrix;
                varying vec2 vTexCoord;
                void main(){
                    gl_Position = position;
                    vTexCoord = (texMatrix * aTexCoord).xy;
                }
            """.trimIndent()

    //片元着色器
    private val FRAGMENT_SHADER =
            """
                #extension GL_OES_EGL_image_external : require
                precision mediump float;
                uniform samplerExternalOES texture;
                varying vec2 vTexCoord;
                void main () {
                    gl_FragColor = texture2D(texture, vTexCoord);
                }
            """.trimIndent()

    private var textureId: Int = GLES20.GL_NONE
    private val program: Int

    private val textureDraw: TextureDraw

    init {
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        textureDraw = TextureDraw(program)
    }

    fun genTextureId(): Int {
        textureId = buildTextureId()
        return textureId
    }

    /**
     * 生成一个帧缓冲区。
     * 并将纹理绑定到颜色附件
     * */
//    @Deprecated("not need framebuffer")
//    private fun genFrameBuffer() {
//        buildFrameBuffer()
//        appendFBOTexture(textureId)
//    }

    fun drawFrame(st: SurfaceTexture) {
        textureDraw.drawFromSurfaceTexture(st, textureId)
    }

    fun release() {
        releaseTexture(intArrayOf(textureId))
        GLES20.glDeleteProgram(program)
    }
}