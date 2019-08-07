package com.jadyn.mediakit.camera2

import android.media.MediaCodec

/**
 *@version:
 *@FileDescription: 视频帧储存对象
 *@Author:Jing
 *@Since:2019-05-20
 *@ChangeList:
 */
class VideoPacket(val buffer: ByteArray,
                  val size: Int,
                  val timeMills: Long,
                  val duration: Int,
                  val bufferInfo: MediaCodec.BufferInfo)
