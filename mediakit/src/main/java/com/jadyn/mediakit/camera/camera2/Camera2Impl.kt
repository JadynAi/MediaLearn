package com.jadyn.mediakit.camera.camera2

import android.app.Activity
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import com.jadyn.mediakit.camera.ICamera
import com.jadyn.mediakit.camera2.CameraIDC
import java.lang.ref.SoftReference

/**
 *@version:
 *@FileDescription:camera2 business realize
 *@Author:Jing
 *@Since:2019-09-06
 *@ChangeList:
 */
class Camera2Impl(private val activity: SoftReference<Activity>) : ICamera {

    private lateinit var cameraMgr: CameraManager
    private lateinit var cameraIDC: CameraIDC

    override fun openCamera(): Boolean {
        val act = activity.get()
        if (act == null || act.isFinishing) return false
        if (!::cameraMgr.isInitialized) {
            cameraMgr = act.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            try {
                cameraIDC = CameraIDC(cameraMgr)
                val characteristics = cameraMgr.getCameraCharacteristics(cameraIDC.curID)
//
//                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
//
//                // 选出最大的size,比较方式为 width*height 值的大小 
//                val largest = Collections.max(Arrays.asList(*map.getOutputSizes(ImageFormat.JPEG)),
//                        CompareSizesByArea())
//
//                // 预览的宽高需要根据此时屏幕的旋转角度，以及设备自身的“调整角度”来配合
//                val displayRotation = act.windowManager.defaultDisplay.rotation
//                sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
//                val swappedDimensions = areDimensionsSwapped(displayRotation, sensorOrientation)
//
//                val displaySize = Point(screenWidth, screenHeight)
//                val rotatedPreviewSize = if (swappedDimensions) size.swap() else size
//                val displaySize1 = Size(displaySize.x, displaySize.y)
//                var maxPreviewSize = if (swappedDimensions) displaySize1.swap() else displaySize1
//                maxPreviewSize = maxPreviewSize.maxChoose(DEF_MAX_PREVIEW_SIZE)
//
//                previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture::class.java),
//                        rotatedPreviewSize.width, rotatedPreviewSize.height,
//                        maxPreviewSize.width, maxPreviewSize.height, largest, displayRotation)
//                flashSupported =
//                        characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
//                Log.d(TAG, "display rotation $displayRotation  sensor $sensorOrientation: ")
//                Log.d(TAG, "preview size $previewSize  largest size is $largest")
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }
        return true
    }

    override fun startPreview(surfaceTexture: SurfaceTexture) {
    }

    override fun stopPreview() {
    }

    override fun startRecord(surfaceTexture: SurfaceTexture) {

    }

    override fun stopRecord() {
    }

    override fun takePhoto() {
    }

    override fun switchCamera() {

    }

    override fun release() {
    }

}