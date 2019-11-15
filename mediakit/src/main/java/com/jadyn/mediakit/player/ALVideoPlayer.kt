package com.jadyn.mediakit.player

import android.media.MediaExtractor
import android.util.Size
import android.view.Surface

/**
 *@version:
 *@FileDescription:
 *@Author:Jing
 *@Since:2019-09-27
 *@ChangeList:
 */
class ALVideoPlayer {
    
    init {
        val mediaExtractor = MediaExtractor()
    }

    external fun prepare(src: String, size: Size, surface: Surface)

    external fun play()

    external fun stop()

    external fun stopPlay()

    external fun pause()

    external fun seekTo(position: Float)

    external fun release()
} 