package com.jadyn.mediakit.video.encode

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import android.view.Surface
import com.jadyn.ai.kotlind.utils.createFloatBuffer
import com.jadyn.mediakit.gl.*
import java.nio.ShortBuffer
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 *@version:
 *@FileDescription: Surface OpenGL 编码视频帧核心类
 *@Author:Jing
 *@Since:2019-05-21
 *@ChangeList:
 */
class SurfaceEncodeCore(private val width: Int, private val height: Int) {
    private val TAG = "SurfaceEncodeCore"
    private val eglEnv by lazy {
        EglEnv(width, height)
    }

    private var surfaceTexture: SurfaceTexture? = null

    private val encodeProgram by lazy {
        SurfaceProgram()
    }

    private val semaphore by lazy {
        Semaphore(0)
    }

    fun buildEGLSurface(surface: Surface): SurfaceTexture {
        Log.d(TAG, "build egl thread: ${Thread.currentThread().name}")
        // 构建EGL环境
        eglEnv.setUpEnv().buildWindowSurface(surface)
        val textureId = encodeProgram.genTextureId()
        surfaceTexture = SurfaceTexture(textureId)
        /**
         * 这里必须着重标识！
         * 解决Camera2画面变形的原因，只需要把设置的宽高反过来，因为camera2返回来的匹配列表本来就是宽高反过来的
         * */
        surfaceTexture!!.setDefaultBufferSize(height, width)
        // 监听获取新的图像帧
        surfaceTexture!!.setOnFrameAvailableListener {
            Log.d(TAG, "surface texture available: ")
            semaphore.release()
        }
        return surfaceTexture!!
    }

    fun draw() {
        Log.d(TAG, "core draw thread ${Thread.currentThread().name}")
        awaitNewImage()
        encodeProgram.drawFrame(surfaceTexture!!)
    }

    fun swapData(nesc: Long) {
        eglEnv.setPresentationTime(nesc)
        eglEnv.swapBuffers()
    }

    private fun awaitNewImage() {
        if (!semaphore.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
            throw RuntimeException("Camera frame wait timed out")
        }
        checkGlError("before updateTexImage")
        surfaceTexture!!.updateTexImage()
    }

    fun release() {
        encodeProgram.release()
        eglEnv.release()
        surfaceTexture?.release()
        surfaceTexture = null
    }
}

/**
 * 使用 作为数据传递介质 Surface 绘制
 *
 * 每一帧的数据从SurfaceTexture 中获取
 * */

class SurfaceProgram {
    //顶点着色器
    private val VERTEX_SHADER =
            """
                attribute vec4 position;
                attribute vec4 aTexCoord;
                uniform mat4 texMatrix;
                varying vec2 vTexCoord;
                void main(){
                    gl_Position = position;
                    vTexCoord = (texMatrix * aTexCoord).xy;
                }
            """.trimIndent()

    //片元着色器
    private val FRAGMENT_SHADER =
            """
                #extension GL_OES_EGL_image_external : require
                precision mediump float;
                uniform samplerExternalOES texture;
                varying vec2 vTexCoord;
                void main () {
                    gl_FragColor = texture2D(texture, vTexCoord);
                }
            """.trimIndent()

    private val camera2Draw: Camera2Draw
    private var textureId: Int = GLES20.GL_NONE
    private var program: Int = GLES20.GL_NONE

    init {
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        camera2Draw = Camera2Draw(program)
    }

    fun genTextureId(): Int {
        textureId = buildTextureId()
        return textureId
    }

    fun drawFrame(st: SurfaceTexture) {
        camera2Draw.drawFrame(st, textureId)
    }

    fun release() {
        releaseTexture(intArrayOf(textureId))
        GLES20.glDeleteProgram(program)
    }
}

class Camera2Draw(program: Int) {
    private val vertexBuffer by lazy {
        createFloatBuffer(floatArrayOf(
                -1f, 1f, 0f,
                -1f, -1f, 0f,
                1f, -1f, 0f,
                1f, 1f, 0f
        ))
    }

    private val indexBuffer by lazy {
        val data = shortArrayOf(0, 1, 2, 0, 2, 3)
        val buffer = ShortBuffer.allocate(data.size).put(data)
        buffer.position(0)
        buffer
    }

    private val texCoordBuffer by lazy {
        createFloatBuffer(floatArrayOf(
                0f, 0f, 1f, 1f,
                0f, 1f, 1f, 1f,
                1f, 1f, 1f, 1f,
                1f, 0f, 1f, 1f
        ))
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

    fun drawFrame(surfaceTexture: SurfaceTexture, textureId: Int) {
        surfaceTexture.getTransformMatrix(stMatrix)

        // camera返回的纹理是，左右镜像，上下颠倒的
        // rotateM。这个函数的意义就是原点绕着x、y、z三个轴旋转
        // 这里的一个单位就是纹理在各自轴上的全长
        // 先把图像沿着x轴向右平移一个单位。然后在沿着y轴做180f旋转。这样图像镜像就处理好了。
        // 又回到了原来的坐标系
        Matrix.translateM(stMatrix, 0, 1f, 0f, 0f)
        // 绕着y轴旋转
        Matrix.rotateM(stMatrix, 0, 180f, 0f, 1f, 0f)

        // 接下来处理上下颠倒。颠倒的话，就是原点（0，0）沿着z轴转180度
        // 只沿着z轴转180度的话，那么x和y都会变成-1个单位。所以先把x和y都沿着轴的正方向平移一个单位，再旋转
        Matrix.translateM(stMatrix, 0, 1f, 1f, 0f)
        // 绕着z轴旋转
        Matrix.rotateM(stMatrix, 0, 180f, 0f, 0f, 1f)
        
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

        GLES20.glUniformMatrix4fv(stMatrixHandle, 1, false, stMatrix, 0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6,
                GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        disableVertexAttrib(posHandle)
        disableVertexAttrib(texCoordHandle)

        unBindTexture()
    }
}