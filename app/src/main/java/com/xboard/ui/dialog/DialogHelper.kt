package com.xboard.ui.dialog

import androidx.fragment.app.Fragment

/**
 * 对话框辅助类，简化 DialogFragment 的使用
 */
object DialogHelper {

    /**
     * 显示确认对话框
     */
    fun showConfirmDialog(
        fragment: Fragment,
        title: String,
        message: String,
        positiveButtonText: String = "确定",
        negativeButtonText: String = "取消",
        showNegativeButton: Boolean = true,
        onPositiveClick: (() -> Unit)? = null,
        onNegativeClick: (() -> Unit)? = null
    ) {
        val dialog = ConfirmDialog.newInstance(
            title = title,
            message = message,
            positiveButtonText = positiveButtonText,
            negativeButtonText = negativeButtonText,
            showNegativeButton = showNegativeButton,
            onPositiveClick = onPositiveClick,
            onNegativeClick = onNegativeClick
        )
        dialog.show(fragment.childFragmentManager, "ConfirmDialog")
    }

    /**
     * 显示简单提示对话框（只有确定按钮）
     */
    fun showSimpleDialog(
        fragment: Fragment,
        title: String,
        message: String,
        buttonText: String = "知道了",
        onDismiss: (() -> Unit)? = null
    ) {
        val dialog = ConfirmDialog.newInstance(
            title = title,
            message = message,
            positiveButtonText = buttonText,
            showNegativeButton = false,
            onPositiveClick = onDismiss
        )
        dialog.show(fragment.childFragmentManager, "SimpleDialog")
    }
}
