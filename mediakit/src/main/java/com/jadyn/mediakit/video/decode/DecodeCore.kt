package com.jadyn.mediakit.video.decode

import android.graphics.Bitmap
import android.media.MediaCodec
import android.view.Surface

/**
 *@version:
 *@FileDescription: 视频帧处理核心类
 *@Author:jing
 *@Since:2019/2/18
 *@ChangeList:
 */
abstract class DecodeCore {

    protected val TAG = this.javaClass.name

    /*
    * 为MediaCodeC配置一个可为null的Surface，作为输出数据承载体
    * */
    open fun fkOutputSurface(width: Int, height: Int): Surface? = null

    protected var outputDir: String = ""
        private set

    fun configure(outputDir: String) {
        this.outputDir = outputDir
    }

    /*
    * 视频帧编码为图片
    * 
    * return : 成功编码的图片帧index。否则返回-1
    * */
    abstract fun codeToFrame(bufferInfo: MediaCodec.BufferInfo, outputBufferId: Int,
                             outputFrameCount: Int, decoder: MediaCodec): Int

    abstract fun codeFrameBitmap(bufferInfo: MediaCodec.BufferInfo, outputBufferId: Int,
                                 decoder: MediaCodec, ob: (Bitmap) -> Unit)

    abstract fun release()
}