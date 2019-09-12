package com.jadyn.mediakit.camera

import android.app.Activity
import android.graphics.SurfaceTexture
import com.jadyn.mediakit.camera.camera2.Camera2Impl
import java.lang.ref.SoftReference

/**
 *@version:
 *@FileDescription: camera function
 *@Author:Jing
 *@Since:2019-09-06
 *@ChangeList:
 */
interface ICamera {

    fun openCamera(surfaceTexture: SurfaceTexture)

    fun startPreview()

    fun stopPreview()

    fun startRecord(surfaceTexture: SurfaceTexture)

    fun stopRecord()

    fun takePhoto()

}

class CameraFactory {
    companion object {
        fun createCamera(activity: Activity): ICamera {
            val soft = SoftReference<Activity>(activity)
            return Camera2Impl(soft)
        }
    }
}