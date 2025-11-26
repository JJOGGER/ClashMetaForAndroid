package com.xboard.network

import com.xboard.api.ApiService
import com.xboard.model.ChangePasswordRequest
import com.xboard.model.ForgetPasswordRequest
import com.xboard.model.LoginRequest
import com.xboard.model.LoginResponse
import com.xboard.model.RegisterRequest
import com.xboard.model.SendEmailVerifyRequest
import com.xboard.storage.MMKVManager

/**
 * 认证相关的Repository
 */
class AuthRepository(private val apiService: ApiService) : BaseRepository() {

    /**
     * 登录
     */
    suspend fun login(email: String, password: String): ApiResult<LoginResponse> {
        return safeApiCall {
            apiService.login(LoginRequest(email, password))
        }.onSuccess { response ->
            // 保存Token和用户信息
            response.token?.let { MMKVManager.saveToken(it) }
        }
    }

    /**
     * 注册
     */
    suspend fun register(
        email: String,
        password: String,
        emailCode: String,
        inviteCode: String? = null
    ): ApiResult<LoginResponse> {
        return safeApiCall {
            apiService.register(
                RegisterRequest(
                    email = email,
                    password,
                    emailCode,
                    inviteCode
                )
            )
        }
    }

    /**
     * 发送邮箱验证码
     */
    suspend fun sendEmailVerifyCode(email: String, scene: String = "register"): ApiResult<Unit> {
        return safeApiCallVoid {
            apiService.sendEmailVerify(SendEmailVerifyRequest(email, scene))
        }
    }

    /**
     * 忘记密码
     */
    suspend fun forgetPassword(
        email: String,
        emailCode: String,
        password: String
    ): ApiResult<Unit> {
        return safeApiCallVoid {
            apiService.forgetPassword(ForgetPasswordRequest(email, emailCode, password))
        }
    }

    /**
     * 修改密码
     */
    suspend fun changePassword(oldPassword: String, newPassword: String): ApiResult<Unit> {
        return safeApiCallVoid {
            apiService.changePassword(ChangePasswordRequest(oldPassword, newPassword))
        }
    }

    /**
     * 登出
     */
    suspend fun logout(): ApiResult<Unit> {
        return safeApiCallVoid {
            apiService.logout()
        }.onSuccess {
            // 清除本地Token和用户信息
            MMKVManager.clearToken()
            MMKVManager.clearUserInfo()
        }
    }
}
