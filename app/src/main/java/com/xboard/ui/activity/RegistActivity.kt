package com.xboard.ui.activity

import android.content.Intent
import android.os.Bundle
import com.xboard.R
import com.xboard.base.BaseComposeActivity
import com.xboard.ui.compose.RegistScreen
import com.xboard.ui.viewmodel.RegistViewModel

/**
 * 注册页面
 */
class RegistActivity : BaseComposeActivity() {
    
    private val viewModel by lazy { RegistViewModel(application) }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val appName = getString(R.string.app_name)
        
        setThemeContent {
            RegistScreen(
                viewModel = viewModel,
                appName = appName,
                onNavigateBack = { finish() },
                onNavigateToMain = {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                },
                onNavigateToLogin = {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                },
                onNavigateToAgreement = { type ->
                    startActivity(Intent(this, AgreementActivity::class.java).apply {
                        putExtra(AgreementActivity.EXTRA_TYPE, type)
                    })
                }
            )
        }
    }
}
