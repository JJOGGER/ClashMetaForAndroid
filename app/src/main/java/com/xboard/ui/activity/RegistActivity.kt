package com.xboard.ui.activity

import android.content.Intent
import android.os.CountDownTimer
import android.util.Patterns
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.github.kr328.clash.databinding.ActivityRegistBinding
import com.xboard.api.RetrofitClient
import com.xboard.api.TokenManager
import com.xboard.base.BaseActivity
import com.xboard.network.AuthRepository
import com.xboard.storage.MMKVManager
import com.xboard.utils.onClick
import kotlinx.coroutines.launch

/**
 * 登录/注册页面
 */
class RegistActivity : BaseActivity<ActivityRegistBinding>() {

    private val authRepository by lazy { AuthRepository(RetrofitClient.getApiService()) }
    private var countDownTimer: CountDownTimer? = null

    override fun getViewBinding(): ActivityRegistBinding {
        return ActivityRegistBinding.inflate(layoutInflater)
    }

    override fun initView() {
    }

    override fun initListener() {
        setupListeners()
    }

    private fun setupListeners() {
        binding.vBack.onClick { finish() }
        binding.btnRegist.setOnClickListener {
            performRegister()
        }

        binding.tvToggleMode.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.btnSendCode.setOnClickListener {
            sendEmailVerifyCode()
        }

        binding.tvUserAgreement.setOnClickListener {
            startActivity(Intent(this, AgreementActivity::class.java).apply {
                putExtra(AgreementActivity.EXTRA_TYPE, AgreementActivity.TYPE_USER_AGREEMENT)
            })
        }

        binding.tvPrivacyPolicy.setOnClickListener {
            startActivity(Intent(this, AgreementActivity::class.java).apply {
                putExtra(AgreementActivity.EXTRA_TYPE, AgreementActivity.TYPE_PRIVACY_POLICY)
            })
        }
    }

    private fun performRegister() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val emailCode = binding.etEmailCode.text.toString().trim()
        val inviteCode = binding.etInviteCode.text.toString().trim()

        if (!validateInput(email, password)) return
        if (emailCode.isEmpty()) {
            showError("请输入邮箱验证码")
            return
        }
        if (!binding.cbAgreement.isChecked) {
            showError("请勾选用户协议和隐私政策")
            return
        }

        lifecycleScope.launch {
            showLoading("注册中...")
            val result =
                authRepository.register(email, password, emailCode, inviteCode.ifEmpty { null })
            hideLoading()

            result.onSuccess { response ->
                TokenManager.saveToken(response.token, response.authData, email, password)
                showSuccess("注册成功")
                startActivity(Intent(this@RegistActivity, MainActivity::class.java))
                finish()
            }.onError { error ->
                showError(error.message)
            }
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            Toast.makeText(this, "请输入邮箱", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "邮箱格式不正确", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.isEmpty()) {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.length < 6) {
            Toast.makeText(this, "密码至少6位", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun sendEmailVerifyCode() {
        val email = binding.etEmail.text.toString().trim()

        if (email.isEmpty()) {
            showError("请输入邮箱")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("邮箱格式不正确")
            return
        }

        binding.btnSendCode.isEnabled = false

        lifecycleScope.launch {
            val result = authRepository.sendEmailVerifyCode(email, "register")

            result.onSuccess {
                showSuccess("验证码已发送")
                startCodeCountdown()
            }.onError { error ->
                showError(error.message)
                binding.btnSendCode.isEnabled = true
            }
        }
    }

    private fun startCodeCountdown() {
        // 取消之前的计时器
        countDownTimer?.cancel()

        binding.btnSendCode.isEnabled = false
        var timeLeft = 60

        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeft--
                binding.btnSendCode.text = "${timeLeft}s"
                binding.btnSendCode.isEnabled = false
            }

            override fun onFinish() {
                binding.btnSendCode.text = "发送验证码"
                binding.btnSendCode.isEnabled = true
                countDownTimer = null
            }
        }
        countDownTimer?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        countDownTimer = null
    }
}
