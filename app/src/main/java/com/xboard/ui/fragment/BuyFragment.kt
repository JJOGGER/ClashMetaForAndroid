package com.xboard.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xboard.ui.compose.BuyScreen
import com.xboard.ui.theme.MaClashTheme
import com.xboard.event.ThemeChangedEvent
import com.xboard.ui.viewmodel.BuyViewModel
import com.xboard.ui.viewmodel.ThemeViewModel
import com.xboard.utils.ThemeHelper
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * 购买页面（订阅页面）
 */
class BuyFragment : Fragment() {

    private val themeViewModel = ThemeViewModel.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 加载主题设置
        ThemeHelper.loadThemeSettings(themeViewModel, resources)
        
        return ComposeView(requireContext()).apply {
            setContent {
                val themeState by themeViewModel.uiState.collectAsState()
                MaClashTheme(
                    colorSchemeSelection = themeState.colorScheme,
                    darkTheme = themeState.isDarkMode
                ) {
                    val viewModel: BuyViewModel = viewModel(
                        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : androidx.lifecycle.ViewModel> create(
                                modelClass: Class<T>
                            ): T {
                                return BuyViewModel(requireContext().applicationContext as android.app.Application) as T
                            }
                        }
                    )
                    
                    BuyScreen(
                        viewModel = viewModel,
                        onRefresh = { viewModel.loadPlans() }
                    )
                }
            }
        }
    }
    
    override fun onStart() {
        super.onStart()
        // 注册 EventBus 监听主题变化事件
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        // 重新加载主题设置，确保从其他页面返回时主题正确
        ThemeHelper.loadThemeSettings(themeViewModel, resources)
    }
    
    override fun onStop() {
        super.onStop()
        // 注销 EventBus
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }
    
    /**
     * 监听主题变化事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onThemeChanged(event: ThemeChangedEvent) {
        // 重新加载主题设置
        ThemeHelper.loadThemeSettings(themeViewModel, resources)
    }
}
