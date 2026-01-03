package com.xboard.ui.activity

import android.os.Bundle
import com.xboard.base.BaseComposeActivity
import com.xboard.ui.compose.ForgotPasswordScreen
import com.xboard.ui.viewmodel.ForgotPasswordViewModel

/**
 * 忘记密码页面
 */
class ForgotPasswordActivity : BaseComposeActivity() {
    
    private val viewModel by lazy { ForgotPasswordViewModel(application) }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setThemeContent {
            ForgotPasswordScreen(
                viewModel = viewModel,
                onNavigateBack = { finish() },
                onSuccess = { finish() }
            )
        }
    }
}
