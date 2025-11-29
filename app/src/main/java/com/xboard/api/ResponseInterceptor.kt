package com.xboard.api

import android.content.Intent
import android.util.Log
import com.github.kr328.clash.common.Global
import com.xboard.ui.activity.LoginActivity
import com.xboard.util.NetworkDiagnostics
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * 响应拦截器，用于处理特定的HTTP响应状态码
 * 特别是处理403状态码（token过期）的情况
 */
class ResponseInterceptor : Interceptor {
    companion object {
        private const val TAG = "ResponseInterceptor"
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        
        // 诊断 502 错误（可能是代理配置问题）
        if (response.code == 502) {
            try {
                val domain = request.url.host
                val diagnostics = NetworkDiagnostics(Global.application)
                diagnostics.diagnoseApiFailure(domain, response.code, "Bad Gateway")
                Log.e(TAG, "502 Error detected for domain: $domain")
            } catch (e: Exception) {
                Log.e(TAG, "Error during 502 diagnosis: ${e.message}")
            }
        }
        
        // 检查是否是403状态码（Forbidden）
        if (response.code == 403) {
            // 尝试读取响应体来检查是否是token过期
            try {
                val responseBody = response.peekBody(1024) // 限制读取大小
                val responseString = responseBody.string()

                // 解码 Unicode 转义序列
                val decodedBody = decodeUnicode(responseString)
                // 检查响应中是否包含token过期的相关信息
                if (decodedBody.contains("未登录或登陆已过期") ||
                    decodedBody.contains("token expired") ||
                    decodedBody.contains("unauthorized") ||
                    decodedBody.contains("未授权")) {
                    
                    Log.d(TAG, "Token expired or unauthorized, redirecting to login")
                    
                    // 清除本地存储的token
                    TokenManager.clearToken()
                    
                    // 在主线程中启动登录Activity
                    val appContext = Global.application
                    val intent = Intent(appContext, LoginActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    appContext.startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading response body", e)
            }
        }
        
        return response
    }
    private fun decodeUnicode(input: String): String {
        val pattern = "\\\\u([0-9a-fA-F]{4})".toRegex()
        return pattern.replace(input) { matchResult ->
            val hexCode = matchResult.groupValues[1]
            hexCode.toInt(16).toChar().toString()
        }
    }
}