package com.jadyn.mediakit.video.decode.pixg

import android.graphics.PixelFormat
import android.media.ImageReader
import android.util.Size
import android.view.Surface

/**
 *@version:
 *@FileDescription: 像素拷贝
 *@Author:Jing
 *@Since:2019-07-16
 *@ChangeList:
 */
abstract class PixelsGen {

    open fun configSurface(): Surface? {
        return null
    }

    lateinit var size: Size
    
    abstract fun release()
}

class ImageReaderPixelsGen : PixelsGen() {
    private val imageReader by lazy {
        ImageReader.newInstance(size.width, size.height, PixelFormat.RGBA_8888, 3)
    }

    override fun configSurface(): Surface? {
        return imageReader.surface
    }

    override fun release() {
    }
}

/**
 * 普通的glReadPixels
 * */
class FBOPixelsGen : PixelsGen() {

    override fun release() {
    }
}

/**
 * 使用PBO模式
 * */
class PBOPixelsGen : PixelsGen() {

    override fun release() {
    }
}


class MyLinkedList() {

    /** Initialize your data structure here. */
    private var head: Node? = null
    private var tail: Node? = null
    private var size = 0

    /** Get the value of the index-th node in the linked list. If the index is invalid, return -1. */
    fun get(index: Int): Int {
        if (index < 0) {
            return -1
        }
        if (index == 0) {
            return (head?.`val`) ?: -1
        }
        var s = head
        var f = head?.next
        var sI = 0
        var fI = 1
        loop@ while (s != null || f != null) {
            if (sI == index || fI == index) {
                break@loop
            }
            s = s?.next?.next
            f = f?.next?.next
            sI += 2
            fI += 2
        }
        if (sI == index) {
            return s?.`val` ?: -1
        }
        if (fI == index) {
            return f?.`val` ?: -1
        }
        return -1
    }

    /** Add a node of value val before the first element of the linked list. After the insertion, the new node will be the first node of the linked list. */
    fun addAtHead(`val`: Int) {
        size++
        head?.apply {
            val newH = Node(`val`)
            newH.next = this
            head = newH
            return
        }
        head = Node(`val`)
    }

    /** Append a node of value val to the last element of the linked list. */
    fun addAtTail(`val`: Int) {
        size++
        val newT = Node(`val`)
        tail?.apply {
            this.next = newT
            tail = newT
            return
        }
        if (head != null) {
            head!!.next = newT
            tail = newT
            return
        }
        addAtHead(`val`)
    }

    /** Add a node of value val before the index-th node in the linked list. If index equals to the length of linked list, the node will be appended to the end of linked list. If index is greater than the length, the node will not be inserted. */
    fun addAtIndex(index: Int, `val`: Int) {
        if (index < 0) {
            return
        }
        if (index == 0) {
            addAtHead(`val`)
            return
        }
        if (index == size) {
            addAtTail(`val`)
            return
        }
        size++
        var cur = head
        var cI = 0
        var isLoop = true
        while (isLoop) {
            cI++
            val n = cur?.next
            if (cI == index) {
                val nA = Node(`val`)
                cur?.next = nA
                nA.next = n
                isLoop = false
            }
            cur = cur?.next
        }
    }

    /** Delete the index-th node in the linked list, if the index is valid. */
    fun deleteAtIndex(index: Int) {
        if (index < 0) {
            return
        }
        if (size <= 0) {
            return
        }
        if (index >= size) {
            return
        }
        size--
        var cur = head
        if (index == 0) {
            head = head?.next
            return
        }
        var cI = 0
        loop@ while (cur != null) {
            cI++
            if (cI == index) {
                cur!!.next = cur!!.next?.next
                if (index == size - 1) {
                    tail = cur
                }
                break@loop
            }
        }
    }

    private class Node(val `val`: Int) {
        var next: Node? = null
    }

}