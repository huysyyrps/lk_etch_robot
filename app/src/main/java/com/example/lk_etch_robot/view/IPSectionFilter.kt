package com.example.lk_etch_robot.view

import android.text.InputFilter
import android.text.Spanned

class IPSectionFilter : InputFilter {
    private var min = 0
    private var max = 0

    constructor(setMin:Int, setMax:Int){
        max = setMax
        min = setMin
    }

    override fun filter(source: CharSequence?, start: Int, end: Int, dest: Spanned?, dstart: Int, dend: Int): CharSequence {
        var sourceText = source.toString()
        var destText = dest.toString()
        if (dstart == 0 && "0".equals(source)) {
            //如果输入是0 且位置在第一位，取消输入
            return ""
        }

        var totalText = StringBuilder()
        totalText.append(destText.substring(0, dstart))
            .append(sourceText)
            .append(destText.substring(dstart, destText.length));


        try {
            if (Integer.parseInt(totalText.toString()) > max) {
                return ""
            } else if (Integer.parseInt(totalText.toString()) == 0) {
                //如果输入是0，取消输入
                return ""
            }
        } catch (e:Exception) {
            return "";
        }
        if ("".equals(source.toString())) {
            return "";
        }
        return "" + Integer.parseInt(source.toString());
    }
}