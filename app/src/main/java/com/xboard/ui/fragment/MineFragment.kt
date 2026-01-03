package com.xboard.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.xboard.api.RetrofitClient
import com.xboard.event.ThemeChangedEvent
import com.xboard.network.UserRepository
import com.xboard.ui.compose.MineScreen
import com.xboard.ui.theme.MaClashTheme
import com.xboard.ui.viewmodel.MineViewModel
import com.xboard.ui.viewmodel.ThemeViewModel
import com.xboard.utils.ThemeHelper
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * 我的页面（用户中心）
 * 已重构为 Compose 实现
 */
class MineFragment : Fragment() {

    private val userRepository by lazy { UserRepository(RetrofitClient.getApiService()) }
    
    private val viewModel: MineViewModel by viewModels {
        MineViewModel.Factory(
            requireActivity().application,
            userRepository
        )
    }
    
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
                    MineScreen(
                        viewModel = viewModel,
                        onRefresh = { viewModel.refreshData() }
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
     * ComposeView 会自动响应 themeViewModel.uiState 的变化，无需手动触发
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onThemeChanged(event: ThemeChangedEvent) {
        // 重新加载主题设置
        // themeViewModel.uiState 的变化会自动触发 Compose 重新组合
        ThemeHelper.loadThemeSettings(themeViewModel, resources)
    }
}
