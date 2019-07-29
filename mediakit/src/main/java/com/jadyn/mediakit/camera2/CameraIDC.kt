package com.jadyn.mediakit.camera2

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.support.annotation.IntRange
import android.util.ArrayMap
import android.util.Size
import com.jadyn.ai.kotlind.utils.isValid

/**
 *@version:
 *@FileDescription: manager camera ids
 *@Author:Jing
 *@Since:2019-07-26
 *@ChangeList:
 */
class CameraIDC(cameraMgr: CameraManager,
                @IntRange(from = 0, to = 1) defLoc: Int = 1) {
    private val ids by lazy {
        Array(2) { "" }
    }
    private val idData by lazy {
        ArrayMap<String, Array<Size>>()
    }

    private var curLoc: Int = 1

    var curID: String
        private set

    init {
        curLoc = defLoc
        curID = findIds(cameraMgr)
    }

    private fun findIds(cameraMgr: CameraManager): String {
        for (cameraId in cameraMgr.cameraIdList) {
            val characteristics = cameraMgr.getCameraCharacteristics(cameraId)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            facing?.apply {
                if (this == CameraMetadata.LENS_FACING_FRONT) {
                    ids[0] = cameraId
                } else if (this == CameraMetadata.LENS_FACING_BACK) {
                    ids[1] = cameraId
                }
                idData[cameraId] = map.getOutputSizes(SurfaceTexture::class.java)
            }
        }
        val s = ids[curLoc]
        if (!s.isValid()) {
            return swap()
        }
        return s
    }

    fun swap(): String {
        next()
        curID = ids[curLoc]
        return curID
    }

    private fun next() {
        curLoc = (curLoc + 1) % 2
    }

    fun getCurCameraInfo() :Array<Size>{
        return getCameraSizeInfo(curLoc)
    }

    fun getCameraSizeInfo(@IntRange(from = 0L, to = 1L) index: Int): Array<Size> {
        return idData.getOrDefault(ids[index], arrayOf())
    }
}