package com.xboard.ui.activity

import android.os.Bundle
import com.xboard.base.BaseComposeActivity
import com.xboard.ui.compose.AgreementScreen

/**
 * 协议展示页面（WebView）
 * 已重构为 Compose 实现
 */
class AgreementActivity : BaseComposeActivity() {

    companion object {
        const val EXTRA_TYPE = "agreement_type"
        const val TYPE_USER_AGREEMENT = "user_agreement"
        const val TYPE_PRIVACY_POLICY = "privacy_policy"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val type = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_USER_AGREEMENT
        
        setThemeContent {
            AgreementScreen(
                agreementType = type,
                onNavigateBack = { finish() }
            )
        }
    }
}
