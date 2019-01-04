package com.jadyn.ai.medialearn.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.os.bundleOf

/**
 *@version:
 *@FileDescription:
 *@Author:jing
 *@Since:2018/11/26
 *@ChangeList:
 */

inline fun <reified T : Activity> Context.start(vararg params: Pair<String, Any?>) {
    val intent = Intent(this, T::class.java)
    if (params.isNotEmpty()) {
        val bundle = bundleOf(*params)
        intent.putExtras(bundle)
    }
    startActivity(intent)
}