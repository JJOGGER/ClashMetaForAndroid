package com.xboard.ui.dialog

import android.view.LayoutInflater
import android.view.ViewGroup
import com.github.kr328.clash.databinding.DialogConfirmBinding

/**
 * 通用确认对话框
 */
class ConfirmDialog : BaseBottomSheetDialog<DialogConfirmBinding>() {

    private var title: String = ""
    private var message: String = ""
    private var positiveButtonText: String = "确定"
    private var negativeButtonText: String = "取消"
    private var onPositiveClick: (() -> Unit)? = null
    private var onNegativeClick: (() -> Unit)? = null
    private var showNegativeButton: Boolean = true

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): DialogConfirmBinding {
        return DialogConfirmBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        binding.tvTitle.text = title
        binding.tvMessage.text = message
        binding.btnPositive.text = positiveButtonText
        binding.btnNegative.text = negativeButtonText
        
        if (!showNegativeButton) {
            binding.btnNegative.visibility = android.view.View.GONE
        }
    }

    override fun initListener() {
        binding.btnPositive.setOnClickListener {
            onPositiveClick?.invoke()
            dismiss()
        }
        
        binding.btnNegative.setOnClickListener {
            onNegativeClick?.invoke()
            dismiss()
        }
    }

    companion object {
        fun newInstance(
            title: String,
            message: String,
            positiveButtonText: String = "确定",
            negativeButtonText: String = "取消",
            showNegativeButton: Boolean = true,
            onPositiveClick: (() -> Unit)? = null,
            onNegativeClick: (() -> Unit)? = null
        ): ConfirmDialog {
            return ConfirmDialog().apply {
                this.title = title
                this.message = message
                this.positiveButtonText = positiveButtonText
                this.negativeButtonText = negativeButtonText
                this.showNegativeButton = showNegativeButton
                this.onPositiveClick = onPositiveClick
                this.onNegativeClick = onNegativeClick
            }
        }
    }
}
