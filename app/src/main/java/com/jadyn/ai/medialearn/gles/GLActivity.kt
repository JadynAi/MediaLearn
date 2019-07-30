package com.jadyn.ai.medialearn.gles

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.jadyn.ai.medialearn.R
import com.jadyn.ai.medialearn.gles.render.TextureRender
import kotlinx.android.synthetic.main.activity_gles.*

/**
 *@version:
 *@FileDescription:
 *@Author:Jing
 *@Since:2019/4/17
 *@ChangeList:
 */
class GLActivity : AppCompatActivity() {

    private val TAG = "GLTest"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gles)

        gl_texture_view.setEGLContextClientVersion(2)
        gl_texture_view.setEGLConfigChooser(8, 8, 8, 8,
                16, 0)
        gl_texture_view.setRenderer(TextureRender())
        gl_texture_view.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

    }

    override fun onDestroy() {
        super.onDestroy()
    }
}