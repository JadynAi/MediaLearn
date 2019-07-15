package com.jadyn.ai.medialearn.gles.render

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import com.jadyn.ai.kotlind.utils.createFloatBuffer
import com.jadyn.mediakit.gl.createProgram
import com.jadyn.mediakit.gl.getAttribLocation
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 *@version:
 *@FileDescription: 绘制一个长方形
 *@Author:Jing
 *@Since:2019-07-09
 *@ChangeList:
 */
class SecondRender : GLSurfaceView.Renderer {
    private val VERTEX_SHADER =
            """
                attribute vec4 position;
                void main(){
                    gl_Position = position;
                }
            """.trimIndent()

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
                -1f, 1f, 0f,
                -0.5f, -1f, 0f,
                0.2f, -1f, 0f,

                0.8f, -1f, 0f,
                0.8f, 1f, 0f,
                -0.8f, 1f, 0f
        )
    }

    private val vertexBuffer by lazy {
        val floatBuffer = createFloatBuffer(vertex)
        Log.d("cece", "buffer pos : ${floatBuffer.position()}")
        floatBuffer
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        vertexBuffer.position(0)
        val program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        val pos = getAttribLocation(program, "position")

        // 允许顶点着色器，读取GPU的数据，启用指定属性
        GLES20.glEnableVertexAttribArray(pos)

        // 每隔3个float值，取一组长度为3的数据
        GLES20.glVertexAttribPointer(pos, 3, GLES20.GL_FLOAT, false,
                12, vertexBuffer)
    }
}