package com.example.bodyscanapp.navigation

import com.example.bodyscanapp.utils.NativeBridge

/**
 * Type-safe navigation arguments for Body Scan App
 * 
 * These data classes ensure type safety when passing data between screens.
 * For large data (like images), we use ViewModel/StateFlow instead of navigation arguments.
 */

/**
 * Arguments for CaptureSequenceScreen
 * @param heightCm User height in centimeters
 */
data class CaptureArgs(
    val heightCm: Float
)

/**
 * Arguments for ProcessingScreen
 * Note: Images are passed via ViewModel/StateFlow instead of navigation args
 * to avoid serialization issues with large ByteArrays
 */
data class ProcessingArgs(
    val userHeightCm: Float
    // Images are passed via ViewModel/StateFlow, not navigation args
)

/**
 * Arguments for Result3DScreen
 * Note: ScanResult and images are passed via ViewModel/StateFlow instead of navigation args
 * to avoid serialization issues with large ByteArrays
 * 
 * This is a placeholder - actual data is passed via ViewModel/StateFlow
 */
object ResultArgs {
    // ScanResult and images are passed via ViewModel/StateFlow, not navigation args
    // This avoids serialization issues with large ByteArrays
}

