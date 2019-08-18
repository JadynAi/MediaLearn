package com.jadyn.ai.medialearn

import com.jadyn.ai.kotlind.base.BaseApplication
import com.squareup.leakcanary.LeakCanary

/**
 *@version:
 *@FileDescription:
 *@Author:Jing
 *@Since:2019-08-15
 *@ChangeList:
 */
class AppApplication : BaseApplication() {

    override fun onCreate() {
        super.onCreate()
        LeakCanary.install(this)
    }
}