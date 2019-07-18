package com.jadyn.ai.medialearn.cutout

import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.widget.ArrayAdapter
import com.jadyn.ai.kotlind.function.ui.getResDrawable
import com.jadyn.ai.medialearn.R
import com.jadyn.mediakit.video.decode.VideoDecoder2Compat
import kotlinx.android.synthetic.main.activity_cutout.*
import java.io.File

/**
 *@version:
 *@FileDescription:
 *@Author:Jing
 *@Since:2019-04-25
 *@ChangeList:
 */
class CutOutActivity : AppCompatActivity() {

    private val effects = TextUtils.concat(Environment.getExternalStorageDirectory().path,
            "/effect").toString()

    private val videoDecoder2Compat by lazy {
        VideoDecoder2Compat {
            cutout_show.setImageBitmap(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cutout)
        
        bg.setImageDrawable(getResDrawable(R.drawable.girl))

        val listFiles = File(effects).listFiles().map {
            it.absolutePath
        }
        cutout_list.adapter = ArrayAdapter(this, android.R.layout.simple_expandable_list_item_1,
                listFiles)
        cutout_list.setOnItemClickListener { parent, view, position, id ->
            videoDecoder2Compat.setDataSource(listFiles[position])
        }
    }
}