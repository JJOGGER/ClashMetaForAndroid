package com.xboard.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.kr328.clash.databinding.ItemNoticeBannerBinding
import com.xboard.model.Notice

/**
 * 公告轮播 Banner Adapter
 */
class NoticeBannerAdapter(
    private val onNoticeClick: (Notice) -> Unit,
    private val onIndicatorUpdate: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<NoticeBannerAdapter.NoticeBannerViewHolder>() {

    private val notices = mutableListOf<Notice>()

    fun setData(newNotices: List<Notice>) {
        notices.clear()
        notices.addAll(newNotices)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoticeBannerViewHolder {
        val binding = ItemNoticeBannerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoticeBannerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoticeBannerViewHolder, position: Int) {
        holder.bind(notices[position % notices.size])
        // 更新指示器
        onIndicatorUpdate?.invoke(position % notices.size)
    }

    override fun getItemCount(): Int {
        // 无限循环：返回一个很大的数字
        return if (notices.isEmpty()) 0 else Int.MAX_VALUE
    }

    inner class NoticeBannerViewHolder(private val binding: ItemNoticeBannerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notice: Notice) {
            binding.tvNoticeTitle.text = notice.title
            Glide.with(binding.root).load(notice.imgUrl).into(binding.bannerBackground)
            // 点击事件
            binding.root.setOnClickListener {
                onNoticeClick(notice)
            }

        }
    }
}
