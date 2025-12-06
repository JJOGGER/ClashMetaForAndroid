package com.xboard.util

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.xboard.api.RetrofitClient
import com.xboard.network.ApiResult
import com.xboard.network.UserRepository
import com.xboard.storage.MMKVManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 备用域名管理器
 * 用于在主要域名不可用时，从备用域名列表获取可用的域名
 */
object DomainFallbackManager {
    private const val TAG = "DomainFallbackManager"
    private const val FALLBACK_API_URL = "https://xiuxiuyunapp.cc/api/api.json"
    private const val KEY_MAIN_DOMAIN = "cached_main_domain"
    private const val KEY_API_DOMAIN = "cached_api_domain"
    private const val KEY_DOMAIN_RESPONSE = "cached_domain_response"
    private const val KEY_LAST_UPDATE_TIME = "last_domain_update_time"
    private const val TEST_TIMEOUT_SECONDS = 5L

    /**
     * 备用域名响应模型
     */
    data class DomainResponse(
        @SerializedName("main_domain")
        val mainDomain: String?,
        @SerializedName("domain")
        val domains: List<String>,
        @SerializedName("update")
        val update: Int = 24 // 更新间隔，单位：小时，默认24小时
    )

    /**
     * 获取域名配置（从 api.json）
     */
    suspend fun fetchDomainConfig(): ApiResult<DomainResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始从备用API获取域名配置: $FALLBACK_API_URL")

            val client = OkHttpClient.Builder()
                .connectTimeout(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(FALLBACK_API_URL)
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "获取域名配置失败，HTTP状态码: ${response.code}")
                return@withContext ApiResult.Error(
                    code = response.code,
                    message = "HTTP ${response.code}"
                )
            }

            val responseBody = response.body?.string()
            if (responseBody.isNullOrEmpty()) {
                Log.e(TAG, "获取域名配置失败，响应体为空")
                return@withContext ApiResult.Error(
                    code = -1,
                    message = "响应体为空"
                )
            }

            Log.d(TAG, "获取域名配置响应: $responseBody")

            val domainResponse = Gson().fromJson(responseBody, DomainResponse::class.java)

            if (domainResponse.domains.isNullOrEmpty()) {
                Log.e(TAG, "接口域名列表为空")
                return@withContext ApiResult.Error(
                    code = -1,
                    message = "接口域名列表为空"
                )
            }

            Log.d(
                TAG,
                "成功获取域名配置 - 主域名: ${domainResponse.mainDomain}, 接口域名列表: ${domainResponse.domains}, 更新间隔: ${domainResponse.update}小时"
            )

            // 缓存域名配置和更新时间
            cacheDomainConfig(domainResponse)

            ApiResult.Success(domainResponse)
        } catch (e: IOException) {
            Log.e(TAG, "获取域名配置网络异常", e)
            ApiResult.Error(
                code = -1,
                message = "网络异常: ${e.message}",
                exception = e
            )
        } catch (e: Exception) {
            Log.e(TAG, "获取域名配置异常", e)
            ApiResult.Error(
                code = -1,
                message = "解析异常: ${e.message}",
                exception = e
            )
        }
    }

    /**
     * 测试域名是否可用
     * 通过调用 guest/comm/config 接口来测试
     * @param domain 要测试的域名
     * @param skipRestore 是否跳过恢复 baseUrl（用于连续测试多个域名时优化性能）
     */
    suspend fun testDomain(domain: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val baseUrl = "$domain/api/v1/"
                // 创建临时 Retrofit 客户端用于测试
                RetrofitClient.initialize(baseUrl)

                val testApiService = RetrofitClient.getApiService()
                val testRepository = UserRepository(testApiService)

                // 尝试调用一个简单的接口
                val result = testRepository.getCommonConfig()

                val isAvailable = result.isSuccess()

                if (isAvailable) {
                    Log.d(TAG, "域名可用: $domain")
                } else {
                    Log.w(
                        TAG,
                        "域名不可用: $domain, 错误: ${(result as? ApiResult.Error)?.message}"
                    )
                }

                isAvailable
            } catch (e: Exception) {
                Log.e(TAG, "测试域名异常: $domain", e)
                false
            }
        }

    /**
     * 遍历备用域名列表，找到第一个可用的域名
     * @param domains 域名列表
     * @param excludeDomains 要排除的域名列表（已测试过不可用的域名）
     */
    suspend fun findAvailableDomain(
        domains: List<String>
    ): String? {

        Log.d(TAG, "开始遍历备用域名列表，共 ${domains.size} 个域名")

        // 优化：连续测试时，最后一个域名测试后不需要恢复 baseUrl
        for ((index, domain) in domains.withIndex()) {
            Log.d(TAG, "测试备用域名 [${index + 1}/${domains.size}]: $domain")

            // 最后一个域名测试时跳过恢复，优化性能
            if (testDomain(domain)) {
                Log.i(TAG, "找到可用域名: $domain")
                return domain
            }

            Log.w(TAG, "域名不可用，继续测试下一个: $domain")
        }

        Log.e(TAG, "所有备用域名都不可用")
        return null
    }

    /**
     * 检查是否需要更新域名配置
     * @return true 如果需要更新（超过 update 小时或没有缓存），false 如果可以使用缓存
     */
    fun shouldUpdateDomainConfig(): Boolean {
        val lastUpdateTime = getLastUpdateTime()
        if (lastUpdateTime == 0L) {
            Log.d(TAG, "没有缓存更新时间，需要更新")
            return true
        }

        val cachedConfig = getCachedDomainConfig()
        if (cachedConfig == null) {
            Log.d(TAG, "没有缓存的域名配置，需要更新")
            return true
        }

        val updateIntervalHours = cachedConfig.update
        val currentTime = System.currentTimeMillis()
        val elapsedHours = (currentTime - lastUpdateTime) / (1000 * 60 * 60)

        val shouldUpdate = elapsedHours >= updateIntervalHours
        Log.d(
            TAG,
            "距离上次更新已过去 ${elapsedHours} 小时，更新间隔 ${updateIntervalHours} 小时，需要更新: $shouldUpdate"
        )
        return shouldUpdate
    }

    /**
     * 获取可用的接口域名
     * 优先使用缓存的域名，如果不可用则从备用列表遍历，全不可用则重新请求 api.json
     */
    suspend fun getAvailableApiDomain(): String? {
        Log.d(TAG, "开始获取可用接口域名")

        // 从缓存的域名配置中获取备用列表
        val cachedConfig = getCachedDomainConfig()
        if (cachedConfig != null && cachedConfig.domains.isNotEmpty()) {
            Log.d(TAG, "从缓存的域名配置中遍历备用列表: ${cachedConfig.domains}")
            // 排除已测试失败的域名，避免重复测试
            val availableDomain = findAvailableDomain(cachedConfig.domains)
            if (availableDomain != null) {
                return availableDomain
            }
            Log.w(TAG, "缓存的备用域名列表全部不可用")
        }

        // 如果缓存的域名都不可用，需要重新请求 api.json
        Log.d(TAG, "缓存的域名都不可用，需要重新请求 api.json")
        return null
    }

    /**
     * 初始化域名配置（完整的流程）
     * 1. 检查是否需要更新
     * 2. 如果需要更新或没有缓存，请求 api.json
     * 3. 测试并找到可用的接口域名（排除已测试失败的域名）
     * 4. 缓存所有数据
     */
    suspend fun initializeDomainConfig(): ApiResult<String> = withContext(Dispatchers.IO) {
        try {

            var domainResponse: DomainResponse? = null
            Log.d(TAG, "需要更新域名配置，请求 api.json")
            val fetchResult = fetchDomainConfig()
            if (fetchResult is ApiResult.Error) {
                Log.e(TAG, "请求 api.json 失败: ${fetchResult.message}")
                // 如果请求失败，尝试使用缓存的配置
                domainResponse = getCachedDomainConfig()
                if (domainResponse == null) {
                    return@withContext ApiResult.Error(
                        code = -1,
                        message = "无法获取域名配置且没有缓存"
                    )
                }
                Log.d(TAG, "使用缓存的域名配置")
            } else {
                domainResponse = (fetchResult as ApiResult.Success).data
            }

            // 确保有域名配置
            if (domainResponse.domains.isEmpty()) {
                return@withContext ApiResult.Error(
                    code = -1,
                    message = "域名配置无效"
                )
            }

            // 测试并找到可用的接口域名（排除已测试失败的域名）
            val availableDomain = findAvailableDomain(domainResponse.domains)
            if (availableDomain == null) {
                return@withContext ApiResult.Error(
                    code = -1,
                    message = "所有接口域名都不可用"
                )
            }

            // 缓存主域名和接口域名
            if (!domainResponse.mainDomain.isNullOrEmpty()) {
                cacheMainDomain(domainResponse.mainDomain)
            }
            cacheApiDomain(availableDomain)

            Log.i(
                TAG,
                "域名配置初始化成功 - 主域名: ${domainResponse.mainDomain}, 接口域名: $availableDomain"
            )
            ApiResult.Success(availableDomain)
        } catch (e: Exception) {
            Log.e(TAG, "初始化域名配置异常", e)
            ApiResult.Error(
                code = -1,
                message = "初始化异常: ${e.message}",
                exception = e
            )
        }
    }

    /**
     * 切换 Retrofit 客户端到指定接口域名
     */
    fun switchToApiDomain(domain: String) {
        val baseUrl = "$domain/api/v1/"
        Log.i(TAG, "切换API域名到: $baseUrl")
        RetrofitClient.initialize(baseUrl)
        MMKVManager.saveApiBaseUrl(baseUrl)
        cacheApiDomain(domain)
    }

    /**
     * 缓存主域名
     */
    private fun cacheMainDomain(domain: String) {
        Log.d(TAG, "缓存主域名: $domain")
        MMKVManager.saveString(KEY_MAIN_DOMAIN, domain)
    }

    /**
     * 获取缓存的主域名
     */
    fun getCachedMainDomain(): String? {
        val domain = MMKVManager.getString(KEY_MAIN_DOMAIN, "")
        return if (domain.isNotEmpty()) domain else null
    }

    /**
     * 缓存接口域名
     */
     fun cacheApiDomain(domain: String) {
        Log.d(TAG, "缓存接口域名: $domain")
        MMKVManager.saveString(KEY_API_DOMAIN, domain)
    }

    /**
     * 获取缓存的接口域名
     */
    fun getCachedApiDomain(): String? {
        val domain = MMKVManager.getString(KEY_API_DOMAIN, "")
        return if (domain.isNotEmpty()) domain else null
    }

    /**
     * 缓存域名配置和更新时间
     */
    private fun cacheDomainConfig(domainResponse: DomainResponse) {
        Log.d(TAG, "缓存域名配置: $domainResponse")
        val configJson = Gson().toJson(domainResponse)
        MMKVManager.saveString(KEY_DOMAIN_RESPONSE, configJson)
        // 记录更新时间
        saveLastUpdateTime(System.currentTimeMillis())
    }

    /**
     * 获取缓存的域名配置
     */
    fun getCachedDomainConfig(): DomainResponse? {
        val configJson = MMKVManager.getString(KEY_DOMAIN_RESPONSE, "")
        if (configJson.isEmpty()) {
            return null
        }
        return try {
            Gson().fromJson(configJson, DomainResponse::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "解析缓存的域名配置失败", e)
            null
        }
    }

    /**
     * 保存最后更新时间
     */
    private fun saveLastUpdateTime(timeMillis: Long) {
        Log.d(TAG, "保存最后更新时间: $timeMillis")
        MMKVManager.saveLong(KEY_LAST_UPDATE_TIME, timeMillis)
    }

    /**
     * 获取最后更新时间
     */
    private fun getLastUpdateTime(): Long {
        return MMKVManager.getLong(KEY_LAST_UPDATE_TIME, 0L)
    }

    /**
     * 清除所有缓存
     */
    fun clearCache() {
        Log.d(TAG, "清除所有域名缓存")
        MMKVManager.remove(KEY_MAIN_DOMAIN)
        MMKVManager.remove(KEY_API_DOMAIN)
        MMKVManager.remove(KEY_DOMAIN_RESPONSE)
        MMKVManager.remove(KEY_LAST_UPDATE_TIME)
    }
}

