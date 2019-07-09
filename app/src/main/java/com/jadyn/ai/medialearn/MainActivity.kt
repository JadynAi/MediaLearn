package com.jadyn.ai.medialearn

import android.Manifest
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.jadyn.ai.kotlind.function.start
import com.jadyn.ai.kotlind.function.ui.click
import com.jadyn.ai.medialearn.camera.Camera2Activity
import com.jadyn.ai.medialearn.camera2.Camera2RecordActivity
import com.jadyn.ai.medialearn.cutout.CutOutActivity
import com.jadyn.ai.medialearn.decode.DecodeActivity
import com.jadyn.ai.medialearn.decode.DecodeFrameActivity
import com.jadyn.ai.medialearn.encode.EncodeFrameActivity
import com.jadyn.ai.medialearn.gles.GLActivity
import com.jadyn.ai.medialearn.permissions.RxPermissions
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val subscribe = RxPermissions(this).request(Manifest.permission.CAMERA)
                .subscribe {
                    RxPermissions(this).request(Manifest.permission.UPDATE_DEVICE_STATS).subscribe()
                }

        tv_go_camera.setOnClickListener {
            start<Camera2Activity>()
        }

        tv_go_camera2.setOnClickListener {
            RxPermissions(this).request(
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    .doOnNext {
                        if (it) {
                            start<Camera2RecordActivity>()
                        }
                    }
                    .subscribe()
        }

        tv_decode.setOnClickListener {
            RxPermissions(this).request(
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    .doOnNext { it1 ->
                        if (it1) {
                            RxPermissions(this).request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    .doOnNext {
                                        if (it) {
                                            start<DecodeActivity>()
                                        }
                                    }.subscribe()
                        }
                    }
                    .subscribe()

        }

        tv_decode_frame.setOnClickListener {
            RxPermissions(this).request(
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    .doOnNext {
                        if (it) {
                            start<DecodeFrameActivity>()
                        }
                    }
                    .subscribe()

        }

        // 视频编码
        tv_encode_frame.setOnClickListener {
            RxPermissions(this).request(
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    .doOnNext { it1 ->
                        if (it1) {
                            RxPermissions(this).request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    .doOnNext {
                                        if (it) {
                                            start<EncodeFrameActivity>()
                                        }
                                    }.subscribe()
                        }
                    }
                    .subscribe()

        }

        tv_gl.setOnClickListener {
            start<GLActivity>()
        }
        
        tv_cutout.click {
            start<CutOutActivity>()
//            val s = System.currentTimeMillis()
//            Log.d("cece", "start :$s ")
//            val get = Single.fromCallable {
//                Thread.sleep(5000)
//                Log.d("cece", "blocking : ${Thread.currentThread().name}")
//                1
//            }.subscribeOn(Schedulers.io()).blockingGet()
//            Log.d("cece", "start :${System.currentTimeMillis() - s} and $get")
        }
        
    }
}
