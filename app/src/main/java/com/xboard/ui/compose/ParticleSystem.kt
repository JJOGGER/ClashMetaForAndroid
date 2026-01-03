package com.xboard.ui.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * 粒子系统 - 为启动页添加动态粒子效果
 */
@Composable
 fun ParticleSystem(
    colorScheme: ColorScheme,
    rotation: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = size.maxDimension / 2
        
        // 绘制多个粒子
        for (i in 0..11) {
            val angle = (rotation + i * 30f) * PI / 180f
            val radius = maxRadius * 0.4f
            val x = centerX + radius * cos(angle).toFloat()
            val y = centerY + radius * sin(angle).toFloat()
            
            val particleColor = when (i % 3) {
                0 -> colorScheme.primary
                1 -> colorScheme.secondary
                else -> colorScheme.tertiary
            }
            
            drawCircle(
                color = particleColor.copy(alpha = 0.4f),
                radius = 6f + (i % 3) * 2f,
                center = Offset(x, y)
            )
        }
    }
}

