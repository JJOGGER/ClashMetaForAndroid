package com.xboard.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.databinding.ItemInviteDetailBinding
import com.xboard.model.InviteDetail
import java.text.DecimalFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * 邀请明细列表 Adapter
 */
class InviteDetailAdapter : RecyclerView.Adapter<InviteDetailAdapter.InviteDetailViewHolder>() {

    private val inviteDetails = mutableListOf<InviteDetail>()
    private val serverDateParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val localDateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val priceFormatter = DecimalFormat("#.##")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InviteDetailViewHolder {
        val binding = ItemInviteDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return InviteDetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InviteDetailViewHolder, position: Int) {
        holder.bind(inviteDetails[position])
    }

    override fun getItemCount(): Int = inviteDetails.size

    fun updateData(newDetails: List<InviteDetail>) {
        val diffResult = DiffUtil.calculateDiff(InviteDetailDiffCallback(inviteDetails, newDetails))
        inviteDetails.clear()
        inviteDetails.addAll(newDetails)
        diffResult.dispatchUpdatesTo(this)
    }

    inner class InviteDetailViewHolder(private val binding: ItemInviteDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(detail: InviteDetail) {
            binding.tvInviteUser.text = "邀请用户 ID: ${detail.inviteUserId}"
            binding.tvInviteReward.text = "¥${formatPrice(detail.getAmount)}"
            binding.tvInviteDate.text = formatServerDate(detail.createdAt)
            binding.tvInviteStatus.text = "订单 #${detail.orderId}"
        }
        
        private fun formatPrice(amount: Double): String {
            // Round to avoid floating point precision issues
            val rounded = Math.round(amount * 100.0) / 100.0
            return priceFormatter.format(rounded)
        }

        private fun formatServerDate(value: String): String {
            return try {
                val date = serverDateParser.parse(value)
                date?.let { localDateFormatter.format(it) } ?: value
            } catch (e: ParseException) {
                value
            }
        }
    }

    private class InviteDetailDiffCallback(
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
