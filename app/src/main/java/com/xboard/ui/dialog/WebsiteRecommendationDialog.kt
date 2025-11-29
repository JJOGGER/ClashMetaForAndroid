package com.xboard.ui.dialog

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import com.xboard.model.KnowledgeArticle
import com.xboard.ui.activity.WebViewActivity
import com.xboard.ui.adapter.WebsiteRecommendationAdapter
import com.xboard.ex.showToast
import com.github.kr328.clash.R

/**
 * 网站推荐对话框 - 中间弹窗样式
 */
class WebsiteRecommendationDialog : DialogFragment() {

    private var articles: List<KnowledgeArticle> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_website_recommendation_new, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置列表
        val rvWebsites = view.findViewById<RecyclerView>(R.id.rv_websites)
        val btnClose = view.findViewById<TextView>(R.id.btn_close)

        val adapter = WebsiteRecommendationAdapter { article ->
            //跳转外部浏览器

            if (!article.getWebsite()?.url.isNullOrEmpty()) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(article.getWebsite()?.url))
                    startActivity(intent)
                    dismiss()
                } catch (e: Exception) {
                    showToast("无法打开浏览器")
                }
            } else {
                showToast("网址不可用")
            }
        }

        rvWebsites.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }
        adapter.setData(articles)

        btnClose.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        // 设置对话框宽度和样式
        dialog?.window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    companion object {
        fun newInstance(articles: List<KnowledgeArticle>): WebsiteRecommendationDialog {
            return WebsiteRecommendationDialog().apply {
                this.articles = articles
            }
        }
    }
}
