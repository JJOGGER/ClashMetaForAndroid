package com.xboard.model

/**
 * 订阅信息数据模型
 */
data class SubscriptionInfo(
    val upload: Long,      // 已上传流量 (字节)
    val download: Long,    // 已下载流量 (字节)
    val total: Long,       // 总流量 (字节)
    val expire: Long       // 过期时间 (毫秒时间戳)
)
