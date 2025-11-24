package com.example.bodyscanapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.bodyscanapp.ui.screens.CapturedImageData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for managing capture sequence state
 * 
 * Manages:
 * - Current capture step (front, left, right)
 * - Captured images
 * - Capture state (idle, capturing, complete)
 */
sealed class CaptureState {
    data object Idle : CaptureState()
    data object Capturing : CaptureState()
    data class Complete(val images: List<CapturedImageData>) : CaptureState()
    data class Error(val message: String) : CaptureState()
}

class CaptureSequenceViewModel : ViewModel() {
    private val _captureState = MutableStateFlow<CaptureState>(CaptureState.Idle)
    val captureState: StateFlow<CaptureState> = _captureState.asStateFlow()
    
    private val _capturedImages = MutableStateFlow<List<CapturedImageData>>(emptyList())
    val capturedImages: StateFlow<List<CapturedImageData>> = _capturedImages.asStateFlow()
    
    private val _currentStep = MutableStateFlow(0) // 0 = front, 1 = left, 2 = right
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()
    
    /**
     * Capture an image
     */
    fun captureImage(image: ByteArray, width: Int, height: Int) {
        val imageData = CapturedImageData(image, width, height)
        val updatedImages = _capturedImages.value + imageData
        _capturedImages.value = updatedImages
        
        // Move to next step if not complete
        if (updatedImages.size < 3) {
            _currentStep.value = updatedImages.size
        } else {
            _captureState.value = CaptureState.Complete(updatedImages)
        }
    }
    
    /**
     * Get all captured images
     */
    fun getCapturedImages(): List<CapturedImageData> = _capturedImages.value
    
    /**
     * Check if all images are captured
     */
    fun isComplete(): Boolean = _capturedImages.value.size >= 3
    
    /**
     * Reset capture state
     */
    fun reset() {
        _captureState.value = CaptureState.Idle
        _capturedImages.value = emptyList()
        _currentStep.value = 0
    }
    
    /**
     * Set capturing state
     */
    fun setCapturing() {
        _captureState.value = CaptureState.Capturing
    }
    
    /**
     * Set error state
     */
    fun setError(message: String) {
        _captureState.value = CaptureState.Error(message)
    }
}

