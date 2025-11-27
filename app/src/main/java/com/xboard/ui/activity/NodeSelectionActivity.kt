package com.xboard.ui.activity

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.core.model.ProxySort
import com.github.kr328.clash.core.model.ProxyGroup
import com.github.kr328.clash.databinding.ActivityNodeSelectionBinding
import com.github.kr328.clash.service.data.SelectionDao
import com.xboard.base.BaseActivity
import com.xboard.model.ServerGroupNode
import com.xboard.ui.adapter.NodeSelectionAdapter
import com.github.kr328.clash.util.withClash
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class NodeSelectionActivity : BaseActivity<ActivityNodeSelectionBinding>() {

    companion object {
        private const val TAG = "NodeSelectionActivity"
    }

    private lateinit var nodeAdapter: NodeSelectionAdapter
    private var profileUUID: UUID? = null
    private var groupNames: List<String> = emptyList()
    private var states: List<ProxyState> = emptyList()
    private var unorderedStates: Map<String, ProxyState> = emptyMap()
    private var currentGroupIndex: Int = -1

    override fun getViewBinding(): ActivityNodeSelectionBinding {
        return ActivityNodeSelectionBinding.inflate(layoutInflater)
    }

    override fun initView() {
        profileUUID = intent.getStringExtra("profileUUID")?.let { UUID.fromString(it) }
        
        setupNodeAdapter()
        setupRefresh()
    }

    override fun initData() {
        loadProxyGroups()
    }

    private fun loadProxyGroups() {
        lifecycleScope.launch {
            try {
                // 获取所有代理组名称
                groupNames = withClash { 
                    queryProxyGroupNames(uiStore.proxyExcludeNotSelectable) 
                }
                
                // 初始化状态
                states = List(groupNames.size) { ProxyState("") }
                unorderedStates = groupNames.indices.associate { groupNames[it] to states[it] }
                
                // 确定默认选择的代理组索引
                val initialGroupName = uiStore.proxyLastGroup
                currentGroupIndex = if (initialGroupName.isNotEmpty() && groupNames.contains(initialGroupName)) {
                    groupNames.indexOf(initialGroupName)
                } else if (groupNames.isNotEmpty()) {
                    0 // 默认选择第一个
                } else {
                    -1 // 没有代理组
                }
                
                // 加载节点
                if (currentGroupIndex >= 0) {
                    loadNodesForGroup(currentGroupIndex)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load proxy groups: ${e.message}")
                Toast.makeText(this@NodeSelectionActivity, "加载代理组失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadNodesForGroup(index: Int) {
        lifecycleScope.launch {
            try {
                if (index < 0 || index >= groupNames.size) return@launch
                
                val groupName = groupNames[index]
                // 使用 withClash 包装器确保 Clash 已正确初始化
                val group = withClash {
                    queryProxyGroup(groupName, uiStore.proxySort)
                }
                
                // 更新当前组的状态
                states[index].now = group.now
                
                val nodes = group.proxies.map { proxy ->
                    ServerGroupNode(
                        id = proxy.name.hashCode(),
                        type = proxy.type.name,
                        version = "",
                        name = proxy.name,
                        rate = 0.0,
                        tags = emptyList(),
                        isOnline = if (proxy.delay > 0) 1 else 0,
                        cacheKey = proxy.name,
                        lastCheckAt = ""
                    )
                }
                
                nodeAdapter.submitList(nodes)
                
                // 更新标题显示当前组名
                title = groupName
                
                // 更新当前组索引
                currentGroupIndex = index
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load nodes: ${e.message}")
                Toast.makeText(this@NodeSelectionActivity, "加载节点列表失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupNodeAdapter() {
        nodeAdapter = NodeSelectionAdapter { node ->
            selectNode(node.name)
        }
        binding.rvNodes.adapter = nodeAdapter
        binding.rvNodes.layoutManager = LinearLayoutManager(this)
    }

    private fun selectNode(nodeName: String) {
        lifecycleScope.launch {
            try {
                if (currentGroupIndex < 0) return@launch
                
                val groupName = groupNames[currentGroupIndex]
                // 使用 withClash 包装器确保操作正确执行
                val success = withClash {
                    patchSelector(groupName, nodeName)
                }
                if (!success) {
                    Toast.makeText(this@NodeSelectionActivity, "选择节点失败", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // 更新当前状态
                states[currentGroupIndex].now = nodeName

                if (profileUUID != null) {
                    val selection = com.github.kr328.clash.service.data.Selection(
                        uuid = profileUUID!!,
                        proxy = groupName,
                        selected = nodeName
                    )
                    SelectionDao().setSelected(selection)
                }

                Toast.makeText(this@NodeSelectionActivity, "已切换到 $nodeName", Toast.LENGTH_SHORT).show()
                
                delay(500)
                setResult(RESULT_OK)
                finish()

            } catch (e: Exception) {
                Log.e(TAG, "Error selecting node: ${e.message}")
                Toast.makeText(this@NodeSelectionActivity, "切换节点失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 添加刷新功能
    private fun setupRefresh() {

    }
    
    // 添加URL测试功能
    private fun urlTest() {
        lifecycleScope.launch {
            try {
                if (currentGroupIndex >= 0) {
                    val groupName = groupNames[currentGroupIndex]
                    withClash {
                        healthCheck(groupName)
                    }
                    loadNodesForGroup(currentGroupIndex) // 重新加载节点以更新延迟信息
                }
            } catch (e: Exception) {
                Log.e(TAG, "URL test failed: ${e.message}")
                Toast.makeText(this@NodeSelectionActivity, "测速失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 内部状态类，用于跟踪代理组的当前状态
    private class ProxyState(var now: String)
}