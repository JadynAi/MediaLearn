package com.jadyn.mediakit.function

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observables.ConnectableObservable
import java.util.*

/**
 *@version:
 *@FileDescription: 同一线程，按队列依次执行的Publish
 *@Author:AiLo
 *@Since:2019/2/27
 *@ChangeList:
 */
class SchedulerArrayExecutor<D> private constructor() {

    private val queueTask by lazy {
        Collections.synchronizedList(arrayListOf<ConnectableObservable<D>>())
    }

    private val compositeDisposable by lazy { 
        CompositeDisposable()
    }


    fun execute(ob: ConnectableObservable<D>) {
        if (queueTask.isEmpty()) {
            ob.connect()
            queueTask.add(ob)
        } else {
            queueTask.contains(ob)
        }
    }

    companion object {

        fun <D> create() = SchedulerArrayExecutor<D>()
    }
}