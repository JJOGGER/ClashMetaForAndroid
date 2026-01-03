package com.xboard.ui.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesomeMotion
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CardMembership
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.xboard.model.Notice
import com.xboard.ui.theme.MaClashTheme
import com.xboard.ui.viewmodel.AccelerateUiState
import com.xboard.ui.viewmodel.AccelerateViewModel
import com.xboard.ui.viewmodel.AnnouncementUiState
import com.xboard.ui.compose.AnnouncementBottomSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccelerateScreen(
    viewModel: AccelerateViewModel,
    onNavigateToNodeSelection: () -> Unit,
    onToggleConnection: () -> Unit,
    onRefreshSubscription: () -> Unit,
    onShowAnnouncement: (AnnouncementUiState) -> Unit,
    onNavigateToBuy: () -> Unit,
    onNavigateToTest: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val trafficState by viewModel.trafficState.collectAsState()
    val modes = listOf("智能", "全局")

    // 公告对话框状态
    var announcementDialogState by remember { mutableStateOf<AnnouncementUiState?>(null) }

    val animatedBackgroundColor by animateColorAsState(
        targetValue = if (uiState.isConnected) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 300),
        label = "background"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = animatedBackgroundColor,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                .verticalScroll(scrollState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 公告轮播放在最前面 - 使用AnimatedVisibility避免跳变
                AnimatedVisibility(
                    visible = uiState.announcements.isNotEmpty(),
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(200))
                ) {
                    if (uiState.announcements.isNotEmpty()) {
                        val notice = uiState.announcements.first()
                        // 如果只有一条公告且没有图片，使用简化布局
                        if (uiState.announcements.size == 1 && notice.imgUrl.isBlank()) {
                            SimpleNoticeCard(
                                notice = notice,
                                onClick = {
                                    announcementDialogState = AnnouncementUiState(
                                        title = notice.title,
                                        message = notice.content,
                                        actionLabel = "查看详情",
                                        actionUrl = notice.imgUrl // imgUrl 用于图片展示
                                    )
                                }
                            )
                        } else {
                            NoticeBannerCarousel(
                                notices = uiState.announcements,
                                onNoticeClick = { notice ->
                                    announcementDialogState = AnnouncementUiState(
                                        title = notice.title,
                                        message = notice.content,
                                        actionLabel = "查看详情",
                                        actionUrl = notice.imgUrl
                                    )
                                }
                            )
                        }
                    }
                }

                HeroHeaderSection(
                    uiState = uiState,
                    onNavigateToNodeSelection = onNavigateToNodeSelection,
                    onToggleConnection = onToggleConnection,
                    onRefresh = onRefreshSubscription,
                    onNavigateToTest = onNavigateToTest,
                    trafficState = trafficState
                )

                ConnectionInfoSection(
                    uiState = uiState,
                    onNavigateToNodeSelection = onNavigateToNodeSelection
                )

                SubscriptionStatusBanner(
                    isUpdating = uiState.isSubscriptionUpdating,
                    statusMessage = uiState.subscriptionStatusMessage,
                    onRefresh = onRefreshSubscription
                )

                // 模式切换按钮
                ModeSwitch(selectedMode = uiState.mode, modes = modes) { newMode ->
                    viewModel.switchTunnelMode(newMode)
                }

                // 连接按钮固定在底部
                ConnectButton(isConnected = uiState.isConnected, onClick = onToggleConnection)
            }
        }

        // 公告详情 BottomSheet
        announcementDialogState?.let { announcement ->
            AnnouncementBottomSheet(
                title = announcement.title,
                message = announcement.message,
                imageUrl = announcement.actionUrl?.takeIf { it.isNotBlank() },
                actionUrl = announcement.actionUrl?.takeIf { it.isNotBlank() && it.startsWith("http") },
                onDismiss = { announcementDialogState = null }
            )
        }

        // 无订阅提示（Compose M3 Dialog）
        if (uiState.showNoSubscriptionDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.setShowNoSubscriptionDialog(false) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.setShowNoSubscriptionDialog(false)
                            onNavigateToBuy()
                        }
                    ) {
                        Text("去购买")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.setShowNoSubscriptionDialog(false) }) {
                        Text("取消")
                    }
                },
                title = {
                    Text(
                        text = "提示",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "当前无有效订阅，请前往购买页面。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroHeaderSection(
    uiState: AccelerateUiState,
    onNavigateToNodeSelection: () -> Unit,
    onToggleConnection: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateToTest: () -> Unit,
    trafficState: com.xboard.ui.viewmodel.TrafficState
) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.tertiary
        ),
        start = Offset.Zero,
        end = Offset(600f, 300f)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 130.dp, max = 150.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .fillMaxSize()
                .background(gradientBrush)
        ) {

            // 波动动画（仅在连接时显示，作为背景装饰）
            if (uiState.isConnected) {
                CompactWaveformChart(
                    uploadSpeed = trafficState.uploadSpeed,
                    downloadSpeed = trafficState.downloadSpeed,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.8f)
                )
            }

            // 内容层
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (uiState.isConnected) "极速守护中" else "准备加速",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        Text(
                            text = uiState.subscriptionPlanName.ifBlank {
                                "未订阅"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconCircleButton(
                            icon = Icons.Rounded.Refresh,
                            contentDescription = "刷新页面",
                            onClick = onRefresh
                        )
                        IconCircleButton(
                            icon = Icons.Rounded.Speed,
                            contentDescription = "测试",
                            onClick = onNavigateToTest
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    MetricChip(
                        icon = Icons.Rounded.Speed,
                        label = "延迟",
                        value = "${
                            if (uiState.latency in 1..59999) {
                                uiState.latency
                            } else {
                                0
                            }
                        }ms"
                    )
                    MetricChip(icon = Icons.Rounded.Bolt, label = "模式", value = uiState.mode)
                    MetricChip(
                        icon = Icons.Rounded.Schedule,
                        label = "时长",
                        value = uiState.connectionTime
                    )
                }
            }
        }
    }
}

@Composable
private fun IconCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
        contentColor = MaterialTheme.colorScheme.onPrimary,
        tonalElevation = 0.dp,
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }
    }
}

@Composable
private fun MetricChip(
    icon: ImageVector,
    label: String,
    value: String
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.onPrimary,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .wrapContentWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SubscriptionInfoCard(planName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.CardMembership,
                contentDescription = "订阅",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "当前订阅：$planName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ConnectionInfoSection(
    uiState: AccelerateUiState,
    onNavigateToNodeSelection: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "连接状态",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = if (uiState.isConnected) "已连接" else "未连接",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (uiState.isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(onClick = onNavigateToNodeSelection)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    val iconResId = remember(uiState.nodeName) {
                        com.xboard.util.CountryIconMapper.getCountryIconResId(uiState.nodeName)
                    }
                    Icon(
                        painter = painterResource(id = iconResId),
                        contentDescription = "节点",
                        tint = Color.Unspecified, // 国家图标保持原始颜色
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.nodeName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                }
                // 圆点状态（仅在连接时显示延迟）
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 状态圆点
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (uiState.isConnected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                    )
                    // 仅在连接时显示延迟，未连接时显示箭头
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "查看详情",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SimpleNoticeCard(
    notice: Notice,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Campaign,
                contentDescription = "公告",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = notice.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (notice.content.isNotBlank()) {
                    Text(
                        text = notice.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "查看详情",
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoticeBannerCarousel(
    notices: List<Notice>,
    onNoticeClick: (Notice) -> Unit
) {
    if (notices.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { notices.size }
    )
    val scope = rememberCoroutineScope()

    // 自动轮播
    LaunchedEffect(pagerState.currentPage) {
        while (true) {
            delay(5000)
            if (notices.size > 1) {
                val nextPage = (pagerState.currentPage + 1) % notices.size
                scope.launch {
                    pagerState.animateScrollToPage(nextPage)
                }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(modifier = Modifier.height(120.dp)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val notice = notices[page]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onNoticeClick(notice) }
                ) {
                    // 背景图片（如果有）
                    if (notice.imgUrl.isNotBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                ImageRequest.Builder(LocalContext.current)
                                    .data(notice.imgUrl)
                                    .crossfade(true)
                                    .build()
                            ),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // 无图片时使用渐变背景
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                )
                        )
                    }
                    // 渐变遮罩
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                )
                            )
                    )
                    // 标题
                    Text(
                        text = notice.title,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 指示器
            if (notices.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(notices.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 8.dp else 6.dp)
                                .background(
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.onSurface
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnouncementSection(
    title: String = "系统公告",
    message: String = "高峰时段优先使用智能模式，保持连接更稳定。",
    actionLabel: String = "查看详情",
    onActionClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f),
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Rounded.Campaign, contentDescription = "公告")
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(
                onClick = { onActionClick?.invoke() },
                enabled = onActionClick != null
            ) {
                Text(text = actionLabel)
            }
        }
    }
}

@Composable
fun SubscriptionStatusBanner(
    isUpdating: Boolean,
    statusMessage: String,
    onRefresh: () -> Unit
) {
    // 只有在更新中或有错误消息时才显示（成功消息不显示）
    val isSuccess = statusMessage.contains("成功", ignoreCase = true)
    val isError = statusMessage.contains("失败", ignoreCase = true) ||
            statusMessage.contains("错误", ignoreCase = true)

    // 成功时不显示，只在更新中或错误时显示
    val shouldShow = isUpdating || (isError && statusMessage.isNotBlank())

    if (!shouldShow) {
        return
    }

    val containerColor = when {
        isSuccess -> MaterialTheme.colorScheme.primaryContainer
        isError -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val contentColor = when {
        isSuccess -> MaterialTheme.colorScheme.onPrimaryContainer
        isError -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        border = BorderStroke(
            1.dp,
            if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标/进度条
            AnimatedContent(
                targetState = isUpdating,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(
                        animationSpec = tween(
                            150
                        )
                    )
                },
                label = "icon-transition"
            ) { updating ->
                if (updating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = contentColor
                    )
                } else {
                    val icon = when {
                        isSuccess -> Icons.Rounded.CheckCircle
                        isError -> Icons.Rounded.Error
                        else -> Icons.Rounded.Info
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = contentColor
                    )
                }
            }

            // 消息文本
            Text(
                text = when {
                    isUpdating -> "订阅更新中..."
                    statusMessage.isNotBlank() -> statusMessage
                    else -> ""
                },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // 刷新按钮（仅在失败时显示）
            AnimatedVisibility(
                visible = isError && !isUpdating,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(150))
            ) {
                TextButton(
                    onClick = onRefresh,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "刷新",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "刷新",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

/**
 * 紧凑的波动图表，用作背景装饰
 */
@Composable
private fun CompactWaveformChart(
    uploadSpeed: Float,
    downloadSpeed: Float,
    modifier: Modifier = Modifier
) {
    val maxSpeed = 5120f
    val uploadRatio by animateFloatAsState(
        targetValue = (uploadSpeed / maxSpeed).coerceIn(0f, 1f),
        animationSpec = tween(300),
        label = "upload-ratio"
    )
    val downloadRatio by animateFloatAsState(
        targetValue = (downloadSpeed / maxSpeed).coerceIn(0f, 1f),
        animationSpec = tween(300),
        label = "download-ratio"
    )

    val brush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        )
    )

    Canvas(modifier = modifier) {
        val path = Path()
        val width = size.width
        val height = size.height

        path.moveTo(0f, height)

        // 使用更简单的波动效果，作为背景装饰
        for (i in 0..width.toInt() step 15) {
            val x = i.toFloat()
            val wave =
                (kotlin.math.sin(x * 0.03f + (uploadRatio + downloadRatio) * 5) * 0.3f + 0.3f) *
                        (uploadRatio + downloadRatio).coerceIn(0f, 1f)
            path.lineTo(x, height * (1 - wave * 0.4f))
        }

        path.lineTo(width, height)
        path.close()

        drawPath(path, brush = brush)
    }
}

@Composable
fun ModeSwitch(selectedMode: String, modes: List<String>, onModeSelected: (String) -> Unit) {
    val modeIcons: List<ImageVector> =
        listOf(Icons.Rounded.AutoAwesomeMotion, Icons.Rounded.Public)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        modes.forEach { mode ->
            val isSelected = selectedMode == mode
            val animatedWeight by animateFloatAsState(
                targetValue = if (isSelected) 1.4f else 1f,
                animationSpec = tween(300),
                label = "mode-weight"
            )
            val animatedColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                animationSpec = tween(300),
                label = "mode-color"
            )

            val animatedContentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Surface(
                modifier = Modifier
                    .weight(animatedWeight)
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
                color = animatedColor,
                contentColor = animatedContentColor,
                border = if (!isSelected) BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                ) else null,
                onClick = { onModeSelected(mode) },
                tonalElevation = if (isSelected) 2.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (mode == "智能") modeIcons[0] else modeIcons[1],
                        contentDescription = mode,
                        modifier = Modifier.size(20.dp),
                        tint = animatedContentColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = mode,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = animatedContentColor
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectButton(isConnected: Boolean, onClick: () -> Unit) {
    val gradientColors = if (isConnected) {
        listOf(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error)
    } else {
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
    }
    val contentColor = if (isConnected) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onPrimary
    }
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow-value"
    )

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        enabled = true,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(colors = gradientColors))
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isConnected,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(300)) togetherWith fadeOut(
                        animationSpec = tween(
                            300
                        )
                    ))
                },
                label = "connect-label"
            ) { targetState ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (targetState) Icons.Rounded.Stop else Icons.Rounded.Wifi,
                        contentDescription = if (targetState) "断开" else "连接",
                        tint = contentColor,
                        modifier = Modifier.size(36.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = if (targetState) "断开连接" else "立即加速",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AccelerateScreenPreview() {
    MaClashTheme {
        // Preview placeholder
    }
}
