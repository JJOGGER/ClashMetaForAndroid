package com.xboard.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.databinding.ItemOrderHistoryBinding
import com.xboard.model.OrderDetailResponse
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

/**
 * 订单历史列表适配器
 */
class OrderHistoryAdapter(
    private val onItemClick: (OrderDetailResponse) -> Unit
) : RecyclerView.Adapter<OrderHistoryAdapter.OrderViewHolder>() {

    private val orders = mutableListOf<OrderDetailResponse>()

    fun updateData(newOrders: List<OrderDetailResponse>) {
        orders.clear()
        orders.addAll(newOrders)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount(): Int = orders.size

    inner class OrderViewHolder(
        private val binding: ItemOrderHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(order: OrderDetailResponse) {
            // 订单号
            binding.tvTradeNo.text = order.tradeNo

            // 套餐名称
            binding.tvPlanName.text = order.plan?.name

            // 价格
            binding.tvPrice.text =
                "¥${formatPrice(order.plan?.getRealPlanPrice(order.period) ?: 0.0)}"

            // 创建时间
            binding.tvCreatedAt.text = formatTime(order.createdAt)

            // 状态
            val statusText = order.getStatusText()
            binding.tvStatus.text = statusText
            // 根据状态设置颜色
            binding.tvStatus.setTextColor(order.getStatusColor())
            binding.vStatus.setBackgroundColor(order.getStatusColor())

            // 点击事件
            binding.root.setOnClickListener {
                onItemClick(order)
            }
        }

        private val priceFormatter = DecimalFormat("#.##")
        private fun formatPrice(amount: Double): String {
            // Round to avoid floating point precision issues
            val rounded = (max(amount, 0.0)) / 100.0
            return priceFormatter.format(rounded)
        }

        private fun formatTime(timestamp: Long): String {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                sdf.format(Date(timestamp * 1000))
            } catch (e: Exception) {
                "未知时间"
            }
        }
    }
}
