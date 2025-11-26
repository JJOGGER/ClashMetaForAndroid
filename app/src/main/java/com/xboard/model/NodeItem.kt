package com.xboard.model

/**
 * 节点数据模型
 */
data class NodeItem(
    val name: String,           // 节点名称
    val type: String,           // 节点类型 (ss, ssr, vmess, trojan, etc.)
    val delay: Int,             // 延迟 (ms)
    val alive: Boolean,         // 是否在线
    val selected: Boolean = false  // 是否被选中
)
