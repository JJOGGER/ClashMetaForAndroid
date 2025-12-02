package com.xboard.api

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 请求头拦截器，只添加Authorization头
 */
class HeaderInterceptor(
    private val deviceIdProvider: () -> String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        requestBuilder.header("content-language", "zh-CN")
        val authData = TokenManager.getAuthData()
        if (authData.isNotEmpty()) {
            requestBuilder.header("Authorization", authData)
        }
        val deviceId = deviceIdProvider()
        val nonce = DeviceIdCrypto.encryptDeviceIdToNonce(deviceId)
        requestBuilder.addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .addHeader("X-Nonce", nonce)
        val newRequest = requestBuilder.build()
        return chain.proceed(newRequest)
    }
}