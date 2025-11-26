package com.xboard.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.databinding.ItemTrafficDetailBinding
import com.xboard.ui.activity.TrafficDetailItem
import java.text.DecimalFormat

/**
 * 流量明细适配器
 */
class TrafficDetailAdapter : RecyclerView.Adapter<TrafficDetailAdapter.ViewHolder>() {

    private val items = mutableListOf<TrafficDetailItem>()
    private val sizeFormatter = DecimalFormat("#,##0.00")

    fun updateData(newItems: List<TrafficDetailItem>) {
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

        fun bind(item: TrafficDetailItem) {
            binding.tvDate.text = item.date

            if (item.isHeader) {
                // 头部样式
                binding.tvUpload.text = "上传: ${formatSize(item.upload)}"
                binding.tvDownload.text = "下载: ${formatSize(item.download)}"
                binding.tvTotal.text = "总计: ${formatSize(item.upload + item.download)}"
                binding.root.setBackgroundColor(binding.root.context.getColor(android.R.color.darker_gray))
            } else {
                // 普通样式
                binding.tvUpload.text = formatSize(item.upload)
                binding.tvDownload.text = formatSize(item.download)
                binding.tvTotal.text = formatSize(item.upload + item.download)
                binding.root.setBackgroundColor(binding.root.context.getColor(android.R.color.transparent))
            }
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
