package com.xboard.network

import com.xboard.api.ApiService
import com.xboard.model.*

/**
 * 邀请相关的Repository
 */
class InviteRepository(private val apiService: ApiService) : BaseRepository() {

    /**
     * 获取邀请信息（邀请码列表和统计数据）
     */
    suspend fun getInviteInfo(): ApiResult<InviteDetailsResponse> {
        return safeApiCall {
            apiService.getInviteInfo()
        }
    }

    /**
     * 获取邀请详情（邀请用户的详细信息）
     */
    suspend fun getInviteDetails(
        current: Int = 1,
        pageSize: Int = 20
    ): ApiResult<InviteDetailResponse?> {
        return safeApiDirectCall {
            apiService.getInviteDetails(current, pageSize)
        }
    }

    /**
     * 生成邀请码
     */
    suspend fun generateInviteCode(): ApiResult<Boolean> {
        return safeApiCall {
            apiService.generateInviteCode()
        }
    }

    /**
     * 返利划转到余额
     */
    suspend fun transferCommission(amount: Double): ApiResult<Unit> {
        return safeApiCallVoid {
            apiService.transferCommission(
                TransferRequest(amount)
            )
        }
    }
}
