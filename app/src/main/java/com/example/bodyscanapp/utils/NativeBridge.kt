package com.example.bodyscanapp.utils

object NativeBridge {
    init { 
        System.loadLibrary("bodyscan") 
    }

    data class ScanResult(
        val keypoints3d: FloatArray,   // 135*3
        val meshGlb: ByteArray,        // GLB binary
        val measurements: FloatArray   // e.g. waist, chest, hips, …
    )

    external fun processThreeImages(
        images: Array<ByteArray>,
        widths: IntArray,
        heights: IntArray,
        userHeightCm: Float
    ): ScanResult
}

