package com.xboard.utils

import android.view.View
import com.github.kr328.clash.R

fun throttleClick(wait: Long = 200, block: ((View) -> Unit)): View.OnClickListener {

    return View.OnClickListener { v ->
        val current = System.currentTimeMillis()
        val lastClickTime = (v.getTag(R.id.qmui_click_timestamp) as? Int) ?: 0
        if (current - lastClickTime > wait) {
            block(v)
            v.setTag(R.id.qmui_click_timestamp, current)
        }
    }
}


fun View.onClick(wait: Long = 500, block: ((View) -> Unit)) {
    setOnClickListener(throttleClick(wait, block))
}
