package com.xboard.ui.fragment

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.github.kr328.clash.R
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.core.model.ProxySort
import com.github.kr328.clash.databinding.FragmentAccelerateBinding
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.remote.Remote
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
import com.xboard.ui.activity.MainActivity
import com.xboard.ui.activity.NodeSelectionActivity
import com.xboard.utils.onClick
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.*

/**
 * 加速页面
 */
class AccelerateFragment : BaseFragment<FragmentAccelerateBinding>() {

    companion object {
        private const val TAG = "AccelerateFragment"
    }

    private val userRepository by lazy { UserRepository(RetrofitClient.getApiService()) }
    private val ticketRepository by lazy { TicketRepository(RetrofitClient.getApiService()) }
    private val uiStore by lazy { UiStore(requireContext()) }
    private var selectedServer: Server? = null
    private var currentProfileUUID: UUID? = null
    private var currentGroupName: String? = null
    private var currentProxyName: String? = null
    private var connectionStartTime: Long = 0L
    private var connectionTimerJob: kotlinx.coroutines.Job? = null
    protected val clashRunning: Boolean
        get() = Remote.broadcasts.clashRunning
    private val nodeSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // 节点已选择，重新加载节点信息
            loadCurrentNodeInfo()
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
            binding.btnConnect.setBackgroundColor(android.graphics.Color.parseColor("#FF9800"))
        }
    }


    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAccelerateBinding {
        return FragmentAccelerateBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        viewLifecycleOwner.lifecycleScope.launch {
            fetch()
        }
        binding.modeToggle.setOnTabSelectedListener {
//            when (checkedId) {
//                binding.btnModeSmart.id -> {
//                    binding.tvModeHint.text = "使用智能模式，将自动选择最佳节点"
//                }
//                binding.btnModeGlobal.id -> {
//                    binding.tvModeHint.text = "使用全局模式，将使用所有节点"
//                }
//            }
        }
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
        renderNodeCard(null, selectedServer?.name)
        loadData()
        loadCurrentNodeInfo()
//        loadLatestNotice()
//        updateButtonState()
    }

    override fun onResume() {
        super.onResume()
        loadCurrentNodeInfo()
    }

    private fun loadData() {

        viewLifecycleOwner.lifecycleScope.launch {
            val statResult = userRepository.getUserStat()
            val serversResult = userRepository.getUserServers()

            statResult
                .onSuccess { stat ->
                    binding.tvSelectedNodeName.text = "--"
//                    binding.tvUpload.text = formatBytes(stat.upload)
//                    binding.tvDownload.text = formatBytes(stat.download)
//                    binding.tvTotal.text = formatBytes(stat.total)
//                    binding.progressBar.progress = if (stat.total > 0) {
//                        ((stat.upload + stat.download) * 100 / stat.total).toInt()
//                    } else {
//                        0
//                    }
                }
                .onError { error ->
                    showError(error.message)
                }

            serversResult
                .onSuccess { servers ->
                    updateSelectedServer(servers.firstOrNull())
                }
                .onError { error ->
                    showError(error.message)
                }

        }
    }

    // ============ 节点和流量显示 ============

    private fun loadLatestNotice() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val notices = ticketRepository.getNotices(page = 1, perPage = 1)
                if (!notices?.data.isNullOrEmpty()) {
                    val notice = notices.data.first()
                    // 显示公告弹窗
                    showNoticeDialog(notice.title, notice.content)
                    Log.d(TAG, "Latest notice: ${notice.title}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load notice: ${e.message}")
            }
        }
    }

    private fun showNoticeDialog(title: String, content: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(content)
            .setPositiveButton("知道了") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
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
                    Log.d(TAG, "Connection time: $timeStr")
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

    private fun startTrafficMonitoring() {
        viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                try {
                    val traffic = Clash.queryTrafficNow()
//                    binding.tvUploadSpeed.text = formatSpeed(traffic.upload)
//                    binding.tvDownloadSpeed.text = formatSpeed(traffic.download)
//                    binding.tvTotalTraffic.text = formatBytes(traffic.uploadTotal + traffic.downloadTotal)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to query traffic: ${e.message}")
                }
                kotlinx.coroutines.delay(1000) // 每秒更新一次
            }
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

    private fun loadCurrentNodeInfo(connectedOverride: Boolean? = null) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val activeProfile = withProfile { queryActive() }
                currentProfileUUID = activeProfile?.uuid
                if (activeProfile == null) {
                    Log.w(TAG, "No active profile found")
                    renderNodeCard(null, selectedServer?.name, false)
                    return@launch
                }

                // 使用 withClash 获取代理组名称
                val groupNames = withClash {
                    queryProxyGroupNames(uiStore.proxyExcludeNotSelectable)
                }
                Log.d(TAG, "Available groups: $groupNames")

                // 优先选择 "Proxy" 组，否则选择第一个可用的组
                currentGroupName = groupNames.firstOrNull { it == "Proxy" }
                    ?: groupNames.firstOrNull()

                if (currentGroupName.isNullOrBlank()) {
                    Log.w(TAG, "No proxy group found")
                    renderNodeCard(null, selectedServer?.name, connectedOverride)
                    return@launch
                }

                // 使用 withClash 获取代理组信息
                val group = withClash {
                    queryProxyGroup(currentGroupName!!, uiStore.proxySort)
                }
                currentProxyName = group.now
                Log.d(TAG, "Current proxy: ${group.now}, total proxies: ${group.proxies.size}")
                
                val activeProxy = group.proxies.firstOrNull { it.name == group.now }
                if (activeProxy != null) {
                    Log.d(TAG, "Active proxy found: ${activeProxy.name}, delay: ${activeProxy.delay}")
                } else {
                    Log.w(TAG, "Active proxy not found in group")
                }
                
                renderNodeCard(activeProxy, group.now, connectedOverride)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load node info: ${e.message}", e)
                renderNodeCard(null, selectedServer?.name, connectedOverride)
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

    private fun updateSelectedServer(server: Server?) {
        selectedServer = server
        if (!clashRunning) {
            renderNodeCard(null, server?.name, false)
        }
    }

    private fun renderNodeCard(
        proxy: Proxy?,
        fallbackName: String? = null,
        connectedOverride: Boolean? = null
    ) {
        if (!isAdded) return
        val ctx = context ?: return
        val isConnected = connectedOverride ?: clashRunning
        if (isConnected) {
            binding.cardSelectedNode.visible()
        } else {
            binding.cardSelectedNode.gone()
        }
        val candidateName = selectedServer?.name
        val displayName = when {
            proxy?.title?.isNotBlank() == true -> proxy.title
            proxy?.name?.isNotBlank() == true -> proxy.name
            !fallbackName.isNullOrBlank() -> fallbackName
            !candidateName.isNullOrBlank() -> candidateName
            else -> getString(R.string.selected_node_placeholder)
        }
        if (displayName.contains("香港")) {
            binding.ivNodeIcon.setImageResource(R.drawable.ico_hk)
        } else if (displayName.contains("日本")) {
            binding.ivNodeIcon.setImageResource(R.drawable.ico_jp)
        } else if (displayName.contains("美国")) {
            binding.ivNodeIcon.setImageResource(R.drawable.ico_us)
        } else if (displayName.contains("新加坡")) {
            binding.ivNodeIcon.setImageResource(R.drawable.ico_sg)
        } else if (displayName.contains("韩国")) {
            binding.ivNodeIcon.setImageResource(R.drawable.ico_ko)
        } else if (displayName.contains("荷兰")) {
            binding.ivNodeIcon.setImageResource(R.drawable.ico_helan)
        } else if (displayName.contains("瑞典")) {
            binding.ivNodeIcon.setImageResource(R.drawable.ico_ruidian)
        } else {
            binding.ivNodeIcon.setImageResource(R.drawable.ico_unknown)
        }
        binding.tvSelectedNodeName.text = displayName
        binding.tvSelectedNodeLatency.text = if (isConnected) {
            val latencyValue = proxy?.delay?.takeIf { it > 0 }
            val latencyText = latencyValue?.let {
                getString(R.string.selected_node_latency_value, it)
            } ?: getString(R.string.selected_node_latency_unknown)
            getString(R.string.selected_node_connected_format, latencyText)
        } else {
            getString(R.string.selected_node_disconnected)
        }

        val dotColor = ContextCompat.getColor(
            ctx,
            if (isConnected) R.color.accent_success else R.color.text_tertiary
        )
        binding.vDot.setBackgroundColor(dotColor)
    }

    private fun navigateToNodeSelection() {
        val context = requireContext()
        val intent = Intent(context, NodeSelectionActivity::class.java)
        nodeSelectionLauncher.launch(intent)
    }

    private fun updateModeHint() {

    }

    /**
     * 点击连接按钮的处理
     *
     * 逻辑：
     * 1. 检查是否有有效的订阅
     * 2. 如果没有订阅，弹出提示对话框
     * 3. 如果有订阅，调用 withProfile 连接节点
     */
    private fun onConnectButtonClicked() {
        // 检查是否有有效的订阅
//        if (!hasValidSubscription()) {
//            // 没有有效订阅，弹出提示
//            showNoSubscriptionDialog()
//        } else {
//            // 有有效订阅，连接节点
//            connectToNode()
//        }
    }


    /**
     * 弹出没有订阅的提示对话框
     *
     * 提示内容：《当前无任何有效订阅，请前往购买》
     * 点击好的，切换到第二个tab（购买页面）
     */
    private fun showNoSubscriptionDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("提示")
            .setMessage("当前无任何有效订阅，请前往购买")
            .setPositiveButton("好的") { dialog, _ ->
                dialog.dismiss()
                // 切换到第二个tab（购买页面）
                switchToBuyTab()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
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
//                val bottomNav =
//                    activity.findViewById<BottomNavigationView>(
//                        R.id.bottom_navigation
//                    )
//                bottomNav?.selectedItemId = R.id.nav_buy
            } catch (e: Exception) {
                // 如果找不到，尝试通过 supportFragmentManager 切换
                Toast.makeText(requireContext(), "切换到购买页面", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 连接到节点
     *
     * 参考项目中"点此启动"的逻辑：
     * 1. 调用 withProfile 获取 IProfileManager 实例
     * 2. 查询所有Profile
     * 3. 找到订阅类型的Profile（URL类型）
     * 4. 调用 update(uuid) 更新Profile
     * 5. Clash服务监听数据库变化
     * 6. VPN配置生效
     */
//    private fun connectToNode() {
//        viewLifecycleOwner.lifecycleScope.launch {
//            try {
//                // 显示连接中状态
//                binding.btnConnect.text = "连接中..."
//                binding.btnConnect.isEnabled = false
//
//                withProfile {
//                    // 获取所有Profile
//                    val profiles = queryAll()
//
//                    // 找到订阅类型的Profile（URL类型）
//                    profiles.forEach { profile ->
//                        if (profile.imported && profile.type.name == "Url") {
//                            // 调用 update 方法更新Profile
//                            update(profile.uuid)
//                        }
//                    }
//                }
//
//                // 启动 Clash Service（会触发 VPN 权限请求）
//                val vpnRequest = startClashService()
//
//                if (vpnRequest != null) {
//                    // 需要用户授权 VPN 权限
//                    vpnPermissionLauncher.launch(vpnRequest)
//                } else {
//                    // 已授权，直接启动成功
//                    onConnectSuccess()
//                }
//            } catch (e: Exception) {
//                binding.btnConnect.text = "开启连接"
//                binding.btnConnect.isEnabled = true
//                Toast.makeText(requireContext(), "连接失败: ${e.message}", Toast.LENGTH_SHORT)
//                    .show()
//            }
//        }
//    }

//    private val vpnPermissionLauncher = registerForActivityResult(
//        ActivityResultContracts.StartActivityForResult()
//    ) { result ->
//        if (result.resultCode == Activity.RESULT_OK) {
//            // 用户授权成功，再次启动 Clash Service
//            viewLifecycleOwner.lifecycleScope.launch {
//                try {
//                    startClashService()
//                    onConnectSuccess()
//                } catch (e: Exception) {
//                    binding.btnConnect.text = "开启连接"
//                    binding.btnConnect.isEnabled = true
//                    Toast.makeText(requireContext(), "连接失败: ${e.message}", Toast.LENGTH_SHORT)
//                        .show()
//                }
//            }
//        } else {
//            // 用户拒绝授权
//            binding.btnConnect.text = "开启连接"
//            binding.btnConnect.isEnabled = true
//            Toast.makeText(requireContext(), "VPN 权限被拒绝", Toast.LENGTH_SHORT).show()
//        }
//    }

    private fun onToggleClashStatus() {
        if (clashRunning) {
            activity?.stopClashService()
            binding.btnConnect.text = "开启连接"
            binding.btnConnect.setBackgroundColor(android.graphics.Color.parseColor("#FF9800"))
            stopConnectionTimer()
            renderNodeCard(null, selectedServer?.name, false)
        } else {
            viewLifecycleOwner.lifecycleScope.launch {
                startClash()
            }
        }

    }

    private suspend fun startClash() {
        val active = withProfile { queryActive() }

        if (active == null || !active.imported) {
            showToast("连接失败")
            renderNodeCard(null, selectedServer?.name, false)
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
        binding.btnConnect.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
        connectionStartTime = System.currentTimeMillis()
        startConnectionTimer()

        // 立即加载节点信息（从已知的配置中获取，不需要等待延迟数据）
        // 这样用户能立即看到当前连接的节点
        loadCurrentNodeInfo(true)

        // 延迟 1 秒后再次加载，确保 Clash 已完全初始化并获取到最新的延迟信息
        // 这次加载会更新延迟数据
        kotlinx.coroutines.delay(1000)
        loadCurrentNodeInfo(true)
    }

    private suspend fun fetch() {
        val state = withClash {
            queryTunnelState()
        }
        val providers = withClash {
            queryProviders()
        }
//        withContext(Dispatchers.Main) {
//            binding.mode = when (mode) {
//                TunnelState.Mode.Direct -> context.getString(R.string.direct_mode)
//                TunnelState.Mode.Global -> context.getString(R.string.global_mode)
//                TunnelState.Mode.Rule -> context.getString(R.string.rule_mode)
//                else -> context.getString(R.string.rule_mode)
//            }
//        }
//        setMode(state.mode)
//        setHasProviders(providers.isNotEmpty())
    }

    private enum class Mode { SMART, GLOBAL }

    private val isActive: Boolean
        get() = viewLifecycleOwner.lifecycle.currentState.isAtLeast(
            androidx.lifecycle.Lifecycle.State.STARTED
        )
}
