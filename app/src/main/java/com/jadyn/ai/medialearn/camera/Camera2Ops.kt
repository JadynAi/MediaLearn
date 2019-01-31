package com.jadyn.ai.medialearn.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.Size
import android.view.Surface
import com.jadyn.ai.medialearn.codec.AiLoiVideoEncoder
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.*

/**
 *@version:
 *@FileDescription: Camera2 管理类
 *@Author:jing
 *@Since:2018/12/8
 *@ChangeList:
 */
class Camera2Ops(val activity: AppCompatActivity, val size: Size,
                 val isFront: Boolean = false, var handler: Handler? = null, init2: ((Camera2Ops, Size) -> Unit)? = null) {

    private val TAG = "Camera2Ops"

    private val cameraMgr by lazy {
        activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private val compositeDisposable by lazy {
        CompositeDisposable()
    }

    private lateinit var mPreviewSize: Size
    private lateinit var mCaptureSize: Size
    private lateinit var mCameraId: String

    private var cameraDevice: CameraDevice? = null
    private var builder: CaptureRequest.Builder? = null
    private var cameraSession: CameraCaptureSession? = null

    private var previewSurface: Surface? = null

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
        if (handler == null) {
            handler = Handler(Looper.getMainLooper())
        }
        try {
            for (cameraId in cameraMgr.cameraIdList) {
                val characteristics = cameraMgr.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue
                }
                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                mPreviewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture::class.java),
                        size.width, size.height)
                mCaptureSize = Collections.max(Arrays.asList(*map.getOutputSizes(ImageFormat.JPEG))) { lhs, rhs ->
                    java.lang.Long.signum((lhs.width * lhs.height - rhs.height * rhs.width).toLong())
                }
                mCameraId = cameraId
                break
            }
            init2?.invoke(this, mPreviewSize)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    fun openCamera(surface: Surface) {
        this.previewSurface = surface
        cameraMgr.openCamera(mCameraId, stateCallback, null)
    }

    //------preview---
    var beginPreview: (() -> Unit)? = null

    private fun startPreview() {
        try {
            beginPreview?.invoke()
            cameraDevice?.apply {
                builder = createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
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
            cameraSession?.setRepeatingRequest(builder!!.build(), null, null)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
    }

    private fun stopPreview() {
        cameraSession?.close()
        cameraSession = null
    }

    //-----record-----
    private var isRecordingVideo = false

    // 2018/12/28-16:01 录制准备好
    var recordPrepared: (() -> Unit)? = null

    var beginRecord: (() -> Unit)? = null

    var recordStopped: (() -> Unit)? = null

    private var encoder2: AiLoiVideoEncoder? = null

    fun toggleRecord(encoder: AiLoiVideoEncoder) {
        if (isRecordingVideo) {
            stopRecord()
        } else {
            startRecord(encoder)
        }
    }

    private fun startRecord(encoder: AiLoiVideoEncoder) {
        if (cameraDevice == null) return
        if (encoder2 == null) {
            encoder2 = encoder
        }
        val disposable = Single.fromCallable {
            stopPreview()
            val encoderSurface = encoder2!!.createSurface(mPreviewSize.width, mPreviewSize.height)
            cameraDevice?.apply {
                builder = createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
                val list = arrayListOf<Surface>()
                previewSurface?.apply {
                    list.add(this)
                    builder?.addTarget(this)
                }
                builder?.addTarget(encoderSurface)
                list.add(encoderSurface) 
                createCaptureSession(list, object : CameraCaptureSession.StateCallback() {
                    override fun onConfigureFailed(session: CameraCaptureSession?) {
                        Log.d(TAG, "onConfigureFailed:")
                    }

                    override fun onConfigured(session: CameraCaptureSession?) {
                        Log.d(TAG, "onConfigured :${Thread.currentThread().name} ")
                        cameraSession = session
                        updatePreview()
                        recordPrepared?.invoke()
                        isRecordingVideo = true
                    }
                }, handler)
            }
            encoder2?.start()
            true
        }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.d(TAG, " record end ")
                    // 2019/1/2-17:46 录制视频真正结束的地方
                    compositeDisposable.clear()
                    startPreview()
                    recordStopped?.invoke()
                }, {
                    Log.d(TAG, "record start failed ${it.message}")
                })
        compositeDisposable.add(disposable)
    }

    private fun stopRecord() {
        isRecordingVideo = false
        encoder2?.stop()
    }

    fun release() {
        stopRecord()
        stopPreview()
        encoder2?.release()
    }
}