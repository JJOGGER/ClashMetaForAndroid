package com.xboard.api

import com.xboard.model.*
import retrofit2.http.*

/**
 * XBoard API 服务接口
 * Base URL: https://{DOMAIN}/api/v1
 */
interface ApiService {

    // ==================== 访客接口 ====================

    /**
     * 获取套餐列表（前台/登录页）
     */
    @GET("guest/plan/fetch")
    suspend fun getGuestPlans(): ApiResponse<GuestPlanResponse>

    /**
     * 获取公共配置
     */
    @GET("guest/comm/config")
    suspend fun getGuestConfig(): ApiResponse<CommConfigResponse>
    @GET("user/comm/config")
    suspend fun getUserConfig(): ApiResponse<UserConfigResponse>

    // ==================== 认证接口 ====================

    /**
     * 用户注册
     */
    @POST("passport/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<LoginResponse>

    /**
     * 用户登录
     */
    @POST("passport/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>

    /**
     * 发送邮件验证码
     */
    @POST("passport/comm/sendEmailVerify")
    suspend fun sendEmailVerify(@Body request: SendEmailVerifyRequest): ApiResponse<Boolean>

    /**
     * 忘记密码
     */
    @POST("passport/auth/forget")
    suspend fun forgetPassword(@Body request: ForgetPasswordRequest): ApiResponse<Boolean>

    // ==================== 用户模块 ====================

    /**
     * 获取用户信息
     */
    @GET("user/info")
    suspend fun getUserInfo(): ApiResponse<UserInfo>

    /**
     * 获取用户套餐列表（含个人折扣）
     */
    @GET("user/plan/fetch")
    suspend fun getUserPlans(): ApiResponse<List<Plan>>

    /**
     * 获取用户订阅的套餐
     */
    @GET("user/plan/fetch")
    suspend fun getUserPlan(): ApiResponse<Plan>

    /**
     * 获取当期用量
     */
    @GET("user/getStat")
    suspend fun getUserStat(): ApiResponse<List<Long>>

    /**
     * 获取流量日志
     */
    @GET("user/stat/getTrafficLog")
    suspend fun getTrafficLog(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): ApiResponse<List<TrafficLog>?>

//    /**
//     * 获取用户可用的服务器列表
//     */
//    @GET("user/server/fetch")
//    suspend fun getUserServers(): ApiResponse<List<Server>?>
    @GET("guest/server/fetch")
    suspend fun getUserServers(): ApiResponse<List<Server>?>

    /**
     * 根据套餐分组获取节点
     */
    @GET("user/server/fetchByGroup")
    suspend fun getServersByGroup(
        @Query("group_id") groupId: Int
    ): ApiResponse<List<ServerGroupNode>>
    /**
     * 获取订阅链接
     */
    @GET("user/getSubscribe")
    suspend fun getSubscribe(): ApiResponse<SubscribeResponse>

    /**
     * 获取订阅配置内容（纯文本）
     * 
     * 注意：订阅URL返回的是纯文本YAML格式，不是JSON格式的ApiResponse
     * 所以这个方法返回 ResponseBody，需要手动处理
     * 
     * @param subscribeUrl 完整的订阅URL，例如：https://example.com/api/v1/client/subscribe?token=xxx
     * @return 配置内容（YAML格式）
     */
    @GET
    suspend fun getSubscribeConfig(@Url subscribeUrl: String): okhttp3.ResponseBody

    /**
     * 重置订阅链接
     */
    @GET("user/resetSecurity")
    suspend fun resetSecurity(): ApiResponse<Unit>

    /**
     * 更新用户信息
     */
    @POST("user/update")
    suspend fun updateUserInfo(@Body request: UpdateUserRequest): ApiResponse<Unit>

    /**
     * 更新用户信息（通用）
     */
    @POST("user/update")
    suspend fun updateUserInfo(@Body params: Map<String, Any>): ApiResponse<UserInfo>

    /**
     * 修改密码
     */
    @POST("user/changePassword")
    suspend fun changePassword(@Body request: ChangePasswordRequest): ApiResponse<Unit>

    /**
     * 修改密码（通用）
     */
    @POST("user/changePassword")
    suspend fun changePassword(@Body params: Map<String, String>): ApiResponse<Unit>

    /**
     * 获取邀请信息（邀请码列表和统计数据）
     */
    @GET("user/invite/fetch")
    suspend fun getInviteInfo(): ApiResponse<InviteDetailsResponse>

    /**
     * 获取邀请详情（邀请用户的详细信息）
     */
    @GET("user/invite/details")
    suspend fun getInviteDetails(
        @Query("current") current: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): InviteDetailResponse?

    /**
     * 生成邀请码
     */
    @GET("user/invite/save")
    suspend fun generateInviteCode(): ApiResponse<Boolean>

    /**
     * 返利划转到余额
     */
    @POST("user/transfer")
    suspend fun transferCommission(@Body request: TransferRequest): ApiResponse<Unit>

    // ==================== 订单 & 支付 ====================

    /**
     * 创建订单
     */
    @POST("user/order/save")
    suspend fun createOrder(@Body request: CreateOrderRequest): ApiResponse<String?>

    /**
     * 获取支付方式列表
     */
    @GET("user/order/getPaymentMethod")
    suspend fun getPaymentMethods(): ApiResponse<List<PaymentMethod>>

    /**
     * 发起支付
     */
    @POST("user/order/checkout")
    suspend fun checkout(@Body request: CheckoutRequest): OrderPay<Any?>?

    /**
     * 检查支付状态
     */
    @GET("user/order/check")
    suspend fun checkOrderStatus(@Query("trade_no") tradeNo: String): ApiResponse<OrderStatusResponse>

    /**
     * 获取订单详情
     */
    @GET("user/order/detail")
    suspend fun getOrderDetail(@Query("trade_no") tradeNo: String): ApiResponse<OrderDetailResponse>

    /**
     * 获取订单历史
     */
    @GET("user/order/fetch")
    suspend fun getOrderHistory(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): ApiResponse<List<OrderDetailResponse>>

    /**
     * 取消订单
     */
    @POST("user/order/cancel")
    suspend fun cancelOrder(@Body request: CancelOrderRequest): ApiResponse<Any>

    /**
     * 检查优惠券
     */
    @POST("user/coupon/check")
    suspend fun checkCoupon(@Body request: CheckCouponRequest): ApiResponse<CouponResponse>

    /**
     * 检查礼品卡
     */
    @POST("user/gift-card/check")
    suspend fun checkGiftCard(@Body request: CheckGiftCardRequest): ApiResponse<GiftCardResponse>

    /**
     * 兑换礼品卡
     */
    @POST("user/gift-card/redeem")
    suspend fun redeemGiftCard(@Body request: RedeemGiftCardRequest): ApiResponse<Unit>

    /**
     * 获取礼品卡历史
     */
    @GET("user/gift-card/history")
    suspend fun getGiftCardHistory(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): ApiResponse<GiftCardHistoryResponse>

    // ==================== 工单 & 客服 ====================

    /**
     * 创建工单
     */
    @POST("user/ticket/save")
    suspend fun createTicket(@Body request: CreateTicketRequest): ApiResponse<TicketResponse>

    /**
     * 获取工单列表
     */
    @GET("user/ticket/fetch")
    suspend fun getTickets(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): ApiResponse<List<TicketResponse>?>

    /**
     * 回复工单
     */
    @POST("user/ticket/reply")
    suspend fun replyTicket(@Body request: ReplyTicketRequest): ApiResponse<Any>

    /**
     * 关闭工单
     */
    @POST("user/ticket/close")
    suspend fun closeTicket(@Body request: CloseTicketRequest): ApiResponse<Any>

    /**
     * 获取工单详情
     */
    @GET("user/ticket/fetch")
    suspend fun getTicketDetail(@Query("id") ticketId: Int): ApiResponse<TicketResponse>

    /**
     * 获取公告列表
     */
    @GET("user/notice/fetch")
    suspend fun getNotices(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): NoticeListResponse?

    /**
     * 获取知识库分类
     */
    @GET("user/knowledge/getCategory")
    suspend fun getKnowledgeCategories(): ApiResponse<List<KnowledgeCategory>>

    /**
     * 获取知识库文章
     */
    @GET("user/knowledge/fetch")
    suspend fun getKnowledgeArticles(
        @Query("category_id") categoryId: Int? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): ApiResponse<KnowledgeArticleResponse>

    /**
     * 用户登出
     */
    @POST("passport/auth/logout")
    suspend fun logout(): ApiResponse<Boolean>
}
