package com.jadyn.mediakit.camera.camera2

import android.app.Activity
import android.graphics.SurfaceTexture
import com.jadyn.mediakit.camera.ICamera
import java.lang.ref.SoftReference

/**
 *@version:
 *@FileDescription:camera2 business realize
 *@Author:Jing
 *@Since:2019-09-06
 *@ChangeList:
 */
class Camera2Impl(private val activity: SoftReference<Activity>) : ICamera {

    override fun openCamera(surfaceTexture: SurfaceTexture) {
    }

    override fun startPreview() {
    }

    override fun stopPreview() {
    }

    override fun startRecord(surfaceTexture: SurfaceTexture) {

    }

    override fun stopRecord() {
    }

    override fun takePhoto() {
    }

}