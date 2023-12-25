package com.example.lk_etch_robot.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.lk_etch_robot.R


class BaseElectricity constructor(context: Context?, attrs: AttributeSet? = null) : View(context, attrs) {
    var paintFill = Paint()
    var paintFillRed = Paint()
    var paintText = Paint()
    var electricity = 0
    fun baseElectricity() {
        paintFill.isAntiAlias = true//是否抗锯齿
        paintFill.style = Paint.Style.FILL// 描边填充效果 1.STROKE 描边 2.FIll 填充 3.FILL_AND_STROKE 描边+填充
        paintFill.style = Paint.Style.STROKE
        paintFill.strokeWidth = 5F
        paintFill.color = resources.getColor(R.color.electricity_color)

        paintFillRed.isAntiAlias = true//是否抗锯齿
        paintFillRed.style = Paint.Style.FILL// 描边填充效果 1.STROKE 描边 2.FIll 填充 3.FILL_AND_STROKE 描边+填充
        paintFillRed.style = Paint.Style.STROKE
        paintFillRed.strokeWidth = 5F
        paintFillRed.color = resources.getColor(R.color.red)

        paintText.isAntiAlias = true//是否抗锯齿
        paintText.strokeWidth = 5F
        paintText.color = resources.getColor(R.color.red)
        paintText.textSize = 26F
    }

    // android.view.View
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (electricity == 100) {
            canvas.drawArc(0F, 3F, 70F, 73F, 0F, 360F, false, paintFill)
            paintText.color = resources.getColor(R.color.electricity_color)
            canvas.drawText(electricity.toString(), 12F, 45F, paintText)
        } else {
            if (electricity > 10) {
                canvas.drawArc(0F, 3F, 70F, 73F, -90F, 36F, false, paintFillRed)
                canvas.drawArc(0F, 3F, 70F, 73F, -54F, 336F * ((electricity-10) / 90F), false, paintFill)
                paintText.color = resources.getColor(R.color.electricity_color)
                canvas.drawText(electricity.toString(), 20F, 45F, paintText)
            } else if (electricity < 10) {
                canvas.drawArc(0F, 3F, 70F, 73F, -90F, electricity*3.6F, false, paintFillRed)
                paintText.textSize=35F
                canvas.drawText(electricity.toString(), 28F, 48F, paintText)
            }else if (electricity == 10) {
                canvas.drawArc(0F, 3F, 70F, 73F, -59F, electricity*3.6F, false, paintFillRed)
                paintText.textSize=35F
                canvas.drawText(electricity.toString(), 20F, 48F, paintText)
            }
        }
    }

    @Synchronized
    fun setElectricity1(electricity: Int) {
        this.electricity = electricity
        postInvalidate()
    }

}