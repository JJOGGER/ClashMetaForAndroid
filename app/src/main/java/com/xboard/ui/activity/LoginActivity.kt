package com.xboard.ui.activity

import android.content.Intent
import android.os.CountDownTimer
import android.text.TextUtils
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.github.kr328.clash.databinding.ActivityLoginBinding
import com.xboard.api.RetrofitClient
import com.xboard.api.TokenManager
import com.xboard.base.BaseActivity
import com.xboard.network.AuthRepository
import com.xboard.storage.MMKVManager
import kotlinx.coroutines.launch

/**
 * 登录/注册页面
 */
class LoginActivity : BaseActivity<ActivityLoginBinding>() {

    private val authRepository by lazy { AuthRepository(RetrofitClient.getApiService()) }
    private var countDownTimer: CountDownTimer? = null

    override fun getViewBinding(): ActivityLoginBinding {
        return ActivityLoginBinding.inflate(layoutInflater)
    }

    override fun initView() {
        if (!TextUtils.isEmpty(MMKVManager.getUserEmail())) {
            binding.etEmail.setText(MMKVManager.getUserEmail())
        }
    }

    override fun initListener() {
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        binding.tvToggleMode.setOnClickListener {
            startActivity(Intent(this, RegistActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

    }

    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (!validateInput(email, password)) return

        lifecycleScope.launch {
            showLoading("登录中...")
            val result = authRepository.login(email, password)
            hideLoading()

            result
                .onSuccess { response ->
                    TokenManager.saveToken(response.token, response.authData, email, password)
                    showSuccess("登录成功")
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }
                .onError { error ->
                    showError(error.message)
                }
        }
    }


    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            Toast.makeText(this, "请输入邮箱", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
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

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        countDownTimer = null
    }
}
