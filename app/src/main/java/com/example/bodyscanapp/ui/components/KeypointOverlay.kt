package com.example.bodyscanapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * KeypointOverlay - Displays 2D keypoints on an image with BODY_25 skeleton connections
 * 
 * Features:
 * - Draws image as background
 * - Draws circles at each keypoint position
 * - Optional skeleton lines connecting keypoints (complete BODY_25 format)
 * - Handles missing/invalid keypoints gracefully
 * - Supports partial keypoint sets (doesn't require all 25 keypoints)
 * 
 * BODY_25 Keypoint Indices:
 * 0: Nose, 1: Neck, 2: Right Shoulder, 3: Right Elbow, 4: Right Wrist,
 * 5: Left Shoulder, 6: Left Elbow, 7: Left Wrist, 8: Mid-Hip,
 * 9: Right Hip, 10: Right Knee, 11: Right Ankle, 12: Left Hip,
 * 13: Left Knee, 14: Left Ankle, 15: Right Eye, 16: Left Eye,
 * 17: Right Ear, 18: Left Ear, 19: Left Big Toe, 20: Left Small Toe,
 * 21: Left Heel, 22: Right Big Toe, 23: Right Small Toe, 24: Right Heel
 * 
 * @param imageBitmap The image to display as background
 * @param keypoints2d List of normalized keypoints (0.0-1.0) as Pair<Float, Float> (x, y).
 *                     Invalid keypoints should have coordinates outside [0,1] range (e.g., -1, -1)
 * @param modifier Modifier for the overlay
 * @param showSkeleton Whether to draw skeleton lines connecting keypoints (default: false)
 * @param keypointColor Color for keypoint circles (default: Red)
 * @param skeletonColor Color for skeleton lines (default: Blue)
 * @param keypointRadius Radius of keypoint circles in pixels (default: 8f)
 * @param skeletonStrokeWidth Width of skeleton lines in pixels (default: 3f)
 */
@Composable
fun KeypointOverlay(
    imageBitmap: ImageBitmap,
    keypoints2d: List<Pair<Float, Float>>,
    modifier: Modifier = Modifier,
    showSkeleton: Boolean = false,
    keypointColor: Color = Color.Red,
    skeletonColor: Color = Color.Blue,
    keypointRadius: Float = 8f,
    skeletonStrokeWidth: Float = 3f
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // Validate image bitmap
        if (imageBitmap.width <= 0 || imageBitmap.height <= 0) {
            return@Canvas
        }
        
        // Calculate scale to fit image within canvas while maintaining aspect ratio
        val imageWidth = imageBitmap.width.toFloat()
        val imageHeight = imageBitmap.height.toFloat()
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        // Calculate scale factors for both dimensions
        val scaleX = canvasWidth / imageWidth
        val scaleY = canvasHeight / imageHeight
        // Use the smaller scale to ensure image fits within canvas
        val scale = minOf(scaleX, scaleY)
        
        // Calculate scaled image dimensions
        val scaledWidth = imageWidth * scale
        val scaledHeight = imageHeight * scale
        
        // Calculate offset to center the image
        val offsetX = (canvasWidth - scaledWidth) / 2f
        val offsetY = (canvasHeight - scaledHeight) / 2f
        
        // Draw the entire image scaled and centered
        // Clip to canvas bounds to ensure we only draw within the visible area
        clipRect(
            left = 0f,
            top = 0f,
            right = canvasWidth,
            bottom = canvasHeight
        ) {
            // Use transform to scale and position the full image
            // Scale first, then translate to position
            // When scaling first, translation happens in scaled space, so divide by scale
            withTransform({
                scale(scale, scale)
                translate(offsetX / scale, offsetY / scale)
            }) {
                // Draw the entire image starting from (0,0) - this draws the FULL image
                drawImage(
                    image = imageBitmap,
                    topLeft = Offset.Zero
                )
            }
        }
        
        // Validate and filter keypoints
        // Valid keypoints must be in [0, 1] range (normalized coordinates)
        // Invalid keypoints (e.g., -1, -1) are filtered out
        val validKeypoints = keypoints2d.mapIndexedNotNull { index, (x, y) ->
            if (x in 0f..1f && y in 0f..1f) {
                index to Pair(x, y)
            } else {
                null
            }
        }
        
        if (validKeypoints.isEmpty()) {
            return@Canvas
        }
        
        // Draw keypoints as circles
        // Transform normalized coordinates (0.0-1.0 relative to original image) 
        // to canvas coordinates accounting for scale and offset
        validKeypoints.forEach { (_, point) ->
            val (nx, ny) = point
            // Convert normalized image coordinates to canvas coordinates
            val x = offsetX + (nx * imageWidth * scale)
            val y = offsetY + (ny * imageHeight * scale)
            drawCircle(
                color = keypointColor,
                radius = keypointRadius,
                center = Offset(x, y)
            )
        }
        
        // Draw skeleton lines if enabled (complete BODY_25 format connections)
        if (showSkeleton) {
            // Create a map for quick keypoint lookup by index
            val keypointMap = validKeypoints.associate { it.first to it.second }
            
            // Complete BODY_25 skeleton connections
            // Format: Pair(startIndex, endIndex)
            val body25Connections = listOf(
                // Head connections
                Pair(0, 1),   // Nose to Neck
                Pair(0, 15), // Nose to Right Eye
                Pair(0, 16), // Nose to Left Eye
                Pair(15, 17), // Right Eye to Right Ear
                Pair(16, 18), // Left Eye to Left Ear
                
                // Torso connections
                Pair(1, 2),  // Neck to Right Shoulder
                Pair(1, 5),  // Neck to Left Shoulder
                Pair(1, 8),  // Neck to Mid-Hip
                Pair(2, 5),  // Right Shoulder to Left Shoulder
                
                // Right arm connections
                Pair(2, 3),  // Right Shoulder to Right Elbow
                Pair(3, 4),  // Right Elbow to Right Wrist
                
                // Left arm connections
                Pair(5, 6),  // Left Shoulder to Left Elbow
                Pair(6, 7),  // Left Elbow to Left Wrist
                
                // Torso lower connections
                Pair(8, 9),  // Mid-Hip to Right Hip
                Pair(8, 12), // Mid-Hip to Left Hip
                Pair(9, 12), // Right Hip to Left Hip
                
                // Right leg connections
                Pair(9, 10),  // Right Hip to Right Knee
                Pair(10, 11), // Right Knee to Right Ankle
                Pair(11, 22), // Right Ankle to Right Big Toe
                Pair(11, 23), // Right Ankle to Right Small Toe
                Pair(11, 24), // Right Ankle to Right Heel
                Pair(23, 24), // Right Small Toe to Right Big Toe
                
                // Left leg connections
                Pair(12, 13), // Left Hip to Left Knee
                Pair(13, 14), // Left Knee to Left Ankle
                Pair(14, 19), // Left Ankle to Left Big Toe
                Pair(14, 20), // Left Ankle to Left Small Toe
                Pair(14, 21), // Left Ankle to Left Heel
                Pair(19, 20), // Left Big Toe to Left Small Toe
            )
            
            // Draw skeleton lines only if both keypoints are valid
            body25Connections.forEach { (startIdx, endIdx) ->
                val startPoint = keypointMap[startIdx]
                val endPoint = keypointMap[endIdx]
                
                // Only draw if both keypoints exist and are valid
                if (startPoint != null && endPoint != null) {
                    // Transform normalized coordinates to canvas coordinates
                    val startX = offsetX + (startPoint.first * imageWidth * scale)
                    val startY = offsetY + (startPoint.second * imageHeight * scale)
                    val endX = offsetX + (endPoint.first * imageWidth * scale)
                    val endY = offsetY + (endPoint.second * imageHeight * scale)
                    
                    drawLine(
                        color = skeletonColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = skeletonStrokeWidth
                    )
                }
            }
        }
    }
}

