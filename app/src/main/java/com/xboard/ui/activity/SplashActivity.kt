package com.xboard.ui.activity

import android.content.Intent
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.github.kr328.clash.databinding.ActivitySplashBinding
import com.xboard.api.RetrofitClient
import com.xboard.base.BaseActivity
import com.xboard.network.ApiResult
import com.xboard.network.UserRepository
import com.xboard.storage.MMKVManager
import com.xboard.util.DomainFallbackManager
import kotlinx.coroutines.launch

/**
 * 启动页
 */
class SplashActivity : BaseActivity<ActivitySplashBinding>() {

    private val TAG = "SplashActivity"
    private val userRepository by lazy { UserRepository(RetrofitClient.getApiService()) }


    override fun getViewBinding(): ActivitySplashBinding {
        return ActivitySplashBinding.inflate(layoutInflater)
    }

    override fun getStatusBarColor(): Int {
        return android.graphics.Color.TRANSPARENT
    }

    override fun initView() {
    }

    override fun initData() {
        lifecycleScope.launch {
            // 检查是否有主域名和接口域名
            val hasMainDomain = !DomainFallbackManager.getCachedMainDomain().isNullOrEmpty()
            val cachedApiDomain = DomainFallbackManager.getCachedApiDomain()
            val hasApiDomain = !cachedApiDomain.isNullOrEmpty()

            if (!hasMainDomain || !hasApiDomain) {
                // 没有主域名或接口域名，需要请求 api.json
                Log.d(TAG, "没有主域名或接口域名，开始初始化域名配置")
                initializeAndRequest()
            } else {
                // 有缓存的域名，检查是否需要更新
                if (DomainFallbackManager.shouldUpdateDomainConfig()) {
                    Log.d(TAG, "超过更新间隔，需要重新请求 api.json")
                    initializeAndRequest()
                } else {
                    // 使用缓存的域名
                    Log.d(TAG, "使用缓存的域名配置")
                    // 如果域名不可用，会在实际请求时失败，然后走备用流程
                    // cachedApiDomain 已经通过 hasApiDomain 检查，不为空
                    DomainFallbackManager.switchToApiDomain(cachedApiDomain!!)
                    // 重新创建 Repository
                    val newUserRepository = UserRepository(RetrofitClient.getApiService())
                    // 直接请求，如果失败再走备用流程
                    val testResult = newUserRepository.getCommonConfig()
                    testResult.onSuccess {
                        Log.d(TAG, "缓存的接口域名可用，请求成功")
                        navigateToNextScreen()
                    }.onError { error ->
                        Log.w(TAG, "缓存的接口域名不可用: ${error.message}，尝试从备用列表遍历")
                        // 从备用列表遍历（会自动排除已失败的域名）
                        tryFallbackFromCache()
                    }
                }
            }
        }
    }

    /**
     * 初始化域名配置并请求公共配置
     */
    private fun initializeAndRequest() {
        lifecycleScope.launch {
            Log.d(TAG, "开始初始化域名配置")
            val initResult = DomainFallbackManager.initializeDomainConfig()

            if (initResult is ApiResult.Success) {
                val availableDomain = initResult.data
                Log.i(TAG, "域名配置初始化成功，使用接口域名: $availableDomain")
                
                // 切换到可用的接口域名
                DomainFallbackManager.switchToApiDomain(availableDomain)
                
                // 重新创建 Repository
                val newUserRepository = UserRepository(RetrofitClient.getApiService())
                
                // 请求公共配置
                val configResult = newUserRepository.getCommonConfig()
                configResult.onSuccess {
                    Log.d(TAG, "公共配置请求成功")
                    navigateToNextScreen()
                }.onError { error ->
                    Log.e(TAG, "公共配置请求失败: ${error.message}")
                    // 即使失败也跳转，避免阻塞用户
                    navigateToNextScreen()
                }
            } else {
                Log.e(TAG, "域名配置初始化失败: ${(initResult as ApiResult.Error).message}")
                // 初始化失败，尝试使用默认域名
                tryDefaultDomain()
            }
        }
    }

    /**
     * 从缓存的备用列表遍历
     */
    private fun tryFallbackFromCache() {
        lifecycleScope.launch {
            Log.d(TAG, "从缓存的备用列表遍历可用域名")
            val availableDomain = DomainFallbackManager.getAvailableApiDomain()

            if (availableDomain != null) {
                Log.i(TAG, "从备用列表找到可用域名: $availableDomain")
                DomainFallbackManager.switchToApiDomain(availableDomain)
                navigateToNextScreen()
            } else {
                // 缓存的备用列表都不可用，需要重新请求 api.json
                Log.d(TAG, "缓存的备用列表都不可用，重新请求 api.json")
                initializeAndRequest()
            }
        }
    }

    /**
     * 尝试使用缓存的域名（降级方案）
     */
    private fun tryDefaultDomain() {
        lifecycleScope.launch {
            Log.d(TAG, "域名配置初始化失败，尝试使用缓存的域名")

            // 尝试从缓存的配置中获取第一个备用域名
            val cachedConfig = DomainFallbackManager.getCachedDomainConfig()
            if (cachedConfig != null && cachedConfig.domains.isNotEmpty()) {
                val firstDomain = cachedConfig.domains[0]
                val baseUrl = "$firstDomain/api/v1/"
                Log.d(TAG, "使用缓存的第一个备用域名: $baseUrl")
                RetrofitClient.initialize(baseUrl)
                MMKVManager.saveApiBaseUrl(baseUrl)

                val repository = UserRepository(RetrofitClient.getApiService())
                val result = repository.getCommonConfig()
                result.onSuccess {
                    Log.d(TAG, "使用缓存的备用域名请求成功")
                    navigateToNextScreen()
                }.onError {
                    Log.e(TAG, "使用缓存的备用域名请求失败，直接跳转")
                    navigateToNextScreen()
                }
            } else {
                // 完全没有缓存，直接跳转
                Log.e(TAG, "没有缓存的域名配置，直接跳转")
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
