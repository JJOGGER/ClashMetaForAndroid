package com.xboard.ui.activity

import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.R
import com.github.kr328.clash.databinding.ActivityNodeSelectionBinding
import com.github.kr328.clash.service.data.Selection
import com.github.kr328.clash.service.data.SelectionDao
import com.github.kr328.clash.util.withClash
import com.github.kr328.clash.util.withProfile
import com.xboard.api.RetrofitClient
import com.xboard.base.BaseActivity
import com.xboard.ex.showToast
import com.xboard.model.Server
import com.xboard.network.UserRepository
import com.xboard.storage.MMKVManager
import com.xboard.ui.adapter.NodeAdapter
import com.xboard.util.ServerProxyMapper
import com.xboard.utils.onClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NodeSelectionActivity : BaseActivity<ActivityNodeSelectionBinding>() {

    companion object {
        private const val TAG = "NodeSelectionActivity"
        const val SERVER = "server"
    }

    private lateinit var nodeAdapter: NodeAdapter
    private var groupNames: List<String> = emptyList()
    private var currentGroupIndex: Int = -1
    private val userRepository by lazy { UserRepository(RetrofitClient.getApiService()) }
    private var urlTesting: Boolean = false
    private var mCurrentServer: Server? = null
    private var serverList: MutableList<Server> = mutableListOf()
    private var serverProxyMapping: Map<String, com.github.kr328.clash.core.model.Proxy> =
        emptyMap()

    override fun getViewBinding(): ActivityNodeSelectionBinding {
        return ActivityNodeSelectionBinding.inflate(layoutInflater)
    }

    override fun initView() {
        // 返回按钮
        binding.vBack.onClick {
            finish()
        }

        // 测速按钮
        binding.root.findViewById<ImageView>(
            R.id.url_test_view
        )?.onClick {
            if (clashRunning) {
                requestUrlTesting()
            } else {
                showToast("未连接")
            }
        }
        mCurrentServer = if (MMKVManager.getCurrentGroup() == "自动选择") {
            Server(
                id = -1,
                name = "自动选择",
                host = "",
                port = 0
            )
        } else {
            intent?.getSerializableExtra(SERVER) as? Server?
        }
        setupNodeAdapter()
    }

    override fun initData() {
        loadProxyGroups()
    }

    /**
     * 加载所有代理组
     */
    private fun loadProxyGroups() {
        lifecycleScope.launch {
            userRepository.getUserServers()
                .onSuccess { servers ->
                    if (servers.isNotEmpty()) {
                        serverList = servers
                        val default = Server(
                            id = -1,
                            name = "自动选择",
                            host = "",
                            port = 0
                        )
                        serverList.add(0,default)
                        nodeAdapter.submitList(servers, mCurrentServer?.name)

                        // 如果 Clash 已连接，建立映射关系并更新延迟信息
                        if (clashRunning) {
                            buildServerProxyMapping()
                        }
                    }
                }
        }
//        lifecycleScope.launch {
//            try {
//                // 获取所有代理组名称
//                groupNames = withClash {
//                    queryProxyGroupNames(uiStore.proxyExcludeNotSelectable)
//                }
//
//                if (groupNames.isEmpty()) {
//                    showToast("没有可用的代理组")
//                    return@launch
//                }
//
//                // 默认选择第一个代理组
//                currentGroupIndex = 0
//                loadNodesForGroup(0)
//
//            } catch (e: Exception) {
//                Log.e(TAG, "Failed to load proxy groups: ${e.message}")
//                showToast("加载代理组失败: ${e.message}")
//            }
//        }
    }

    /**
     * 加载指定代理组的所有节点
     */
    private fun loadNodesForGroup(index: Int) {
//        lifecycleScope.launch {
//            try {
//                if (index < 0 || index >= groupNames.size) {
//                    return@launch
//                }
//
//                val groupName = groupNames[index]
//
//                // 使用 withClash 包装器获取代理组信息
//                // 每次都重新查询，确保获取 Clash 中最新的选中状态
//                val group = withClash {
//                    queryProxyGroup(groupName, uiStore.proxySort)
//                }
//
//                // 提交节点列表到适配器，并传递当前选中的节点
//                // group.now 是 Clash 中实际选中的节点名称，确保显示正确的选中状态
//                nodeAdapter.submitList(group.proxies, group.now)
//
//                // 更新标题
//                binding.root.findViewById<android.widget.TextView>(
//                    com.github.kr328.clash.R.id.tv_title
//                )?.text = groupName
//
//                currentGroupIndex = index
//
//            } catch (e: Exception) {
//                Log.e(TAG, "Failed to load nodes: ${e.message}")
//                showToast("加载节点列表失败: ${e.message}")
//            }
//        }
    }

    /**
     * 设置节点适配器
     */
    private fun setupNodeAdapter() {
        nodeAdapter = NodeAdapter { proxy ->
            selectNode(proxy.name)
            MMKVManager.setDefaultServer(proxy)
            setResult(RESULT_OK)
            finish()
        }
        binding.rvNodes.adapter = nodeAdapter
        binding.rvNodes.layoutManager = LinearLayoutManager(this)
    }

    /**
     * 选择节点
     */
    private fun selectNode(serverName: String) {
        nodeAdapter.setSelectedNode(serverName)

        lifecycleScope.launch {
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

                        Log.d(
                            TAG,
                            "Preset node selection: group=${
                                MMKVManager.getCurrentGroup().toString()
                            }, proxy=$clashProxyName"
                        )
                    }
                    return@launch
                }

                // 如果 Clash 已连接，直接切换节点
                val groupNames = withClash {
                    queryProxyGroupNames(uiStore.proxyExcludeNotSelectable)
                }

                val entryGroupName = groupNames.firstOrNull { it == "XBoard" || it == "Proxy" }
                    ?: groupNames.firstOrNull()

                if (entryGroupName == null) {
                    showToast("代理组索引无效")
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

                if (success) {
                    showToast("已切换到 $serverName")
                } else {
                    showToast("切换节点失败")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error selecting node: ${e.message}")
                showToast("切换节点失败: ${e.message}")
            }
        }
    }

    /**
     * 请求 URL 测速
     */
    private fun requestUrlTesting() {
        urlTesting = true
        updateUrlTestButtonStatus()
        performUrlTest()
    }

    /**
     * 建立 Server 到 Clash Proxy 的映射关系
     */
    private suspend fun buildServerProxyMapping() {
        try {
            // 1. 获取入口代理组名称（优先 "XBoard"，否则第一个 select 组）
            val groupNames = withClash {
                queryProxyGroupNames(uiStore.proxyExcludeNotSelectable)
            }

            val entryGroupName = groupNames.firstOrNull { it == "XBoard" || it == "Proxy" }
                ?: groupNames.firstOrNull()

            if (entryGroupName == null) {
                Log.w(TAG, "No proxy group found")
                return
            }

            // 2. 获取代理组的所有 proxy
            val group = withClash {
                queryProxyGroup(entryGroupName, uiStore.proxySort)
            }

            if (group == null) {
                Log.w(TAG, "Proxy group '$entryGroupName' not found")
                return
            }

            // 3. 建立映射关系
            serverProxyMapping = ServerProxyMapper.buildMapping(serverList, group.proxies)

            // 4. 更新 Server 列表的延迟信息（通过扩展 Server 或创建包装类）
            updateServerListWithDelays(group.proxies)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to build server-proxy mapping: ${e.message}", e)
        }
    }

    /**
     * 更新 Server 列表的延迟信息
     */
    private fun updateServerListWithDelays(clashProxies: List<com.github.kr328.clash.core.model.Proxy>) {
        // 由于 Server 是 data class 且没有 delay 字段，我们需要：
        // 1. 扩展 Server 添加 delay 字段，或者
        // 2. 创建一个包装类 ServerWithDelay，或者
        // 3. 在 NodeAdapter 中维护一个 delay Map

        // 暂时在 adapter 中处理，或者创建一个扩展的 Server 类
        // 这里先建立映射，延迟信息在 adapter 中显示
        val delayMap = mutableMapOf<String, Int>()

        for (server in serverList) {
            val proxy = serverProxyMapping[server.name]
            if (proxy != null) {
                delayMap[server.name] = proxy.delay
            }
        }

        // 通知 adapter 更新延迟信息
        nodeAdapter.updateDelays(delayMap)
    }

    /**
     * 执行 URL 测速
     */
    private fun performUrlTest() {
        lifecycleScope.launch {
            try {
                // 1. 获取入口代理组名称
                val groupNames = withClash {
                    queryProxyGroupNames(uiStore.proxyExcludeNotSelectable)
                }

                val entryGroupName = groupNames.firstOrNull { it == "XBoard" || it == "Proxy" }
                    ?: groupNames.firstOrNull()

                if (entryGroupName == null) {
                    showToast("没有可用的代理组")
                    urlTesting = false
                    updateUrlTestButtonStatus()
                    return@launch
                }

                // 2. 执行健康检查（对所有节点测速）
                withClash {
                    healthCheck(entryGroupName)
                }

                // 3. 重新建立映射关系并更新延迟信息
                buildServerProxyMapping()

                showToast("测速完成")

            } catch (e: Exception) {
                Log.e(TAG, "URL test failed: ${e.message}")
                showToast("测速失败: ${e.message}")
            } finally {
                urlTesting = false
                updateUrlTestButtonStatus()
            }
        }
    }

    /**
     * 更新 URL 测速按钮状态
     */
    private fun updateUrlTestButtonStatus() {
        val urlTestView = binding.root.findViewById<ImageView>(
            R.id.url_test_view
        )
        val urlTestProgressView = binding.root.findViewById<android.widget.ProgressBar>(
            R.id.url_test_progress_view
        )

        if (urlTesting) {
            urlTestView?.visibility = View.GONE
            urlTestProgressView?.visibility = View.VISIBLE
        } else {
            urlTestView?.visibility = View.VISIBLE
            urlTestProgressView?.visibility = View.GONE
        }
    }
}