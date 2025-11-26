package com.xboard.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.databinding.ItemOrderHistoryBinding
import com.xboard.model.OrderDetailResponse
import java.text.SimpleDateFormat
import java.util.*

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

        fun bind(order: OrderDetailResponse) {
            // 订单号
            binding.tvTradeNo.text = order.tradeNo

            // 套餐名称
            binding.tvPlanName.text = order.plan?.name

            // 价格
            binding.tvPrice.text = "¥${String.format("%.2f", order.payableAmount)}"

            // 创建时间
            binding.tvCreatedAt.text = formatTime(order.createdAt)

            // 状态
            val statusText = when (order.status) {
                0 -> "待支付"
                1 -> "已完成"
                2 -> "已取消"
                else -> "未知状态"
            }
            binding.tvStatus.text = statusText
            val statusColor = when (order.status) {
                1 -> "#0F1419".toColorInt()
                else -> "#808A93".toColorInt()
            }
            binding.tvStatus.setTextColor(statusColor)
            val statusDotColor = when (order.status) {
                0 -> "#F44336".toColorInt()
                1 -> "#259526".toColorInt()
                2 -> "#252B33".toColorInt()
                else -> "#252B33".toColorInt()
            }
            // 根据状态设置颜色
            binding.tvStatus.setTextColor(statusColor)
            binding.vStatus.setBackgroundColor(statusDotColor)

            // 点击事件
            binding.root.setOnClickListener {
                onItemClick(order)
            }
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
