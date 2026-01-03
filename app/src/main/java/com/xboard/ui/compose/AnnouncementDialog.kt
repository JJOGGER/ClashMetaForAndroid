package com.xboard.ui.compose

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import java.util.regex.Pattern

/**
 * Material 3 公告详情 BottomSheet
 * 使用 ModalBottomSheet，参照订阅详情的弹出方式
 * 支持图片URL展示和文本中的URL链接（参照ConfirmDialog规则）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementBottomSheet(
    title: String,
    message: String,
    imageUrl: String? = null,
    actionUrl: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val colorScheme = MaterialTheme.colorScheme
    
    // 处理文本中的URL链接（参照ConfirmDialog规则）
    val processedText = processHyperlinks(message, colorScheme.primary)
    
    // URL点击处理
    val onUrlClick: (Int) -> Unit = { offset ->
        processedText.getStringAnnotations(
            tag = "URL",
            start = offset,
            end = offset
        ).firstOrNull()?.let { annotation ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                context.startActivity(intent)
            } catch (e: Exception) {
                // 忽略错误
            }
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(2.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题和关闭按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // 图片展示（如果有）
            if (!imageUrl.isNullOrBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(context)
                            .data(imageUrl)
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = "公告图片",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            
            // 内容文本（支持URL链接）
            ClickableText(
                text = processedText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth(),
                onClick = onUrlClick
            )
            
            // 操作按钮（如果有actionUrl）
            if (!actionUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(actionUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // 忽略错误
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "查看详情",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            // 底部间距
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * 处理文本中的超链接（参照ConfirmDialog规则）
 * 支持 Markdown 格式: [text](url) 和普通 URL
 */
private fun processHyperlinks(
    text: String,
    primaryColor: androidx.compose.ui.graphics.Color
): androidx.compose.ui.text.AnnotatedString {
    val annotatedString = buildAnnotatedString {
        var currentIndex = 0
        
        // 首先处理 Markdown 链接格式: [text](url)
        val markdownUrlPattern = Pattern.compile("""\[(.*?)\]\((.*?)\)""")
        val markdownMatcher = markdownUrlPattern.matcher(text)
        val markdownMatches = mutableListOf<java.util.regex.MatchResult>()
        
        while (markdownMatcher.find()) {
            markdownMatches.add(markdownMatcher.toMatchResult())
        }
        
        // 从后往前处理避免索引偏移问题
        val sortedMatches = markdownMatches.sortedByDescending { matchResult: java.util.regex.MatchResult -> matchResult.start() }
        
        for (match in sortedMatches) {
            // 添加匹配前的文本
            if (currentIndex < match.start()) {
                append(text.substring(currentIndex, match.start()))
            }
            
            val linkText = match.group(1)
            val url = match.group(2).replace("\\", "") // 移除转义字符
            
            // 添加可点击的链接文本
            pushStringAnnotation(tag = "URL", annotation = url)
            withStyle(
                style = SpanStyle(
                    color = primaryColor,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(linkText)
            }
            pop()
            
            currentIndex = match.end()
        }
        
        // 添加剩余的文本
        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }
    
    // 处理普通URL（在已处理的文本中）
    val plainUrlPattern = Pattern.compile("""https?://[^\s\])>"/]+""")
    val result = buildAnnotatedString {
        var lastIndex = 0
        val plainUrlMatcher = plainUrlPattern.matcher(annotatedString.text)
        
        while (plainUrlMatcher.find()) {
            // 添加URL前的文本
            if (lastIndex < plainUrlMatcher.start()) {
                append(annotatedString.subSequence(lastIndex, plainUrlMatcher.start()))
            }
            
            val url = plainUrlMatcher.group()
            
            // 检查是否已经在Markdown链接中
            val isInMarkdownLink = annotatedString.getStringAnnotations(
                tag = "URL",
                start = plainUrlMatcher.start(),
                end = plainUrlMatcher.end()
            ).isNotEmpty()
            
            if (!isInMarkdownLink) {
                pushStringAnnotation(tag = "URL", annotation = url)
                withStyle(
                    style = SpanStyle(
                        color = primaryColor,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(url)
                }
                pop()
            } else {
                append(url)
            }
            
            lastIndex = plainUrlMatcher.end()
        }
        
        // 添加剩余的文本
        if (lastIndex < annotatedString.length) {
            append(annotatedString.subSequence(lastIndex, annotatedString.length))
        }
    }
    
    return result
}

