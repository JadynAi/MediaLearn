package com.jadyn.mediakit.gl

import android.graphics.SurfaceTexture
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
    private val VERTEX_SHADER = """
        attribute vec4 vPosition;
        attribute vec4 vTexCoordinate;
        uniform mat4 textureTransform;
        varying vec2 v_TexCoordinate;

        void main () {
            v_TexCoordinate = (textureTransform * vTexCoordinate).xy;
            gl_Position = vPosition;
        }
        """

    //片元着色器. 替换黑色背景
    private val FRAGMENT_SHADER = """
        #extension GL_OES_EGL_image_external : require
        precision highp float;
        uniform samplerExternalOES texture;
        varying highp vec2 v_TexCoordinate;
        void main () {
            vec4 color = texture2D(texture, v_TexCoordinate);
            vec3 colorToReplace = vec3(0.0, 0.0, 0.0);
            float maskY = 0.2989 * colorToReplace.r + 0.5866 * colorToReplace.g + 0.1145 * colorToReplace.b;
            float maskCr = 0.7132 * (colorToReplace.r - maskY);
            float maskCb = 0.5647 * (colorToReplace.b - maskY);

            float Y = 0.2989 * color.r + 0.5866 * color.g + 0.1145 * color.b;
            float Cr = 0.7132 * (color.r - Y);
            float Cb = 0.5647 * (color.b - Y);
            
            float thresholdSensitivity = 0.3;
            float smoothing = 0.1;

            float blendValue = smoothstep(thresholdSensitivity, thresholdSensitivity + smoothing, distance(vec2(Cr, Cb), vec2(maskCr, maskCb)));
            gl_FragColor = vec4(color.rgb, color.a * blendValue);
//            gl_FragColor = vec4(color.rgb * blendValue, 1.0 * blendValue);
        }
        """

    //片元着色器
    private val NORMAL_FRAGMENT_SHADER = """
        #extension GL_OES_EGL_image_external : require
        precision highp float;
        uniform samplerExternalOES texture;
        varying highp vec2 v_TexCoordinate;
        void main () {
            gl_FragColor = texture2D(texture, v_TexCoordinate);
        }
        """

    private var textureId: Int = 0

    private val textureDraw: TextureDraw

    init {
        Log.d(this.javaClass.name, " create texture 2d program ")
        val program = createProgram(VERTEX_SHADER, NORMAL_FRAGMENT_SHADER)
        textureDraw = TextureDraw(program)
    }

    fun genTextureId(): Int {
        textureId = buildTextureId()
        return textureId
    }

    fun drawFrame(st: SurfaceTexture, isRevert: Boolean = true) {
        textureDraw.drawFromSurfaceTexture(st, textureId, isRevert)
    }
}