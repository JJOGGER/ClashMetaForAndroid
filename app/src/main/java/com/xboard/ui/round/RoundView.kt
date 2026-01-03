package com.xboard.ui.round

import com.xboard.R
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View

/**
 *    author : jogger
 *    date   : 9/2/21
 *    desc   :圆角textview
 */
class RoundView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val widget: GradientDrawable = GradientDrawable().parseAttribute(context, attrs)
    private var needChangeAlpha = false

    init {
        this.background = widget
        val a = context.obtainStyledAttributes(attrs, R.styleable.RoundView)
        needChangeAlpha = a.getBoolean(R.styleable.RoundView_needChangeAlpha, false)
        a.recycle()
    }



    fun setColors(colors: IntArray) {
        widget.colors = colors
    }

    override fun setBackgroundColor(color: Int) {
        widget.setColor(color)
    }


}