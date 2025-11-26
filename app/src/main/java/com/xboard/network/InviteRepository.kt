package com.xboard.network

import com.xboard.api.ApiService
import com.xboard.model.*

/**
 * 邀请相关的Repository
 */
class InviteRepository(private val apiService: ApiService) : BaseRepository() {

    /**
     * 获取邀请信息
     */
    suspend fun getInviteInfo(): ApiResult<InviteResponse> {
        return safeApiCall {
            apiService.getInviteInfo()
        }
    }

    /**
     * 获取邀请明细
     */
    suspend fun getInviteDetails(
        page: Int = 1,
        perPage: Int = 20
    ): ApiResult<List<InviteDetail>> {
        return safeApiCall {
            apiService.getInviteDetails(page, perPage)
        }.map { response ->
            response.data
        }
    }

    /**
     * 生成邀请码
     */
    suspend fun generateInviteCode(): ApiResult<String> {
        return safeApiCall {
            apiService.generateInviteCode()
        }.map { response ->
            response.inviteCode
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
