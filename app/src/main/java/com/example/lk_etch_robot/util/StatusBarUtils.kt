package com.example.lk_etch_robot.util

import android.app.Activity
import android.os.Build
import android.view.WindowManager

object StatusBarUtils {
    fun setWindowStatusBarColor(activity: Activity, colorResId:Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val window = activity.getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(activity.getResources().getColor(colorResId));

                //底部导航栏
                //window.setNavigationBarColor(activity.getResources().getColor(colorResId));
            }
        } catch (e: Exception) {
            e.printStackTrace();
        }
    }
}