package com.xboard.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.kr328.clash.databinding.ItemWebsiteRecommendationBinding
import com.xboard.model.KnowledgeArticle

/**
 * 网站推荐列表 Adapter
 */
class WebsiteRecommendationAdapter(
    private val onItemClick: (KnowledgeArticle) -> Unit
) : RecyclerView.Adapter<WebsiteRecommendationAdapter.WebsiteRecommendationViewHolder>() {

    private val items = mutableListOf<KnowledgeArticle>()

    fun setData(newItems: List<KnowledgeArticle>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WebsiteRecommendationViewHolder {
        val binding = ItemWebsiteRecommendationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WebsiteRecommendationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WebsiteRecommendationViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class WebsiteRecommendationViewHolder(private val binding: ItemWebsiteRecommendationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(article: KnowledgeArticle) {
            binding.tvWebsiteName.text = article.title
            Glide.with(binding.root)
                .load(article.getWebsite()?.img?:"")
                .into(binding.ivWebsiteIcon)
            // 点击事件
            binding.root.setOnClickListener {
                onItemClick(article)
            }
        }
    }
}
