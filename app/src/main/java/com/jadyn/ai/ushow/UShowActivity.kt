package com.jadyn.ai.ushow

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jadyn.ai.medialearn.R

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

        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.container, UShowFragment(), UShowFragment::class.java.name)
    }
}