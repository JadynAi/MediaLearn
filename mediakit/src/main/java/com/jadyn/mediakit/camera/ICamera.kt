package com.jadyn.mediakit.camera

import android.app.Activity
import android.graphics.SurfaceTexture
import com.jadyn.mediakit.camera.camera2.Camera2Impl
import java.lang.ref.SoftReference

/**
 *@version:
 *@FileDescription: camera function。只单纯的提供Camera应该配置的功能，不提供功能
 *                  间的业务协调
 *@Author:Jing
 *@Since:2019-09-06
 *@ChangeList:
 */
interface ICamera {

    fun openCamera():Boolean

    fun startPreview(surfaceTexture: SurfaceTexture)

    fun stopPreview()

    fun startRecord(surfaceTexture: SurfaceTexture)

    fun stopRecord()

    fun takePhoto()
    
    fun switchCamera()

    fun release()
}

class CameraFactory {
    companion object {
        fun createCamera(activity: Activity): ICamera {
            val soft = SoftReference<Activity>(activity)
            return Camera2Impl(soft)
        }
    }
}