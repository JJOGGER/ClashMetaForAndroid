package com.xboard.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.remote.Remote
import com.github.kr328.clash.service.data.Selection
import com.github.kr328.clash.service.data.SelectionDao
import com.github.kr328.clash.util.withClash
import com.github.kr328.clash.util.withProfile
import com.xboard.api.RetrofitClient
import com.xboard.model.Server
import com.xboard.network.UserRepository
import com.xboard.storage.MMKVManager
import com.xboard.util.ServerProxyMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 节点选择 ViewModel
 */
class NodeSelectionViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository(RetrofitClient.getApiService())
    private val uiStore by lazy { UiStore(getApplication()) }

    data class NodeSelectionUiState(
        val isLoading: Boolean = false,
        val isTestingSpeed: Boolean = false,
        val error: String? = null,
        val servers: List<Server> = emptyList(),
        val selectedServerName: String? = null,
        val delayMap: Map<String, Int> = emptyMap(), // Server.name -> delay (ms)
        // 是否根据当前订阅套餐限制节点可用范围
        val hasPlanRestriction: Boolean = false,
        // 当前套餐可用的节点名称集合（为空且 hasPlanRestriction=false 时表示不限制）
        val allowedServerNames: Set<String> = emptySet(),
        // 节点标签：Server.name -> 标签列表
        val serverTags: Map<String, List<String>> = emptyMap()
    )

    private val _uiState = MutableStateFlow(NodeSelectionUiState())
    val uiState: StateFlow<NodeSelectionUiState> = _uiState.asStateFlow()

    private var serverProxyMapping: Map<String, com.github.kr328.clash.core.model.Proxy> = emptyMap()

    /**
     * Clash 是否运行中
     */
    val clashRunning: Boolean
        get() = Remote.broadcasts.clashRunning

    /**
     * 加载服务器列表
     */
    fun loadServers(currentServerName: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            userRepository.getUserServers()
                .onSuccess { servers ->
                    if (servers.isNotEmpty()) {
                        val serverList = servers.toMutableList()
                        // 添加"自动选择"选项
                        val default = Server(
                            id = -1,
                            name = "自动选择",
                            host = "",
                            port = 0
                        )
                        serverList.add(0, default)
                        
                        val selectedName = currentServerName ?: if (MMKVManager.getCurrentGroup() == "自动选择") {
                            "自动选择"
                        } else {
                            currentServerName
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            servers = serverList,
                            selectedServerName = selectedName
                        )

                        // 如果 Clash 已连接，建立映射关系并更新延迟信息
                        if (clashRunning) {
                            buildServerProxyMapping()
                        }

                        // 根据当前订阅套餐加载可用节点分组与标签
                        loadPlanRestrictions()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "没有可用的服务器"
                        )
                    }
                }
                .onError { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "加载服务器列表失败"
                    )
                }
        }
    }

    /**
     * 选择节点
     */
    fun selectNode(serverName: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                if (!clashRunning) {
                    // 如果 Clash 未连接，提前写入 SelectionDao
                    val activeProfile = withProfile { queryActive() }
                    val uuid = activeProfile?.uuid

                    if (uuid != null) {
                        // 将 Server 名称映射到 Clash proxy 名称
                        val clashProxyName = if (serverProxyMapping.isNotEmpty()) {
                            ServerProxyMapper.mapToClashProxyName(serverName, serverProxyMapping)
                        } else {
                            // 如果还没有映射，先尝试使用 serverName，等连接后再更新
                            serverName
                        }

                        // 写入 SelectionDao，等连接时 Clash 会自动应用
                        withContext(Dispatchers.IO) {
                            SelectionDao().setSelected(
                                Selection(
                                    uuid,
                                    MMKVManager.getCurrentGroup().toString(),
                                    clashProxyName
                                )
                            )
                        }

                        MMKVManager.saveCurrentNode(
                            MMKVManager.getCurrentGroup().toString(), clashProxyName
                        )
                    }
                    onSuccess()
                    return@launch
                }

                // 如果 Clash 已连接，直接切换节点
                val groupNames = withClash {
                    queryProxyGroupNames(uiStore.proxyExcludeNotSelectable)
                }

                val entryGroupName = groupNames.firstOrNull()

                if (entryGroupName == null) {
                    _uiState.value = _uiState.value.copy(error = "代理组索引无效")
                    return@launch
                }

                // 将 Server 名称映射到 Clash proxy 名称
                val clashProxyName = if (serverProxyMapping.isNotEmpty()) {
                    ServerProxyMapper.mapToClashProxyName(serverName, serverProxyMapping)
                } else {
                    // 如果还没有映射，先建立映射
                    buildServerProxyMapping()
                    ServerProxyMapper.mapToClashProxyName(serverName, serverProxyMapping)
                }

                // 使用 withClash 包装器切换节点
                val success = withClash {
                    patchSelector(entryGroupName, clashProxyName)
                }
                MMKVManager.saveCurrentNode(entryGroupName, clashProxyName)
                
                if (success) {
                    _uiState.value = _uiState.value.copy(selectedServerName = serverName)
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(error = "切换节点失败")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "切换节点失败: ${e.message}")
            }
        }
    }

    /**
     * 请求 URL 测速
     */
    fun requestUrlTesting() {
        if (!clashRunning) {
            _uiState.value = _uiState.value.copy(error = "未连接")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingSpeed = true, error = null)
            performUrlTest()
        }
    }

    /**
     * 建立 Server 到 Clash Proxy 的映射关系
     */
    private suspend fun buildServerProxyMapping() {
        try {
            val serverList = _uiState.value.servers
            if (serverList.isEmpty()) return

            // 1. 获取入口代理组名称（优先 "XBoard"，否则第一个 select 组）
            val groupNames = withClash {
                queryProxyGroupNames(uiStore.proxyExcludeNotSelectable)
            }

            val entryGroupName = groupNames.firstOrNull { it == "XBoard" || it == "Proxy" }
                ?: groupNames.firstOrNull()

            if (entryGroupName == null) {
                return
            }

            // 2. 获取代理组的所有 proxy
            val group = withClash {
                queryProxyGroup(entryGroupName, uiStore.proxySort)
            }

            if (group == null) {
                return
            }

            // 3. 建立映射关系
            serverProxyMapping = ServerProxyMapper.buildMapping(serverList, group.proxies)

            // 4. 更新 Server 列表的延迟信息
            updateServerListWithDelays(group.proxies)

        } catch (e: Exception) {
            // 静默处理异常
        }
    }

    /**
     * 更新 Server 列表的延迟信息
     */
    private fun updateServerListWithDelays(clashProxies: List<com.github.kr328.clash.core.model.Proxy>) {
        val delayMap = mutableMapOf<String, Int>()

        for (server in _uiState.value.servers) {
            val proxy = serverProxyMapping[server.name]
            if (proxy != null) {
                val delay = proxy.delay
                // Clash 返回 0 或 65535（或大于等于 60000）代表测速失败/超时
                if (delay in 1..59999) {
                    delayMap[server.name] = delay
                }
            }
        }

        _uiState.value = _uiState.value.copy(delayMap = delayMap)
    }

    /**
     * 根据当前订阅套餐加载可用节点分组与标签
     */
    private suspend fun loadPlanRestrictions() {
        try {
            val subscribe = MMKVManager.getSubscribe()
            val groupId = subscribe?.plan?.groupId

            if (subscribe?.planId == null || groupId == null) {
                // 未订阅或套餐没有分组信息，不做限制
                _uiState.value = _uiState.value.copy(
                    hasPlanRestriction = false,
                    allowedServerNames = emptySet(),
                    serverTags = emptyMap()
                )
                return
            }

            val result = userRepository.getServersByGroup(groupId)
            result.onSuccess { groupNodes ->
                if (groupNodes.isNullOrEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        hasPlanRestriction = false,
                        allowedServerNames = emptySet(),
                        serverTags = emptyMap()
                    )
                } else {
                    val allowedNames = groupNodes.map { it.name }.toSet()
                    val tagsMap = groupNodes.associate { it.name to it.tags }
                    _uiState.value = _uiState.value.copy(
                        hasPlanRestriction = true,
                        allowedServerNames = allowedNames,
                        serverTags = tagsMap
                    )
                }
            }.onError {
                // 获取失败则不做限制
                _uiState.value = _uiState.value.copy(
                    hasPlanRestriction = false,
                    allowedServerNames = emptySet(),
                    serverTags = emptyMap()
                )
            }
        } catch (_: Exception) {
            // 忽略异常，不影响节点选择基础功能
            _uiState.value = _uiState.value.copy(
                hasPlanRestriction = false,
                allowedServerNames = emptySet(),
                serverTags = emptyMap()
            )
        }
    }

    /**
     * 执行 URL 测速
     */
    private suspend fun performUrlTest() {
        try {
            // 1. 获取入口代理组名称
            val groupNames = withClash {
                queryProxyGroupNames(uiStore.proxyExcludeNotSelectable)
            }

            val entryGroupName = groupNames.firstOrNull { it == "XBoard" || it == "Proxy" }
                ?: groupNames.firstOrNull()

            if (entryGroupName == null) {
                _uiState.value = _uiState.value.copy(
                    isTestingSpeed = false,
                    error = "没有可用的代理组"
                )
                return
            }

            // 2. 执行健康检查（对所有节点测速）
            withClash {
                healthCheck(entryGroupName)
            }

            // 3. 重新建立映射关系并更新延迟信息
            buildServerProxyMapping()

            _uiState.value = _uiState.value.copy(isTestingSpeed = false)

        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isTestingSpeed = false,
                error = "测速失败: ${e.message}"
            )
        }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

