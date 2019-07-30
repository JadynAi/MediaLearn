package com.jadyn.mediakit.gl

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLES30

/**
 *@version:
 *@FileDescription: 纹理相关的协助类
 *@Author:Jing
 *@Since:2019/4/10
 *@ChangeList:
 */

/**
 * 生成帧缓冲
 * */
fun buildFrameBuffer(): Int {
    val frames = IntArray(1)
    GLES20.glGenFramebuffers(1, frames, 0)
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frames[0])
    checkGlError("create frame buffer check")
    return frames[0]
}

fun appendFBOTexture(textureId: Int, target: Int = GLES11Ext.GL_TEXTURE_EXTERNAL_OES) {
    GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
            GLES20.GL_COLOR_ATTACHMENT0, target, textureId, 0)
    unBindTexture(target)
    unBindFrameBuffer()
}

/**
 * 创建一个纹理对象，并且和ES绑定
 *
 * 生成camera特殊的Texture
 * 在Android中Camera产生的preview texture是以一种特殊的格式传送的，
 * 因此shader里的纹理类型并不是普通的sampler2D,而是samplerExternalOES,
 * 在shader的头部也必须声明OES 的扩展。除此之外，external OES的纹理和Sampler2D在使用时没有差别
 * */
fun buildTextureId(target: Int = GLES11Ext.GL_TEXTURE_EXTERNAL_OES): Int {
    val ids = IntArray(1)
    GLES20.glGenTextures(1, ids, 0)
    checkGlError("create texture check")
    val id = ids[0]
    bindSetTexture(target, id)
    return id
}

private fun bindSetTexture(target: Int, id: Int) {
    // 这里的绑定纹理是将GPU的纹理数据和ID对应起来，载入纹理到此ID处
    // 渲染时绑定纹理，是绑定纹理ID到激活的纹理单元
    GLES20.glBindTexture(target, id)
    checkGlError("bind texture : $id check")

    //----设置纹理参数----
    // 纹理过滤器放大缩小
    GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)

    GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
    GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
}

fun unBindTexture(target: Int = GLES11Ext.GL_TEXTURE_EXTERNAL_OES) {
    GLES20.glBindTexture(target, GLES20.GL_NONE)
}

fun unBindFrameBuffer() {
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_NONE)
}

fun unBindFrameBuffer3() {
    GLES30.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_NONE)
}

/**
 * 释放纹理
 * */
fun releaseTexture(id: IntArray) {
    GLES20.glDeleteTextures(id.size, id, 0)
}

/**
 * 释放帧缓冲和纹理
 * */
fun releaseFrameBufferTexture(frame: IntArray, textureId: IntArray) {
    GLES20.glDeleteFramebuffers(1, frame, 0)
    releaseTexture(textureId)
}

fun releaseFrameBufferTexture(frame: Int, textureId: Int) {
    releaseFrameBufferTexture(intArrayOf(frame), intArrayOf(textureId))
}