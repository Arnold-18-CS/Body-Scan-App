package com.example.bodyscanapp.utils

/**
 * Utility for projecting 3D keypoints to 2D for display
 * 
 * This is a simplified projection for visualization purposes.
 * For accurate 2D keypoints, the native code should return them directly.
 */
object KeypointProjection {
    /**
     * Project 3D keypoints to 2D normalized coordinates (0.0-1.0)
     * 
     * Uses a simple orthographic projection assuming:
     * - Front view camera
     * - Keypoints are centered around origin
     * - Simple scaling to fit image bounds
     * 
     * @param keypoints3d Array of 3D keypoints (x, y, z) - 135*3 floats
     * @param imageWidth Image width in pixels
     * @param imageHeight Image height in pixels
     * @return List of normalized 2D keypoints (x, y) in range 0.0-1.0
     */
    fun project3DTo2D(
        keypoints3d: FloatArray,
        imageWidth: Int,
        imageHeight: Int
    ): List<Pair<Float, Float>> {
        if (keypoints3d.size < 135 * 3) {
            return emptyList()
        }
        
        // Find bounding box of 3D points
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE
        
        for (i in 0 until 135) {
            val x = keypoints3d[i * 3 + 0]
            val y = keypoints3d[i * 3 + 1]
            
            if (x != 0f || y != 0f) { // Skip zero/invalid points
                minX = minOf(minX, x)
                maxX = maxOf(maxX, x)
                minY = minOf(minY, y)
                maxY = maxOf(maxY, y)
            }
        }
        
        // If no valid points, return empty
        if (minX == Float.MAX_VALUE) {
            return emptyList()
        }
        
        // Calculate scale and offset to fit image
        val rangeX = maxX - minX
        val rangeY = maxY - minY
        
        if (rangeX == 0f || rangeY == 0f) {
            return emptyList()
        }
        
        // Scale to fit with padding (80% of image size)
        val scaleX = (imageWidth * 0.8f) / rangeX
        val scaleY = (imageHeight * 0.8f) / rangeY
        val scale = minOf(scaleX, scaleY)
        
        // Center the points
        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f
        
        // Project to 2D (front view - ignore Z)
        val keypoints2d = mutableListOf<Pair<Float, Float>>()
        for (i in 0 until 135) {
            val x3d = keypoints3d[i * 3 + 0]
            val y3d = keypoints3d[i * 3 + 1]
            
            if (x3d == 0f && y3d == 0f) {
                // Invalid point - place outside visible area
                keypoints2d.add(Pair(-1f, -1f))
            } else {
                // Project to 2D and normalize
                val x2d = ((x3d - centerX) * scale + imageWidth / 2f) / imageWidth
                val y2d = ((y3d - centerY) * scale + imageHeight / 2f) / imageHeight
                
                // Clamp to 0-1 range
                val normalizedX = x2d.coerceIn(0f, 1f)
                val normalizedY = y2d.coerceIn(0f, 1f)
                
                keypoints2d.add(Pair(normalizedX, normalizedY))
            }
        }
        
        return keypoints2d
    }
    
    /**
     * Project 3D keypoints to 2D for a specific view (front, left, right)
     * 
     * @param keypoints3d Array of 3D keypoints
     * @param viewIndex 0=front, 1=left, 2=right
     * @param imageWidth Image width
     * @param imageHeight Image height
     * @return List of normalized 2D keypoints
     */
    fun project3DTo2DForView(
        keypoints3d: FloatArray,
        viewIndex: Int,
        imageWidth: Int,
        imageHeight: Int
    ): List<Pair<Float, Float>> {
        // For now, use the same projection for all views
        // In a full implementation, this would use different camera angles
        return project3DTo2D(keypoints3d, imageWidth, imageHeight)
    }
}

