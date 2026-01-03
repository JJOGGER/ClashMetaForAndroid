package com.xboard.ui.round

import com.xboard.R
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 *    author : jogger
 *    date   : 9/2/21
 *    desc   :圆角FrameLayout
 */
open class RoundFrameLayout(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    private val widget: GradientDrawable = GradientDrawable().parseAttribute(context, attrs)
    private var needChangeAlpha = false

    init {
        this.background = widget
        val a = context.obtainStyledAttributes(attrs, R.styleable.RoundFrameLayout)
        needChangeAlpha = a.getBoolean(R.styleable.RoundFrameLayout_needChangeAlpha, false)
        a.recycle()
    }

    fun setColors(colors: IntArray) {
        widget.colors = colors
    }

    override fun setBackgroundColor(color: Int) {
        widget.setColor(color)
    }

}