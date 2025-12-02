package com.xboard.ui.activity

import androidx.lifecycle.lifecycleScope
import com.github.kr328.clash.databinding.ActivityCreateTicketBinding
import com.xboard.api.RetrofitClient
import com.xboard.base.BaseActivity
import com.xboard.ex.showToast
import com.xboard.network.TicketRepository
import com.xboard.utils.onClick
import kotlinx.coroutines.launch

class CreateTicketActivity : BaseActivity<ActivityCreateTicketBinding>() {

    companion object {
        private const val TAG = "CreateTicketActivity"
    }

    private val ticketRepository by lazy { TicketRepository(RetrofitClient.getApiService()) }

    override fun getViewBinding(): ActivityCreateTicketBinding {
        return ActivityCreateTicketBinding.inflate(layoutInflater)
    }

    override fun initView() {
        binding.vBack.onClick { finish() }
        binding.btnSubmit.setOnClickListener {
            submitTicket()
        }
    }

    override fun initData() {
    }

    private fun submitTicket() {
        val subject = binding.etSubject.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        if (subject.isEmpty()) {
            showToast("请输入问题")
            return
        }

        if (description.isEmpty()) {
            showToast("请输入问题描述")
            return
        }

        lifecycleScope.launch {
            try {
                val result = ticketRepository.createTicket(
                    subject = subject,
                    description = description
                )
                result.onSuccess { result ->
                    if (result) {
                        showToast("提交问题成功")
                        finish()
                    }
                }.onError { error ->
                    showToast("提交失败: ${error.message}")
                }
            } catch (e: Exception) {
                showToast("提交失败: ${e.message}")
            }
        }
    }
}
