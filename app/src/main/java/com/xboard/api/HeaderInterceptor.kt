package com.xboard.api

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 请求头拦截器，只添加Authorization头
 */
class HeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        requestBuilder.header("content-language", "zh-CN")
        val token = TokenManager.getToken()
        if (token.isNotEmpty()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        requestBuilder.addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
        val newRequest = requestBuilder.build()
        return chain.proceed(newRequest)
    }
}