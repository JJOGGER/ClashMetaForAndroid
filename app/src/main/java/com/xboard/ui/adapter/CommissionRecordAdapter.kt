package com.xboard.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.xboard.databinding.ItemCommissionRecordBinding
import com.xboard.model.InviteDetail
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 佣金发放记录列表 Adapter
 */
class CommissionRecordAdapter : RecyclerView.Adapter<CommissionRecordAdapter.CommissionRecordViewHolder>() {

    private val records = mutableListOf<InviteDetail>()
    private val priceFormatter = DecimalFormat("#.##")
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun updateData(newRecords: List<InviteDetail>) {
        val diffCallback = CommissionRecordDiffCallback(records, newRecords)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        records.clear()
        records.addAll(newRecords)
        diffResult.dispatchUpdatesTo(this)
    }

    fun appendData(newRecords: List<InviteDetail>) {
        val startPosition = records.size
        records.addAll(newRecords)
        notifyItemRangeInserted(startPosition, newRecords.size)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CommissionRecordViewHolder {
        val binding = ItemCommissionRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CommissionRecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommissionRecordViewHolder, position: Int) {
        holder.bind(records[position])
    }

    override fun getItemCount(): Int = records.size

    inner class CommissionRecordViewHolder(private val binding: ItemCommissionRecordBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(record: InviteDetail) {
            // 邀请用户 ID
            binding.tvInviteUserId.text = "用户 ID: ${record.inviteUserId}"
            
            // 订单 ID
            binding.tvOrderId.text = "订单 ID: ${record.orderId}"
            
            // 佣金金额（单位是分，需要转换为元）
            val amount = record.getAmount / 100.0
            binding.tvCommissionAmount.text = "¥${formatPrice(amount)}"
            
            // 创建时间
            binding.tvCreatedAt.text = formatDate(record.createdAt)
        }

        private fun formatPrice(amount: Double): String {
            return priceFormatter.format(amount)
        }

        private fun formatDate(dateString: String): String {
            return try {
                // 将 ISO 8601 格式转换为本地格式
                // 例如: "2025-01-01T00:00:00Z" -> "2025-01-01 00:00"
                val parts = dateString.split("T")
                if (parts.size >= 2) {
                    val date = parts[0]
                    val time = parts[1].split(":").take(2).joinToString(":")
                    "$date $time"
                } else {
                    dateString
                }
            } catch (e: Exception) {
                dateString
            }
        }
    }

    private class CommissionRecordDiffCallback(
        private val oldList: List<InviteDetail>,
        private val newList: List<InviteDetail>
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
