package com.xboard.util

import android.content.Context
import android.util.Log

/**
 * 网络诊断工具
 * 用于诊断 Clash 代理问题
 */
class NetworkDiagnostics(private val context: Context) {

    companion object {
        private const val TAG = "NetworkDiagnostics"
    }

    private val clashRuleManager = ClashRuleManager(context)

    /**
     * 诊断特定域名的代理配置
     */
    fun diagnoseDomain(domain: String) {
        Log.d(TAG, "========== Diagnosing: $domain ==========")
        
        // 1. 检查规则
        val rule = clashRuleManager.checkDomainRule(domain)
        Log.d(TAG, "Rule: ${rule ?: "No specific rule (using default)"}")
        
        // 2. 检查是否需要代理
        val needsProxy = clashRuleManager.shouldUseProxy(domain)
        Log.d(TAG, "Needs Proxy: $needsProxy")
        
        // 3. 显示所有规则
        Log.d(TAG, "\n--- All Rules ---")
        clashRuleManager.printAllRules()
        
        Log.d(TAG, "========== End Diagnosis ==========\n")
    }

    /**
     * 诊断 API 请求失败的原因
     */
    fun diagnoseApiFailure(domain: String, statusCode: Int, message: String) {
        Log.e(TAG, "========== API Failure Diagnosis ==========")
        Log.e(TAG, "Domain: $domain")
        Log.e(TAG, "Status Code: $statusCode")
        Log.e(TAG, "Message: $message")
        
        when (statusCode) {
            502 -> {
                Log.e(TAG, "502 Bad Gateway - Possible causes:")
                Log.e(TAG, "1. Target server is down")
                Log.e(TAG, "2. Domain needs to use proxy instead of DIRECT")
                Log.e(TAG, "3. Network routing issue")
                
                // 检查规则
                val rule = clashRuleManager.checkDomainRule(domain)
                if (rule?.contains("DIRECT") == true) {
                    Log.e(TAG, "⚠️ FOUND: Domain is set to DIRECT, may need to use proxy")
                }
            }
            503 -> {
                Log.e(TAG, "503 Service Unavailable - Server is temporarily unavailable")
            }
            504 -> {
                Log.e(TAG, "504 Gateway Timeout - Request timeout, may be network issue")
            }
            else -> {
                Log.e(TAG, "Unknown error code: $statusCode")
            }
        }
        
        Log.e(TAG, "========== End Diagnosis ==========\n")
    }

    /**
     * 获取诊断报告
     */
    fun generateReport(): String {
        val sb = StringBuilder()
        sb.append("========== Clash Diagnostic Report ==========\n")
        
        // 获取所有规则
        val rules = clashRuleManager.getAllRules()
        sb.append("Total Rules: ${rules.size}\n")
        sb.append("\nRules:\n")
        rules.forEach { rule ->
            sb.append("  $rule\n")
        }
        
        sb.append("\n========== End Report ==========\n")
        return sb.toString()
    }
}
