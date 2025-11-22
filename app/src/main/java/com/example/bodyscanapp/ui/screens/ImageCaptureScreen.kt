package com.example.bodyscanapp.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.net.Uri
import java.io.ByteArrayOutputStream
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.bodyscanapp.data.HeightData
import com.example.bodyscanapp.ui.theme.BodyScanAppTheme
import com.example.bodyscanapp.ui.theme.BodyScanBackground
import com.example.bodyscanapp.utils.PerformanceLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import kotlin.math.roundToInt
import androidx.compose.ui.tooling.preview.Preview as ComposePreview

// Placeholder function for image processing
// TODO: Remove or update after MediaPipe integration
fun processImageData(imageByteArray: ByteArray) {
    Log.d("ImageCaptureScreen", "Processing image data of size: ${imageByteArray.size} bytes")
    // TODO: Implement actual image processing logic with MediaPipe
}

// Encapsulated validation logic outside the composable
private fun validateHeight(input: String): Pair<Boolean, String?> {
    return try {
        when (val height = input.toIntOrNull()) {
            null -> false to "Please enter a valid number"
            !in 100..250 -> false to "Height must be between 100 and 250 cm"
            else -> true to null
        }
    } catch (e: NumberFormatException) {
        false to "Please enter a valid number"
    }
}


/**
 * Enum for capture phases
 */
enum class CapturePhase {
    FRONT,
    LEFT,
    RIGHT
}

@Composable
fun ImageCaptureScreen(
    modifier: Modifier = Modifier,
    heightData: HeightData? = null,
    onBackClick: () -> Unit = {},
    onCaptureComplete: (List<CapturedImageData>) -> Unit = { _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    
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

    // Capture phase state
    var currentPhase by remember { mutableStateOf(CapturePhase.FRONT) }
    var capturedImages by remember { mutableStateOf<List<CapturedImageData>>(emptyList()) }
    var isPickingImage by remember { mutableStateOf(false) }
    
    var feedbackText by remember { 
        mutableStateOf("Position yourself in frame")
    }
    
    // Update feedback text when phase changes
    LaunchedEffect(currentPhase) {
        feedbackText = when (currentPhase) {
            CapturePhase.FRONT -> "Face the camera"
            CapturePhase.LEFT -> "Turn to your left"
            CapturePhase.RIGHT -> "Turn to your right"
        }
    }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    // Camera permission launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Gallery image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isPickingImage = true
            performanceLogger.logAction("button_click", "gallery_picker_${currentPhase.name}")
            performanceLogger.startAction("image_pick_${currentPhase.name}")
            
            // Convert URI to CapturedImageData in a coroutine
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val capturedData = convertUriToCapturedImageData(uri, context)
                    withContext(Dispatchers.Main) {
                        val duration = performanceLogger.endAction(
                            "image_pick_${currentPhase.name}",
                            "size: ${capturedData.imageBytes.size} bytes"
                        )
                        
                        // Add captured image to list
                        val updatedImages = capturedImages + capturedData
                        capturedImages = updatedImages
                        
                        // Move to next phase or complete
                        if (updatedImages.size < 3) {
                            // Move to next phase
                            currentPhase = when (currentPhase) {
                                CapturePhase.FRONT -> CapturePhase.LEFT
                                CapturePhase.LEFT -> CapturePhase.RIGHT
                                CapturePhase.RIGHT -> CapturePhase.FRONT // Should not reach here
                            }
                            feedbackText = when (currentPhase) {
                                CapturePhase.FRONT -> "Face the camera"
                                CapturePhase.LEFT -> "Turn to your left"
                                CapturePhase.RIGHT -> "Turn to your right"
                            }
                        } else {
                            // All 3 images picked, complete
                            feedbackText = "All images selected! Proceeding to preview..."
                            onCaptureComplete(updatedImages)
                        }
                        
                        isPickingImage = false
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        performanceLogger.endAction(
                            "image_pick_${currentPhase.name}",
                            "error: ${e.message}"
                        )
                        Log.e("ImageCaptureScreen", "Error picking image from gallery", e)
                        feedbackText = "Failed to load image: ${e.message}"
                        isPickingImage = false
                    }
                }
            }
        }
    }
    
    // Request permission if not granted
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BodyScanBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = "Image Capture",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        // Phase indicator
        Text(
            text = when (currentPhase) {
                CapturePhase.FRONT -> "Phase 1 of 3: Front Profile"
                CapturePhase.LEFT -> "Phase 2 of 3: Left Side Profile"
                CapturePhase.RIGHT -> "Phase 3 of 3: Right Side Profile"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2196F3),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Progress indicator
        androidx.compose.material3.LinearProgressIndicator(
            progress = { (capturedImages.size + 0.5f) / 3f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            color = Color(0xFF2196F3),
            trackColor = Color(0xFFE0E0E0)
        )

        // Camera Preview with Framing Guide
        if (hasCameraPermission) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Camera Preview
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onImageCaptureReady = { capture ->
                        imageCapture = capture
                    },
                    lifecycleOwner = lifecycleOwner
                )

                // Framing Guide Overlay
                FramingGuideOverlay(
                    modifier = Modifier.fillMaxSize()
                )

                // Feedback Text
                Text(
                    text = feedbackText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
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

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Capture Button
            Button(
                onClick = {
                    if (imageCapture != null && !isPickingImage) {
                        // Log button click and start tracking capture duration
                        performanceLogger.logAction("button_click", "capture_button_${currentPhase.name}")
                        performanceLogger.startAction("image_capture_${currentPhase.name}")
                        
                        captureImage(
                            imageCapture = imageCapture!!,
                            context = context,
                            onImageCaptured = { byteArray, width, height ->
                                // End capture duration tracking
                                val duration = performanceLogger.endAction("image_capture_${currentPhase.name}", "size: ${byteArray.size} bytes")
                                
                                // Add captured image to list
                                val newImage = CapturedImageData(
                                    imageBytes = byteArray,
                                    width = width,
                                    height = height
                                )
                                val updatedImages = capturedImages + newImage
                                capturedImages = updatedImages
                                
                                // Move to next phase or complete
                                if (updatedImages.size < 3) {
                                    // Move to next phase
                                    currentPhase = when (currentPhase) {
                                        CapturePhase.FRONT -> CapturePhase.LEFT
                                        CapturePhase.LEFT -> CapturePhase.RIGHT
                                        CapturePhase.RIGHT -> CapturePhase.FRONT // Should not reach here
                                    }
                                    feedbackText = when (currentPhase) {
                                        CapturePhase.FRONT -> "Face the camera"
                                        CapturePhase.LEFT -> "Turn to your left"
                                        CapturePhase.RIGHT -> "Turn to your right"
                                    }
                                } else {
                                    // All 3 images captured, complete
                                    feedbackText = "All images captured! Proceeding to preview..."
                                    processImageData(byteArray)
                                    onCaptureComplete(updatedImages)
                                }
                            },
                            onError = { exception ->
                                // End capture tracking even on error
                                performanceLogger.endAction("image_capture_${currentPhase.name}", "error: ${exception.message}")
                                
                                feedbackText = "Capture failed: ${exception.message}"
                                Log.e("ImageCaptureScreen", "Capture error", exception)
                            }
                        )
                    }
                },
                enabled = hasCameraPermission && !isPickingImage && capturedImages.size < 3,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3),
                    contentColor = Color.White,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text(
                    text = if (capturedImages.size < 3) "Capture" else "Complete",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Pick from Gallery Button
            Button(
                onClick = {
                    if (!isPickingImage && capturedImages.size < 3) {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                },
                enabled = !isPickingImage && capturedImages.size < 3,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(60                                                                                                                                                                                                                                                                                                                                                                                                                                        .dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = if (isPickingImage) "Picking..." else "Pick from Gallery",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// A state holder for managing camera-related state and logic.
private class CameraState(
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
            Log.e("CameraState", "Camera setup failed", e)
        }
    }

    fun releaseCamera() {
        cameraProvider?.unbindAll()
    }
}

// A composable function to remember and manage the CameraState.
@Composable
private fun rememberCameraState(
    lifecycleOwner: LifecycleOwner,
    onImageCaptureReady: (ImageCapture) -> Unit
): CameraState {
    val context = LocalContext.current

    // Remember the state instance across recompositions.
    val state = remember(lifecycleOwner, context) {
        CameraState(context, lifecycleOwner, onImageCaptureReady)
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
fun CameraPreview(
    modifier: Modifier = Modifier,
    onImageCaptureReady: (ImageCapture) -> Unit,
    lifecycleOwner: LifecycleOwner
) {
    // Use the state holder to manage camera logic.
    val cameraState = rememberCameraState(lifecycleOwner, onImageCaptureReady)

    // Display the camera preview using AndroidView.
    AndroidView(
        factory = { cameraState.previewView },
        modifier = modifier
    )
}

@Composable
fun FramingGuideOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Define framing guide dimensions (body outline)
        val frameWidth = canvasWidth * 0.6f
        val frameHeight = canvasHeight * 0.7f
        val frameLeft = (canvasWidth - frameWidth) / 2
        val frameTop = (canvasHeight - frameHeight) / 2

        // Draw dashed rectangle for body frame
        drawRect(
            color = Color.White,
            topLeft = Offset(frameLeft, frameTop),
            size = Size(frameWidth, frameHeight),
            style = Stroke(
                width = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
            )
        )

        // Draw head circle guide
        val headRadius = frameWidth * 0.15f
        val headCenterX = canvasWidth / 2
        val headCenterY = frameTop + headRadius + 20f

        drawCircle(
            color = Color.White,
            radius = headRadius,
            center = Offset(headCenterX, headCenterY),
            style = Stroke(
                width = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
            )
        )

        // Draw horizontal alignment lines
        val lineCount = 4
        val lineSpacing = frameHeight / lineCount
        for (i in 1 until lineCount) {
            val y = frameTop + (lineSpacing * i)
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(frameLeft, y),
                end = Offset(frameLeft + frameWidth, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        }
    }
}

fun captureImage(
    imageCapture: ImageCapture,
    context: Context,
    onImageCaptured: (ByteArray, Int, Int) -> Unit,
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
                    val format = image.format
                    
                    // Validate dimensions
                    if (width <= 0 || height <= 0) {
                        throw IllegalArgumentException("Invalid image dimensions: ${width}x${height}")
                    }
                    
                    // Convert ImageProxy to RGBA ByteArray (native code expects RGBA)
                    val imageBytes: ByteArray = when (format) {
                        ImageFormat.YUV_420_888 -> {
                            // Convert YUV to RGBA
                            convertYuvToRgba(image, width, height)
                        }
                        ImageFormat.JPEG -> {
                            // JPEG format - decode to bitmap then convert to RGBA
                            val buffer = image.planes[0].buffer
                            val jpegBytes = ByteArray(buffer.remaining())
                            buffer.get(jpegBytes)
                            convertJpegToRgba(jpegBytes, width, height)
                        }
                        else -> {
                            // Try to handle as YUV (most common case)
                            Log.w("ImageCaptureScreen", "Unknown image format: $format, attempting YUV conversion")
                            convertYuvToRgba(image, width, height)
                        }
                    }
                    
                    Log.d("ImageCaptureScreen", "Image captured: ${width}x${height}, format: $format, size: ${imageBytes.size} bytes")
                    onImageCaptured(imageBytes, width, height)
                } catch (e: Exception) {
                    Log.e("ImageCaptureScreen", "Error processing image", e)
                    val errorCode = ImageCapture.ERROR_UNKNOWN
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

/**
 * Convert YUV_420_888 ImageProxy to RGBA ByteArray
 */
private fun convertYuvToRgba(image: ImageProxy, width: Int, height: Int): ByteArray {
    val bitmap = try {
        // Try to use ImageProxy.toBitmap() extension (available in CameraX 1.1+)
        image.toBitmap()
    } catch (e: NoSuchMethodError) {
        // Fallback: Manual YUV conversion
        Log.w("ImageCaptureScreen", "ImageProxy.toBitmap() not available, using manual conversion", e)
        convertYuvToBitmap(image, width, height)
    } catch (e: Exception) {
        Log.e("ImageCaptureScreen", "Error converting ImageProxy to Bitmap", e)
        convertYuvToBitmap(image, width, height)
    }
    
    // Ensure bitmap matches expected dimensions
    val finalBitmap = if (bitmap.width != width || bitmap.height != height) {
        Bitmap.createScaledBitmap(bitmap, width, height, true)
    } else {
        bitmap
    }
    
    // Convert bitmap to RGBA ByteArray
    return bitmapToRgba(finalBitmap, width, height)
}

/**
 * Fallback method: Convert YUV_420_888 ImageProxy to Bitmap manually
 */
private fun convertYuvToBitmap(image: ImageProxy, width: Int, height: Int): Bitmap {
    val yBuffer = image.planes[0].buffer
    val uBuffer = image.planes[1].buffer
    val vBuffer = image.planes[2].buffer
    
    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()
    
    val yBytes = ByteArray(ySize)
    val uBytes = ByteArray(uSize)
    val vBytes = ByteArray(vSize)
    
    yBuffer.get(yBytes)
    uBuffer.get(uBytes)
    vBuffer.get(vBytes)
    
    // Convert YUV_420_888 to NV21 format (required by YuvImage)
    val nv21Bytes = convertYuv420888ToNv21(yBytes, uBytes, vBytes, width, height)
    
    // Create YuvImage and convert to JPEG
    val yuvImage = YuvImage(
        nv21Bytes,
        ImageFormat.NV21,
        width,
        height,
        null
    )
    
    val jpegStream = ByteArrayOutputStream()
    yuvImage.compressToJpeg(
        Rect(0, 0, width, height),
        100, // Quality
        jpegStream
    )
    val jpegBytes = jpegStream.toByteArray()
    
    // Decode JPEG to Bitmap
    return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
        ?: throw IllegalStateException("Failed to decode YUV image to Bitmap")
}

/**
 * Convert YUV_420_888 format to NV21 format
 */
private fun convertYuv420888ToNv21(
    yBytes: ByteArray,
    uBytes: ByteArray,
    vBytes: ByteArray,
    width: Int,
    height: Int
): ByteArray {
    val ySize = width * height
    val uvSize = ySize / 4
    val nv21 = ByteArray(ySize + uvSize * 2)
    
    // Copy Y plane
    System.arraycopy(yBytes, 0, nv21, 0, ySize)
    
    // Interleave U and V (NV21: VU interleaved)
    var uvIndex = ySize
    for (i in 0 until uvSize) {
        nv21[uvIndex++] = vBytes[i]
        nv21[uvIndex++] = uBytes[i]
    }
    
    return nv21
}

/**
 * Convert JPEG bytes to RGBA ByteArray
 */
private fun convertJpegToRgba(jpegBytes: ByteArray, width: Int, height: Int): ByteArray {
    val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
        ?: throw IllegalStateException("Failed to decode JPEG image")
    
    // Ensure bitmap matches expected dimensions
    val finalBitmap = if (bitmap.width != width || bitmap.height != height) {
        Bitmap.createScaledBitmap(bitmap, width, height, true)
    } else {
        bitmap
    }
    
    // Convert bitmap to RGBA
    val rgbaBytes = bitmapToRgba(finalBitmap, width, height)
    
    // Clean up scaled bitmap if created
    if (finalBitmap != bitmap) {
        finalBitmap.recycle()
    }
    
    return rgbaBytes
}

/**
 * Convert Bitmap to RGBA ByteArray
 * Optimized to reduce memory pressure by processing pixels directly without intermediate IntArray
 * 
 * @param bitmap The bitmap to convert
 * @param width Expected width (should match bitmap width)
 * @param height Expected height (should match bitmap height)
 * @return RGBA byte array (4 bytes per pixel: R, G, B, A)
 */
private fun bitmapToRgba(bitmap: Bitmap, width: Int, height: Int): ByteArray {
    // Verify dimensions match for safety
    val actualWidth = bitmap.width
    val actualHeight = bitmap.height
    if (actualWidth != width || actualHeight != height) {
        Log.w("ImageCaptureScreen", "Bitmap dimensions (${actualWidth}x${actualHeight}) " +
                "don't match expected (${width}x${height}). This may cause issues.")
    }
    
    // Calculate required memory
    val requiredBytes = width.toLong() * height * 4
    if (requiredBytes > Int.MAX_VALUE) {
        throw IllegalArgumentException("Image too large: ${width}x${height} exceeds maximum size")
    }
    
    // Check available memory before allocation
    val runtime = Runtime.getRuntime()
    val maxMemory = runtime.maxMemory()
    val totalMemory = runtime.totalMemory()
    val freeMemory = runtime.freeMemory()
    val availableMemory = maxMemory - (totalMemory - freeMemory)
    
    // Require at least 2x the allocation size to be available (for safety margin)
    val requiredMemory = requiredBytes * 2
    if (availableMemory < requiredMemory) {
        Log.w("ImageCaptureScreen", "Low memory: available=${availableMemory / 1024 / 1024}MB, " +
                "required=${requiredMemory / 1024 / 1024}MB. Attempting GC...")
        System.gc()
        Thread.sleep(100) // Give GC time to run
        
        // Recheck after GC
        val newFreeMemory = runtime.freeMemory()
        val newAvailableMemory = maxMemory - (totalMemory - newFreeMemory)
        if (newAvailableMemory < requiredMemory) {
            throw OutOfMemoryError("Insufficient memory to process image: " +
                    "available=${newAvailableMemory / 1024 / 1024}MB, " +
                    "required=${requiredMemory / 1024 / 1024}MB")
        }
    }
    
    val rgbaBytes = ByteArray(width * height * 4) // RGBA = 4 bytes per pixel
    
    // Process bitmap row by row to reduce peak memory usage
    // Instead of loading all pixels into an IntArray, process them directly
    val rowPixels = IntArray(width)
    var rgbaOffset = 0
    
    for (y in 0 until height) {
        bitmap.getPixels(rowPixels, 0, width, 0, y, width, 1)
        
        for (pixel in rowPixels) {
            // Extract RGBA components (ARGB format in Int: 0xAARRGGBB)
            rgbaBytes[rgbaOffset++] = ((pixel shr 16) and 0xFF).toByte() // R
            rgbaBytes[rgbaOffset++] = ((pixel shr 8) and 0xFF).toByte()  // G
            rgbaBytes[rgbaOffset++] = (pixel and 0xFF).toByte()          // B
            rgbaBytes[rgbaOffset++] = ((pixel shr 24) and 0xFF).toByte() // A
        }
    }
    
    return rgbaBytes
}

/**
 * Calculate the appropriate sample size to downscale an image to fit within max dimensions
 * while maintaining aspect ratio and preventing OOM errors.
 * 
 * @param options BitmapFactory.Options with outWidth and outHeight set
 * @param maxWidth Maximum width for the downscaled image
 * @param maxHeight Maximum height for the downscaled image
 * @return Sample size (power of 2) to use for decoding
 */
private fun calculateInSampleSize(options: BitmapFactory.Options, maxWidth: Int, maxHeight: Int): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1
    
    if (height > maxHeight || width > maxWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        
        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while ((halfHeight / inSampleSize) >= maxHeight && (halfWidth / inSampleSize) >= maxWidth) {
            inSampleSize *= 2
        }
    }
    
    return inSampleSize
}

/**
 * Convert URI from gallery picker to CapturedImageData
 * 
 * Reads the image from the URI, downscales it if necessary to prevent OOM errors,
 * decodes it to Bitmap, and converts to RGBA ByteArray format that matches the camera capture format.
 * 
 * @param uri The URI of the picked image
 * @param context The context to access content resolver
 * @return CapturedImageData with image bytes, width, and height
 */
private fun convertUriToCapturedImageData(uri: Uri, context: Context): CapturedImageData {
    val inputStream = context.contentResolver.openInputStream(uri)
        ?: throw IllegalStateException("Failed to open input stream for URI: $uri")
    
    try {
        // Decode the image to get dimensions first (without loading full image)
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, options)
        
        val originalWidth = options.outWidth
        val originalHeight = options.outHeight
        
        if (originalWidth <= 0 || originalHeight <= 0) {
            throw IllegalArgumentException("Invalid image dimensions: ${originalWidth}x${originalHeight}")
        }
        
        // Reduced maximum dimensions to prevent OOM (1536x1536 = ~9MB RGBA, safer for low-memory devices)
        // This is still sufficient for pose detection while being more memory-friendly
        val maxWidth = 1536
        val maxHeight = 1536
        
        // Calculate sample size for downscaling
        val sampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
        
        // Calculate final dimensions after downscaling
        var finalWidth = originalWidth / sampleSize
        var finalHeight = originalHeight / sampleSize
        
        // Ensure dimensions don't exceed max (sample size calculation might not be perfect)
        if (finalWidth > maxWidth) finalWidth = maxWidth
        if (finalHeight > maxHeight) finalHeight = maxHeight
        
        Log.d("ImageCaptureScreen", "Original image: ${originalWidth}x${originalHeight}, " +
                "downscaling by $sampleSize to ${finalWidth}x${finalHeight}")
        
        // Close and reopen stream to decode the downscaled image
        inputStream.close()
        val fullInputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Failed to reopen input stream for URI: $uri")
        
        try {
            // Decode the image with downscaling
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
                // Add memory optimization flags
                inPurgeable = false // Don't allow purging (we need the bitmap)
                inInputShareable = false // Don't share input (safer)
            }
            
            var bitmap: Bitmap? = null
            try {
                bitmap = BitmapFactory.decodeStream(fullInputStream, null, decodeOptions)
                    ?: throw IllegalStateException("Failed to decode image from URI: $uri")
                
                // Ensure bitmap dimensions match expected (should match, but verify)
                val actualWidth = bitmap.width
                val actualHeight = bitmap.height
                
                // If dimensions don't match (shouldn't happen, but handle edge cases),
                // scale the bitmap to match expected dimensions
                val finalBitmap = if (actualWidth != finalWidth || actualHeight != finalHeight) {
                    Log.w("ImageCaptureScreen", "Bitmap dimensions mismatch: expected ${finalWidth}x${finalHeight}, " +
                            "got ${actualWidth}x${actualHeight}. Scaling...")
                    val scaled = Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
                    // Recycle original if we created a scaled version
                    if (scaled != bitmap) {
                        bitmap.recycle()
                    }
                    scaled
                } else {
                    bitmap
                }
                
                // Convert bitmap to RGBA ByteArray
                val imageBytes = try {
                    bitmapToRgba(finalBitmap, finalWidth, finalHeight)
                } catch (e: OutOfMemoryError) {
                    Log.e("ImageCaptureScreen", "OOM during RGBA conversion, attempting further downscale", e)
                    // If still OOM, try even smaller dimensions
                    val fallbackWidth = (finalWidth * 0.75).toInt()
                    val fallbackHeight = (finalHeight * 0.75).toInt()
                    Log.d("ImageCaptureScreen", "Retrying with reduced dimensions: ${fallbackWidth}x${fallbackHeight}")
                    
                    val fallbackBitmap = Bitmap.createScaledBitmap(finalBitmap, fallbackWidth, fallbackHeight, true)
                    try {
                        if (finalBitmap != bitmap) {
                            finalBitmap.recycle()
                        }
                        bitmapToRgba(fallbackBitmap, fallbackWidth, fallbackHeight).also {
                            // Update final dimensions
                            finalWidth = fallbackWidth
                            finalHeight = fallbackHeight
                        }
                    } finally {
                        if (fallbackBitmap != finalBitmap && fallbackBitmap != bitmap) {
                            fallbackBitmap.recycle()
                        }
                    }
                }
                
                Log.d("ImageCaptureScreen", "Image picked from gallery: ${finalWidth}x${finalHeight}, " +
                        "size: ${imageBytes.size} bytes (${imageBytes.size / 1024 / 1024}MB)")
                
                // Clean up bitmaps
                if (finalBitmap != bitmap) {
                    finalBitmap.recycle()
                }
                
                return CapturedImageData(
                    imageBytes = imageBytes,
                    width = finalWidth,
                    height = finalHeight
                )
            } finally {
                // Recycle bitmap to free memory (if not already recycled)
                bitmap?.let {
                    if (!it.isRecycled) {
                        it.recycle()
                    }
                }
            }
        } finally {
            fullInputStream.close()
        }
    } finally {
        inputStream.close()
    }
}

// Apply the alias to the Compose Preview annotation
@ComposePreview(showBackground = true)
@Composable
fun ImageCaptureScreenPreview() {
    BodyScanAppTheme {
        ImageCaptureScreen()
    }
}
