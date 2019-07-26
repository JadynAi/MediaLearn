package com.jadyn.mediakit.camera2

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.support.annotation.IntRange
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
        ArrayList<String>(2)
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
            facing?.apply {
                if (this == CameraMetadata.LENS_FACING_FRONT) {
                    ids[0] = cameraId
                } else if (this == CameraMetadata.LENS_FACING_BACK) {
                    ids[1] = cameraId
                }
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
}