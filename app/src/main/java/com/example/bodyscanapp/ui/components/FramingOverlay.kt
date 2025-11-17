package com.example.bodyscanapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * FramingOverlay - Displays a body outline/frame guide on camera preview
 * 
 * Features:
 * - Body frame rectangle (dashed outline)
 * - Head circle guide
 * - Horizontal alignment lines
 * - Visual feedback (green when in frame, red when not - can be enhanced with ML Kit later)
 * 
 * @param modifier Modifier for the overlay
 * @param isInFrame Whether the user is properly positioned in frame (for visual feedback)
 * @param instructionText Optional instruction text to display
 */
@Composable
fun FramingOverlay(
    modifier: Modifier = Modifier,
    isInFrame: Boolean = false,
    instructionText: String = "Position yourself in the frame"
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Define framing guide dimensions (body outline)
        val frameWidth = canvasWidth * 0.6f
        val frameHeight = canvasHeight * 0.7f
        val frameLeft = (canvasWidth - frameWidth) / 2
        val frameTop = (canvasHeight - frameHeight) / 2

        // Color based on frame status (green if in frame, white otherwise)
        val frameColor = if (isInFrame) {
            Color(0xFF4CAF50) // Green
        } else {
            Color.White
        }

        // Draw dashed rectangle for body frame
        drawRect(
            color = frameColor,
            topLeft = Offset(frameLeft, frameTop),
            size = Size(frameWidth, frameHeight),
            style = Stroke(
                width = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
            )
        )

        // Draw head circle guide
        val headRadius = frameWidth * 0.15f
        val headCenterX = canvasWidth / 2
        val headCenterY = frameTop + headRadius + 20f

        drawCircle(
            color = frameColor,
            radius = headRadius,
            center = Offset(headCenterX, headCenterY),
            style = Stroke(
                width = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
            )
        )

        // Draw horizontal alignment lines
        val lineCount = 4
        val lineSpacing = frameHeight / lineCount
        for (i in 1 until lineCount) {
            val y = frameTop + (lineSpacing * i)
            drawLine(
                color = frameColor.copy(alpha = 0.5f),
                start = Offset(frameLeft, y),
                end = Offset(frameLeft + frameWidth, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        }

        // Draw corner guides for better alignment
        val cornerSize = 30f
        val cornerThickness = 4f
        
        // Top-left corner
        drawLine(
            color = frameColor,
            start = Offset(frameLeft, frameTop),
            end = Offset(frameLeft + cornerSize, frameTop),
            strokeWidth = cornerThickness
        )
        drawLine(
            color = frameColor,
            start = Offset(frameLeft, frameTop),
            end = Offset(frameLeft, frameTop + cornerSize),
            strokeWidth = cornerThickness
        )
        
        // Top-right corner
        drawLine(
            color = frameColor,
            start = Offset(frameLeft + frameWidth, frameTop),
            end = Offset(frameLeft + frameWidth - cornerSize, frameTop),
            strokeWidth = cornerThickness
        )
        drawLine(
            color = frameColor,
            start = Offset(frameLeft + frameWidth, frameTop),
            end = Offset(frameLeft + frameWidth, frameTop + cornerSize),
            strokeWidth = cornerThickness
        )
        
        // Bottom-left corner
        drawLine(
            color = frameColor,
            start = Offset(frameLeft, frameTop + frameHeight),
            end = Offset(frameLeft + cornerSize, frameTop + frameHeight),
            strokeWidth = cornerThickness
        )
        drawLine(
            color = frameColor,
            start = Offset(frameLeft, frameTop + frameHeight),
            end = Offset(frameLeft, frameTop + frameHeight - cornerSize),
            strokeWidth = cornerThickness
        )
        
        // Bottom-right corner
        drawLine(
            color = frameColor,
            start = Offset(frameLeft + frameWidth, frameTop + frameHeight),
            end = Offset(frameLeft + frameWidth - cornerSize, frameTop + frameHeight),
            strokeWidth = cornerThickness
        )
        drawLine(
            color = frameColor,
            start = Offset(frameLeft + frameWidth, frameTop + frameHeight),
            end = Offset(frameLeft + frameWidth, frameTop + frameHeight - cornerSize),
            strokeWidth = cornerThickness
        )
    }
}


