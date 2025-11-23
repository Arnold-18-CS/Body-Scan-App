package com.example.bodyscanapp.utils

import androidx.annotation.Keep

object NativeBridge {
    init { 
        System.loadLibrary("bodyscan") 
    }

    @Keep
    data class ScanResult(
        val keypoints3d: FloatArray,   // 135*3 (empty for single image)
        val meshGlb: ByteArray,        // GLB binary (empty for single image)
        val measurements: FloatArray,  // e.g. waist, chest, hips, …
        val keypoints2d: FloatArray? = null  // 135*2 normalized (x, y) coordinates
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ScanResult

            if (!keypoints3d.contentEquals(other.keypoints3d)) return false
            if (!meshGlb.contentEquals(other.meshGlb)) return false
            if (!measurements.contentEquals(other.measurements)) return false
            if (keypoints2d != null) {
                if (other.keypoints2d == null || !keypoints2d.contentEquals(other.keypoints2d)) return false
            } else if (other.keypoints2d != null) return false

            return true
        }

        override fun hashCode(): Int {
            var result = keypoints3d.contentHashCode()
            result = 31 * result + meshGlb.contentHashCode()
            result = 31 * result + measurements.contentHashCode()
            result = 31 * result + (keypoints2d?.contentHashCode() ?: 0)
            return result
        }
    }

    // Single image processing
    external fun processOneImage(
        image: ByteArray,
        width: Int,
        height: Int,
        userHeightCm: Float
    ): ScanResult

    // Initialize MediaPipe Pose Detector
    external fun initializeMediaPipe(context: android.content.Context): Boolean

    // Image validation
    data class ImageValidationResult(
        val hasPerson: Boolean,
        val isFullBody: Boolean,
        val confidence: Float,
        val message: String
    ) {
        val isValid: Boolean
            get() = hasPerson && isFullBody
    }

    // TODO: Update to use MediaPipe for validation
    external fun validateImage(
        image: ByteArray,
        width: Int,
        height: Int
    ): ImageValidationResult

    // Detect keypoints for preview overlay
    // Returns FloatArray of 135*2 = 270 floats (normalized x, y coordinates)
    external fun detectKeypoints(
        image: ByteArray,
        width: Int,
        height: Int
    ): FloatArray

    // Multi-image processing with MediaPipe and 3D reconstruction
    external fun processThreeImages(
        images: Array<ByteArray>,
        widths: IntArray,
        heights: IntArray,
        userHeightCm: Float
    ): ScanResult
}

