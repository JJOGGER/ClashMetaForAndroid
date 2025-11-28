package com.xboard.ui.activity

import android.app.AlertDialog
import android.content.Intent
import android.view.View
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.databinding.ActivityOrderBinding
import com.github.kr328.clash.databinding.BottomSheetPlanDetailBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.xboard.api.RetrofitClient
import com.xboard.base.BaseActivity
import com.xboard.model.Plan
import com.xboard.network.OrderRepository
import com.xboard.ui.adapter.PlanFeatureAdapter
import com.xboard.storage.MMKVManager
import com.xboard.utils.onClick
import kotlinx.coroutines.launch
import java.text.DecimalFormat

/**
 * 订单创建和支付页面
 */
class OrderActivity : BaseActivity<ActivityOrderBinding>() {

    private val orderRepository by lazy { OrderRepository(RetrofitClient.getApiService()) }
    private var selectedPlan: Plan? = null
    private var selectedPeriod: String? = null
    private var planDetailDialog: BottomSheetDialog? = null
    private lateinit var planFeatureAdapter: PlanFeatureAdapter

    companion object {
        const val EXTRA_PLAN = "plan"
        private const val REQUEST_CODE_PAYMENT = 1001
    }

    override fun getViewBinding(): ActivityOrderBinding {
        return ActivityOrderBinding.inflate(layoutInflater)
    }

    private val priceFormatter = DecimalFormat("#.##")

    override fun initView() {
        selectedPlan = intent.getSerializableExtra(EXTRA_PLAN) as? Plan?
        setupUI()
    }

    override fun initData() {
        loadPaymentMethods()
    }

    private fun setupUI() {
        binding.vBack.setOnClickListener {
            finish()
        }
        binding.vPlanDetail.onClick {
            showPlanDetail(plan = selectedPlan)
        }
        // 显示套餐信息
        selectedPlan?.let { plan ->
            binding.tvPlanName.text = plan.name
            binding.tvTotalPrice.text = "¥${formatPrice(plan.price)}"
            if (plan.transferEnable >= Integer.MAX_VALUE) {
                binding.tvPlanTraffic.text = " 无限制"
            } else {
                binding.tvPlanTraffic.text = " ${plan.transferEnable} GB"
            }
        }

        planFeatureAdapter = PlanFeatureAdapter()
        // 设置周期选择
        setupPeriodSelection()


        // 优惠券输入
        binding.btnCheckCoupon.setOnClickListener {
            checkCoupon()
        }

        // 创建订单
        binding.btnCheckout.setOnClickListener {
            if (selectedPeriod == null) {
                showError("请选择付款周期")
                return@setOnClickListener
            }
            createOrder()
        }
    }

    private fun showPlanDetail(plan: Plan?) {
        plan ?: return
        planDetailDialog?.dismiss()

        val sheetBinding = BottomSheetPlanDetailBinding.inflate(layoutInflater)
        planDetailDialog = BottomSheetDialog(this).apply {
            setContentView(sheetBinding.root)
            behavior.apply {
                isFitToContents = true
                peekHeight = (resources.displayMetrics.heightPixels * 0.55f).toInt()
            }
            setOnDismissListener { planDetailDialog = null }
        }

        bindPlanDetail(sheetBinding, plan)

        sheetBinding.btnClose.setOnClickListener { planDetailDialog?.dismiss() }
        sheetBinding.btnPurchase.setOnClickListener {
            planDetailDialog?.dismiss()
        }

        planDetailDialog?.show()
    }

    private fun bindPlanDetail(binding: BottomSheetPlanDetailBinding, plan: Plan) {
        // Setup features RecyclerView
        binding.rvPlanFeatures.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = planFeatureAdapter
        }
        binding.tvPlanTitle.text = plan.name
        binding.tvPlanPrice.text = "¥ ${formatPrice(plan.price)}"
        if (plan.onetimePrice != null && plan.onetimePrice != 0.0) {
            binding.tvPlanPrice.text = "¥ ${formatPrice(plan.onetimePrice)}"
            binding.tvPriceType.text = "一次性"
        } else if (plan.monthPrice != null && plan.monthPrice != 0.0) {
            binding.tvPlanPrice.text = "¥ ${formatPrice(plan.monthPrice)}"
            binding.tvPriceType.text = "月付"
        }
        if (plan.transferEnable >= Integer.MAX_VALUE) {
            binding.tvPlanTraffic.text = " 无限制"
        } else {
            binding.tvPlanTraffic.text = " ${plan.transferEnable} GB"
        }
        // Setup features RecyclerView
        binding.rvPlanFeatures.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = planFeatureAdapter
        }

        // Update features list
        val features = buildFeatureList(plan)
        planFeatureAdapter.updateFeatures(features)
    }

    private fun buildFeatureList(plan: Plan): List<String> {
        // Parse the plan description, handling line breaks
        val parsed = plan.description
            ?.split('\n')
            ?.flatMap { it.split('、', '，') }
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

        return (parsed).distinct()
    }

    private fun setupPeriodSelection() {
        selectedPlan?.let { plan ->
            val periods = buildPeriodOptions(plan)
            binding.containerPeriodOptions.removeAllViews()

            // Create rows for period options (2 columns per row)
            var currentRow: LinearLayout? = null
            periods.forEachIndexed { index, period ->
                if (index % 3 == 0) {
                    currentRow = LinearLayout(this).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 0, 0, 12)
                        }
                        orientation = LinearLayout.HORIZONTAL
                    }
                    binding.containerPeriodOptions.addView(currentRow)
                }

                val periodView = createPeriodOptionView(index, period)

                currentRow?.addView(periodView)
            }
        }
    }

    private fun buildPeriodOptions(plan: Plan): List<PeriodOption> {
        val options = mutableListOf<PeriodOption>()
        // Month
        if (plan.monthPrice != null && plan.monthPrice != 0.0) {
            options.add(PeriodOption("month_price", "月付", plan.monthPrice!!))
        }

        // Quarter
        if (plan.quarterPrice != null && plan.quarterPrice != 0.0) {
            options.add(PeriodOption("quarter_price", "季付", plan.quarterPrice!!))
        }

        // Half Year
        if (plan.halfYearPrice != null && plan.halfYearPrice != 0.0) {
            options.add(PeriodOption("half_year_price", "半年付", plan.halfYearPrice!!))
        }

        // Year
        if (plan.yearPrice != null && plan.yearPrice != 0.0) {
            options.add(PeriodOption("year_price", "年付", plan.yearPrice!!))
        }

        // Two Year
        if (plan.twoYearPrice != null && plan.twoYearPrice != 0.0) {
            options.add(PeriodOption("two_year_price", "两年付", plan.twoYearPrice!!))
        }

        // Three Year
        if (plan.threeYearPrice != null && plan.threeYearPrice != 0.0) {
            options.add(PeriodOption("three_year_price", "三年付", plan.threeYearPrice!!))
        }
        // One Time
        if (options.isEmpty()) {
            options.add(PeriodOption("onetime_price", "一次性", plan.onetimePrice ?: 0.0))
        }

        return options
    }

    private fun createPeriodOptionView(index: Int, period: PeriodOption): android.view.View {
        val inflater = android.view.LayoutInflater.from(this)
        val view = inflater.inflate(com.github.kr328.clash.R.layout.item_period_option, null)
        val binding = com.github.kr328.clash.databinding.ItemPeriodOptionBinding.bind(view)

        binding.tvPeriodLabel.text = period.label
        binding.tvPeriodPrice.text = "¥ ${formatPrice(period.price)}"

        // Calculate width: (screenWidth - totalMargin) / 3
        val screenWidth = resources.displayMetrics.widthPixels
        val totalMargin =
            (16 * 5 + 8 * 2).dpToPx()  // 16dp padding on sides + 8dp margin between items
        val cardWidth = (screenWidth - totalMargin) / 3

        val layoutParams =
            LinearLayout.LayoutParams(cardWidth, 80.dpToPx())
        layoutParams.setMargins(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
        view.layoutParams = layoutParams
        view.setOnClickListener {
            selectPeriod(period, binding)
        }
        if (index == 0) {
            selectPeriod(period, binding)
        }

        view.tag = period
        return view
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun selectPeriod(
        period: PeriodOption,
        binding: com.github.kr328.clash.databinding.ItemPeriodOptionBinding
    ) {
        // Deselect all other periods in the container
        for (rowIndex in 0 until this.binding.containerPeriodOptions.childCount) {
            val row =
                this.binding.containerPeriodOptions.getChildAt(rowIndex) as? android.view.ViewGroup
            row?.let {
                for (colIndex in 0 until it.childCount) {
                    val child = it.getChildAt(colIndex)
                    if (child != null) {
                        val childBinding =
                            com.github.kr328.clash.databinding.ItemPeriodOptionBinding.bind(child)
                        childBinding.vContainer.isSelected = false
                    }
                }
            }
        }

        // Select current period
        selectedPeriod = period.periodKey
        binding.vContainer.isSelected = true
        updateOrderSummary()
    }

    private fun updateOrderSummary() {
        selectedPlan?.let { plan ->
            selectedPeriod?.let { period ->
                val price = when (period) {
                    "month_price" -> plan.monthPrice ?: plan.price
                    "quarter_price" -> plan.quarterPrice ?: plan.price
                    "half_year_price" -> plan.halfYearPrice ?: plan.price
                    "year_price" -> plan.yearPrice ?: plan.price
                    "two_year_price" -> plan.twoYearPrice ?: plan.price
                    "three_year_price" -> plan.threeYearPrice ?: plan.price
                    "onetime_price" -> plan.onetimePrice ?: plan.price
                    else -> plan.price
                }
                binding.tvTotalPrice.text = "¥${formatPrice(price)}"
            }
        }
    }

    data class PeriodOption(
        val periodKey: String,
        val label: String,
        val price: Double
    )

    private fun checkCoupon() {
        val couponCode = binding.etCouponCode.text.toString().trim()
        if (couponCode.isEmpty()) {
            showError("请输入优惠券代码")
            return
        }

        binding.btnCheckCoupon.isEnabled = false

        lifecycleScope.launch {
            val planRepository = com.xboard.network.PlanRepository(RetrofitClient.getApiService())
            val result = planRepository.checkCoupon(couponCode, selectedPlan?.id)

            result
                .onSuccess { coupon ->
                    binding.tvCouponDiscount.text = "优惠: ¥${formatPrice(coupon.discount)}"
                    binding.tvCouponDiscount.visibility = View.VISIBLE
                    showSuccess("优惠券有效")
                }
                .onError { error ->
                    showError(error.message ?: "优惠券无效")
                }

            binding.btnCheckCoupon.isEnabled = true
        }
    }

    private fun loadPaymentMethods() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getApiService().getPaymentMethods()

                if (response.isSuccess() && response.data != null) {
//                    paymentMethodAdapter.updateData(response.data)
                } else {
                    showError("加载支付方式失败")
                }
            } catch (e: Exception) {
                showError("加载失败: ${e.message}")
            }
        }
    }

    private fun createOrder() {
        if (selectedPeriod == null) {
            showError("请选择订阅周期")
            return
        }

        if (shouldWarnSubscriptionChange()) {
            showSubscriptionOverwriteDialog {
                performCreateOrder()
            }
        } else {
            performCreateOrder()
        }
    }

    private fun shouldWarnSubscriptionChange(): Boolean {
        val subscription = MMKVManager.getSubscribe()
        return subscription != null && subscription.planId > 0
    }

    private fun showSubscriptionOverwriteDialog(onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setTitle("注意")
            .setMessage("请注意，变更订阅会导致当前订阅被覆盖。")
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("确定") { dialog, _ ->
                dialog.dismiss()
                onConfirm()
            }
            .create()
            .show()
    }

    private fun performCreateOrder() {
        lifecycleScope.launch {
            showLoading("创建订单中...")
            val result = orderRepository.createOrder(
                planId = selectedPlan!!.id,
                period = selectedPeriod!!,
                couponCode = binding.etCouponCode.text.toString().trim().takeIf { it.isNotEmpty() }
            )
            hideLoading()

            result
                .onSuccess { tradeNo ->
                    val intent = Intent(this@OrderActivity, OrderDetailActivity::class.java)
                    intent.putExtra(PaymentActivity.EXTRA_TRADE_NO, tradeNo)
                    startActivityForResult(intent, REQUEST_CODE_PAYMENT)
                }
                .onError { error ->
                    showError(error.message ?: "创建订单失败")
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PAYMENT && resultCode == RESULT_OK) {
            showSuccess("支付成功")
            finish()
        }
    }


    private fun formatPrice(amount: Double): String {
        // Round to avoid floating point precision issues
        val rounded = amount / 100.0
        return priceFormatter.format(rounded)
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
}
