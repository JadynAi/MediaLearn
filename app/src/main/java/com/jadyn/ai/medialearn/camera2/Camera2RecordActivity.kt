package com.jadyn.ai.medialearn.camera2

import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Size
import android.view.Surface
import android.view.TextureView
import com.jadyn.ai.medialearn.R
import com.jadyn.mediakit.camera2.CameraMgr
import kotlinx.android.synthetic.main.activity_camera2_record.*

/**
 *@version:
 *@FileDescription:
 *@Author:Jing
 *@Since:2019-05-07
 *@ChangeList:
 */
class Camera2RecordActivity : AppCompatActivity() {

    private lateinit var cameraMgr: CameraMgr

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera2_record)

        toggle_full_screen.setOnClickListener {
            toggle_full_screen.isSelected = !toggle_full_screen.isSelected
            camera2_record_texture.toggleFullscreen()
        }

        camera2_record_texture.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                return false
            }

            override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
                cameraMgr = CameraMgr(this@Camera2RecordActivity, Size(width, height))
                // texture view 自动配置宽高
                camera2_record_texture.setAspectRatio(cameraMgr.previewSize.height,
                        cameraMgr.previewSize.width)
                configureTextureTransform(camera2_record_texture.width,
                        camera2_record_texture.height)
                cameraMgr.openCamera(Surface(surface))
                surface?.setDefaultBufferSize(cameraMgr.previewSize.height,
                        cameraMgr.previewSize.width)
            }
        }
    }

    private fun configureTextureTransform(viewWidth: Int, viewHeight: Int) {
        val rotation = windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, cameraMgr.previewSize.height.toFloat(),
                cameraMgr.previewSize.width.toFloat())
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