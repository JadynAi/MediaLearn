package com.jadyn.mediakit.video.decode

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.jadyn.mediakit.cache.DiskLruCache
import com.jadyn.mediakit.function.convertString
import com.jadyn.mediakit.function.hashKeyForDisk
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

/**
 *@version:
 *@FileDescription:
 *@Author:Jing
 *@Since:2019/3/1
 *@ChangeList:
 */

/*
* maxSize:max MB
* */
class DiskCacheAssist(dir: String, appVersion: Int, valueCount: Int, maxSize: Int) {

    private val TAG = "DiskCacheAssist"

    private val diskLruCache by lazy {
        DiskLruCache.open(File(dir), appVersion, valueCount, (maxSize * 1024 * 1024).toLong())
    }

    private val writerExecutors by lazy {
        Executors.newSingleThreadExecutor()
    }

    private val queueWriteTask by lazy {
        CopyOnWriteArrayList<Int>()
    }

    /*
    *  key :缓存的文件名
    *  
    * 存Disk缓存在异步线程执行
    * */
    fun writeBitmap(key: String, b: Bitmap, success: () -> Unit = {},
                    failed: (Exception) -> Unit = {}) {
        //正在写入就不重复执行
        val code = key.hashCode()
        if (queueWriteTask.contains(code)) {
            return
        }
        //对key进行hash化
        val cacheKey = key.hashKeyForDisk()
        //查看LruDiskCache是否有这个缓存，这一步基本不消耗时间
        val snapshot = diskLruCache.get(cacheKey)
        if (snapshot != null) {
            success.invoke()
            return
        }

        queueWriteTask.add(code)
        //在一个子线程执行写入磁盘缓存的任务队列
        writerExecutors.execute(WriteR(cacheKey, b, {
            queueWriteTask.remove(code)
            success.invoke()
        }, {
            queueWriteTask.remove(code)
            failed.invoke(it)
        }))
    }

    private val readerExecutor by lazy {
        Executors.newSingleThreadExecutor()
    }
    private val compositeDisposable by lazy {
        CompositeDisposable()
    }

    //异步读取磁盘缓存
    fun asyncReadBitmap(key: String, success: (Bitmap) -> Unit = {},
                        failed: (Exception) -> Unit = {}) {
        var disposable: Disposable? = null
        disposable = Single.fromCallable {
            Log.d(TAG, "read key $key thread ${Thread.currentThread().name}: ")
            blockingReadBitmap(key) ?: throw NullPointerException("null bitmap")
        }.subscribeOn(Schedulers.from(readerExecutor))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    success.invoke(it)
                    disposable?.dispose()
                }, {
                    Log.d(TAG, "async load disk failed $it: ")
                    failed.invoke(Exception(it))
                    disposable?.dispose()
                })
        compositeDisposable.add(disposable)
    }

    //同步得到缓存
    fun blockingReadBitmap(key: String): Bitmap? {
        val cacheKey = key.hashKeyForDisk()
        diskLruCache.get(cacheKey)?.apply {
            val start = System.currentTimeMillis()
            val inputStream = getInputStream(0)
            val b = BitmapFactory.decodeStream(inputStream)
            Log.d(TAG, "read bitmap cost ${System.currentTimeMillis() - start}")
            return b
        }
        return null
    }


    fun release() {
        writerExecutors.shutdownNow()
        compositeDisposable.clear()
    }

    private inner class WriteR(private val key: String, private val b: Bitmap,
                               private val success: () -> Unit,
                               private val failed: (Exception) -> Unit) : Runnable {

        override fun run() {
            try {
                val editor = diskLruCache.edit(key)
                editor?.apply {
                    Log.d(TAG, "writer disk cache $key running")
                    val bitmapString = b.convertString()
                    if (bitmapString.isBlank()) {
                        editor.abort()
                    } else {
                        editor.set(0, bitmapString)
                        editor.commit()
                    }
                    success.invoke()
                    return
                }
                failed.invoke(Exception("edit key is null"))
            } catch (e: Exception) {
                e.printStackTrace()
                failed.invoke(e)
            }
        }
    }

    private inner class ReadB(private val cacheKey: String, private val success: (Bitmap) -> Unit,
                              private val failed: (Exception) -> Unit)
        : Runnable {
        override fun run() {
            try {
                diskLruCache.get(cacheKey)?.apply {
                    val inputStream = getInputStream(0)
                    success.invoke(BitmapFactory.decodeStream(inputStream))
                    return
                }
                failed.invoke(Exception("not cache this "))
            } catch (e: Exception) {
                failed.invoke(e)
            }
        }
    }
}