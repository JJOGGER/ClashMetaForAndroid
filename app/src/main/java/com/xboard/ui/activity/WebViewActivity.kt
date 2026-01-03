package com.xboard.ui.activity

import android.webkit.WebView
import android.webkit.WebViewClient
import com.xboard.databinding.ActivityWebViewBinding
import com.xboard.base.BaseActivity

/**
 * WebView 页面，用于显示服务协议、隐私协议等网页内容
 */
class WebViewActivity : BaseActivity<ActivityWebViewBinding>() {

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_URL = "url"
    }

    override fun getViewBinding(): ActivityWebViewBinding {
        return ActivityWebViewBinding.inflate(layoutInflater)
    }

    override fun initView() {
        // 获取传入的参数
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "网页"
        val url = intent.getStringExtra(EXTRA_URL) ?: ""

        // 设置标题
        binding.tvTitle.text = title

        // 返回按钮
        binding.topBar.setNavigationOnClickListener {
            finish()
        }

        // 配置 WebView
        setupWebView(url)
    }

    override fun initData() {
        // 无需加载数据
    }

    private fun setupWebView(url: String) {
        val webView = binding.webView
        
        // 配置 WebView 设置
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        // 设置 WebViewClient
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.progressBar.visibility = android.view.View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.progressBar.visibility = android.view.View.GONE
            }
        }

        // 加载 URL
        if (url.isNotEmpty()) {
            webView.loadUrl(url)
        }
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
