package com.xboard.ui.activity

import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.github.kr328.clash.databinding.ActivityCreateTicketBinding
import com.xboard.api.RetrofitClient
import com.xboard.base.BaseActivity
import com.xboard.ex.showToast
import com.xboard.network.TicketRepository
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
            showToast("请输入工单主题")
            return
        }

        if (description.isEmpty()) {
            showToast("请输入工单描述")
            return
        }

        lifecycleScope.launch {
            try {
                val result = ticketRepository.createTicket(
                    subject = subject,
                    description = description
                )
                result.onSuccess { ticket ->
                    showToast("工单创建成功")
                    initData()
                }.onError { error ->
                    showToast("创建失败: ${error.message}")
                }
            } catch (e: Exception) {
                showToast("创建失败: ${e.message}")
            }
        }
    }
}
