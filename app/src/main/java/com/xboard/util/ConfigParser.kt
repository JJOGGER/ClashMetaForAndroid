package com.xboard.util

import android.content.Context
import android.util.Log
import com.github.kr328.clash.service.data.SelectionDao
import com.github.kr328.clash.service.util.importedDir
import com.github.kr328.clash.util.withProfile
import com.xboard.storage.MMKVManager
import java.io.File
import java.util.regex.Pattern

/**
 * 简单的配置文件解析器
 * 用于在不启动 Clash 服务的情况下读取节点信息
 */
object ConfigParser {
    private const val TAG = "ConfigParser"

    /**
     * 从配置文件中解析节点信息
     * 优先从 SelectionDao 读取保存的选中状态，如果没有则从配置文件读取默认节点
     * @param context Context
     * @return Pair<groupName, nodeName> 或 null
     */
    suspend fun parseNodeFromConfig(context: Context){
         try {
            val activeProfile = withProfile { queryActive() }
            if (activeProfile == null) {
                Log.w(TAG, "No active profile found")
                return
            }
            var currentGroup = MMKVManager.getCurrentGroup()
            // 1. 优先从 SelectionDao 读取保存的选中状态（这是 Clash 连接时会使用的节点）
//            val selections = SelectionDao().querySelections(activeProfile.uuid)
//            if (selections.isNotEmpty()) {
//                // 优先选择 "Proxy" 组，否则选择第一个
//                val selection = selections.firstOrNull { it.proxy == "Proxy" }
//                    ?: selections.firstOrNull()
//
//                if (selection != null) {
//                    Log.d(
//                        TAG,
//                        "Found saved selection: group=${selection.proxy}, node=${selection.selected}"
//                    )
//                    MMKVManager.saveCurrentNode(selection.proxy, selection.selected)
//                    return Pair(selection.proxy, selection.selected)
//                }
//            }
            if (currentGroup==null) {
                // 2. 如果没有保存的状态，从配置文件读取默认节点（第一个 proxy-group 的第一个节点）
                val configFile = context.importedDir.resolve(activeProfile.uuid.toString())
                    .resolve("config.yaml")

                if (!configFile.exists()) {
                    Log.w(TAG, "Config file not found: ${configFile.absolutePath}")
                    return
                }

                val configContent = configFile.readText()
                val configNode = parseNodeFromYaml(configContent)
                if (configNode != null) {
                    Log.d(
                        TAG,
                        "Found node from config: group=${configNode.first}, node=${configNode.second}"
                    )
                }
                MMKVManager.saveCurrentNode(configNode?.first,configNode?.second)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse config: ${e.message}", e)
        }
    }

    /**
     * 从 YAML 内容中解析节点信息（公开方法，用于直接解析 YAML 字符串）
     * 优先从 SelectionDao 读取保存的选中状态，如果没有则从 YAML 解析默认节点
     *
     * @param yamlContent YAML 配置内容
     * @param profileUuid 可选的 profile UUID，如果提供则优先从 SelectionDao 读取选中状态
     * @return Pair<groupName, nodeName> 或 null
     */
    suspend fun parseNodeFromYamlContent(
        yamlContent: String,
        profileUuid: java.util.UUID? = null
    ): Pair<String, String>? {
        // 1. 如果提供了 profileUuid，优先从 SelectionDao 读取保存的选中状态
        if (profileUuid != null) {
            try {
                val selections = SelectionDao().querySelections(profileUuid)
                if (selections.isNotEmpty()) {
                    // 优先选择 "Proxy" 或 "XBoard" 组，否则选择第一个
                    val selection =
                        selections.firstOrNull { it.proxy == "Proxy" || it.proxy == "XBoard" }
                            ?: selections.firstOrNull()

                    if (selection != null) {
                        Log.d(
                            TAG,
                            "Found saved selection from SelectionDao: group=${selection.proxy}, node=${selection.selected}"
                        )
                        return Pair(selection.proxy, selection.selected)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to query SelectionDao: ${e.message}", e)
            }
        }

        // 2. 如果没有保存的状态，从 YAML 解析默认节点
        return parseNodeFromYaml(yamlContent)
    }

    /**
     * 从 YAML 内容中解析节点信息
     * 1. 先解析所有 proxy-groups，建立 group 名称集合
     * 2. 解析所有 proxies，建立实际节点名称集合
     * 3. 找到第一个 select 类型的 group（优先 "Proxy" 或 "XBoard"）
     * 4. 在它的 proxies 列表中，找到第一个实际节点（不在 group 名称集合中的）
     */
    private fun parseNodeFromYaml(yamlContent: String): Pair<String, String>? {
        try {
            // 1. 解析所有 proxy-groups，建立 group 名称集合
            val groupNames = mutableSetOf<String>()
            val groupTypes = mutableMapOf<String, String>() // group name -> type
            val groupProxies = mutableMapOf<String, List<String>>() // group name -> proxies list

            // 2. 解析所有 proxies，建立实际节点名称集合
            val proxyNames = mutableSetOf<String>()

            val lines = yamlContent.lines()
            var inProxies = false
            var inProxyGroups = false
            var currentGroupName: String? = null
            var currentGroupType: String? = null
            var inGroupProxies = false
            var currentGroupProxies = mutableListOf<String>()

            for (line in lines) {
                val trimmed = line.trim()

                // 解析 proxies 部分
                if (trimmed == "proxies:" || trimmed.startsWith("proxies:")) {
                    inProxies = true
                    continue
                }

                if (inProxies) {
                    // 如果遇到下一个顶级键，停止解析 proxies
                    if (trimmed.isNotEmpty() && !line.startsWith(" ") && !line.startsWith("-") && !trimmed.startsWith(
                            "#"
                        )
                    ) {
                        inProxies = false
                    } else if (line.trim().startsWith("- { name:") || line.trim()
                            .startsWith("- name:")
                    ) {
                        // 提取节点名称
                        val nameMatch =
                            Pattern.compile("name:\\s*['\"]?([^'\"\\n,}]+)['\"]?").matcher(line)
                        if (nameMatch.find()) {
                            val proxyName = nameMatch.group(1).trim()
                            proxyNames.add(proxyName)
                        }
                    }
                }

                // 解析 proxy-groups 部分
                if (trimmed == "proxy-groups:" || trimmed.startsWith("proxy-groups:")) {
                    inProxyGroups = true
                    continue
                }

                // 如果遇到下一个顶级键，停止解析 proxy-groups
                if (inProxyGroups && trimmed.isNotEmpty() && !line.startsWith(" ") && !line.startsWith(
                        "-"
                    ) && !trimmed.startsWith("#")
                ) {
                    break
                }

                if (!inProxyGroups) continue

                // 检测 group 开始（以 - name: 开头或 - { name: 开头）
                // 支持格式：
                // - { name: XBoard, type: select, proxies: [...] }
                // - name: XBoard
                if (line.trim().startsWith("- { name:") || line.trim().startsWith("- name:") ||
                    (line.trim().startsWith("-{") && line.contains("name:")) ||
                    (line.trim()
                        .startsWith("-") && line.contains("name:") && line.contains("type:"))
                ) {
                    // 保存上一个 group 的 proxies
                    if (currentGroupName != null) {
                        groupProxies[currentGroupName] = currentGroupProxies.toList()
                    }

                    inGroupProxies = false
                    currentGroupProxies.clear()

                    // 提取 group 名称（支持 name: XBoard 或 name: 'XBoard' 或 name: "XBoard"）
                    val nameMatch =
                        Pattern.compile("name:\\s*['\"]?([^'\"\\n,}]+)['\"]?").matcher(line)
                    if (nameMatch.find()) {
                        currentGroupName = nameMatch.group(1).trim()
                        groupNames.add(currentGroupName)
                        Log.d(TAG, "Found group: $currentGroupName")
                    }

                    // 提取 group type（在同一行或下一行）
                    val typeMatch =
                        Pattern.compile("type:\\s*['\"]?([^'\"\\n,}]+)['\"]?").matcher(line)
                    if (typeMatch.find()) {
                        currentGroupType = typeMatch.group(1).trim()
                        if (currentGroupName != null) {
                            groupTypes[currentGroupName] = currentGroupType
                            Log.d(TAG, "Group $currentGroupName type: $currentGroupType")
                        }
                    }

                    // 如果这一行包含内联的 proxies: [...]，立即解析
                    if (line.contains("proxies:") && line.contains("[")) {
                        val proxiesMatch =
                            Pattern.compile("proxies:\\s*\\[([^\\]]+)\\]").matcher(line)
                        if (proxiesMatch.find()) {
                            val proxiesStr = proxiesMatch.group(1)
                            val proxyItems = proxiesStr.split(",").map {
                                it.trim().removeSurrounding("'", "\"").trim()
                            }
                            currentGroupProxies.addAll(proxyItems)
                            Log.d(TAG, "Found inline proxies for $currentGroupName: $proxyItems")
                            // 保存这个 group 的 proxies
                            if (currentGroupName != null) {
                                groupProxies[currentGroupName] = currentGroupProxies.toList()
                            }
                            // 重置状态，准备下一个 group
                            currentGroupName = null
                            currentGroupType = null
                            currentGroupProxies.clear()
                        }
                    }
                    continue
                }

                // 提取 type（如果不在 name 行）
                if (currentGroupName != null && currentGroupType == null) {
                    val typeMatch =
                        Pattern.compile("type:\\s*['\"]?([^'\"\\n,}]+)['\"]?").matcher(line)
                    if (typeMatch.find()) {
                        currentGroupType = typeMatch.group(1).trim()
                        groupTypes[currentGroupName] = currentGroupType
                    }
                }

                // 在 group 内检测 proxies 列表
                if (currentGroupName != null) {
                    if (trimmed == "proxies:" || trimmed.startsWith("proxies:") ||
                        (line.contains("proxies:") && line.contains("["))
                    ) {
                        inGroupProxies = true
                        // 如果是内联格式 proxies: [xxx, yyy]
                        if (line.contains("[")) {
                            val proxiesMatch =
                                Pattern.compile("proxies:\\s*\\[([^\\]]+)\\]").matcher(line)
                            if (proxiesMatch.find()) {
                                val proxiesStr = proxiesMatch.group(1)
                                val proxyItems = proxiesStr.split(",")
                                    .map { it.trim().removeSurrounding("'", "\"") }
                                currentGroupProxies.addAll(proxyItems)
                                inGroupProxies = false
                            }
                        }
                        continue
                    }

                    // 如果遇到下一个 group（以 - 开头），保存当前 group 并重置
                    if (line.trim().startsWith("-") && !inGroupProxies) {
                        if (currentGroupName != null) {
                            groupProxies[currentGroupName] = currentGroupProxies.toList()
                        }
                        currentGroupName = null
                        currentGroupType = null
                        currentGroupProxies.clear()
                        continue
                    }

                    // 在 proxies 列表中提取节点名称
                    if (inGroupProxies) {
                        // 匹配列表项：- "节点名" 或 - 节点名 或 节点名,
                        val proxyMatch =
                            Pattern.compile("-\\s*['\"]?([^'\"\\n,}]+)['\"]?|['\"]?([^'\"\\n,}]+)['\"]?")
                                .matcher(line)
                        if (proxyMatch.find()) {
                            val proxyName = (proxyMatch.group(1) ?: proxyMatch.group(2))?.trim()
                            if (!proxyName.isNullOrBlank()) {
                                currentGroupProxies.add(proxyName)
                            }
                        }
                    }
                }
            }

            // 保存最后一个 group 的 proxies
            if (currentGroupName != null) {
                groupProxies[currentGroupName] = currentGroupProxies.toList()
            }

            // 3. 找到第一个 select 类型的 group（优先 "Proxy" 或 "XBoard"）
            val targetGroupName = groupTypes.entries
                .firstOrNull { (name, type) ->
                    type == "select" && (name == "Proxy" || name == "XBoard")
                }?.key
                ?: groupTypes.entries.firstOrNull { it.value == "select" }?.key

            if (targetGroupName == null) {
                Log.w(TAG, "No select type group found")
                return null
            }

            // 4. 在它的 proxies 列表中，找到第一个实际节点（不在 group 名称集合中的）
            val proxies = groupProxies[targetGroupName] ?: emptyList()
            val actualNode = proxies.firstOrNull { proxyName ->
                // 节点名称不在 group 名称集合中，说明是实际节点
                !groupNames.contains(proxyName)
            }

            if (actualNode != null) {
                Log.d(TAG, "Parsed node: group=$targetGroupName, node=$actualNode")
                return Pair(targetGroupName, actualNode)
            }

            Log.w(TAG, "No actual node found in group $targetGroupName")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse YAML: ${e.message}", e)
            null
        }
        return null
    }

    /**
     * 获取所有代理组名称
     */
    suspend fun getProxyGroupNames(context: Context): List<String> {
        return try {
            val activeProfile = withProfile { queryActive() }
            if (activeProfile == null) {
                return emptyList()
            }

            val configFile = context.importedDir.resolve(activeProfile.uuid.toString())
                .resolve("config.yaml")

            if (!configFile.exists()) {
                return emptyList()
            }

            val configContent = configFile.readText()
            val groupNames = mutableListOf<String>()

            // 查找所有 proxy-groups 的 name
            val namePattern = Pattern.compile(
                "-\\s*name:\\s*['\"]?([^'\"\\n]+)['\"]?",
                Pattern.MULTILINE
            )
            val matcher = namePattern.matcher(configContent)

            // 找到 proxy-groups 部分
            val proxyGroupsIndex = configContent.indexOf("proxy-groups:")
            if (proxyGroupsIndex == -1) {
                return emptyList()
            }

            // 只匹配 proxy-groups 部分的 name
            val afterProxyGroups = configContent.substring(proxyGroupsIndex)
            val groupsMatcher = namePattern.matcher(afterProxyGroups)

            while (groupsMatcher.find()) {
                val name = groupsMatcher.group(1).trim()
                // 检查是否在 proxy-groups 部分（在下一个顶级键之前）
                val nextTopLevel = afterProxyGroups.indexOf("\n", groupsMatcher.end())
                val sectionEnd = afterProxyGroups.indexOf("\n", nextTopLevel + 1)
                val nextSection = afterProxyGroups.substring(
                    nextTopLevel,
                    sectionEnd.coerceAtMost(afterProxyGroups.length)
                )

                // 简单检查：如果下一行不是以空格或 - 开头，说明已经离开了 proxy-groups 部分
                if (nextSection.isNotEmpty() && !nextSection.trimStart().startsWith("-") &&
                    !nextSection.trimStart().startsWith(" ") &&
                    !nextSection.trimStart().isEmpty()
                ) {
                    break
                }

                groupNames.add(name)
            }

            groupNames
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get proxy group names: ${e.message}", e)
            emptyList()
        }
    }
}

