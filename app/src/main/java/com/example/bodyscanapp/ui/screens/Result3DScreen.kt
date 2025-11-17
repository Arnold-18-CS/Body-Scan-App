package com.example.bodyscanapp.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bodyscanapp.ui.components.FilamentMeshViewer
import com.example.bodyscanapp.ui.components.KeypointOverlay
import com.example.bodyscanapp.ui.theme.BodyScanBackground
import com.example.bodyscanapp.utils.NativeBridge

/**
 * Result3DScreen - Displays 3D scan results
 * 
 * Layout:
 * - Top Section (50%): 3 captured photos with keypoint overlays (horizontal scrollable)
 * - Middle Section (40%): FilamentMeshViewer displaying 3D mesh
 * - Bottom Section (10%): List of measurements + action buttons
 * 
 * @param modifier Modifier for the screen
 * @param scanResult The scan result from NativeBridge
 * @param capturedImages List of captured images (ByteArray) - front, left, right
 * @param imageWidths Array of image widths
 * @param imageHeights Array of image heights
 * @param onBackClick Callback when user goes back
 * @param onSaveClick Callback when user saves scan
 * @param onExportClick Callback when user exports results
 * @param onNewScanClick Callback when user starts new scan
 * @param onShareClick Callback when user shares results
 */
@Composable
fun Result3DScreen(
    modifier: Modifier = Modifier,
    scanResult: NativeBridge.ScanResult? = null,
    capturedImages: List<ByteArray> = emptyList(),
    imageWidths: List<Int> = emptyList(),
    imageHeights: List<Int> = emptyList(),
    onBackClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    onExportClick: () -> Unit = {},
    onNewScanClick: () -> Unit = {},
    onShareClick: () -> Unit = {}
) {
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BodyScanBackground)
    ) {
        // Top bar with back button
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
                text = "3D Scan Results",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top Section: Captured photos with keypoints (50% height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Display each captured image with keypoint overlay
                    val imageLabels = listOf("Front", "Left", "Right")
                    capturedImages.forEachIndexed { index, imageBytes ->
                        if (index < imageLabels.size) {
                            CapturedPhotoCard(
                                imageBytes = imageBytes,
                                label = imageLabels[index],
                                keypoints2d = emptyList(), // TODO: Extract 2D keypoints from scanResult if available
                                modifier = Modifier.width(250.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Middle Section: 3D Mesh Viewer (40% height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .padding(horizontal = 16.dp)
            ) {
                if (scanResult != null && scanResult.meshGlb.isNotEmpty()) {
                    FilamentMeshViewer(
                        glbBytes = scanResult.meshGlb,
                        modifier = Modifier.fillMaxSize(),
                        onError = { error ->
                            // Show error message
                            android.util.Log.e("Result3DScreen", "Mesh loading error: $error")
                        }
                    )
                } else {
                    // Placeholder if mesh is not available
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "3D Mesh not available",
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Instructions
            Text(
                text = "Rotate to view your 3D model",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Bottom Section: Measurements (scrollable)
            Text(
                text = "Measurements",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (scanResult != null && scanResult.measurements.isNotEmpty()) {
                MeasurementsList(
                    measurements = scanResult.measurements,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                Text(
                    text = "No measurements available",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSaveClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    )
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save")
                }
                
                OutlinedButton(
                    onClick = onExportClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Export")
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onNewScanClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("New Scan")
                }
                
                OutlinedButton(
                    onClick = onShareClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Card displaying a captured photo with keypoint overlay
 */
@Composable
private fun CapturedPhotoCard(
    imageBytes: ByteArray,
    label: String,
    keypoints2d: List<Pair<Float, Float>>,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(imageBytes) {
        try {
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            android.util.Log.e("Result3DScreen", "Error decoding image", e)
            null
        }
    }
    
    Card(
        modifier = modifier.height(280.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (bitmap != null) {
                KeypointOverlay(
                    imageBitmap = bitmap.asImageBitmap(),
                    keypoints2d = keypoints2d,
                    modifier = Modifier.fillMaxSize(),
                    showSkeleton = false
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E1E1E)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Image not available",
                        color = Color.White
                    )
                }
            }
            
            // Label
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * List of measurements displayed as cards
 */
@Composable
private fun MeasurementsList(
    measurements: FloatArray,
    modifier: Modifier = Modifier
) {
    // Measurement labels (assuming order: waist, chest, hips, thighs, etc.)
    val measurementLabels = listOf(
        "Waist",
        "Chest",
        "Hips",
        "Thighs",
        "Arms",
        "Neck"
    )
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        measurements.forEachIndexed { index, value ->
            if (index < measurementLabels.size) {
                MeasurementCard(
                    label = measurementLabels[index],
                    value = value,
                    unit = "cm"
                )
            }
        }
    }
}

/**
 * Card displaying a single measurement
 */
@Composable
private fun MeasurementCard(
    label: String,
    value: Float,
    unit: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Text(
                text = String.format("%.1f %s", value, unit),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3)
            )
        }
    }
}

