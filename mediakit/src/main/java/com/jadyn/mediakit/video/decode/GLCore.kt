package com.jadyn.mediakit.video.decode

import android.graphics.Bitmap
import android.media.MediaCodec
import android.view.Surface
import com.jadyn.mediakit.gl.OutputSurface
import java.io.File

/**
 *@version:
 *@FileDescription:
 *@Author:jing
 *@Since:2019/2/18
 *@ChangeList:
 */
class GLCore : DecodeCore() {

    private var outputSurface: OutputSurface? = null

    override fun fkOutputSurface(width: Int, height: Int): Surface? {
        if (outputSurface == null) {
            outputSurface = OutputSurface(width, height)
        }
        return outputSurface!!.surface
    }

    override fun codeToFrame(bufferInfo: MediaCodec.BufferInfo, outputBufferId: Int, outputFrameCount: Int,
                             decoder: MediaCodec): Int {
        outputSurface?.apply {
            val doRender = bufferInfo.size != 0
            // CodeC搭配输出Surface时，调用此方法将数据及时渲染到Surface上
            decoder.releaseOutputBuffer(outputBufferId, doRender)
            if (doRender) {
                // 2019/2/14-15:24 必须和surface创建时保持统一线程
                awaitNewImage()
                drawImage(true)

                val file = File(outputDir, String.format("frame-%02d.jpg", outputFrameCount))
                saveFrame(file.toString())
                return outputFrameCount
            }
            return -1
        }
        return -1
    }

    override fun codeFrameBitmap(bufferInfo: MediaCodec.BufferInfo, outputBufferId: Int,
                                 decoder: MediaCodec, ob: (Bitmap) -> Unit) {
        outputSurface?.apply {
            val doRender = bufferInfo.size != 0
            // CodeC搭配输出Surface时，调用此方法将数据及时渲染到Surface上
            decoder.releaseOutputBuffer(outputBufferId, doRender)
            if (doRender) {
                // 2019/2/14-15:24 必须和surface创建时保持统一线程
                awaitNewImage()
                drawImage(true)
                ob.invoke(produceBitmap())
            }
        }
    }

    override fun release() {
        outputSurface?.release()
    }
}