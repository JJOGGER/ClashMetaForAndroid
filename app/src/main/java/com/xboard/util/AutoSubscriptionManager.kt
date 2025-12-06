package com.xboard.util

import android.util.Log
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.util.withProfile
import com.xboard.model.SubscribeResponse
import com.xboard.network.UserRepository
import com.xboard.storage.MMKVManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

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
    private val userRepository: UserRepository,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "AutoSubscriptionManager"
    }

    private val subscriptionManager = SubscriptionManager(userRepository, scope)
    private val updatingState = AtomicBoolean(false)
    private val updatingStateFlow = MutableStateFlow(false)

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
        if (!updatingState.compareAndSet(false, true)) {
            Log.d(TAG, "Skip auto import, another update is running")
            return false
        }
        updatingStateFlow.value = true
        return try {
            Log.d(TAG, "Starting auto import and apply")

            // 步骤1: 获取订阅信息（OkHttp 层已处理重试）
            Log.d(TAG, "[Step 1] Fetching subscribe information from server...")
            val subscribeFromServer = try {
                withTimeout(20_000) { // 20秒超时
                    subscriptionManager.getSubscribe()
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "[Step 1] Timeout: getSubscribe() took more than 20 seconds")
                return false
            } catch (e: Exception) {
                Log.e(TAG, "[Step 1] Exception in getSubscribe(): ${e.message}", e)
                return false
            }
            
            if (subscribeFromServer == null) {
                Log.w(TAG, "[Step 1] Failed to fetch subscribe information from server")
                return false
            }
            Log.d(TAG, "[Step 1] Success: subscribeUrl=${subscribeFromServer.subscribeUrl}")

            // 步骤2: 确保 Profile 存在
            Log.d(TAG, "[Step 2] Ensuring profile exists...")
            val profile = try {
                withTimeout(10_000) { // 10秒超时
                    ensureProfile(subscribeFromServer)
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "[Step 2] Timeout: ensureProfile() took more than 10 seconds")
                return false
            } catch (e: Exception) {
                Log.e(TAG, "[Step 2] Exception in ensureProfile(): ${e.message}", e)
                return false
            }
            
            if (profile == null) {
                Log.w(
                    TAG,
                    "[Step 2] Unable to locate or create profile for ${subscribeFromServer.subscribeUrl}"
                )
                return false
            }
            Log.d(TAG, "[Step 2] Success: profile=${profile.uuid}, imported=${profile.imported}")

            // 步骤3: 获取缓存信息
            Log.d(TAG, "[Step 3] Getting cached information...")
            val cachedUrl = subscriptionManager.getCachedSubscribeUrl().orEmpty()
            val cachedHash = MMKVManager.getSubscribeConfigHash().orEmpty()
            Log.d(TAG, "[Step 3] cachedUrl=$cachedUrl, cachedHash=${if (cachedHash.isNotBlank()) "exists" else "empty"}")

            // 步骤4: 获取远程配置（OkHttp 层已处理重试）
            Log.d(TAG, "[Step 4] Fetching remote config...")
            var remoteConfig = try {
                withTimeout(30_000) { // 30秒超时（配置下载可能较大）
                    fetchConfigAndHash(subscribeFromServer.subscribeUrl)
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "[Step 4] Timeout: fetchConfigAndHash() took more than 30 seconds")
                null
            } catch (e: Exception) {
                Log.e(TAG, "[Step 4] Exception in fetchConfigAndHash(): ${e.message}", e)
                null
            }
            Log.d(TAG, "[Step 4] remoteConfig=${if (remoteConfig != null) "exists" else "null"}")

            // 步骤5: 确保 Profile 已导入
            Log.d(TAG, "[Step 5] Ensuring profile is imported...")
            val ensuredProfile = try {
                withTimeout(30_000) { // 30秒超时（commit 可能较慢）
                    ensureProfileImported(profile, subscribeFromServer, remoteConfig)
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "[Step 5] Timeout: ensureProfileImported() took more than 30 seconds")
                return false
            } catch (e: Exception) {
                Log.e(TAG, "[Step 5] Exception in ensureProfileImported(): ${e.message}", e)
                return false
            }
            
            if (ensuredProfile == null) {
                Log.w(TAG, "[Step 5] Failed to ensure profile imported")
                return false
            }
            Log.d(TAG, "[Step 5] Success: profile imported=${ensuredProfile.imported}")

            // 步骤6: 判断是否需要更新
            Log.d(TAG, "[Step 6] Checking if update is needed...")
            val urlChanged = cachedUrl.isBlank() || cachedUrl != subscribeFromServer.subscribeUrl
            val configChanged =
                remoteConfig?.second?.let { it.isNotBlank() && it != cachedHash } ?: false
            Log.d(TAG, "[Step 6] urlChanged=$urlChanged, configChanged=$configChanged")

            // 步骤7: 执行相应的流程
            Log.d(TAG, "[Step 7] Executing flow...")
            val result = when {
                urlChanged -> {
                    Log.d(TAG, "[Step 7] Running import flow (url changed)")
                    runImportFlow(ensuredProfile, subscribeFromServer, remoteConfig)
                }
                configChanged -> {
                    Log.d(TAG, "[Step 7] Running update flow (config changed)")
                    runUpdateFlow(ensuredProfile, subscribeFromServer, remoteConfig)
                }
                remoteConfig == null && cachedHash.isBlank() -> {
                    Log.d(TAG, "[Step 7] Running import flow (no cache)")
                    runImportFlow(ensuredProfile, subscribeFromServer, null)
                }
                else -> {
                    Log.d(TAG, "[Step 7] No update needed, just saving and activating")
                    saveSubscribe(subscribeFromServer, remoteConfig?.first, remoteConfig?.second)
                    ensureProfileActive(ensuredProfile)
                    true
                }
            }
            
            Log.d(TAG, "[Step 7] Flow result: $result")
            Log.d(TAG, "Auto import and apply completed: $result")
            result
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Auto import timeout: ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Auto import and apply failed: ${e.message}", e)
            e.printStackTrace()
            false
        } finally {
            updatingState.set(false)
            updatingStateFlow.value = false
            Log.d(TAG, "Auto import state reset")
        }
    }

    fun saveSubscribe(subscribe: SubscribeResponse, configContent: String?, configHash: String?) {
        try {
            MMKVManager.saveSubscribe(subscribe)
            configContent?.let { MMKVManager.saveSubscribeConfig(it) }
            configHash?.let { MMKVManager.saveSubscribeConfigHash(it) }

            Log.d(TAG, "Saved subscribe config to cache")
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to save subscribe config: ${e.message}"
            )
        }
    }

    suspend fun fetchConfigAndHash(subscribeUrl: String): Pair<String, String>? {
        return try {
            val configContent = getConfigContentFromUrl(subscribeUrl)
            if (configContent.isBlank()) {
                Log.w(TAG, "Empty config content from $subscribeUrl")
                null
            } else {
                val configHash = calculateConfigHash(configContent)
                if (configHash.isBlank()) {
                    Log.w(TAG, "Failed to calculate config hash for $subscribeUrl")
                    null
                } else {
                    Log.d(TAG, "Config hash: $configHash")
                    configContent to configHash
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch config and hash: ${e.message}")
            null
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

    private suspend fun ensureProfile(subscribe: SubscribeResponse): Profile? {
        if (subscribe.subscribeUrl.isBlank()) {
            Log.w(TAG, "Subscribe url is empty, skip ensureProfile")
            return null
        }

        return try {
            withProfile {
                val existing = queryAll().firstOrNull { it.source == subscribe.subscribeUrl }
                existing ?: run {
                    val uuid = create(
                        Profile.Type.Url,
                        buildProfileName(subscribe),
                        subscribe.subscribeUrl
                    )
                    queryByUUID(uuid)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to ensure profile: ${e.message}", e)
            null
        }
    }

    private fun buildProfileName(subscribe: SubscribeResponse): String {
        return subscribe.plan?.name.takeIf { it?.isNotBlank()==true }
            ?: subscribe.email
    }

    private suspend fun ensureProfileImported(
        profile: Profile,
        subscribe: SubscribeResponse,
        configBundle: Pair<String, String>?
    ): Profile? {
        if (profile.imported) {
            return profile
        }

        return if (runImportFlow(profile, subscribe, configBundle)) {
            withProfile { queryByUUID(profile.uuid) }
        } else {
            null
        }
    }

    private suspend fun runImportFlow(
        profile: Profile,
        subscribe: SubscribeResponse,
        configBundle: Pair<String, String>?
    ): Boolean {
        return try {
            withProfile {
                patch(
                    profile.uuid,
                    buildProfileName(subscribe),
                    subscribe.subscribeUrl,
                    profile.interval
                )
                coroutineScope {
                    commit(profile.uuid) {
                        launch {
                            saveSubscribe(subscribe, configBundle?.first, configBundle?.second)
                            val latest = queryByUUID(profile.uuid) ?: profile
                            setActive(latest)
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import profile: ${e.message}", e)
            false
        }
    }

    private suspend fun runUpdateFlow(
        profile: Profile,
        subscribe: SubscribeResponse,
        configBundle: Pair<String, String>?
    ): Boolean {
        return try {
            withProfile {
                update(profile.uuid)
            }
            saveSubscribe(subscribe, configBundle?.first, configBundle?.second)
            ensureProfileActive(profile)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update profile: ${e.message}", e)
            false
        }
    }

    private suspend fun ensureProfileActive(profile: Profile) {
        withProfile {
            val latest = queryByUUID(profile.uuid) ?: return@withProfile
            if (!latest.active) {
                setActive(latest)
            }
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
            Log.d(TAG, "getSubscribeConfig() returned: ${if (result.isSuccess()) "success" else "error"}, contentLength=${result.getOrNull()?.length ?: 0}")
            result.getOrNull() ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch config: ${e.message}", e)
            e.printStackTrace()
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


    fun isUpdating(): Boolean = updatingState.get()

    fun updatingFlow(): StateFlow<Boolean> = updatingStateFlow.asStateFlow()

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
