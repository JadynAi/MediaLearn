package com.jadyn.mediakit.gl

/**
 *@version:
 *@FileDescription:
 *@Author:Jing
 *@Since:2019-07-20
 *@ChangeList:
 */
object GLJni {
    
    init {
        System.loadLibrary("gljni")
    }
    
    external fun glReadPixels(x: Int, y: Int, width: Int, height: Int, 
                              format: Int,
                              type: Int)
} 