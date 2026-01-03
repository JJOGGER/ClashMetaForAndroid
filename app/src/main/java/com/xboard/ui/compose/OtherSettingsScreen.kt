package com.xboard.ui.compose

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.xboard.ui.theme.Purple40
import com.xboard.ui.theme.BluePrimary
import com.xboard.ui.theme.GreenPrimary
import com.xboard.ui.theme.OrangePrimary
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.AccessControlActivity
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.service.model.AccessControlMode
import com.xboard.ui.activity.AgreementActivity
import com.xboard.ui.activity.ChangePasswordActivity
import com.xboard.ui.theme.MaClashTheme
import com.xboard.ui.viewmodel.ColorSchemeSelection
import com.xboard.ui.viewmodel.OtherSettingsViewModel
import com.xboard.ui.viewmodel.OtherSettingsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherSettingsScreen(
    viewModel: OtherSettingsViewModel,
    onNavigateBack: () -> Unit,
    onThemeChanged: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "其他设置",
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
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(innerPadding)
//                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                .verticalScroll(rememberScrollState())
        ) {
            // 顶部固定间距，避免主题切换时跳变
//            Spacer(modifier = Modifier.height(8.dp))
            
            // 网络
            SettingsSection(title = "网络") {
                // 访问控制模式
                SettingsItem(
                    title = "访问控制模式",
                    subtitle = viewModel.getAccessControlModeText(uiState.accessControlMode),
                    onClick = {
                        // 检查是否需要先断开连接
                        if (false) { // TODO: 检查连接状态
                            // 显示提示
                        } else {
                            viewModel.showAccessControlModeDialog()
                        }
                    }
                )
                
                // 访问控制应用包列表（条件显示）
                if (viewModel.shouldShowAccessControlPackages()) {
                    SettingsItem(
                        title = "访问控制应用包列表",
                        onClick = {
                            context.startActivity(AccessControlActivity::class.intent)
                        }
                    )
                }
            }
            
            // 个性化
            SettingsSection(title = "个性化") {
                // 主题颜色
                ThemeColorSelector(
                    selectedScheme = uiState.colorScheme,
                    onSchemeSelected = { scheme ->
                        viewModel.updateColorScheme(scheme)
                        onThemeChanged()
                    }
                )
                
                // 暗黑模式
                SettingsItemWithSwitch(
                    title = "暗黑模式",
                    checked = uiState.isDarkMode,
                    onCheckedChange = { isDark ->
                        viewModel.toggleDarkMode(isDark)
                        onThemeChanged()
                    }
                )
                
                // 修改密码
                SettingsItem(
                    title = "修改密码",
                    onClick = {
                        context.startActivity(Intent(context, ChangePasswordActivity::class.java))
                    }
                )
                
                // 到期邮件提醒
                SettingsItemWithSwitch(
                    title = "到期邮件提醒",
                    checked = uiState.expireNotification,
                    onCheckedChange = { checked ->
                        viewModel.updateNotificationSettings("expire", checked)
                    }
                )
                
                // 流量邮件提醒
                SettingsItemWithSwitch(
                    title = "流量邮件提醒",
                    checked = uiState.trafficNotification,
                    onCheckedChange = { checked ->
                        viewModel.updateNotificationSettings("traffic", checked)
                    }
                )
            }
            
            // 关于
            SettingsSection(title = "关于") {
                val versionName = remember {
                    try {
                        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                        pInfo.versionName
                    } catch (e: PackageManager.NameNotFoundException) {
                        "1.0.0"
                    }
                }
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "当前版本",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = versionName.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                SettingsItem(
                    title = "服务协议",
                    onClick = {
                        context.startActivity(Intent(context, AgreementActivity::class.java).apply {
                            putExtra(AgreementActivity.EXTRA_TYPE, AgreementActivity.TYPE_USER_AGREEMENT)
                        })
                    }
                )
                
                SettingsItem(
                    title = "隐私协议",
                    onClick = {
                        context.startActivity(Intent(context, AgreementActivity::class.java).apply {
                            putExtra(AgreementActivity.EXTRA_TYPE, AgreementActivity.TYPE_PRIVACY_POLICY)
                        })
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // 访问控制模式对话框
    uiState.showAccessControlDialog?.let { showDialog ->
        if (showDialog) {
            AccessControlModeDialog(
                currentMode = uiState.accessControlMode,
                onModeSelected = { mode ->
                    viewModel.updateAccessControlMode(mode)
                    viewModel.hideAccessControlModeDialog()
                },
                onDismiss = {
                    viewModel.hideAccessControlModeDialog()
                }
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    showArrow: Boolean = true
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
            if (showArrow) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsItemWithSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun ThemeColorSelector(
    selectedScheme: ColorSchemeSelection,
    onSchemeSelected: (ColorSchemeSelection) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "主题颜色",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ThemeColorOption(
                        scheme = ColorSchemeSelection.PURPLE,
                        color = Purple40,
                        isSelected = selectedScheme == ColorSchemeSelection.PURPLE,
                        onClick = { onSchemeSelected(ColorSchemeSelection.PURPLE) }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ThemeColorOption(
                        scheme = ColorSchemeSelection.BLUE,
                        color = BluePrimary,
                        isSelected = selectedScheme == ColorSchemeSelection.BLUE,
                        onClick = { onSchemeSelected(ColorSchemeSelection.BLUE) }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ThemeColorOption(
                        scheme = ColorSchemeSelection.GREEN,
                        color = GreenPrimary,
                        isSelected = selectedScheme == ColorSchemeSelection.GREEN,
                        onClick = { onSchemeSelected(ColorSchemeSelection.GREEN) }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ThemeColorOption(
                        scheme = ColorSchemeSelection.ORANGE,
                        color = OrangePrimary,
                        isSelected = selectedScheme == ColorSchemeSelection.ORANGE,
                        onClick = { onSchemeSelected(ColorSchemeSelection.ORANGE) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeColorOption(
    scheme: ColorSchemeSelection,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = MaterialTheme.shapes.medium,
        color = color,
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                3.dp,
                MaterialTheme.colorScheme.primary
            )
        } else {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        },
        tonalElevation = if (isSelected) 4.dp else 1.dp,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = scheme.name,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun AccessControlModeDialog(
    currentMode: AccessControlMode,
    onModeSelected: (AccessControlMode) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(currentMode) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "访问控制模式",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AccessControlModeOption(
                    mode = AccessControlMode.AcceptAll,
                    title = "允许所有应用",
                    isSelected = selectedMode == AccessControlMode.AcceptAll,
                    onClick = { selectedMode = AccessControlMode.AcceptAll }
                )
                AccessControlModeOption(
                    mode = AccessControlMode.AcceptSelected,
                    title = "仅允许已选择的应用",
                    isSelected = selectedMode == AccessControlMode.AcceptSelected,
                    onClick = { selectedMode = AccessControlMode.AcceptSelected }
                )
                AccessControlModeOption(
                    mode = AccessControlMode.DenySelected,
                    title = "不允许已选择的应用",
                    isSelected = selectedMode == AccessControlMode.DenySelected,
                    onClick = { selectedMode = AccessControlMode.DenySelected }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onModeSelected(selectedMode) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun AccessControlModeOption(
    mode: AccessControlMode,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        shape = MaterialTheme.shapes.small,
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

