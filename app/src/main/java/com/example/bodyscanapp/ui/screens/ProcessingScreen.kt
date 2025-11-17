package com.example.bodyscanapp.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bodyscanapp.ui.theme.BodyScanAppTheme
import com.example.bodyscanapp.ui.theme.BodyScanBackground
import com.example.bodyscanapp.utils.NativeBridge
import com.example.bodyscanapp.utils.PerformanceLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Processing state enum
 */
enum class ProcessingState {
    PROCESSING,
    SUCCESS,
    FAILURE,
    CANCELLED
}

/**
 * Data class to hold processing status information
 */
data class ProcessingStatus(
    val progress: Float = 0f,
    val statusText: String = "",
    val state: ProcessingState = ProcessingState.PROCESSING,
    val errorMessage: String? = null
)

/**
 * ProcessingScreen - Displays processing progress with status updates
 *
 * @param modifier Modifier for the screen
 * @param capturedImages List of 3 captured images (ByteArray) - front, left, right
 * @param imageWidths Array of image widths
 * @param imageHeights Array of image heights
 * @param userHeightCm User height in centimeters
 * @param onProcessingComplete Callback when processing completes successfully (passes ScanResult)
 * @param onProcessingFailed Callback when processing fails with error message
 * @param onCancelClick Callback when user cancels processing
 * @param simulateProcessing If true, simulates processing (for testing). If false, calls NativeBridge
 * @param externalStatus Optional external status for manual control (overrides simulation)
 */
@Composable
fun ProcessingScreen(
    modifier: Modifier = Modifier,
    capturedImages: List<ByteArray> = emptyList(),
    imageWidths: List<Int> = emptyList(),
    imageHeights: List<Int> = emptyList(),
    userHeightCm: Float = 0f,
    onProcessingComplete: (NativeBridge.ScanResult) -> Unit = {},
    onProcessingFailed: (String) -> Unit = {},
    onCancelClick: () -> Unit = {},
    simulateProcessing: Boolean = false,
    externalStatus: ProcessingStatus? = null
) {
    // Performance logging
    val context = LocalContext.current
    val performanceLogger = remember { PerformanceLogger.getInstance(context) }
    
    // Internal state for simulated processing
    var internalStatus by remember { mutableStateOf(ProcessingStatus()) }
    
    // Use external status if provided, otherwise use internal
    val currentStatus = externalStatus ?: internalStatus
    
    // Animated progress value for smooth transitions
    val animatedProgress by animateFloatAsState(
        targetValue = currentStatus.progress,
        animationSpec = tween(durationMillis = 300),
        label = "progress_animation"
    )
    
    // Track processing start
    LaunchedEffect(Unit) {
        performanceLogger.startAction("image_processing")
        performanceLogger.logAction("processing_start", "ProcessingScreen", "images: ${capturedImages.size}, totalSize: ${capturedImages.sumOf { it.size }} bytes")
    }
    
    // Process images using NativeBridge or simulate
    LaunchedEffect(simulateProcessing, capturedImages.size) {
        if (externalStatus != null) {
            // External status provided, don't process
            return@LaunchedEffect
        }
        
        if (simulateProcessing) {
            // Simulate processing
            // Define processing stages with status text
            val stages = listOf(
                ProcessingStatus(0.0f, "Initializing processing..."),
                ProcessingStatus(0.15f, "Analyzing image quality..."),
                ProcessingStatus(0.35f, "Detecting key landmarks..."),
                ProcessingStatus(0.55f, "Measuring body proportions..."),
                ProcessingStatus(0.75f, "Calculating dimensions..."),
                ProcessingStatus(0.90f, "Finalizing measurements..."),
                ProcessingStatus(1.0f, "Processing complete!")
            )
            
            // Simulate processing with delays
            for (stage in stages) {
                if (currentStatus.state == ProcessingState.CANCELLED) {
                    break
                }
                
                internalStatus = stage.copy(state = ProcessingState.PROCESSING)
                
                // Delay between stages (total ~5 seconds)
                delay(700)
            }
            
            // Check if not cancelled before marking as success
            if (currentStatus.state != ProcessingState.CANCELLED) {
                internalStatus = internalStatus.copy(state = ProcessingState.SUCCESS)
                
                // Log processing completion
                val duration = performanceLogger.endAction("image_processing", "status: success")
                performanceLogger.logAction("processing_complete", "ProcessingScreen", "duration: ${duration}ms")
                
                // Wait a moment to show success state
                delay(1000)
                
                // Create mock result for simulation
                val mockResult = NativeBridge.ScanResult(
                    keypoints3d = FloatArray(135 * 3) { 0f },
                    meshGlb = ByteArray(0),
                    measurements = floatArrayOf(80f, 100f, 90f, 60f, 30f, 40f) // Mock measurements
                )
                
                // Call completion callback
                onProcessingComplete(mockResult)
            } else {
                // Log processing cancellation
                performanceLogger.endAction("image_processing", "status: cancelled")
                performanceLogger.logAction("processing_cancelled", "ProcessingScreen")
            }
        } else {
            // Real processing using NativeBridge
            if (capturedImages.size == 3 && 
                imageWidths.size == 3 && 
                imageHeights.size == 3 && 
                userHeightCm > 0f) {
                
                try {
                    // Update status
                    internalStatus = ProcessingStatus(
                        progress = 0.1f,
                        statusText = "Initializing processing...",
                        state = ProcessingState.PROCESSING
                    )
                    
                    // Update status before processing
                    internalStatus = ProcessingStatus(
                        progress = 0.3f,
                        statusText = "Detecting keypoints...",
                        state = ProcessingState.PROCESSING
                    )
                    
                    // Call NativeBridge on background thread
                    val result = withContext(Dispatchers.IO) {
                        NativeBridge.processThreeImages(
                            images = capturedImages.toTypedArray(),
                            widths = imageWidths.toIntArray(),
                            heights = imageHeights.toIntArray(),
                            userHeightCm = userHeightCm
                        )
                    }
                    
                    // Update status during processing
                    internalStatus = ProcessingStatus(
                        progress = 0.7f,
                        statusText = "Generating 3D mesh...",
                        state = ProcessingState.PROCESSING
                    )
                    
                    // Update status to success
                    internalStatus = ProcessingStatus(
                        progress = 1.0f,
                        statusText = "Processing complete!",
                        state = ProcessingState.SUCCESS
                    )
                    
                    // Log processing completion
                    val duration = performanceLogger.endAction("image_processing", "status: success")
                    performanceLogger.logAction("processing_complete", "ProcessingScreen", "duration: ${duration}ms")
                    
                    // Wait a moment to show success state
                    delay(1000)
                    
                    // Call completion callback with result
                    onProcessingComplete(result)
                    
                } catch (e: Exception) {
                    // Log error
                    android.util.Log.e("ProcessingScreen", "Processing error", e)
                    performanceLogger.endAction("image_processing", "status: error")
                    
                    // Update status to failure
                    internalStatus = ProcessingStatus(
                        progress = 0f,
                        statusText = "Processing failed",
                        state = ProcessingState.FAILURE,
                        errorMessage = e.message ?: "Unknown error occurred"
                    )
                    
                    // Call failure callback
                    onProcessingFailed(e.message ?: "Unknown error occurred")
                }
            } else {
                // Invalid input
                internalStatus = ProcessingStatus(
                    progress = 0f,
                    statusText = "Invalid input data",
                    state = ProcessingState.FAILURE,
                    errorMessage = "Expected 3 images with dimensions and valid height"
                )
                onProcessingFailed("Invalid input data")
            }
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BodyScanBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Text(
            text = when (currentStatus.state) {
                ProcessingState.PROCESSING -> "Processing Your Image"
                ProcessingState.SUCCESS -> "Success!"
                ProcessingState.FAILURE -> "Processing Failed"
                ProcessingState.CANCELLED -> "Cancelled"
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Progress indicator or status icon
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            when (currentStatus.state) {
                ProcessingState.PROCESSING -> {
                    // Circular progress indicator with percentage
                    CircularProgressWithPercentage(
                        progress = animatedProgress,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                ProcessingState.SUCCESS -> {
                    // Success checkmark
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color(0xFF4CAF50), shape = androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Success",
                            tint = Color.White,
                            modifier = Modifier.size(120.dp)
                        )
                    }
                }
                ProcessingState.FAILURE -> {
                    // Failure X mark
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color(0xFFF44336), shape = androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Failure",
                            tint = Color.White,
                            modifier = Modifier.size(120.dp)
                        )
                    }
                }
                ProcessingState.CANCELLED -> {
                    // Cancelled state (can add custom icon if needed)
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color.Gray, shape = androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancelled",
                            tint = Color.White,
                            modifier = Modifier.size(120.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Status text
        Text(
            text = currentStatus.statusText,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        
        // Error message (if failure)
        if (currentStatus.state == ProcessingState.FAILURE && currentStatus.errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = currentStatus.errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFF44336),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Cancel/Action button
        when (currentStatus.state) {
            ProcessingState.PROCESSING -> {
                OutlinedButton(
                    onClick = {
                        // Log cancel action
                        performanceLogger.logAction("button_click", "cancel_processing")
                        performanceLogger.endAction("image_processing", "status: cancelled_by_user")
                        
                        internalStatus = internalStatus.copy(state = ProcessingState.CANCELLED)
                        onCancelClick()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFF44336)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = "Cancel Scan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            ProcessingState.SUCCESS -> {
                // Success state - navigation will be handled by parent
                // No button needed as navigation happens automatically
            }
            ProcessingState.FAILURE -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onCancelClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = "Try Again",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            ProcessingState.CANCELLED -> {
                // No button needed, will navigate back automatically
            }
        }
    }
}

/**
 * Circular progress indicator with percentage display
 */
@Composable
fun CircularProgressWithPercentage(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 16f,
    backgroundColor: Color = Color(0xFFE0E0E0),
    progressColor: Color = Color(0xFF2196F3)
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Background circle
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = size.minDimension
            val radius = (canvasSize - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)
            
            // Draw background circle
            drawCircle(
                color = backgroundColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )
            
            // Draw progress arc
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(
                    center.x - radius,
                    center.y - radius
                ),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        
        // Percentage text
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            fontSize = 48.sp
        )
    }
}

// Preview Composables
@Preview(showBackground = true, name = "Processing - In Progress")
@Composable
fun ProcessingScreenPreview() {
    BodyScanAppTheme {
        ProcessingScreen(
            simulateProcessing = false,
            externalStatus = ProcessingStatus(
                progress = 0.45f,
                statusText = "Detecting key landmarks...",
                state = ProcessingState.PROCESSING
            )
        )
    }
}

@Preview(showBackground = true, name = "Processing - Success")
@Composable
fun ProcessingScreenSuccessPreview() {
    BodyScanAppTheme {
        ProcessingScreen(
            simulateProcessing = false,
            externalStatus = ProcessingStatus(
                progress = 1.0f,
                statusText = "Processing complete!",
                state = ProcessingState.SUCCESS
            )
        )
    }
}

@Preview(showBackground = true, name = "Processing - Failure")
@Composable
fun ProcessingScreenFailurePreview() {
    BodyScanAppTheme {
        ProcessingScreen(
            simulateProcessing = false,
            externalStatus = ProcessingStatus(
                progress = 0.65f,
                statusText = "An error occurred during processing",
                state = ProcessingState.FAILURE,
                errorMessage = "Unable to detect body landmarks. Please ensure proper lighting and body visibility."
            )
        )
    }
}

