package com.xboard.network

import com.xboard.api.ApiService
import com.xboard.model.*

/**
 * 套餐相关的Repository
 */
class PlanRepository(private val apiService: ApiService) : BaseRepository() {

//    /**
//     * 获取所有可用套餐（游客）
//     */
//    suspend fun getGuestPlans(): ApiResult<List<Plan>> {
//        return safeApiCall {
//            apiService.getGuestPlans()
//        }.map { response ->
//            response.plans
//        }
//    }

    /**
     * 获取用户套餐列表
     */
    suspend fun getAllPlans(): ApiResult<List<Plan>> {
        return safeApiCall {
            apiService.getUserPlans()
        }
    }


    /**
     * 检查优惠券
     */
    suspend fun checkCoupon(
        couponCode: String,
        planId: Int? = null,
        period: String? = null
    ): ApiResult<CouponResponse> {
        return safeApiCall {
            apiService.checkCoupon(
                CheckCouponRequest(couponCode, planId,period)
            )
        }
    }

    /**
     * 检查礼品卡
     */
    suspend fun checkGiftCard(cardCode: String): ApiResult<GiftCardResponse> {
        return safeApiCall {
            apiService.checkGiftCard(
                CheckGiftCardRequest(cardCode)
            )
        }
    }

    /**
     * 兑换礼品卡
     */
    suspend fun redeemGiftCard(cardCode: String): ApiResult<Unit> {
        return safeApiCallVoid {
            apiService.redeemGiftCard(
                RedeemGiftCardRequest(cardCode)
            )
        }
    }

    /**
     * 获取礼品卡历史
     */
    suspend fun getGiftCardHistory(
        page: Int = 1,
        perPage: Int = 20
    ): ApiResult<List<GiftCardHistory>> {
        return safeApiCall {
            apiService.getGiftCardHistory(page, perPage)
        }.map { response ->
            response.data
        }
    }
}
