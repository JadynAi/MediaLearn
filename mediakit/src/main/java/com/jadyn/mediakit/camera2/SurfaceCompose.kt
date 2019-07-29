package com.jadyn.mediakit.camera2

import android.view.Surface

/**
 *@version:
 *@FileDescription:
 *@Author:Jing
 *@Since:2019-07-25
 *@ChangeList:
 */
class Camera2Ext {
    private val mgrList by lazy {
        LinkedHashSet<Surface>()
    }

    fun add(surface: Surface?) {
        surface?.apply {
            mgrList.add(this)
        }
    }

    fun add(vararg s: Surface?) {
        s.forEach { 
            it?.apply {
                mgrList.add(this)
            }
        }
    }

    fun clear() {
        if (mgrList.isEmpty()) {
            return
        }
        mgrList.forEach {
            it.release()
        }
        mgrList.clear()
    }
}