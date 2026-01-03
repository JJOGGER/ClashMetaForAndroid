package com.xboard.ui.activity

import android.os.Bundle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xboard.base.BaseComposeActivity
import com.xboard.ui.compose.CommissionRecordScreen

/**
 * 佣金发放记录列表页
 */
class CommissionRecordActivity : BaseComposeActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setThemeContent {
            CommissionRecordScreen(
                viewModel = viewModel(),
                onNavigateBack = { finish() }
            )
        }
    }
}
