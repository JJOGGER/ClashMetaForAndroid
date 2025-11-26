package com.xboard.ui.activity

import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.github.kr328.clash.databinding.ActivityForgotPasswordBinding
import com.xboard.base.BaseActivity
import com.xboard.network.AuthRepository
import com.xboard.api.RetrofitClient
import com.xboard.ex.showToast
import kotlinx.coroutines.launch

/**
 * 忘记密码页面
 */
class ForgotPasswordActivity : BaseActivity<ActivityForgotPasswordBinding>() {

    private val authRepository by lazy { AuthRepository(RetrofitClient.getApiService()) }
    private var countdownSeconds = 0

    override fun getViewBinding(): ActivityForgotPasswordBinding {
        return ActivityForgotPasswordBinding.inflate(layoutInflater)
    }

    override fun getStatusBarColor(): Int {
        return android.graphics.Color.TRANSPARENT
    }

    override fun initListener() {
        binding.btnSendCode.setOnClickListener {
            sendVerificationCode()
        }

        binding.btnReset.setOnClickListener {
            resetPassword()
        }

        binding.ivBack.setOnClickListener {
            finish()
        }
    }

    private fun sendVerificationCode() {
        val email = binding.etEmail.text.toString().trim()

        if (email.isEmpty()) {
            showError("请输入邮箱")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("邮箱格式不正确")
            return
        }

        binding.btnSendCode.isEnabled = false

        lifecycleScope.launch {
            val result = authRepository.sendEmailVerifyCode(email)

            result
                .onSuccess {
                    showSuccess("验证码已发送")
                    binding.tilCode.visibility = View.VISIBLE
                    binding.tilNewPassword.visibility = View.VISIBLE
                    binding.btnReset.visibility = View.VISIBLE
                    startCountdown()
                }
                .onError { error ->
                    showError(error.message)
                    binding.btnSendCode.isEnabled = true
                }
        }
    }

    private fun startCountdown() {
        countdownSeconds = 60
        updateCountdownButton()
    }

    private fun updateCountdownButton() {
        if (countdownSeconds > 0) {
            binding.btnSendCode.text = "${countdownSeconds}s"
            binding.btnSendCode.isEnabled = false
            countdownSeconds--
            binding.btnSendCode.postDelayed({
                updateCountdownButton()
            }, 1000)
        } else {
            binding.btnSendCode.text = "重新发送"
            binding.btnSendCode.isEnabled = true
        }
    }

    private fun resetPassword() {
        val email = binding.etEmail.text.toString().trim()
        val code = binding.etCode.text.toString().trim()
        val newPassword = binding.etNewPassword.text.toString().trim()

        if (email.isEmpty() || code.isEmpty() || newPassword.isEmpty()) {
            showError("请填写所有字段")
            return
        }

        if (newPassword.length < 6) {
            showError("密码至少6位")
            return
        }

        lifecycleScope.launch {
            showLoading("重置中...")
            val result = authRepository.forgetPassword(email, code, newPassword)
            hideLoading()

            result
                .onSuccess {
                    showSuccess("密码重置成功")
                    finish()
                }
                .onError { error ->
                    showToast(error.message)
                }
        }
    }


}
