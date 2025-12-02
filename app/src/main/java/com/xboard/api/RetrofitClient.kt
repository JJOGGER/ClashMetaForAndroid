package com.xboard.api

import android.content.Context
import android.util.Log
import android.provider.Settings
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.xboard.model.Server
import com.github.kr328.clash.common.Global
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okio.Buffer
import okio.IOException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * 重试拦截器，自动重试失败的请求
 * 
 * 重试策略：
 * - 网络异常（IOException）：自动重试
 * - 服务器错误（5xx）：自动重试
 * - 客户端错误（4xx）：不重试，直接返回
 * - 成功响应（2xx）：直接返回
 */
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val retryDelayMs: Long = 1000L
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var lastException: IOException? = null

        // 尝试请求，最多重试 maxRetries 次（总共 maxRetries + 1 次尝试）
        for (attempt in 0..maxRetries) {
            try {
                val response = chain.proceed(request)
                
                // 如果响应成功（2xx），直接返回
                if (response.isSuccessful) {
                    return response
                }
                
                // 如果是客户端错误（4xx），不重试，直接返回
                if (response.code in 400..499) {
                    return response
                }
                
                // 服务器错误（5xx），关闭响应后重试
                response.close()
                
                if (attempt < maxRetries) {
                    val delay = retryDelayMs * (1 shl attempt) // 指数退避：1s, 2s, 4s
                    try {
                        Thread.sleep(delay)
                    } catch (ie: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw IOException("Retry interrupted", ie)
                    }
                } else {
                    // 最后一次尝试也失败，返回错误响应
                    return response
                }
            } catch (e: IOException) {
                lastException = e
                // 网络异常，重试
                if (attempt < maxRetries) {
                    val delay = retryDelayMs * (1 shl attempt)
                    try {
                        Thread.sleep(delay)
                    } catch (ie: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw IOException("Retry interrupted", ie)
                    }
                } else {
                    // 最后一次尝试也失败，抛出异常
                    throw e
                }
            }
        }

        // 理论上不会到这里
        throw lastException ?: IOException("Request failed after $maxRetries retries")
    }
}

/**
 * 自定义日志拦截器，支持中文显示
 */
class ChineseLoggingInterceptor : Interceptor {
    companion object {
        private const val TAG = "OkHttp"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()

        // 记录请求信息
        Log.i(TAG, "--> 请求开始")
        Log.i(TAG, "URL: ${request.url}")
        Log.i(TAG, "方法: ${request.method}")

        // 记录请求头
        request.headers.forEach { (name, value) ->
            Log.i(TAG, "请求头: $name: $value")
        }

        // 记录请求体
        request.body?.let {
            val buffer = Buffer()
            it.writeTo(buffer)
            val charset = StandardCharsets.UTF_8
            Log.i(TAG, "请求体: ${buffer.readString(charset)}")
        }

        val response = chain.proceed(request)
        val duration = System.currentTimeMillis() - startTime

        // 记录响应信息
        Log.i(TAG, "<-- 响应开始 (耗时: ${duration}ms)")
        Log.i(TAG, "状态码: ${response.code}")

        // 记录响应头
        response.headers.forEach { (name, value) ->
            Log.i(TAG, "响应头: $name: $value")
        }

        // 记录响应体
        val responseBody = response.peekBody(Long.MAX_VALUE)
        val charset = StandardCharsets.UTF_8
        val bodyString = responseBody.string()
        // 解码 Unicode 转义序列
        val decodedBody = decodeUnicode(bodyString)
        Log.i(TAG, "响应体: $decodedBody")
        Log.i(TAG, "<-- 响应结束")

        return response
    }

    /**
     * 解码 Unicode 转义序列
     * 例如：\u64cd\u4f5c\u6210\u529f -> 操作成功
     */
    private fun decodeUnicode(input: String): String {
        val pattern = "\\\\u([0-9a-fA-F]{4})".toRegex()
        return pattern.replace(input) { matchResult ->
            val hexCode = matchResult.groupValues[1]
            hexCode.toInt(16).toChar().toString()
        }
    }
}

/**
 * Custom deserializer for List<Server> to handle cases where the API returns [0,0,0] instead of proper server objects
 */
class ServerListDeserializer : JsonDeserializer<List<Server>?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): List<Server>? {
        if (json == null || json.isJsonNull) {
            return null
        }

        // If it's an array of integers like [0,0,0], return empty list
        if (json.isJsonArray) {
            val array = json.asJsonArray
            if (array.size() > 0 && array[0].isJsonPrimitive && array[0].asJsonPrimitive.isNumber) {
                // This is the case where we get [0,0,0] - return empty list instead
                return emptyList()
            }

            // Otherwise, deserialize normally
            val listType = object : TypeToken<List<Server>>() {}.type
            return Gson().fromJson(json, listType)
        }

        return emptyList()
    }
}


/**
 * 创建信任所有证书的TrustManager
 * 用于处理SSL证书验证问题（如自签名证书或证书链问题）
 */
fun createTrustAllManager(): X509TrustManager {
    return object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }
}

/**
 * 创建不验证主机名的HostnameVerifier
 */
fun createTrustAllHostnameVerifier(): HostnameVerifier {
    return HostnameVerifier { _, _ -> true }
}

/**
 * Retrofit 客户端配置
 */
object RetrofitClient {
    private var retrofit: Retrofit? = null
    private var apiService: ApiService? = null
    private var baseUrl: String = ""
    const val BASE_URL = "http://xiuxiujd.cc"
    fun initialize( baseUrl: String) {
        this.baseUrl = baseUrl
        retrofit = null
        apiService = null
    }

    private fun getRetrofit(): Retrofit {
        if (retrofit == null) {
            val gson: Gson = GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .disableHtmlEscaping()
                .registerTypeAdapter(
                    object : TypeToken<List<Server>>() {}.type,
                    ServerListDeserializer()
                )
                .create()

            // 配置SSL信任管理器，处理证书验证问题
            val trustManager = createTrustAllManager()
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf<TrustManager>(trustManager), java.security.SecureRandom())

            val httpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .sslSocketFactory(sslContext.socketFactory, trustManager)
                .hostnameVerifier(createTrustAllHostnameVerifier())
                // 重试拦截器（放在最前面，确保其他拦截器也能重试）
                .addInterceptor(RetryInterceptor(maxRetries = 3, retryDelayMs = 1000L))
                // 为每个请求添加 X-Nonce（设备 ID 加密）
                .addInterceptor(DeviceNonceInterceptor { getDeviceId() })
                .addInterceptor(HeaderInterceptor{getDeviceId() })
                .addInterceptor(ChineseLoggingInterceptor())
                .addInterceptor(ResponseInterceptor())
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        }
        return retrofit!!
    }

    fun getApiService(): ApiService {
        if (apiService == null) {
            apiService = getRetrofit().create(ApiService::class.java)
        }
        return apiService!!
    }

    fun reset() {
        retrofit = null
        apiService = null
    }
}

/**
 * 获取用于加密的设备 ID。
 *
 * 这里使用 ANDROID_ID，保证在同一设备上稳定且无需额外权限。
 */
private fun getDeviceId(): String {
    val context = Global.application
    return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        ?: "unknown"
}
