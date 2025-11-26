package com.xboard.util

import android.content.Context
import android.util.Log
import com.github.kr328.clash.service.ProfileManager
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.util.withProfile
import com.xboard.model.SubscribeResponse
import com.xboard.network.UserRepository
import com.xboard.storage.MMKVManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.*

/**
 * 自动订阅管理器
 *
 * 负责完整的自动化流程：
 * 1. 获取或创建 Profile
 * 2. 自动更新配置（导入）
 * 3. 自动选中 Profile（应用）
 * 4. 自动启动 VPN
 *
 * 用户购买订阅后，无需任何手动操作，系统会自动完成所有步骤
 */
class AutoSubscriptionManager(
    private val context: Context,
    private val userRepository: UserRepository,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "AutoSubscriptionManager"
    }

    private val profileManager = ProfileManager(context)
    private val subscriptionManager = SubscriptionManager(context, userRepository, scope)

    /**
     * 自动导入和应用订阅
     *
     * 完整流程：
     * 1. 获取或创建 Profile UUID
     * 2. 自动更新配置（导入）
     * 3. 提交配置（commit）
     * 4. 自动选中 Profile（应用）
     *
     * 注意：不会自动启动 VPN，用户需要手动点击开始连接
     *
     * @return 是否成功完成整个流程
     */
    suspend fun autoImportAndApply(): Boolean {
        try {
            Log.d(TAG, "Starting auto import and apply")
            val subscribeUrl = subscriptionManager.getCachedSubscribeUrl()
            Log.d(TAG, "本地缓存的订阅地址: $subscribeUrl")
            val subscribeConfig = subscriptionManager.getCachedConfigContent()
            Log.d(TAG, "本地缓存的订阅配置: $subscribeConfig")
            val subscribeUrlServer = subscriptionManager.getSubscribe()
            if (subscribeUrlServer == null) {
                Log.d(TAG, "未获取到服务端配置")
                return false
            }
            // 3. 获取配置内容
            val configContent = fetchConfigAndHash(subscribeUrlServer.subscribeUrl)
            if (subscribeUrlServer.subscribeUrl != subscribeUrl || configContent.first != subscribeConfig) {
                Log.d(TAG, "更新配置")
                withProfile {

                    val profile =
                        queryByUUID(UUID.fromString(subscribeUrlServer.uuid)) ?: return@withProfile
                    // 4. 更新VPN配置
                    if (subscribeConfig == null) {
                        Log.d(TAG, "导入VPN配置")
                        patch(profile.uuid, profile.name, profile.source, profile.interval)
                        coroutineScope {
                            commit(profile.uuid) {
                                launch {
                                    Log.d(TAG, "导入配置成功")
                                    saveSubscribe(subscribeUrlServer,configContent.first,
                                        configContent.second)
                                    setActive(profile)
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, "更新VPN配置")
                        update(profile.uuid)
                    }

                }
                return true
            }

            return true
        } catch (e: Exception) {

        }
        return false
    }

    fun saveSubscribe(subscribe: SubscribeResponse, configContent: String, configHash: String) {
        try {
            MMKVManager.saveSubscribe(subscribe)
            MMKVManager.saveSubscribeConfig(configContent)
            MMKVManager.saveSubscribeConfigHash(configHash)

            Log.d(TAG, "Saved subscribe config to cache")
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to save subscribe config: ${e.message}"
            )
        }
    }
    suspend fun fetchConfigAndHash(subscribeUrl: String): Pair<String, String> {
        return try {
            // 1. 获取配置内容
            val configContent = getConfigContentFromUrl(subscribeUrl)

            // 2. 计算哈希值
            val configHash = calculateConfigHash(configContent)

            Log.d(TAG, "Config hash: $configHash")

            Pair(configContent, configHash)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch config and hash: ${e.message}")
            Pair("", "")
        }
    }

    fun calculateConfigHash(content: String): String {
        return try {
            if (content.isEmpty()) {
                return ""
            }

            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(content.toByteArray())

            // 转换为十六进制字符串
            val hexString = StringBuilder()
            for (byte in hashBytes) {
                val hex = Integer.toHexString(0xff and byte.toInt())
                if (hex.length == 1) {
                    hexString.append('0')
                }
                hexString.append(hex)
            }

            hexString.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate hash: ${e.message}")
            ""
        }
    }

    /**
     * 获取订阅配置内容
     *
     * @param subscribeUrl 订阅URL，例如：https://example.com/api/v1/client/subscribe?token=xxx
     * @return 配置内容（YAML格式）
     */
    suspend fun getConfigContentFromUrl(subscribeUrl: String): String {
        return try {
            Log.d(TAG, "Fetching config from: $subscribeUrl")
            val result = userRepository.getSubscribeConfig(subscribeUrl)
            result.getOrNull() ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch config: ${e.message}")
            ""
        }
    }


    /**
     * 提交配置（commit）
     *
     * 这一步会：
     * 1. 验证 Profile 配置
     * 2. 提交配置到 Clash
     * 3. 将 Profile 从 Pending 转移到 Imported
     *
     * @param profileUUID Profile 的 UUID
     * @return 是否成功提交
     */
    private suspend fun commitProfile(profile: Profile): Boolean {
        return try {
            var success = false
            var completed = false

            withProfile {
                // 提交配置
                commit(profile.uuid) { updateStatus ->
                    Log.d(TAG, "Profile commit status: $updateStatus")
                    success = true
                    completed = true
                }
            }

            // 等待提交完成（最多等待 10 秒）
            var waitCount = 0
            while (!completed && waitCount < 100) {
                delay(100)
                waitCount++
            }

            if (!completed) {
                Log.e(TAG, "Profile commit timeout")
                return false
            }

            Log.d(TAG, "Profile committed successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to commit profile: ${e.message}", e)
            false
        }
    }

    /**
     * 选中 Profile（应用）
     *
     * 这一步会：
     * 1. 设置 Profile 为活跃
     * 2. 触发 ProfileProcessor.apply()
     * 3. 生成 Clash 配置文件
     *
     * @param profileUUID Profile 的 UUID
     * @return 是否成功选中
     */
    private suspend fun selectProfile(profileUUID: UUID): Boolean {
        return try {
            Log.d(TAG, "Selecting profile: $profileUUID")

            val result = withProfile {
                // 首先获取 Profile 对象
                val profile = queryByUUID(profileUUID)
                if (profile == null) {
                    Log.e(TAG, "Profile not found: $profileUUID")
                    return@withProfile false
                }

                // 设置为活跃 Profile
                setActive(profile)

                Log.d(TAG, "Profile set as active: $profileUUID")
                true
            }

            result ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to select profile: ${e.message}", e)
            false
        }
    }


    /**
     * 异步自动导入和应用订阅
     *
     * @param onSuccess 成功回调
     * @param onError 失败回调
     */
    fun autoImportAndApplyAsync(
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        scope.launch {
            try {
                val success = autoImportAndApply()
                if (success) {
                    onSuccess?.invoke()
                } else {
                    onError?.invoke("Auto import and apply failed")
                }
            } catch (e: Exception) {
                onError?.invoke(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * 检查是否需要自动导入
     *
     * @return 是否需要自动导入
     */
    suspend fun shouldAutoImport(): Boolean {
        return try {
            withProfile {
                val profiles = queryAll()
                val urlProfile = profiles.find { it.type.name == "Url" }

                // 如果没有 Url 类型的 Profile，需要自动导入
                urlProfile == null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check if should auto import: ${e.message}", e)
            false
        }
    }

    /**
     * 获取当前活跃的 Profile
     *
     * @return 活跃 Profile，如果没有返回 null
     */
    suspend fun getActiveProfile(): com.github.kr328.clash.service.model.Profile? {
        return try {
            withProfile {
                val profiles = queryAll()
                profiles.find { it.active }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get active profile: ${e.message}", e)
            null
        }
    }

}
