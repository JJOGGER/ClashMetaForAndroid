package com.xboard.util

import android.content.Context
import android.util.Log
import com.github.kr328.clash.service.ProfileManager
import com.xboard.model.SubscribeResponse
import com.xboard.network.UserRepository
import com.xboard.storage.MMKVManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*

/**
 * 订阅管理器
 * 
 * 统一管理订阅配置的获取、更新和 VPN 配置同步
 * 集成 ProfileManager 的查询流程和 SubscribeUpdateHelper 的配置更新逻辑
 * 
 * 工作流程：
 * 1. 获取最新的订阅地址 (getSubscribeUrl)
 * 2. 保存到本地缓存 (MMKVManager)
 * 3. 获取配置内容 (getConfigContentFromUrl)
 * 4. 计算配置哈希值 (calculateConfigHash)
 * 5. 检测配置变化 (hasConfigChanged)
 * 6. 保存配置到本地 (saveSubscribeConfig)
 * 7. 更新 VPN 配置 (ProfileManager.update)
 * 8. 通知 UI 更新 (sendProfileChanged)
 */
class SubscriptionManager(
    private val userRepository: UserRepository,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "SubscriptionManager"
    }


    /**
     * 获取订阅
     */
    suspend fun getSubscribe(): SubscribeResponse? {
        return try {
            Log.d(TAG, "Calling userRepository.getSubscribe()...")
            val result = userRepository.getSubscribe()
            Log.d(TAG, "getSubscribe() returned: ${if (result.isSuccess()) "success" else "error"}, ${if (result.isError()) "message=${(result as com.xboard.network.ApiResult.Error).message}" else ""}")
            result.getOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get subscribe URL: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取缓存的订阅地址
     * 
     * 从本地缓存获取订阅地址，不进行网络请求
     * 
     * @return 缓存的订阅地址，如果没有缓存返回 null
     */
    fun getCachedSubscribeUrl(): String? {
        return MMKVManager.getSubscribe()?.subscribeUrl
    }

    /**
     * 获取缓存的配置内容
     * 
     * 从本地缓存获取配置内容
     * 
     * @return 缓存的配置内容，如果没有缓存返回 null
     */
    fun getCachedConfigContent(): String? {
        return MMKVManager.getSubscribeConfig()
    }

    /**
     * 清除缓存
     * 
     * 清除所有本地缓存的订阅信息
     */
    fun clearCache() {
        try {
            // 清除订阅地址
            MMKVManager.saveSubscribeUrl("")
            // 清除配置内容
            MMKVManager.saveSubscribeConfig("")
            // 清除配置哈希值
            MMKVManager.saveSubscribeConfigHash("")
            Log.d(TAG, "Cleared subscription cache")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache: ${e.message}")
        }
    }

}
