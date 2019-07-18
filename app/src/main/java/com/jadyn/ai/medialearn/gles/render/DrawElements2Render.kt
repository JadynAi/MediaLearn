package com.jadyn.ai.medialearn.gles.render

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import com.jadyn.ai.kotlind.utils.createFloatBuffer
import com.jadyn.mediakit.gl.createProgram
import com.jadyn.mediakit.gl.getAttribLocation
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 *@version:
 *@FileDescription: DrawElements，使用索引缓冲对象
 *@Author:Jing
 *@Since:2019-07-09
 *@ChangeList:
 */
class DrawElements2Render : GLSurfaceView.Renderer {
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

    private val vertex by lazy {
        floatArrayOf(
                // 要画一个长方形，只需要四个顶点。然后定制一个顺序列表，按照顺序列表绘制两个三角形出来
                -0.8f, 1f, 0f,
                -0.8f, -1f, 0f,

                0.8f, -1f, 0f,
                0.8f, 1f, 0f
        )
    }

    // 长方形有四个顶点，但是绘制的时候还是要绘制两个三角形。即六个点。这里是顺序
    // 线绘制 vertex 的第一个顶点，然后绘制其他的
    private val indices by lazy {
        byteArrayOf(0, 1, 2, 2, 3, 0)
    }

    private val indicesBuffer by lazy {
        val buffer = ByteBuffer.allocate(indices.size)
                .order(ByteOrder.nativeOrder())
                .put(indices)
        buffer.position(0)
        buffer
    }

    private val EBO by lazy {
        IntArray(1)
    }

    private val vertexBuffer by lazy {
        val floatBuffer = createFloatBuffer(vertex)
        Log.d("cece", "buffer pos : ${floatBuffer.position()}")
        floatBuffer
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                6, GLES20.GL_UNSIGNED_BYTE, 0)
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

        // 创建索引缓冲对象
        GLES20.glGenBuffers(1, EBO, 0)

        // 复制索引数组到索引缓冲
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, EBO[0])
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indices.size,
                indicesBuffer, GLES20.GL_STATIC_DRAW)
    }
}