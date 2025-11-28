package com.xboard.ui.activity

import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.github.kr328.clash.databinding.ActivitySplashBinding
import com.xboard.api.RetrofitClient
import com.xboard.base.BaseActivity
import com.xboard.network.UserRepository
import com.xboard.storage.MMKVManager
import kotlinx.coroutines.launch

/**
 * 启动页
 */
class SplashActivity : BaseActivity<ActivitySplashBinding>() {

    private val userRepository by lazy { UserRepository(RetrofitClient.getApiService()) }


    override fun getViewBinding(): ActivitySplashBinding {
        return ActivitySplashBinding.inflate(layoutInflater)
    }

    override fun getStatusBarColor(): Int {
        return android.graphics.Color.TRANSPARENT
    }

    override fun initView() {
        // 初始化Retrofit客户端
        val baseUrl = RetrofitClient.BASE_URL + "/api/v1/"
        RetrofitClient.initialize(baseUrl)
        MMKVManager.saveApiBaseUrl(baseUrl)
    }

    override fun initData() {
        lifecycleScope.launch {
            userRepository.getCommonConfig().onSuccess {
                navigateToNextScreen()
            }.onError {
                navigateToNextScreen()
            }
        }
    }

    private fun navigateToNextScreen() {
//        TokenManager.saveToken("mZRgEgJZT4DQ46pjrIZWySHc3S9hGpNxNKK09Cqwa9580646","40517142@qq.com","88888888")

        if (MMKVManager.isLoggedIn()) {
            // Token有效，先请求订阅URL
            navigateToMain()
        } else {
            // Token无效，直接跳转到登录页
            navigateToLogin()
        }
    }


    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
