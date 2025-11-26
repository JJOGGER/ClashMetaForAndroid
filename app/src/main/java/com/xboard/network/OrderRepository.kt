package com.xboard.network

import com.xboard.api.ApiService
import com.xboard.model.*

/**
 * 订单相关的Repository
 */
class OrderRepository(private val apiService: ApiService) : BaseRepository() {

    /**
     * 创建订单
     */
    suspend fun createOrder(
        planId: Int,
        period: String = "month_1",
        couponCode: String? = null
    ): ApiResult<String?> {
        return safeApiCall {
            apiService.createOrder(
                CreateOrderRequest(
                    planId = planId,
                    period = period,
                    couponCode = couponCode
                )
            )
        }.map { response ->
            response
        }
    }

    /**
     * 获取订单详情
     */
    suspend fun getOrderDetail(tradeNo: String): ApiResult<OrderDetailResponse> {
        return safeApiCall {
            apiService.getOrderDetail(tradeNo)
        }
    }

    /**
     * 检查订单状态
     */
    suspend fun checkOrderStatus(tradeNo: String): ApiResult<OrderStatusResponse> {
        return safeApiCall {
            apiService.checkOrderStatus(tradeNo)
        }
    }


    /**
     * 发起支付
     */
    suspend fun checkout(
        tradeNo: String,
        methodId: Int
    ): OrderPay<Any?>? {
        return  apiService.checkout(
                CheckoutRequest(
                    tradeNo = tradeNo,
                    method = methodId
                )
            )
    }

    /**
     * 取消订单
     */
    suspend fun cancelOrder(tradeNo: String): ApiResult<Unit> {
        return safeApiCallVoid {
            apiService.cancelOrder(
                CancelOrderRequest(tradeNo)
            )
        }
    }
}
