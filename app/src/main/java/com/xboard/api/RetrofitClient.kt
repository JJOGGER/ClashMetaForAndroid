package com.xboard.api

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.xboard.model.Server
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okio.Buffer
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

    fun initialize(context: Context, baseUrl: String) {
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
                .addInterceptor(HeaderInterceptor())
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