package com.jadyn.ai.medialearn.encode

import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import androidx.appcompat.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.TextureView
import com.jadyn.ai.medialearn.R
import com.jadyn.mediakit.video.encode.VideoEncoder
import kotlinx.android.synthetic.main.activity_encode_frame.*
import java.io.File

/**
 *@version:
 *@FileDescription: 将帧编码为视频
 *@Author:Jing
 *@Since:2019/4/2
 *@ChangeList:
 */
class EncodeFrameActivity : AppCompatActivity() {

    // 测试文件夹
    private val encodePicDir = TextUtils.concat(Environment.getExternalStorageDirectory().path,
            "/decode").toString()


    private val thread by lazy {
        val handlerThread = HandlerThread("encodeFrame")
        handlerThread.start()
        handlerThread
    }

    private val handler by lazy {
        Handler(thread.looper)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_encode_frame)
        frame_texture_view.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
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

        handler.post {
            Log.d("fuck", "thread ${Thread.currentThread().name} ")
            val videoEncoder = VideoEncoder(640, 480, 1800000,
                    24)
            videoEncoder.start(Environment.getExternalStorageDirectory().path
                    + "/encodeyazi640${videoEncoder.bitRate}.mp4")
            val file = File(encodePicDir)
            file.listFiles().forEachIndexed { index, it ->
                Log.d("fuck", " file name ${it.absolutePath}  path is ${it.path}")
                BitmapFactory.decodeFile(it.path)?.apply {
                    videoEncoder.drainFrame(this, index)
                }
            }
            videoEncoder.drainEnd()
        }
    }
}


