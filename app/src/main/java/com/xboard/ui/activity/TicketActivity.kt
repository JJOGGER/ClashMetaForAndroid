package com.xboard.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xboard.base.BaseComposeActivity
import com.xboard.ui.compose.TicketScreen
import com.xboard.ui.viewmodel.TicketViewModel

/**
 * 工单列表页面
 */
class TicketActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setThemeContent {
            val viewModel: TicketViewModel = viewModel()
            TicketScreen(
                viewModel = viewModel,
                onNavigateBack = { finish() },
                onNavigateToCreate = {
                    startActivity(Intent(this, CreateTicketActivity::class.java))
                },
                onNavigateToDetail = { ticketId, ticket ->
                    val intent = Intent(this, TicketDetailActivity::class.java)
                    intent.putExtra("ticketId", ticketId)
                    intent.putExtra("ticket", ticket)
                    startActivity(intent)
                }
            )
        }
    }
}
