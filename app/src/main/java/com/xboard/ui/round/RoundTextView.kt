package com.xboard.ui.round

import com.github.kr328.clash.R
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 *    author : jogger
 *    date   : 9/2/21
 *    desc   :圆角textview
 */
class RoundTextView(context: Context, attrs: AttributeSet?) : AppCompatTextView(context, attrs) {
    constructor(context: Context) : this(context, null)

    val widget: GradientDrawable = GradientDrawable().parseAttribute(context, attrs)
    private var needChangeAlpha = false

    init {
        this.background = widget
        val a = context.obtainStyledAttributes(attrs, R.styleable.RoundTextView)
        needChangeAlpha = a.getBoolean(R.styleable.RoundTextView_needChangeAlpha, false)
        a.recycle()
    }

    fun setStroke(strokeWidth: Int, strokeColor: Int) {

        widget.setStroke(strokeWidth, strokeColor)
    }


    fun setNeedChangeAlpha(needChangeAlpha: Boolean) {
        this.needChangeAlpha = needChangeAlpha
    }

    fun setColors(colors: IntArray) {
        widget.colors = colors
    }

    override fun setBackgroundColor(color: Int) {
        widget.setColor(color)
    }
}