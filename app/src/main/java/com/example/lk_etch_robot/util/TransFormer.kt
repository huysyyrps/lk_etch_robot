package com.example.lk_etch_robot.util

import android.view.View
import androidx.viewpager2.widget.ViewPager2

class TransFormer : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        if (position >= -1.0f && position <= 0.0f) {
            //控制左侧滑入或者滑出的缩放比例
            page.scaleX = 1 + position * 0.1f;
            page.scaleY = 1 + position * 0.2f;
        } else if (position > 0.0f && position < 1.0f) {
            //控制右侧滑入或者滑出的缩放比例
            page.scaleX = 1 - position * 0.1f;
            page.scaleY = 1 - position * 0.2f;
        } else {
            //控制其他View缩放比例
            page.scaleX = 0.9f;
            page.scaleY = 0.8f;
        }
    }
}