package com.example.lk_etch_robot.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.internal.view.SupportMenu
import com.example.lk_etch_robot.R
import com.example.lk_etch_robot.util.LogUtil


/**
 * 抬升高度
 */
 class HeightView constructor(context: Context?, attrs: AttributeSet? = null) : View(context, attrs) {
    private var percent = 0
    var paintFill = Paint()
    var paintPowerBody = Paint()
    var paintPowerHeader = Paint()
    var paintText = Paint()

    fun HeightView() {
        paintFill.isAntiAlias = true//是否抗锯齿
        paintFill.style = Paint.Style.FILL// 描边填充效果 1.STROKE 描边 2.FIll 填充 3.FILL_AND_STROKE 描边+填充

        paintPowerBody.isAntiAlias = true
        paintPowerBody.style = Paint.Style.FILL
        paintPowerBody.strokeWidth = dip2px(1.0f).toFloat()
        paintPowerBody.color = resources.getColor(R.color.red)

        paintPowerHeader.isAntiAlias = true
        paintPowerHeader.style = Paint.Style.FILL
        paintPowerHeader.color = resources.getColor(R.color.theme_color)

        paintText.isAntiAlias = true
        paintText.style = Paint.Style.FILL
        paintText.color = resources.getColor(R.color.white)

        val dm = resources.displayMetrics
        val mScreenWidth = dm.widthPixels
        val mScreenHeight = dm.heightPixels
        val ratioWidth = 60F
        val ratioHeight = mScreenHeight-100F
        val ratioMetrics = Math.min(ratioWidth, ratioHeight)
        val textSize = Math.round(25.0f * ratioMetrics)
        paintText.textSize = textSize.toFloat()
    }

    private fun dip2px(dpValue: Float): Int {
        val scale = resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    // android.view.View
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paintFill.color = resources.getColor(R.color.theme_color)
        paintPowerBody.color = resources.getColor(R.color.red)
        val a = width - dip2px(2.0f)
        val b = height - dip2px(3.0f)
        val left = dip2px(0.5f).toFloat()
        val top = dip2px(0.5f).toFloat()
        val right = dip2px(2.5f).toFloat()
        val bottom = dip2px(0.5f).toFloat()
        val textRectF = RectF(10.0f, 9.4f, a.toFloat()-27, b.toFloat()*(100-percent)/100)
        val powerBodyRectF = RectF(10.0f, 10.0f, a.toFloat()-27, (b.toFloat()-10))
        canvas.drawRoundRect(powerBodyRectF, 18.0f, 18.0f, paintPowerBody)
        canvas.drawRoundRect(textRectF,18.0f, 18.0f, paintFill)
//        canvas.drawText(percent.toString(), 100F, 100F, paintText)
    }

    @Synchronized
    fun setHeight(percent: Int) {
        this.percent = percent
        postInvalidate()
    }
}