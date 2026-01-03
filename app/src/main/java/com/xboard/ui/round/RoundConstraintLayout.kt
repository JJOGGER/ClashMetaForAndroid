package com.xboard.ui.round

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.xboard.R

/**
 *    author : jogger
 *    date   : 9/2/21
 *    desc   :圆角ConstraintLayout
 */
class RoundConstraintLayout(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {
    private val widget: GradientDrawable = GradientDrawable().parseAttribute(context, attrs)
    private var needChangeAlpha = false

    init {
        this.background = widget
        val a = context.obtainStyledAttributes(attrs, R.styleable.RoundConstraintLayout)
        needChangeAlpha = a.getBoolean(R.styleable.RoundConstraintLayout_needChangeAlpha, false)
        a.recycle()
    }

    fun setColors(colors: IntArray) {
        widget.colors = colors
    }

    override fun setBackgroundColor(color: Int) {
        widget.setColor(color)
    }
}