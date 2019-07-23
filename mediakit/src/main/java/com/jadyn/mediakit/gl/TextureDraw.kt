package com.jadyn.mediakit.gl

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import com.jadyn.ai.kotlind.utils.createFloatBuffer
import java.nio.ShortBuffer


/**
 *@version:
 *@FileDescription: 负责从SurfaceTexture拉取数据，并执行纹理的绘制工作
 *@Author:Jing
 *@Since:2019/4/3
 *@ChangeList:
 */
class TextureDraw(private val program: Int) {
    /**
     * 顶点
     * */
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

    /**
     * 顶点顺序
     * */
    private val indicesBuffer by lazy {
        val data = shortArrayOf(0, 1, 2, 0, 2, 3)
        val buffer = ShortBuffer.allocate(data.size)
        buffer.put(data)
        buffer.position(0)
        buffer
    }

    /**
     * 纹理坐标
     * */
    private val texCoordBuffer by lazy {
        val data = floatArrayOf(
                0f, 0f, 1f, 1f,
                0f, 1f, 1f, 1f,
                1f, 1f, 1f, 1f,
                1f, 0f, 1f, 1f
        )
        val buffer = createFloatBuffer(data)
        buffer.position(0)
        buffer
    }

    private var posHandle: Int = -1
    private var texCoordHandle: Int = -1
    private var textureHandle: Int = -1
    private var stMatrixHandle: Int = -1

    private val stMatrix by lazy {
        FloatArray(16)
    }

    init {

        posHandle = getAttribLocation(program, "position")
        texCoordHandle = getAttribLocation(program, "aTexCoord")
        textureHandle = getUniformLocation(program, "texture")

        stMatrixHandle = getUniformLocation(program, "texMatrix")
    }

    fun drawFromSurfaceTexture(st: SurfaceTexture, textureId: Int, isRevert: Boolean = false) {
        st.getTransformMatrix(stMatrix)

        // camera返回的纹理是，左右镜像，上下颠倒的
        if (isRevert) {
            // 这里的一个单位就是纹理在各自轴上的全长
            // 先把图像沿着x轴向右平移一个单位。然后在沿着y轴做180f旋转。这样图像镜像就处理好了。
            // 又回到了原来的坐标系
            Matrix.translateM(stMatrix, 0, 1f, 0f, 0f)
            Matrix.rotateM(stMatrix, 0, 180f, 0f, 1f, 0.0f)

            // 接下来处理上下颠倒。颠倒的话，就是原点（0，0）沿着z轴转180度
            // 只沿着z轴转180度的话，那么x和y都会变成-1个单位。所以先把x和y都沿着轴的正方向平移一个单位，再旋转
            Matrix.translateM(stMatrix, 0, 1f, 1f, 0f)
            Matrix.rotateM(stMatrix, 0, 180f, 0f, 0f, 1f)
//            Matrix.rotateM(stMatrix, 0, 180f, 0.5f, 0.5f, 1.0f)
        }

        enableVertexAttrib(posHandle)
        enableVertexAttrib(texCoordHandle)

        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT,
                false, 12, vertexBuffer)
        GLES20.glVertexAttribPointer(texCoordHandle, 4, GLES20.GL_FLOAT,
                false, 16, texCoordBuffer)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUniform1i(textureHandle, 0)

        /**
         * @param count。矩阵数
         * */
        GLES20.glUniformMatrix4fv(stMatrixHandle, 1, false, stMatrix, 0)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6,
                GLES20.GL_UNSIGNED_SHORT, indicesBuffer)

        disableVertexAttrib(posHandle)
        disableVertexAttrib(texCoordHandle)

        unBindTexture()
    }
}

/**
 *  [1.0:0.0:0.0:0.0
 *  :0.0:-1.0:0.0:0.0:
 *  0.0:0.0:1.0:0.0:
 *  0.0:1.0:0.0:1.0
 *
 *
 *  camera: [0.0:-1.0:0.0:0.0:
 *          -1.0:0.0:0.0:0.0:
 *          0.0:0.0:1.0:0.0:
 *          1.0:1.0:0.0:1.0
 * */


fun FloatArray.toS(): String {
    val s = StringBuffer("[")
    this.forEach {
        s.append("$it:")
    }
    return s.toString()
}