package com.jadyn.ai.medialearn.camera2

import android.Manifest
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import com.jadyn.ai.medialearn.R
import com.jadyn.ai.medialearn.permissions.RxPermissions
import com.jadyn.mediakit.camera2.CameraMgr
import com.jadyn.mediakit.camera2.VideoGen
import kotlinx.android.synthetic.main.activity_camera2_record.*
import java.io.File
import java.lang.ref.WeakReference
import java.util.*

/**
 *@version:
 *@FileDescription:
 *@Author:Jing
 *@Since:2019-05-07
 *@ChangeList:
 */
class Camera2RecordActivity : AppCompatActivity() {

    private lateinit var cameraMgr: CameraMgr

    private val surfaceListener by lazy {
        object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                return false
            }

            override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
                openCamera2(width, height)
            }
        }
    }

    private val videoRecorder by lazy {
        //        1080, 1920, 4000000
        VideoGen()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera2_record)

        toggle_full_screen.setOnClickListener {
            toggle_full_screen.isSelected = !toggle_full_screen.isSelected
            camera2_record_texture.toggleFullscreen()
        }

        record_video.setOnClickListener {
            record_time.visibility = View.GONE
            RxPermissions(this).request(Manifest.permission.RECORD_AUDIO).doOnNext {
                val instance = Calendar.getInstance()
                val file = File(Environment.getExternalStorageDirectory(),
                        "test${instance.get(Calendar.DAY_OF_MONTH)}" +
                                ":${instance.get(Calendar.HOUR_OF_DAY)}" +
                                ":${instance.get(Calendar.MINUTE)}.mp4")
                
                videoRecorder.start(720, 1280, 8000000, surfaceCallback = {
                    // 录制的时候根据，定制的视频尺寸，重新调整PreviewSurface的数据
                    cameraMgr.startRecord(it)
                    runOnUiThread {
                        record_video.visibility = View.GONE
                        take_photo.visibility = View.GONE
                        stop_video.visibility = View.VISIBLE
                    }
                }, outputPath = file.toString())
            }.subscribe()
        }

        take_photo.setOnClickListener {
        }

        stop_video.setOnClickListener {
            videoRecorder.stop()
            cameraMgr.stopRecord { t ->
                runOnUiThread {
                    record_time.visibility = View.VISIBLE
                    record_time.text = t.toString()
                }
            }
            record_video.visibility = View.VISIBLE
            take_photo.visibility = View.VISIBLE
            stop_video.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        if (camera2_record_texture.isAvailable) {
            openCamera2(camera2_record_texture.width, camera2_record_texture.height)
        } else {
            camera2_record_texture.surfaceTextureListener = surfaceListener
        }
    }

    private fun openCamera2(width: Int, height: Int) {
        if (!::cameraMgr.isInitialized) {
            cameraMgr = CameraMgr(WeakReference(this@Camera2RecordActivity), Size(width, height))
        }
        val texture = camera2_record_texture.surfaceTexture
        // texture view 自动配置宽高
        camera2_record_texture.setAspectRatio(cameraMgr.previewSize)
        configureTextureTransform(camera2_record_texture.width,
                camera2_record_texture.height)
        cameraMgr.openCamera(Surface(texture))
        texture?.setDefaultBufferSize(cameraMgr.previewSize.width,
                cameraMgr.previewSize.height)
    }


    private fun configureTextureTransform(viewWidth: Int, viewHeight: Int) {
        val rotation = windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, cameraMgr.previewSize.width.toFloat(),
                cameraMgr.previewSize.height.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            val scale = Math.max(
                    viewHeight.toFloat() / cameraMgr.previewSize.height,
                    viewWidth.toFloat() / cameraMgr.previewSize.width)
            with(matrix) {
                setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
                postScale(scale, scale, centerX, centerY)
                postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
            }
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        camera2_record_texture.setTransform(matrix)
    }
}