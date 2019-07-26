package com.jadyn.mediakit.camera2

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import com.jadyn.ai.kotlind.utils.maxChoose
import com.jadyn.ai.kotlind.utils.screenHeight
import com.jadyn.ai.kotlind.utils.screenWidth
import com.jadyn.ai.kotlind.utils.swap
import com.jadyn.mediakit.function.CompareSizesByArea
import com.jadyn.mediakit.function.areDimensionsSwapped
import com.jadyn.mediakit.function.chooseOptimalSize
import java.lang.ref.WeakReference
import java.util.*

/**
 *@version:
 *@FileDescription:
 *@Author:Jing
 *@Since:2019-05-07
 *@ChangeList:
 */
class CameraMgr(private var activity: WeakReference<Activity>, size: Size) {

    private val TAG = "Camera2Ops"

    private val DEF_MAX_PREVIEW_SIZE = Size(1920, 1080)
    private var isReady = false
    private lateinit var cameraMgr: CameraManager
    private lateinit var camerIDC: CameraIDC

    private var sensorOrientation = 0

    //是否支持闪光灯
    private var flashSupported = false

    lateinit var previewSize: Size
        private set
    private lateinit var cameraId: String

    private val workerThread by lazy {
        val thread = HandlerThread("Camera2Run")
        thread.start()
        thread
    }

    private val workerHandler by lazy {
        Handler(workerThread.looper)
    }

    private var previewSurface: Surface? = null
    private var cameraDevice: CameraDevice? = null
    private var builder: CaptureRequest.Builder? = null
    private var cameraSession: CameraCaptureSession? = null
    //    private var imageReader: ImageReader? = null
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
        isReady = tryConfig(size)
    }

    /**
     * @param surface preview surface
     * */
    @SuppressLint("MissingPermission")
    fun openCamera(surface: Surface, previewStartedF: () -> Unit = {}): Boolean {
        if (!isReady) {
            return false
        }
        this.previewSurface = surface
        this.previewStarted = previewStartedF
        cameraMgr.openCamera(cameraId, stateCallback, null)
        return true
    }

    private fun tryConfig(size: Size): Boolean {
        val act = activity.get()
        if (act == null || act.isFinishing) return false

        cameraMgr = act.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        // 计算出和给定宽高以及设备屏幕宽高，最接近的摄像头尺寸。以及一些api的初始化
        try {
            camerIDC = CameraIDC(cameraMgr)
            val characteristics = cameraMgr.getCameraCharacteristics(cameraId)

            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            // 选出最大的size,比较方式为 width*height 值的大小 
            val largest = Collections.max(Arrays.asList(*map.getOutputSizes(ImageFormat.JPEG)),
                    CompareSizesByArea())

            // 预览的宽高需要根据此时屏幕的旋转角度，以及设备自身的“调整角度”来配合
            val displayRotation = act.windowManager.defaultDisplay.rotation
            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
            val swappedDimensions = areDimensionsSwapped(displayRotation, sensorOrientation)

            val displaySize = Point(screenWidth, screenHeight)
            val rotatedPreviewSize = if (swappedDimensions) size.swap() else size
            val displaySize1 = Size(displaySize.x, displaySize.y)
            var maxPreviewSize = if (swappedDimensions) displaySize1.swap() else displaySize1
            maxPreviewSize = maxPreviewSize.maxChoose(DEF_MAX_PREVIEW_SIZE)

            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture::class.java),
                    rotatedPreviewSize.width, rotatedPreviewSize.height,
                    maxPreviewSize.width, maxPreviewSize.height, largest, displayRotation)
            this.cameraId = cameraId
//                imageReader = ImageReader.newInstance(previewSize.width, previewSize.height
//                        , ImageFormat.JPEG, 2).apply {
//                    setOnImageAvailableListener({
//
//                    }, null)
//                }
            flashSupported =
                    characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            Log.d(TAG, "display rotation $displayRotation  sensor $sensorOrientation: ")
            Log.d(TAG, "preview size $previewSize  largest size is $largest")
            Log.d(TAG, "all size ${Arrays.toString(map.getOutputSizes(SurfaceTexture::class.java))}")
            Log.d(TAG, "all video size ${Arrays.toString(map.getOutputSizes(MediaRecorder::class.java))}")
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        return true
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
                createCaptureSession2(list, {
                    cameraSession = it
                    updatePreview()
                })
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

    private var startRecordTime = 0L

    fun startRecord(recordSurface: Surface) {
        Log.d(TAG, "record thread : ${Thread.currentThread().name} ")
        stopPreview()
        try {
            cameraDevice?.apply {
                builder = createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
                // 自动对焦 
                builder?.set(CaptureRequest.CONTROL_AF_MODE,
                        CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                val list = arrayListOf<Surface>()

                builder?.addTarget(recordSurface)
                list.add(recordSurface)

                previewSurface?.apply {
                    list.add(this)
                    builder?.addTarget(this)
                }
                createCaptureSession2(list, {
                    Log.d(TAG, "record configure ${Thread.currentThread().name}")
                    startRecordTime = System.currentTimeMillis()
                    Log.d(TAG, "start record $startRecordTime")
                    cameraSession = it
                    updatePreview()
                    isRecordingVideo = true
                }, handler = workerHandler)
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    fun stopRecord(callback: (duration: Long) -> Unit = {}) {
        stopPreview()
        val endTime = System.currentTimeMillis()
        callback.invoke(endTime - startRecordTime)
        startPreview()
    }

    //-----------Destroy--------------
    fun onDestory() {
        workerHandler.removeCallbacksAndMessages(null)
        workerThread.quitSafely()
        activity.clear()
    }
} 