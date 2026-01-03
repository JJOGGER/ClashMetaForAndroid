package com.xboard.ui.activity

import android.content.Intent
import android.os.Bundle
import com.xboard.base.BaseComposeActivity
import com.xboard.ui.compose.LoginScreen
import com.xboard.ui.viewmodel.LoginViewModel

/**
 * 登录页面
 */
class LoginActivity : BaseComposeActivity() {
    
    private val viewModel by lazy { LoginViewModel(application) }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setThemeContent {
            LoginScreen(
                viewModel = viewModel,
                onNavigateToMain = {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                },
                onNavigateToRegister = {
                    startActivity(Intent(this, RegistActivity::class.java))
                },
                onNavigateToForgotPassword = {
                    startActivity(Intent(this, ForgotPasswordActivity::class.java))
                }
            )
        }
    }
}
