package com.jadyn.ai.medialearn.codec

import android.opengl.EGL14
import android.opengl.GLES20

/**
 *@version:
 *@FileDescription:
 *@Author:jing
 *@Since:2018/12/5
 *@ChangeList:
 */
fun checkEglError(msg: String) {
    val error: Int = EGL14.eglGetError()
    if (error != EGL14.EGL_SUCCESS) {
        throw RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error))
    }
}

fun checkGlError(op: String) {
    var error: Int = GLES20.glGetError()
    while (error != GLES20.GL_NO_ERROR) {
        error = GLES20.glGetError()
        throw RuntimeException("$op: glError $error")
    }
}

fun computePresentationTimeNsec(frameIndex: Int, frameRate: Int): Long {
    val ONE_BILLION: Long = 1000000000
    return frameIndex * ONE_BILLION / frameRate
}

fun checkLocation(location: Int, label: String) {
    if (location < 0) {
        throw RuntimeException("Unable to locate '$label' in program")
    }
}