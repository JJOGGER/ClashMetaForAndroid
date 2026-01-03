package com.xboard.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import com.xboard.ui.fragment.AccelerateFragment
import com.xboard.ui.fragment.BuyFragment
import com.xboard.ui.fragment.MineFragment

/**
 * 主页内容包装器，用于在 Compose 中嵌入 Fragment
 */
@Composable
fun MainScreenContent(
    pageIndex: Int,
    fragmentManager: FragmentManager,
    modifier: Modifier = Modifier
) {
    // 使用固定的容器ID，确保Fragment可以正确缓存
    val containerViewId = remember { android.view.View.generateViewId() }
    
    // 创建Fragment实例并缓存
    val fragments = remember {
        mapOf(
            0 to AccelerateFragment(),
            1 to BuyFragment(),
            2 to MineFragment()
        )
    }
    
    var currentPageIndex by remember { mutableStateOf(pageIndex) }
    
    // 当 pageIndex 变化时，更新 Fragment
    LaunchedEffect(pageIndex) {
        if (currentPageIndex != pageIndex) {
            currentPageIndex = pageIndex
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                FragmentContainerView(ctx).apply {
                    id = containerViewId
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                // 初始化：添加所有Fragment，但只显示当前的
                if (fragmentManager.findFragmentById(containerViewId) == null) {
                    fragmentManager.beginTransaction().apply {
                        fragments.forEach { (index, fragment) ->
                            add(containerViewId, fragment, "page_$index")
                            if (index != currentPageIndex) {
                                hide(fragment)
                            }
                        }
                        commitNow()
                    }
                } else {
                    // 切换Fragment：显示当前的，隐藏其他的
                    fragmentManager.beginTransaction().apply {
                        fragments.forEach { (index, fragment) ->
                            if (index == currentPageIndex) {
                                show(fragment)
                            } else {
                                hide(fragment)
                            }
                        }
                        commitNow()
                    }
                }
            }
        )
    }
}

