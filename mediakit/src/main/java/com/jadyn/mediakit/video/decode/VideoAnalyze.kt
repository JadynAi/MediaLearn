package com.jadyn.mediakit.video.decode

import android.media.MediaExtractor
import android.media.MediaFormat
import android.support.annotation.IntRange
import com.jadyn.mediakit.function.duration
import com.jadyn.mediakit.function.fps
import com.jadyn.mediakit.function.minDifferenceValue
import com.jadyn.mediakit.function.selectVideoTrack

/**
 *@version:
 *@FileDescription:
 *@Author:jing
 *@Since:2019/2/12
 *@ChangeList:
 */
class VideoAnalyze(val dataSource: String) {

    val mediaExtractor by lazy {
        MediaExtractor()
    }

    val checkExtractor by lazy {

        MediaExtractor().apply {
            setDataSource(dataSource)
            val trackIndex = selectVideoTrack()
            selectTrack(trackIndex)
        }
    }

    val mediaFormat: MediaFormat

    val firstFrameTime by lazy {
        //得到第一个关键帧的时间点
        checkExtractor.seekTo(0L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        checkExtractor.sampleTime
    }

    val lastFrameTime by lazy {
        //测试过，MediaExtractor video末端的sampleTime会大于duration，用这个方法来得到最后一帧的时间点
        checkExtractor.seekTo(checkExtractor.getTrackFormat(checkExtractor.selectVideoTrack()).duration,
                MediaExtractor.SEEK_TO_NEXT_SYNC)
        var l = mediaExtractor.sampleTime
        var end = false
        while (!end) {
            mediaExtractor.advance()
            val sampleTime = mediaExtractor.sampleTime
            if (sampleTime != -1L) {
                l = sampleTime
            } else {
                end = true
            }
        }
        l
    }

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
    val totalFramesCount by lazy {
        (mediaFormat.duration.toFloat() / 1000000f * mediaFormat.fps).toInt()
    }


    /*
    * 
    * return : 每一帧持续时间，微秒
    * */
    val perFrameTime by lazy {
        1000000L / mediaFormat.fps
    }


    /*
    * 比较两个时间戳是否为同一帧
    * fixedTime: 目标时间戳
    * sampleTime:被比较时间戳
    * */
    fun sameFrame(fixedTime: Long, sampleTime: Long): Boolean {
        return sampleTime >= fixedTime - firstFrameTime && sampleTime < fixedTime + perFrameTime - firstFrameTime
    }

    /*
    * 
    * 查找这个时间点对应的最接近的一帧
    * 
    * maxRange:查找范围
    * */
    fun getValidSampleTime(time: Long, @IntRange(from = 2) maxRange: Int = 5): Long {
        checkExtractor.seekTo(time, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        var count = 0
        var sampleTime = checkExtractor.sampleTime
        while (count < maxRange) {
            checkExtractor.advance()
            val s = checkExtractor.sampleTime
            if (s != -1L) {
                count++
                sampleTime = time.minDifferenceValue(sampleTime, s)
            } else {
                count = maxRange
            }
        }

        return sampleTime
    }

    fun getLaterTime(time: Long, range: Int): ArrayList<Long> {
        checkExtractor.seekTo(time, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        checkExtractor.advance()
        val list = arrayListOf<Long>()
        if (checkExtractor.sampleTime == -1L) {
            return list
        }
        for (i in 0 until range) {
            val element = checkExtractor.sampleTime
            if (element == -1L) {
                return list
            }
            list.add(element)
            checkExtractor.advance()
        }
        return list
    }

    fun release() {
        mediaExtractor.release()
        checkExtractor.release()
    }

}