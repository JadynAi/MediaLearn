package com.jadyn.ai.medialearn.gles.render

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import com.jadyn.ai.kotlind.utils.createFloatBuffer
import com.jadyn.mediakit.gl.createProgram
import com.jadyn.mediakit.gl.getAttribLocation
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 *@version:
 *@FileDescription: 测试DrawElements
 *@Author:Jing
 *@Since:2019-07-09
 *@ChangeList:
 */
class DrawElementsRender : GLSurfaceView.Renderer {
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

    // 最原始的写法，要绘制6个顶点，两个三角形。但是有两个重复值
    private val rawVertex by lazy {
        floatArrayOf(
                // 左侧三角
                -0.8f, 1f, 0f,
                -0.8f, -1f, 0f,
                0.8f, -1f, 0f,

                // 右侧三角，两个三角拼成一个长方形
                0.8f, -1f, 0f,
                0.8f, 1f, 0f,
                -0.8f, 1f, 0f
        )
    }

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

    //------IntBuffer测试-----    
     private val indicesInts by lazy {
        intArrayOf(0, 1, 2, 2, 3, 0)
    }

    private val indicesIntBuffer by lazy {
        val buffer = IntBuffer.allocate(indices.size)
                .put(indicesInts)
        buffer.position(0)
        buffer
    }
    
    private val vertexBuffer by lazy {
        val floatBuffer = createFloatBuffer(vertex)
        Log.d("cece", "buffer pos : ${floatBuffer.position()}")
        floatBuffer
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // 按照次序来绘制
        // Type 设置为GL_UNSIGNED_BYTE，那么indicesBuffer的数据类型就为ByteBuffer
        GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                indices.size, GLES20.GL_UNSIGNED_BYTE, indicesBuffer)

        // Int 类型次序，测试可行
//        GLES20.glDrawElements(GLES20.GL_TRIANGLES,
//                indicesInts.size, GLES20.GL_UNSIGNED_INT, indicesIntBuffer)
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