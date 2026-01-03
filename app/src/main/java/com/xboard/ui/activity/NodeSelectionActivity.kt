package com.xboard.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xboard.base.BaseComposeActivity
import com.xboard.storage.MMKVManager
import com.xboard.ui.compose.NodeSelectionScreen
import com.xboard.ui.viewmodel.NodeSelectionViewModel

/**
 * 节点选择页面
 */
class NodeSelectionActivity : BaseComposeActivity() {

    companion object {
        const val SERVER = "server"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentServerName = if (MMKVManager.getCurrentGroup() == "自动选择") {
            "自动选择"
        } else {
            intent?.getStringExtra(SERVER)
        }

        setThemeContent {
            val viewModel: NodeSelectionViewModel = viewModel()
            NodeSelectionScreen(
                currentServerName = currentServerName,
                viewModel = viewModel,
                onNavigateBack = { finish() },
                onNodeSelected = {
                    // 节点选择已在 ViewModel 中处理，这里只需要设置结果
                    setResult(RESULT_OK)
                }
            )
        }
    }
}