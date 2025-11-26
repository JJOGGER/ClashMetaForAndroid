package com.xboard.ui.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.databinding.FragmentShareBinding
import com.xboard.api.RetrofitClient
import com.xboard.base.BaseFragment
import com.xboard.network.InviteRepository
import com.xboard.ui.adapter.InviteDetailAdapter
import kotlinx.coroutines.launch
import java.text.DecimalFormat

/**
 * 分享页面 - 邀请返利
 */
class ShareFragment : BaseFragment<FragmentShareBinding>() {

    private val inviteRepository by lazy { InviteRepository(RetrofitClient.getApiService()) }
    private lateinit var inviteDetailAdapter: InviteDetailAdapter
    private val priceFormatter = DecimalFormat("#.##")
    private var currentInviteCode: String = ""

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentShareBinding {
        return FragmentShareBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        // 设置邀请明细列表
        inviteDetailAdapter = InviteDetailAdapter()
//        binding.rvInviteDetails.apply {
//            layoutManager = LinearLayoutManager(requireContext())
//            adapter = inviteDetailAdapter
//        }
    }

    override fun initListener() {
        binding.btnGenerateCode.setOnClickListener {
            generateInviteCode()
        }

        binding.btnCopyCode.setOnClickListener {
            copyInviteCode()
        }

        binding.btnCopyLink.setOnClickListener {
            copyInviteLink()
        }

//        binding.btnTransfer.setOnClickListener {
//            transferCommission()
//        }

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
            // 加载邀请信息
            val infoResult = inviteRepository.getInviteInfo()
            infoResult
                .onSuccess { inviteInfo ->
                    binding.tvInviteCount.text = inviteInfo.inviteCount.toString()
                    binding.tvCommissionBalance.text = "¥${formatPrice(inviteInfo.commissionBalance)}"
                    binding.tvCommissionRate.text = "${(inviteInfo.commissionRate * 100).toInt()}%"
//                    binding.btnTransfer.isEnabled = inviteInfo.commissionBalance > 0
                }
                .onError { error ->
                    showError(error.message)
                }

            // 加载邀请明细
            val detailsResult = inviteRepository.getInviteDetails(page = 1, perPage = 20)
            detailsResult
                .onSuccess { details ->
                    inviteDetailAdapter.updateData(details)
                }
                .onError { error ->
                    showError(error.message)
                }

            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun generateInviteCode() {
        binding.btnGenerateCode.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val result = inviteRepository.generateInviteCode()

            result
                .onSuccess { code ->
                    currentInviteCode = code
                    binding.tvInviteCode.text = code
                    binding.tvInviteCode.visibility = View.VISIBLE
                    binding.btnCopyCode.visibility = View.VISIBLE
                    binding.btnCopyLink.visibility = View.VISIBLE
                    showSuccess("邀请码已生成")
                }
                .onError { error ->
                    showError(error.message)
                }

            binding.btnGenerateCode.isEnabled = true
        }
    }

    private fun copyInviteCode() {
        val code = binding.tvInviteCode.text.toString()
        if (code.isNotEmpty()) {
            copyToClipboard("邀请码", code)
        }
    }

    private fun copyInviteLink() {
        val code = binding.tvInviteCode.text.toString()
        if (code.isNotEmpty()) {
            val link = "https://example.com/register?invite_code=$code"
            copyToClipboard("邀请链接", link)
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

//        binding.btnTransfer.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val result = inviteRepository.transferCommission(balance)

            result
                .onSuccess {
                    showSuccess("转账成功")
                    loadData()
                }
                .onError { error ->
                    showError(error.message)
//                    binding.btnTransfer.isEnabled = true
                }
        }
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        showSuccess("已复制到剪贴板")
    }

    override fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun showSuccess(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
