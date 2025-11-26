package com.xboard.ui.activity

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.model.ProxySort
import com.github.kr328.clash.databinding.ActivityNodeSelectionBinding
import com.github.kr328.clash.service.data.SelectionDao
import com.xboard.base.BaseActivity
import com.xboard.model.ServerGroupNode
import com.xboard.ui.adapter.NodeSelectionAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class NodeSelectionActivity : BaseActivity<ActivityNodeSelectionBinding>() {

    companion object {
        private const val TAG = "NodeSelectionActivity"

    }

    private lateinit var nodeAdapter: NodeSelectionAdapter
    private var profileUUID: UUID? = null
    private var groupName: String? = null

    override fun getViewBinding(): ActivityNodeSelectionBinding {
        return ActivityNodeSelectionBinding.inflate(layoutInflater)
    }

    override fun initView() {
        profileUUID = UUID.fromString(intent.getStringExtra("profileUUID"))
        groupName = intent.getStringExtra("groupName")
        
        setupNodeAdapter()
        setupRefresh()
    }

    override fun initData() {
        loadNodes()
    }

    private fun loadNodes() {
        lifecycleScope.launch {
            try {
                if (groupName == null) return@launch
                
                val group = Clash.queryGroup(groupName!!, ProxySort.Default)
                
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
        binding.rvNodesVip.adapter = nodeAdapter
        binding.rvNodesVip.layoutManager = LinearLayoutManager(this)
    }

    private fun selectNode(nodeName: String) {
        lifecycleScope.launch {
            try {
                val success = Clash.patchSelector(groupName!!, nodeName)
                if (!success) {
                    Toast.makeText(this@NodeSelectionActivity, "选择节点失败", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                if (profileUUID != null && groupName != null) {
                    val selection = com.github.kr328.clash.service.data.Selection(
                        uuid = profileUUID!!,
                        proxy = groupName!!,
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

    private fun setupRefresh() {
        // SwipeRefresh 如果布局中存在则可以添加
        // binding.swipeRefresh?.setOnRefreshListener {
        //     lifecycleScope.launch {
        //         loadNodes()
        //         binding.swipeRefresh?.isRefreshing = false
        //     }
        // }
    }

}
