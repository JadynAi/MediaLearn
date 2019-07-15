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
 *@FileDescription:
 *@Author:Jing
 *@Since:2019-07-09
 *@ChangeList:
 */
class FirstRender : GLSurfaceView.Renderer {
    private val VERTEX_SHADER =
            """
                attribute vec3 position;
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

        /**
         * 1、 表示要绘制的是三角形
         * 2、 顶点数组其实索引
         * 3、 绘制的顶点个数
         * */
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)
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

        /**
         * 告诉OpenGL 如何解析Vertex 顶点数据
         * 1、 顶点着色器返回的ID
         * 2、 Vertex的大小， vec3 or vec4
         * 3、 数据类型
         * 4、 是否要把数据标准化，即把数据标准化到 0~1
         * 5、 stride，单位为byte。一个float为4个byte。0代表紧密排列。
         * 这个值得意思是，每隔stride取一组数据.这一组数据的长度即为size
         * */
        GLES20.glVertexAttribPointer(pos, 3, GLES20.GL_FLOAT, false,
                0, vertexBuffer)
    }
}