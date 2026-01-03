package com.xboard.ui.activity

import android.os.Bundle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xboard.base.BaseComposeActivity
import com.xboard.ui.compose.CreateTicketScreen
import com.xboard.ui.viewmodel.CreateTicketViewModel

/**
 * 创建工单页面
 */
class CreateTicketActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setThemeContent {
            val viewModel: CreateTicketViewModel = viewModel()
            CreateTicketScreen(
                viewModel = viewModel,
                onNavigateBack = { finish() },
                onSubmitSuccess = {
                    finish()
                }
            )
        }
    }
}
