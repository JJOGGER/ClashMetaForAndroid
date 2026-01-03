package com.xboard.network

import com.xboard.api.ApiService
import com.xboard.model.CommConfigResponse
import com.xboard.model.OrderDetailResponse
import com.xboard.model.Plan
import com.xboard.model.Server
import com.xboard.model.ServerGroupNode
import com.xboard.model.SubscribeResponse
import com.xboard.model.TrafficLog
import com.xboard.model.UpdateUserRequest
import com.xboard.model.UserConfigResponse
import com.xboard.model.UserInfo
import com.xboard.model.UserStat
import com.xboard.storage.MMKVManager

/**
 * 用户相关的Repository
 */
class UserRepository(private val apiService: ApiService) : BaseRepository() {
    suspend fun getCommonConfig(): ApiResult<CommConfigResponse> {
        return safeApiCall {
            apiService.getGuestConfig()
        }.onSuccess { config ->
            // 更新缓存
            MMKVManager.setCommConfigResponse(config)
        }
    }

    suspend fun getUserCommonConfig(): ApiResult<UserConfigResponse> {
        return safeApiCall {
            apiService.getUserConfig()
        }.onSuccess { config ->
            // 更新缓存
            MMKVManager.setUserConfigResponse(config)
        }
    }

    /**
     * 获取用户信息
     */
    suspend fun getUserInfo(): ApiResult<UserInfo> {
        return safeApiCall {
            apiService.getUserInfo()
        }.onSuccess {
            MMKVManager.setUserInfo(it)
        }
    }


    /**
     * 获取用户套餐列表
     */
    suspend fun getUserPlans(): ApiResult<List<Plan>> {
        return safeApiCall {
            apiService.getUserPlans()
        }
    }

    /**
     * 获取用户统计
     */
    suspend fun getUserStat(): ApiResult<UserStat> {
        return safeApiCall {
            apiService.getUserStat()
        }.map { stats ->
            val orderCount = stats.getOrNull(0) ?: 0
            val tickerCount = stats.getOrNull(1) ?: 0
            val inviteCount = stats.getOrNull(2) ?: 0
            UserStat(orderCount, tickerCount, inviteCount)
        }
    }

    /**
     * 获取流量日志
     */
    suspend fun getTrafficLog(
        page: Int = 1,
        perPage: Int = 20
    ): ApiResult<List<TrafficLog>?> {
        return safeApiCall {
            apiService.getTrafficLog(page, perPage)
        }.map { response ->
            response
        }
    }

    /**
     * 获取用户服务器列表
     */
    suspend fun getUserServers(): ApiResult<MutableList<Server>> {
        return safeApiCall {
            apiService.getUserServers()
        }.map { servers ->
            // Handle case where servers might be empty list instead of null
            servers ?: mutableListOf()
        }
    }

    /**
     * 根据套餐分组获取节点
     */
    suspend fun getServersByGroup(groupId: Int): ApiResult<List<ServerGroupNode>> {
        return safeApiCall {
            apiService.getServersByGroup(groupId)
        }
    }
    /**
     * 更新用户设置（通用方法）
     */
    suspend fun updateUserInfo(request: UpdateUserRequest): ApiResult<Boolean> {
        return safeApiCall {
            apiService.updateUserInfo(request)
        }
    }

    /**
     * 修改密码
     */
    suspend fun changePassword(
        oldPassword: String,
        newPassword: String
    ): ApiResult<Unit> {
        return safeApiCallVoid {
            apiService.changePassword(
                mapOf(
                    "old_password" to oldPassword,
                    "new_password" to newPassword
                )
            )
        }
    }

    suspend fun getSubscribe(): ApiResult<SubscribeResponse> {
        return safeApiCall {
            apiService.getSubscribe()
        }.map { response ->
            response
        }
    }

    /**
     * 获取订阅配置内容
     *
     * 注意：订阅URL返回的是纯文本YAML格式，不是JSON格式的ApiResponse
     * 所以需要直接处理 ResponseBody，而不是通过 safeApiCall
     *
     * @param subscribeUrl 订阅URL，例如：https://example.com/api/v1/client/subscribe?token=xxx
     * @return 配置内容（YAML格式）
     */
    suspend fun getSubscribeConfig(subscribeUrl: String): ApiResult<String> {
        return try {
            val responseBody = apiService.getSubscribeConfig(subscribeUrl)
            // 使用 UTF-8 字符集读取，确保编码一致
            // ResponseBody.string() 默认使用 UTF-8，但为了明确，我们显式指定
            val content = responseBody.string()

            if (content.isNotEmpty()) {
                ApiResult.Success(content)
            } else {
                ApiResult.Error(code = -1, message = "Empty response body")
            }
        } catch (e: Exception) {
            ApiResult.Error(code = -1, message = e.message ?: "Unknown error")
        }
    }

    /**
     * 获取订单历史
     */
    suspend fun getOrderHistory(
        page: Int = 1,
        perPage: Int = 20
    ): ApiResult<List<OrderDetailResponse>> {
        return safeApiCall {
            apiService.getOrderHistory(page, perPage)
        }
    }
}