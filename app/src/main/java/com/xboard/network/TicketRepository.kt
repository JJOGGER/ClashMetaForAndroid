package com.xboard.network

import android.util.Log
import com.xboard.api.ApiService
import com.xboard.model.*
import com.xboard.storage.MMKVManager
import com.xboard.ui.fragment.AccelerateFragment

/**
 * 工单和客服相关的Repository
 */
class TicketRepository(private val apiService: ApiService) : BaseRepository() {

    /**
     * 创建工单
     */
    suspend fun createTicket(
        subject: String,
        description: String,
        level: Int = 0
    ): ApiResult<Boolean> {
        return safeApiCall {
            apiService.createTicket(
                CreateTicketRequest(
                    subject = subject,
                    message = description,
                    level = level
                )
            )
        }
    }

    /**
     * 获取工单列表
     */
    suspend fun getTickets(
        page: Int = 1,
        perPage: Int = 20
    ): ApiResult<List<TicketResponse>?> {
        return safeApiCall {
            apiService.getTickets(page, perPage)
        }.map { response ->
            response
        }
    }

    /**
     * 回复工单
     */
    suspend fun replyTicket(
        ticketId: Int,
        message: String
    ): ApiResult<Unit> {
        return safeApiCallVoid {
            apiService.replyTicket(
                ReplyTicketRequest(
                    ticketId = ticketId,
                    message = message
                )
            )
        }
    }

    /**
     * 关闭工单
     */
    suspend fun closeTicket(ticketId: Int): ApiResult<Unit> {
        return safeApiCallVoid {
            apiService.closeTicket(
                CloseTicketRequest(ticketId)
            )
        }
    }

    /**
     * 获取工单详情
     */
    suspend fun getTicketDetail(ticketId: Int): ApiResult<TicketResponse> {
        return safeApiCall {
            apiService.getTicketDetail(ticketId)
        }
    }

    /**
     * 获取公告列表
     */
    suspend fun getNotices(
        page: Int = 1,
        perPage: Int = 20
    ): NoticeListResponse? {
        return apiService.getNotices(page, perPage)
    }

    /**
     * 获取知识库分类
     */
    suspend fun getKnowledgeCategories(): ApiResult<List<KnowledgeCategory>> {
        return safeApiCall {
            apiService.getKnowledgeCategories()
        }
    }

    /**
     * 获取知识库文章
     */
    suspend fun getKnowledgeArticles(): ApiResult<List<KnowledgeArticle>?> {
        return safeApiCall {
            apiService.getKnowledgeArticles("zh-CN")
        }.map { response ->
            response.site
        }.onSuccess { articles ->
            // 保存到本地缓存
            MMKVManager.saveWebsiteRecommendations(articles)
        }
    }
}
