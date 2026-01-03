package com.xboard.ui.activity

import android.os.Bundle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xboard.base.BaseComposeActivity
import com.xboard.ui.compose.TrafficDetailScreen
import com.xboard.ui.viewmodel.TrafficDetailViewModel

/**
 * 流量明细页面
 */
class TrafficDetailActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setThemeContent {
            val viewModel: TrafficDetailViewModel = viewModel()
            TrafficDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { finish() }
            )
        }
    }
}
