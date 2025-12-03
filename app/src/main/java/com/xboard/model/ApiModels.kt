package com.xboard.model

import android.graphics.Color
import androidx.core.graphics.toColorInt
import com.google.gson.annotations.SerializedName
import com.sunmi.background.utils.GsonUtil
import java.io.Serializable
import java.text.DecimalFormat
import kotlin.math.max

// ==================== 通用响应模型 ====================

/**
 * 统一API响应格式
 */
data class ApiResponse<T>(
    val status: String?,
    val data: T? = null,
    val message: String? = "ok",
    val error: String? = null,
) {
    fun isSuccess(): Boolean = status == null || status == "success"
}

data class OrderPay<T>(
    val type: Int?,
    val data: T? = null
)
// ==================== 认证相关 ====================

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    @SerializedName("email_code")
    val emailCode: String,
    @SerializedName("invite_code")
    val inviteCode: String? = null
)

data class LoginResponse(
    val token: String?,
    @SerializedName("auth_data")
    val authData: String?,
    @SerializedName("is_admin")
    val isAdmin: Boolean?
)

data class UserInfo(

    //"email": "980328722@qq.com",
    //        "transfer_enable": 107374182400,
    //        "last_login_at": 1764298287,
    //        "created_at": 1763379590,
    //        "banned": false,
    //        "remind_expire": true,
    //        "remind_traffic": true,
    //        "expired_at": 1779020849,
    //        "balance": 100,
    //        "commission_balance": 100,
    //        "plan_id": 1,
    //        "discount": null,
    //        "commission_rate": null,
    //        "telegram_id": null,
    //        "uuid": "b13152a7-1cad-43b4-89ab-6a6de4fc4061",
    //        "avatar_url": "https:\/\/cdn.v2ex.com\/gravatar\/729991e46535d907bb47cb02708af1e2?s=64&d=identicon"
    //
    val email: String,
    val balance: Double = 0.0,
    @SerializedName("commission_balance")
    val commissionBalance: Double = 0.0,
    @SerializedName("commission_rate")
    val commissionRate: Int = 0,

    @SerializedName("expired_at")
    val expiredAt: Long? = null,
    @SerializedName("plan_id")
    val planId: Int? = null,
    @SerializedName("created_at")
    val createdAt: Long,
    @SerializedName("updated_at")
    val updatedAt: Long,
    // 订阅信息字段
    @SerializedName("transfer_enable")
    val transferEnable: Long? = null,

    // 新增字段 - 根据注释中的示例JSON添加
    @SerializedName("last_login_at")
    val lastLoginAt: String? = null,
    @SerializedName("banned")
    val banned: Boolean = false,
    @SerializedName("remind_expire")
    var remindExpire: Boolean = true,
    @SerializedName("remind_traffic")
    var remindTraffic: Boolean = true,
    @SerializedName("discount")
    val discount: Int = 100,
    @SerializedName("telegram_id")
    val telegramId: String? = null,
    @SerializedName("uuid")
    val uuid: String? = null,
    @SerializedName("avatar_url")
    val avatarUrl: String? = null
)

data class SendEmailVerifyRequest(
    val email: String,
    val scene: String // "register", "forget", "change_email"
)

data class ForgetPasswordRequest(
    val email: String,
    val email_code: String,
    val password: String
)

data class UpdateUserRequest(
    val remind_expire: Int? = null,
    val remind_traffic: Int? = null
)

data class ChangePasswordRequest(
    @SerializedName("old_password")
    val oldPassword: String,
    @SerializedName("new_password")
    val newPassword: String
)

// ==================== 套餐相关 ====================

data class GuestPlanResponse(
    val plans: List<Plan>,
    @SerializedName("coupons_enabled")
    val couponsEnabled: Boolean = false
)

data class UserPlanResponse(
    val plans: List<Plan>,
    @SerializedName("coupons_enabled")
    val couponsEnabled: Boolean = false
)

data class Plan(
    val id: Int,
    val name: String,
    val traffic: Long = 0, // 字节
    @SerializedName("transfer_enable")
    val transferEnable: Long = 0,
    @SerializedName("group_id")
    val groupId: Int? = null,

    @SerializedName("reset_traffic_method")
    val resetTrafficMethod: Int? = null,
    @SerializedName("content")
    val content: String? = null,
    @SerializedName("description")
    val description: String? = null,

    @SerializedName("month_price")
    val monthPrice: Double? = null,

    @SerializedName("quarter_price")
    val quarterPrice: Double? = null,

    @SerializedName("half_year_price")
    val halfYearPrice: Double? = null,

    @SerializedName("year_price")
    val yearPrice: Double? = null,

    @SerializedName("two_year_price")
    val twoYearPrice: Double? = null,

    @SerializedName("three_year_price")
    val threeYearPrice: Double? = null,

    @SerializedName("onetime_price")
    val onetimePrice: Double? = null,

    @SerializedName("reset_price")
    val resetPrice: Double? = null,

    @SerializedName("capacity_limit")
    val capacityLimit: Long? = null,

    @SerializedName("speed_limit")
    val speedLimit: Long? = null,

    @SerializedName("device_limit")
    val deviceLimit: Int? = null,

    val show: Boolean = true,
    val sell: Boolean = true,
    val renew: Boolean = true,
    val tags: List<String>? = null,

    @SerializedName("created_at")
    val createdAt: Long? = null,

    @SerializedName("updated_at")
    val updatedAt: Long? = null
) : Serializable {
    companion object {
        private val priceFormatter = DecimalFormat("#.##")
        const val MAX_TRAFFIC = Integer.MAX_VALUE
    }

    fun getRealPlanPrice(period: String?): Double {
        return when (period) {
            "month_price" -> monthPrice
            "quarter_price" -> quarterPrice
            "half_year_price" -> halfYearPrice
            "year_price" -> yearPrice
            "two_year_price" -> twoYearPrice
            "three_year_price" -> threeYearPrice
            "onetime_price" -> onetimePrice
            else -> 0.0
        } ?: 0.0
    }

    fun getShowPrice(): Pair<String, String> {
        if (monthPrice != null && monthPrice != 0.0) {
            return Pair("月付", "${formatPrice(monthPrice)}")
        } else if (quarterPrice != null && quarterPrice != 0.0) {
            return Pair("季付", "${formatPrice(quarterPrice)}")
        } else if (halfYearPrice != null && halfYearPrice != 0.0) {
            return Pair("半年付", "${formatPrice(halfYearPrice)}")
        } else if (yearPrice != null && yearPrice != 0.0) {
            return Pair("年付", "${formatPrice(yearPrice)}")
        } else if (twoYearPrice != null && twoYearPrice != 0.0) {
            return Pair("两年付", "${formatPrice(twoYearPrice)}")
        } else if (threeYearPrice != null && threeYearPrice != 0.0) {
            return Pair("三年付", "${formatPrice(threeYearPrice)}")
        } else if (onetimePrice != null && onetimePrice != 0.0) {
            return Pair("一次性", "${formatPrice(onetimePrice)}")
        } else {
            return Pair("试用套餐", "0")
        }

    }


    private fun formatPrice(amount: Double): String {
        // Round to avoid floating point precision issues
        val rounded = (max(amount, 0.0)) / 100.0
        return priceFormatter.format(rounded)
    }
}

// ==================== 用户统计相关 ====================

data class UserStat(
    val orderCount: Int = 0,
    val tickerCount: Int = 0,
    val inviteCount: Int = 0
)

data class TrafficLogResponse(
    val data: List<TrafficLog>,
    val total: Int,
    @SerializedName("per_page")
    val perPage: Int,
    val current_page: Int
)

data class TrafficLog(
    @SerializedName("user_id")
    val user_id: Int? = null,
    @SerializedName("u")
    val u: Long? = null,
    @SerializedName("d")
    val d: Long? = null,
    @SerializedName("record_at")
    val record_at: String? = null
)

// ==================== 节点相关 ====================

data class ServerListResponse(
    val servers: List<Server>
)

data class Server(
    val id: Int,
    val name: String,
    val host: String,
    val port: Int,
    val cipher: String? = null,
    val protocol: String? = null,
    val obfs: String? = null,
    val obfs_param: String? = null,
    val method: String? = null,
    @SerializedName("server_port")
    val serverPort: Int? = null,
    @SerializedName("server_cipher")
    val serverCipher: String? = null
): Serializable

data class ServerGroupNode(
    val id: Int,
    val type: String,
    val version: String,
    val name: String,
    val rate: Double,
    val tags: List<String> = emptyList(),
    @SerializedName("is_online")
    val isOnline: Int,
    @SerializedName("cache_key")
    val cacheKey: String,
    @SerializedName("last_check_at")
    val lastCheckAt: String
)

data class SubscribeResponse(
    @SerializedName("plan_id")
    val planId: Int? = null,
    @SerializedName("token")
    val token: String,
    @SerializedName("expired_at")
    val expiredAt: String,
    @SerializedName("u")
    val upload: Long,
    @SerializedName("d")
    val download: Long,
    @SerializedName("transfer_enable")
    val transferEnable: Long,
    @SerializedName("email")
    val email: String,
    @SerializedName("uuid")
    val uuid: String,
    @SerializedName("device_limit")
    val deviceLimit: Int,
    @SerializedName("speed_limit")
    val speedLimit: Int,
    @SerializedName("next_reset_at")
    val nextResetAt: String,
    @SerializedName("plan")
    val plan: PlanInfo,
    @SerializedName("subscribe_url")
    val subscribeUrl: String,
    @SerializedName("clash_url")
    val clashUrl: String? = null,
    @SerializedName("surge_url")
    val surgeUrl: String? = null,
    @SerializedName("sing_box_url")
    val singBoxUrl: String? = null,
    @SerializedName("reset_day")
    val resetDay: Int = 0
)

data class PlanInfo(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("transfer_enable")
    val transferEnable: Long
)

// ==================== 公共配置 ====================

data class CommConfigResponse(
    @SerializedName("tos_url")
    val tosUrl: String?,
    @SerializedName("is_email_verify")
    val isEmailVerify: Int,
    @SerializedName("is_invite_force")
    val isInviteForce: Int,
    @SerializedName("email_whitelist_suffix")
    val emailWhitelistSuffix: Int,
    @SerializedName("is_captcha")
    val isCaptcha: Int,
    @SerializedName("captcha_type")
    val captchaType: String? = null,
    @SerializedName("recaptcha_site_key")
    val recaptchaSiteKey: Boolean = false,
    @SerializedName("recaptcha_v3_site_key")
    val recaptchaV3SiteKey: String? = null,
    @SerializedName("recaptcha_v3_score_threshold")
    val recaptchaV3ScoreThreshold: Double = 0.5,
    @SerializedName("turnstile_site_key")
    val turnstileSiteKey: String? = null,
    @SerializedName("app_description")
    val appDescription: String,
    @SerializedName("app_url")
    val appUrl: String,
    @SerializedName("logo")
    val logo: String,
    @SerializedName("is_recaptcha")
    val isRecaptcha: Int
)

data class UserConfigResponse(
    @SerializedName("is_telegram")
    val isTelegram: Int?,
    @SerializedName("telegram_discuss_link")
    val telegramDiscussLink: Int,
    @SerializedName("stripe_pk")
    val isInviteForce: Int,
    @SerializedName("withdraw_methods")
    val withdrawMethods: List<String>? = null,
    @SerializedName("withdraw_close")
    val withdrawClose: Int,
    @SerializedName("currency")
    val currency: String? = null,
    @SerializedName("currency_symbol")
    val currencySymbol: String? = null,
    @SerializedName("commission_distribution_enable")
    val commissionDistributionEnable: Int,
    @SerializedName("commission_distribution_l1")
    val commissionDistributionL1: String? = null,
    @SerializedName("commission_distribution_l2")
    val commissionDistributionL2: String? = null,
    @SerializedName("commission_distribution_l3")
    val commissionDistributionL3: String? = null,

    )

// ==================== 邀请相关 ====================

/**
 * 邀请信息响应 (user/invite/fetch)
 * 包含邀请码列表和统计数据
 */
data class InviteDetailsResponse(
    val codes: List<InviteCode> = emptyList(),
    val stat: List<Int> = emptyList()
)

/**
 * 邀请码信息
 */
data class InviteCode(
    val id: Int,
    val code: String,
    @SerializedName("user_id")
    val userId: Int,
    val pv: Int = 0,
    @SerializedName("created_at")
    val createdAt: String
)

/**
 * 邀请详情响应 (user/invite/details)
 * 包含邀请用户的详细信息
 */
data class InviteDetailResponse(
    val data: List<InviteDetail> = emptyList(),
    val total: Int = 0
)

/**
 * 邀请用户详情
 */
data class InviteDetail(
    val id: Int,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("invite_user_id")
    val inviteUserId: Int,
    @SerializedName("order_id")
    val orderId: Int,
    @SerializedName("get_amount")
    val getAmount: Double,
    @SerializedName("created_at")
    val createdAt: String
)

data class TransferRequest(
    val amount: Double
)

// ==================== 订单 & 支付 ====================

data class CreateOrderRequest(
    @SerializedName("plan_id")
    val planId: Int,
    val period: String,
    @SerializedName("coupon_code")
    val couponCode: String? = null,
    @SerializedName("payment_method_ids")
    val paymentMethodIds: List<Int>? = null
)

data class OrderResponse(
    @SerializedName("trade_no")
    val tradeNo: String,
    @SerializedName("payable_amount")
    val payableAmount: Double,
    val plan: Plan,
    @SerializedName("period_value")
    val periodValue: Int? = null,
    @SerializedName("period_unit")
    val periodUnit: String? = null
)

data class PaymentMethod(
    val id: Int,
    val name: String,
    @SerializedName("fee_percent")
    val feePercent: Double,
    @SerializedName("plugin_code")
    val pluginCode: String
)

data class CheckoutRequest(
    @SerializedName("trade_no")
    val tradeNo: String,
    val method: Int
)

data class CheckoutResponse(
    val type: Int, // -1=免费订单, 0=二维码, 1=支付链接
    val data: Any?, // 根据type不同，可能是Boolean(true)、String(url)或String(二维码数据)
    @SerializedName("trade_no")
    val tradeNo: String
)

data class OrderStatusResponse(
    val data: Int, // 0=待支付, 1=已支付, 2=已过期, 3=已取消
)

data class OrderDetailResponse(
    val id: Int? = null,
    @SerializedName("invite_user_id")
    val inviteUserId: Int? = null,
    @SerializedName("user_id")
    val userId: Int? = null,
    @SerializedName("plan_id")
    val planId: Int,
    @SerializedName("coupon_id")
    val couponId: Int? = null,
    @SerializedName("payment_id")
    val paymentId: Int? = null,
    val type: Int? = null,
    val period: String? = null,
    @SerializedName("trade_no")
    val tradeNo: String,
    @SerializedName("callback_no")
    val callbackNo: String? = null,
    @SerializedName("total_amount")
    val totalAmount: Double? = null,
    @SerializedName("handling_amount")
    val handlingAmount: Double? = null,
    @SerializedName("discount_amount")
    val discountAmount: Double? = null,
    @SerializedName("surplus_amount")
    val surplusAmount: Double? = null,
    @SerializedName("refund_amount")
    val refundAmount: Double? = null,
    @SerializedName("balance_amount")
    val balanceAmount: Double? = null,
    @SerializedName("surplus_order_ids")
    val surplusOrderIds: IntArray? = null,
    val status: Int,
    @SerializedName("commission_status")
    val commissionStatus: Int? = null,
    @SerializedName("commission_balance")
    val commissionBalance: Double? = null,
    @SerializedName("actual_commission_balance")
    val actualCommissionBalance: Double? = null,
    @SerializedName("paid_at")
    val paidAt: Long? = null,
    @SerializedName("created_at")
    val createdAt: Long,
    @SerializedName("updated_at")
    val updatedAt: Long? = null,
    val plan: Plan? = null,
    val amount: Double = 0.0,
) {
    companion object {
        //0=待支付, 3=已支付, 4=折扣, 2=已取消
        const val STATUS_PAID = 3
        const val STATUS_CANCELED = 2
        const val STATUS_WAITING = 0
        const val STATUS_EXPIRE = 2
        const val STATUS_DISCOUNT = 4
        const val STATUS_LOADING = 1
    }

    fun getStatusText(): String {
        return when (status) {
            STATUS_LOADING -> "开通中"
            STATUS_PAID -> "已完成"
            STATUS_CANCELED -> "已取消"
            STATUS_WAITING -> "待支付"
            STATUS_DISCOUNT -> "已折抵"
            else -> "未知"
        }
    }

    fun getStatusColor(): Int {
        return when (status) {
            STATUS_LOADING, STATUS_PAID -> "#4CAF50".toColorInt()
            STATUS_DISCOUNT, STATUS_CANCELED -> Color.GRAY
            STATUS_WAITING -> "#FF9800".toColorInt()
            STATUS_EXPIRE -> Color.GRAY
            else -> Color.GRAY
        }

    }
}

data class OrderHistoryResponse(
    val data: List<OrderDetailResponse>,
    val total: Int,
    @SerializedName("per_page")
    val perPage: Int,
    val current_page: Int
)

data class CancelOrderRequest(
    @SerializedName("trade_no")
    val tradeNo: String
)

data class CheckCouponRequest(
    @SerializedName("code")
    val couponCode: String,
    @SerializedName("plan_id")
    val planId: Int? = null,
    @SerializedName("period")
    val period: String? = null
)

data class CouponResponse(
    val id: Int,
    val code: String,
    val name: String,
    val type: Int,
    val value: Double,
    val show: Boolean,
    @SerializedName("limit_use")
    val limitUse: Int? = null,
    @SerializedName("limit_use_with_user")
    val limitUseWithUser: Int? = null,
    @SerializedName("limit_plan_ids")
    val limitPlanIds: List<Int>? = null,
    @SerializedName("limit_period")
    val limitPeriod: List<String>? = null,
    @SerializedName("started_at")
    val startedAt: Long,
    @SerializedName("ended_at")
    val endedAt: Long,
    @SerializedName("created_at")
    val createdAt: Long,
    @SerializedName("updated_at")
    val updatedAt: Long
)

data class CheckGiftCardRequest(
    @SerializedName("card_code")
    val cardCode: String
)

data class GiftCardResponse(
    val id: Int,
    val code: String,
    val balance: Double,
    val status: Int
)

data class RedeemGiftCardRequest(
    @SerializedName("card_code")
    val cardCode: String
)

data class GiftCardHistoryResponse(
    val data: List<GiftCardHistory>,
    val total: Int,
    @SerializedName("per_page")
    val perPage: Int,
    val current_page: Int
)

data class GiftCardHistory(
    val id: Int,
    val code: String,
    val balance: Double,
    @SerializedName("redeemed_at")
    val redeemedAt: Long? = null
)

// ==================== 工单 & 客服 ====================

data class CreateTicketRequest(
    val subject: String,
    val message: String,
    val level: Int? = null
)

data class TicketResponse(
    val id: Int,
    val subject: String,
    val status: Int,
    val level: Int? = null,
    @SerializedName("reply_status")
    val replyStatus: Int? = null,
    val message: List<TicketMessageResponse>? = null,
    @SerializedName("created_at")
    val createdAt: Long? = null,
    @SerializedName("updated_at")
    val updatedAt: Long? = null,
    @SerializedName("user_id")
    val userId: Int? = null,
    // 兼容旧字段
    val description: String? = null,
    val replies: List<TicketReplyResponse>? = null
) : Serializable

data class TicketMessageResponse(
    val id: Int,
    @SerializedName("ticket_id")
    val ticketId: Int,
    @SerializedName("is_me")
    val isMe: Boolean,
    val message: String,
    @SerializedName("created_at")
    val createdAt: Long? = null,
    @SerializedName("updated_at")
    val updatedAt: Long? = null
) : Serializable

data class TicketReplyResponse(
    val id: Int,
    val message: String,
    @SerializedName("created_at")
    val createdAt: Long? = null,
    @SerializedName("is_admin")
    val isAdmin: Int = 0
) : Serializable

data class TicketListResponse(
    val data: List<TicketResponse>,
    val total: Int,
    @SerializedName("per_page")
    val perPage: Int,
    val current_page: Int
)

data class ReplyTicketRequest(
    @SerializedName("ticket_id")
    val ticketId: Int,
    val message: String
)

data class CloseTicketRequest(
    @SerializedName("id")
    val ticketId: Int
)

data class NoticeListResponse(
    val data: List<Notice>,
    val total: Int,
    @SerializedName("per_page")
    val perPage: Int,
    val current_page: Int
)

data class Notice(
    val id: Int,
    val title: String,
    val content: String,
    @SerializedName("img_url")
    val imgUrl: String,
    @SerializedName("created_at")
    val createdAt: Long
)

data class KnowledgeCategory(
    val id: Int,
    val name: String,
    val description: String? = null
)

data class KnowledgeArticleResponse(
    val site: List<KnowledgeArticle>
)

data class KnowledgeArticle(
    val id: Int,
    val title: String,
    val body: String,
    @SerializedName("category")
    val category: String,
    @SerializedName("updated_at")
    val updatedAt: Long,
    val show: Boolean,
    var item: KnowledgeItem? = null,
) {
    fun getWebsite(): KnowledgeItem? {
        if (item == null) {
            item = GsonUtil.getGson().fromJson(body, KnowledgeItem::class.java)
        }
        return item
    }
}

data class KnowledgeItem(val url: String? = null, val img: String? = null)