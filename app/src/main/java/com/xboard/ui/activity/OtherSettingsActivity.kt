package com.xboard.ui.activity

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xboard.base.BaseComposeActivity
import com.xboard.ui.compose.OtherSettingsScreen
import com.xboard.ui.viewmodel.OtherSettingsViewModel

/**
 * 其他设置页面
 */
class OtherSettingsActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        
        setThemeContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface
            ) {
                val viewModel: OtherSettingsViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return OtherSettingsViewModel(application) as T
                        }
                    }
                )
                
                OtherSettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                    onThemeChanged = {
                        // 基类会自动处理主题变化，触发 recreate()
                    }
                )
            }
        }
    }
}