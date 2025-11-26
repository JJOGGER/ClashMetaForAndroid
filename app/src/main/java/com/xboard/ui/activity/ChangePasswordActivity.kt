package com.xboard.ui.activity

import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.github.kr328.clash.databinding.ActivityChangePasswordBinding
import com.xboard.api.RetrofitClient
import com.xboard.base.BaseActivity
import com.xboard.network.AuthRepository
import com.xboard.widget.LoadingDialog
import kotlinx.coroutines.launch

/**
 * 修改密码页面
 */
class ChangePasswordActivity : BaseActivity<ActivityChangePasswordBinding>() {

    private val authRepository by lazy { AuthRepository(RetrofitClient.getApiService()) }
    private val loadingDialog by lazy { LoadingDialog(this) }

    override fun getViewBinding(): ActivityChangePasswordBinding {
        return ActivityChangePasswordBinding.inflate(layoutInflater)
    }

    override fun initView() {
        setupUI()
    }

    private fun setupUI() {
        // 返回按钮
        binding.vBack.setOnClickListener {
            finish()
        }

        // 提交按钮
        binding.btnSubmit.setOnClickListener {
            changePassword()
        }
    }

    private fun changePassword() {
        val oldPassword = binding.etOldPassword.text.toString().trim()
        val newPassword = binding.etNewPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        // 验证输入
        if (oldPassword.isEmpty()) {
            showToast("请输入旧密码")
            return
        }

        if (newPassword.isEmpty()) {
            showToast("请输入新密码")
            return
        }

        if (confirmPassword.isEmpty()) {
            showToast("请确认新密码")
            return
        }

        if (newPassword.length < 6) {
            showToast("新密码至少6位")
            return
        }

        if (newPassword != confirmPassword) {
            showToast("两次输入的密码不一致")
            return
        }

        if (oldPassword == newPassword) {
            showToast("新密码不能与旧密码相同")
            return
        }

        // 提交修改
        loadingDialog.show()
        lifecycleScope.launch {
            val result = authRepository.changePassword(oldPassword, newPassword)

            result
                .onSuccess {
                    loadingDialog.dismiss()
                    showToast("密码修改成功")
                    finish()
                }
                .onError { error ->
                    loadingDialog.dismiss()
                    showToast("修改失败: ${error.message}")
                }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
