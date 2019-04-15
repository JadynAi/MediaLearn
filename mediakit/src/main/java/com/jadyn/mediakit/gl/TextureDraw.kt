package com.jadyn.mediakit.gl

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import android.util.Size
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 *@version:
 *@FileDescription: 负责纹理的绘制工作
 *@Author:Jing
 *@Since:2019/4/3
 *@ChangeList:
 */
class TextureDraw(private val program: Int) {

    private val FLOAT_SIZE_BYTES = 4
    private val TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES
    private val TRIANGLE_VERTICES_DATA_POS_OFFSET = 0
    private val TRIANGLE_VERTICES_DATA_UV_OFFSET = 3

    private val mTriangleVerticesData = floatArrayOf(
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0f, 0f, 0f,
            1.0f, -1.0f, 0f, 1f, 0f,
            -1.0f, 1.0f, 0f, 0f, 1f,
            1.0f, 1.0f, 0f, 1f, 1f)

    private val triangleVertices: FloatBuffer

    private val mvpMatrix = FloatArray(16)
    private val stMatrix = FloatArray(16)

    private var uMVPMatrixHandle: Int = 0
    private var uSTMatrixHandle: Int = 0
    private var maPositionHandle: Int = 0
    private var glTextureCoord: Int = 0

    init {
        triangleVertices = ByteBuffer.allocateDirect(
                mTriangleVerticesData.size * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
        triangleVertices.put(mTriangleVerticesData).position(0)

        Matrix.setIdentityM(stMatrix, 0)

        // get locations of attributes and uniforms
        maPositionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        checkLocation(maPositionHandle, "aPosition")
        glTextureCoord = GLES20.glGetAttribLocation(program, "aTextureCoord")
        checkLocation(glTextureCoord, "aTextureCoord")

        uMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        checkLocation(uMVPMatrixHandle, "uMVPMatrix")
        uSTMatrixHandle = GLES20.glGetUniformLocation(program, "uSTMatrix")
        checkLocation(uSTMatrixHandle, "uSTMatrix")
    }


    fun drawFromSurfaceTexture(st: SurfaceTexture, invert: Boolean, textureId: Int) {
        st.getTransformMatrix(stMatrix)
        if (invert) {
            stMatrix[5] = -stMatrix[5]
            stMatrix[13] = 1.0f - stMatrix[13]
        }

        GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f)
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)

        // 使用显卡绘制程序
        GLES20.glUseProgram(program)
        checkGlError("glUseProgram")

        // 指定将要绘制的纹理对象并传递给对应的FragmentShader
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        // 设置物体坐标
        triangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET)
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices)
        checkGlError("glVertexAttribPointer maPosition")
        GLES20.glEnableVertexAttribArray(maPositionHandle)
        checkGlError("glEnableVertexAttribArray maPositionHandle")

        // 设置纹理坐标
        triangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET)
        GLES20.glVertexAttribPointer(glTextureCoord, 2, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices)
        checkGlError("glVertexAttribPointer maTextureHandle")
        GLES20.glEnableVertexAttribArray(glTextureCoord)
        checkGlError("glEnableVertexAttribArray maTextureHandle")

        Matrix.setIdentityM(mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uSTMatrixHandle, 1, false, stMatrix, 0)

        // 执行挥之操作
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        checkGlError("glDrawArrays")
        unBindTexture()
    }

    fun drawBitmap(b: Bitmap, size: Size, textureId: Int) {
        GLES20.glViewport(0, 0, size.width, size.height)

        GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f)
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)

        // 使用显卡绘制程序
        GLES20.glUseProgram(program)
        checkGlError("glUseProgram")

        // 指定将要绘制的纹理对象并传递给对应的FragmentShader
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        Matrix.setIdentityM(mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0)

        // 设置物体坐标
        triangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET)
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices)
        checkGlError("glVertexAttribPointer maPosition")
        GLES20.glEnableVertexAttribArray(maPositionHandle)
        checkGlError("glEnableVertexAttribArray maPositionHandle")

        // 设置纹理坐标
        triangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET)
        GLES20.glVertexAttribPointer(glTextureCoord, 2, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices)
        checkGlError("glVertexAttribPointer maTextureHandle")
        GLES20.glEnableVertexAttribArray(glTextureCoord)
        checkGlError("glEnableVertexAttribArray maTextureHandle")


        GLUtils.texImage2D(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0, b, 0)

        // 执行挥之操作
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        checkGlError("glDrawArrays")
        unBindTexture()
    }

    private fun unBindTexture() {
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
    }
}