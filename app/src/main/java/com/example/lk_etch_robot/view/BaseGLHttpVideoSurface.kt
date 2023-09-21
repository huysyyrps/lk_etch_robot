package com.example.lk_etch_robot.view

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.annotation.RequiresApi
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.skydroid.fpvlibrary.widget.GLHttpVideoSurface


class BaseGLHttpVideoSurface : GLHttpVideoSurface {
    private var downX = 0
    private var downY = 0
    private val isMove = false

    //屏幕密度
    private val density = resources.displayMetrics.density

    @RequiresApi(Build.VERSION_CODES.M)
    constructor(context: Context) : super(context) {
    }

    @RequiresApi(Build.VERSION_CODES.M)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                //相对于控件左边缘的距离
                downX = event.rawX.toInt()
                downY = event.rawY.toInt()
            }
            MotionEvent.ACTION_MOVE -> {
                val moveX = event.rawX.toInt()
                val moveY = event.rawY.toInt()
                val dx: Int = moveX - downX
                val dy: Int = moveY - downY
                val layoutParams = layoutParams as CoordinatorLayout.LayoutParams
                layoutParams.leftMargin = layoutParams.leftMargin + (dx * density) as Int
                layoutParams.topMargin = layoutParams.topMargin + (dy * density) as Int
                setLayoutParams(layoutParams)
                requestLayout()
                downX = moveX
                downY = moveY
            }
            MotionEvent.ACTION_UP -> {}
        }
        return isMove
    }

}