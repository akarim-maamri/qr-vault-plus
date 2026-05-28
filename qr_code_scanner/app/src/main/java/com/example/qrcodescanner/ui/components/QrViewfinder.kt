package com.example.qrcodescanner.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun QrViewfinder(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "LaserTransition")
    
    // Laser line vertical animation
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserProgress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val cutoutSize = 280.dp.toPx()
        val left = (canvasWidth - cutoutSize) / 2
        val top = (canvasHeight - cutoutSize) / 2
        val right = left + cutoutSize
        val bottom = top + cutoutSize

        // 1. Draw dark semi-transparent overlay surrounding the viewfinder cutout
        // Top area
        drawRect(color = Color(0x99000000), size = Size(canvasWidth, top))
        // Bottom area
        drawRect(color = Color(0x99000000), topLeft = Offset(0f, bottom), size = Size(canvasWidth, canvasHeight - bottom))
        // Left area
        drawRect(color = Color(0x99000000), topLeft = Offset(0f, top), size = Size(left, cutoutSize))
        // Right area
        drawRect(color = Color(0x99000000), topLeft = Offset(right, top), size = Size(canvasWidth - right, cutoutSize))

        // 2. Draw glowing cyan corner brackets
        val cornerLength = 32.dp.toPx()
        val strokeWidth = 4.dp.toPx()
        val neonCyan = Color(0xFF00F2FE)

        // Top-Left corner
        drawLine(color = neonCyan, start = Offset(left, top), end = Offset(left + cornerLength, top), strokeWidth = strokeWidth)
        drawLine(color = neonCyan, start = Offset(left, top), end = Offset(left, top + cornerLength), strokeWidth = strokeWidth)

        // Top-Right corner
        drawLine(color = neonCyan, start = Offset(right, top), end = Offset(right - cornerLength, top), strokeWidth = strokeWidth)
        drawLine(color = neonCyan, start = Offset(right, top), end = Offset(right, top + cornerLength), strokeWidth = strokeWidth)

        // Bottom-Left corner
        drawLine(color = neonCyan, start = Offset(left, bottom), end = Offset(left + cornerLength, bottom), strokeWidth = strokeWidth)
        drawLine(color = neonCyan, start = Offset(left, bottom), end = Offset(left, bottom - cornerLength), strokeWidth = strokeWidth)

        // Bottom-Right corner
        drawLine(color = neonCyan, start = Offset(right, bottom), end = Offset(right - cornerLength, bottom), strokeWidth = strokeWidth)
        drawLine(color = neonCyan, start = Offset(right, bottom), end = Offset(right, bottom - cornerLength), strokeWidth = strokeWidth)

        // 3. Draw scanning laser line
        val laserY = top + (bottom - top) * laserProgress
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0x0000F2FE),
                    Color(0xCC00F2FE),
                    Color(0x0000F2FE)
                )
            ),
            topLeft = Offset(left, laserY - 10.dp.toPx()),
            size = Size(cutoutSize, 20.dp.toPx())
        )
    }
}
