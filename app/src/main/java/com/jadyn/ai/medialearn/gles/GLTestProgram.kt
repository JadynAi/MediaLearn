package com.jadyn.ai.medialearn.gles

import com.jadyn.mediakit.gl.createProgram

/**
 *@version:
 *@FileDescription:
 *@Author:Jing
 *@Since:2019/4/17
 *@ChangeList:
 */
class GLTestProgram {
    private val VERTEX_SHADER = """
        attribute vec4 position;
        attribute vec2 texcoord;
        varying vec2 v_texcoord;
        void main{
            gl_position = position;
            v_texcoord = texcoord;
        }
    """

    private val FRAGMENT_SHADER = """
        
    """

    init {
        createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
    }
}