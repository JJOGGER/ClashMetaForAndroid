package com.xboard.network

/**
 * API请求结果的统一封装
 */
sealed class ApiResult<out T> {
    /**
     * 成功
     */
    data class Success<T>(val data: T) : ApiResult<T>()

    /**
     * 错误
     */
    data class Error(
        val code: Int = -1,
        val message: String = "Unknown error",
        val exception: Exception? = null
    ) : ApiResult<Nothing>()

    /**
     * 加载中
     */
    object Loading : ApiResult<Nothing>()

    /**
     * 获取数据或null
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    /**
     * 获取异常或null
     */
    fun exceptionOrNull(): Exception? = when (this) {
        is Error -> exception
        else -> null
    }

    /**
     * 是否成功
     */
    fun isSuccess(): Boolean = this is Success

    /**
     * 是否失败
     */
    fun isError(): Boolean = this is Error

    /**
     * 是否加载中
     */
    fun isLoading(): Boolean = this is Loading

    /**
     * 转换结果
     */
    inline fun <R> map(transform: (T) -> R): ApiResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> Loading
    }

    /**
     * 处理结果
     */
    inline fun onSuccess(action: (T) -> Unit): ApiResult<T> {
        if (this is Success) action(data)
        return this
    }

    /**
     * 处理错误
     */
    inline fun onError(action: (Error) -> Unit): ApiResult<T> {
        if (this is Error) action(this)
        return this
    }

    /**
     * 处理加载
     */
    inline fun onLoading(action: () -> Unit): ApiResult<T> {
        if (this is Loading) action()
        return this
    }
}
