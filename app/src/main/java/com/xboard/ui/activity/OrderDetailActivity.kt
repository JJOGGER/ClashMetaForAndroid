package com.xboard.ui.activity

import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.databinding.ActivityOrderDetailBinding
import com.github.kr328.clash.databinding.BottomSheetPlanDetailBinding
import com.github.kr328.clash.util.withProfile
import com.xboard.api.RetrofitClient
import com.xboard.base.BaseActivity
import com.xboard.ex.gone
import com.xboard.ex.visible
import com.xboard.model.Plan
import com.xboard.network.OrderRepository
import com.xboard.network.UserRepository
import com.xboard.storage.MMKVManager
import com.xboard.ui.adapter.PaymentMethodAdapter
import com.xboard.ui.adapter.PlanFeatureAdapter
import com.xboard.util.AutoSubscriptionManager
import com.xboard.utils.onClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 订单详情页面
 * 如果是未支付订单，进入PaymentActivity
 * 如果是取消和已完成的，展示商品信息和订单信息
 */
class OrderDetailActivity : BaseActivity<ActivityOrderDetailBinding>() {

    private val orderRepository by lazy { OrderRepository(RetrofitClient.getApiService()) }
    private val userRepository by lazy { UserRepository(RetrofitClient.getApiService()) }
    private val autoSubscriptionManager by lazy { 
        AutoSubscriptionManager(this, userRepository, lifecycleScope) 
    }
    private var tradeNo: String? = null
    private var orderStatus: Int = 0
    private lateinit var planFeatureAdapter: PlanFeatureAdapter
    private lateinit var paymentMethodAdapter: PaymentMethodAdapter
    private var paymentMethodId: Int? = null

    companion object {
        const val EXTRA_TRADE_NO = "trade_no"
        const val EXTRA_ORDER_STATUS = "order_status"
    }

    override fun getViewBinding(): ActivityOrderDetailBinding {
        return ActivityOrderDetailBinding.inflate(layoutInflater)
    }

    override fun initView() {
        setupUI()
        tradeNo = intent.getStringExtra(EXTRA_TRADE_NO)
        orderStatus = intent.getIntExtra(EXTRA_ORDER_STATUS, 0)
        paymentMethodAdapter = PaymentMethodAdapter { method ->
            paymentMethodId = method.id
        }
        binding.rvPaymentMethods.apply {
            layoutManager = LinearLayoutManager(this@OrderDetailActivity)
            adapter = paymentMethodAdapter
        }
    }

    private fun loadPaymentMethods() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getApiService().getPaymentMethods()

                if (response.isSuccess() && response.data != null) {
                    paymentMethodAdapter.updateData(response.data)
                } else {
                    showError("加载支付方式失败")
                }
            } catch (e: Exception) {
                showError("加载失败: ${e.message}")
            }
        }
    }

    override fun initData() {
        if (tradeNo == null) {
            showError("订单信息错误")
            finish()
            return
        }
        loadOrderDetail()
    }

    private fun setupUI() {
        binding.vBack.setOnClickListener {
            finish()
        }
        binding.btnCancel.onClick {
            //取消订单
            cancelOrder()
        }
        planFeatureAdapter = PlanFeatureAdapter()
        binding.btnCheckout.setOnClickListener {
            startPayment()
            // 进入支付页面
//            if (tradeNo != null) {
//                val intent = Intent(this, PaymentActivity::class.java)
//                intent.putExtra(PaymentActivity.EXTRA_TRADE_NO, tradeNo)
//                startActivity(intent)
//            }
        }
    }

    private fun startPayment() {
        if (tradeNo == null || paymentMethodId == null) {
            showError("参数错误")
            return
        }

//        binding.progressBar.visibility = View.VISIBLE
//        binding.tvStatus.text = "正在发起支付..."
        showLoading("正在发起支付...")
        lifecycleScope.launch {
            val checkoutData = orderRepository.checkout(tradeNo!!, paymentMethodId!!)
            hideLoading()
            when (checkoutData?.type) {
//                        1 -> {
//                            // 打开URL
//                            binding.webView.visibility = View.VISIBLE
//                            binding.tvStatus.visibility = View.GONE
//                            binding.progressBar.visibility = View.GONE
//                            binding.webView.loadUrl(checkoutData.data)
//                            startPollingPaymentStatus()
//                        }
//                        0 -> {
//                            // 二维码
//                            binding.progressBar.visibility = View.GONE
//                            binding.tvStatus.text = "请扫描二维码完成支付\n二维码: ${checkoutData.data}"
//                            startPollingPaymentStatus()
//                        }
                -1 -> {
                    // 免费订单，直接成功
                    showSuccess("支付成功")
                    // 支付成功，更新订阅配置
                    updateSubscribeUrlAfterPayment()
                }
//                        else -> {
//                            binding.progressBar.visibility = View.GONE
//                            Toast.makeText(this@PaymentActivity, "未知的支付类型", Toast.LENGTH_SHORT).show()
//                            finish()
//                        }
            }
        }
    }

    /**
     * 支付成功后自动导入和应用订阅
     * 
     * 使用 AutoSubscriptionManager 完成整个自动化流程：
     * 1. 获取或创建 Profile
     * 2. 自动更新配置（导入）
     * 3. 自动选中 Profile（应用）
     * 
     * 用户可以手动点击开始连接来启动 VPN
     */
    private fun updateSubscribeUrlAfterPayment() {
        lifecycleScope.launch {
            try {
                // 自动导入和应用订阅
                val success = autoSubscriptionManager.autoImportAndApply()
                
                if (success) {
                    // 显示成功提示
                    Log.e("TAG","订阅已自动导入和应用，请点击开始连接")
                } else {
                    // 显示失败提示
                    Log.e("TAG","订阅导入失败，请稍后重试")
                }
                
                // 延迟后返回
                delay(2000)
                setResult(RESULT_OK)
                finish()
            } catch (e: Exception) {
                // 异常处理，继续返回
                Log.e("TAG","处理支付结果失败")
                delay(2000)
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    private fun cancelOrder() {
        if (tradeNo == null) {
            return
        }
        lifecycleScope.launch {
            val result = orderRepository.cancelOrder(tradeNo!!)

            result
                .onSuccess { order ->
                    loadOrderDetail()
                }
                .onError { error ->
                    showError("加载订单详情失败: ${error.message}")
                }
        }

    }

    private fun loadOrderDetail() {
        lifecycleScope.launch {
            val result = orderRepository.getOrderDetail(tradeNo!!)

            result
                .onSuccess { order ->
                    if (order.status == 0) {
                        binding.vPayContainer.visible()
                        binding.vComplatedContainer.gone()
                    } else {
                        binding.vPayContainer.gone()
                        binding.vComplatedContainer.visible()
                    }
                    displayOrderDetail(order)
                }
                .onError { error ->
                    showError("加载订单详情失败: ${error.message}")
                }
        }
    }

    private fun displayOrderDetail(order: com.xboard.model.OrderDetailResponse) {
        // 显示状态和原因（头部）
        val statusText = when (order.status) {
            0 -> "待支付"
            1 -> "已完成"
            2 -> "已取消"
            else -> "未知状态"
        }

        // 更新两个容器中的状态显示
        binding.tvOrderStatus.text = statusText
        binding.tvOrderStatus.setTextColor(
            when (order.status) {
                0 -> android.graphics.Color.parseColor("#FF9800") // 橙色 - 待支付
                1 -> android.graphics.Color.parseColor("#4CAF50") // 绿色 - 已完成
                -1 -> android.graphics.Color.parseColor("#F44336") // 红色 - 已取消
                else -> android.graphics.Color.GRAY
            }
        )

        // 显示订单基本信息（两个容器都有）
        binding.tvTradeNo.text = order.tradeNo
        binding.tvCreatedAt.text = formatTime(order.createdAt)
        binding.tvPaidAt.visibility =
            if (order.status != 0) android.view.View.VISIBLE else android.view.View.GONE
        if (order.paidAt != null && order.paidAt > 0) {
            binding.tvPaidAt.text = "支付时间: ${formatTime(order.paidAt)}"
        }

        // 填充待支付容器的数据
        if (order.plan != null) {
            // 商品信息
            binding.tvPlanName.text = order.plan.name
            binding.tvPlanNameResult.text = order.plan.name
            binding.tvPlanPeriod.text = getPeriodLabel(order.period)
            if (order.plan.transferEnable >= Integer.MAX_VALUE) {
                binding.tvPlanTraffic.text = " 无限制"
            } else {
                binding.tvPlanTraffic.text = " ${order.plan.transferEnable} GB"
            }
            // 订单信息
            binding.tvOrderId.text = order.tradeNo
            binding.tvOrderTime.text = formatTime(order.createdAt)

            // 价格信息
            binding.tvTotalTitle.text =
                "¥${String.format("%.2f", order.totalAmount ?: order.payableAmount)}"
            binding.tvTotalPrice.text = "¥${String.format("%.2f", order.payableAmount)}"

            // 已完成容器的数据
            binding.tvPlanName2.text = order.plan.name
            binding.tvPlanPeriod2.text = getPeriodLabel(order.period)
            if (order.plan.transferEnable >= Integer.MAX_VALUE) {
                binding.tvPlanTraffic2.text = " 无限制"
            } else {
                binding.tvPlanTraffic2.text = " ${order.plan.transferEnable} GB"
            }

            binding.tvOrderId2.text = order.tradeNo
            binding.tvOrderTime2.text = formatTime(order.createdAt)

            binding.tvTotalTitle2.text =
                "¥${String.format("%.2f", order.totalAmount ?: order.payableAmount)}"
            binding.tvTotalPrice2.text = "¥${String.format("%.2f", order.payableAmount)}"
        }

        // 加载支付方式（仅待支付状态）
        if (order.status == 0) {
            loadPaymentMethods()
        }
    }

    private fun getPeriodLabel(period: String?): String = when (period) {
        "month_price", "month" -> "月付"
        "quarter_price", "quarter" -> "季付"
        "half_year_price", "half_year" -> "半年付"
        "year_price", "year" -> "年付"
        else -> "灵活周期"
    }

    private fun bindPlanDetail(binding: BottomSheetPlanDetailBinding, plan: Plan) {
//        // Setup features RecyclerView
//        binding.rvPlanFeatures.apply {
//            layoutManager = LinearLayoutManager(context)
//            adapter = planFeatureAdapter
//        }
//        binding.tvPlanTitle.text = plan.name
//        binding.tvPlanPrice.text = "¥ ${formatPrice(plan.price)}"
//        if (plan.onetimePrice != null && plan.onetimePrice != 0.0) {
//            binding.tvPlanPrice.text = "¥ ${formatPrice(plan.onetimePrice)}"
//            binding.tvPriceType.text = "一次性"
//        } else if (plan.monthPrice != null && plan.monthPrice != 0.0) {
//            binding.tvPlanPrice.text = "¥ ${formatPrice(plan.monthPrice)}"
//            binding.tvPriceType.text = "月付"
//        }
//        if (plan.transferEnable >= Integer.MAX_VALUE) {
//            binding.tvPlanTraffic.text = " 无限制"
//        } else {
//            binding.tvPlanTraffic.text = " ${plan.transferEnable} GB"
//        }
//        // Setup features RecyclerView
//        binding.rvPlanFeatures.apply {
//            layoutManager = LinearLayoutManager(context)
//            adapter = planFeatureAdapter
//        }
//
//        // Update features list
//        val features = buildFeatureList(plan)
//        planFeatureAdapter.updateFeatures(features)
    }

    private fun formatTime(timestamp: Long): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            sdf.format(Date(timestamp * 1000))
        } catch (e: Exception) {
            "未知时间"
        }
    }
}

