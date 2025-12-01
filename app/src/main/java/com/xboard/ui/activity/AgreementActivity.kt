package com.xboard.ui.activity

import android.os.Bundle
import android.webkit.WebViewClient
import com.github.kr328.clash.databinding.ActivityAgreementBinding
import com.github.kr328.clash.databinding.ActivityCommissionRecordBinding
import com.xboard.api.RetrofitClient
import com.xboard.base.BaseActivity

/**
 * 协议展示页面（WebView）
 */
class AgreementActivity : BaseActivity<ActivityAgreementBinding>() {


    companion object {
        const val EXTRA_TYPE = "agreement_type"
        const val TYPE_USER_AGREEMENT = "user_agreement"
        const val TYPE_PRIVACY_POLICY = "privacy_policy"
    }

    override fun getViewBinding(): ActivityAgreementBinding {
        return ActivityAgreementBinding.inflate(layoutInflater)

    }

    override fun initView() {
        super.initView()
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
        when (type) {
            TYPE_PRIVACY_POLICY -> binding.webView.loadUrl(
                "${RetrofitClient.BASE_URL}/user_privacy.html",
            )

            else -> binding.webView.loadUrl(
                "${RetrofitClient.BASE_URL}/user_agreement.html",
            )
        }


    }

    private fun setupToolbar(type: String) {
        val title = when (type) {
            TYPE_PRIVACY_POLICY -> "隐私政策"
            else -> "用户协议"
        }

        binding.tvTitle.text = title
        binding.vBack.setOnClickListener {
            finish()
        }
    }
}
