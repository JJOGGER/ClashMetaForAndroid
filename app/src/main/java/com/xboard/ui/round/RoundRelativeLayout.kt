package com.xboard.ui.round

import com.xboard.R
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.widget.RelativeLayout

/**
 *  圆角RelativeLayout
 *  @author: zzq 2022/5/27
 */
class RoundRelativeLayout(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
    private val widget: GradientDrawable = GradientDrawable().parseAttribute(context, attrs)
    private var needChangeAlpha = false

    init {
        this.background = widget
        val a = context.obtainStyledAttributes(attrs, R.styleable.RoundRelativeLayout)
        needChangeAlpha = a.getBoolean(R.styleable.RoundRelativeLayout_needChangeAlpha, false)
        a.recycle()
    }

    fun setColors(colors: IntArray) {
        widget.colors = colors
    }

    override fun setBackgroundColor(color: Int) {
        widget.setColor(color)
    }
}