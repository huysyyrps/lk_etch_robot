package com.example.lk_etch_robot.util.popup

import android.content.Context
import android.widget.TextView
import com.example.lk_etch_robot.R
import com.lxj.xpopup.core.BubbleAttachPopupView
import com.lxj.xpopup.util.XPopupUtils

class CustomBubbleAttachPopup : BubbleAttachPopupView {
    var title = ""
    constructor(context: Context, date:String) : super(context) {
        title = date
    }

    override fun getImplLayoutId(): Int {
        return R.layout.popup_item
    }

    override fun onCreate() {
        super.onCreate()
//        setBubbleBgColor(R.color.theme_back_color)
        setBubbleShadowSize(XPopupUtils.dp2px(context, 1f))
        //        setBubbleShadowColor(Color.RED);
        setArrowWidth(XPopupUtils.dp2px(context, 8f))
        setArrowHeight(XPopupUtils.dp2px(context, 5f))
        //                                .setBubbleRadius(100)
        setArrowRadius(XPopupUtils.dp2px(context, 2f))
        val textView = findViewById<TextView>(R.id.tvPopupTitle)
        textView.text = title
    }
}