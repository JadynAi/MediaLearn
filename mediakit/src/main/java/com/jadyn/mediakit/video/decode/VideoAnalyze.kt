package com.jadyn.mediakit.video.decode

import android.media.MediaExtractor
import android.media.MediaFormat
import com.jadyn.mediakit.function.duration
import com.jadyn.mediakit.function.fps
import com.jadyn.mediakit.function.selectVideoTrack

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
        // 查看是否含有视频轨
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

    fun release() {
        mediaExtractor.release()
    }

}