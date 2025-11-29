package com.xboard.ui.activity

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.github.kr328.clash.AccessControlActivity
import com.github.kr328.clash.R
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.databinding.ActivityOtherSettingsBinding
import com.github.kr328.clash.service.model.AccessControlMode
import com.github.kr328.clash.service.store.ServiceStore
import com.github.kr328.clash.util.startClashService
import com.github.kr328.clash.util.stopClashService
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.xboard.api.RetrofitClient
import com.xboard.base.BaseActivity
import com.xboard.ex.showToast
import com.xboard.model.UpdateUserRequest
import com.xboard.network.UserRepository
import com.xboard.storage.MMKVManager
import com.xboard.utils.onClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 其他设置页面
 */
class OtherSettingsActivity : BaseActivity<ActivityOtherSettingsBinding>() {
    val service by lazy {
        ServiceStore(this@OtherSettingsActivity)
    }
    private val userRepository by lazy { UserRepository(RetrofitClient.getApiService()) }

    override fun getViewBinding(): ActivityOtherSettingsBinding {
        return ActivityOtherSettingsBinding.inflate(layoutInflater)
    }

    override fun initView() {
        launch {

            val selected = withContext(Dispatchers.IO) {
                service.accessControlPackages.toMutableSet()
            }

            defer {
                withContext(Dispatchers.IO) {
                    val changed = selected != service.accessControlPackages
                    service.accessControlPackages = selected
                    if (clashRunning && changed) {
                        stopClashService()
                        while (clashRunning) {
                            delay(200)
                        }
                        startClashService()
                    }
                }
            }
        }
        setupUI()
    }

    override fun initData() {
        loadSettings()
    }

    private fun setupUI() {
        // 返回按钮
        binding.vBack.setOnClickListener {
            finish()
        }

        // 访问控制模式
        binding.itemAccessControl.setOnClickListener {

            if (clashRunning) {
                launch {
                    showToast("请先断开连接后再设置")
                }
            }
            showAccessControlModeBottomSheet()
        }
        // 主题设置
//        binding.itemThemeSettings.setOnClickListener {
//            startActivity(Intent(this, ThemeSettingsActivity::class.java))
//        }

        // 修改密码
        binding.itemChangePassword.setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }

        // 初始化通知开关
        binding.switchExpireNotification.isChecked = MMKVManager.getExpireNotification()
        binding.switchTrafficNotification.isChecked = MMKVManager.getTrafficNotification()

        // 到期邮件提醒
        binding.vExpireNotification.onClick {
            updateNotificationSettings("expire", !binding.switchExpireNotification.isChecked)
        }
        binding.vTrafficNotification.onClick {
            // 流量邮件提醒
            updateNotificationSettings("traffic", !binding.switchTrafficNotification.isChecked)
        }

        // 当前版本
        binding.vCurrentVersion.onClick {
            showVersionInfo()
        }

        // 服务协议
        binding.vAgreement.onClick {
            openServiceAgreement()
        }

        // 隐私协议
        binding.vPrivacy.onClick {
            openPrivacyPolicy()
        }

        // 初始化访问控制模式显示
        updateAccessControlModeDisplay()

    }

    private fun loadSettings() {
        val userInfo = MMKVManager.getUserInfo()
        // 从本地存储加载设置
        val expireNotification = userInfo?.remindExpire ?: true
        val trafficNotification = userInfo?.remindTraffic ?: true
        binding.switchExpireNotification.isChecked = expireNotification
        binding.switchTrafficNotification.isChecked = trafficNotification
    }

    private fun updateNotificationSettings(type: String, enabled: Boolean) {
        lifecycleScope.launch {
            try {
                // 调用 API 更新用户设置
                val result = when (type) {
                    "expire" ->
                        userRepository.updateUserInfo(UpdateUserRequest(remind_expire = if (enabled) 1 else 0))

                    "traffic" ->
                        userRepository.updateUserInfo(UpdateUserRequest(remind_traffic = if (enabled) 1 else 0))

                    else -> return@launch
                }

                result
                    .onSuccess {
                        // 保存到本地存储
                        when (type) {
                            "expire" -> {
                                val userInfo = MMKVManager.getUserInfo()
                                userInfo?.remindExpire = enabled
                                MMKVManager.setUserInfo(userInfo)
                            }

                            "traffic" -> {
                                val userInfo = MMKVManager.getUserInfo()
                                userInfo?.remindTraffic = enabled
                                MMKVManager.setUserInfo(userInfo)
                            }
                        }
                        showToast("设置已保存")
                        loadSettings()
                    }
                    .onError { error ->
                        loadSettings()
                        showToast("设置失败: ${error.message}")
                    }
            } catch (e: Exception) {
                // 恢复开关状态
                loadSettings()
                showToast("设置失败: ${e.message}")
            }
        }
    }

    /**
     * 显示访问控制模式 BottomSheet
     */
    private fun showAccessControlModeBottomSheet() {
        val bottomSheet = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_access_control, null)
        bottomSheet.setContentView(view)

        // 设置选项点击事件
        val option1 = view.findViewById<View>(R.id.option_allow_all)
        val option2 = view.findViewById<View>(R.id.option_allow_selected)
        val option3 = view.findViewById<View>(R.id.option_deny_selected)
        val voption1 = view.findViewById<View>(R.id.v_option1)
        val voption2 = view.findViewById<View>(R.id.v_option2)
        val voption3 = view.findViewById<View>(R.id.v_option3)
        val vCancel = view.findViewById<View>(R.id.v_cancel)
        val vConfirm = view.findViewById<View>(R.id.v_confirm)
        val mode = service::accessControlMode.get()
        when (mode) {
            AccessControlMode.AcceptAll -> {
                voption1.isSelected = true
                voption2.isSelected = false
                voption3.isSelected = false
            }

            AccessControlMode.AcceptSelected -> {
                voption1.isSelected = false
                voption2.isSelected = true
                voption3.isSelected = false
            }

            AccessControlMode.DenySelected -> {
                voption1.isSelected = false
                voption2.isSelected = false
                voption3.isSelected = true
            }
        }
        option1?.setOnClickListener {
            voption1.isSelected = true
            voption2.isSelected = false
            voption3.isSelected = false
        }

        option2?.setOnClickListener {
            voption1.isSelected = false
            voption2.isSelected = true
            voption3.isSelected = false
        }

        option3?.setOnClickListener {
            voption1.isSelected = false
            voption2.isSelected = false
            voption3.isSelected = true
        }
        vCancel.onClick {
            bottomSheet.dismiss()
        }
        vConfirm.onClick {
            when {
                voption1.isSelected -> {
                    service::accessControlMode.set(AccessControlMode.AcceptAll)
                }

                voption2.isSelected -> {
                    service::accessControlMode.set(AccessControlMode.AcceptSelected)
                }

                voption3.isSelected -> {
                    service::accessControlMode.set(AccessControlMode.DenySelected)
                }
            }
            bottomSheet.dismiss()
            updateAccessControlModeDisplay()
        }
        bottomSheet.show()
    }


    /**
     * 更新访问控制模式显示
     * 仅当不是"允许所有应用"时才显示访问控制应用包列表
     */
    private fun updateAccessControlModeDisplay() {
        val mode = service::accessControlMode.get()


        // 条件显示访问控制应用包列表
        // 仅当模式不是"允许所有应用"时才显示
        if (mode == AccessControlMode.AcceptAll) {
            binding.itemAccessControlPackages.visibility = View.GONE
        } else {
            binding.itemAccessControlPackages.visibility = View.VISIBLE
            binding.itemAccessControlPackages.onClick {
                if (clashRunning) {
                    showToast("请先断开连接后再设置")
                    return@onClick
                }
                // 跳转到访问控制应用包列表页面
                startActivity(AccessControlActivity::class.intent)
            }
        }
        // 更新访问控制模式的描述文本
        val modeText = when (mode) {
            AccessControlMode.AcceptAll -> "允许所有应用"
            AccessControlMode.AcceptSelected -> "仅允许已选择的应用"
            AccessControlMode.DenySelected -> "不允许已选择的应用"
            else -> "允许所有应用"
        }

        binding.tvAccessControlMode.text = modeText
    }

    private fun showVersionInfo() {
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            "1.0.0"
        }
        showToast("当前版本: $versionName")
    }

    private fun openServiceAgreement() {
        startActivity(Intent(this, AgreementActivity::class.java).apply {
            putExtra(AgreementActivity.EXTRA_TYPE, AgreementActivity.TYPE_USER_AGREEMENT)
        })
    }

    private fun openPrivacyPolicy() {
        startActivity(Intent(this, AgreementActivity::class.java).apply {
            putExtra(AgreementActivity.EXTRA_TYPE, AgreementActivity.TYPE_PRIVACY_POLICY)
        })
    }
}