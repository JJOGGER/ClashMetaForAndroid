package com.xboard.ui.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.R
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.databinding.FragmentAccelerateBinding
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.remote.Remote
import com.github.kr328.clash.service.data.Selection
import com.github.kr328.clash.service.data.SelectionDao
import com.github.kr328.clash.util.startClashService
import com.github.kr328.clash.util.stopClashService
import com.github.kr328.clash.util.withClash
import com.github.kr328.clash.util.withProfile
import com.xboard.api.RetrofitClient
import com.xboard.base.BaseFragment
import com.xboard.ex.gone
import com.xboard.ex.showToast
import com.xboard.ex.visible
import com.xboard.model.Server
import com.xboard.network.TicketRepository
import com.xboard.network.UserRepository
import com.xboard.storage.MMKVManager
import com.xboard.ui.activity.MainActivity
import com.xboard.ui.activity.NodeSelectionActivity
import com.xboard.ui.adapter.NoticeBannerAdapter
import com.xboard.ui.dialog.DialogHelper
import com.xboard.ui.dialog.WebsiteRecommendationDialog
import com.xboard.event.OrderPayEvent
import com.xboard.util.AutoSubscriptionManager
import com.xboard.util.ConfigParser
import com.xboard.util.CountryIconMapper
import com.xboard.utils.onClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

/**
 * 加速页面
 */
class AccelerateFragment : BaseFragment<FragmentAccelerateBinding>() {

    companion object {
        private const val TAG = "AccelerateFragment"
    }

    private val autoSubscriptionManager by lazy {
        AutoSubscriptionManager(userRepository, lifecycleScope)
    }
    private val userRepository by lazy { UserRepository(RetrofitClient.getApiService()) }
    private var mDefaultSelected: String? = null
    private val ticketRepository by lazy { TicketRepository(RetrofitClient.getApiService()) }
    private val uiStore by lazy { UiStore(requireContext()) }
    private lateinit var noticeBannerAdapter: NoticeBannerAdapter
    private var noticeAutoScrollJob: kotlinx.coroutines.Job? = null
    private var currentProfileUUID: UUID? = null
    private var currentGroupName: String? = null
    private var currentProxyName: String? = null
    private var connectionStartTime: Long = 0L
    private var connectionTimerJob: kotlinx.coroutines.Job? = null
    private var configCheckJob: kotlinx.coroutines.Job? = null
    protected val clashRunning: Boolean
        get() = Remote.broadcasts.clashRunning
    private val nodeSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // 节点已选择，重新加载节点信息
            mDefaultSelected = MMKVManager.getCurrentProxy()
            lifecycleScope.launch {
                if (clashRunning) {
                    loadCurrentNodeInfo()
                } else {
                    syncClashSelectedNode()
                }
            }
        }
    }

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // VPN 权限已授予，启动 Clash Service
            viewLifecycleOwner.lifecycleScope.launch {
                startClashServiceInternal()
            }
        } else {
            showToast("VPN 权限被拒绝，无法连接")
            binding.btnConnect.text = "开启连接"
            binding.btnConnect.setBackgroundColor("#FF9800".toColorInt())
        }
    }


    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAccelerateBinding {
        return FragmentAccelerateBinding.inflate(inflater, container, false)
    }

    private var mMode = 0
    override fun initView() {
        binding.modeToggle.setOnTabSelectedListener { position ->
            mMode = position
        }
        binding.vWebsite.setOnClickListener {
            showWebsiteRecommendationDialog()
        }
        // 初始化轮播 Banner
        initNoticeBanner()

        binding.cardSelectedNode.onClick {
            navigateToNodeSelection()
        }
        binding.btnConnect.onClick {
            // 点击连接/断开按钮 - 对标 Clash 的 ToggleStatus
            onToggleClashStatus()
        }

    }

    override fun initListener() {

    }

    override fun initData() {
        loadData()
        getSiteLists()
        fetchSubscribeUrl()
        loadNotices()
//        updateButtonState()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    /**
     * 监听支付成功事件，刷新订阅和节点信息
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrderPayEvent(event: OrderPayEvent) {
        Log.d(TAG, "Received OrderPayEvent, refreshing UI...")
        lifecycleScope.launch {
            try {
                // 先刷新订阅配置（等待完成）
                val success = autoSubscriptionManager.autoImportAndApply()
                if (success) {
                    Log.d(TAG, "Subscription updated successfully, refreshing nodes...")
                } else {
                    Log.w(TAG, "Subscription update failed, but still refreshing nodes...")
                }
                
                // 刷新节点列表
                loadData()
                
                // 如果已连接，刷新当前节点信息
                if (clashRunning) {
                    loadCurrentNodeInfo()
                } else {
                    syncClashSelectedNode()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing UI after payment: ${e.message}", e)
                // 即使出错也尝试刷新节点列表
                loadData()
            }
        }
    }

    /**
     *
     * 使用 AutoSubscriptionManager 完成整个自动化流程：
     * 1. 获取或创建配置文件 UUID
     * 2. 自动更新配置（导入）
     * 3. 自动选中 Profile（应用）
     *
     * 无论成功还是失败，都继续跳转到首页
     */
    private fun fetchSubscribeUrl() {
        lifecycleScope.launch {
            try {
                // 自动导入和应用订阅
                autoSubscriptionManager.autoImportAndApply()
            } catch (e: Exception) {
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!clashRunning) {
            binding.btnConnect.text = "开启连接"
            binding.btnConnect.setBackgroundColor("#FF9800".toColorInt())
            stopConnectionTimer()
            return
        }
        switchTunnelMode()
        // 更新 UI 状态
        binding.btnConnect.text = "关闭连接"
        binding.btnConnect.setBackgroundColor("#4CAF50".toColorInt())
        if (connectionTimerJob?.isActive!=true){
            startConnectionTimer()
        }
        loadCurrentNodeInfo()
    }

    private fun loadData() {
        lifecycleScope.launch {
//            userRepository.getSubscribe()
//                .onSuccess {usbscribe->
//                    MMKVManager.saveSubscribe(usbscribe)
//
//                }
            userRepository.getUserServers()
                .onSuccess {
                    if (it.isNotEmpty()) {
                        ConfigParser.parseNodeFromConfig(requireContext())
                        mDefaultSelected = MMKVManager.getCurrentProxy() ?: it[0].name
                        syncClashSelectedNode()
                    }
                }
        }
    }

    private suspend fun syncClashSelectedNode() {
        val name = mDefaultSelected ?: ""
        //获取默认选中
        val defaultSelected =
            Proxy(
                name = name,
                title = MMKVManager.getCurrentGroup().toString(),
                subtitle = "",
                type = if (name == "自动选择") {
                    Proxy.Type.URLTest
                } else {
                    Proxy.Type.Selector
                },
                delay = 0
            )
        val activeProfile = withProfile { queryActive() }
        val uuid = activeProfile?.uuid
        if (uuid != null) {
            withContext(Dispatchers.IO) {
                // 在这里执行数据库操作
                SelectionDao().setSelected(
                    Selection(
                        uuid,
                        MMKVManager.getCurrentGroup() ?: "",
                        mDefaultSelected ?: MMKVManager.getCurrentProxy()
                        ?: ""
                    )
                )

            }
        }
        erNodeCard(defaultSelected)
    }

    private fun showNoticeDialog(title: String, content: String) {
        DialogHelper.showSimpleDialog(
            fragment = this,
            title = title,
            message = content,
            buttonText = "知道了"
        )
    }


    private fun startConnectionTimer() {
        binding.tvConnectionTime.visible()
        connectionTimerJob?.cancel()
        connectionTimerJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                try {
                    val elapsedSeconds = (System.currentTimeMillis() - connectionStartTime) / 1000
                    val hours = elapsedSeconds / 3600
                    val minutes = (elapsedSeconds % 3600) / 60
                    val seconds = elapsedSeconds % 60

                    val timeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                    binding.tvConnectionTime.text = timeStr
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update connection timer: ${e.message}")
                }
                kotlinx.coroutines.delay(1000) // 每秒更新一次
            }
        }
    }

    private fun stopConnectionTimer() {
        connectionTimerJob?.cancel()
        connectionTimerJob = null
        connectionStartTime = 0L
        binding.tvConnectionTime.text = "00:00:00"
        binding.tvConnectionTime.gone()
    }

    private fun getSiteLists() {
        viewLifecycleOwner.lifecycleScope.launch {
            ticketRepository.getKnowledgeArticles()
        }
    }

    private fun formatSpeed(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B/s"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB/s"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB/s"
            else -> "${bytes / (1024 * 1024 * 1024)} GB/s"
        }
    }

    private fun loadCurrentNodeInfo(
        connectedOverride: Boolean? = null,
        retryCount: Int = 0
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val activeProfile = withProfile { queryActive() }
                currentProfileUUID = activeProfile?.uuid
                if (activeProfile == null) {
                    Log.w(TAG, "No active profile found")
                    return@launch
                }
                kotlinx.coroutines.delay(500)
                // 使用 withClash 获取代理组名称
                val groupNames = withClash {
                    queryProxyGroupNames(uiStore.proxyExcludeNotSelectable)
                }
                Log.d(TAG, "Available groups: $groupNames (retry: $retryCount)")

                // 如果没有获取到，且还有重试机会，延迟后重试
                if (groupNames.isEmpty() && retryCount < 2) {
                    Log.w(TAG, "No proxy groups found, retrying... (${retryCount + 1}/2)")
                    kotlinx.coroutines.delay(200)
                    loadCurrentNodeInfo(connectedOverride, retryCount + 1)
                    return@launch
                }

                // 优先选择 "Proxy" 组，否则选择第一个可用的组
                currentGroupName =  groupNames.firstOrNull()

                if (currentGroupName.isNullOrBlank()) {
                    Log.w(TAG, "No proxy group found")
                    return@launch
                }

                // 使用 withClash 获取代理组信息
                val group = withClash {
                    queryProxyGroup(currentGroupName!!, uiStore.proxySort)
                }
                currentProxyName = group.now//当前代理组选中的节点
                Log.d(TAG, "Current proxy: ${group.now}, total proxies: ${group.proxies.size}")
                val activeProxy = group.proxies.firstOrNull { it.name == group.now }
                if (activeProxy != null) {
                    Log.d(
                        TAG,
                        "Active proxy found: ${activeProxy.name}, delay: ${activeProxy.delay}"
                    )
                } else {
                    Log.w(TAG, "Active proxy not found in group")
                }

                erNodeCard(activeProxy, connectedOverride)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load node info: ${e.message}", e)
                erNodeCard(null, connectedOverride)
            }
        }
    }


    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }

    override fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun erNodeCard(server: Server?) {
        server ?: return
        erNodeCard(
            Proxy(
                name = server.name,
                title = MMKVManager.getCurrentGroup().toString(),
                subtitle = "",
                type = if (server.name == "自动选择") {
                    Proxy.Type.URLTest
                } else {
                    Proxy.Type.Selector
                },
                delay = 0
            )
        )
    }

    private fun erNodeCard(
        proxy: Proxy?,
        connectedOverride: Boolean? = null
    ) {
        if (!isAdded) return
        val ctx = context ?: return
        val isConnected = connectedOverride ?: clashRunning
        if (proxy == null) {
            return
        }
        binding.cardSelectedNode.visible()
        if (proxy.type == Proxy.Type.URLTest) {
            binding.ivNodeIcon.setImageResource(R.drawable.ico_unknown)
            binding.tvSelectedNodeName.text = "自动选择"
            return
        }
        val candidateName = proxy?.name
        var displayName = when {
            proxy?.name?.isNotBlank() == true -> proxy.name
            !candidateName.isNullOrBlank() -> candidateName
            else -> getString(R.string.selected_node_placeholder)
        }
        binding.ivNodeIcon.setImageResource(CountryIconMapper.getCountryIconResId(displayName))
        binding.tvSelectedNodeName.text = displayName
//        binding.tvSelectedNodeLatency.text = if (isConnected) {
//            val latencyValue = proxy?.delay?.takeIf { it > 0 }
//            val latencyText = latencyValue?.let {
//                getString(R.string.selected_node_latency_value, it)
//            } ?: getString(R.string.selected_node_latency_unknown)
//            getString(R.string.selected_node_connected_format, latencyText)
//        } else {
//            getString(R.string.selected_node_disconnected)
//        }

        val dotColor = ContextCompat.getColor(
            ctx,
            if (isConnected) R.color.accent_success else R.color.text_tertiary
        )
        binding.vDot.setBackgroundColor(dotColor)
    }

    private fun navigateToNodeSelection() {
        val context = requireContext()
        val intent = Intent(context, NodeSelectionActivity::class.java)
            .apply {
                putExtra(NodeSelectionActivity.SERVER, mDefaultSelected)
            }
        nodeSelectionLauncher.launch(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopConnectionTimer()

    }

    /**
     * 弹出没有订阅的提示对话框
     *
     * 提示内容：《当前无任何有效订阅，请前往购买》
     * 点击好的，切换到第二个tab（购买页面）
     */
    private fun showNoSubscriptionDialog() {
        DialogHelper.showConfirmDialog(
            fragment = this,
            title = "提示",
            message = "当前无任何有效订阅，请前往购买",
            positiveButtonText = "好的",
            negativeButtonText = "取消",
            showNegativeButton = true,
            onPositiveClick = {
                switchToBuyTab()
            }
        )
    }

    /**
     * 切换到购买页面（第二个tab）
     */
    private fun switchToBuyTab() {
        val activity = requireActivity()
        if (activity is MainActivity) {
            // 通过 MainActivity 的 bottomNavigation 切换到购买页面
            // 使用反射或直接访问 bottomNavigation
            try {
                activity.setCurrentTab(1)
            } catch (e: Exception) {
                // 如果找不到，尝试通过 supportFragmentManager 切换
                Toast.makeText(requireContext(), "切换到购买页面", Toast.LENGTH_SHORT).show()
            }
        }
    }


    @SuppressLint("UseKtx")
    private fun onToggleClashStatus() {
        //判断是否有订阅信息
        if (autoSubscriptionManager.isUpdating()) {
            showToast("节点更新中，请稍后再试")
            return
        }
        if (MMKVManager.getSubscribe() == null) {
            showNoSubscriptionDialog()
            return
        }
        if (clashRunning) {
            activity?.stopClashService()
            binding.btnConnect.text = "开启连接"
            binding.btnConnect.setBackgroundColor(Color.parseColor("#FF9800"))
            stopConnectionTimer()
            stopConfigurationCheckLoop()  // ← 停止轮询
        } else {
            switchTunnelMode()
            viewLifecycleOwner.lifecycleScope.launch {
                startClash()
            }
        }

    }

    private fun loadSubscribeInfo() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = userRepository.getSubscribe()

            result
                .onSuccess { subscribe ->
                    MMKVManager.saveSubscribe(subscribe)

                }
                .onError { error ->
                    // 加载失败不影响页面显示
                }
        }
    }

    private suspend fun startClash() {
        val active = withProfile { queryActive() }

        if (active == null || !active.imported) {
            showToast("连接失败")
            return
        }

        // 检查 VPN 权限
        val vpnRequest = android.net.VpnService.prepare(requireContext())
        if (vpnRequest != null) {
            // 需要请求 VPN 权限
            vpnPermissionLauncher.launch(vpnRequest)
        } else {
            // VPN 权限已授予，直接启动 Clash Service
            startClashServiceInternal()
        }
    }

    private suspend fun startClashServiceInternal() {
        val context = requireContext()

        // 使用 startClashService() 工具函数，它会根据 enableVpn 设置选择启动 TunService 或 ClashService
        val vpnRequest = context.startClashService()

        if (vpnRequest != null) {
            // 如果返回了 VPN 请求，说明权限已被拒绝，不应该到这里
            showToast("VPN 权限被拒绝")
            return
        }

        // 更新 UI 状态
        binding.btnConnect.text = "关闭连接"
        binding.btnConnect.setBackgroundColor("#4CAF50".toColorInt())
        connectionStartTime = System.currentTimeMillis()
        startConnectionTimer()

        // 方案 1: 立即尝试加载（可能成功）
        loadCurrentNodeInfo(true)

    }

    // ============ 公告轮播 Banner ============

    private fun initNoticeBanner() {
        noticeBannerAdapter = NoticeBannerAdapter(
            onNoticeClick = { notice ->
                showNoticeDetailDialog(notice.title, notice.content)
            },
            onIndicatorUpdate = { position ->
                updateNoticeIndicators(position)
            }
        )
        binding.vpNoticeBanner.adapter = noticeBannerAdapter
        binding.vpNoticeBanner.offscreenPageLimit = 1

        // 启用用户交互（手势滑动）
        binding.vpNoticeBanner.isUserInputEnabled = true

        // 添加页面变化监听器以重置自动轮播
        binding.vpNoticeBanner.registerOnPageChangeCallback(object :
            androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                // 用户交互时重置自动轮播计时器
                if (state != androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE) {
                    noticeAutoScrollJob?.cancel()
                    // 用户停止交互后，重新启动自动轮播
                } else if (state == androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE) {
                    startNoticeAutoScroll()
                }
            }
        })
    }

    private fun loadNotices() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val noticeResponse = ticketRepository.getNotices(page = 1, perPage = 10)
                if (noticeResponse != null && noticeResponse.data.isNotEmpty()) {
                    noticeBannerAdapter.setData(noticeResponse.data)
                    binding.vpNoticeBanner.visible()

                    // 初始化指示器
                    createNoticeIndicators(noticeResponse.data.size)

                    // 启动自动轮播
                    startNoticeAutoScroll()
                    Log.d(TAG, "Loaded ${noticeResponse.data.size} notices")
                } else {
                    binding.vpNoticeBanner.gone()
                    Log.d(TAG, "No notices available")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load notices: ${e.message}")
                binding.vpNoticeBanner.gone()
            }
        }
    }

    private fun startNoticeAutoScroll() {
        noticeAutoScrollJob?.cancel()
        noticeAutoScrollJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                try {
                    kotlinx.coroutines.delay(5000) // 5秒切换一次
                    val currentItem = binding.vpNoticeBanner.currentItem
                    binding.vpNoticeBanner.setCurrentItem(currentItem + 1, true)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in auto scroll: ${e.message}")
                }
            }
        }
    }

    private fun showNoticeDetailDialog(title: String, content: String) {
        DialogHelper.showSimpleDialog(
            fragment = this,
            title = title,
            message = content,
            buttonText = "知道了"
        )
    }

    private fun showWebsiteRecommendationDialog() {
        try {
            // 从缓存获取网站推荐
            val articles = MMKVManager.getWebsiteRecommendations()

            if (articles.isNullOrEmpty()) {
                showToast("暂无网站推荐")
                return
            }

            val dialog = WebsiteRecommendationDialog.newInstance(articles)
            dialog.show(childFragmentManager, "WebsiteRecommendationDialog")

            Log.d(TAG, "Showing ${articles.size} website recommendations")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show website recommendation dialog: ${e.message}")
            showToast("加载网站推荐失败")
        }
    }

    // ============ 公告指示器 ============

    private fun createNoticeIndicators(count: Int) {
        try {
            // 获取第一个 ViewPager2 的页面视图来访问指示器容器
            val firstPageView = binding.vpNoticeBanner.getChildAt(0) as? android.view.ViewGroup
            val indicatorContainer =
                firstPageView?.findViewWithTag<LinearLayout>("indicator_container")
                    ?: return

            indicatorContainer.removeAllViews()

            for (i in 0 until count) {
                val indicator = android.widget.ImageView(requireContext()).apply {
                    setImageResource(if (i == 0) R.drawable.indicator_active else R.drawable.indicator_inactive)
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.util.TypedValue.applyDimension(
                            android.util.TypedValue.COMPLEX_UNIT_DIP,
                            8f,
                            resources.displayMetrics
                        ).toInt(),
                        android.util.TypedValue.applyDimension(
                            android.util.TypedValue.COMPLEX_UNIT_DIP,
                            8f,
                            resources.displayMetrics
                        ).toInt()
                    ).apply {
                        setMargins(3, 0, 3, 0)
                    }
                }
                indicatorContainer.addView(indicator)
            }
            Log.d(TAG, "Created $count notice indicators")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating notice indicators: ${e.message}")
        }
    }

    private fun updateNoticeIndicators(position: Int) {
        try {
            // 获取 ViewPager2 内部的 RecyclerView
            val recyclerView =
                binding.vpNoticeBanner.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView
                    ?: return
            val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                ?: return

            // 遍历所有可见的 ViewHolder，更新它们的指示器
            for (i in 0 until recyclerView.childCount) {
                val itemView = recyclerView.getChildAt(i) as? android.view.ViewGroup
                val indicatorContainer =
                    itemView?.findViewWithTag<LinearLayout>("indicator_container")
                        ?: continue

                if (indicatorContainer.childCount <= 0) {
                    continue
                }

                val realPosition = position % (indicatorContainer.childCount)

                for (j in 0 until indicatorContainer.childCount) {
                    val indicator =
                        indicatorContainer.getChildAt(j) as? android.widget.ImageView ?: continue
                    indicator.setImageResource(
                        if (j == realPosition) R.drawable.indicator_active else R.drawable.indicator_inactive
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notice indicators: ${e.message}")
        }
    }


    /**
     * 停止配置检查轮询
     */
    private fun stopConfigurationCheckLoop() {
        configCheckJob?.cancel()
        configCheckJob = null
    }

    /**
     * 切换 Tunnel 模式
     * 逻辑复用自 ProxyActivity 中的 PatchMode 处理
     *
     * @param position 0 = Rule (智能), 1 = Global (全局)
     */
    private fun switchTunnelMode() {
        val mode = when (mMode) {
            0 -> com.github.kr328.clash.core.model.TunnelState.Mode.Rule      // 智能模式
            1 -> com.github.kr328.clash.core.model.TunnelState.Mode.Global    // 全局模式
            else -> com.github.kr328.clash.core.model.TunnelState.Mode.Rule
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {

                // 调用 Clash 核心切换模式
                withClash {
                    // 查询当前覆写配置
                    val override =
                        queryOverride(com.github.kr328.clash.core.Clash.OverrideSlot.Session)

                    // 更新模式
                    override.mode = mode

                    // 应用覆写配置
                    patchOverride(com.github.kr328.clash.core.Clash.OverrideSlot.Session, override)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error switching tunnel mode: ${e.message}", e)
                showToast("模式切换失败: ${e.message}")
            }
        }
    }

}
