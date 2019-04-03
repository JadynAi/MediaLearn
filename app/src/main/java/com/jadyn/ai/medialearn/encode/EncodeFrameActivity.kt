package com.jadyn.ai.medialearn.encode

import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import com.jadyn.ai.medialearn.R

/**
 *@version:
 *@FileDescription: 将帧编码为视频
 *@Author:Jing
 *@Since:2019/4/2
 *@ChangeList:
 */
class EncodeFrameActivity : AppCompatActivity() {

    // 测试文件夹
    private val encodePicDir = TextUtils.concat(Environment.getExternalStorageDirectory().path,
            "/decode").toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_encode_frame)
        
    }
}


