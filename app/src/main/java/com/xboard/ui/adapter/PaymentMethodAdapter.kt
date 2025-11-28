package com.xboard.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.databinding.ItemPaymentMethodBinding
import com.xboard.model.PaymentMethod

/**
 * 支付方式列表适配器
 */
class PaymentMethodAdapter(private val onMethodSelect: (PaymentMethod) -> Unit) :
    RecyclerView.Adapter<PaymentMethodAdapter.PaymentMethodViewHolder>() {

    private val methods = mutableListOf<PaymentMethod>()
    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentMethodViewHolder {
        val binding = ItemPaymentMethodBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PaymentMethodViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PaymentMethodViewHolder, position: Int) {
        holder.bind(methods[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = methods.size

    fun updateData(newMethods: List<PaymentMethod>) {
        val diffResult = DiffUtil.calculateDiff(PaymentMethodDiffCallback(methods, newMethods))
        methods.clear()
        methods.addAll(newMethods)
        diffResult.dispatchUpdatesTo(this)

        if (selectedPosition >= methods.size) {
            selectedPosition = -1
        }
    }

    inner class PaymentMethodViewHolder(private val binding: ItemPaymentMethodBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(method: PaymentMethod, isSelected: Boolean) {
            binding.tvMethodName.text = method.name
            binding.tvMethodFee.text = "手续费: ${(method.feePercent * 100).toInt()}%"
            binding.cbSelect.isChecked = isSelected

            binding.root.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = bindingAdapterPosition
                if (previousPosition >= 0) {
                    notifyItemChanged(previousPosition)
                }
                notifyItemChanged(selectedPosition)
                onMethodSelect(method)
            }
        }
    }

    private class PaymentMethodDiffCallback(
        private val oldList: List<PaymentMethod>,
        private val newList: List<PaymentMethod>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
