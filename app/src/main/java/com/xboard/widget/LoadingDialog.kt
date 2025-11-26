package com.xboard.widget

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.WindowManager
import com.github.kr328.clash.R
import com.github.kr328.clash.databinding.DialogLoadingBinding

/**
 * 加载对话框
 */
class LoadingDialog(context: Context) : Dialog(context, R.style.LoadingDialogStyle) {

    private val binding: DialogLoadingBinding

    init {
        binding = DialogLoadingBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        // 设置对话框属性
        val window = window
        window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            val params = attributes
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            attributes = params
        }

        // 禁止返回键关闭
        setCancelable(false)
        setCanceledOnTouchOutside(false)
    }

    /**
     * 设置加载文本
     */
    fun setLoadingText(text: String) {
        binding.tvLoadingText.text = text
    }

    /**
     * 显示加载对话框
     */
    fun showLoading(text: String = "加载中...") {
        setLoadingText(text)
        show()
    }

    /**
     * 隐藏加载对话框
     */
    fun hideLoading() {
        if (isShowing) {
            dismiss()
        }
    }
}
