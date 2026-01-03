package com.xboard.ui.fragment

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.xboard.R
import com.xboard.databinding.FragmentShareBinding
import com.xboard.api.RetrofitClient
import com.xboard.base.BaseFragment
import com.xboard.model.InviteDetailsResponse
import com.xboard.network.InviteRepository
import com.xboard.storage.MMKVManager
import com.xboard.ui.activity.CommissionRecordActivity
import com.xboard.ui.adapter.InviteDetailAdapter
import com.xboard.ui.dialog.DialogHelper
import com.xboard.ui.dialog.TransferDialog
import com.xboard.ui.round.RoundTextView
import com.xboard.utils.onClick
import kotlinx.coroutines.launch
import java.text.DecimalFormat

/**
 * 分享页面 - 邀请返利
 */
class ShareFragment : BaseFragment<FragmentShareBinding>() {

    private val inviteRepository by lazy { InviteRepository(RetrofitClient.getApiService()) }
    private val userRepository by lazy { com.xboard.network.UserRepository(RetrofitClient.getApiService()) }
    private lateinit var inviteDetailAdapter: InviteDetailAdapter
    private val priceFormatter = DecimalFormat("#.##")
    private var currentInviteCode: String = ""

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentShareBinding {
        return FragmentShareBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        binding.vBack.onClick { activity?.finish() }
        // 设置邀请明细列表
        inviteDetailAdapter = InviteDetailAdapter(activity)
        binding.tvUnit.text = MMKVManager.getUserConfig()?.currency ?: "CNY"
        binding.rvInviteDetails.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = inviteDetailAdapter
        }
    }

    override fun initListener() {
        binding.btnGenerateCode.setOnClickListener {
            generateInviteCode()
        }

        binding.btnTransfer.setOnClickListener {
            transferCommission()
        }

        binding.tvRecord.setOnClickListener {
            startActivity(Intent(requireContext(), CommissionRecordActivity::class.java))
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadData()
        }
    }

    override fun initData() {
        loadData()
    }

    private fun loadData() {
        binding.swipeRefresh.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            // 1. 加载用户信息（获取返利余额和返利比例）
            val userResult = userRepository.getUserInfo()
            userResult
                .onSuccess { userInfo ->
                    binding.tvCommissionBalance.text =
                        "¥${formatPrice(userInfo.commissionBalance / 100.0)}"
                    // 显示返利比例
                    binding.tvCommissionRate.text = "${userInfo.commissionRate}%"
                }
                .onError { error ->
                    showError("获取用户信息失败: ${error.message}")
                }

            // 2. 加载邀请信息（邀请码列表和邀请用户数）
            val infoResult = inviteRepository.getInviteInfo()
            infoResult
                .onSuccess { inviteInfo ->
                    // stat: [邀请用户数, 邀请返利(分), 佣金余额(分), 佣金比率(%), 其他]
                    updateInviteInfo(inviteInfo)

                }
                .onError { error ->
                    showError("获取邀请信息失败: ${error.message}")
                }

            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun generateInviteCode() {
        binding.btnGenerateCode.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val result = inviteRepository.generateInviteCode()

            result
                .onSuccess { success ->
                    if (success) {
                        // 生成成功，重新加载邀请信息以获取最新的邀请码
                        val infoResult = inviteRepository.getInviteInfo()
                        infoResult
                            .onSuccess { inviteInfo ->
                                updateInviteInfo(inviteInfo)
                            }
                            .onError { error ->
                                showError("获取邀请码失败: ${error.message}")
                            }
                    }
                }
                .onError { error ->
                    showError("生成邀请码失败: ${error.message}")
                }

            binding.btnGenerateCode.isEnabled = true
        }
    }

    private fun updateInviteInfo(inviteInfo: InviteDetailsResponse) {
        if (inviteInfo.codes.isNotEmpty()) {
            if (inviteInfo.stat.isNotEmpty()) {
                binding.tvInviteCount.text = inviteInfo.stat[0].toString()
                binding.tvCommissionRate.text = "${inviteInfo.stat[3]}%"
                binding.tvReconfirmBalance.text = inviteInfo.stat[1].toString()
                binding.tvTotalBalance.text = inviteInfo.stat[2].toString()
            }
            inviteDetailAdapter.updateData(inviteInfo.codes)
        }
    }


    private fun formatPrice(amount: Double): String {
        // Round to avoid floating point precision issues
        val rounded = Math.round(amount * 100.0) / 100.0
        return priceFormatter.format(rounded)
    }

    private fun transferCommission() {
        val balanceText = binding.tvCommissionBalance.text.toString()
        val balance = balanceText.replace("¥", "").toDoubleOrNull() ?: 0.0

        if (balance <= 0) {
            showError("余额不足")
            return
        }

        // 显示划转弹窗
        showTransferDialog(balance)
    }

    private fun showTransferDialog(maxBalance: Double) {
        val dialog = TransferDialog.newInstance(maxBalance) { amountText ->
            val amount = amountText.toDoubleOrNull() ?: 0.0

            if (amount <= 0) {
                showError("请输入有效的划转金额")
                return@newInstance
            }

            if (amount > maxBalance) {
                showError("划转金额不能超过余额")
                return@newInstance
            }

            performTransfer(amount)
        }
        dialog.show(childFragmentManager, "TransferDialog")
    }

    private fun performTransfer(amount: Double) {
        binding.btnTransfer.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val result = inviteRepository.transferCommission(amount)

            result
                .onSuccess {
                    showSuccess("划转成功")
                    loadData()
                }
                .onError { error ->
                    showError(error.message ?: "划转失败")
                    binding.btnTransfer.isEnabled = true
                }
        }
    }


}
