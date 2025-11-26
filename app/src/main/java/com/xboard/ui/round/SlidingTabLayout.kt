package com.xboard.ui.round

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.github.kr328.clash.R

class SlidingTabLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HorizontalScrollView(context, attrs, defStyleAttr) {

    private val tabContainer = LinearLayout(context)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var selectedPosition = 0
    private var tabWidths = mutableListOf<Int>()
    private var onTabSelectedListener: ((Int) -> Unit)? = null
    private var tabTextColors = intArrayOf(
        ContextCompat.getColor(context, R.color.white),
        ContextCompat.getColor(context, R.color.white)
    )
    private val cornerRadius = context.resources.getDimension(R.dimen.sliding_tab_corner_radius)
    private val tabHeight = context.resources.getDimension(R.dimen.sliding_tab_height)

    var tabs: List<String> = emptyList()
        set(value) {
            field = value
            setupTabs()
        }

    var selectedTabPosition: Int = 0
        set(value) {
            if (value in tabs.indices) {
                field = value
                selectedPosition = value
                updateTabStyles()
                smoothScrollToSelectedTab()
            }
        }

    init {
        isHorizontalScrollBarEnabled = false
        tabContainer.orientation = LinearLayout.HORIZONTAL
        tabContainer.gravity = Gravity.CENTER
        addView(tabContainer, ViewGroup.LayoutParams.WRAP_CONTENT, tabHeight.toInt())

        // Set background paint
        backgroundPaint.style = Paint.Style.FILL
        backgroundPaint.color = ContextCompat.getColor(context, R.color.primary_light)

        // Set padding from design library
//        setPadding(
//            context.resources.getDimensionPixelSize(R.dimen.divider_size),
//            context.resources.getDimensionPixelSize(R.dimen.divider_size),
//            context.resources.getDimensionPixelSize(R.dimen.divider_size),
//            context.resources.getDimensionPixelSize(R.dimen.divider_size)
//        )

        // Set background
        setBackgroundResource(R.drawable.sliding_tab_background)

        // 初始化tabs
        tabs = listOf("智能", "全局")
    }

    fun setOnTabSelectedListener(listener: (Int) -> Unit) {
        onTabSelectedListener = listener
    }

    private fun setupTabs() {
        tabContainer.removeAllViews()
        tabWidths.clear()

        tabs.forEachIndexed { index, title ->
            val tabView = createTabView(title, index)
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            tabContainer.addView(tabView, params)
        }

        // Measure all tab widths
        post {
            tabWidths.clear()
            for (i in 0 until tabContainer.childCount) {
                tabWidths.add(tabContainer.getChildAt(i).measuredWidth)
            }
        }
    }

    private fun createTabView(title: String, position: Int): View {
        return object : FrameLayout(context) {
            private val selectedBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

            init {
                selectedBgPaint.style = Paint.Style.FILL
                selectedBgPaint.color = ContextCompat.getColor(context, R.color.primary)

                val textView = TextView(context).apply {
                    text = title
                    textSize = 14f
                    gravity = Gravity.CENTER
                    setPadding(
                        context.resources.getDimensionPixelSize(R.dimen.slding_pading),
                        0,
                        context.resources.getDimensionPixelSize(R.dimen.slding_pading),
                        0
                    )
                }

                addView(
                    textView,
                    LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.CENTER
                    })

                setOnClickListener {
                    if (position != selectedPosition) {
                        selectedTabPosition = position
                        onTabSelectedListener?.invoke(position)
                    }
                }
            }

            override fun dispatchDraw(canvas: Canvas) {
                // Draw selected background with rounded corners
                if (position == selectedPosition) {
                    val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
                    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, selectedBgPaint)
                }

                super.dispatchDraw(canvas)

                // Update TextView color
                val textView = getChildAt(0) as? TextView
                textView?.setTextColor(if (position == selectedPosition) tabTextColors[1] else tabTextColors[0])
            }
        }
    }

    private fun updateTabStyles() {
        for (i in 0 until tabContainer.childCount) {
            tabContainer.getChildAt(i).invalidate()
        }
    }

    private fun smoothScrollToSelectedTab() {
        if (tabWidths.isEmpty() || selectedPosition >= tabWidths.size) return

        // Scroll to center the selected tab
        val child = tabContainer.getChildAt(selectedPosition)
        val scrollPos = child.left + child.width / 2
        val screenWidth = width
        val scrollTo = scrollPos - screenWidth / 2
        smoothScrollTo(scrollTo, 0)
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
    }
}