package com.xboard.ui.activity

import android.os.Bundle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xboard.base.BaseComposeActivity
import com.xboard.ui.compose.ShareScreen

/**
 * 分享/返利页面
 */
class ShareActivity : BaseComposeActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setThemeContent {
            ShareScreen(
                viewModel = viewModel(),
                onNavigateBack = { finish() }
            )
        }
    }
}