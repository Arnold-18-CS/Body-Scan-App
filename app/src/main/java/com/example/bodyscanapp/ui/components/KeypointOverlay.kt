package com.example.bodyscanapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * KeypointOverlay - Displays 2D keypoints on an image
 * 
 * Features:
 * - Draws image as background
 * - Draws red circles at each keypoint position
 * - Optional skeleton lines connecting keypoints (BODY_25 format)
 * 
 * @param imageBitmap The image to display
 * @param keypoints2d List of normalized keypoints (0.0-1.0) as Pair<Float, Float> (x, y)
 * @param modifier Modifier for the overlay
 * @param showSkeleton Whether to draw skeleton lines connecting keypoints
 * @param keypointColor Color for keypoint circles (default: Red)
 * @param skeletonColor Color for skeleton lines (default: Blue)
 */
@Composable
fun KeypointOverlay(
    imageBitmap: ImageBitmap,
    keypoints2d: List<Pair<Float, Float>>,
    modifier: Modifier = Modifier,
    showSkeleton: Boolean = false,
    keypointColor: Color = Color.Red,
    skeletonColor: Color = Color.Blue
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // Draw the image as background
        // Use drawImage - it's a member function of DrawScope
        // Draw the image to fill the canvas
        drawImage(
            image = imageBitmap,
            topLeft = Offset.Zero
        )
        
        // Validate keypoints
        val validKeypoints = keypoints2d.filter { (x, y) ->
            x in 0f..1f && y in 0f..1f
        }
        
        if (validKeypoints.isEmpty()) {
            return@Canvas
        }
        
        // Draw keypoints
        validKeypoints.forEach { (nx, ny) ->
            val x = nx * size.width
            val y = ny * size.height
            drawCircle(
                color = keypointColor,
                radius = 8f,
                center = Offset(x, y)
            )
        }
        
        // Draw skeleton lines if enabled (BODY_25 format connections)
        if (showSkeleton && validKeypoints.size >= 25) {
            // BODY_25 keypoint indices (simplified connections)
            // Note: This is a simplified skeleton - full BODY_25 has more connections
            val connections = listOf(
                // Head to neck
                Pair(0, 1), // Nose to neck
                // Torso
                Pair(1, 8), // Neck to mid-hip
                Pair(1, 2), // Neck to right shoulder
                Pair(1, 5), // Neck to left shoulder
                Pair(2, 3), // Right shoulder to right elbow
                Pair(3, 4), // Right elbow to right wrist
                Pair(5, 6), // Left shoulder to left elbow
                Pair(6, 7), // Left elbow to left wrist
                // Lower body
                Pair(8, 9), // Mid-hip to right hip
                Pair(8, 12), // Mid-hip to left hip
                Pair(9, 10), // Right hip to right knee
                Pair(10, 11), // Right knee to right ankle
                Pair(12, 13), // Left hip to left knee
                Pair(13, 14), // Left knee to left ankle
            )
            
            connections.forEach { (startIdx, endIdx) ->
                if (startIdx < validKeypoints.size && endIdx < validKeypoints.size) {
                    val start = validKeypoints[startIdx]
                    val end = validKeypoints[endIdx]
                    val startX = start.first * size.width
                    val startY = start.second * size.height
                    val endX = end.first * size.width
                    val endY = end.second * size.height
                    
                    drawLine(
                        color = skeletonColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3f
                    )
                }
            }
        }
    }
}

