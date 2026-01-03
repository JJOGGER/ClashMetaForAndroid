package com.xboard.ui.activity

import android.os.Bundle
import com.xboard.base.BaseComposeActivity
import com.xboard.ui.compose.ChangePasswordScreen
import com.xboard.ui.viewmodel.ChangePasswordViewModel

/**
 * 修改密码页面
 */
class ChangePasswordActivity : BaseComposeActivity() {
    
    private val viewModel by lazy { ChangePasswordViewModel(application) }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setThemeContent {
            ChangePasswordScreen(
                viewModel = viewModel,
                onNavigateBack = { finish() },
                onSuccess = { finish() }
            )
        }
    }
}
