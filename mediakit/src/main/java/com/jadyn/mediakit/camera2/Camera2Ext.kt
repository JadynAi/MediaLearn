package com.jadyn.mediakit.camera2

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.os.Handler
import android.view.Surface

/**
 *@version:
 *@FileDescription: Camera2 external function
 *@Author:Jing
 *@Since:2019-07-25
 *@ChangeList:
 */
fun CameraDevice.createCaptureSession2(outputs: List<Surface>,
                                       success: (session: CameraCaptureSession?) -> Unit = {},
                                       failed: (session: CameraCaptureSession?) -> Unit = {},
                                       handler: Handler? = null) {
    createCaptureSession(outputs, object : CameraCaptureSession.StateCallback() {
        override fun onConfigureFailed(session: CameraCaptureSession?) {
            failed.invoke(session)
        }

        override fun onConfigured(session: CameraCaptureSession?) {
            success.invoke(session)
        }

    }, handler)
}

class StateCallBack(
        private val opened: (cameraDevice: CameraDevice?) -> Unit,
        private val closed: (cameraDevice: CameraDevice?) -> Unit,
        private val disConnected: (cameraDevice: CameraDevice?) -> Unit = {},
        private val onError: (cameraDevice: CameraDevice?, error: Int) -> Unit = { _, _ -> Unit }
) : CameraDevice.StateCallback() {
    override fun onOpened(camera: CameraDevice?) {
        opened.invoke(camera)
    }

    override fun onClosed(camera: CameraDevice?) {
        super.onClosed(camera)
        closed.invoke(camera)
    }

    override fun onDisconnected(camera: CameraDevice?) {
        disConnected.invoke(camera)
    }

    override fun onError(camera: CameraDevice?, error: Int) {
        onError.invoke(camera, error)
    }
}