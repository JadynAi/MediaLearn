package com.jadyn.mediakit.camera2

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.Surface
import com.jadyn.mediakit.function.*
import java.util.*

/**
 *@version:
 *@FileDescription:
 *@Author:Jing
 *@Since:2019-05-07
 *@ChangeList:
 */
class CameraMgr(private val activity: Activity, size: Size) {

    private val TAG = "Camera2Ops"

    private val DEF_MAX_PREVIEW_SIZE = Size(1920, 1080)

    private val cameraMgr by lazy {
        activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private var sensorOrientation = 0

    //是否支持闪光灯
    private var flashSupported = false

    lateinit var previewSize: Size
        private set
    private lateinit var cameraId: String

    private val UIHandler by lazy {
        Handler(Looper.getMainLooper())
    }

    private var previewSurface: Surface? = null
    private var cameraDevice: CameraDevice? = null
    private var builder: CaptureRequest.Builder? = null
    private var cameraSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private val stateCallback by lazy {
        object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice?) {
                Log.d(TAG, " open: ")
                cameraDevice = camera
                startPreview()
            }

            override fun onDisconnected(camera: CameraDevice?) {
                Log.d(TAG, " onDisconnected ")
                cameraDevice?.close()
                cameraDevice = null
            }

            override fun onError(camera: CameraDevice?, error: Int) {
                Log.d(TAG, " onError: $error ")
            }

        }
    }

    init {
        // 计算出和给定宽高以及设备屏幕宽高，最接近的摄像头尺寸。以及一些api的初始化
        try {
            for (cameraId in cameraMgr.cameraIdList) {
                val characteristics = cameraMgr.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue
                }
                val map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: continue

                // 选出最大的size,比较方式为 width*height 值的大小 
                val largest = Collections.max(Arrays.asList(*map.getOutputSizes(ImageFormat.JPEG)),
                        CompareSizesByArea())

                // 预览的宽高需要根据此时屏幕的旋转角度，以及设备自身的“调整角度”来配合
                val displayRotation = activity.windowManager.defaultDisplay.rotation
                sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
                val swappedDimensions = areDimensionsSwapped(displayRotation, sensorOrientation)

                val displaySize = Point()
                activity.windowManager.defaultDisplay.getSize(displaySize)
                val rotatedPreviewSize = if (swappedDimensions) size.swapp() else size
                val displaySize1 = Size(displaySize.x, displaySize.y)
                var maxPreviewSize = if (swappedDimensions) displaySize1.swapp() else displaySize1
                maxPreviewSize = maxPreviewSize.maxChoose(DEF_MAX_PREVIEW_SIZE)

                previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture::class.java),
                        rotatedPreviewSize.width, rotatedPreviewSize.height,
                        maxPreviewSize.width, maxPreviewSize.height, largest, displayRotation)
                this.cameraId = cameraId

                imageReader = ImageReader.newInstance(previewSize.width, previewSize.height
                        , ImageFormat.JPEG, 2).apply {
                    setOnImageAvailableListener({

                    }, null)
                }
                flashSupported =
                        characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                Log.d(TAG, "phone width ${activity.resources.displayMetrics.widthPixels} " +
                        "height ${activity.resources.displayMetrics.heightPixels}")
                Log.d(TAG, "display rotation $displayRotation  sensor $sensorOrientation: ")
                Log.d(TAG, "preview size $previewSize  largest size is $largest")
                break
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    /**
     * @param surface preview surface
     * */
    @SuppressLint("MissingPermission")
    fun openCamera(surface: Surface, previewStartedF: () -> Unit = {}) {
        this.previewSurface = surface
        this.previewStarted = previewStartedF
        cameraMgr.openCamera(cameraId, stateCallback, null)
    }

    private var previewStarted: () -> Unit = {}

    private fun startPreview() {
        try {
            cameraDevice?.apply {
                previewStarted.invoke()
                builder = createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                // 自动对焦 
                builder?.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                val list = arrayListOf<Surface>()
                previewSurface?.apply {
                    list.add(this)
                    builder?.addTarget(this)
                }
                createCaptureSession(list, object : CameraCaptureSession.StateCallback() {
                    override fun onConfigureFailed(session: CameraCaptureSession?) {

                    }

                    override fun onConfigured(session: CameraCaptureSession?) {
                        cameraSession = session
                        updatePreview()
                    }

                }, null)
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun updatePreview() {
        try {
            Log.d(TAG, "update preview thread : ${Thread.currentThread().name}")
            cameraSession?.setRepeatingRequest(builder!!.build(), null, null)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
    }

    private fun stopPreview() {
        cameraSession?.close()
        cameraSession = null
    }

    //------------TakePicture---------

    fun takePhoto(path: String? = null) {
        try {
            builder?.apply {
                set(CaptureRequest.CONTROL_AF_TRIGGER,
                        CameraMetadata.CONTROL_AF_TRIGGER_START)
//                cameraSession?.capture(build(),)
            }
        } catch (e: CameraAccessException) {

        }
    }

    //------------Record--------------
    private var isRecordingVideo = false


    fun startRecord(recordSurface: Surface) {
        Log.d(TAG, "record thread : ${Thread.currentThread().name} ")
        stopPreview()
        try {
            cameraDevice?.apply {
                builder = createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
                // 自动对焦 
                builder?.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                val list = arrayListOf<Surface>()
                previewSurface?.apply {
                    list.add(this)
                    builder?.addTarget(this)
                }
                builder?.addTarget(recordSurface)
                list.add(recordSurface)
                createCaptureSession(list, object : CameraCaptureSession.StateCallback() {
                    override fun onConfigureFailed(session: CameraCaptureSession?) {
                        Log.d(TAG, "record failed ${Thread.currentThread().name}")
                    }

                    override fun onConfigured(session: CameraCaptureSession?) {
                        Log.d(TAG, "record configure ${Thread.currentThread().name}")
                        cameraSession = session
                        updatePreview()
                        isRecordingVideo = true
                    }

                }, UIHandler)
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    //-----------Destroy--------------
    fun onDestory() {

    }
} 