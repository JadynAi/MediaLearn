package com.jadyn.mediakit.function

import android.hardware.Camera
import android.util.Log
import android.util.Size
import android.view.Surface
import com.jadyn.ai.kotlind.utils.swap
import java.lang.Long
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
        Collections.min(sizeList) { lhs, rhs -> Long.signum((lhs.width * lhs.height - rhs.width * rhs.height).toLong()) }
    } else sizeMap[0]
}

/**
 *
 * @param displayRotation window rotation
 * */
fun chooseOptimalSize(choices: Array<Size>, textureViewWidth: Int, textureViewHeight: Int, maxWidth: Int,
                      maxHeight: Int,
                      aspectRatio: Size,
                      displayRotation: Int): Size {
    // Collect the supported resolutions that are at least as big as the preview Surface
    val bigEnough = ArrayList<Size>()
    // Collect the supported resolutions that are smaller than the preview Surface
    val notBigEnough = ArrayList<Size>()
    val w = aspectRatio.width
    val h = aspectRatio.height
    for (option in choices) {
        if (option.width <= maxWidth && option.height <= maxHeight &&
                option.height == option.width * h / w) {
            if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                bigEnough.add(option)
            } else {
                notBigEnough.add(option)
            }
        }
    }
    // Pick the smallest of those big enough. If there is no one big enough, pick the
    // largest of those not big enough.
    val s = if (bigEnough.isNotEmpty()) Collections.min(bigEnough, CompareSizesByArea())
    else (if (notBigEnough.isNotEmpty()) Collections.max(notBigEnough, CompareSizesByArea()
    ) else choices[0])
    // 如果window的旋转是0 或者 180，那么就将size的width和height置换
    return if (displayRotation == Surface.ROTATION_0 || displayRotation == Surface.ROTATION_180) 
        s.swap() else s
}

/**
 *  根据屏幕旋转角度以及 调整角度决定是否要交换 宽和高
 * */
fun areDimensionsSwapped(displayRotation: Int, sensorOrientation: Int): Boolean {
    var swappedDimensions = false
    when (displayRotation) {
        Surface.ROTATION_0, Surface.ROTATION_180 -> {
            if (sensorOrientation == 90 || sensorOrientation == 270) {
                swappedDimensions = true
            }
        }
        Surface.ROTATION_90, Surface.ROTATION_270 -> {
            if (sensorOrientation == 0 || sensorOrientation == 180) {
                swappedDimensions = true
            }
        }
        else -> {
            Log.e("areDimensionsSwapped", "Display rotation is invalid: $displayRotation")
        }
    }
    return swappedDimensions
}

internal class CompareSizesByArea : Comparator<Size> {

    // We cast here to ensure the multiplications won't overflow
    override fun compare(lhs: Size, rhs: Size) =
            Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)

}