package com.example.bodyscanapp.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import kotlin.math.roundToInt
import androidx.compose.ui.tooling.preview.Preview as ComposePreview

// Placeholder function for image processing
fun processImageData(imageByteArray: ByteArray) {
    Log.d("ImageCaptureScreen", "Processing image data of size: ${imageByteArray.size} bytes")
    // TODO: Implement actual image processing logic here
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


@Composable
fun ImageCaptureScreen(
    modifier: Modifier = Modifier,
    heightData: HeightData? = null,
    onBackClick: () -> Unit = {},
    onCaptureComplete: (ByteArray) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Camera state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var feedbackText by remember { mutableStateOf("Align body within the frame") }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

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
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Height Display (if provided)
        heightData?.let { height ->
            Text(
                text = "Height: ${height.getDisplayValue()} (${height.toCentimeters().roundToInt()} cm)",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .background(
                        Color(0xFFC7C7C7),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

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

        // Capture Button
        Button(
            onClick = {
                if (imageCapture != null) {
                    captureImage(
                        imageCapture = imageCapture!!,
                        context = context,
                        onImageCaptured = { byteArray ->
                            feedbackText = "Image captured successfully!"
                            processImageData(byteArray)
                            onCaptureComplete(byteArray)
                        },
                        onError = { exception ->
                            feedbackText = "Capture failed: ${exception.message}"
                            Log.e("ImageCaptureScreen", "Capture error", exception)
                        }
                    )
                }
            },
            enabled = hasCameraPermission,
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
        ) {
            Text(
                text = "Capture Image",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
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
    onImageCaptured: (ByteArray) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val executor: Executor = ContextCompat.getMainExecutor(context)

    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                // Convert ImageProxy to ByteArray
                val buffer: ByteBuffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)

                onImageCaptured(bytes)

                // Close the image
                image.close()
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}

// Apply the alias to the Compose Preview annotation
@ComposePreview(showBackground = true)
@Composable
fun ImageCaptureScreenPreview() {
    BodyScanAppTheme {
        ImageCaptureScreen()
    }
}
