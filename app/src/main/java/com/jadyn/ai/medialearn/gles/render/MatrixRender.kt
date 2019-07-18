package com.jadyn.ai.medialearn.gles.render

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.jadyn.ai.kotlind.utils.createFloatBuffer
import com.jadyn.mediakit.gl.createProgram
import com.jadyn.mediakit.gl.getAttribLocation
import com.jadyn.mediakit.gl.getUniformLocation
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 *@version:
 *@FileDescription:
 *@Author:Jing
 *@Since:2019-07-17
 *@ChangeList:
 */
class MatrixRender : GLSurfaceView.Renderer {

    private val VERTEX_SHADER =
            """
                uniform mat4 mvpMatrix;
                attribute vec4 position;
                void main(){
                    gl_Position = mvpMatrix * position;
                }
            """

    private val FRAGMENT_SHADER =
            """
                precision mediump float;
                void main(){
                    gl_FragColor = vec4(0, 1, 0, 1);
                }
            """.trimIndent()

    // 坐标原点在屏幕中心，以1为全长
    private val vertex by lazy {
        floatArrayOf(
                -0.8f, 1f, 0f,
                -0.8f, -1f, 0f,
                0.8f, -1f, 0f
        )
    }

    private val vertexBuffer by lazy {
        val floatBuffer = createFloatBuffer(vertex)
        Log.d("cece", "buffer pos : ${floatBuffer.position()}")
        floatBuffer
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        //  向mvpMatrix传值
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                mvpMatrix, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        Matrix.perspectiveM(mvpMatrix, 0, 45f, width / height.toFloat(),
                0.1f, 100f)
        Matrix.translateM(mvpMatrix, 0, 0f, 0f, -2.5f)
    }

    private var positionHandle = 0
    private var mvpMatrixHandle = 0
    private val mvpMatrix = FloatArray(16)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)

        positionHandle = getAttribLocation(program, "position")
        mvpMatrixHandle = getUniformLocation(program, "mvpMatrix")

        GLES20.glEnableVertexAttribArray(positionHandle)

        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false,
                0, vertexBuffer)
    }
}