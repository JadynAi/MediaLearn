package com.jadyn.mediakit.gl

import android.app.ActivityManager
import android.content.Context
import android.opengl.EGL14
import android.opengl.GLES20
import android.util.Log
import com.jadyn.ai.kotlind.base.BaseApplication

/**
 *@version:
 *@FileDescription:OpenGL ES 工具类
 *@Author:Jing
 *@Since:2019/3/27
 *@ChangeList:
 */

private val TAG = "GLFunction"

//顶点着色器
val NORMAL_VERTEX_SHADER = """
        attribute vec4 vPosition;
        attribute vec4 vTexCoordinate;
        uniform mat4 textureTransform;
        varying vec2 v_TexCoordinate;

        void main () {
            v_TexCoordinate = (textureTransform * vTexCoordinate).xy;
            gl_Position = vPosition;
        }
        """

//片元着色器
val NORMAL_FRAGMENT_SHADER = """
        #extension GL_OES_EGL_image_external : require
        precision highp float;
        uniform samplerExternalOES texture;
        varying highp vec2 v_TexCoordinate;
        void main () {
            gl_FragColor = texture2D(texture, v_TexCoordinate);
        }
        """

fun createCommoneProgram(): Int {
    return createProgram(NORMAL_VERTEX_SHADER, NORMAL_FRAGMENT_SHADER)
}

/**
 * 创建一个显卡可执行程序，运行在GPU
 * */
fun createProgram(vertexSource: String, fragmentSource: String): Int {
    val ints = IntArray(1)
    GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_ATTRIBS, ints, 0)
    Log.d(TAG, "create program max vertex attribs : ${ints[0]}")

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
    if (program == 0) {
        throw RuntimeException("create GPU program failed")
    }
    GLES20.glUseProgram(program)
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

fun getAttribLocation(program: Int, name: String): Int {
    val location = GLES20.glGetAttribLocation(program, name)
    checkLocation(location, name)
    return location
}

fun getUniformLocation(program: Int, name: String): Int {
    val uniform = GLES20.glGetUniformLocation(program, name)
    checkLocation(uniform, name)
    return uniform
}

fun checkEglError(msg: String) {
    val error: Int = EGL14.eglGetError()
    if (error != EGL14.EGL_SUCCESS) {
        throw RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error))
    }
}

fun checkGlError(op: String) {
    val error: Int = GLES20.glGetError()
    if (error != GLES20.GL_NO_ERROR) {
        throw RuntimeException("$op: glError $error and msg is ${Integer.toHexString(error)}")
    }
}

fun checkFrameBuffer(){
    val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
    if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
        throw RuntimeException("Frame Buffer is not complete")
    }
}

fun checkLocation(location: Int, label: String) {
    if (location < 0) {
        throw RuntimeException("Unable to locate '$label' in program")
    }
}

fun glVersion(): Int {
    val am = BaseApplication.instance.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val info = am.deviceConfigurationInfo ?: return 0
    return info.reqGlEsVersion
}

fun enableVertexAttrib(handle: Int) {
    GLES20.glEnableVertexAttribArray(handle)
}

fun disableVertexAttrib(handle: Int) {
    GLES20.glDisableVertexAttribArray(handle)
}

private const val PBO_SUPPORT_VERSION = 0x30000

fun isSupportPBO(): Boolean {
    return glVersion() > PBO_SUPPORT_VERSION
}