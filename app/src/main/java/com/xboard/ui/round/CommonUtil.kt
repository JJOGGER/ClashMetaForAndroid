package com.xboard.ui.round

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.github.kr328.clash.R

/**
 *    author : jogger
 *    date   : 9/6/21
 *    desc   :圆角属性配置
 *    支持渐变色、渐变角度、背景色、圆角、描边功能
 */
fun GradientDrawable.parseAttribute(context: Context, attrs: AttributeSet?): GradientDrawable {
    val a = context.obtainStyledAttributes(attrs, R.styleable.GradientDrawable)
    val backgroundColor = a.getColorStateList(R.styleable.GradientDrawable_roundBackgroundColor)
    val radius = a.getDimensionPixelOffset(R.styleable.GradientDrawable_roundCornerRadius, 0)
    val topLeftRadius = a.getDimensionPixelSize(R.styleable.GradientDrawable_roundTopLeftRadius, 0)
    val topRightRadius =
        a.getDimensionPixelSize(R.styleable.GradientDrawable_roundTopRightRadius, 0)
    val bottomLeftRadius =
        a.getDimensionPixelSize(R.styleable.GradientDrawable_roundBottomLeftRadius, 0)
    val bottomRightRadius =
        a.getDimensionPixelSize(R.styleable.GradientDrawable_roundBottomRightRadius, 0)
    val startColor = a.getColor(R.styleable.GradientDrawable_roundStartColor, -2)
    val centerColor = a.getColor(R.styleable.GradientDrawable_roundCenterColor, -2)
    val endColor = a.getColor(R.styleable.GradientDrawable_roundEndColor, -2)
    val angle = a.getInt(R.styleable.GradientDrawable_roundColorAngle, 0)
    val strokeWidth = a.getDimensionPixelSize(R.styleable.GradientDrawable_roundStrokeWidth, 0)
    val strokeColor = a.getColorStateList(R.styleable.GradientDrawable_roundStrokeColor)
    a.recycle()
    if (startColor != -2 && endColor != -2) {
        colors = if (centerColor != -2) {
            intArrayOf(startColor, centerColor, endColor)
        } else {
            intArrayOf(startColor, endColor)
        }
        orientation = when (angle) {
            0 -> GradientDrawable.Orientation.LEFT_RIGHT
            45 -> GradientDrawable.Orientation.BL_TR
            90 -> GradientDrawable.Orientation.BOTTOM_TOP
            135 -> GradientDrawable.Orientation.BR_TL
            180 -> GradientDrawable.Orientation.RIGHT_LEFT
            225 -> GradientDrawable.Orientation.TR_BL
            270 -> GradientDrawable.Orientation.TOP_BOTTOM
            315 -> GradientDrawable.Orientation.TL_BR
            else ->
                GradientDrawable.Orientation.LEFT_RIGHT
        }
    } else {
        color = backgroundColor
    }
    if (topLeftRadius > 0 || topRightRadius > 0 || bottomLeftRadius > 0 || bottomRightRadius > 0) {
        val radii = floatArrayOf(
            topLeftRadius.toFloat(), topLeftRadius.toFloat(),
            topRightRadius.toFloat(), topRightRadius.toFloat(),
            bottomRightRadius.toFloat(), bottomRightRadius.toFloat(),
            bottomLeftRadius.toFloat(), bottomLeftRadius.toFloat()
        )
        cornerRadii = radii
    } else {
        cornerRadius = radius.toFloat()
    }
    setStroke(strokeWidth, strokeColor)
    return this
}

/**
 * 根据比例，在两个color值之间计算出一个color值
 * **注意该方法是ARGB通道分开计算比例的**
 *
 * @param fromColor 开始的color值
 * @param toColor   最终的color值
 * @param fraction  比例，取值为[0,1]，为0时返回 fromColor， 为1时返回 toColor
 * @return 计算出的color值
 */
fun computeColor(
    @ColorInt fromColor: Int,
    @ColorInt toColor: Int,
    fraction: Float
): Int {
    var fraction = fraction
    fraction = constrain(fraction, 0f, 1f)
    val minColorA = Color.alpha(fromColor)
    val maxColorA = Color.alpha(toColor)
    val resultA = ((maxColorA - minColorA) * fraction).toInt() + minColorA
    val minColorR = Color.red(fromColor)
    val maxColorR = Color.red(toColor)
    val resultR = ((maxColorR - minColorR) * fraction).toInt() + minColorR
    val minColorG = Color.green(fromColor)
    val maxColorG = Color.green(toColor)
    val resultG = ((maxColorG - minColorG) * fraction).toInt() + minColorG
    val minColorB = Color.blue(fromColor)
    val maxColorB = Color.blue(toColor)
    val resultB = ((maxColorB - minColorB) * fraction).toInt() + minColorB
    return Color.argb(resultA, resultR, resultG, resultB)
}

fun constrain(amount: Float, low: Float, high: Float): Float {
    return if (amount < low) low else if (amount > high) high else amount
}