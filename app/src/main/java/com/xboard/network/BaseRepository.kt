package com.xboard.network

import android.util.Log
import com.sunmi.background.utils.GsonUtil
import com.xboard.model.ApiResponse
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Repository基类 - 处理网络请求的通用逻辑
 */
abstract class BaseRepository {

    companion object {
        private const val TAG = "BaseRepository"
    }

    /**
     * 执行网络请求并处理异常
     */
    protected suspend fun <T> safeApiCall(
        apiCall: suspend () -> ApiResponse<T>
    ): ApiResult<T> {
        return try {
            val response = apiCall()
            if (response.isSuccess() && response.data != null) {
                ApiResult.Success(response.data)
            } else {
                // Return response body even for failed responses (like HTTP 400)
                ApiResult.Error(
                    message = response.message ?: response.error ?: "Unknown error",
                    code = if (response.status == "fail") 0 else -1 // Use code 0 to indicate business failure vs network error
                )
            }
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Request timeout", e)
            ApiResult.Error(
                code = -1,
                message = "Request timeout, please try again",
                exception = e
            )
        } catch (e: IOException) {
            Log.e(TAG, "Network error", e)
            ApiResult.Error(
                code = -1,
                message = "Network error, please check your connection",
                exception = e
            )
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP error: ${e.code()}", e)
            // Parse error body for HTTP exceptions
            val errorBody = try {
                e.response()?.errorBody()?.string() ?: "Server error: ${e.code()}"
            } catch (ex: Exception) {
                "Server error: ${e.code()}"
            }
            try {
                val errorResponse = GsonUtil.getGson()
                    .fromJson<ApiResponse<Any>>(errorBody, ApiResponse::class.java)
                ApiResult.Error(
                    code = e.code(),
                    message = errorResponse?.message.toString(),
                    exception = e
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing error body: ${e.message}", e)
                ApiResult.Error(
                    code = -1,
                    message = e.message.toString(),
                    exception = e
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Unknown error", e)
            ApiResult.Error(
                code = -1,
                message = e.message ?: "Unknown error",
                exception = e
            )
        }
    }

    protected suspend fun <T> safeApiDirectCall(
        apiCall: suspend () -> T
    ): ApiResult<T> {
        return try {
            val response = apiCall()
            if (response != null) {
                ApiResult.Success(response)
            } else {
                // Return response body even for failed responses (like HTTP 400)
                ApiResult.Error(
                    message =  "Unknown error",
                    code =  -1 // Use code 0 to indicate business failure vs network error
                )
            }
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Request timeout", e)
            ApiResult.Error(
                code = -1,
                message = "Request timeout, please try again",
                exception = e
            )
        } catch (e: IOException) {
            Log.e(TAG, "Network error", e)
            ApiResult.Error(
                code = -1,
                message = "Network error, please check your connection",
                exception = e
            )
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP error: ${e.code()}", e)
            // Parse error body for HTTP exceptions
            val errorBody = try {
                e.response()?.errorBody()?.string() ?: "Server error: ${e.code()}"
            } catch (ex: Exception) {
                "Server error: ${e.code()}"
            }
            try {
                val errorResponse = GsonUtil.getGson()
                    .fromJson<ApiResponse<Any>>(errorBody, ApiResponse::class.java)
                ApiResult.Error(
                    code = e.code(),
                    message = errorResponse?.message.toString(),
                    exception = e
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing error body: ${e.message}", e)
                ApiResult.Error(
                    code = -1,
                    message = e.message.toString(),
                    exception = e
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Unknown error", e)
            ApiResult.Error(
                code = -1,
                message = e.message ?: "Unknown error",
                exception = e
            )
        }
    }
    /**
     * 执行网络请求（不需要返回数据）
     */
    protected suspend fun safeApiCallVoid(
        apiCall: suspend () -> ApiResponse<*>
    ): ApiResult<Unit> {
        return try {
            val response = apiCall()
            if (response.isSuccess()) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error(
                    message = response.message ?: response.error ?: "Unknown error"
                )
            }
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Request timeout", e)
            ApiResult.Error(
                code = -1,
                message = "Request timeout, please try again",
                exception = e
            )
        } catch (e: IOException) {
            Log.e(TAG, "Network error", e)
            ApiResult.Error(
                code = -1,
                message = "Network error, please check your connection",
                exception = e
            )
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP error: ${e.code()}", e)
            ApiResult.Error(
                code = e.code(),
                message = "Server error: ${e.code()}",
                exception = e
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unknown error", e)
            ApiResult.Error(
                code = -1,
                message = e.message ?: "Unknown error",
                exception = e
            )
        }
    }
}