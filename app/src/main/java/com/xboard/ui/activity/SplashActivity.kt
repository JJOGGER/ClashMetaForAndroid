package com.xboard.ui.activity

import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.lifecycleScope
import com.github.kr328.clash.databinding.ActivitySplashBinding
import com.xboard.api.RetrofitClient
import com.xboard.api.TokenManager
import com.xboard.base.BaseActivity
import com.xboard.network.UserRepository
import com.xboard.storage.MMKVManager
import com.xboard.util.AutoSubscriptionManager
import kotlinx.coroutines.launch
import java.util.*

/**
 * 启动页
 */
class SplashActivity : BaseActivity<ActivitySplashBinding>() {

    private val userRepository by lazy { UserRepository(RetrofitClient.getApiService()) }
    
    private val autoSubscriptionManager by lazy { 
        AutoSubscriptionManager(this, userRepository, lifecycleScope) 
    }

    override fun getViewBinding(): ActivitySplashBinding {
        return ActivitySplashBinding.inflate(layoutInflater)
    }

    override fun getStatusBarColor(): Int {
        return android.graphics.Color.TRANSPARENT
    }

    override fun initView() {
        // 初始化Retrofit客户端
        val baseUrl = "http://xiuxiujd.cc/api/v1/" // TODO: 从配置读取
//        val baseUrl = "http://192.168.3.3:7001/api/v1/" // TODO: 从配置读取
        RetrofitClient.initialize(this, baseUrl)
        MMKVManager.saveApiBaseUrl(baseUrl)
    }

    override fun initData() {
        // 延迟500ms后跳转
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, 500)
    }

    private fun navigateToNextScreen() {
//        TokenManager.saveToken("mZRgEgJZT4DQ46pjrIZWySHc3S9hGpNxNKK09Cqwa9580646","40517142@qq.com","88888888")
        
        if (MMKVManager.isLoggedIn()) {
            // Token有效，先请求订阅URL
            fetchSubscribeUrl()
        } else {
            // Token无效，直接跳转到登录页
            navigateToLogin()
        }
    }

    /**
     * 启动时自动导入和应用订阅
     * 
     * 使用 AutoSubscriptionManager 完成整个自动化流程：
     * 1. 获取或创建配置文件 UUID
     * 2. 自动更新配置（导入）
     * 3. 自动选中 Profile（应用）
     * 
     * 无论成功还是失败，都继续跳转到首页
     */
    private fun fetchSubscribeUrl() {
        lifecycleScope.launch {
            try {
                // 自动导入和应用订阅
                autoSubscriptionManager.autoImportAndApply()
                
                // 无论成功还是失败，都继续跳转到首页
                navigateToMain()
            } catch (e: Exception) {
                // 异常处理，继续跳转到首页
                navigateToMain()
            }
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
