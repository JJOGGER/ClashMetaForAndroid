package com.xboard.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xboard.ex.showToast
import com.xboard.model.Server
import com.xboard.ui.viewmodel.NodeSelectionViewModel
import com.xboard.util.CountryIconMapper

/**
 * 节点选择页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeSelectionScreen(
    currentServerName: String? = null,
    viewModel: NodeSelectionViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNodeSelected: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 加载服务器列表
    LaunchedEffect(Unit) {
        viewModel.loadServers(currentServerName)
    }

    // 处理错误消息
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            showToast(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "选择节点",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    // 测速按钮
                    IconButton(
                        onClick = { viewModel.requestUrlTesting() },
                        enabled = !uiState.isTestingSpeed && viewModel.clashRunning
                    ) {
                        if (uiState.isTestingSpeed) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Speed,
                                contentDescription = "测速",
                                tint = if (viewModel.clashRunning) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading && uiState.servers.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.error != null && uiState.servers.isEmpty() -> {
                    ErrorState(
                        message = uiState.error ?: "加载失败",
                        onRetry = { viewModel.loadServers(currentServerName) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.servers,
                            key = { it.id }
                        ) { server ->
                            val index = uiState.servers.indexOf(server)
                            val isAuto = server.id == -1
                            val hasRestriction = uiState.hasPlanRestriction
                            val isAllowed = !hasRestriction || isAuto || uiState.allowedServerNames.contains(server.name)
                            val tags = uiState.serverTags[server.name]
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        delayMillis = index * 50
                                    )
                                ) + scaleIn(
                                    initialScale = 0.95f,
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        delayMillis = index * 50,
                                        easing = FastOutSlowInEasing
                                    )
                                ) + slideInVertically(
                                    initialOffsetY = { 30 },
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        delayMillis = index * 50,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                            ) {
                                NodeItem(
                                    server = server,
                                    isSelected = server.name == uiState.selectedServerName,
                                    delay = uiState.delayMap[server.name],
                                    isAvailable = isAllowed,
                                    tags = tags,
                                    onClick = {
                                        if (isAllowed) {
                                            viewModel.selectNode(server.name) {
                                                onNodeSelected()
                                                onNavigateBack()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NodeItem(
    server: Server,
    isSelected: Boolean,
    delay: Int?,
    isAvailable: Boolean,
    tags: List<String>?,
    onClick: () -> Unit
) {
    // 点击反馈和选中状态动画
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "node-item-scale"
    )
    
    val animatedColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            !isAvailable -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(300),
        label = "node-color"
    )
    
    val animatedElevation by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 0.dp,
        animationSpec = tween(300),
        label = "node-elevation"
    )
    
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        color = animatedColor,
        tonalElevation = animatedElevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 国家图标
            Icon(
                painter = painterResource(
                    id = CountryIconMapper.getCountryIconResId(server.name)
                ),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color.Unspecified // 保持图标原始颜色
            )

            // 节点信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                        !isAvailable -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )

                // 节点标签
                if (!tags.isNullOrEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tags.take(3).forEach { tag ->
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                label = {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }
                    }
                }

                // 延迟信息（不展示测试失败，其他速度先除以10再展示）
                if (delay != null && delay in 1..59999) {
                    val displayDelay = delay
                    val delayColor = getDelayColor(displayDelay)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SignalCellularAlt,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = delayColor.copy(
                                alpha = if (isSelected) 0.9f else 0.7f
                            )
                        )
                        Text(
                            text = "${displayDelay}ms",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            } else {
                                delayColor
                            }
                        )
                    }
                }

                // 不可用标识（当前套餐无法使用）
                if (!isAvailable) {
                    Text(
                        text = "当前套餐不可用",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // 选中指示器
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "已选中",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun getDelayColor(delay: Int): Color {
    return when {
        delay < 50 -> MaterialTheme.colorScheme.primary // 极好 - 使用主色
        delay < 100 -> MaterialTheme.colorScheme.tertiary // 很好 - 使用第三色
        delay < 200 -> MaterialTheme.colorScheme.secondary // 良好 - 使用次色
        delay < 500 -> MaterialTheme.colorScheme.errorContainer // 一般 - 使用错误容器色
        else -> MaterialTheme.colorScheme.error // 差 - 使用错误色
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "加载失败",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}

