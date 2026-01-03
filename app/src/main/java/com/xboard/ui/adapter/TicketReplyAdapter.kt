package com.xboard.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xboard.databinding.ItemTicketReplyBinding
import java.text.SimpleDateFormat
import java.util.*

data class TicketReply(
    val id: Int,
    val message: String,
    val createdAt: Long,
    val isAdmin: Boolean = false
)

class TicketReplyAdapter : ListAdapter<TicketReply, TicketReplyAdapter.ViewHolder>(TicketReplyDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemTicketReplyBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemTicketReplyBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(reply: TicketReply) {
            binding.apply {
                tvTime.text = formatTime(reply.createdAt)
                
                if (reply.isAdmin) {
                    // 客服消息 - 左侧
                    llAdminMessage.visibility = android.view.View.VISIBLE
                    llUserMessage.visibility = android.view.View.GONE
                    tvMessageAdmin.text = reply.message
                } else {
                    // 用户消息 - 右侧
                    llAdminMessage.visibility = android.view.View.GONE
                    llUserMessage.visibility = android.view.View.VISIBLE
                    tvMessageUser.text = reply.message
                }
            }
        }

        private fun formatTime(timestamp: Long): String {
            return try {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                sdf.format(Date(timestamp * 1000))
            } catch (e: Exception) {
                ""
            }
        }
    }

    class TicketReplyDiffCallback : DiffUtil.ItemCallback<TicketReply>() {
        override fun areItemsTheSame(oldItem: TicketReply, newItem: TicketReply) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: TicketReply, newItem: TicketReply) =
            oldItem == newItem
    }
}
