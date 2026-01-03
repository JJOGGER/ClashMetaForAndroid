package com.xboard.ui.compose

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xboard.R
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlinx.coroutines.delay

/**
 * 启动页 Compose 实现
 * 包含酷炫的循环动画效果，至少2秒，符合M3设计规范
 * 
 * @param onAnimationComplete 动画完成回调（至少2秒后调用）
 */
@Composable
fun SplashScreen(
    onAnimationComplete: (() -> Unit)? = null
) {
    // 动画状态
    var logoVisible by remember { mutableStateOf(false) }
    var appNameVisible by remember { mutableStateOf(false) }
    var taglineVisible by remember { mutableStateOf(false) }
    var particlesVisible by remember { mutableStateOf(false) }
    
    // 最小动画时长：2秒
    val MIN_ANIMATION_DURATION = 2000L
    
    // 记录动画开始时间
    val animationStartTime = remember { System.currentTimeMillis() }
    
    // 渐变背景动画 - 使用多个颜色循环（2秒循环）
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val gradientOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient-1"
    )
    val gradientOffset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient-2"
    )
    
    // 旋转动画 - 2秒一圈
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // 脉冲动画 - 1秒循环
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    // 波纹动画 - 多个波纹扩散
    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave-1"
    )
    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = androidx.compose.animation.core.StartOffset(500)
        ),
        label = "wave-2"
    )
    
    // 启动动画序列
    LaunchedEffect(Unit) {
        logoVisible = true
        delay(200)
        particlesVisible = true
        delay(300)
        appNameVisible = true
        delay(200)
        taglineVisible = true
        
        // 确保至少播放2秒动画
        val elapsed = System.currentTimeMillis() - animationStartTime
        val remaining = MIN_ANIMATION_DURATION - elapsed
        if (remaining > 0) {
            delay(remaining)
        }
        
        // 动画完成回调
        onAnimationComplete?.invoke()
    }
    
    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = remember(colorScheme) { colorScheme.primary }
    val secondaryColor = remember(colorScheme) { colorScheme.secondary }
    val tertiaryColor = remember(colorScheme) { colorScheme.tertiary }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.surface)
    ) {
        // 动态渐变背景层
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val maxRadius = size.maxDimension
            
            // 绘制动态渐变背景
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.15f + gradientOffset1 * 0.1f),
                        secondaryColor.copy(alpha = 0.1f + gradientOffset2 * 0.08f),
                        tertiaryColor.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(
                        x = centerX + 150f * sin(gradientOffset1 * 2 * PI).toFloat(),
                        y = centerY + 150f * cos(gradientOffset2 * 2 * PI).toFloat()
                    ),
                    radius = maxRadius * 0.8f
                ),
                radius = maxRadius * 0.8f,
                center = Offset(centerX, centerY)
            )
        }
        
        // 旋转的装饰元素
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            
            // 绘制旋转的几何图形
            rotate(rotation, Offset(centerX, centerY)) {
                // 外圈装饰圆环
                for (i in 0..5) {
                    val angle = i * 60f
                    val radius = size.minDimension * 0.35f
                    val x = centerX + radius * cos(angle * PI / 180f).toFloat()
                    val y = centerY + radius * sin(angle * PI / 180f).toFloat()
                    
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.15f - i * 0.02f),
                        radius = 40f + 20f * pulseScale,
                        center = Offset(x, y),
                        style = Stroke(width = 3f)
                    )
                }
                
                // 内圈装饰点
                for (i in 0..7) {
                    val angle = i * 45f
                    val radius = size.minDimension * 0.2f
                    val x = centerX + radius * cos(angle * PI / 180f).toFloat()
                    val y = centerY + radius * sin(angle * PI / 180f).toFloat()
                    
                    drawCircle(
                        color = secondaryColor.copy(alpha = 0.2f),
                        radius = 8f + 4f * pulseScale,
                        center = Offset(x, y)
                    )
                }
            }
            
            // 波纹效果
            val waveRadius = size.minDimension * 0.3f
            val wave1Radius = waveRadius + wave1 * size.minDimension * 0.2f
            val wave2Radius = waveRadius + wave2 * size.minDimension * 0.2f
            
            drawCircle(
                color = primaryColor.copy(alpha = (1f - wave1) * 0.1f),
                radius = wave1Radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2f)
            )
            drawCircle(
                color = secondaryColor.copy(alpha = (1f - wave2) * 0.08f),
                radius = wave2Radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2f)
            )
        }
        
        // 粒子效果
        AnimatedVisibility(
            visible = particlesVisible,
            enter = fadeIn(animationSpec = tween(800))
        ) {
            ParticleSystem(
                colorScheme = colorScheme,
                rotation = rotation
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo - 缩放和淡入动画，带持续旋转和脉冲效果
            AnimatedVisibility(
                visible = logoVisible,
                enter = scaleIn(
                    initialScale = 0.2f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(
                    animationSpec = tween(800)
                ),
                exit = scaleOut() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(pulseScale)
                        .rotate(rotation * 0.1f), // 缓慢旋转
                    contentAlignment = Alignment.Center
                ) {
                    // Logo 光晕效果
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.3f * pulseScale),
                                    Color.Transparent
                                )
                            ),
                            radius = size.minDimension * 0.6f,
                            center = Offset(size.width / 2, size.height / 2)
                        )
                    }
                    
                    Image(
                        painter = painterResource(id = R.drawable.home_main_logo),
                        contentDescription = stringResource(id = R.string.app_name),
                        modifier = Modifier.size(120.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // 应用名称 - 淡入和上滑动画，带持续脉冲
            AnimatedVisibility(
                visible = appNameVisible,
                enter = fadeIn(
                    animationSpec = tween(800)
                ) + slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = tween(800, easing = FastOutSlowInEasing)
                ),
                exit = fadeOut() + slideOutVertically()
            ) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.scale(0.95f + pulseScale * 0.05f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 标语 - 淡入动画（带延迟）
            AnimatedVisibility(
                visible = taglineVisible,
                enter = fadeIn(
                    animationSpec = tween(800)
                ) + slideInVertically(
                    initialOffsetY = { 20 },
                    animationSpec = tween(800, easing = FastOutSlowInEasing)
                ),
                exit = fadeOut()
            ) {
                Text(
                    text = stringResource(id = R.string.app_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(56.dp))
            
            // 加载指示器 - 带旋转光晕效果
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                // 外圈旋转光晕
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(rotation)
                ) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.minDimension / 2 - 4f
                    
                    // 绘制渐变圆弧
                    for (i in 0..7) {
                        val angle = i * 45f
                        val startAngle = angle - 10f
                        val sweepAngle = 20f
                        
                        drawArc(
                            color = primaryColor.copy(alpha = 0.3f - i * 0.03f),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = 3f)
                        )
                    }
                }
                
                // 内圈进度指示器
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(48.dp)
                        .scale(pulseScale),
                    color = primaryColor,
                    strokeWidth = 3.dp,
                    trackColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}

