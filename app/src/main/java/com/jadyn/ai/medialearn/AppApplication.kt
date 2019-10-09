package com.jadyn.ai.medialearn

import android.app.Application
import com.jadyn.ai.kotlind.base.KD

/**
 *@version:
 *@FileDescription:
 *@Author:Jing
 *@Since:2019-08-15
 *@ChangeList:
 */
class AppApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        KD.init(this)
    }
}