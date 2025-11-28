package com.xboard.ui.fragment

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.databinding.BottomSheetPlanDetailBinding
import com.github.kr328.clash.databinding.FragmentBuyBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.xboard.api.RetrofitClient
import com.xboard.base.BaseFragment
import com.xboard.model.Plan
import com.xboard.network.PlanRepository
import com.xboard.ui.activity.OrderActivity
import com.xboard.ui.adapter.PlanAdapter
import com.xboard.ui.adapter.PlanFeatureAdapter
import kotlinx.coroutines.launch
import java.text.DecimalFormat

/**
 * 购买页面
 */
class BuyFragment : BaseFragment<FragmentBuyBinding>() {

    private val planRepository by lazy { PlanRepository(RetrofitClient.getApiService()) }
    private lateinit var planAdapter: PlanAdapter
    private var planDetailDialog: BottomSheetDialog? = null
    private val priceFormatter by lazy { DecimalFormat("#,##0.00") }
    private lateinit var planFeatureAdapter: PlanFeatureAdapter

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentBuyBinding {
        return FragmentBuyBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        planAdapter = PlanAdapter { plan ->
            showPlanDetail(plan)
        }
        binding.rvPlans.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = planAdapter
        }

        planFeatureAdapter = PlanFeatureAdapter()
    }

    override fun initListener() {
//capacity_limit
//:
//1
//content
//:
//""
//created_at
//:
//1764265489
//device_limit
//:
//1
//group_id
//:
//2
//half_year_price
//:
//0
//id
//:
//2
//month_price
//:
//0
//name
//:
//"Vip"
//onetime_price
//:
//0
//quarter_price
//:
//0
//renew
//:
//true
//reset_price
//:
//0
//reset_traffic_method
//:
//null
//sell
//:
//true
//show
//:
//true
//sort
//:
//null
//speed_limit
//:
//1111
//tags
//:
//[]
//three_year_price
//:
//0
//transfer_enable
//:
//10
//two_year_price
//:
//0
//updated_at
//:
//1764265492
//year_price
//:
//0
    }

    override fun initData() {
        loadData()
    }

    private fun loadData() {

        viewLifecycleOwner.lifecycleScope.launch {
            val result = planRepository.getAllPlans()

            result
                .onSuccess { plans ->
                    planAdapter.updateData(plans)
                }
                .onError { error ->
                    showError(error.message)
                }

        }
    }


    private fun showPlanDetail(plan: Plan) {
        planDetailDialog?.dismiss()

        val sheetBinding = BottomSheetPlanDetailBinding.inflate(layoutInflater)
        planDetailDialog = BottomSheetDialog(requireContext()).apply {
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
            navigateToOrder(plan)
        }

        planDetailDialog?.show()
    }

    private fun bindPlanDetail(binding: BottomSheetPlanDetailBinding, plan: Plan) {
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

    private fun navigateToOrder(plan: Plan) {
        val intent = Intent(requireContext(), OrderActivity::class.java)
        intent.putExtra(OrderActivity.EXTRA_PLAN, plan)
        startActivity(intent)
    }

    private fun formatPrice(amount: Double): String {
        // Round to avoid floating point precision issues
        val rounded = Math.round(amount * 100.0) / 100.0
        return priceFormatter.format(rounded)
    }

    private fun formatTraffic(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unitIndex = 0
        while (value >= 1024 && unitIndex < units.lastIndex) {
            value /= 1024
            unitIndex++
        }
        return String.format("%.0f %s", value, units[unitIndex])
    }

    private fun getPeriodLabel(period: String?): String = when (period) {
        "month_price", "month" -> "月付"
        "quarter_price", "quarter" -> "季付"
        "half_year_price", "half_year" -> "半年付"
        "year_price", "year" -> "年付"
        else -> "灵活周期"
    }
}
