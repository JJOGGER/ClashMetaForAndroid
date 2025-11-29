package com.xboard.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.databinding.ItemTrafficDetailBinding
import com.xboard.model.TrafficLog
import com.xboard.utils.DateUtils
import java.text.DecimalFormat

/**
 * 流量明细适配器
 */
class TrafficDetailAdapter : RecyclerView.Adapter<TrafficDetailAdapter.ViewHolder>() {

    private val items = mutableListOf<TrafficLog>()
    private val sizeFormatter = DecimalFormat("#,##0.00")

    fun updateData(newItems: List<TrafficLog>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTrafficDetailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemTrafficDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TrafficLog) {
            binding.tvDate.text =  DateUtils.getStringTime(
                (item.record_at?.toLongOrNull() ?: 0L) * 1000,
                "yyyy-MM-dd"
            )
            // 普通样式
            binding.tvUpload.text = formatSize(item.u ?: 0)
            binding.tvDownload.text = formatSize(item.d ?: 0)
            binding.tvTotal.text = formatSize((item.u ?: 0) + (item.d ?: 0))
        }

        private fun formatSize(bytes: Long): String {
            return when {
                bytes >= 1024 * 1024 * 1024 -> "${sizeFormatter.format(bytes / (1024.0 * 1024 * 1024))} GB"
                bytes >= 1024 * 1024 -> "${sizeFormatter.format(bytes / (1024.0 * 1024))} MB"
                bytes >= 1024 -> "${sizeFormatter.format(bytes / 1024.0)} KB"
                else -> "$bytes B"
            }
        }
    }
}
