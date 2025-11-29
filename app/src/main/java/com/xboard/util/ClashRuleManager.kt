package com.xboard.util

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Clash 规则管理器
 * 用于检查和修改 Clash 配置中的规则
 */
class ClashRuleManager(private val context: Context) {

    companion object {
        private const val TAG = "ClashRuleManager"
        // Clash 配置文件路径（Android）
        private const val CLASH_CONFIG_PATH = "/data/data/com.clash.zero.alpha/files/clash/config.yaml"
    }

    /**
     * 获取当前 Clash 配置
     */
    fun getClashConfig(): String? {
        return try {
            val configFile = File(CLASH_CONFIG_PATH)
            if (configFile.exists()) {
                configFile.readText()
            } else {
                Log.w(TAG, "Clash config file not found at: $CLASH_CONFIG_PATH")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading Clash config: ${e.message}")
            null
        }
    }

    /**
     * 检查特定域名的规则
     */
    fun checkDomainRule(domain: String): String? {
        val config = getClashConfig() ?: return null
        
        val lines = config.split("\n")
        var inRulesSection = false
        
        for (line in lines) {
            when {
                line.trim().startsWith("rules:") -> {
                    inRulesSection = true
                    continue
                }
                inRulesSection && line.trim().isEmpty() -> {
                    // 规则段结束
                    break
                }
                inRulesSection && line.contains(domain) -> {
                    Log.d(TAG, "Found rule for $domain: $line")
                    return line.trim()
                }
            }
        }
        
        Log.d(TAG, "No specific rule found for $domain, using default")
        return null
    }

    /**
     * 获取所有规则
     */
    fun getAllRules(): List<String> {
        val config = getClashConfig() ?: return emptyList()
        
        val rules = mutableListOf<String>()
        val lines = config.split("\n")
        var inRulesSection = false
        
        for (line in lines) {
            when {
                line.trim().startsWith("rules:") -> {
                    inRulesSection = true
                    continue
                }
                inRulesSection && line.trim().isEmpty() -> {
                    break
                }
                inRulesSection && line.trim().startsWith("-") -> {
                    rules.add(line.trim())
                }
            }
        }
        
        return rules
    }

    /**
     * 打印所有规则（用于调试）
     */
    fun printAllRules() {
        val rules = getAllRules()
        Log.d(TAG, "========== Clash Rules ==========")
        rules.forEachIndexed { index, rule ->
            Log.d(TAG, "$index: $rule")
        }
        Log.d(TAG, "================================")
    }

    /**
     * 检查是否需要代理
     */
    fun shouldUseProxy(domain: String): Boolean {
        val rule = checkDomainRule(domain)
        
        return when {
            rule == null -> {
                // 没有特定规则，检查默认规则
                Log.d(TAG, "$domain: No specific rule, checking default")
                false
            }
            rule.contains("DIRECT") -> {
                Log.d(TAG, "$domain: Using DIRECT (no proxy)")
                false
            }
            rule.contains("PROXY") || 
            rule.contains("ProxyNode") ||
            !rule.contains("DIRECT") -> {
                Log.d(TAG, "$domain: Using PROXY")
                true
            }
            else -> false
        }
    }
}
