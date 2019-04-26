package com.jadyn.mediakit.gl

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer


/**
 *@version:
 *@FileDescription: 负责纹理的绘制工作
 *@Author:Jing
 *@Since:2019/4/3
 *@ChangeList:
 */
class TextureDraw(private val program: Int) {

    private val stMatrix = FloatArray(16)

    /**
     * 绘制的区域尺寸
     */
    private val squareSize = 1.0f
    private val squareCoords = floatArrayOf(-squareSize, squareSize, //left top
            -squareSize, -squareSize, //left bottom
            squareSize, -squareSize, //right bottom
            squareSize, squareSize      //right top
    )

    /**
     * 纹理坐标
     */
    private val textureCoords = floatArrayOf(
            0.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 1.0f, 0.0f, 1.0f)

    /**
     * 用来缓存纹理坐标，因为纹理都是要在后台被绘制好，然
     * 后不断的替换最前面显示的纹理图像
     */
    private var textureBuffer: FloatBuffer? = null

    /**
     * 绘制次序的缓存
     */
    private var drawOrderBuffer: ShortBuffer? = null

    /**
     * squareCoords的的顶点缓存
     */
    private var vertexBuffer: FloatBuffer? = null


    /**
     * 绘制次序
     */
    private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3)


    init {
        setUpTexture()
        setupVertexBuffer()
    }

    private fun setupVertexBuffer() {
        val orderByteBuffer = ByteBuffer.allocateDirect(drawOrder.size * 2)
        orderByteBuffer.order(ByteOrder.nativeOrder())  //Modifies this buffer's byte order
        drawOrderBuffer = orderByteBuffer.asShortBuffer()  //创建此缓冲区的视图，作为一个short缓冲区.
        drawOrderBuffer!!.put(drawOrder)
        drawOrderBuffer!!.position(0) //下一个要被读或写的元素的索引，从0 开始

        // Initialize the texture holder
        val bb = ByteBuffer.allocateDirect(squareCoords.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer!!.put(squareCoords)
        vertexBuffer!!.position(0)
    }

    private fun setUpTexture() {
        val texturebb = ByteBuffer.allocateDirect(textureCoords.size * 4)
        texturebb.order(ByteOrder.nativeOrder())
        textureBuffer = texturebb.asFloatBuffer()
        textureBuffer!!.put(textureCoords)
        textureBuffer!!.position(0)
    }


    fun drawFromSurfaceTexture(st: SurfaceTexture, textureId: Int) {
        st.getTransformMatrix(stMatrix)
        stMatrix[5] = -stMatrix[5]
        stMatrix[13] = 1.0f - stMatrix[13]

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)
        val positionAttr = GLES20.glGetAttribLocation(program, "vPosition")
        val textureCoordinateAttr = GLES20.glGetAttribLocation(program, "vTexCoordinate")
        val textureUniform = GLES20.glGetUniformLocation(program, "texture")
        val textureTransforUniform = GLES20.glGetUniformLocation(program, "textureTransform")

        GLES20.glEnableVertexAttribArray(positionAttr)
        GLES20.glVertexAttribPointer(positionAttr, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glUniform1i(textureUniform, 0)

        GLES20.glEnableVertexAttribArray(textureCoordinateAttr)
        GLES20.glVertexAttribPointer(textureCoordinateAttr, 4, GLES20.GL_FLOAT, false, 0, textureBuffer)

        GLES20.glUniformMatrix4fv(textureTransforUniform, 1, false,
                stMatrix, 0)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.size, GLES20.GL_UNSIGNED_SHORT, drawOrderBuffer)
        GLES20.glDisableVertexAttribArray(positionAttr)
        GLES20.glDisableVertexAttribArray(textureCoordinateAttr)
        unBindTexture()
    }

    private fun unBindTexture() {
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
    }
}