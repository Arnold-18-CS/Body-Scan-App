package com.example.bodyscanapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.bodyscanapp.ui.screens.ProcessingState
import com.example.bodyscanapp.ui.screens.ProcessingStatus
import com.example.bodyscanapp.utils.NativeBridge
import com.example.bodyscanapp.utils.PerformanceLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * ViewModel for managing processing state
 * 
 * Manages:
 * - Processing status and progress
 * - NativeBridge processing results
 * - Error handling
 */
class ProcessingViewModel(
    private val performanceLogger: PerformanceLogger
) : ViewModel() {
    private val _processingState = MutableStateFlow<ProcessingStatus>(
        ProcessingStatus(
            progress = 0f,
            statusText = "Initializing...",
            state = ProcessingState.PROCESSING
        )
    )
    val processingState: StateFlow<ProcessingStatus> = _processingState.asStateFlow()
    
    private val _scanResult = MutableStateFlow<NativeBridge.ScanResult?>(null)
    val scanResult: StateFlow<NativeBridge.ScanResult?> = _scanResult.asStateFlow()
    
    /**
     * Process images using NativeBridge
     * 
     * @param images List of 3 captured images (ByteArray)
     * @param widths Array of image widths
     * @param heights Array of image heights
     * @param userHeightCm User height in centimeters
     * @return Result containing ScanResult on success, or error message on failure
     */
    suspend fun processImages(
        images: List<ByteArray>,
        widths: IntArray,
        heights: IntArray,
        userHeightCm: Float
    ): Result<NativeBridge.ScanResult> {
        return try {
            // Validate input
            if (images.size != 3 || widths.size != 3 || heights.size != 3) {
                return Result.failure(IllegalArgumentException("Expected 3 images with dimensions"))
            }
            
            if (userHeightCm <= 0f) {
                return Result.failure(IllegalArgumentException("Invalid user height"))
            }
            
            // Update status
            _processingState.value = ProcessingStatus(
                progress = 0.1f,
                statusText = "Initializing processing...",
                state = ProcessingState.PROCESSING
            )
            
            // Log processing start
            performanceLogger.startAction("image_processing")
            performanceLogger.logAction("processing_start", "ProcessingViewModel", 
                "images: ${images.size}, totalSize: ${images.sumOf { it.size }} bytes")
            
            // Update status
            _processingState.value = ProcessingStatus(
                progress = 0.3f,
                statusText = "Detecting keypoints...",
                state = ProcessingState.PROCESSING
            )
            
            // Process on background thread
            val result = withContext(Dispatchers.IO) {
                NativeBridge.processThreeImages(
                    images = images.toTypedArray(),
                    widths = widths,
                    heights = heights,
                    userHeightCm = userHeightCm
                )
            }
            
            // Update status
            _processingState.value = ProcessingStatus(
                progress = 0.7f,
                statusText = "Generating 3D mesh...",
                state = ProcessingState.PROCESSING
            )
            
            // Update status to success
            _processingState.value = ProcessingStatus(
                progress = 1.0f,
                statusText = "Processing complete!",
                state = ProcessingState.SUCCESS
            )
            
            // Log completion
            val duration = performanceLogger.endAction("image_processing", "status: success")
            performanceLogger.logAction("processing_complete", "ProcessingViewModel", 
                "duration: ${duration}ms")
            
            _scanResult.value = result
            Result.success(result)
            
        } catch (e: Exception) {
            // Log error
            android.util.Log.e("ProcessingViewModel", "Processing error", e)
            performanceLogger.endAction("image_processing", "status: error")
            
            // Update status to failure
            _processingState.value = ProcessingStatus(
                progress = 0f,
                statusText = "Processing failed",
                state = ProcessingState.FAILURE,
                errorMessage = e.message ?: "Unknown error occurred"
            )
            
            Result.failure(e)
        }
    }
    
    /**
     * Reset processing state
     */
    fun reset() {
        _processingState.value = ProcessingStatus(
            progress = 0f,
            statusText = "Initializing...",
            state = ProcessingState.PROCESSING
        )
        _scanResult.value = null
    }
    
    /**
     * Cancel processing
     */
    fun cancel() {
        _processingState.value = ProcessingStatus(
            progress = 0f,
            statusText = "Cancelled",
            state = ProcessingState.CANCELLED
        )
        performanceLogger.endAction("image_processing", "status: cancelled")
    }
}

