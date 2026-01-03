package com.xboard.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.remote.Broadcasts
import com.github.kr328.clash.remote.Remote
import com.github.kr328.clash.remote.StatusClient
import com.github.kr328.clash.util.withClash
import com.github.kr328.clash.util.withProfile
import com.github.kr328.clash.util.stopClashService
import com.github.kr328.clash.util.startClashService
import com.github.kr328.clash.service.data.Selection
import com.github.kr328.clash.service.data.SelectionDao
import com.xboard.api.RetrofitClient
import com.xboard.network.UserRepository
import com.xboard.network.TicketRepository
import com.xboard.storage.MMKVManager
import com.xboard.ui.fragment.AccelerateFragment
import com.xboard.util.AutoSubscriptionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

data class AnnouncementUiState(
    val title: String = "系统公告",
    val message: String = "高峰时段优先使用智能模式，保持连接更稳定。",
    val actionLabel: String? = null,
    val actionUrl: String? = null
)

data class AccelerateUiState(
    val isConnected: Boolean = false,
    val plan: String = "未订阅",
    val nodeName: String = "自动选择",
    val mode: String = "智能",
    val connectionTime: String = "00:00:00",
    val latency: Int = 0,
    val isSubscriptionUpdating: Boolean = false,
    val subscriptionStatusMessage: String = "",
    val announcements: List<com.xboard.model.Notice> = emptyList(),
    val subscriptionPlanName: String = "",
    val showNoSubscriptionDialog: Boolean = false
)

data class TrafficState(
    val uploadSpeed: Float = 0f,
    val downloadSpeed: Float = 0f
)

class AccelerateViewModel(application: Application, userRepository: UserRepository) : AndroidViewModel(application), Broadcasts.Observer {

    private val _uiState = MutableStateFlow(AccelerateUiState())
    val uiState: StateFlow<AccelerateUiState> = _uiState.asStateFlow()

    private val _trafficState = MutableStateFlow(TrafficState())
    val trafficState: StateFlow<TrafficState> = _trafficState.asStateFlow()

    private val uiStore by lazy { UiStore(application.applicationContext) }
    private val autoSubscriptionManager by lazy { AutoSubscriptionManager(userRepository, viewModelScope) }
    private val ticketRepository by lazy { TicketRepository(RetrofitClient.getApiService()) }
    private var trafficJob: Job? = null
    private var connectionTimerJob: Job? = null
    private var latencyJob: Job? = null
    private var connectionStartTime: Long? = null

    init {
        Remote.broadcasts.addObserver(this)
        _uiState.value = _uiState.value.copy(isConnected = Remote.broadcasts.clashRunning)
        if (Remote.broadcasts.clashRunning) {
            connectionStartTime = System.currentTimeMillis()
            loadCurrentNodeInfo()
            startTrafficUpdates()
            startConnectionTimer()
            startLatencyMonitor()
        }

        viewModelScope.launch {
            autoSubscriptionManager.updatingFlow().collect { updating ->
                _uiState.value = _uiState.value.copy(isSubscriptionUpdating = updating)
                // 当订阅更新完成时，刷新订阅信息显示
                if (!updating) {
                    loadSubscriptionInfoFromCache()
                }
            }
        }
        
        // 初始化时从缓存加载
        loadSubscriptionInfoFromCache()
        // 初始化时加载节点信息
        loadCurrentNodeInfo()
        // 初始化时加载保存的模式
        _uiState.value = _uiState.value.copy(mode = MMKVManager.getTunnelMode())
    }

    override fun onStarted() {
        _uiState.value = _uiState.value.copy(isConnected = true)
        connectionStartTime = System.currentTimeMillis()
        
        // 应用保存的模式设置
        viewModelScope.launch {
            try {
                withClash {
                    val override = queryOverride(Clash.OverrideSlot.Session)
                    
                    // 从 MMKV 获取保存的模式（优先使用 MMKV，因为这是持久化的）
                    val savedMode = MMKVManager.getTunnelMode()
                    val targetMode = if (savedMode == "全局") TunnelState.Mode.Global else TunnelState.Mode.Rule
                    
                    // 如果 Clash 的模式与保存的模式不一致，切换模式
                    if (override.mode != targetMode) {
                        override.mode = targetMode
                        patchOverride(Clash.OverrideSlot.Session, override)
                        delay(200) // 等待模式切换生效
                    }
                    
                    // 如果切换到全局模式，自动选择 GLOBAL 组中对应的节点
                    if (override.mode == TunnelState.Mode.Global) {
                        // 获取当前选中的节点名称（从 MMKV 或 UI 状态）
                        val currentNodeName = _uiState.value.nodeName.takeIf { it != "自动选择" && it.isNotBlank() }
                            ?: MMKVManager.getCurrentProxy()?.takeIf { it != "自动选择" && it.isNotBlank() }
                        
                        // 获取 GLOBAL 组
                        val groupNames = queryProxyGroupNames(uiStore.proxyExcludeNotSelectable)
                        val globalGroup = groupNames.firstOrNull { it == "GLOBAL" }
                        
                        if (globalGroup != null) {
                            // 更新 uiStore.proxyLastGroup
                            uiStore.proxyLastGroup = "GLOBAL"
                            
                            val globalGroupInfo = queryProxyGroup(globalGroup, uiStore.proxySort)
                            
                            // 定义需要排除的特殊节点
                            val excludedNames = setOf(
                                "自动选择", "DIRECT", "REJECT", "REJECT-DROP", "PASS",
                                "故障转移", "COMPATIBLE"
                            )
                            
                            // 在 GLOBAL 组中查找对应的节点
                            val targetProxy = if (currentNodeName != null) {
                                globalGroupInfo.proxies.firstOrNull { 
                                    it.name == currentNodeName || 
                                    it.name.contains(currentNodeName, ignoreCase = true) ||
                                    currentNodeName.contains(it.name, ignoreCase = true)
                                }
                            } else {
                                null
                            }
                            
                            // 验证找到的节点不是特殊节点
                            val validTargetProxy = if (targetProxy != null) {
                                val name = targetProxy.name
                                if (name.isNotBlank() && 
                                    !excludedNames.contains(name) && 
                                    !name.startsWith("故障转移")) {
                                    targetProxy
                                } else {
                                    null
                                }
                            } else {
                                null
                            } ?: globalGroupInfo.proxies.firstOrNull { proxy ->
                                // 如果没有找到匹配的节点，选择第一个有效的节点（排除所有特殊节点）
                                val name = proxy.name
                                name.isNotBlank() &&
                                !excludedNames.contains(name) &&
                                !name.startsWith("故障转移")
                            }
                            
                            if (validTargetProxy != null) {
                                patchSelector(globalGroup, validTargetProxy.name)
                                MMKVManager.saveCurrentNode(globalGroup, validTargetProxy.name)
                                _uiState.value = _uiState.value.copy(nodeName = validTargetProxy.name)
                            }
                        }
                    } else {
                        // 智能模式，更新 uiStore.proxyLastGroup
                        val groupNames = queryProxyGroupNames(uiStore.proxyExcludeNotSelectable)
                        val preferredGroup = groupNames.firstOrNull { it == "XBoard" || it == "Proxy" } 
                            ?: groupNames.firstOrNull()
                        if (preferredGroup != null) {
                            uiStore.proxyLastGroup = preferredGroup
                        }
                    }
                }
            } catch (e: Exception) {
                // 静默处理异常，继续执行后续逻辑
            }
        }
        
        loadCurrentNodeInfo()
        startTrafficUpdates()
        startConnectionTimer()
        startLatencyMonitor()
    }

    override fun onStopped(cause: String?) {
        _uiState.value = _uiState.value.copy(
            isConnected = false,
            connectionTime = "00:00:00",
            latency = 0
        )
        // 不重置节点名称，保持显示当前选择的节点
        stopTrafficUpdates()
        stopConnectionTimer()
        stopLatencyMonitor()
    }

    private fun startTrafficUpdates() {
        trafficJob?.cancel()
        trafficJob = viewModelScope.launch {
            while (true) {
                _trafficState.value = TrafficState(
                    uploadSpeed = Random.nextFloat() * 1024, // Simulate speed in KB/s
                    downloadSpeed = Random.nextFloat() * 5120 // Simulate speed in KB/s
                )
                delay(1000)
            }
        }
    }

    private fun stopTrafficUpdates() {
        trafficJob?.cancel()
        _trafficState.value = TrafficState()
    }

    private fun startConnectionTimer() {
        connectionTimerJob?.cancel()
        connectionTimerJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = connectionStartTime?.let { System.currentTimeMillis() - it } ?: 0L
                _uiState.value = _uiState.value.copy(connectionTime = formatElapsedTime(elapsed))
                delay(1_000)
            }
        }
    }

    private fun stopConnectionTimer() {
        connectionTimerJob?.cancel()
        connectionTimerJob = null
        connectionStartTime = null
        _uiState.value = _uiState.value.copy(connectionTime = "00:00:00")
    }

    private fun startLatencyMonitor() {
        latencyJob?.cancel()
        latencyJob = viewModelScope.launch {
            while (isActive) {
                val latencyValue = fetchCurrentLatency()
                _uiState.value = _uiState.value.copy(latency = latencyValue)
                delay(3_000)
            }
        }
    }

    private fun stopLatencyMonitor() {
        latencyJob?.cancel()
        latencyJob = null
        _uiState.value = _uiState.value.copy(latency = 0)
    }

    private suspend fun fetchCurrentLatency(): Int {
        return try {
            withClash {
                val override = queryOverride(Clash.OverrideSlot.Session)
                val groupNames = queryProxyGroupNames(uiStore.proxyExcludeNotSelectable)
                
                // 根据当前模式选择对应的组
                val targetGroup = if (override.mode == TunnelState.Mode.Global) {
                    // 全局模式：使用 GLOBAL 组
                    groupNames.firstOrNull { it == "GLOBAL" }
                } else {
                    // 智能模式：使用 XBoard 或 Proxy 组
                    groupNames.firstOrNull { it == "XBoard" || it == "Proxy" } ?: groupNames.firstOrNull()
                }
                
                if (targetGroup != null) {
                    val group = queryProxyGroup(targetGroup, uiStore.proxySort)
                    
                    // 如果当前选择的是"自动选择"，需要获取自动选择真正选中的节点的延迟
                    if (group.now == "自动选择") {
                        // 查询"自动选择"子组的状态
                        val autoSelectGroup = queryProxyGroup("自动选择", uiStore.proxySort)
                        if (autoSelectGroup != null && autoSelectGroup.now.isNotBlank() && autoSelectGroup.now != "自动选择") {
                            // 找到实际使用的节点，获取它的延迟
                            val actualProxy = autoSelectGroup.proxies.firstOrNull { it.name == autoSelectGroup.now }
                            return@withClash actualProxy?.delay ?: 0
                        }
                    }
                    
                    // 如果不是"自动选择"，或者无法获取自动选择子组的状态，使用当前节点的延迟
                    val activeProxy = group.proxies.firstOrNull { it.name == group.now }
                    activeProxy?.delay ?: 0
                } else {
                    0
                }
            }
        } catch (e: Exception) {
            0
        }
    }

    private fun formatElapsedTime(elapsedMillis: Long): String {
        val totalSeconds = (elapsedMillis / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun switchTunnelMode(newMode: String) {
        viewModelScope.launch {
            val isSwitchingToGlobal = newMode != "智能"
            
            // 保存模式到 MMKV（无论是否连接都要保存）
            MMKVManager.saveTunnelMode(newMode)
            
            // 如果切换到全局模式，先获取当前智能模式下的节点名称（用于后续选择）
            val currentNodeName = if (isSwitchingToGlobal) {
                // 优先从 UI 状态获取，否则从 MMKV 获取
                _uiState.value.nodeName.takeIf { it != "自动选择" && it.isNotBlank() }
                    ?: MMKVManager.getCurrentProxy()?.takeIf { it != "自动选择" && it.isNotBlank() }
            } else {
                null
            }
            
            // 如果 Clash 已连接，立即切换模式并选择节点
            if (Remote.broadcasts.clashRunning) {
                withClash {
                    val override = queryOverride(Clash.OverrideSlot.Session)
                    val oldMode = override.mode
                    
                    // 如果切换到全局模式且之前是智能模式，从智能模式组获取当前使用的节点
                    // 注意：如果智能模式使用的是"自动选择"，全局模式也要使用"自动选择"
                    var nodeNameToUse = currentNodeName
                    if (isSwitchingToGlobal && oldMode == TunnelState.Mode.Rule) {
                        // 如果还没有获取到节点名称，从智能模式组获取
                        if (nodeNameToUse == null) {
                            val groupNames = queryProxyGroupNames(uiStore.proxyExcludeNotSelectable)
                            val preferredGroup = groupNames.firstOrNull { it == "XBoard" || it == "Proxy" } 
                                ?: groupNames.firstOrNull()
                            if (preferredGroup != null) {
                                val group = queryProxyGroup(preferredGroup, uiStore.proxySort)
                                // 直接使用 group.now，不管是什么节点（包括"自动选择"）
                                // 因为全局模式下也有"自动选择"节点，如果智能模式用自动选择，全局模式也要用自动选择
                                nodeNameToUse = group.now.takeIf { it.isNotBlank() }
                            }
                        }
                    }
                    
                    // 切换模式
                    override.mode = if (newMode == "智能") TunnelState.Mode.Rule else TunnelState.Mode.Global
                    patchOverride(Clash.OverrideSlot.Session, override)
                    
                    // 如果切换到全局模式，等待一下让模式切换生效，然后自动选择GLOBAL组中对应的节点
                    if (isSwitchingToGlobal) {
                        delay(200) // 等待模式切换生效
                        
                        // 重新查询组名，因为切换模式后组名列表可能会变化
                        val groupNames = queryProxyGroupNames(uiStore.proxyExcludeNotSelectable)
                        val globalGroup = groupNames.firstOrNull { it == "GLOBAL" }
                        
                        if (globalGroup != null) {
                            // 更新 uiStore.proxyLastGroup，确保 ProxyActivity 打开时选中 GLOBAL tab
                            uiStore.proxyLastGroup = "GLOBAL"
                            
                            val globalGroupInfo = queryProxyGroup(globalGroup, uiStore.proxySort)
                            
                            // 在 GLOBAL 组中查找对应的节点
                            // 优先精确匹配当前使用的节点（包括"自动选择"）
                            val targetProxy = if (nodeNameToUse != null) {
                                // 优先精确匹配
                                globalGroupInfo.proxies.firstOrNull { it.name == nodeNameToUse }
                                    // 如果精确匹配失败，尝试更严格的模糊匹配
                                    ?: globalGroupInfo.proxies.firstOrNull { proxy ->
                                        val proxyName = proxy.name.lowercase().trim()
                                        val targetName = nodeNameToUse.lowercase().trim()
                                        
                                        // 精确匹配
                                        if (proxyName == targetName) return@firstOrNull true
                                        
                                        // 避免误匹配：例如"故障转移（美国专线01）"不应该匹配"美国专线01"
                                        // 只有当节点名称是目标名称的一部分，且是完整的单词匹配时才匹配
                                        
                                        // 检查是否是完整的子字符串匹配（在括号内或作为独立单词）
                                        val isInParentheses = proxyName.contains("($targetName)") || proxyName.contains("（$targetName）")
                                        val isExactSubstring = proxyName.contains(targetName) && 
                                            (proxyName.startsWith(targetName) || proxyName.endsWith(targetName)) &&
                                            (proxyName.length - targetName.length <= 5) // 允许少量额外字符（如括号、空格）
                                        
                                        // 反向检查：目标名称是否包含节点名称
                                        val isReverseMatch = targetName.contains(proxyName) && 
                                            (targetName.startsWith(proxyName) || targetName.endsWith(proxyName)) &&
                                            (targetName.length - proxyName.length <= 5)
                                        
                                        isInParentheses || (isExactSubstring && !proxyName.contains("故障转移")) || 
                                        (isReverseMatch && !targetName.contains("故障转移"))
                                    }
                            } else {
                                null
                            }
                            
                            // 定义需要排除的特殊节点（注意：不包括"自动选择"，因为全局模式也支持自动选择）
                            val excludedNames = setOf(
                                "DIRECT", "REJECT", "REJECT-DROP", "PASS",
                                "故障转移", "COMPATIBLE"
                            )
                            
                            // 如果找到匹配的节点，验证它不是特殊节点（但允许"自动选择"）
                            val finalTargetProxy = if (targetProxy != null) {
                                val name = targetProxy.name
                                if (name.isNotBlank() && 
                                    !excludedNames.contains(name) && 
                                    !name.startsWith("故障转移")) {
                                    targetProxy
                                } else {
                                    null
                                }
                            } else {
                                null
                            } ?: globalGroupInfo.proxies.firstOrNull { proxy ->
                                // 如果没有找到匹配的节点，选择第一个有效的节点（排除特殊节点，但允许"自动选择"）
                                val name = proxy.name
                                name.isNotBlank() &&
                                !excludedNames.contains(name) &&
                                !name.startsWith("故障转移")
                            }
                            
                            if (finalTargetProxy != null) {
                                patchSelector(globalGroup, finalTargetProxy.name)
                                MMKVManager.saveCurrentNode(globalGroup, finalTargetProxy.name)
                                _uiState.value = _uiState.value.copy(nodeName = finalTargetProxy.name)
                            } else {
                                // 如果找不到合适的节点，记录错误但不选择任何节点
                                // 这样可以避免选择 REJECT 等特殊节点导致无法连接
                                android.util.Log.e("AccelerateViewModel", "无法找到有效的代理节点，跳过选择")
                            }
                        }
                    } else {
                        // 切换回智能模式，重新加载节点信息
                        // 更新 uiStore.proxyLastGroup 为智能模式的首选组
                        val groupNames = queryProxyGroupNames(uiStore.proxyExcludeNotSelectable)
                        val preferredGroup = groupNames.firstOrNull { it == "XBoard" || it == "Proxy" } 
                            ?: groupNames.firstOrNull()
                        if (preferredGroup != null) {
                            uiStore.proxyLastGroup = preferredGroup
                        }
                        loadCurrentNodeInfo()
                    }
                }
            } else {
                // 如果 Clash 未连接，保存节点名称到 MMKV，等连接时使用
                if (isSwitchingToGlobal) {
                    // 如果切换到全局模式，保存节点名称（如果有）
                    if (currentNodeName != null) {
                        MMKVManager.saveCurrentNode("GLOBAL", currentNodeName)
                    }
                }
                // 模式已经保存到 MMKV，连接时会在 onStarted 中应用
            }
            
            // 更新 UI 状态
            _uiState.value = _uiState.value.copy(mode = newMode)
        }
    }

    fun loadCurrentNodeInfo() {
        viewModelScope.launch {
            // 优先从 MMKVManager 获取当前选中的节点名称
            val currentNodeName = MMKVManager.getCurrentProxy() ?: "自动选择"
            _uiState.value = _uiState.value.copy(nodeName = currentNodeName)
            
            // 如果 Clash 已连接，尝试从 Clash 获取更准确的节点信息
            if (Remote.broadcasts.clashRunning) {
                try {
                    withClash {
                        val groupNames = queryProxyGroupNames(uiStore.proxyExcludeNotSelectable)
                        val preferredGroup = groupNames.firstOrNull { it == "XBoard" || it == "Proxy" } ?: groupNames.firstOrNull()
                        if (preferredGroup != null) {
                            val group = queryProxyGroup(preferredGroup, uiStore.proxySort)
                            val activeProxyName = group.now
                            if (activeProxyName.isNotBlank()) {
                                _uiState.value = _uiState.value.copy(nodeName = activeProxyName)
                                // 同步保存到 MMKV
                                MMKVManager.saveCurrentNode(preferredGroup, activeProxyName)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // 如果获取失败，使用 MMKV 中的值
                }
            }
        }
    }

    fun refreshPage() {
        viewModelScope.launch {
            loadAnnouncements()
            loadCurrentNodeInfo()
            // 只从缓存刷新，不发起网络请求
            loadSubscriptionInfoFromCache()
            // 检查并恢复无效节点（包括套餐节点验证）
            // 优先使用订阅更新后的验证方法，因为它会检查套餐节点
            validateAndRestoreNodeAfterSubscriptionUpdate()
            // 如果订阅更新验证没有执行（比如没有套餐信息），则使用通用验证
            validateAndRestoreNode()
        }
    }
    
    /**
     * 验证当前节点是否有效，如果无效则恢复默认
     */
    private suspend fun validateAndRestoreNode() {
//        if (!Remote.broadcasts.clashRunning) {
//            return
//        }
        
        try {
            val currentProxyName = MMKVManager.getCurrentProxy() ?: "自动选择"
            if (currentProxyName == "自动选择") {
                return
            }
            
            // 检查节点是否在服务端列表中
            val userRepository = UserRepository(RetrofitClient.getApiService())
            val serversResult = userRepository.getUserServers()
            
            serversResult.onSuccess { servers ->
                val serverNames = servers.map { it.name }.toSet()
                val isValidInServerList = serverNames.contains(currentProxyName)
                
                // 检查节点是否在当前套餐内
                val subscribe = MMKVManager.getSubscribe()
                val groupId = subscribe?.plan?.groupId
                var isValidInPlan = true
                
                if (subscribe?.planId != null && groupId != null) {
                    val planNodesResult = userRepository.getServersByGroup(groupId)
                    planNodesResult.onSuccess { planNodes ->
                        if (planNodes.isNotEmpty()) {
                            val planNodeNames = planNodes.map { it.name }.toSet()
                            isValidInPlan = planNodeNames.contains(currentProxyName)
                        }
                        
                        // 如果节点无效，恢复默认
                        if (!isValidInServerList || !isValidInPlan) {
                            restoreDefaultNode()
                        }
                    }.onError {
                        // 如果获取套餐节点失败，只检查服务端列表
                        if (!isValidInServerList) {
                            restoreDefaultNode()
                        }
                    }
                } else {
                    // 如果没有套餐限制，只检查服务端列表
                    if (!isValidInServerList) {
                        restoreDefaultNode()
                    }
                }
            }.onError {
                // 如果获取服务端列表失败，静默处理
            }
        } catch (e: Exception) {
            // 静默处理异常
        }
    }
    
    /**
     * 订阅更新后验证节点并恢复为自动选择（如果节点无效）
     * 这个方法专门用于订阅更新后的节点验证，会强制恢复为自动选择
     */
    private suspend fun validateAndRestoreNodeAfterSubscriptionUpdate() {
        try {
            // 等待一下，确保订阅信息已经完全保存到 MMKV
            delay(300)
            
            val currentProxyName = MMKVManager.getCurrentProxy() ?: "自动选择"
            if (currentProxyName == "自动选择") {
                return
            }
            
            // 检查节点是否在当前套餐内
            val subscribe = MMKVManager.getSubscribe()
            val groupId = subscribe?.plan?.groupId
            
            if (subscribe?.planId != null && groupId != null) {
                val userRepository = UserRepository(RetrofitClient.getApiService())
                val planNodesResult = userRepository.getServersByGroup(groupId)
                
                // 使用 suspendCoroutine 等待异步操作完成
                suspendCoroutine<Unit> { continuation ->
                    planNodesResult.onSuccess { planNodes ->
                        if (planNodes.isNotEmpty()) {
                            val planNodeNames = planNodes.map { it.name }.toSet()
                            val isValidInPlan = planNodeNames.contains(currentProxyName)
                            
                            // 如果节点不在新套餐中，恢复为自动选择
                            if (!isValidInPlan) {
                                // 直接调用 restoreDefaultNode，因为它是 suspend 函数
                                // 使用 launch 在后台执行，但等待完成
                                viewModelScope.launch {
                                    restoreDefaultNode()
                                    continuation.resume(Unit)
                                }
                            } else {
                                continuation.resume(Unit)
                            }
                        } else {
                            // 如果套餐没有节点，恢复为自动选择
                            viewModelScope.launch {
                                restoreDefaultNode()
                                continuation.resume(Unit)
                            }
                        }
                    }.onError {
                        // 如果获取套餐节点失败，为了安全起见，恢复为自动选择
                        viewModelScope.launch {
                            restoreDefaultNode()
                            continuation.resume(Unit)
                        }
                    }
                }
            } else {
                // 如果没有套餐信息，保持当前节点不变
            }
        } catch (e: Exception) {
            // 静默处理异常
        }
    }
    
    /**
     * 恢复默认节点
     * 参考旧代码 syncClashSelectedNode 的实现，支持 Clash 未连接的情况
     */
    private suspend fun restoreDefaultNode() {
        try {
            if (!Remote.broadcasts.clashRunning) {
                // 如果 Clash 未连接，提前写入 SelectionDao，等连接时 Clash 会自动应用
                val activeProfile = withProfile { queryActive() }
                val uuid = activeProfile?.uuid
                
                if (uuid != null) {
                    // 获取默认节点名称（自动选择）
                    val defaultProxy = "自动选择"
                    val groupName = MMKVManager.getCurrentGroup()?.toString() ?: "XBoard"
                    
                    // 写入 SelectionDao，等连接时 Clash 会自动应用
                    withContext(Dispatchers.IO) {
                        SelectionDao().setSelected(
                            Selection(
                                uuid,
                                groupName,
                                defaultProxy
                            )
                        )
                    }
                    
                    MMKVManager.saveCurrentNode(groupName, defaultProxy)
                    _uiState.value = _uiState.value.copy(nodeName = defaultProxy)
                }
                return
            }
            
            // 如果 Clash 已连接，直接切换为自动选择
            withClash {
                val groupNames = queryProxyGroupNames(uiStore.proxyExcludeNotSelectable)
                val preferredGroup = groupNames.firstOrNull { it == "XBoard" || it == "Proxy" } 
                    ?: groupNames.firstOrNull()
                if (preferredGroup != null) {
                    // 恢复为自动选择
                    patchSelector(preferredGroup, "自动选择")
                    MMKVManager.saveCurrentNode(preferredGroup, "自动选择")
                    _uiState.value = _uiState.value.copy(nodeName = "自动选择")
                }
            }
        } catch (e: Exception) {
            // 静默处理异常
        }
    }

    fun refreshSubscription(onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val hadSubscribe = MMKVManager.getSubscribe()?.plan != null
            val result = try {
                autoSubscriptionManager.autoImportAndApply()
            } catch (e: Exception) {
                false
            }
            if (result) {
                loadCurrentNodeInfo()
                // AutoSubscriptionManager 已经保存订阅信息到 MMKVManager，直接刷新显示
                loadSubscriptionInfoFromCache()
                if (hadSubscribe) {
                    postSubscriptionStatus("订阅更新成功")
                } else {
                    // 首次无订阅用户，不提示
                    postSubscriptionStatus("")
                }
            } else {
                if (hadSubscribe) {
                    postSubscriptionStatus("订阅更新失败，请稍后再试")
                } else {
                    // 无购买记录时视为无提示的完成
                    postSubscriptionStatus("")
                }
            }
            // 无论订阅更新成功与否，都执行节点验证（使用已有的订阅信息）
            // 这样可以确保即使订阅更新失败，也能验证节点
            validateAndRestoreNodeAfterSubscriptionUpdate()
            onComplete?.invoke(result)
        }
    }

    private fun postSubscriptionStatus(message: String) {
        _uiState.value = _uiState.value.copy(subscriptionStatusMessage = message)
    }

    fun setShowNoSubscriptionDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showNoSubscriptionDialog = show)
    }

    fun loadAnnouncements() {
        viewModelScope.launch {
            try {
                val response = ticketRepository.getNotices(page = 1, perPage = 10)
                if (response != null && response.data.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(announcements = response.data)
                }
            } catch (e: Exception) {
                // keep empty list
            }
        }
    }
    
    /**
     * 从本地缓存加载订阅信息（不发起网络请求）
     * AutoSubscriptionManager 在更新订阅时已经保存了订阅信息到 MMKVManager
     */
    private fun loadSubscriptionInfoFromCache() {
        try {
            val subscribe = MMKVManager.getSubscribe()
            if (subscribe != null && subscribe.plan != null) {
                _uiState.value = _uiState.value.copy(
                    subscriptionPlanName = subscribe.plan.name
                )
            } else {
                _uiState.value = _uiState.value.copy(subscriptionPlanName = "")
            }
        } catch (e: Exception) {
            // keep default
            _uiState.value = _uiState.value.copy(subscriptionPlanName = "")
        }
    }

    fun toggleConnectionState(fragment: AccelerateFragment) {
        // 先同步实际连接状态，避免状态不一致导致的问题
        syncConnectionState()
        
        if (_uiState.value.isConnected) {
            getApplication<Application>().stopClashService()
        } else {
            fragment.requestVpnPermission()
        }
    }
    
    /**
     * 同步VPN连接状态
     * 检查实际的VPN服务状态，如果与UI状态不一致，则同步状态
     */
    fun syncConnectionState() {
        viewModelScope.launch {
            try {
                val actualRunning = StatusClient(getApplication()).currentProfile() != null
                val uiStateRunning = _uiState.value.isConnected
                
                // 如果实际状态与UI状态不一致，同步状态
                if (actualRunning != uiStateRunning) {
                    if (actualRunning) {
                        // 实际已连接，但UI显示未连接，触发onStarted
                        onStarted()
                    } else {
                        // 实际已断开，但UI显示连接中，触发onStopped
                        onStopped("状态同步")
                    }
                }
            } catch (e: Exception) {
                // 静默处理异常
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        Remote.broadcasts.removeObserver(this)
    }

    // Unused overrides from Broadcasts.Observer
    override fun onServiceRecreated() {}
    override fun onProfileChanged() {}
    override fun onProfileUpdateCompleted(uuid: java.util.UUID?) {}
    override fun onProfileUpdateFailed(uuid: java.util.UUID?, reason: String?) {}
    override fun onProfileLoaded() {}

    class Factory(
        private val application: Application,
        private val userRepository: UserRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AccelerateViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AccelerateViewModel(application, userRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}