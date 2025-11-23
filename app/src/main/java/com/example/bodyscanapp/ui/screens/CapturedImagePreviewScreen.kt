package com.example.bodyscanapp.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bodyscanapp.ui.components.KeypointOverlay
import com.example.bodyscanapp.ui.theme.BodyScanBackground
import com.example.bodyscanapp.utils.NativeBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Screen to preview all captured images before processing
 * 
 * @param capturedImages List of captured images with metadata
 * @param onProceedClick Callback when user wants to proceed with these images
 * @param onRetakeClick Callback when user wants to retake the photos
 * @param modifier Modifier for the screen
 */
@Composable
fun CapturedImagePreviewScreen(
    capturedImages: List<CapturedImageData>,
    onProceedClick: () -> Unit,
    onRetakeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Decode all images to Bitmaps
    val bitmaps = remember(capturedImages) {
        capturedImages.map { imageData ->
            val bitmap = try {
                if (imageData.imageBytes.isEmpty()) {
                    android.util.Log.w("CapturedImagePreviewScreen", "Image bytes are empty")
                    null
                } else if (imageData.width <= 0 || imageData.height <= 0) {
                    android.util.Log.w("CapturedImagePreviewScreen", "Invalid dimensions: ${imageData.width}x${imageData.height}")
                    // Try standard image format (JPEG/PNG) as fallback
                    BitmapFactory.decodeByteArray(imageData.imageBytes, 0, imageData.imageBytes.size)
                } else {
                    val expectedSize = imageData.width * imageData.height * 4
                    // Images are stored as RGBA bytes (4 bytes per pixel)
                    // Try to decode as RGBA if size matches, otherwise try standard format
                    if (imageData.imageBytes.size == expectedSize) {
                        // Convert RGBA bytes to Bitmap
                        rgbaBytesToBitmap(imageData.imageBytes, imageData.width, imageData.height)
                    } else {
                        android.util.Log.d("CapturedImagePreviewScreen", "Image size mismatch: got ${imageData.imageBytes.size}, expected $expectedSize. Trying standard format.")
                        // Try standard image format (JPEG/PNG) as fallback
                        BitmapFactory.decodeByteArray(imageData.imageBytes, 0, imageData.imageBytes.size)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CapturedImagePreviewScreen", "Error decoding image: ${e.message}", e)
                null
            }
            bitmap
        }
    }
    
    // Validate all images and detect keypoints
    var validationResults by remember(capturedImages) {
        mutableStateOf<List<NativeBridge.ImageValidationResult?>>(
            List(capturedImages.size) { null }
        )
    }
    var keypointsList by remember(capturedImages) {
        mutableStateOf<List<List<Pair<Float, Float>>?>>(
            List(capturedImages.size) { null }
        )
    }
    var isValidating by remember { mutableStateOf(true) }
    
    // Perform validation and keypoint detection when images change
    LaunchedEffect(capturedImages) {
        isValidating = true
        val results = withContext(Dispatchers.IO) {
            capturedImages.mapIndexed { index, imageData ->
                try {
                    NativeBridge.validateImage(
                        image = imageData.imageBytes,
                        width = imageData.width,
                        height = imageData.height
                    )
                } catch (e: Exception) {
                    android.util.Log.e("CapturedImagePreviewScreen", "Validation error for image $index: ${e.message}", e)
                    NativeBridge.ImageValidationResult(
                        hasPerson = false,
                        isFullBody = false,
                        hasMultiplePeople = false,
                        confidence = 0f,
                        message = "Validation failed: ${e.message}"
                    )
                }
            }
        }
        validationResults = results
        
        // Detect keypoints for all images
        val keypoints = withContext(Dispatchers.IO) {
            capturedImages.mapIndexed { index, imageData ->
                try {
                    val keypointsArray = NativeBridge.detectKeypoints(
                        image = imageData.imageBytes,
                        width = imageData.width,
                        height = imageData.height
                    )
                    // Convert FloatArray to List<Pair<Float, Float>>
                    // keypointsArray is 135*2 = 270 floats: [x1, y1, x2, y2, ...]
                    val keypointsList = mutableListOf<Pair<Float, Float>>()
                    for (i in 0 until 135) {
                        val x = keypointsArray[i * 2]
                        val y = keypointsArray[i * 2 + 1]
                        // Add keypoint (KeypointOverlay will filter invalid ones)
                        keypointsList.add(Pair(x, y))
                    }
                    keypointsList
                } catch (e: Exception) {
                    android.util.Log.e("CapturedImagePreviewScreen", "Keypoint detection error for image $index: ${e.message}", e)
                    null
                }
            }
        }
        keypointsList = keypoints
        isValidating = false
    }
    
    val phaseLabels = listOf("Front Profile", "Left Side Profile", "Right Side Profile")
    val allImagesValid = validationResults.all { it?.isValid == true }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BodyScanBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "Preview Captured Images",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )
            
            // Images Preview Grid
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                bitmaps.forEachIndexed { index, bitmap ->
                    val validation = validationResults.getOrNull(index)
                    val keypoints = keypointsList.getOrNull(index)
                    
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Phase label with validation badge
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Phase ${index + 1}: ${phaseLabels.getOrNull(index) ?: "Image ${index + 1}"}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            
                            // Validation Badge
                            if (isValidating) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            Color(0xFF757575),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Validating...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            } else {
                                val badgeColor = when {
                                    validation == null -> Color(0xFF757575) // Gray - unknown
                                    validation.hasMultiplePeople -> Color(0xFFFF5722) // Orange-red - multiple people
                                    validation.isValid -> Color(0xFF4CAF50) // Green - valid
                                    else -> Color(0xFFF44336) // Red - invalid
                                }
                                val badgeText = when {
                                    validation == null -> "Unknown"
                                    validation.hasMultiplePeople -> "✗ Multiple People"
                                    validation.isValid -> "✓ Valid"
                                    else -> "✗ Invalid"
                                }
                                
                                Column(
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                badgeColor,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    // Show validation message if invalid or multiple people detected
                                    if (validation != null && (!validation.isValid || validation.hasMultiplePeople) && validation.message.isNotEmpty()) {
                                        Text(
                                            text = validation.message,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (validation.hasMultiplePeople) Color(0xFFFF5722) else Color(0xFFFF9800),
                                            modifier = Modifier.padding(top = 2.dp),
                                            maxLines = 3
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Image preview with keypoint overlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(3f / 4f) // Portrait aspect ratio
                                .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            if (bitmap != null) {
                                val imageBitmap = bitmap.asImageBitmap()
                                // Use KeypointOverlay if keypoints are available, otherwise show plain image
                                if (keypoints != null && keypoints.any { 
                                    val (x, y) = it
                                    x >= 0f && x <= 1f && y >= 0f && y <= 1f && (x > 0f || y > 0f)
                                }) {
                                    KeypointOverlay(
                                        imageBitmap = imageBitmap,
                                        keypoints2d = keypoints,
                                        modifier = Modifier.fillMaxSize(),
                                        keypointColor = Color(0xFFFF5722), // Orange-red for visibility
                                        keypointRadius = 2f, // Smaller radius for preview
                                        showSkeleton = false // Don't show skeleton in preview
                                    )
                                } else {
                                    Image(
                                        bitmap = imageBitmap,
                                        contentDescription = "Captured image ${index + 1} preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Unable to display image",
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            
                            // Overlay border based on validation status
                            if (!isValidating && validation != null) {
                                val borderColor = when {
                                    validation.hasMultiplePeople -> Color(0xFFFF5722).copy(alpha = 0.5f) // Orange-red for multiple people
                                    validation.isValid -> Color(0xFF4CAF50).copy(alpha = 0.5f) // Green for valid
                                    else -> Color(0xFFF44336).copy(alpha = 0.5f) // Red for invalid
                                }
                                Canvas(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    drawRect(
                                        color = borderColor,
                                        style = Stroke(width = 4f)
                                    )
                                }
                            }
                        }
                        
                        // Final processing info - show what will be sent to native code
                        val imageData = capturedImages.getOrNull(index)
                        if (imageData != null) {
                            // Calculate final dimensions after preprocessing (max 640px width)
                            val maxProcessWidth = 640
                            val scale = if (imageData.width > maxProcessWidth) {
                                maxProcessWidth.toFloat() / imageData.width
                            } else {
                                1.0f
                            }
                            val finalWidth = (imageData.width * scale).toInt()
                            val finalHeight = (imageData.height * scale).toInt()
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .background(
                                        Color(0xFF2A2A2A).copy(alpha = 0.7f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Processing Info",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB0B0B0),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = "Input: ${imageData.width}×${imageData.height} (${String.format("%.1f", imageData.width * imageData.height * 4 / 1024f / 1024f)}MB)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF9E9E9E)
                                )
                                Text(
                                    text = "Final: ${finalWidth}×${finalHeight} (after resize + CLAHE)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (scale < 1.0f) Color(0xFFFF9800) else Color(0xFF4CAF50),
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (scale < 1.0f) {
                                    Text(
                                        text = "Will be downscaled by ${String.format("%.1f", (1.0f / scale))}× for processing",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFF9800),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Proceed Button - disabled if validation is in progress or any image is invalid
                Button(
                    onClick = onProceedClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isValidating && allImagesValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isValidating && allImagesValid) {
                            Color(0xFF2196F3)
                        } else {
                            Color(0xFF757575)
                        },
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF424242),
                        disabledContentColor = Color(0xFF9E9E9E)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = when {
                            isValidating -> "Validating Images..."
                            !allImagesValid -> "Fix Invalid Images First"
                            else -> "Proceed to Processing"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Retake Button
                Button(
                    onClick = onRetakeClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF424242),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Retake All Photos",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Convert RGBA bytes to Bitmap
 * 
 * @param rgbaBytes RGBA byte array (4 bytes per pixel: R, G, B, A)
 * @param width Image width
 * @param height Image height
 * @return Bitmap created from RGBA bytes, or null if conversion fails
 */
private fun rgbaBytesToBitmap(rgbaBytes: ByteArray, width: Int, height: Int): Bitmap? {
    return try {
        val expectedSize = width * height * 4
        if (rgbaBytes.size < expectedSize) {
            android.util.Log.e("CapturedImagePreviewScreen", "RGBA bytes size (${rgbaBytes.size}) is less than expected ($expectedSize)")
            return null
        }
        
        // Create ARGB int array from RGBA bytes
        val pixels = IntArray(width * height)
        var offset = 0
        for (i in pixels.indices) {
            if (offset + 3 >= rgbaBytes.size) {
                android.util.Log.e("CapturedImagePreviewScreen", "Array index out of bounds at pixel $i, offset $offset")
                return null
            }
            val r = rgbaBytes[offset++].toInt() and 0xFF
            val g = rgbaBytes[offset++].toInt() and 0xFF
            val b = rgbaBytes[offset++].toInt() and 0xFF
            val a = rgbaBytes[offset++].toInt() and 0xFF
            // Combine into ARGB format: 0xAARRGGBB
            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        
        // Create bitmap from pixels
        Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    } catch (e: Exception) {
        android.util.Log.e("CapturedImagePreviewScreen", "Error converting RGBA to Bitmap: ${e.message}", e)
        null
    }
}

