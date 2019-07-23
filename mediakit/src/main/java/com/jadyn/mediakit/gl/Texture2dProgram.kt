package com.jadyn.mediakit.gl

import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.util.Log

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
    var frameBufferId: Int = GLES20.GL_NONE
        private set

    private val textureDraw: TextureDraw

    init {
        Log.d(this.javaClass.name, " create texture 2d program ")
        val program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
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
    private fun genFrameBuffer() {
        frameBufferId = buildFrameBuffer()
        appendFBOTexture(textureId)
    }

    fun drawFrame(st: SurfaceTexture, isRevert: Boolean = true) {
        textureDraw.drawFromSurfaceTexture(st, textureId, isRevert)
    }

    fun release() {
//        releaseFrameBufferTexture(frameBufferId, textureId)
    }
}