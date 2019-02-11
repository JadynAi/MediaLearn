package com.jadyn.ai.medialearn.decode

import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import com.jadyn.ai.medialearn.R

/**
 *@version:
 *@FileDescription:
 *@Author:jing
 *@Since:2019/1/22
 *@ChangeList:
 */
class DecodeActivity : AppCompatActivity() {

    private val decodeMP4Path = TextUtils.concat(Environment.getExternalStorageDirectory().path,
            "/yazi.mp4").toString()

    private val videoDecoder by lazy {
        VideoDecoder.DecoderBuilder().makeFile(decodeMP4Path).saveDirectory(Environment.getExternalStorageDirectory().path
                + "/yaziWebpBitmap").build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_decode)

        videoDecoder.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        videoDecoder.stop()
    }
}