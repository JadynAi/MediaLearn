package com.jadyn.mediakit.gl

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Size
import com.jadyn.mediakit.function.checkGlError
import com.jadyn.mediakit.function.checkLocation
import com.jadyn.mediakit.function.getByteBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 *@version:
 *@FileDescription: 创建一个2D纹理，显卡可执行程序Program
 *@Author:Jing
 *@Since:2019/3/27
 *@ChangeList:
 */
class Texture2dProgram {
    private val FLOAT_SIZE_BYTES = 4
    private val TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES
    private val TRIANGLE_VERTICES_DATA_POS_OFFSET = 0
    private val TRIANGLE_VERTICES_DATA_UV_OFFSET = 3

    private val mTriangleVerticesData = floatArrayOf(
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0f, 0f, 0f, 1.0f, -1.0f, 0f, 1f, 0f, -1.0f, 1.0f, 0f, 0f, 1f, 1.0f, 1.0f, 0f, 1f, 1f)

    private val mTriangleVertices: FloatBuffer

    private val mMVPMatrix = FloatArray(16)
    private val mSTMatrix = FloatArray(16)

    private val program: Int
    private var textureId: Int = 0

    private var muMVPMatrixHandle: Int = 0
    private var muSTMatrixHandle: Int = 0
    private var maPositionHandle: Int = 0
    private var maTextureHandle: Int = 0

    init {
        mTriangleVertices = ByteBuffer.allocateDirect(
                mTriangleVerticesData.size * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
        mTriangleVertices.put(mTriangleVerticesData).position(0)

        Matrix.setIdentityM(mSTMatrix, 0)
        program = createTexture2DProgram()

        // get locations of attributes and uniforms
        maPositionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        checkLocation(maPositionHandle, "aPosition")
        maTextureHandle = GLES20.glGetAttribLocation(program, "aTextureCoord")
        checkLocation(maTextureHandle, "aTextureCoord")

        muMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        checkLocation(muMVPMatrixHandle, "uMVPMatrix")
        muSTMatrixHandle = GLES20.glGetUniformLocation(program, "uSTMatrix")
        checkLocation(muSTMatrixHandle, "uSTMatrix")
    }

    /**
     * 创建一个纹理对象，并且和ES绑定
     *
     * 生成camera特殊的Texture
     * 在Android中Camera产生的preview texture是以一种特殊的格式传送的，
     * 因此shader里的纹理类型并不是普通的sampler2D,而是samplerExternalOES,
     * 在shader的头部也必须声明OES 的扩展。除此之外，external OES的纹理和Sampler2D在使用时没有差别
     * */
    fun genTextureId(): Int {
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        checkGlError("create texture check")
        val id = ids[0]
        textureId = id
        bindSetTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, id)
        return id
    }

    private fun bindSetTexture(target: Int, id: Int) {
        GLES20.glBindTexture(target, id)
        checkGlError("bind texture : $id check")

        //----设置纹理参数----
        // 纹理过滤器放大缩小
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)

        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    /**
     * 将bitmap渲染到纹理上
     * */
    fun renderTexture(bitmap: Bitmap, targetSize: Size) {
        val scaleBitmap = Bitmap.createBitmap(bitmap, 0, 0, targetSize.width, targetSize.height)
        val byteBuffer = scaleBitmap.getByteBuffer()
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                targetSize.width, targetSize.height, 0, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, byteBuffer)
    }

    fun drawFrame(st: SurfaceTexture, invert: Boolean) {
        checkGlError("onDrawFrame start")
        st.getTransformMatrix(mSTMatrix)
        if (invert) {
            mSTMatrix[5] = -mSTMatrix[5]
            mSTMatrix[13] = 1.0f - mSTMatrix[13]
        }

        GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f)
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(program)
        checkGlError("glUseProgram")

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET)
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices)
        checkGlError("glVertexAttribPointer maPosition")
        GLES20.glEnableVertexAttribArray(maPositionHandle)
        checkGlError("glEnableVertexAttribArray maPositionHandle")

        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET)
        GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices)
        checkGlError("glVertexAttribPointer maTextureHandle")
        GLES20.glEnableVertexAttribArray(maTextureHandle)
        checkGlError("glEnableVertexAttribArray maTextureHandle")

        Matrix.setIdentityM(mMVPMatrix, 0)
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0)
        GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        checkGlError("glDrawArrays")
        unBindTexture()
    }

    fun unBindTexture() {
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
    }
}