package com.xboard.ui.compose

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.*
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xboard.ex.showToast
import com.xboard.model.OrderDetailResponse
import com.xboard.model.PaymentMethod
import com.xboard.storage.MMKVManager
import com.xboard.ui.viewmodel.OrderDetailViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

/**
 * 订单详情页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    tradeNo: String,
    viewModel: OrderDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 加载订单详情
    LaunchedEffect(tradeNo) {
        viewModel.loadOrderDetail(tradeNo)
    }

    // 处理错误消息
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            showToast(error)
            viewModel.clearError()
        }
    }

    // 处理打开浏览器
    val openBrowser: (String) -> Unit = { url ->
        try {
            MMKVManager.setOrderCacheUrl(url)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            showToast("无法打开浏览器")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "订单详情",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
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
                uiState.isLoading && uiState.order == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.error != null && uiState.order == null -> {
                    ErrorState(
                        message = uiState.error ?: "加载失败",
                        onRetry = { viewModel.loadOrderDetail(tradeNo) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                uiState.order != null -> {
                    OrderDetailContent(
                        order = uiState.order!!,
                        paymentMethods = uiState.paymentMethods,
                        selectedPaymentMethodId = uiState.selectedPaymentMethodId,
                        isLoadingPayment = uiState.isLoadingPayment || uiState.isPolling,
                        loadingMessage = uiState.loadingMessage,
                        onSelectPaymentMethod = { methodId ->
                            viewModel.selectPaymentMethod(methodId)
                        },
                        onStartPayment = {
                            viewModel.startPayment(tradeNo, openBrowser)
                        },
                        onCancelOrder = {
                            viewModel.cancelOrder(tradeNo)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderDetailContent(
    order: OrderDetailResponse,
    paymentMethods: List<PaymentMethod>,
    selectedPaymentMethodId: Int?,
    isLoadingPayment: Boolean,
    loadingMessage: String?,
    onSelectPaymentMethod: (Int) -> Unit,
    onStartPayment: () -> Unit,
    onCancelOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isWaitingPayment = order.status == OrderDetailResponse.STATUS_WAITING

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 订单状态卡片 - 添加进入动画
        item {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                    initialScale = 0.95f,
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            ) {
                OrderStatusCard(order = order)
            }
        }

        // 商品信息卡片 - 添加进入动画
        item {
            if (order.plan != null) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(300, delayMillis = 50)) + scaleIn(
                        initialScale = 0.95f,
                        animationSpec = tween(300, delayMillis = 50, easing = FastOutSlowInEasing)
                    )
                ) {
                    PlanInfoCard(order = order)
                }
            }
        }

        // 订单信息卡片 - 添加进入动画
        item {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(300, delayMillis = 100)) + scaleIn(
                    initialScale = 0.95f,
                    animationSpec = tween(300, delayMillis = 100, easing = FastOutSlowInEasing)
                )
            ) {
                OrderInfoCard(order = order)
            }
        }

        // 价格明细卡片（仅待支付状态显示）- 添加进入动画
        item {
            if (isWaitingPayment && order.plan != null) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(300, delayMillis = 150)) + scaleIn(
                        initialScale = 0.95f,
                        animationSpec = tween(300, delayMillis = 150, easing = FastOutSlowInEasing)
                    )
                ) {
                    PriceDetailCard(order = order)
                }
            }
        }

        // 支付方式选择（仅待支付状态显示）
        item {
            AnimatedVisibility(
                visible = isWaitingPayment,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                PaymentMethodCard(
                    methods = paymentMethods,
                    selectedMethodId = selectedPaymentMethodId,
                    onSelectMethod = onSelectPaymentMethod
                )
            }
        }

        // 操作按钮（仅待支付状态显示）
        item {
            AnimatedVisibility(
                visible = isWaitingPayment,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                ActionButtonsCard(
                    isLoadingPayment = isLoadingPayment,
                    loadingMessage = loadingMessage,
                    onStartPayment = onStartPayment,
                    onCancelOrder = onCancelOrder
                )
            }
        }

        // 底部间距
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun OrderStatusCard(order: OrderDetailResponse) {
    val (statusText, statusColor) = getOrderStatusInfo(order.status)
    val subStatusText = when (order.status) {
        OrderDetailResponse.STATUS_LOADING -> "订单已支付，正在开通中。"
        OrderDetailResponse.STATUS_PAID,
        OrderDetailResponse.STATUS_DISCOUNT -> "订单已支付并开通。"
        OrderDetailResponse.STATUS_CANCELED -> "订单已取消。"
        else -> ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = when (order.status) {
                    OrderDetailResponse.STATUS_PAID,
                    OrderDetailResponse.STATUS_LOADING,
                    OrderDetailResponse.STATUS_DISCOUNT -> Icons.Rounded.CheckCircle
                    OrderDetailResponse.STATUS_CANCELED -> Icons.Rounded.Cancel
                    else -> Icons.Rounded.Pending
                },
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = statusColor
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
            if (subStatusText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subStatusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PlanInfoCard(order: OrderDetailResponse) {
    val plan = order.plan ?: return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "商品信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            PlanInfoRow("商品名称", plan.name)
            PlanInfoRow("计费周期", getPeriodLabel(order.period))
            PlanInfoRow(
                "流量限制",
                if (plan.transferEnable >= Integer.MAX_VALUE) {
                    "无限制"
                } else {
                    "${plan.transferEnable} GB"
                }
            )
        }
    }
}

@Composable
private fun PlanInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun OrderInfoCard(order: OrderDetailResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "订单信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            PlanInfoRow("订单编号", order.tradeNo)
            PlanInfoRow("下单时间", formatTime(order.createdAt))
        }
    }
}

@Composable
private fun PriceDetailCard(order: OrderDetailResponse) {
    val plan = order.plan ?: return
    val planPrice = plan.getRealPlanPrice(order.period)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "价格明细",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            // 商品原价
            PriceRow("商品原价", planPrice, showDiscount = false)

            // 折扣金额
            if ((order.discountAmount ?: 0.0) > 0.0) {
                PriceRow("折扣优惠", order.discountAmount ?: 0.0, showDiscount = true)
            }

            // 余额抵扣
            if ((order.balanceAmount ?: 0.0) > 0.0) {
                PriceRow("余额抵扣", order.balanceAmount ?: 0.0, showDiscount = true)
            }

            // 余量抵扣
            if ((order.surplusAmount ?: 0.0) > 0.0) {
                PriceRow("余量抵扣", order.surplusAmount ?: 0.0, showDiscount = true)
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.5.dp)

            // 实付金额
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "实付金额",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "¥${formatPrice(order.totalAmount ?: 0.0)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PriceRow(label: String, amount: Double, showDiscount: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (showDiscount) "-¥${formatPrice(amount)}" else "¥${formatPrice(amount)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (showDiscount) FontWeight.Normal else FontWeight.Medium,
            color = if (showDiscount) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun PaymentMethodCard(
    methods: List<PaymentMethod>,
    selectedMethodId: Int?,
    onSelectMethod: (Int) -> Unit
) {
    if (methods.isEmpty()) {
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "支付方式",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            methods.forEach { method ->
                PaymentMethodItem(
                    method = method,
                    isSelected = method.id == selectedMethodId,
                    onClick = { onSelectMethod(method.id) }
                )
            }
        }
    }
}

@Composable
private fun PaymentMethodItem(
    method: PaymentMethod,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = method.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "手续费: ${(method.feePercent * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun ActionButtonsCard(
    isLoadingPayment: Boolean,
    loadingMessage: String?,
    onStartPayment: () -> Unit,
    onCancelOrder: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 加载提示
            AnimatedVisibility(
                visible = isLoadingPayment && loadingMessage != null,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(150))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = loadingMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 支付按钮
            Button(
                onClick = onStartPayment,
                enabled = !isLoadingPayment,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("立即支付")
            }

            // 取消订单按钮
            OutlinedButton(
                onClick = onCancelOrder,
                enabled = !isLoadingPayment,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("取消订单")
            }
        }
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}

@Composable
private fun getOrderStatusInfo(status: Int): Pair<String, Color> {
    return when (status) {
        OrderDetailResponse.STATUS_LOADING -> "开通中" to MaterialTheme.colorScheme.primary
        OrderDetailResponse.STATUS_PAID -> "已完成" to MaterialTheme.colorScheme.primary
        OrderDetailResponse.STATUS_WAITING -> "待支付" to MaterialTheme.colorScheme.tertiary
        OrderDetailResponse.STATUS_CANCELED -> "已取消" to MaterialTheme.colorScheme.onSurfaceVariant
        OrderDetailResponse.STATUS_DISCOUNT -> "已折抵" to MaterialTheme.colorScheme.onSurfaceVariant
        else -> "未知" to MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private val priceFormatter = DecimalFormat("#.##")
private fun formatPrice(amount: Double): String {
    val rounded = (max(amount, 0.0)) / 100.0
    return priceFormatter.format(rounded)
}

private fun formatTime(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sdf.format(Date(timestamp * 1000))
    } catch (e: Exception) {
        "未知时间"
    }
}

private fun getPeriodLabel(period: String?): String = when (period) {
    "month_price", "month" -> "月付"
    "quarter_price", "quarter" -> "季付"
    "half_year_price", "half_year" -> "半年付"
    "year_price", "year" -> "年付"
    else -> "灵活周期"
}

