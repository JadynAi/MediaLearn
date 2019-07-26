package com.jadyn.mediakit.camera2

import android.view.Surface

/**
 *@version:
 *@FileDescription:
 *@Author:Jing
 *@Since:2019-07-25
 *@ChangeList:
 */
class SurfaceCompose {
    private val mgrList by lazy { 
        LinkedHashSet<Surface>()
    }

    fun add(surface: Surface?) {
        surface?.apply { 
            mgrList.add(this)
        }
    }
}