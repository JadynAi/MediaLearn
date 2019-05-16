package com.jadyn.mediakit.camera2

import android.content.Context
import android.graphics.Matrix
import android.util.AttributeSet
import android.util.Size
import android.view.MotionEvent
import android.view.TextureView

/**
 *@version:
 *@FileDescription:
 *@Author:jing
 *@Since:2018/9/12
 *@ChangeList:
 */
class AutoFitTextureView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
) : TextureView(context, attrs, defStyle) {

    private var ratioWidth = 0
    private var ratioHeight = 0
    var isFullScreen = false
        private set
    private var defTransform: Matrix? = null
    private val fullScreenTransform by lazy {
        Matrix()
    }

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    fun setAspectRatio(width: Int, height: Int) {
        if (width < 0 || height < 0) {
            throw IllegalArgumentException("Size cannot be negative.")
        }
        ratioWidth = width
        ratioHeight = height
        requestLayout()
    }

    fun setAspectRatio(size: Size) {
        setAspectRatio(size.width, size.height)
    }

    override fun setTransform(transform: Matrix?) {
        if (defTransform == null) {
            defTransform = transform
        }
        super.setTransform(transform)
    }

    fun toggleFullscreen() {
        isFullScreen = !isFullScreen
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        if (ratioWidth == 0 || ratioHeight == 0) {
            setMeasuredDimension(width, height)
        } else {
            if (isFullScreen) {
                val w = resources.displayMetrics.widthPixels
                val h = resources.displayMetrics.heightPixels
                setMeasuredDimension(w, h)
                fullScreenTransform.reset()
                fullScreenTransform.set(defTransform)
                // 宽拉伸，高不变
                fullScreenTransform.postScale(h.toFloat() / ratioHeight,
                        1f, w * 0.5f, h * 0.5f)
                setTransform(fullScreenTransform)
            } else {
                setTransform(defTransform)
                if (width < ((height * ratioWidth) / ratioHeight)) {
                    // 控件本身的宽小于 根据比例计算来得宽，则使用控件本身的宽
                    setMeasuredDimension(width, (width * ratioHeight) / ratioWidth)
                } else {
                    setMeasuredDimension((height * ratioWidth) / ratioHeight, height)
                }
            }
        }
    }
    
    private val gestureDetector by lazy { 
        
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return super.onTouchEvent(event)
    }
}