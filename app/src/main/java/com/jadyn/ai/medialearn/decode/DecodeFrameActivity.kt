package com.jadyn.ai.medialearn.decode

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.TextureView
import android.widget.SeekBar
import com.jadyn.ai.medialearn.R
import com.jadyn.mediakit.function.durationSecond
import com.jadyn.mediakit.video.decode.VideoAnalyze
import com.jadyn.mediakit.video.decode.VideoDecoder2
import kotlinx.android.synthetic.main.activity_decode_frame.*
import java.util.*

/**
 *@version:
 *@FileDescription:
 *@Author:jing
 *@Since:2019/2/19
 *@ChangeList:
 */
class DecodeFrameActivity : AppCompatActivity() {

    private val decodeMP4Path = TextUtils.concat(Environment.getExternalStorageDirectory().path,
            "/yazi.mp4").toString()

    private var videoAnalyze: VideoAnalyze? = null

    private var videoDecoder2: VideoDecoder2? = null

    private val q by lazy { 
        Stack<Int>()
    }

    private val thread by lazy {
        HandlerThread("decoder")
    }

    private val handler by lazy {
        thread.start()
        Handler(thread.looper)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_decode_frame)
        file_frame_et.setText(decodeMP4Path)

        sure_video.setOnClickListener {
            val dataSource = file_frame_et.text.toString()
            if (videoAnalyze != null && videoAnalyze!!.dataSource.equals(dataSource)) {
                return@setOnClickListener
            }
            videoAnalyze = VideoAnalyze(dataSource)
            video_seek.max = videoAnalyze!!.mediaFormat.durationSecond
            video_seek.progress = 0

            videoDecoder2 = VideoDecoder2(dataSource)
            updateTime(0, video_seek.max)
        }
        
        texture_view.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                return false
            }

            override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            }
        }

        video_seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                videoAnalyze?.apply {
                    updateTime(progress, mediaFormat.durationSecond)
                }
                videoDecoder2?.apply {
                    getFrame(seekBar.progress.toFloat(), {
                        frame_img.setImageBitmap(it)
                    }, {
                        Log.d("cece", "throwable ${it.message}: ")
                    })
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                q.push(seekBar.progress)
                Log.d("cece", " q ${q.toString()} : ${q.peek()} ")
            }
        })

    }

    private fun updateTime(progress: Int, max: Int) {
        time.text = "现在 : $progress 总时长为 : $max"
    }

    override fun onDestroy() {
        super.onDestroy()
        videoDecoder2?.release()
        
        thread.quitSafely()
        handler.removeCallbacksAndMessages(null)
    }
}