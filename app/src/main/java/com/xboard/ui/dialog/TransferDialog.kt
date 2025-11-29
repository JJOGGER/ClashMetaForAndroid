package com.xboard.ui.dialog

import android.view.LayoutInflater
import android.view.ViewGroup
import com.github.kr328.clash.databinding.DialogTransferBinding

/**
 * 转账对话框
 */
class TransferDialog : BaseBottomSheetDialog<DialogTransferBinding>() {

    private var maxBalance: Double = 0.0
    private var onConfirm: ((amount: String) -> Unit)? = null

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): DialogTransferBinding {
        return DialogTransferBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        binding.tvCurrentBalance.text = formatPrice(maxBalance)
    }

    override fun initListener() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnConfirm.setOnClickListener {
            val amount = binding.etTransferAmount.text.toString().trim()
            if (amount.isNotEmpty()) {
                onConfirm?.invoke(amount)
                dismiss()
            }
        }
    }

    private fun formatPrice(price: Double): String {
        return String.format("%.2f", price)
    }

    companion object {
        fun newInstance(
            maxBalance: Double,
            onConfirm: ((amount: String) -> Unit)? = null
        ): TransferDialog {
            return TransferDialog().apply {
                this.maxBalance = maxBalance
                this.onConfirm = onConfirm
            }
        }
    }
}
