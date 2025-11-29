package com.xboard.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import com.github.kr328.clash.R

/**
 * 自定义圆角 ImageView
 * 支持统一圆角和分别设置四个角的圆角
 */
@SuppressLint("AppCompatCustomView")
class RoundCornerImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {

    private var cornerRadius = 0f
    private var topLeftRadius = 0f
    private var topRightRadius = 0f
    private var bottomLeftRadius = 0f
    private var bottomRightRadius = 0f
    private val path = Path()
    private val rectF = RectF()

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.RoundCornerImageView)
        try {
            cornerRadius = typedArray.getDimension(R.styleable.RoundCornerImageView_cornerRadius, 0f)
            topLeftRadius = typedArray.getDimension(R.styleable.RoundCornerImageView_topLeftRadius, cornerRadius)
            topRightRadius = typedArray.getDimension(R.styleable.RoundCornerImageView_topRightRadius, cornerRadius)
            bottomLeftRadius = typedArray.getDimension(R.styleable.RoundCornerImageView_bottomLeftRadius, cornerRadius)
            bottomRightRadius = typedArray.getDimension(R.styleable.RoundCornerImageView_bottomRightRadius, cornerRadius)
        } finally {
            typedArray.recycle()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()

        // 创建圆角路径
        path.reset()
        rectF.set(0f, 0f, width, height)

        val radii = floatArrayOf(
            topLeftRadius, topLeftRadius,
            topRightRadius, topRightRadius,
            bottomRightRadius, bottomRightRadius,
            bottomLeftRadius, bottomLeftRadius
        )

        path.addRoundRect(rectF, radii, Path.Direction.CW)
        canvas.clipPath(path)

        super.onDraw(canvas)
    }

    /**
     * 设置统一的圆角半径
     */
    fun setCornerRadius(radius: Float) {
        this.cornerRadius = radius
        this.topLeftRadius = radius
        this.topRightRadius = radius
        this.bottomLeftRadius = radius
        this.bottomRightRadius = radius
        invalidate()
    }

    /**
     * 分别设置四个角的圆角半径
     */
    fun setCornerRadius(topLeft: Float, topRight: Float, bottomRight: Float, bottomLeft: Float) {
        this.topLeftRadius = topLeft
        this.topRightRadius = topRight
        this.bottomRightRadius = bottomRight
        this.bottomLeftRadius = bottomLeft
        invalidate()
    }

    /**
     * 设置顶部圆角
     */
    fun setTopCornerRadius(radius: Float) {
        this.topLeftRadius = radius
        this.topRightRadius = radius
        invalidate()
    }

    /**
     * 设置底部圆角
     */
    fun setBottomCornerRadius(radius: Float) {
        this.bottomLeftRadius = radius
        this.bottomRightRadius = radius
        invalidate()
    }
}
