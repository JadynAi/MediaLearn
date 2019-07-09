package com.jadyn.mediakit.video.encode

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.Size
import com.jadyn.ai.kotlind.utils.createFloatBuffer
import com.jadyn.mediakit.gl.*
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL10

/**
 *@version:
 *@FileDescription: 用来编码的GPU程序
 *@Author:Jing
 *@Since:2019/4/10
 *@ChangeList:
 */
class EncodeProgram(private val size: Size) {

    /**
     * 逐行解释
     *
     *  1、4*4的矩阵
     *  2、4维向量
     *  3、2维向量
     *  4、varying 修饰从顶点着色器传递到片元着色器过来的数据
     *
     * */
    private val VERTEX_SHADER = """
                uniform mat4 u_Matrix;
                attribute vec4 a_Position;
                attribute vec2 a_TexCoord;
                varying vec2 v_TexCoord;
                void main() {
                    v_TexCoord = a_TexCoord;
                    gl_Position = u_Matrix * a_Position;
                }
        """

    /**
     * 一下为代码的逐行解释
     *
     * 1、float 精度修饰， medium 16bit，用于纹理坐标
     * 2、varying 修饰从顶点着色器传递到片元着色器过来的数据
     * 3、二维纹理声明
     *
     * 4、使用texture2D取出纹理坐标点上的纹理像素值
     * */
    private val FRAGMENT_SHADER = """
                precision mediump float;
                varying vec2 v_TexCoord;
                uniform sampler2D u_TextureUnit;
                void main() {
                    gl_FragColor = texture2D(u_TextureUnit, v_TexCoord);
                }
                """

    private val pointData = floatArrayOf(
            2 * -0.5f, -0.5f * 2,
            2 * -0.5f, 0.5f * 2,
            2 * 0.5f, 0.5f * 2,
            2 * 0.5f, -0.5f * 2)

    /**
     * 纹理坐标
     */
    private val texVertex = floatArrayOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 1f)
    private val projectionMatrix = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            1f, 0f, 0f, 0f, 0f,
            1f, 0f, 0f, 0f, 0f, 1f)
    private var vertexData: FloatBuffer
    private var texVertexBuffer: FloatBuffer
    private var mAPositionLocation = 0
    private var uTextureUnitLocation = 0
    private var program: Int = 0
    private var aTextCoordLocation = 0
    
    var textureID = 0
        private set

    init {
        vertexData = createFloatBuffer(pointData)
        texVertexBuffer = createFloatBuffer(texVertex)
    }

    fun build() {
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        GLES20.glUseProgram(program)
        initLocation()
    }

    private fun initLocation() {
        mAPositionLocation = getAttribLocation(program, "a_Position")
        val uMatrixLocation = getUniformLocation(program, "u_Matrix")

        aTextCoordLocation = getAttribLocation(program, "a_TexCoord")
        uTextureUnitLocation = getUniformLocation(program, "u_TextureUnit")

        textureID = buildTextureId(GLES20.GL_TEXTURE_2D)

        // 加载纹理坐标
        texVertexBuffer.position(0)
        GLES20.glVertexAttribPointer(aTextCoordLocation, 2, GLES20.GL_FLOAT,
                false, 0, texVertexBuffer)
        GLES20.glEnableVertexAttribArray(aTextCoordLocation)

        GLES20.glClearColor(0f, 0f, 0f, 0f)
        // 开启纹理透明混合，这样才能绘制透明图片
        GLES20.glEnable(GL10.GL_BLEND)
        GLES20.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA)

        GLES20.glViewport(0, 0, size.width, size.height)
//        Matrix.orthoM(projectionMatrix, 0, -size.aspectRatio(), size.aspectRatio(), -1f,
//                1f, -1f, 1f)
        GLES20.glUniformMatrix4fv(uMatrixLocation, 1, false, projectionMatrix, 0)
    }

    fun renderBitmap(b: Bitmap) {
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, b, 0)
//        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        b.recycle()
        unBindTexture()

        GLES20.glClear(GL10.GL_COLOR_BUFFER_BIT)
        vertexData.position(0)

        GLES20.glVertexAttribPointer(mAPositionLocation, 2,
                GLES20.GL_FLOAT, false, 0, vertexData)
        GLES20.glEnableVertexAttribArray(mAPositionLocation)

        // 设置当前活动的纹理单元为纹理单元0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)

        // 将纹理ID绑定到当前活动的纹理单元上
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureID)

        GLES20.glUniform1i(uTextureUnitLocation, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, pointData.size / 2)
        unBindTexture()
    }
}