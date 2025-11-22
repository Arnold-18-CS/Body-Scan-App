package com.example.bodyscanapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import kotlin.math.min
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp

/**
 * Overlay component that displays an image with keypoints overlaid on top
 * 
 * @param imageBitmap The image to display
 * @param keypoints2d List of normalized keypoints (0.0-1.0) as pairs of (x, y)
 * @param modifier Modifier for the component
 * @param keypointColor Color for keypoint circles
 * @param keypointRadius Radius of keypoint circles
 * @param showSkeleton Whether to draw skeleton connections between keypoints
 * @param skeletonColor Color for skeleton lines
 * @param skeletonStrokeWidth Width of skeleton lines
 * @param showAllKeypoints If true, shows all 135 keypoints with indices and color coding
 * @param showKeypointIndices If true, displays index numbers next to each keypoint
 */
@Composable
fun KeypointOverlay(
    imageBitmap: ImageBitmap,
    keypoints2d: List<Pair<Float, Float>>,
    modifier: Modifier = Modifier,
    keypointColor: Color = Color.Red,
    keypointRadius: Float = 3f,
    showSkeleton: Boolean = false,
    skeletonColor: Color = Color.Blue,
    skeletonStrokeWidth: Float = 2f,
    showAllKeypoints: Boolean = true,
    showKeypointIndices: Boolean = true
) {
    val textMeasurer = rememberTextMeasurer()
    
    Box(modifier = modifier.fillMaxSize()) {
        // Draw the image
        Image(
            bitmap = imageBitmap,
            contentDescription = "Image with keypoints",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        
        // Overlay keypoints on top
        Canvas(modifier = Modifier.fillMaxSize()) {
            val imageWidth = imageBitmap.width.toFloat()
            val imageHeight = imageBitmap.height.toFloat()
            
            // Calculate scale to fit image within canvas while maintaining aspect ratio
            val scaleX = size.width / imageWidth
            val scaleY = size.height / imageHeight
            val scale = minOf(scaleX, scaleY)
            
            // Calculate scaled image dimensions
            val scaledWidth = imageWidth * scale
            val scaledHeight = imageHeight * scale
            
            // Calculate offset to center the image
            val offsetX = (size.width - scaledWidth) / 2f
            val offsetY = (size.height - scaledHeight) / 2f
            
            // Color coding: MediaPipe landmarks (0-32) vs interpolated (33-134)
            val mediapipeColor = Color(0xFFFF5722) // Orange-red for MediaPipe landmarks
            val interpolatedColor = Color(0xFF00BCD4) // Cyan for interpolated keypoints
            val invalidColor = Color(0xFF9E9E9E) // Gray for invalid keypoints
            
            var validKeypointCount = 0
            var invalidKeypointCount = 0
            
            // Draw all keypoints with index labels
            keypoints2d.forEachIndexed { index, (nx, ny) ->
                val isValid = nx >= 0f && nx <= 1f && ny >= 0f && ny <= 1f && (nx > 0f || ny > 0f)
                
                // Convert normalized coordinates to canvas coordinates
                // For invalid keypoints, still calculate position but mark them differently
                val x = offsetX + (nx.coerceIn(0f, 1f) * scaledWidth)
                val y = offsetY + (ny.coerceIn(0f, 1f) * scaledHeight)
                
                // Determine color based on keypoint type
                val color = when {
                    !isValid -> invalidColor
                    index < 33 -> mediapipeColor // First 33 are MediaPipe landmarks
                    else -> interpolatedColor // Rest are interpolated
                }
                
                // Use larger radius for better visibility when showing all keypoints
                val radius = if (showAllKeypoints) {
                    if (index < 33) 4f else 3f // Larger for MediaPipe landmarks
                } else {
                    keypointRadius
                }
                
                // Draw keypoint circle
                drawCircle(
                    color = color,
                    radius = radius,
                    center = Offset(x, y)
                )
                
                // Draw index label if enabled
                if (showKeypointIndices && showAllKeypoints) {
                    val text = index.toString()
                    val textStyle = TextStyle(
                        fontSize = if (index < 33) 10.sp else 8.sp,
                        color = if (isValid) Color.White else Color.Gray
                    )
                    val textLayoutResult = textMeasurer.measure(text, textStyle)
                    
                    // Draw text with background for better visibility
                    val padding = 2f
                    val bgTopLeft = Offset(
                        x - textLayoutResult.size.width / 2 - padding,
                        y - radius - textLayoutResult.size.height - padding
                    )
                    val bgSize = Size(
                        textLayoutResult.size.width + padding * 2,
                        textLayoutResult.size.height + padding * 2
                    )
                    
                    // Draw semi-transparent background with rounded corners
                    val cornerRadius = 4f
                    val radius = min(cornerRadius, min(bgSize.width, bgSize.height) / 2f)
                    val roundRect = RoundRect(
                        left = bgTopLeft.x,
                        top = bgTopLeft.y,
                        right = bgTopLeft.x + bgSize.width,
                        bottom = bgTopLeft.y + bgSize.height,
                        radiusX = radius,
                        radiusY = radius
                    )
                    val path = Path().apply {
                        addRoundRect(roundRect)
                    }
                    drawPath(path, Color.Black.copy(alpha = 0.78f))
                    
                    // Draw the text
                    drawText(
                        textMeasurer = textMeasurer,
                        text = text,
                        style = textStyle,
                        topLeft = Offset(
                            x - textLayoutResult.size.width / 2,
                            y - radius - textLayoutResult.size.height
                        )
                    )
                }
                
                if (isValid) {
                    validKeypointCount++
                } else {
                    invalidKeypointCount++
                }
            }
            
            // Draw debug info showing keypoint counts
            if (showAllKeypoints) {
                val mediapipeValidCount = keypoints2d.take(33).count { 
                    it.first >= 0f && it.first <= 1f && it.second >= 0f && it.second <= 1f && 
                    (it.first > 0f || it.second > 0f) 
                }
                val interpolatedValidCount = keypoints2d.drop(33).count { 
                    it.first >= 0f && it.first <= 1f && it.second >= 0f && it.second <= 1f && 
                    (it.first > 0f || it.second > 0f) 
                }
                val debugText = "Total: ${keypoints2d.size} | Valid: $validKeypointCount | Invalid: $invalidKeypointCount | MediaPipe (0-32): $mediapipeValidCount | Interpolated (33-134): $interpolatedValidCount"
                val debugTextStyle = TextStyle(
                    fontSize = 12.sp,
                    color = Color.White
                )
                val debugTextLayout = textMeasurer.measure(debugText, debugTextStyle)
                
                // Draw background for debug text
                val padding = 8f
                val bgTopLeft = Offset(8f, 8f)
                val bgSize = Size(
                    debugTextLayout.size.width + padding * 2,
                    debugTextLayout.size.height + padding * 2
                )
                
                // Draw background with rounded corners
                val cornerRadius = 8f
                val radius = min(cornerRadius, min(bgSize.width, bgSize.height) / 2f)
                val roundRect = RoundRect(
                    left = bgTopLeft.x,
                    top = bgTopLeft.y,
                    right = bgTopLeft.x + bgSize.width,
                    bottom = bgTopLeft.y + bgSize.height,
                    radiusX = radius,
                    radiusY = radius
                )
                val path = Path().apply {
                    addRoundRect(roundRect)
                }
                drawPath(path, Color.Black.copy(alpha = 0.86f))
                
                // Draw debug text
                drawText(
                    textMeasurer = textMeasurer,
                    text = debugText,
                    style = debugTextStyle,
                    topLeft = Offset(8f + padding, 8f + padding)
                )
            }
            
            // Draw skeleton connections if enabled
            if (showSkeleton && keypoints2d.size >= 2) {
                // Draw lines between adjacent keypoints (simple connection)
                // This is a basic skeleton - you can customize based on your keypoint structure
                for (i in 0 until keypoints2d.size - 1) {
                    val (nx1, ny1) = keypoints2d[i]
                    val (nx2, ny2) = keypoints2d[i + 1]
                    
                    // Skip if either keypoint is invalid
                    if (nx1 < 0f || nx1 > 1f || ny1 < 0f || ny1 > 1f ||
                        nx2 < 0f || nx2 > 1f || ny2 < 0f || ny2 > 1f) {
                        continue
                    }
                    
                    val x1 = offsetX + (nx1 * scaledWidth)
                    val y1 = offsetY + (ny1 * scaledHeight)
                    val x2 = offsetX + (nx2 * scaledWidth)
                    val y2 = offsetY + (ny2 * scaledHeight)
                    
                    drawLine(
                        color = skeletonColor,
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = skeletonStrokeWidth
                    )
                }
            }
        }
    }
}

