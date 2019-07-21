package com.jadyn.ai.medialearn.gles.render

import android.graphics.drawable.BitmapDrawable
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import com.jadyn.ai.kotlind.function.ui.getResDrawable
import com.jadyn.ai.kotlind.utils.createFloatBuffer
import com.jadyn.ai.medialearn.R
import com.jadyn.mediakit.gl.*
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 *@version:
 *@FileDescription:
 *@Author:Jing
 *@Since:2019-07-17
 *@ChangeList:
 */
class TextureRender : GLSurfaceView.Renderer {

    private val VERTEX_SHADER =
            """
                attribute vec4 position;
                attribute vec2 aTexCoord;
                varying vec2 vTexCoord;
                void main(){
                    gl_Position = position;
                    vTexCoord = aTexCoord;
                }
            """.trimIndent()

    private val FRAGMENT_SHADER =
            """
                precision mediump float;
                varying vec2 vTexCoord;
                uniform sampler2D texture;
                void main(){
                    gl_FragColor = texture2D(texture, vTexCoord);
                }
            """.trimIndent()

    // 顶点坐标，四个顶点
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

    // 顶点的绘制次序
    private val indexBuffer by lazy {
        val data = intArrayOf(0, 1, 2, 0, 2, 3)
        val buffer = IntBuffer.allocate(data.size)
                .put(data)
        buffer.position(0)
        buffer
    }

    //纹理坐标
    private val texBuffer by lazy {
        // 纹理坐标是左下角为原点[0,0].这个绘制顺序是原点开始逆时针绘制
        val data = floatArrayOf(
                0f, 0f,
                0f, 0.5f,
                1f, 0.5f,
                1f, 0f
        )
        val floatBuffer = createFloatBuffer(data)
        floatBuffer.position(0)
        floatBuffer
    }

    private var textureHandle = 0
    private var textureId: Int = -1

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClearColor(1f, 1f, 1f, 1f)

//        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, ,0)
        GLES20.glUniform1i(textureHandle, 0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_INT,
                indexBuffer)

        unBindTexture()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)

        val posHandle = getAttribLocation(program, "position")
        val texCoordHandle = getAttribLocation(program, "aTexCoord")

//        mvpMatrixHandle = getUniformLocation(program, "mvpMatrix")
        textureHandle = getUniformLocation(program, "texture")

        textureId = buildTextureId(GLES20.GL_TEXTURE_2D)

        val bitmap = (getResDrawable(R.drawable.girl) as BitmapDrawable).bitmap

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap?.recycle()

        GLES20.glEnableVertexAttribArray(posHandle)
        GLES20.glEnableVertexAttribArray(texCoordHandle)

        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT,
                false, 12, vertexBuffer)

        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT,
                false, 0, texBuffer)
    }

}