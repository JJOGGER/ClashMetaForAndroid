package com.xboard.ui.activity

import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.databinding.ActivityNodeSelectionBinding
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.util.withClash
import com.xboard.base.BaseActivity
import com.xboard.ex.showToast
import com.xboard.ui.adapter.NodeSelectionAdapter
import kotlinx.coroutines.launch

class NodeSelectionActivity : BaseActivity<ActivityNodeSelectionBinding>() {

    companion object {
        private const val TAG = "NodeSelectionActivity"
    }

    private lateinit var nodeAdapter: NodeSelectionAdapter
    private var groupNames: List<String> = emptyList()
    private var currentGroupIndex: Int = -1
    private var urlTesting: Boolean = false

    override fun getViewBinding(): ActivityNodeSelectionBinding {
        return ActivityNodeSelectionBinding.inflate(layoutInflater)
    }

    override fun initView() {
        // 返回按钮
        binding.vBack.setOnClickListener {
            finish()
        }

        // 测速按钮
        binding.root.findViewById<android.widget.ImageView>(
            com.github.kr328.clash.R.id.url_test_view
        )?.setOnClickListener {
            requestUrlTesting()
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
            try {
                // 获取所有代理组名称
                groupNames = withClash {
                    queryProxyGroupNames(uiStore.proxyExcludeNotSelectable)
                }

                if (groupNames.isEmpty()) {
                    showToast("没有可用的代理组")
                    return@launch
                }

                // 默认选择第一个代理组
                currentGroupIndex = 0
                loadNodesForGroup(0)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to load proxy groups: ${e.message}")
                showToast("加载代理组失败: ${e.message}")
            }
        }
    }

    /**
     * 加载指定代理组的所有节点
     */
    private fun loadNodesForGroup(index: Int) {
        lifecycleScope.launch {
            try {
                if (index < 0 || index >= groupNames.size) {
                    return@launch
                }

                val groupName = groupNames[index]

                // 使用 withClash 包装器获取代理组信息
                // 每次都重新查询，确保获取 Clash 中最新的选中状态
                val group = withClash {
                    queryProxyGroup(groupName, uiStore.proxySort)
                }

                // 提交节点列表到适配器，并传递当前选中的节点
                // group.now 是 Clash 中实际选中的节点名称，确保显示正确的选中状态
                nodeAdapter.submitList(group.proxies, group.now)

                // 更新标题
                binding.root.findViewById<android.widget.TextView>(
                    com.github.kr328.clash.R.id.tv_title
                )?.text = groupName

                currentGroupIndex = index

            } catch (e: Exception) {
                Log.e(TAG, "Failed to load nodes: ${e.message}")
                showToast("加载节点列表失败: ${e.message}")
            }
        }
    }

    /**
     * 设置节点适配器
     */
    private fun setupNodeAdapter() {
        nodeAdapter = NodeSelectionAdapter { proxy ->
            selectNode(proxy.name)
            setResult(RESULT_OK)
            finish()
        }
        binding.rvNodes.adapter = nodeAdapter
        binding.rvNodes.layoutManager = LinearLayoutManager(this)
    }

    /**
     * 选择节点
     */
    private fun selectNode(nodeName: String) {
        lifecycleScope.launch {
            try {
                if (currentGroupIndex < 0 || currentGroupIndex >= groupNames.size) {
                    showToast("代理组索引无效")
                    return@launch
                }

                val groupName = groupNames[currentGroupIndex]

                // 使用 withClash 包装器切换节点
                withClash {
                    patchSelector(groupName, nodeName)
                }

                showToast("已切换到 $nodeName")

                // 更新适配器的选中状态
                nodeAdapter.setSelectedNode(nodeName)

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
     * 执行 URL 测速
     */
    private fun performUrlTest() {
        lifecycleScope.launch {
            try {
                if (currentGroupIndex < 0 || currentGroupIndex >= groupNames.size) {
                    showToast("代理组索引无效")
                    urlTesting = false
                    updateUrlTestButtonStatus()
                    return@launch
                }

                val groupName = groupNames[currentGroupIndex]

                // 执行健康检查
                withClash {
                    healthCheck(groupName)
                }

                // 重新加载节点以获取最新的延迟信息
                loadNodesForGroup(currentGroupIndex)
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
        val urlTestView = binding.root.findViewById<android.widget.ImageView>(
            com.github.kr328.clash.R.id.url_test_view
        )
        val urlTestProgressView = binding.root.findViewById<android.widget.ProgressBar>(
            com.github.kr328.clash.R.id.url_test_progress_view
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