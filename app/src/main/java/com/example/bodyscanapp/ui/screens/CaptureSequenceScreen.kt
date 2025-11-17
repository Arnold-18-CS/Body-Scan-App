package com.example.bodyscanapp.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.bodyscanapp.data.HeightData
import com.example.bodyscanapp.ui.components.FramingOverlay
import com.example.bodyscanapp.ui.theme.BodyScanBackground
import com.example.bodyscanapp.utils.PerformanceLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executor

/**
 * Enum for capture steps
 */
enum class CaptureStep {
    FRONT,
    LEFT,
    RIGHT
}

/**
 * Data class to hold captured image with metadata
 */
data class CapturedImageData(
    val imageBytes: ByteArray,
    val width: Int,
    val height: Int
)

/**
 * CaptureSequenceScreen - Multi-photo capture flow
 * 
 * Features:
 * - Step 1: Display height (from previous screen)
 * - Step 2: Front photo capture
 * - Step 3: Left-side photo capture
 * - Step 4: Right-side photo capture
 * - Progress indicator (1/3, 2/3, 3/3)
 * - Camera preview with FramingOverlay
 * - Navigation to ProcessingScreen after all photos captured
 * 
 * @param modifier Modifier for the screen
 * @param heightData User height from previous screen
 * @param onBackClick Callback when user goes back
 * @param onCaptureComplete Callback when all 3 photos are captured (passes List<CapturedImageData>)
 */
@Composable
fun CaptureSequenceScreen(
    modifier: Modifier = Modifier,
    heightData: HeightData? = null,
    onBackClick: () -> Unit = {},
    onCaptureComplete: (List<CapturedImageData>, Float) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Performance logging
    val performanceLogger = remember { PerformanceLogger.getInstance(context) }
    
    // Camera state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    // Capture state
    var currentStep by remember { mutableStateOf(CaptureStep.FRONT) }
    var capturedImages by remember { mutableStateOf<List<CapturedImageData>>(emptyList()) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isCapturing by remember { mutableStateOf(false) }
    
    // Camera permission launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }
    
    // Request permission if not granted
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }
    
    // Get step instruction text
    val stepInstruction = when (currentStep) {
        CaptureStep.FRONT -> "Position yourself facing the camera"
        CaptureStep.LEFT -> "Turn to your left"
        CaptureStep.RIGHT -> "Turn to your right"
    }
    
    // Get progress (1/3, 2/3, 3/3)
    val progress = (capturedImages.size + if (isCapturing) 0 else 1) / 3f
    
    // Navigate to processing when all 3 photos are captured
    LaunchedEffect(capturedImages.size) {
        if (capturedImages.size == 3 && heightData != null) {
            val userHeightCm = heightData.toCentimeters()
            onCaptureComplete(capturedImages, userHeightCm)
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BodyScanBackground)
    ) {
        // Top bar with back button and progress
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2196F3))
                .padding(16.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            Text(
                text = "Step ${capturedImages.size + 1}/3",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        // Progress indicator
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF2196F3)
        )
        
        // Instruction text
        Text(
            text = stepInstruction,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        
        // Height display
        heightData?.let { height ->
            Text(
                text = "Height: ${height.getDisplayValue()} (${height.toCentimeters().toInt()} cm)",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(
                        Color(0xFFC7C7C7),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Camera Preview with Framing Overlay
        if (hasCameraPermission) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Camera Preview
                CaptureSequenceCameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onImageCaptureReady = { capture: ImageCapture ->
                        imageCapture = capture
                    },
                    lifecycleOwner = lifecycleOwner
                )
                
                // Framing Overlay
                FramingOverlay(
                    modifier = Modifier.fillMaxSize(),
                    isInFrame = !isCapturing && imageCapture != null
                )
            }
        } else {
            // Permission not granted
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Camera permission is required",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Capture Button
        Button(
            onClick = {
                if (imageCapture != null && !isCapturing) {
                    isCapturing = true
                    performanceLogger.logAction("button_click", "capture_button_${currentStep.name}")
                    performanceLogger.startAction("image_capture_${currentStep.name}")
                    
                    captureImageWithMetadata(
                        imageCapture = imageCapture!!,
                        context = context,
                        onImageCaptured = { capturedData ->
                            val duration = performanceLogger.endAction(
                                "image_capture_${currentStep.name}",
                                "size: ${capturedData.imageBytes.size} bytes"
                            )
                            
                            // Add to captured images
                            capturedImages = capturedImages + capturedData
                            
                            // Move to next step
                            currentStep = when (currentStep) {
                                CaptureStep.FRONT -> CaptureStep.LEFT
                                CaptureStep.LEFT -> CaptureStep.RIGHT
                                CaptureStep.RIGHT -> CaptureStep.FRONT // Should not reach here
                            }
                            
                            isCapturing = false
                        },
                        onError = { exception ->
                            performanceLogger.endAction(
                                "image_capture_${currentStep.name}",
                                "error: ${exception.message}"
                            )
                            
                            Log.e("CaptureSequenceScreen", "Capture error", exception)
                            isCapturing = false
                        }
                    )
                }
            },
            enabled = hasCameraPermission && !isCapturing && imageCapture != null,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2196F3),
                contentColor = Color.White,
                disabledContainerColor = Color.Gray,
                disabledContentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = if (isCapturing) "Capturing..." else "Capture Photo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// A state holder for managing camera-related state and logic.
private class CaptureSequenceCameraState(
    val context: Context,
    val lifecycleOwner: LifecycleOwner,
    val onImageCaptureReady: (ImageCapture) -> Unit
) {
    val previewView: PreviewView = PreviewView(context)
    private var cameraProvider: ProcessCameraProvider? = null

    suspend fun setupCamera() {
        try {
            val applicationContext = context.applicationContext
            cameraProvider = withContext(Dispatchers.IO) {
                ProcessCameraProvider.getInstance(applicationContext).await()
            }
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Unbind all use cases before rebinding to avoid errors.
            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            onImageCaptureReady(imageCapture)
        } catch (e: Exception) {
            Log.e("CaptureSequenceCameraState", "Camera setup failed", e)
        }
    }

    fun releaseCamera() {
        cameraProvider?.unbindAll()
    }
}

// A composable function to remember and manage the CameraState.
@Composable
private fun rememberCaptureSequenceCameraState(
    lifecycleOwner: LifecycleOwner,
    onImageCaptureReady: (ImageCapture) -> Unit
): CaptureSequenceCameraState {
    val context = LocalContext.current

    // Remember the state instance across recompositions.
    val state = remember(lifecycleOwner, context) {
        CaptureSequenceCameraState(context, lifecycleOwner, onImageCaptureReady)
    }

    // Launch the camera setup coroutine.
    // It will re-launch if the state object changes.
    LaunchedEffect(state) {
        state.setupCamera()
    }

    // Clean up resources when the composable leaves the composition.
    DisposableEffect(state) {
        onDispose {
            state.releaseCamera()
        }
    }

    return state
}

@Composable
private fun CaptureSequenceCameraPreview(
    modifier: Modifier = Modifier,
    onImageCaptureReady: (ImageCapture) -> Unit,
    lifecycleOwner: LifecycleOwner
) {
    // Use the state holder to manage camera logic.
    val cameraState = rememberCaptureSequenceCameraState(lifecycleOwner, onImageCaptureReady)

    // Display the camera preview using AndroidView.
    AndroidView(
        factory = { cameraState.previewView },
        modifier = modifier
    )
}

/**
 * Capture image with metadata (width, height, and ByteArray)
 */
fun captureImageWithMetadata(
    imageCapture: ImageCapture,
    context: Context,
    onImageCaptured: (CapturedImageData) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val executor: Executor = ContextCompat.getMainExecutor(context)

    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val width = image.width
                    val height = image.height
                    
                    // Convert ImageProxy to ByteArray (RGBA format)
                    val buffer: ByteBuffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    
                    // Create captured image data
                    val capturedData = CapturedImageData(
                        imageBytes = bytes,
                        width = width,
                        height = height
                    )
                    
                    onImageCaptured(capturedData)
                } catch (e: Exception) {
                    Log.e("CaptureSequenceScreen", "Error processing image", e)
                    // Create a generic ImageCaptureException for processing errors
                    // Note: ImageCaptureException constructor requires an error code (int) and message
                    // We'll use error code 0 as a generic error (check ImageCaptureException constants for actual codes)
                    val errorCode = 0 // Generic error code
                    onError(ImageCaptureException(errorCode, e.message ?: "Unknown error", e))
                } finally {
                    // Close the image
                    image.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}

