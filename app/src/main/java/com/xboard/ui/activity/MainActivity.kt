package com.xboard.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.github.kr328.clash.util.stopClashService
import com.xboard.api.RetrofitClient
import com.xboard.base.BaseComposeActivity
import com.xboard.ex.showToast
import com.xboard.network.UserRepository
import com.xboard.ui.compose.MainBottomNavigation
import com.xboard.ui.compose.MainScreenContent
import kotlinx.coroutines.launch

/**
 * 主页（首页）
 * 使用 Compose HorizontalPager 实现3个页面的切换
 */
class MainActivity : BaseComposeActivity() {

    companion object {
        // 用于存储当前选中标签索引的状态，以便从外部（Fragment）访问
        var tabIndexState: MutableState<Int>? = null
    }

    private val userRepository by lazy { UserRepository(RetrofitClient.getApiService()) }

    private var backPressedTime: Long = 0
    private val backPressedTimeout = 2000 // 2 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 权限请求
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher =
                registerForActivityResult(RequestPermission()) { }
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 初始化数据
        lifecycleScope.launch {
            userRepository.getUserCommonConfig()
        }

        // 设置 Compose 内容
        setThemeContent {
            MainActivityContent(
                onBackPressed = {
                    handleBackPress()
                }
            )
        }
    }

    /**
     * 切换到指定的标签页
     * 供外部（如 Fragment）调用
     *
     * @param index 标签页索引：0-加速，1-购买，2-我的
     */
    fun setCurrentTab(index: Int) {
        tabIndexState?.value = index
    }

    private fun handleBackPress() {
        val currentTime = System.currentTimeMillis()
        when {
            currentTime - backPressedTime < backPressedTimeout -> {
                // 两次按返回键，退出应用
                finish()
            }

            else -> {
                // 第一次按返回键，提示再按一次退出
                backPressedTime = currentTime
                showToast("再按一次退出程序")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopClashService()
    }
}

@Composable
private fun MainActivityContent(
    onBackPressed: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }

    // 初始化或获取共享状态
    if (MainActivity.tabIndexState == null) {
        MainActivity.tabIndexState = remember { mutableStateOf(0) }
    }
    val sharedTabIndex = MainActivity.tabIndexState!!

    // 同步共享状态到本地状态
    LaunchedEffect(sharedTabIndex.value) {
        if (selectedTabIndex != sharedTabIndex.value) {
            selectedTabIndex = sharedTabIndex.value
        }
    }

    // 同步本地状态到共享状态
    LaunchedEffect(selectedTabIndex) {
        if (sharedTabIndex.value != selectedTabIndex) {
            sharedTabIndex.value = selectedTabIndex
        }
    }

    val context = LocalContext.current

    // 处理返回键
    BackHandler {
        if (selectedTabIndex != 0) {
            selectedTabIndex = 0
            sharedTabIndex.value = 0
        } else {
            onBackPressed()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 主内容区域
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            MainScreenContent(
                pageIndex = selectedTabIndex,
                fragmentManager = (context as androidx.fragment.app.FragmentActivity).supportFragmentManager,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 底部导航栏
        MainBottomNavigation(
            selectedIndex = selectedTabIndex,
            onItemSelected = { index ->
                selectedTabIndex = index
                sharedTabIndex.value = index
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}