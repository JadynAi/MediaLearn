package com.jadyn.mediakit.video.encode

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.Size
import com.jadyn.ai.kotlind.utils.createFloatBuffer
import com.jadyn.mediakit.gl.*
import java.nio.IntBuffer
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
                attribute vec4 position;
                attribute vec2 aTexCoord;
                varying vec2 vTexCoord;
                void main() {
                    vTexCoord = aTexCoord;
                    gl_Position = position;
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
                varying vec2 vTexCoord;
                uniform sampler2D texture;
                void main() {
                    gl_FragColor = texture2D(texture, vTexCoord);
                }
                """


    private var program: Int = 0

    private val vertexBuffer by lazy {
        val data = floatArrayOf(
                -1f, 1f, 0f,
                -1f, -1f, 0f,
                1f, -1f, 0f,
                1f, 1f, 0f
        )
        val buffer = createFloatBuffer(data)
        buffer.position(0)
        buffer
    }

    private val indexBuffer by lazy {
        val data = intArrayOf(0, 1, 2, 0, 3, 2)
        val allocate = IntBuffer.allocate(data.size).put(data)
        allocate.position(0)
        allocate
    }

    private val texBuffer by lazy {
        val buffer = createFloatBuffer(floatArrayOf(
                0f, 0f,
                0f, 1f,
                1f, 1f,
                1f, 0f
        ))
        buffer.position(0)
        buffer
    }

    private var posHandle: Int = -1
    private var texHandle: Int = -1
    private var textureHandle: Int = -1

    var textureID = 0
        private set


    fun build() {
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        initLocation()
    }

    private fun initLocation() {
        posHandle = getAttribLocation(program, "position")
        texHandle = getAttribLocation(program, "aTexCoord")

        textureHandle = getUniformLocation(program, "texture")

        textureID = buildTextureId(GLES20.GL_TEXTURE_2D)

        GLES20.glClearColor(0f, 0f, 0f, 0f)
        // 开启纹理透明混合，这样才能绘制透明图片
//        GLES20.glEnable(GL10.GL_BLEND)
//        GLES20.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA)

        GLES20.glViewport(0, 0, size.width, size.height)
    }

    fun renderBitmap(b: Bitmap) {
        GLES20.glClear(GL10.GL_COLOR_BUFFER_BIT)

        // 设置当前活动的纹理单元为纹理单元0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        // 将纹理ID绑定到当前活动的纹理单元上
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureID)

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, b, 0)
        b.recycle()

        // 顶点坐标
        GLES20.glEnableVertexAttribArray(posHandle)
        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT,
                false, 12, vertexBuffer)

        // 纹理坐标
        GLES20.glEnableVertexAttribArray(texHandle)
        GLES20.glVertexAttribPointer(texHandle, 2, GLES20.GL_FLOAT,
                false, 0, texBuffer)

        GLES20.glUniform1i(texHandle, 0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6,
                GLES20.GL_UNSIGNED_INT, indexBuffer)
        unBindTexture(GLES20.GL_TEXTURE_2D)
    }

    fun release() {
        releaseTexture(intArrayOf(textureID))
    }
}