package com.xboard.ui.activity

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.github.kr328.clash.databinding.ActivityAgreementBinding

/**
 * 协议展示页面（WebView）
 */
class AgreementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAgreementBinding

    companion object {
        const val EXTRA_TYPE = "agreement_type"
        const val TYPE_USER_AGREEMENT = "user_agreement"
        const val TYPE_PRIVACY_POLICY = "privacy_policy"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgreementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val type = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_USER_AGREEMENT
        
        setupWebView()
        loadAgreement(type)
        setupToolbar(type)
    }

    private fun setupWebView() {
        binding.webView.apply {
            webViewClient = WebViewClient()
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                textZoom = 100
            }
        }
    }

    private fun loadAgreement(type: String) {
        val resourceId = when (type) {
            TYPE_PRIVACY_POLICY -> resources.getIdentifier("privacy_policy", "raw", packageName)
            else -> resources.getIdentifier("user_agreement", "raw", packageName)
        }
        
        if (resourceId != 0) {
            val htmlContent = resources.openRawResource(resourceId).bufferedReader().use { it.readText() }
            binding.webView.loadDataWithBaseURL(
                null,
                htmlContent,
                "text/html",
                "utf-8",
                null
            )
        }
    }

    private fun setupToolbar(type: String) {
        val title = when (type) {
            TYPE_PRIVACY_POLICY -> "隐私政策"
            else -> "用户协议"
        }
        
        binding.tvTitle.text = title
        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}
