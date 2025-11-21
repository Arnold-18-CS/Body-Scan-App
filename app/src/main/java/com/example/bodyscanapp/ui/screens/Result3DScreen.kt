package com.example.bodyscanapp.ui.screens

import android.content.Context
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
import androidx.compose.material3.AlertDialog
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import android.net.Uri
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bodyscanapp.BodyScanApplication
import com.example.bodyscanapp.data.AuthManager
import com.example.bodyscanapp.data.DatabaseModule
import com.example.bodyscanapp.repository.ScanRepository
import com.example.bodyscanapp.repository.UserRepository
import com.example.bodyscanapp.ui.components.ExportDialog
import com.example.bodyscanapp.ui.components.ExportFormat
import com.example.bodyscanapp.ui.components.FilamentMeshViewer
import com.example.bodyscanapp.ui.components.KeypointOverlay
import com.example.bodyscanapp.ui.components.SaveLocation
import com.example.bodyscanapp.ui.components.SaveLocationDialog
import com.example.bodyscanapp.ui.theme.BodyScanBackground
import com.example.bodyscanapp.utils.ShareHelper
import com.example.bodyscanapp.ui.viewmodel.Result3DViewModel
import com.example.bodyscanapp.utils.NativeBridge
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    userHeightCm: Float = 0f,
    onBackClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    onExportClick: () -> Unit = {},
    onNewScanClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onShowSuccessMessage: (String) -> Unit = {},
    onShowErrorMessage: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val application = context.applicationContext as BodyScanApplication
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize repositories
    val database = remember { DatabaseModule.getDatabase(context) }
    val scanRepository = remember { ScanRepository(database.scanDao()) }
    val userRepository = remember { UserRepository(database.userDao()) }
    val authManager = remember { AuthManager(context) }
    val performanceLogger = remember { com.example.bodyscanapp.utils.PerformanceLogger.getInstance(context) }
    
    // Create ViewModel
    val viewModel = remember {
        Result3DViewModel(scanRepository, userRepository, authManager, context)
    }
    
    // Observe ViewModel state
    val saveState by viewModel.saveState.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    
    // Export dialog state
    var showExportDialog by remember { mutableStateOf(false) }
    
    // Save location dialog state
    var showSaveLocationDialog by remember { mutableStateOf(false) }
    
    // Export location dialog state
    var showExportLocationDialog by remember { mutableStateOf(false) }
    var selectedExportFormat by remember { mutableStateOf<ExportFormat?>(null) }
    
    // Share dialog state
    var showShareDialog by remember { mutableStateOf(false) }
    
    // File picker launcher for custom location (save)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("model/gltf-binary")
    ) { uri: Uri? ->
        uri?.let {
            // User selected a custom location
            // TODO: Re-enable save functionality after MediaPipe integration
            // coroutineScope.launch {
            //     performanceLogger.startAction("save_scan")
            //     val result = viewModel.saveScan(
            //         scanResult = scanResult!!,
            //         heightCm = userHeightCm,
            //         saveLocation = SaveLocation.CUSTOM,
            //         customUri = it
            //     )
            //     if (result.isSuccess) {
            //         performanceLogger.endAction("save_scan", "status: success")
            //     } else {
            //         performanceLogger.endAction("save_scan", "status: error")
            //     }
            // }
            onShowSuccessMessage("Save functionality will be available after MediaPipe integration")
        }
    }
    
    // File picker launcher for export custom location
    // Note: MIME type will be set dynamically based on export format
    val exportFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri?.let {
            selectedExportFormat?.let { format ->
                coroutineScope.launch {
                    performanceLogger.startAction("export_scan")
                    
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val fileName = when (format) {
                        ExportFormat.JSON -> "scan_$timestamp.json"
                        ExportFormat.CSV -> "scan_$timestamp.csv"
                        ExportFormat.PDF -> "scan_$timestamp.pdf"
                    }
                    
                    // TODO: Re-enable export functionality after MediaPipe integration
                    // when (format) {
                    //     ExportFormat.JSON -> {
                    //         val tempFile = File(context.cacheDir, fileName)
                    //         viewModel.exportToJson(scanResult!!, userHeightCm, tempFile)
                    //         // Copy to custom location
                    //         tempFile.inputStream().use { input ->
                    //             context.contentResolver.openOutputStream(it)?.use { output ->
                    //                 input.copyTo(output)
                    //             }
                    //         }
                    //         tempFile.delete()
                    //     }
                    //     ExportFormat.CSV -> {
                    //         val measurementLabels = listOf("waist", "chest", "hips", "thighs", "arms", "neck")
                    //         val measurementsMap = scanResult!!.measurements.mapIndexed { index, value ->
                    //             val label = if (index < measurementLabels.size) measurementLabels[index] else "measurement_$index"
                    //             label to value
                    //         }.toMap()
                    //         
                    //         val tempFile = File(context.cacheDir, fileName)
                    //         viewModel.exportToCsv(measurementsMap, tempFile)
                    //         // Copy to custom location
                    //         tempFile.inputStream().use { input ->
                    //             context.contentResolver.openOutputStream(it)?.use { output ->
                    //                 input.copyTo(output)
                    //             }
                    //         }
                    //         tempFile.delete()
                    //     }
                    //     ExportFormat.PDF -> {
                    //         val tempFile = File(context.cacheDir, fileName)
                    //         viewModel.exportToPdf(scanResult!!, userHeightCm, capturedImages, tempFile)
                    //         // Copy to custom location
                    //         tempFile.inputStream().use { input ->
                    //             context.contentResolver.openOutputStream(it)?.use { output ->
                    //                 input.copyTo(output)
                    //             }
                    //         }
                    //         tempFile.delete()
                    //     }
                    // }
                    onShowSuccessMessage("Export functionality will be available after MediaPipe integration")
                    
                    performanceLogger.endAction("export_scan", "format: $format, location: custom")
                }
            }
        }
    }
    
    // Handle save state changes
    LaunchedEffect(saveState) {
        when (saveState) {
            is com.example.bodyscanapp.ui.viewmodel.SaveState.Success -> {
                onShowSuccessMessage("Scan saved successfully!")
                viewModel.resetSaveState()
            }
            is com.example.bodyscanapp.ui.viewmodel.SaveState.Error -> {
                onShowErrorMessage("Failed to save scan: ${(saveState as com.example.bodyscanapp.ui.viewmodel.SaveState.Error).message}")
                viewModel.resetSaveState()
            }
            else -> {}
        }
    }
    
    // Handle export state changes
    LaunchedEffect(exportState) {
        when (exportState) {
            is com.example.bodyscanapp.ui.viewmodel.ExportState.Success -> {
                val file = (exportState as com.example.bodyscanapp.ui.viewmodel.ExportState.Success).file
                onShowSuccessMessage("Exported to: ${file.name}")
                viewModel.resetExportState()
            }
            is com.example.bodyscanapp.ui.viewmodel.ExportState.Error -> {
                onShowErrorMessage("Export failed: ${(exportState as com.example.bodyscanapp.ui.viewmodel.ExportState.Error).message}")
                viewModel.resetExportState()
            }
            else -> {}
        }
    }
    
    // Handle save button click
    fun handleSaveClick() {
        if (scanResult == null) {
            onShowErrorMessage("No scan result to save")
            return
        }
        // Show save location dialog
        showSaveLocationDialog = true
    }
    
    // Handle save location selection
    fun handleSaveLocation(location: SaveLocation) {
        showSaveLocationDialog = false
        
        if (scanResult == null) {
            onShowErrorMessage("No scan result to save")
            return
        }
        
        // Inference code removed - save functionality disabled
        // coroutineScope.launch {
        //     performanceLogger.startAction("save_scan")
        //     
        //     val result = when (location) {
        //         SaveLocation.DEFAULT -> {
        //             viewModel.saveScan(
        //                 scanResult = scanResult,
        //                 heightCm = userHeightCm,
        //                 saveLocation = SaveLocation.DEFAULT
        //             )
        //         }
        //         SaveLocation.DOWNLOADS -> {
        //             viewModel.saveScan(
        //                 scanResult = scanResult,
        //                 heightCm = userHeightCm,
        //                 saveLocation = SaveLocation.DOWNLOADS
        //             )
        //         }
        //         SaveLocation.CUSTOM -> {
        //             // Launch file picker
        //             val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        //             filePickerLauncher.launch("bodyscan_mesh_$timestamp.glb")
        //             return@launch // File picker will handle the save
        //         }
        //     }
        //     
        //     if (result.isSuccess) {
        //         performanceLogger.endAction("save_scan", "status: success")
        //     } else {
        //         performanceLogger.endAction("save_scan", "status: error")
        //     }
        // }
        onShowSuccessMessage("Save functionality disabled - inference code removed")
    }
    
    // Handle export button click
    fun handleExportClick() {
        if (scanResult == null) {
            onShowErrorMessage("No scan result to export")
            return
        }
        showExportDialog = true
    }
    
    
    // Handle export format selection
    fun handleExportFormat(format: ExportFormat) {
        showExportDialog = false
        selectedExportFormat = format
        
        if (scanResult == null) {
            onShowErrorMessage("No scan result to export")
            return
        }
        
        // Show save location dialog for export
        showExportLocationDialog = true
    }
    
    // Handle export location selection
    fun handleExportLocation(location: SaveLocation) {
        showExportLocationDialog = false
        
        val format = selectedExportFormat ?: return
        if (scanResult == null) {
            onShowErrorMessage("No scan result to export")
            return
        }
        
        coroutineScope.launch {
            performanceLogger.startAction("export_scan")
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            
            when (location) {
                SaveLocation.DEFAULT -> {
                    val outputDir = File(context.getExternalFilesDir(null), "exports")
                    outputDir.mkdirs()
                    
                    when (format) {
                        ExportFormat.JSON -> {
                            // TODO: Re-enable export functionality after MediaPipe integration
                            // val outputFile = File(outputDir, "scan_$timestamp.json")
                            // viewModel.exportToJson(scanResult, userHeightCm, outputFile)
                            onShowSuccessMessage("Export functionality will be available after MediaPipe integration")
                        }
                        ExportFormat.CSV -> {
                            // TODO: Re-enable export functionality after MediaPipe integration
                            // val measurementLabels = listOf("waist", "chest", "hips", "thighs", "arms", "neck")
                            // val measurementsMap = scanResult.measurements.mapIndexed { index, value ->
                            //     val label = if (index < measurementLabels.size) measurementLabels[index] else "measurement_$index"
                            //     label to value
                            // }.toMap()
                            // val outputFile = File(outputDir, "scan_$timestamp.csv")
                            // viewModel.exportToCsv(measurementsMap, outputFile)
                            onShowSuccessMessage("Export functionality will be available after MediaPipe integration")
                        }
                        ExportFormat.PDF -> {
                            // TODO: Re-enable export functionality after MediaPipe integration
                            // val outputFile = File(outputDir, "scan_$timestamp.pdf")
                            // viewModel.exportToPdf(scanResult, userHeightCm, capturedImages, outputFile)
                            onShowSuccessMessage("Export functionality will be available after MediaPipe integration")
                        }
                    }
                }
                SaveLocation.DOWNLOADS -> {
                    val fileName = when (format) {
                        ExportFormat.JSON -> "bodyscan_scan_$timestamp.json"
                        ExportFormat.CSV -> "bodyscan_scan_$timestamp.csv"
                        ExportFormat.PDF -> "bodyscan_scan_$timestamp.pdf"
                    }
                    
                    // Create temp file first, then save to Downloads
                    val tempFile = File(context.cacheDir, fileName)
                    when (format) {
                        ExportFormat.JSON -> {
                            // TODO: Re-enable export functionality after MediaPipe integration
                            // viewModel.exportToJson(scanResult, userHeightCm, tempFile)
                            onShowSuccessMessage("Export functionality will be available after MediaPipe integration")
                        }
                        ExportFormat.CSV -> {
                            // TODO: Re-enable export functionality after MediaPipe integration
                            // val measurementLabels = listOf("waist", "chest", "hips", "thighs", "arms", "neck")
                            // val measurementsMap = scanResult.measurements.mapIndexed { index, value ->
                            //     val label = if (index < measurementLabels.size) measurementLabels[index] else "measurement_$index"
                            //     label to value
                            // }.toMap()
                            // viewModel.exportToCsv(measurementsMap, tempFile)
                            onShowSuccessMessage("Export functionality will be available after MediaPipe integration")
                        }
                        ExportFormat.PDF -> {
                            // TODO: Re-enable export functionality after MediaPipe integration
                            // viewModel.exportToPdf(scanResult, userHeightCm, capturedImages, tempFile)
                            onShowSuccessMessage("Export functionality will be available after MediaPipe integration")
                        }
                    }
                    
                    // Save to Downloads
                    val mimeType = com.example.bodyscanapp.utils.FileSaveHelper.getMimeType(fileName)
                    val uri = com.example.bodyscanapp.utils.FileSaveHelper.saveToDownloads(
                        context = context,
                        fileName = fileName,
                        data = tempFile.readBytes(),
                        mimeType = mimeType
                    )
                    
                    tempFile.delete()
                    
                    if (uri == null) {
                        onShowErrorMessage("Failed to save to Downloads folder")
                    }
                }
                SaveLocation.CUSTOM -> {
                    // Launch file picker
                    val fileName = when (format) {
                        ExportFormat.JSON -> "scan_$timestamp.json"
                        ExportFormat.CSV -> "scan_$timestamp.csv"
                        ExportFormat.PDF -> "scan_$timestamp.pdf"
                    }
                    // Use existing launcher (MIME type is set to application/octet-stream as fallback)
                    // The file extension will help the system identify the correct type
                    exportFilePickerLauncher.launch(fileName)
                    return@launch
                }
            }
            
            performanceLogger.endAction("export_scan", "format: $format, location: $location")
        }
    }
    
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
            // Top Section: Captured photos with keypoints (expandable/zoomable)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp) // Increased height for better visibility
            ) {
                // Display captured image with keypoint overlay
                if (capturedImages.isNotEmpty()) {
                    val imageBytes = capturedImages[0]
                    val width = if (imageWidths.isNotEmpty()) imageWidths[0] else 0
                    val height = if (imageHeights.isNotEmpty()) imageHeights[0] else 0
                    
                    // Extract 2D keypoints from scanResult if available
                    val keypoints2d = if (scanResult != null && scanResult.keypoints2d != null) {
                        // Convert FloatArray to List<Pair<Float, Float>>
                        val kpts2d = scanResult.keypoints2d!!
                        (0 until 135).map { i ->
                            val x = kpts2d[i * 2 + 0]
                            val y = kpts2d[i * 2 + 1]
                            Pair(x, y)
                        }
                    } else {
                        emptyList()
                    }
                    
                    CapturedPhotoCardWithKeypoints(
                        imageBytes = imageBytes,
                        label = "Result Image with Keypoints",
                        keypoints2d = keypoints2d,
                        width = width,
                        height = height,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Middle Section: 3D Mesh Viewer - COMMENTED OUT for single image processing
            // Box(
            //     modifier = Modifier
            //         .fillMaxWidth()
            //         .height(400.dp)
            //         .padding(horizontal = 16.dp)
            // ) {
            //     if (scanResult != null && scanResult.meshGlb.isNotEmpty()) {
            //         FilamentMeshViewer(
            //             glbBytes = scanResult.meshGlb,
            //             modifier = Modifier.fillMaxSize(),
            //             onError = { error ->
            //                 // Show error message
            //                 android.util.Log.e("Result3DScreen", "Mesh loading error: $error")
            //             }
            //         )
            //     } else {
            //         // Placeholder if mesh is not available
            //         Box(
            //             modifier = Modifier
            //                 .fillMaxSize()
            //                 .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp)),
            //             contentAlignment = Alignment.Center
            //         ) {
            //             Text(
            //                 text = "3D Mesh not available",
            //                 color = Color.White,
            //                 textAlign = TextAlign.Center
            //             )
            //         }
            //     }
            // }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Instructions - COMMENTED OUT for single image processing
            // Text(
            //     text = "Rotate to view your 3D model",
            //     style = MaterialTheme.typography.bodyMedium,
            //     textAlign = TextAlign.Center,
            //     modifier = Modifier.fillMaxWidth(),
            //     color = Color.Black
            // )
            
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
                    onClick = { handleSaveClick() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    ),
                    enabled = scanResult != null && saveState !is com.example.bodyscanapp.ui.viewmodel.SaveState.Saving
                ) {
                    if (saveState is com.example.bodyscanapp.ui.viewmodel.SaveState.Saving) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save")
                    }
                }
                
                OutlinedButton(
                    onClick = { handleExportClick() },
                    modifier = Modifier.weight(1f),
                    enabled = scanResult != null && exportState !is com.example.bodyscanapp.ui.viewmodel.ExportState.Exporting
                ) {
                    if (exportState is com.example.bodyscanapp.ui.viewmodel.ExportState.Exporting) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text("Export")
                    }
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
                    onClick = {
                        if (scanResult != null) {
                            showShareDialog = true
                        } else {
                            onShowErrorMessage("No scan result to share")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = scanResult != null
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // Export dialog
    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onExportSelected = { format -> handleExportFormat(format) }
        )
    }
    
    // Save location dialog
    if (showSaveLocationDialog) {
        SaveLocationDialog(
            onDismiss = { showSaveLocationDialog = false },
            onLocationSelected = { location -> handleSaveLocation(location) }
        )
    }
    
    // Export location dialog
    if (showExportLocationDialog) {
        SaveLocationDialog(
            onDismiss = { 
                showExportLocationDialog = false
                selectedExportFormat = null
            },
            onLocationSelected = { location -> handleExportLocation(location) }
        )
    }
    
    // Share dialog
    if (showShareDialog) {
        ExportDialog(
            onDismiss = { showShareDialog = false },
            onExportSelected = { format ->
                showShareDialog = false
                if (scanResult == null) {
                    onShowErrorMessage("No scan result to share")
                    return@ExportDialog
                }
                
                coroutineScope.launch {
                    try {
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        val fileName = when (format) {
                            ExportFormat.JSON -> "scan_$timestamp.json"
                            ExportFormat.CSV -> "scan_$timestamp.csv"
                            ExportFormat.PDF -> "scan_$timestamp.pdf"
                        }
                        
                        val tempFile = File(context.cacheDir, fileName)
                        
                        when (format) {
                            ExportFormat.JSON -> {
                                // TODO: Re-enable export functionality after MediaPipe integration
                                // val result = viewModel.exportToJson(scanResult, userHeightCm, tempFile)
                                // result.getOrElse { throw it }
                                throw IllegalStateException("Export functionality will be available after MediaPipe integration")
                            }
                            ExportFormat.CSV -> {
                                // TODO: Re-enable export functionality after MediaPipe integration
                                // val measurementLabels = listOf("waist", "chest", "hips", "thighs", "arms", "neck")
                                // val measurementsMap = scanResult.measurements.mapIndexed { index, value ->
                                //     val label = if (index < measurementLabels.size) measurementLabels[index] else "measurement_$index"
                                //     label to value
                                // }.toMap()
                                // val result = viewModel.exportToCsv(measurementsMap, tempFile)
                                // result.getOrElse { throw it }
                                throw IllegalStateException("Export functionality will be available after MediaPipe integration")
                            }
                            ExportFormat.PDF -> {
                                // TODO: Re-enable export functionality after MediaPipe integration
                                // val result = viewModel.exportToPdf(scanResult, userHeightCm, capturedImages, tempFile)
                                // result.getOrElse { throw it }
                                throw IllegalStateException("Export functionality will be available after MediaPipe integration")
                            }
                        }
                        
                        // Share the file
                        val mimeType = com.example.bodyscanapp.utils.FileSaveHelper.getMimeType(fileName)
                        ShareHelper.shareFile(
                            context = context,
                            file = tempFile,
                            mimeType = mimeType,
                            title = "Share scan results"
                        )
                        
                        onShowSuccessMessage("Sharing scan results...")
                    } catch (e: Exception) {
                        onShowErrorMessage("Failed to share: ${e.message}")
                    }
                }
            }
        )
    }
}

/**
 * Card displaying a captured photo with keypoint overlay
 */
@Composable
private fun CapturedPhotoCardWithKeypoints(
    imageBytes: ByteArray,
    label: String,
    keypoints2d: List<Pair<Float, Float>>,
    width: Int,
    height: Int,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(imageBytes, width, height) {
        try {
            // Validate input
            if (imageBytes.isEmpty()) {
                android.util.Log.w("Result3DScreen", "Image bytes are empty for label: $label")
                return@remember null
            }
            
            if (width <= 0 || height <= 0) {
                android.util.Log.w("Result3DScreen", "Invalid dimensions for label $label: ${width}x${height}")
                // Try standard image format (JPEG/PNG) as fallback
                return@remember BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            }
            
            val expectedSize = width * height * 4
            // Images are stored as RGBA bytes (4 bytes per pixel)
            // Try to decode as RGBA if size matches, otherwise try standard format
            if (imageBytes.size == expectedSize) {
                // Convert RGBA bytes to Bitmap
                rgbaBytesToBitmap(imageBytes, width, height)
            } else {
                android.util.Log.d("Result3DScreen", "Image size mismatch for $label: got ${imageBytes.size}, expected $expectedSize. Trying standard format.")
                // Try standard image format (JPEG/PNG) as fallback
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            }
        } catch (e: Exception) {
            android.util.Log.e("Result3DScreen", "Error decoding image for label $label: ${e.message}", e)
            android.util.Log.e("Result3DScreen", "Image size: ${imageBytes.size}, Expected: ${width * height * 4}, Dimensions: ${width}x${height}")
            null
        }
    }
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (bitmap != null) {
                // Display image with keypoint overlay
                KeypointOverlay(
                    imageBitmap = bitmap.asImageBitmap(),
                    keypoints2d = keypoints2d,
                    modifier = Modifier.fillMaxSize(),
                    keypointColor = Color.Red,
                    keypointRadius = 8f,
                    showSkeleton = false // Set to true to show skeleton connections
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
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
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
            android.util.Log.e("Result3DScreen", "RGBA bytes size (${rgbaBytes.size}) is less than expected ($expectedSize)")
            return null
        }
        
        // Create ARGB int array from RGBA bytes
        val pixels = IntArray(width * height)
        var offset = 0
        for (i in pixels.indices) {
            if (offset + 3 >= rgbaBytes.size) {
                android.util.Log.e("Result3DScreen", "Array index out of bounds at pixel $i, offset $offset")
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
        android.util.Log.e("Result3DScreen", "Error converting RGBA to Bitmap: ${e.message}", e)
        null
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

