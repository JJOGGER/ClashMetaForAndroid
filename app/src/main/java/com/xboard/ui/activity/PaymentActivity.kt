package com.xboard.ui.activity

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.databinding.ActivityPaymentBinding
import com.xboard.api.RetrofitClient
import com.xboard.base.BaseActivity
import com.xboard.network.OrderRepository
import com.xboard.network.UserRepository
import com.xboard.ui.adapter.PaymentMethodAdapter
import com.xboard.util.AutoSubscriptionManager
import com.github.kr328.clash.util.withProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

/**
 * 支付页面
 */
class PaymentActivity : BaseActivity<ActivityPaymentBinding>() {

    private val orderRepository by lazy { OrderRepository(RetrofitClient.getApiService()) }
    private val userRepository by lazy { UserRepository(RetrofitClient.getApiService()) }
    private val autoSubscriptionManager by lazy { 
        AutoSubscriptionManager(this, userRepository, lifecycleScope) 
    }
    private var tradeNo: String? = null
    private var paymentMethodId: Int? = null
    private var isPolling = false
    private lateinit var paymentMethodAdapter: PaymentMethodAdapter

    companion object {
        const val EXTRA_TRADE_NO = "trade_no"
        const val EXTRA_AMOUNT = "amount"
    }

    override fun initView() {
        setupUI()
        tradeNo = intent.getStringExtra(EXTRA_TRADE_NO)
    }

    override fun initData() {
        loadPaymentMethods()
    }

    override fun getViewBinding(): ActivityPaymentBinding {
        return ActivityPaymentBinding.inflate(layoutInflater)
    }

    private fun setupUI() {
        binding.vBack.setOnClickListener {
            stopPolling()
            finish()
        }

        binding.btnCancel.setOnClickListener {
            stopPolling()
            finish()
        }

        // 支付方式列表
        paymentMethodAdapter = PaymentMethodAdapter { method ->
//            binding.btnCheckout.isEnabled = true
            paymentMethodId = method.id
        }
        binding.btnCheckout.setOnClickListener {
            startPayment()
            // 进入支付页面
//            if (tradeNo != null) {
//                val intent = Intent(this, PaymentActivity::class.java)
//                intent.putExtra(PaymentActivity.EXTRA_TRADE_NO, tradeNo)
//                startActivity(intent)
//            }
        }
        binding.rvPaymentMethods.apply {
            layoutManager = LinearLayoutManager(this@PaymentActivity)
            adapter = paymentMethodAdapter
        }
//        // 配置WebView
//        binding.webView.apply {
//            settings.apply {
//                javaScriptEnabled = true
//                domStorageEnabled = true
//                databaseEnabled = true
//            }
//            webViewClient = WebViewClient()
//        }
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

    private fun startPollingPaymentStatus() {
        if (isPolling || tradeNo == null) return
        isPolling = true

        lifecycleScope.launch {
            var attempts = 0
            val maxAttempts = 120 // 2分钟（每秒检查一次）

            while (isPolling && attempts < maxAttempts) {
                try {
                    val result = orderRepository.checkOrderStatus(tradeNo!!)

                    result
                        .onSuccess { orderStatus ->
//                            when (orderStatus.status) {
//                                1 -> {
//                                    // 已支付
//                                    binding.tvStatus.text = "支付成功！"
//                                    Toast.makeText(this@PaymentActivity, "支付成功", Toast.LENGTH_SHORT).show()
//                                    stopPolling()
//                                    // 延迟后返回
//                                    delay(2000)
//                                    setResult(RESULT_OK)
//                                    finish()
//                                    return@launch
//                                }
//                                -1 -> {
//                                    // 已取消
//                                    binding.tvStatus.text = "支付已取消"
//                                    Toast.makeText(this@PaymentActivity, "支付已取消", Toast.LENGTH_SHORT).show()
//                                    stopPolling()
//                                    finish()
//                                    return@launch
//                                }
//                                0 -> {
//                                    // 待支付，继续轮询
//                                    binding.tvStatus.text = "等待支付... (${attempts}s)"
//                                }
//                            }
                        }
                        .onError { error ->
                            // 继续轮询
                        }

                    attempts++
                    delay(1000) // 每秒检查一次
                } catch (e: Exception) {
                    // 继续轮询
                    attempts++
                    delay(1000)
                }
            }

            // 轮询超时
            if (isPolling) {
//                binding.tvStatus.text = "支付超时，请确认支付结果或联系客服"
//                Toast.makeText(this@PaymentActivity, "支付超时", Toast.LENGTH_SHORT).show()
//                stopPolling()
            }
        }
    }

    private fun stopPolling() {
        isPolling = false
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
                    Log.e("TAG","订阅导入失败，请稍后重试")
                    // 显示失败提示
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

    override fun onDestroy() {
        super.onDestroy()
        stopPolling()
    }
}
