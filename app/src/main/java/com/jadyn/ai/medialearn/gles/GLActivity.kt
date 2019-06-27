package com.jadyn.ai.medialearn.gles

import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Surface
import android.view.TextureView
import com.jadyn.ai.medialearn.R
import com.jadyn.mediakit.gl.EglEnv
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
        
        GLSurfaceView(this)
        gl_texture_view.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
            }

            override fun onSurfaceTextureUpdated(texture: SurfaceTexture?) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                return false
            }

            override fun onSurfaceTextureAvailable(texture: SurfaceTexture?, width: Int, height: Int) {
                val surface = Surface(texture)
                EglEnv(width, height).buildWindowSurface(surface)
            }
        }
    }
}