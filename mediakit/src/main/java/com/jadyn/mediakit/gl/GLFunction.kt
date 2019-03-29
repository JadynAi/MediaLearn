package com.jadyn.mediakit.gl

import android.content.ContentValues.TAG
import android.opengl.GLES20
import android.util.Log
import com.jadyn.mediakit.function.checkGlError

/**
 *@version:
 *@FileDescription:OpenGL ES 工具类
 *@Author:Jing
 *@Since:2019/3/27
 *@ChangeList:
 */
//顶点着色器
private val VERTEX_SHADER = "uniform mat4 uMVPMatrix;\n" +
        "uniform mat4 uSTMatrix;\n" +
        "attribute vec4 aPosition;\n" +
        "attribute vec4 aTextureCoord;\n" +
        "varying vec2 vTextureCoord;\n" +
        "void main() {\n" +
        "gl_Position = uMVPMatrix * aPosition;\n" +
        "vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
        "}\n"

//片元着色器
private val FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
        "precision mediump float;\n" +
        "varying vec2 vTextureCoord;\n" +
        "uniform samplerExternalOES sTexture;\n" +
        "void main() {\n" +
        "gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
        "}\n"

private val TAG = "GLFunction"

/**
 * 创建一个纹理program
 * */
fun createTexture2DProgram(): Int {
    return createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
}

/**
 * 创建一个显卡可执行程序，运行在GPU
 * */
fun createProgram(vertexSource: String, fragmentSource: String): Int {
    val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
    Log.d(TAG, "createProgram vertexShader: $vertexShader ")
    if (vertexShader == 0) {
        return 0
    }
    val pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
    Log.d(TAG, "createProgram vertexShader: $pixelShader ")
    if (pixelShader == 0) {
        return 0
    }

    // 创建一个显卡可执行程序
    var program = GLES20.glCreateProgram()
    if (program == 0) {
        Log.e(TAG, "Could not create program")
    }
    // 将编译好的shader着色器加载到这个可执行程序上
    GLES20.glAttachShader(program, vertexShader)
    checkGlError("glAttachShader")
    GLES20.glAttachShader(program, pixelShader)
    checkGlError("glAttachShader")
    // 链接程序
    GLES20.glLinkProgram(program)
    // 检查程序状态，第三个参数是返回值，返回1就是成功，返回0就是失败
    val linkStatus = IntArray(1)
    GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
    if (linkStatus[0] != GLES20.GL_TRUE) {
        Log.e(TAG, "Could not link program: ")
        Log.e(TAG, GLES20.glGetProgramInfoLog(program))
        GLES20.glDeleteProgram(program)
        program = 0
    }
    return program
}

fun loadShader(shaderType: Int, source: String): Int {
    // 创建一个对象，作为shader容器，此函数返回容器对象地址
    var shader = GLES20.glCreateShader(shaderType)
    checkGlError("glCreateShader type=$shaderType")
    // 为shader添加源代码，shader content（着色器程序，根据GLSL语法和内嵌函数编写）
    // 将开发者编写的着色器程序加载到着色器对象的内存中
    GLES20.glShaderSource(shader, source)
    // 编译这个着色器
    GLES20.glCompileShader(shader)
    val compiled = IntArray(1)
    // 验证是否编译成功.第二个参数是需要验证shader的状态值。第三个参数是返回值，返回1说明成功，返回0则不成功
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
    if (compiled[0] == 0) {
        Log.e(TAG, "Could not compile shader $shaderType:")
        // 创建失败，打印日志
        Log.e(TAG, " " + GLES20.glGetShaderInfoLog(shader))
        GLES20.glDeleteShader(shader)
        shader = 0
    }
    return shader
}