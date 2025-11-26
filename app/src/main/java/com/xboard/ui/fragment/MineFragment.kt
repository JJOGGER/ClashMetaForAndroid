package com.xboard.ui.fragment

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.github.kr328.clash.databinding.FragmentMineBinding
import com.github.kr328.clash.databinding.LayoutMineMenuItemBinding
import com.xboard.api.RetrofitClient
import com.xboard.base.BaseFragment
import com.xboard.ex.gone
import com.xboard.ex.visible
import com.xboard.network.AuthRepository
import com.xboard.network.InviteRepository
import com.xboard.network.TicketRepository
import com.xboard.network.UserRepository
import com.xboard.storage.MMKVManager
import com.xboard.ui.activity.LoginActivity
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat

/**
 * 我的页面（用户中心）
 */
class MineFragment : BaseFragment<FragmentMineBinding>() {

    private val userRepository by lazy { UserRepository(RetrofitClient.getApiService()) }
    private val authRepository by lazy { AuthRepository(RetrofitClient.getApiService()) }
    private val ticketRepository by lazy { TicketRepository(RetrofitClient.getApiService()) }
    private val inviteRepository by lazy { InviteRepository(RetrofitClient.getApiService()) }
    private val priceFormatter by lazy { DecimalFormat("#,##0.00") }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMineBinding {
        return FragmentMineBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        super.initView()
        setupMenuItems()
    }

    override fun initListener() {
        binding.menuPurchaseRecords.root.setOnClickListener { loadOrderHistory() }
//        binding.menuDeviceManage.root.setOnClickListener { loadTrafficDetail() }
        binding.menuSetting.root.setOnClickListener { openOtherSettings() }
//        binding.menuOfficialSite.root.setOnClickListener { openWebsite(getSiteBaseUrl()) }
//        binding.menuFaq.root.setOnClickListener { openWebsite(getSiteBaseUrl("/faq")) }
//        binding.menuOnlineService.root.setOnClickListener { openWebsite(getSiteBaseUrl("/contact")) }
        binding.menuMyTickets.root.setOnClickListener { loadTickets() }
//        binding.menuAboutUs.root.setOnClickListener { openWebsite(getSiteBaseUrl("/about")) }
        binding.menuLogout.setOnClickListener { logout() }

        binding.swipeRefresh.setOnRefreshListener {
            loadUserInfo()
            loadInviteStats()
        }
    }

    override fun initData() {
        loadUserInfo()
        loadInviteStats()
    }

    private fun loadUserInfo() {
        binding.swipeRefresh.isRefreshing = true

        viewLifecycleOwner.lifecycleScope.launch {
            val result = userRepository.getUserInfo()

            result
                .onSuccess { user ->
                    binding.tvUserEmail.text = user.email
                    binding.tvUserNickname.text = user.nickname ?: "欢迎回来"
                    binding.tvBalance.text = "¥${formatCurrency(user.balance)}"

                    // 加载订阅信息
                    loadSubscribeInfo()
                }
                .onError { error ->
                    showError(error.message)
                }

            binding.swipeRefresh.isRefreshing = false

        }
    }

    /**
     * 加载订阅信息
     *
     * 从 /user/getSubscribe 接口获取订阅详情，包括：
     * - 总流量 (transfer_enable)
     * - 已用流量 (u + d)
     * - 速度限制 (speed_limit)
     * - 设备限制 (device_limit)
     */
    private fun loadSubscribeInfo() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = userRepository.getSubscribe()

            result
                .onSuccess { subscribe ->
                    MMKVManager.saveSubscribe(subscribe)
                    loadSubscribeDetails()
                }
                .onError { error ->
                    // 加载失败不影响页面显示
                }
        }
    }

    /**
     * 加载订阅详情
     *
     * 需要在 UserRepository 中添加一个新方法来获取完整的订阅信息
     */
    private fun loadSubscribeDetails() {
        val subscribe = MMKVManager.getSubscribe()
        if (subscribe == null) {
            binding.vBuySubscribe.visible()
            binding.vSubscribeInfo.gone()
            return
        } else {
            binding.vBuySubscribe.gone()
            binding.vSubscribeInfo.visible()
        }
        try {
            binding.tvPlanName.text=subscribe.plan.name
            // 总流量：transfer_enable (字节)
            val totalTraffic = subscribe.transferEnable

            // 已用流量：u (上传) + d (下载)
            // 注意：API返回的是 u 和 d 字段，也可能是 upload 和 download
            val uploadTraffic = subscribe.upload ?: 0L
            val downloadTraffic = subscribe.download ?: 0L
            val usedTraffic = uploadTraffic + downloadTraffic
            binding.tvTraffic.text =
                formatTraffic(usedTraffic) + " / " + formatTraffic(totalTraffic)
            // 流量百分比
            val percentage = if (totalTraffic > 0) {
                ((usedTraffic * 100) / totalTraffic).toInt()
            } else {
                0
            }

            binding.pbTrafficPercentage.setProgress(percentage)

            if (subscribe.expiredAt.toLong() > 0L) {
                val expireDate =
                    SimpleDateFormat("yyyy-MM-dd").format(subscribe.expiredAt)
                binding.tvExpireDate.text = "有效期至：$expireDate"
            } else {
                binding.tvExpireDate.text = "未订阅"
            }
        } catch (e: Exception) {
            // 异常处理
        }
    }

    private fun loadInviteStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = inviteRepository.getInviteInfo()

            result
                .onSuccess { info ->
                    binding.tvCommissionBalance.text =
                        "返利余额 ¥${formatCurrency(info.commissionBalance)}"
                }
                .onError { error ->
                    showError(error.message ?: "加载邀请信息失败")
                }
        }
    }

    private fun loadOrderHistory() {
        val intent =
            Intent(requireContext(), com.xboard.ui.activity.OrderHistoryActivity::class.java)
        startActivity(intent)
    }

    private fun loadTrafficDetail() {
        val intent =
            Intent(requireContext(), com.xboard.ui.activity.TrafficDetailActivity::class.java)
        startActivity(intent)
    }

    private fun openOtherSettings() {
        val intent =
            Intent(requireContext(), com.xboard.ui.activity.OtherSettingsActivity::class.java)
        startActivity(intent)
    }

    private fun loadTickets() {
        val intent = Intent(requireContext(), com.xboard.ui.activity.TicketActivity::class.java)
        startActivity(intent)
    }

    private fun logout() {
        viewLifecycleOwner.lifecycleScope.launch {
            authRepository.logout()
            MMKVManager.clearToken()
            MMKVManager.clearUserInfo()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun openWebsite(url: String?) {
        if (url.isNullOrEmpty()) {
            showError("暂无可用地址")
            return
        }

        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            showError("无法打开链接")
        }
    }

    private fun getSiteBaseUrl(path: String = ""): String? {
        val apiUrl = MMKVManager.getApiBaseUrl() ?: return null
        val base = apiUrl.substringBefore("/api")
        return "$base$path"
    }

    private fun setupMenuItems() {
        configureMenu(binding.menuPurchaseRecords, "我的订单", "查看历史订单")
        configureMenu(binding.menuTraffic, "流量明细", showDesc = false)
        configureMenu(binding.menuSetting, "其他设置", showDesc = false)
        configureMenu(binding.menuMyTickets, "我的工单", showDesc = false)
    }

    private fun configureMenu(
        menuBinding: LayoutMineMenuItemBinding,
        title: String,
        desc: String? = null,
        showDesc: Boolean = false
    ) {
        menuBinding.tvMenuTitle.text = title
        if (desc != null || showDesc) {
            menuBinding.tvMenuDesc.isVisible = true
            menuBinding.tvMenuDesc.text = desc ?: ""
        } else {
            menuBinding.tvMenuDesc.isVisible = false
        }
    }

    private fun formatCurrency(amount: Double): String {
        // Round to avoid floating point precision issues
        val rounded = amount / 100.0
        return priceFormatter.format(rounded)
    }

    /**
     * 格式化流量大小
     *
     * @param bytes 字节数
     * @return 格式化后的流量字符串（B/KB/MB/GB/TB）
     */
    private fun formatTraffic(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            bytes < 1024L * 1024 * 1024 * 1024 -> String.format(
                "%.2f GB",
                bytes / (1024.0 * 1024 * 1024)
            )

            else -> String.format("%.2f TB", bytes / (1024.0 * 1024 * 1024 * 1024))
        }
    }

}
