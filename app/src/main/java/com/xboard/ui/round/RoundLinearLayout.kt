package com.xboard.ui.round

import com.xboard.R
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.widget.LinearLayout

/**
 *    author : jogger
 *    date   : 9/2/21
 *    desc   :圆角ConstraintLayout
 */
class RoundLinearLayout(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val widget: GradientDrawable = GradientDrawable().parseAttribute(context, attrs)
    private var needChangeAlpha = false

    init {
        this.background = widget
        val a = context.obtainStyledAttributes(attrs, R.styleable.RoundLinearLayout)
        needChangeAlpha = a.getBoolean(R.styleable.RoundLinearLayout_needChangeAlpha, false)
        a.recycle()
    }

    fun setColors(colors: IntArray) {
        widget.colors = colors
    }

    override fun setBackgroundColor(color: Int) {
        widget.setColor(color)
    }
}