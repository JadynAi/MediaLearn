package com.jadyn.mediakit.gl

import android.graphics.Bitmap
import android.opengl.GLES20
import android.util.Size
import com.jadyn.mediakit.function.checkGlError
import com.jadyn.mediakit.function.getByteBuffer

/**
 *@version:
 *@FileDescription: 创建一个2D纹理，显卡可执行程序Program
 *@Author:Jing
 *@Since:2019/3/27
 *@ChangeList:
 */
class Texture2dProgram {

    init {
        createTexture2DProgram()
    }

    /**
     * 创建一个纹理对象，并且和ES绑定
     * */
    fun genTextureId(): Int {
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        checkGlError("create texture check")
        val id = ids[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id)
        checkGlError("bind texture : $id check")

        //----设置纹理参数----

        // 纹理过滤器放大缩小
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        return id
    }

    /**
     * 将bitmap渲染到纹理上
     * */
    fun renderTexture(bitmap: Bitmap, targetSize: Size) {
        val scaleBitmap = Bitmap.createBitmap(bitmap, 0, 0, targetSize.width, targetSize.height)
        val byteBuffer = scaleBitmap.getByteBuffer()
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                targetSize.width, targetSize.height, 0, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, byteBuffer)
    }

    fun unBindTexture() {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }
}