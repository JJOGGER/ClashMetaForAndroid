package com.xboard.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xboard.base.BaseComposeActivity
import com.xboard.model.TicketResponse
import com.xboard.ui.compose.TicketDetailScreen
import com.xboard.ui.viewmodel.TicketDetailViewModel

/**
 * 工单详情页面
 */
class TicketDetailActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ticketId = intent.getIntExtra("ticketId", -1)
        val ticket = intent.getSerializableExtra("ticket") as? TicketResponse?

        if (ticketId == -1) {
            finish()
            return
        }

        setThemeContent {
            val viewModel: TicketDetailViewModel = viewModel()
            TicketDetailScreen(
                ticketId = ticketId,
                initialTicket = ticket,
                viewModel = viewModel,
                onNavigateBack = { finish() }
            )
        }
    }
}
