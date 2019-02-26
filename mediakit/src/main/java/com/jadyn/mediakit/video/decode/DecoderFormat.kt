package com.jadyn.mediakit.video.decode

import android.media.Image

/**
 *@version:
 *@FileDescription: 解码输出类型，目前只支持JPEG图片文件
 *@Author:jing
 *@Since:2019/2/9
 *@ChangeList:
 */
enum class DecoderFormat(type: String) {
    JPG("JPEG"),
    I420("I420"),
    NV21("NV21")
}

enum class ColorFormat {
    I420,
    NV21
}

/*
* 获得对应格式输出的每一帧文件名
* */
fun DecoderFormat.outputFrameFileName(parentDir: String, frame: Int): String {
    return when {
        this == DecoderFormat.JPG -> parentDir + String.format("frame-%02d.jpg", frame)
        this == DecoderFormat.I420 -> ""
        else -> ""
    }
}

/*
* 将对应的输出格式压缩到对应的文件
* */
fun DecoderFormat.compressCorrespondingFile(fileName: String, image: Image): Boolean {
    return when {
        this == DecoderFormat.JPG -> {
            compressToJpeg(fileName, image)
        }
        this == DecoderFormat.I420 -> {
            false
        }
        else -> {
            false
        }
    }
}