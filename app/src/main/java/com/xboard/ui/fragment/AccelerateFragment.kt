package com.xboard.ui.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.github.kr328.clash.util.startClashService
import com.xboard.api.RetrofitClient
import com.xboard.event.OrderPayEvent
import com.xboard.ex.showToast
import com.xboard.network.UserRepository
import com.xboard.storage.MMKVManager
import com.xboard.ui.activity.MainActivity
import com.xboard.ui.activity.NodeSelectionActivity
import com.xboard.ui.compose.AccelerateScreen
import com.xboard.ui.theme.MaClashTheme
import com.xboard.event.ThemeChangedEvent
import com.xboard.ui.viewmodel.AccelerateViewModel
import com.xboard.ui.viewmodel.ThemeViewModel
import com.xboard.ui.dialog.DialogHelper
import com.xboard.utils.ThemeHelper
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class AccelerateFragment : Fragment() {

    companion object {
        private const val TAG = "AccelerateFragment"
    }

    private val userRepository by lazy { UserRepository(RetrofitClient.getApiService()) }

    private val viewModel: AccelerateViewModel by viewModels {
        AccelerateViewModel.Factory(
            requireActivity().application,
            userRepository
        )
    }
    
    private val themeViewModel = ThemeViewModel.getInstance()

    private val nodeSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.loadCurrentNodeInfo()
        }
    }

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            requireContext().startClashService()
        } else {
            Toast.makeText(requireContext(), "VPN 权限被拒绝，无法连接", Toast.LENGTH_SHORT).show()
        }
    }

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
                    AccelerateScreen(
                        viewModel = viewModel,
                        onNavigateToNodeSelection = { navigateToNodeSelection() },
                        onToggleConnection = { handleToggleConnection() },
                        onRefreshSubscription = { 
                            // 先刷新订阅，获取最新订阅信息和节点列表
                            // 订阅刷新完成后，再刷新页面（包括节点验证）
                            fetchSubscriptionUpdates()
                        },
                    onShowAnnouncement = { }, // 对话框现在在 AccelerateScreen 内部管理
                    onNavigateToBuy = { switchToBuyTab() },
                    onNavigateToTest = { navigateToTest() }
                    )
                }
            }
        }
    }
    
    // 基类已通过 EventBus 监听主题变化，无需在 onResume 中处理

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchSubscriptionUpdates()
        viewModel.loadAnnouncements()
    }

    override fun onStart() {
        super.onStart()
        // 注册 EventBus 监听主题变化事件和支付事件
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        // 重新加载主题设置，确保从其他页面返回时主题正确
        ThemeHelper.loadThemeSettings(themeViewModel, resources)
        // 同步VPN连接状态，避免状态不一致导致的问题
        viewModel.syncConnectionState()
    }

    override fun onStop() {
        super.onStop()
        // 注销 EventBus
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }

    private fun navigateToNodeSelection() {
        val intent = Intent(context, NodeSelectionActivity::class.java)
        nodeSelectionLauncher.launch(intent)
    }
    
    private fun navigateToTest() {
        try {
            val intent = Intent(activity, com.github.kr328.clash.MainActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            showToast("无法打开测试页面")
        }
    }

    fun requestVpnPermission() {
        val vpnRequest = android.net.VpnService.prepare(requireContext())
        if (vpnRequest != null) {
            vpnPermissionLauncher.launch(vpnRequest)
        } else {
            requireContext().startClashService()
        }
    }

    private fun handleToggleConnection() {
        when {
            viewModel.uiState.value.isSubscriptionUpdating -> {
                showToast("节点更新中，请稍后再试")
            }
            MMKVManager.getSubscribe() == null -> {
                viewModel.setShowNoSubscriptionDialog(true)
            }
            else -> {
                viewModel.toggleConnectionState(this)
            }
        }
    }

    private fun fetchSubscriptionUpdates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.refreshSubscription { success ->
                if (!success) {
                    Log.w(TAG, "Subscription refresh failed")
                }
                // 订阅刷新完成后（无论成功或失败），刷新页面并验证节点
                // 这样即使订阅刷新失败，也能使用已有的订阅信息验证节点
                viewModel.refreshPage()
            }
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
    
    /**
     * 监听支付成功事件，刷新订阅和节点信息
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrderPayEvent(event: OrderPayEvent) {
        fetchSubscriptionUpdates()
    }

    private fun showNoSubscriptionDialog() {
        DialogHelper.showConfirmDialog(
            fragment = this,
            title = "提示",
            message = "当前无任何有效订阅，请前往购买",
            positiveButtonText = "好的",
            negativeButtonText = "取消",
            showNegativeButton = true,
            onPositiveClick = { switchToBuyTab() }
        )
    }

    private fun switchToBuyTab() {
        val activity = requireActivity()
        if (activity is MainActivity) {
            activity.setCurrentTab(1)
        } else {
            Toast.makeText(requireContext(), "请前往购买页面", Toast.LENGTH_SHORT).show()
        }
    }
}