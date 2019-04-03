package com.jadyn.mediakit.gl

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Size
import com.jadyn.mediakit.function.getByteBuffer

/**
 *@version:
 *@FileDescription: 创建一个2D纹理，显卡可执行程序Program
 *@Author:Jing
 *@Since:2019/3/27
 *@ChangeList:
 */
class Texture2dProgram {

    private var textureId: Int = 0

    private val textureDraw: TextureDraw

    init {
        val program = createTexture2DProgram()
        textureDraw = TextureDraw(program)
    }

    /**
     * 创建一个纹理对象，并且和ES绑定
     *
     * 生成camera特殊的Texture
     * 在Android中Camera产生的preview texture是以一种特殊的格式传送的，
     * 因此shader里的纹理类型并不是普通的sampler2D,而是samplerExternalOES,
     * 在shader的头部也必须声明OES 的扩展。除此之外，external OES的纹理和Sampler2D在使用时没有差别
     * */
    fun genTextureId(): Int {
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        checkGlError("create texture check")
        val id = ids[0]
        textureId = id
        bindSetTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, id)
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

    /**
     * 将bitmap数据上传到这个纹理对象
     * */
    fun uploadBitmapToTexture(bitmap: Bitmap, targetSize: Size) {
        val scaleBitmap = Bitmap.createBitmap(bitmap, 0, 0, targetSize.width, targetSize.height)
        val byteBuffer = scaleBitmap.getByteBuffer()
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                targetSize.width, targetSize.height, 0, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, byteBuffer)
        textureDraw.drawTextureToWindow(targetSize, textureId)
    }

    fun drawFrame(st: SurfaceTexture, invert: Boolean) {
        textureDraw.drawFromSurfaceTexture(st, invert, textureId)
    }
}