package com.xboard.api

import com.xboard.utils.MMKVUtil

/**
 * Token 管理器
 */
object TokenManager {
    private const val PREF_NAME = "xboard_token"
    private const val KEY_TOKEN = "token"
    private const val AUTH_DATA = "auth_data"
    private const val KEY_USER_PWD = "user_pwd"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_EXPIRE_TIME = "expire_time"

    fun saveToken(token: String?, authData: String?, email: String?, password: String?) {
        MMKVUtil.getInstance().setValue(KEY_TOKEN, token)
        MMKVUtil.getInstance().setValue(AUTH_DATA, authData)
        MMKVUtil.getInstance().setValue(KEY_USER_PWD, password)
        MMKVUtil.getInstance().setValue(KEY_USER_EMAIL, email)
        MMKVUtil.getInstance()
            .setValue(
                KEY_EXPIRE_TIME,
                System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000
            )
    }

    fun getToken(): String {
        return MMKVUtil.getInstance().getStringValue(KEY_TOKEN, "") ?: ""
    }
    fun getAuthData(): String {
        return MMKVUtil.getInstance().getStringValue(AUTH_DATA, "") ?: ""
    }

    fun getUserEmail(): String {
        return MMKVUtil.getInstance().getStringValue(KEY_USER_EMAIL, "") ?: ""
    }

    fun isTokenValid(): Boolean {
        val token = getToken()
        val expireTime = MMKVUtil.getInstance().getLongValue(KEY_EXPIRE_TIME, 0)
        return token.isNotEmpty() && System.currentTimeMillis() < expireTime
    }

    fun clearToken() {
        MMKVUtil.getInstance()
            .clear(arrayOf(KEY_TOKEN, KEY_USER_PWD, KEY_USER_EMAIL, KEY_EXPIRE_TIME))
    }
}
