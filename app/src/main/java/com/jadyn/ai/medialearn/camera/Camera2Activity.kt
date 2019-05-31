package com.jadyn.ai.medialearn.camera

import android.Manifest
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.v7.app.AppCompatActivity
import android.util.Size
import android.view.Surface
import android.view.TextureView
import com.jadyn.ai.medialearn.R
import com.jadyn.ai.medialearn.codec.AiLoiVideoEncoder
import com.jadyn.ai.medialearn.permissions.RxPermissions
import kotlinx.android.synthetic.main.activity_camera2.*

val TAG = "cece"

class Camera2Activity : AppCompatActivity() {

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var camera2Ops: Camera2Ops? = null

    private lateinit var aiLoiVideoEncoder: AiLoiVideoEncoder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera2)
        tv_record.setOnClickListener {
            RxPermissions(this).request(Manifest.permission.RECORD_AUDIO).doOnNext {
                camera2Ops?.toggleRecord(aiLoiVideoEncoder)
            }.subscribe()
        }
        RxPermissions(this).request(
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .doOnNext { result ->
                    texture_view.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
                        }

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
                        }

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                            camera2Ops?.release()
                            backgroundThread?.quitSafely()
                            return false
                        }

                        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
//                            startBackgroundThread()
                            camera2Ops = Camera2Ops(this@Camera2Activity, Size(width, height)) { c, s ->

                                // 2019/1/2-17:38 竖屏视频使用高作宽，宽作高
                                aiLoiVideoEncoder = AiLoiVideoEncoder(s.height, s.width, 2000000)
                                this@Camera2Activity.texture_view.setAspectRatio(s.height, s.width)
                                c.openCamera(Surface(texture_view.surfaceTexture.apply {
                                    setDefaultBufferSize(s.height, s.width)
                                }))

                                c.recordPrepared = {
                                    this@Camera2Activity.runOnUiThread {
                                        this@Camera2Activity.tv_record.text = "录制中"
                                    }
                                }
                                c.recordStopped = {
                                    this@Camera2Activity.runOnUiThread {
                                        this@Camera2Activity.tv_record.text = "录像"
                                    }
                                }
                            }
                        }
                    }
                    texture_view.keepScreenOn = true
                }
                .subscribe()
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground")
        backgroundThread?.start()
        backgroundHandler = Handler(backgroundThread?.looper)
    }
}
