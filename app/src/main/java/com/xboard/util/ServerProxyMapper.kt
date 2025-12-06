package com.xboard.util

import android.util.Log
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.core.model.ProxyGroup
import com.github.kr328.clash.util.withClash
import com.github.kr328.clash.design.store.UiStore
import com.xboard.model.Server
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Server 和 Clash Proxy 之间的映射工具
 * 用于将服务器节点列表与 Clash 内部的 proxy 进行对应
 */
object ServerProxyMapper {
    private const val TAG = "ServerProxyMapper"

    /**
     * 建立 Server 到 Clash Proxy 的映射关系
     * 
     * 映射策略：
     * 1. 优先通过名称完全匹配
     * 2. 如果名称不匹配，尝试通过 host:port 匹配
     * 3. 如果都不匹配，返回 null
     * 
     * @param servers 服务器节点列表
     * @param clashProxies Clash 的 proxy 列表
     * @return Map<Server.name, Clash Proxy>
     */
    fun buildMapping(servers: List<Server>, clashProxies: List<Proxy>): Map<String, Proxy> {
        val mapping = mutableMapOf<String, Proxy>()
        
        // 1. 首先尝试名称完全匹配
        for (server in servers) {
            val matchedProxy = clashProxies.firstOrNull { proxy ->
                // 完全匹配
                proxy.name == server.name ||
                // 或者 proxy 的名称包含 server 的名称（处理 Clash 添加后缀的情况）
                proxy.name.startsWith(server.name) ||
                // 或者 server 的名称包含 proxy 的名称（处理 URL 编码的情况）
                server.name.contains(proxy.name) ||
                // 或者去除后缀后匹配（处理 uniqueName 添加的 -01, -02 等）
                proxy.name.removeSuffix(Regex("-\\d+$").find(proxy.name)?.value ?: "") == server.name
            }
            
            if (matchedProxy != null) {
                mapping[server.name] = matchedProxy
                Log.d(TAG, "Mapped server '${server.name}' -> Clash proxy '${matchedProxy.name}'")
            } else {
                // 2. 如果名称不匹配，尝试通过 host:port 匹配
                val hostPortMatch = clashProxies.firstOrNull { proxy ->
                    // 从 proxy 的 subtitle 或其他信息中提取 host:port
                    // 注意：Proxy 对象可能不直接包含 server/port，需要从其他途径获取
                    // 这里先尝试名称匹配，如果失败再考虑其他方式
                    false // 暂时不实现，因为 Proxy 对象没有直接暴露 server/port
                }
                
                if (hostPortMatch != null) {
                    mapping[server.name] = hostPortMatch
                    Log.d(TAG, "Mapped server '${server.name}' -> Clash proxy '${hostPortMatch.name}' (by host:port)")
                } else {
                    Log.w(TAG, "Could not map server '${server.name}' to any Clash proxy")
                }
            }
        }
        
        return mapping
    }

    /**
     * 从 Clash 获取所有 proxy 并建立映射
     * 
     * @param servers 服务器节点列表
     * @param groupName 代理组名称（如 "XBoard"）
     * @return Map<Server.name, Clash Proxy> 映射关系
     */
    suspend fun buildMappingFromClash(
        servers: List<Server>,
        groupName: String = "XBoard"
    ): Map<String, Proxy> = withContext(Dispatchers.IO) {
        try {
            // 1. 从 Clash 获取指定代理组的所有 proxy
            val group = withClash {
                queryProxyGroup(groupName, UiStore(android.app.Application()).proxySort)
            }
            
            if (group == null) {
                Log.w(TAG, "Proxy group '$groupName' not found in Clash")
                return@withContext emptyMap()
            }
            
            // 2. 建立映射关系
            buildMapping(servers, group.proxies)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build mapping from Clash: ${e.message}", e)
            emptyMap()
        }
    }

    /**
     * 更新 Server 列表的延迟信息（从 Clash proxy 映射）
     * 
     * @param servers 服务器节点列表（会被修改）
     * @param mapping Server.name -> Clash Proxy 的映射
     */
    fun updateServerDelays(servers: MutableList<Server>, mapping: Map<String, Proxy>) {
        for (i in servers.indices) {
            val server = servers[i]
            val proxy = mapping[server.name]
            
            if (proxy != null) {
                // 注意：Server 对象是 data class，需要创建新对象来更新
                // 但 Server 没有 delay 字段，可能需要扩展 Server 或创建包装类
                // 这里先记录日志
                Log.d(TAG, "Server '${server.name}' delay: ${proxy.delay}ms")
            }
        }
    }

    /**
     * 将 Server 名称映射到 Clash proxy 名称
     * 
     * @param serverName 服务器节点名称
     * @param mapping Server.name -> Clash Proxy 的映射
     * @return Clash proxy 名称，如果找不到则返回 serverName
     */
    fun mapToClashProxyName(serverName: String, mapping: Map<String, Proxy>): String {
        return mapping[serverName]?.name ?: serverName
    }
}





