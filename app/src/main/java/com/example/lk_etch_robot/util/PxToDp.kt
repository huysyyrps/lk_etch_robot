package com.example.lk_etch_robot.util

import android.util.TypedValue
import com.example.lk_etch_robot.MyApplication.Companion.context

object PxToDp {
    fun pxToDp(px:Int):Int {
        var scale = context.resources.displayMetrics.density;
        return  ((px / scale + 0.5f).toInt())
    }
}