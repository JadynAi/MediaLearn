package com.jadyn.ai.ushow

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import com.jadyn.ai.medialearn.R
import com.jadyn.ai.presenter.ushow.UShowViewModel

/**
 *@version:
 *@FileDescription:
 *@Author:Jing
 *@Since:2019-09-05
 *@ChangeList:
 */
class UShowActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ushow)
        
        ViewModelProviders.of(this).get(UShowViewModel::class.java)
    }
}