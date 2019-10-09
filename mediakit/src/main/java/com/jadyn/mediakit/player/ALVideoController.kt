package com.jadyn.mediakit.player

/**
 *@version:
 *@FileDescription:
 *@Author:Jing
 *@Since:2019-09-27
 *@ChangeList:
 */
class ALVideoController {

    external fun prepare(src: String)

    external fun start()

    external fun stop()

    external fun pause()

    external fun seekTo(position: Float)
    
    external fun release()
} 