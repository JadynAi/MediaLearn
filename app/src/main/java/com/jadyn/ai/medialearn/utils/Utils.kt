package com.jadyn.ai.medialearn.utils

import android.util.Log
import kotlin.experimental.and

/**
 *@version:
 *@FileDescription:
 *@Author:jing
 *@Since:2018/12/4
 *@ChangeList:
 */
fun rgbaToYuv(rgba: ByteArray, width: Int, height: Int, yuv: ByteArray) {
    val frameSize = width * height

    var yIndex = 0
    var uIndex = frameSize
    var vIndex = frameSize + frameSize / 4

    var R: Int
    var G: Int
    var B: Int
    var Y: Int
    var U: Int
    var V: Int
    var index = 0
    for (j in 0 until height) {
        for (i in 0 until width) {
            index = j * width + i
            if (rgba[index * 4] > 127 || rgba[index * 4] < -128) {
                Log.e("color", "-->" + rgba[index * 4])
            }
            R = (rgba[index * 4] and 0xFF.toByte()).toInt()
            G = (rgba[index * 4 + 1] and 0xFF.toByte()).toInt()
            B = (rgba[index * 4 + 2] and 0xFF.toByte()).toInt()

            Y = (66 * R + 129 * G + 25 * B + 128 shr 8) + 16
            U = (-38 * R - 74 * G + 112 * B + 128 shr 8) + 128
            V = (112 * R - 94 * G - 18 * B + 128 shr 8) + 128

            yuv[yIndex++] = (if (Y < 0) 0 else if (Y > 255) 255 else Y).toByte()
            if (j % 2 == 0 && index % 2 == 0) {
                yuv[uIndex++] = (if (U < 0) 0 else if (U > 255) 255 else U).toByte()
                yuv[vIndex++] = (if (V < 0) 0 else if (V > 255) 255 else V).toByte()
            }
        }
    }
}