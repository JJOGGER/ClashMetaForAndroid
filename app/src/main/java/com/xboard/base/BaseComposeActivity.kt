package com.xboard.base

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xboard.event.ThemeChangedEvent
import com.xboard.ui.theme.MaClashTheme
import com.xboard.ui.viewmodel.ThemeViewModel
import com.xboard.utils.ThemeHelper
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Compose Activity 基类
 * 用于所有使用 Compose 的 Activity
 */
abstract class BaseComposeActivity : FragmentActivity() {

    /**
     * 主题 ViewModel，子类可以使用
     */
    protected val themeViewModel = ThemeViewModel.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 加载主题设置
        ThemeHelper.loadThemeSettings(themeViewModel, resources)
        
        // 初始化状态栏设置（防止在主题切换时显示默认黑色）
        // 设置为透明，让 MaClashTheme 的 SideEffect 来设置正确的颜色
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
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
     * 子类可以覆盖此方法实现自定义的主题更新逻辑
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    open fun onThemeChanged(event: ThemeChangedEvent) {
        // 重新加载主题设置
        ThemeHelper.loadThemeSettings(themeViewModel, resources)
        // 重新创建 Activity 以应用新主题（Compose 需要重新组合）
        recreate()
    }
    
    /**
     * 设置 Compose 内容，自动应用主题
     */
    protected fun setThemeContent(content: @Composable () -> Unit) {
        setContent {
            val themeState by themeViewModel.uiState.collectAsState()
            MaClashTheme(
                colorSchemeSelection = themeState.colorScheme,
                darkTheme = themeState.isDarkMode
            ) {
                content()
            }
        }
    }
}

