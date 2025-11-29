package com.xboard.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.databinding.ItemTicketBinding
import com.xboard.model.TicketResponse
import java.text.SimpleDateFormat
import java.util.*

class TicketAdapter(
    private val onTicketClick: (TicketResponse) -> Unit
) : ListAdapter<TicketResponse, TicketAdapter.ViewHolder>(TicketDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemTicketBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemTicketBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(ticket: TicketResponse) {
            binding.apply {
                tvSubject.text = ticket.subject
                tvDescription.text = ticket.description
                tvTime.text = formatTime(ticket.createdAt?:0L)
                
                // 显示工单状态
                tvStatus.text = when (ticket.status) {
                    0 -> "处理中"
                    1 -> "已关闭"
                    else -> "未知"
                }
                
                tvStatus.setTextColor(
                    when (ticket.status) {
//                        0 -> android.graphics.Color.parseColor("#FF9800") // 橙色
                        0 -> android.graphics.Color.parseColor("#2196F3") // 蓝色
//                        2 -> android.graphics.Color.parseColor("#4CAF50") // 绿色
                        else -> android.graphics.Color.parseColor("#999999") // 灰色
                    }
                )
                
                root.setOnClickListener {
                    onTicketClick(ticket)
                }
            }
        }

        private fun formatTime(timestamp: Long): String {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                sdf.format(Date(timestamp * 1000))
            } catch (e: Exception) {
                ""
            }
        }
    }

    class TicketDiffCallback : DiffUtil.ItemCallback<TicketResponse>() {
        override fun areItemsTheSame(oldItem: TicketResponse, newItem: TicketResponse) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: TicketResponse, newItem: TicketResponse) =
            oldItem == newItem
    }
}
