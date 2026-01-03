package com.xboard.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.xboard.databinding.ItemPlanBinding
import com.xboard.model.Plan
import java.text.DecimalFormat

/**
 * 套餐列表适配器
 */
class PlanAdapter(private val onPlanClick: (Plan) -> Unit) :
    RecyclerView.Adapter<PlanAdapter.PlanViewHolder>() {

    private val plans = mutableListOf<Plan>()
    private val priceFormatter = DecimalFormat("#.##")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val binding = ItemPlanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        holder.bind(plans[position])
    }

    override fun getItemCount(): Int = plans.size

    fun updateData(newPlans: List<Plan>) {
        val diffResult = DiffUtil.calculateDiff(PlanDiffCallback(plans, newPlans))
        plans.clear()
        plans.addAll(newPlans)
        diffResult.dispatchUpdatesTo(this)
    }

    private fun formatPrice(amount: Double): String {
        // Round to avoid floating point precision issues
        val rounded = Math.round(amount * 100.0) / 100.0
        return priceFormatter.format(rounded)
    }

    inner class PlanViewHolder(private val binding: ItemPlanBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(plan: Plan) {
            binding.tvPlanName.text = plan.name
            binding.tvPlanPrice.text = "¥ ${plan.getShowPrice().second}"
            binding.tvPriceType.text = plan.getShowPrice().first
            if (plan.transferEnable >= Integer.MAX_VALUE) {
                binding.tvPlanTraffic.text = " 无限制"
            } else {
                binding.tvPlanTraffic.text = " ${plan.transferEnable} GB"
            }

            binding.root.setOnClickListener {
                onPlanClick(plan)
            }
        }

    }

    private class PlanDiffCallback(
        private val oldList: List<Plan>,
        private val newList: List<Plan>
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
