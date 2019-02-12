package com.jadyn.ai.medialearn.utils

import android.media.MediaFormat
import android.util.Size

/**
 *@version:
 *@FileDescription:
 *@Author:jing
 *@Since:2019/2/12
 *@ChangeList:
 */
val MediaFormat.width
    get() = getInteger(MediaFormat.KEY_WIDTH)

val MediaFormat.height
    get() = getInteger(MediaFormat.KEY_HEIGHT)

val MediaFormat.size
    get() = Size(width, height)

val MediaFormat.duration
    get() = getLong(MediaFormat.KEY_DURATION)

val MediaFormat.fps: Int
    get() = try {
        getInteger(MediaFormat.KEY_FRAME_RATE)
    } catch (e: Exception) {
        0
    }

val MediaFormat.mime
    get() = getString(MediaFormat.KEY_MIME)

val MediaFormat.rotation
    get() = if (containsKey(MediaFormat.KEY_ROTATION)) getInteger(MediaFormat.KEY_ROTATION) else 0