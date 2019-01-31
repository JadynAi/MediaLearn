package com.jadyn.ai.medialearn.camera

import android.Manifest
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.os.Bundle
import android.os.HandlerThread
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.TextureView
import com.jadyn.ai.medialearn.R
import com.jadyn.ai.medialearn.codec.AiLoiVideoEncoder
import com.jadyn.ai.medialearn.permissions.RxPermissions
import kotlinx.android.synthetic.main.activity_camera.*

/**
 *@version:
 *@FileDescription:
 *@Author:jing
 *@Since:2018/11/27
 *@ChangeList:
 */
class CameraActivity : AppCompatActivity() {

    private val TAG = "CameraTest"

    private var camera: Camera? = null

    private var isRecord = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        tv_record_camera.setOnClickListener {
            isRecord = !isRecord
            if (isRecord) {
                startRecord()
            } else {
                arcVideoEncoder.stop()
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                return false
            }

            override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
                prepareCamera(width, height)
            }
        }
    }


    private fun prepareCamera(width: Int, height: Int) {
        if (camera != null) {
            return
        }
        val result = RxPermissions(this).request(Manifest.permission.CAMERA).blockingSingle()
        if (!result) {
            return
        }
        val info = Camera.CameraInfo()
        val numCameras = Camera.getNumberOfCameras()
        for (i in 0 until numCameras) {
            Camera.getCameraInfo(i, info)
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                camera = Camera.open()
                break
            }
        }

        if (camera == null) {
            camera = Camera.open()
        }

        if (camera == null) {
            throw RuntimeException("camera start failed")
        }
        val parms = camera!!.parameters
        choosePreviewSize(parms, width, height)
        camera!!.setDisplayOrientation(90)
        camera!!.parameters = parms
        val size = parms.previewSize
        Log.d(TAG, "Camera preview size is " + size.width + "x" + size.height)
        camera!!.setPreviewTexture(textureView.surfaceTexture)
        camera!!.startPreview()
    }

    private val arcVideoEncoder by lazy {
        AiLoiVideoEncoder(camera!!.parameters.previewSize.width, camera!!.parameters.previewSize.height,
                600000)
    }

    private fun startRecord() {
        Log.d(TAG, ":startRecord ")
        val recordThread = object : HandlerThread("Record") {
            override fun run() {
                super.run()
                Log.d(TAG, ": ")
                val createSurface = arcVideoEncoder.createTexture(camera!!.parameters.previewSize.width,
                        camera!!.parameters.previewSize.height)
                releaseCamera()
                camera!!.setPreviewTexture(createSurface)
                arcVideoEncoder.start()
                camera!!.startPreview()
            }
        }
        recordThread.start()
    }

    private fun releaseCamera() {
        Log.d(TAG, "releasing camera")
        camera?.apply {
            stopPreview()
            release()
        }
    }
}