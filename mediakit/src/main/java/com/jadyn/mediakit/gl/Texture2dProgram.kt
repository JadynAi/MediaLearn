package com.jadyn.mediakit.gl

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.util.Log
import android.util.Size

/**
 *@version:
 *@FileDescription: 创建显卡可执行程序Program
 *@Author:Jing
 *@Since:2019/3/27
 *@ChangeList:
 */
class Texture2dProgram {

    private var textureId: Int = 0

    private val textureDraw: TextureDraw

    init {
        Log.d(this.javaClass.name, " create texture 2d program ")
        val program = createTexture2DProgram()
        textureDraw = TextureDraw(program)
    }

    fun genTextureId(): Int {
        textureId = buildTextureId()
        return textureId
    }

    fun drawFrame(st: SurfaceTexture, invert: Boolean) {
        textureDraw.drawFromSurfaceTexture(st, invert, textureId)
    }

    fun drawBitmap(b: Bitmap, targetSize: Size) {
        textureDraw.drawBitmap(b, targetSize, textureId)
    }
}