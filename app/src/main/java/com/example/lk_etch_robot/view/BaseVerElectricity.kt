package com.example.lk_etch_robot.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.lk_etch_robot.R


class BaseVerElectricity constructor(context: Context?, attrs: AttributeSet? = null) : View(context, attrs) {
    var paintFill = Paint()
    var paintBor = Paint()
    var electricity = 0

    fun baseElectricity() {
        paintFill.isAntiAlias = true//是否抗锯齿
        paintFill.style = Paint.Style.FILL// 描边填充效果 1.STROKE 描边 2.FIll 填充 3.FILL_AND_STROKE 描边+填充
        paintFill.strokeWidth = 5F
        paintFill.color = resources.getColor(R.color.white)

        paintBor.isAntiAlias = true//是否抗锯齿
        paintBor.style = Paint.Style.STROKE// 描边填充效果 1.STROKE 描边 2.FIll 填充 3.FILL_AND_STROKE 描边+填充
        paintBor.strokeWidth = 1F
        paintBor.color = resources.getColor(R.color.white)

    }

    // android.view.View
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (electricity<=10){
            paintFill.color = resources.getColor(R.color.red)
            paintBor.color = resources.getColor(R.color.red)
        }else if (electricity in 11..19){
            paintFill.color = resources.getColor(R.color.theme_color)
            paintBor.color = resources.getColor(R.color.theme_color)
        }else{
            paintFill.color = resources.getColor(R.color.white)
            paintBor.color = resources.getColor(R.color.white)
        }
        canvas.drawRect(0F, 10F, 35F, 70F, paintBor)
        canvas.drawRect(0F, 70-70F*(electricity/100F)+10, 35F, 70F, paintFill)
        canvas.drawRect(10F, 3F, 25F, 10F, paintFill)
    }

    @Synchronized
    fun setElectricity1(electricity: Int) {
        this.electricity = electricity
        postInvalidate()
    }

}