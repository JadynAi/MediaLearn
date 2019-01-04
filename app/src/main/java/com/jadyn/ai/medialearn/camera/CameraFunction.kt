package com.jadyn.ai.medialearn.camera

import android.hardware.Camera
import android.util.Size
import java.util.*

/**
 *@version:
 *@FileDescription:
 *@Author:jing
 *@Since:2018/11/27
 *@ChangeList:
 */
fun choosePreviewSize(parms: Camera.Parameters, width: Int, height: Int) {
    // We should make sure that the requested MPEG size is less than the preferred
    // size, and has the same aspect ratio.
    val ppsfv = parms.preferredPreviewSizeForVideo
    for (size in parms.supportedPreviewSizes) {
        if (size.width == width && size.height == height) {
            parms.setPreviewSize(width, height)
            return
        }
    }

    if (ppsfv != null) {
        parms.setPreviewSize(ppsfv.width, ppsfv.height)
    }
}

fun getOptimalSize(sizeMap: Array<Size>, width: Int, height: Int): Size {
    val sizeList = ArrayList<Size>()
    for (option in sizeMap) {
        if (width > height) {
            if (option.width > width && option.height > height) {
                sizeList.add(option)
            }
        } else {
            if (option.width > height && option.height > width) {
                sizeList.add(option)
            }
        }
    }
    return if (sizeList.size > 0) {
        Collections.min(sizeList) { lhs, rhs -> java.lang.Long.signum((lhs.width * lhs.height - rhs.width * rhs.height).toLong()) }
    } else sizeMap[0]
}