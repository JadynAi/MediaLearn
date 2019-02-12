package com.jadyn.ai.medialearn.decode

import android.media.MediaExtractor
import android.media.MediaFormat
import com.jadyn.ai.medialearn.codec.selectVideoTrack
import com.jadyn.ai.medialearn.utils.duration
import com.jadyn.ai.medialearn.utils.fps

/**
 *@version:
 *@FileDescription:
 *@Author:jing
 *@Since:2019/2/12
 *@ChangeList:
 */
class VideoAnalyze(dataSource: String) {

    val mediaExtractor by lazy {
        MediaExtractor()
    }

    val mediaFormat: MediaFormat

    init {
        mediaExtractor.setDataSource(dataSource)
        val trackIndex = mediaExtractor.selectVideoTrack()
        if (trackIndex < 0) {
            throw RuntimeException("this data source not video")
        }
        mediaExtractor.selectTrack(trackIndex)
        mediaFormat = mediaExtractor.getTrackFormat(trackIndex)
    }

    /*
    * 计算总帧数
    * duration 是微秒，转换成秒再计算
    * */
    fun calculateFramesCount(): Int {
        return ((mediaFormat.duration.toFloat() / 1000000f) * mediaFormat.fps).toInt()
    }

}