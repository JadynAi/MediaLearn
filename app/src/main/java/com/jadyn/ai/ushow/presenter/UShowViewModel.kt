package com.jadyn.ai.ushow.presenter

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 *@version:
 *@FileDescription:
 *@Author:Jing
 *@Since:2019-09-16
 *@ChangeList:
 */
class UShowViewModel : ViewModel() {
    
    val data = MutableLiveData<String>()

    fun onStart() {
    }
}