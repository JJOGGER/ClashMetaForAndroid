package com.xboard.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.xboard.api.RetrofitClient
import com.xboard.base.BaseComposeActivity
import com.xboard.network.ApiResult
import com.xboard.network.UserRepository
import com.xboard.storage.MMKVManager
import com.xboard.ui.compose.SplashScreen
import com.xboard.util.DomainFallbackManager
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.net.SocketTimeoutException

/**
 * 启动页
 * 已迁移到 Compose，使用 Material 3 设计和动画效果
 */
class SplashActivity : BaseComposeActivity() {

    private val TAG = "SplashActivity"
    private val userRepository by lazy { UserRepository(RetrofitClient.getApiService()) }
    
    // 动画和接口请求完成状态
    private var isAnimationComplete = false
    private var isDataReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置 Compose 内容
        setThemeContent {
            SplashScreen(
                onAnimationComplete = {
                    isAnimationComplete = true
                    checkAndNavigate()
                }
            )
        }
        
        // 初始化数据
        initData()
    }
    
    /**
     * 检查动画和接口请求是否都完成，然后导航
     */
    private fun checkAndNavigate() {
        if (isAnimationComplete && isDataReady) {
            navigateToNextScreen()
        }
    }

    private fun initData() {
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
                    // 直接请求，设置超时时间（3秒），如果超时或失败再走备用流程
                    try {
                        val testResult = withTimeout(3_000) { // 3秒超时
                            newUserRepository.getCommonConfig()
                        }
                        testResult.onSuccess {
                            Log.d(TAG, "缓存的接口域名可用，请求成功")
                            isDataReady = true
                            checkAndNavigate()
                        }.onError { error ->
                            // 检查是否是超时异常
                            val isTimeout = error.exception is SocketTimeoutException || 
                                          error.message?.contains("timeout", ignoreCase = true) == true
                            if (isTimeout) {
                                Log.w(TAG, "请求超时: ${error.message}，切换到备用域名")
                                tryFallbackFromCache()
                            } else {
                                Log.w(TAG, "缓存的接口域名不可用: ${error.message}，尝试从备用列表遍历")
                                // 从备用列表遍历（会自动排除已失败的域名）
                                tryFallbackFromCache()
                            }
                        }
                    } catch (e: TimeoutCancellationException) {
                        Log.w(TAG, "请求超时（协程级别），切换到备用域名")
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
                
                // 请求公共配置，设置超时时间（3秒）
                try {
                    val configResult = withTimeout(3_000) { // 3秒超时
                        newUserRepository.getCommonConfig()
                    }
                    configResult.onSuccess {
                        Log.d(TAG, "公共配置请求成功")
                        isDataReady = true
                        checkAndNavigate()
                    }.onError { error ->
                        // 检查是否是超时异常
                        val isTimeout = error.exception is SocketTimeoutException || 
                                      error.message?.contains("timeout", ignoreCase = true) == true
                        if (isTimeout) {
                            Log.w(TAG, "公共配置请求超时: ${error.message}，尝试从备用列表遍历")
                            // 超时后尝试从备用列表遍历
                            tryFallbackFromCache()
                        } else {
                            Log.e(TAG, "公共配置请求失败: ${error.message}")
                            // 即使失败也跳转，避免阻塞用户
                            isDataReady = true
                            checkAndNavigate()
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.w(TAG, "公共配置请求超时（协程级别），尝试从备用列表遍历")
                    tryFallbackFromCache()
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
                isDataReady = true
                checkAndNavigate()
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
                // 设置超时时间（3秒）
                try {
                    val result = withTimeout(3_000) { // 3秒超时
                        repository.getCommonConfig()
                    }
                    result.onSuccess {
                        Log.d(TAG, "使用缓存的备用域名请求成功")
                        isDataReady = true
                        checkAndNavigate()
                    }.onError {
                        Log.e(TAG, "使用缓存的备用域名请求失败，直接跳转")
                        isDataReady = true
                        checkAndNavigate()
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.e(TAG, "使用缓存的备用域名请求超时，直接跳转")
                    isDataReady = true
                    checkAndNavigate()
                }
            } else {
                // 完全没有缓存，直接跳转
                Log.e(TAG, "没有缓存的域名配置，直接跳转")
                isDataReady = true
                checkAndNavigate()
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
