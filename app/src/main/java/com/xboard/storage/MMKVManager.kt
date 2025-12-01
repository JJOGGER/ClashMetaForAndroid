package com.xboard.storage

import android.content.Context
import com.sunmi.background.utils.GsonUtil
import com.tencent.mmkv.MMKV
import com.xboard.model.CommConfigResponse
import com.xboard.model.KnowledgeArticle
import com.xboard.model.Server
import com.xboard.model.SubscribeResponse
import com.xboard.model.UserConfigResponse
import com.xboard.model.UserInfo
import com.xboard.utils.MMKVUtil

/**
 * MMKV本地存储管理器
 * 用于存储用户信息、配置、缓存等数据
 */
object MMKVManager {

    /**
     * 初始化MMKV
     */
    fun init(context: Context) {
        MMKV.initialize(context)
        // 初始化后MMKVUtil会自动使用defaultMMKV
    }

    // ==================== Token相关 ====================

    /**
     * 保存Token
     */
    fun saveToken(token: String) {
        MMKVUtil.getInstance().setValue("token", token)
    }

    /**
     * 获取Token
     */
    fun getToken(): String? {
        return MMKVUtil.getInstance().getStringValue("token", "")
    }

    /**
     * 清除Token
     */
    fun clearToken() {
        MMKVUtil.getInstance().clear(arrayOf("token"))
    }

    /**
     * 是否已登录
     */
    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrEmpty()
    }

    // ==================== 用户信息 ====================

    /**
     * 保存用户ID
     */
    fun saveUserId(userId: Int) {
        MMKVUtil.getInstance().setValue("user_id", userId)
    }

    /**
     * 获取用户ID
     */
    fun getUserId(): Int {
        return MMKVUtil.getInstance().getIntValue("user_id", 0)
    }

    /**
     * 保存用户邮箱
     */
    fun saveUserEmail(email: String) {
        MMKVUtil.getInstance().setValue("user_email", email)
    }

    /**
     * 获取用户邮箱
     */
    fun getUserEmail(): String? {
        return MMKVUtil.getInstance().getStringValue("user_email", "")
    }

    /**
     * 保存用户昵称
     */
    fun saveUserNickname(nickname: String) {
        MMKVUtil.getInstance().setValue("user_nickname", nickname)
    }

    /**
     * 获取用户昵称
     */
    fun getUserNickname(): String? {
        return MMKVUtil.getInstance().getStringValue("user_nickname", "")
    }

    /**
     * 保存用户头像
     */
    fun saveUserAvatar(avatar: String) {
        MMKVUtil.getInstance().setValue("user_avatar", avatar)
    }

    /**
     * 获取用户头像
     */
    fun getUserAvatar(): String? {
        return MMKVUtil.getInstance().getStringValue("user_avatar", "")
    }

    /**
     * 清除用户信息
     */
    fun clearUserInfo() {
        MMKVUtil.getInstance()
            .clear(arrayOf("user_id", "user_email", "user_nickname", "user_avatar"))
    }

    // ==================== 订阅配置 ====================
    fun saveSubscribe(subscribe: SubscribeResponse) {
        MMKVUtil.getInstance().setValue("subscribe", GsonUtil.getGson().toJson(subscribe))
    }

    fun getSubscribe(): SubscribeResponse? {
        val json = MMKVUtil.getInstance().getStringValue("subscribe", "")
        try {
            val subscribe = GsonUtil.getGson().fromJson(json, SubscribeResponse::class.java)
            if (subscribe?.planId == null) {
                return null
            }
            return subscribe
        } catch (e: Exception) {

        }
        return null

    }

    /**
     * 保存订阅URL
     */
    fun saveSubscribeUrl(url: String) {
        MMKVUtil.getInstance().setValue("subscribe_url", url)
    }

    /**
     * 保存订阅配置内容的哈希值（用于检测配置是否变化）
     */
    fun saveSubscribeConfigHash(hash: String) {
        MMKVUtil.getInstance().setValue("subscribe_config_hash", hash)
    }

    /**
     * 获取本地缓存的订阅配置哈希值
     */
    fun getSubscribeConfigHash(): String? {
        return MMKVUtil.getInstance().getStringValue("subscribe_config_hash", "")
    }

    /**
     * 保存订阅配置内容
     */
    fun saveSubscribeConfig(config: String) {
        MMKVUtil.getInstance().setValue("subscribe_config", config)
    }

    /**
     * 获取本地缓存的订阅配置内容
     */
    fun getSubscribeConfig(): String? {
        return MMKVUtil.getInstance().getStringValue("subscribe_config", "")
    }

    /**
     * 清除订阅相关的所有缓存
     */
    fun clearSubscribeCache() {
        MMKVUtil.getInstance()
            .clear(arrayOf("subscribe", "subscribe_config_hash", "subscribe_config"))
    }

    // ==================== 应用配置 ====================

    /**
     * 保存API基础URL
     */
    fun saveApiBaseUrl(url: String) {
        MMKVUtil.getInstance().setValue("api_base_url", url)
    }

    /**
     * 获取API基础URL
     */
    fun getApiBaseUrl(): String? {
        return MMKVUtil.getInstance().getStringValue("api_base_url", "")
    }

    /**
     * 保存主题模式（0=浅色，1=深色，2=跟随系统）
     */
    fun saveThemeMode(mode: Int) {
        MMKVUtil.getInstance().setValue("theme_mode", mode)
    }

    /**
     * 获取主题模式
     */
    fun getThemeMode(): Int {
        return MMKVUtil.getInstance().getIntValue("theme_mode", 2) // 默认跟随系统
    }

    /**
     * 保存是否首次启动
     */
    fun saveFirstLaunch(isFirst: Boolean) {
        MMKVUtil.getInstance().setValue("is_first_launch", isFirst)
    }

    /**
     * 是否首次启动
     */
    fun isFirstLaunch(): Boolean {
        return MMKVUtil.getInstance().getBooleanValue("is_first_launch", true)
    }

    // ==================== 缓存数据 ====================

    /**
     * 保存字符串缓存
     */
    fun saveString(key: String, value: String) {
        MMKVUtil.getInstance().setValue(key, value)
    }

    /**
     * 获取字符串缓存
     */
    fun getString(key: String, defaultValue: String = ""): String {
        return MMKVUtil.getInstance().getStringValue(key, defaultValue) ?: defaultValue
    }

    /**
     * 保存整数缓存
     */
    fun saveInt(key: String, value: Int) {
        MMKVUtil.getInstance().setValue(key, value)
    }

    /**
     * 获取整数缓存
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return MMKVUtil.getInstance().getIntValue(key, defaultValue)
    }

    /**
     * 保存布尔值缓存
     */
    fun saveBoolean(key: String, value: Boolean) {
        MMKVUtil.getInstance().setValue(key, value)
    }

    /**
     * 获取布尔值缓存
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return MMKVUtil.getInstance().getBooleanValue(key, defaultValue)
    }

    /**
     * 保存长整数缓存
     */
    fun saveLong(key: String, value: Long) {
        MMKVUtil.getInstance().setValue(key, value)
    }

    /**
     * 获取长整数缓存
     */
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return MMKVUtil.getInstance().getLongValue(key, defaultValue)
    }

    /**
     * 删除指定key的缓存
     */
    fun remove(key: String) {
        MMKVUtil.getInstance().clear(arrayOf(key))
    }

    /**
     * 清除所有缓存
     */
    fun clear() {
        // 注意：这个操作将会清除所有MMKV数据，不仅限于通过MMKVUtil存储的数据
        MMKV.defaultMMKV().clearAll()
    }

    /**
     * 是否存在指定key
     */
    fun containsKey(key: String): Boolean {
        // 这里需要直接访问MMKV，因为MMKVUtil没有提供containsKey方法
        return MMKV.defaultMMKV().containsKey(key)
    }

    // ==================== 订阅信息 ====================

    /**
     * 保存订阅信息
     */
    fun saveSubscriptionInfo(upload: Long, download: Long, total: Long, expire: Long) {
        MMKVUtil.getInstance().setValue("subscription_upload", upload)
        MMKVUtil.getInstance().setValue("subscription_download", download)
        MMKVUtil.getInstance().setValue("subscription_total", total)
        MMKVUtil.getInstance().setValue("subscription_expire", expire)
    }

    /**
     * 获取已上传流量
     */
    fun getSubscriptionUpload(): Long {
        return MMKVUtil.getInstance().getLongValue("subscription_upload", 0)
    }

    /**
     * 获取已下载流量
     */
    fun getSubscriptionDownload(): Long {
        return MMKVUtil.getInstance().getLongValue("subscription_download", 0)
    }

    /**
     * 获取总流量
     */
    fun getSubscriptionTotal(): Long {
        return MMKVUtil.getInstance().getLongValue("subscription_total", 0)
    }

    /**
     * 获取过期时间
     */
    fun getSubscriptionExpire(): Long {
        return MMKVUtil.getInstance().getLongValue("subscription_expire", 0)
    }

    // ==================== 当前节点信息 ====================

    /**
     * 保存当前节点信息
     */
    fun saveCurrentNode(groupName: String?, proxyName: String?) {
        MMKVUtil.getInstance().setValue("current_group", groupName)
        MMKVUtil.getInstance().setValue("current_proxy", proxyName)
    }

    /**
     * 获取当前代理组
     */
    fun getCurrentGroup(): String? {
        return MMKVUtil.getInstance().getStringValue("current_group", "")
    }

    /**
     * 获取当前代理
     */
    fun getCurrentProxy(): String? {
        return MMKVUtil.getInstance().getStringValue("current_proxy", "")
    }

    fun setExpireNotification(enabled: Boolean) {
        MMKVUtil.getInstance().setValue("expire_notification", enabled)
    }

    fun setTrafficNotification(enabled: Boolean) {
        MMKVUtil.getInstance().setValue("traffic_notification", enabled)
    }

    fun getExpireNotification(): Boolean {
        return MMKVUtil.getInstance().getBooleanValue("expire_notification", false)
    }

    fun getTrafficNotification(): Boolean {
        return MMKVUtil.getInstance().getBooleanValue("traffic_notification", false)
    }

    /**
     * 保存整数值
     */
    fun putInt(key: String, value: Int) {
        MMKVUtil.getInstance().setValue(key, value)
    }

    fun setCommConfigResponse(config: CommConfigResponse) {
        MMKVUtil.getInstance().setValue("config", GsonUtil.getGson().toJson(config))
    }


    fun getCommConfig(): CommConfigResponse? {
        val json = MMKVUtil.getInstance().getStringValue("config", "")
        try {
            return GsonUtil.getGson().fromJson(json, CommConfigResponse::class.java)
        } catch (e: Exception) {
        }
        return null

    }

    fun setUserConfigResponse(config: UserConfigResponse?) {
        if (config == null) {
            MMKVUtil.getInstance().setValue("user_config", "")
            return
        }
        MMKVUtil.getInstance().setValue("user_config", GsonUtil.getGson().toJson(config))
    }


    fun getUserConfig(): UserConfigResponse? {
        val json = MMKVUtil.getInstance().getStringValue("user_config", "")
        try {
            return GsonUtil.getGson().fromJson(json, UserConfigResponse::class.java)
        } catch (e: Exception) {
        }
        return null

    }

    fun setUserInfo(userInfo: UserInfo?) {
        if (userInfo == null) {
            MMKVUtil.getInstance().setValue("user_info", "")
            return
        }
        MMKVUtil.getInstance().setValue("user_info", GsonUtil.getGson().toJson(userInfo))
    }


    fun getUserInfo(): UserInfo? {
        val json = MMKVUtil.getInstance().getStringValue("user_info", "")
        try {
            return GsonUtil.getGson().fromJson(json, UserInfo::class.java)
        } catch (e: Exception) {
        }
        return null

    }

    // ==================== 网站推荐 ====================

    /**
     * 保存网站推荐列表
     */
    fun saveWebsiteRecommendations(articles: List<KnowledgeArticle>?) {
        if (articles == null) {
            return
        }
        MMKVUtil.getInstance()
            .setValue("website_recommendations", GsonUtil.getGson().toJson(articles))
    }

    /**
     * 获取网站推荐列表
     */
    fun getWebsiteRecommendations(): List<KnowledgeArticle>? {
        val json = MMKVUtil.getInstance().getStringValue("website_recommendations", "")
        return try {
            val type = com.google.gson.reflect.TypeToken.getParameterized(
                List::class.java,
                KnowledgeArticle::class.java
            ).type
            GsonUtil.getGson().fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 清除网站推荐缓存
     */
    fun clearWebsiteRecommendations() {
        MMKVUtil.getInstance().clear(arrayOf("website_recommendations"))
    }

    fun setOrderCacheUrl(cacheOrderUrl: String) {
        MMKVUtil.getInstance().setValue("cache_order_url", cacheOrderUrl)
    }

    fun getOrderCacheUrl(): String? {
        return MMKVUtil.getInstance().getStringValue("cache_order_url", "")
    }

    fun setDefaultServer(server: Server?) {
        if (server == null) {
            MMKVUtil.getInstance().setValue("default_server", "")
            return
        }
        MMKVUtil.getInstance().setValue("default_server", GsonUtil.getGson().toJson(server))
    }

    fun getDefaultServer(): Server? {
        val json = MMKVUtil.getInstance().getStringValue("default_server", "")
        try {
            return GsonUtil.getGson().fromJson(json, Server::class.java)
        } catch (e: Exception) {
        }
        return null
    }
}