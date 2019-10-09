package com.jadyn.ai.ushow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

/**
 *@version:
 *@FileDescription:
 *@Author:Jing
 *@Since:2019-09-16
 *@ChangeList:
 */
abstract class BaseXFragment : Fragment(){
    lateinit var mRootView: View
    private var isVisibleToUsers = false
    private var isOnCreateView = false
    private var isSetUserVisibleHint = false
    private var isHiddenChanged = false
    private var isFirstResume = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        isOnCreateView = true
        mRootView = LayoutInflater.from(activity).inflate(getResId(), null, false)
        return mRootView
    }

    abstract fun getResId(): Int

    override fun onResume() {
        super.onResume()
        if (!isHiddenChanged && !isSetUserVisibleHint) {
            if (isFirstResume) {
                setVisibleToUser(true)
            }
        }
        if (isSetUserVisibleHint || (!isFirstResume && !isHiddenChanged)) {
            isVisibleToUsers = true
        }
        isFirstResume = true
    }

    override fun onPause() {
        super.onPause()
        isHiddenChanged = false
        isSetUserVisibleHint = false
        setVisibleToUser(false)
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        isSetUserVisibleHint = true
        setVisibleToUser(isVisibleToUser)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        isHiddenChanged = true
        setVisibleToUser(!hidden)
    }

    private fun setVisibleToUser(isVisibleToUser: Boolean) {
        if (!isOnCreateView) {
            return
        }
        if (isVisibleToUser == isVisibleToUsers) {
            return
        }
        isVisibleToUsers = isVisibleToUser
        onVisibleToUserChanged(isVisibleToUsers)
    }

    protected open fun onVisibleToUserChanged(isVisibleToUser: Boolean) {
    }
}