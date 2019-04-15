package com.jadyn.mediakit.gl

import android.opengl.GLES11Ext
import android.opengl.GLES20

/**
 *@version:
 *@FileDescription: 纹理相关的协助类
 *@Author:Jing
 *@Since:2019/4/10
 *@ChangeList:
 */


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
    GLES20.glBindTexture(target, id)
    checkGlError("bind texture : $id check")

    //----设置纹理参数----
    // 纹理过滤器放大缩小
    GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)

    GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
    GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
}

fun unBindTexture() {
    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
}