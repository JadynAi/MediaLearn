package com.jadyn.mediakit.camera2

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.*
import android.hardware.camera2.*
import android.media.ImageReader
import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.Surface
import com.jadyn.ai.kotlind.utils.*
import com.jadyn.mediakit.function.CompareSizesByArea
import com.jadyn.mediakit.function.areDimensionsSwapped
import com.jadyn.mediakit.function.chooseOptimalSize
import java.io.FileOutputStream
import java.lang.ref.SoftReference
import java.util.*

/**
 *@version:
 *@FileDescription:
 *@Author:Jing
 *@Since:2019-05-07
 *@ChangeList:
 */
class CameraMgr(private var activity: SoftReference<Activity>, size: Size) {

    private val TAG = "Camera2Ops"

    private val DEF_MAX_PREVIEW_SIZE = Size(1920, 1080)
    private var isReady = false
    private lateinit var cameraMgr: CameraManager
    private lateinit var cameraIDC: CameraIDC

    private var sensorOrientation = 0

    //是否支持闪光灯
    private var flashSupported = false

    lateinit var previewSize: Size
        private set

    private var workerThread: HandlerThread? = null
    private var workerHandler: Handler? = null

    private val surfaceCompose by lazy {
        Camera2Ext()
    }

    private lateinit var previewST: SurfaceTexture
    private var cameraDevice: CameraDevice? = null
    private var builder: CaptureRequest.Builder? = null
    private var cameraSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private val stateCallback by lazy {
        object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice?) {
                Log.d(TAG, " open: ")
                if (workerThread == null) {
                    workerThread = HandlerThread("Camera2Run")
                    workerThread?.start()
                    workerHandler = Handler(workerThread?.looper)
                }
                cameraDevice = camera
                if (imageReader == null) {
                    imageReader = ImageReader.newInstance(previewSize.width, previewSize.height
                            , ImageFormat.JPEG, 1).apply {
                        setOnImageAvailableListener({
                            val image = it.acquireNextImage()
                            val byteBuffer = image.planes[0].buffer
                            val byteData = ByteArray(byteBuffer.remaining())
                            byteBuffer.get(byteData)
                            it.close()

                            val bitmap = BitmapFactory.decodeByteArray(byteData, 0, byteData.size)
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, FileOutputStream(takePhotoPath))
//                            val bitmap = Bitmap.createBitmap(previewSize.width, previewSize.height,
//                                    Bitmap.Config.ARGB_8888)
//                            bitmap.copyPixelsFromBuffer(byteBuffer)

                        }, workerHandler)
                    }
                }
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
    fun openCamera(surface: SurfaceTexture, previewStartedF: () -> Unit = {}): Boolean {
        if (!isReady) {
            return false
        }
        this.previewST = surface
        this.previewStarted = previewStartedF
        cameraMgr.openCamera(cameraIDC.curID, stateCallback, null)
        return true
    }

    private fun tryConfig(size: Size): Boolean {
        val act = activity.get()
        if (act == null || act.isFinishing) return false

        cameraMgr = act.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        // 计算出和给定宽高以及设备屏幕宽高，最接近的摄像头尺寸。以及一些api的初始化
        try {
            cameraIDC = CameraIDC(cameraMgr)
            val characteristics = cameraMgr.getCameraCharacteristics(cameraIDC.curID)

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
                val surface = Surface(previewST)
                list.add(surface)
                // session 需要这个surface，用于ImageReader
                list.add(imageReader!!.surface)
                builder?.addTarget(surface)
                surfaceCompose.add(surface)
                createCaptureSession2(list, {
                    cameraSession = it
                    updatePreview()
                }, handler = workerHandler)
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
        cameraSession?.stopRepeating()
        cameraSession?.close()
        cameraSession = null
    }

    //------------TakePicture---------

    private var takePhotoPath = ""

    fun takePhoto(path: String) {
        try {
            takePhotoPath = path
            cameraDevice?.apply {
                val builder = createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                builder.addTarget(imageReader!!.surface)
                // 自动对焦
                builder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                builder.set(CaptureRequest.JPEG_ORIENTATION, sensorOrientation)      //根据摄像头方向对保存的照片进行旋转，使其为"自然方向"
                cameraSession?.capture(builder.build(), null, Handler(Looper.getMainLooper()))
                        ?: toastS("拍照异常")
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

                val preSurface = Surface(previewST)
                builder?.addTarget(preSurface)
                builder?.addTarget(recordSurface)
                list.add(preSurface)
                list.add(recordSurface)

                surfaceCompose.add(preSurface, recordSurface)
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
    fun onDestroy() {
        Log.d(TAG, "camera2 mgr destroy")
        workerHandler?.removeCallbacksAndMessages(null)
        workerThread?.quitSafely()
        try {
            workerThread?.join()
            workerThread = null
            workerHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, e.toString())
        }
        imageReader?.close()
        imageReader = null
        cameraSession?.close()
        cameraSession = null
        cameraDevice?.close()
        cameraDevice = null
        activity.clear()
    }

    fun onResume() {
        startPreview()
    }

    fun onPause() {
        stopPreview()
    }
} 