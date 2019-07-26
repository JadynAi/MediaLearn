package com.jadyn.ai.medialearn.gles

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Surface
import android.view.TextureView
import com.jadyn.ai.medialearn.R
import com.jadyn.ai.medialearn.gles.render.TextureRender
import com.jadyn.mediakit.gl.EglEnv
import kotlinx.android.synthetic.main.activity_gles_texture.*

/**
 *@version:
 *@FileDescription:
 *@Author:Jing
 *@Since:2019-07-26
 *@ChangeList:
 */
class GLTextureActivity : AppCompatActivity() {

    private lateinit var thread: Thread

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gles_texture)

        gl_texture.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                return false
            }

            override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
                thread = Thread {
                    val eglEnv = EglEnv(width, height).setUpEnv().buildEGLSurface(Surface(surface))
                    val textureRender = TextureRender()
                    textureRender.onSurfaceCreated(null, null)
                    textureRender.onSurfaceChanged(null, width, height)
                    textureRender.onDrawFrame(null)
                    eglEnv.swapBuffers()
                }
                thread.start()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        thread.interrupt()
    }
}