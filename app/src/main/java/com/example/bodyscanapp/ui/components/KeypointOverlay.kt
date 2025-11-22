package com.example.bodyscanapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale

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
    skeletonStrokeWidth: Float = 2f
) {
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
            
            // Draw keypoints
            keypoints2d.forEach { (nx, ny) ->
                // Skip invalid keypoints (outside 0-1 range)
                if (nx < 0f || nx > 1f || ny < 0f || ny > 1f) {
                    return@forEach
                }
                
                // Convert normalized coordinates to canvas coordinates
                val x = offsetX + (nx * scaledWidth)
                val y = offsetY + (ny * scaledHeight)
                
                // Draw keypoint circle
                drawCircle(
                    color = keypointColor,
                    radius = keypointRadius,
                    center = Offset(x, y)
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

