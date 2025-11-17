package com.example.bodyscanapp.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bodyscanapp.data.AuthManager
import com.example.bodyscanapp.data.DatabaseModule
import com.example.bodyscanapp.data.entity.Scan
import com.example.bodyscanapp.repository.ScanRepository
import com.example.bodyscanapp.repository.UserRepository
import com.example.bodyscanapp.ui.components.ExportDialog
import com.example.bodyscanapp.ui.components.ExportFormat
import com.example.bodyscanapp.ui.theme.BodyScanBackground
import com.example.bodyscanapp.utils.ExportHelper
import com.example.bodyscanapp.utils.FileSaveHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * HistoryScreen - Displays list of all user's scans
 * 
 * Features:
 * - List of scans with thumbnails
 * - Date and time display
 * - Key measurements preview
 * - Height used for scan
 * - Pull-to-refresh
 * - Search/filter functionality
 * - Sorting options
 * - Delete action (swipe or long-press)
 * - Empty state
 * - Loading states
 */
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onScanClick: (Long) -> Unit = {}, // Navigate to Result3DScreen with scan ID
    onShowSuccessMessage: (String) -> Unit = {},
    onShowErrorMessage: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize repositories
    val database = remember { DatabaseModule.getDatabase(context) }
    val scanRepository = remember { ScanRepository(database.scanDao()) }
    val userRepository = remember { UserRepository(database.userDao()) }
    val authManager = remember { AuthManager(context) }
    
    // State
    var scans by remember { mutableStateOf<List<Scan>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(SortOrder.NEWEST_FIRST) }
    var showDeleteDialog by remember { mutableStateOf<Scan?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var userId by remember { mutableStateOf<Long?>(null) }
    
    // Get current user
    LaunchedEffect(Unit) {
        val firebaseUser = authManager.getCurrentUser()
        if (firebaseUser != null) {
            val user = userRepository.getOrCreateUser(
                firebaseUid = firebaseUser.uid,
                username = firebaseUser.displayName ?: "User",
                email = firebaseUser.email
            )
            userId = user.id
        }
    }
    
    // Load scans
    LaunchedEffect(userId) {
        userId?.let { uid ->
            scanRepository.getScansByUser(uid).collect { scanList ->
                scans = scanList
                isLoading = false
            }
        }
    }
    
    // Filter and sort scans
    val filteredAndSortedScans = remember(scans, searchQuery, sortOrder) {
        var filtered = scans
        
        // Apply search filter (by date)
        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter { scan ->
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(scan.timestamp))
                dateStr.contains(searchQuery, ignoreCase = true)
            }
        }
        
        // Apply sorting
        filtered = when (sortOrder) {
            SortOrder.NEWEST_FIRST -> filtered.sortedByDescending { it.timestamp }
            SortOrder.OLDEST_FIRST -> filtered.sortedBy { it.timestamp }
            SortOrder.BY_WAIST -> filtered.sortedByDescending { 
                parseMeasurements(it.measurementsJson)["waist"] ?: 0f 
            }
            SortOrder.BY_CHEST -> filtered.sortedByDescending { 
                parseMeasurements(it.measurementsJson)["chest"] ?: 0f 
            }
            SortOrder.BY_HIPS -> filtered.sortedByDescending { 
                parseMeasurements(it.measurementsJson)["hips"] ?: 0f 
            }
        }
        
        filtered
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BodyScanBackground)
    ) {
        // Top bar
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
                text = "Scan History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
            
            // Export all scans button
            if (scans.isNotEmpty() && !isLoading) {
                IconButton(
                    onClick = { showExportDialog = true },
                    modifier = Modifier.align(Alignment.CenterEnd),
                    enabled = !isExporting
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export all scans",
                            tint = Color.White
                        )
                    }
                }
            }
        }
        
        // Search and sort bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by date (YYYY-MM-DD)") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Sort options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortChip("Newest", sortOrder == SortOrder.NEWEST_FIRST) {
                    sortOrder = SortOrder.NEWEST_FIRST
                }
                SortChip("Oldest", sortOrder == SortOrder.OLDEST_FIRST) {
                    sortOrder = SortOrder.OLDEST_FIRST
                }
                SortChip("Waist", sortOrder == SortOrder.BY_WAIST) {
                    sortOrder = SortOrder.BY_WAIST
                }
                SortChip("Chest", sortOrder == SortOrder.BY_CHEST) {
                    sortOrder = SortOrder.BY_CHEST
                }
                SortChip("Hips", sortOrder == SortOrder.BY_HIPS) {
                    sortOrder = SortOrder.BY_HIPS
                }
            }
        }
        
        // Scan list
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (filteredAndSortedScans.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (scans.isEmpty()) "No scans yet" else "No scans match your search",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    if (scans.isEmpty()) {
                        Text(
                            text = "Start a new scan to see your history here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(
                    items = filteredAndSortedScans,
                    key = { it.id }
                ) { scan ->
                    ScanCard(
                        scan = scan,
                        onClick = { onScanClick(scan.id) },
                        onDeleteClick = { showDeleteDialog = scan },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    
    // Export dialog
    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onExportSelected = { format ->
                showExportDialog = false
                if (scans.isEmpty()) {
                    onShowErrorMessage("No scans to export")
                    return@ExportDialog
                }
                
                coroutineScope.launch {
                    isExporting = true
                    try {
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        val fileName = when (format) {
                            ExportFormat.JSON -> "all_scans_$timestamp.zip"
                            ExportFormat.CSV -> "all_measurements_$timestamp.csv"
                            ExportFormat.PDF -> "all_scans_report_$timestamp.pdf"
                        }
                        
                        val tempFile = File(context.cacheDir, fileName)
                        
                        val exportResult = when (format) {
                            ExportFormat.JSON -> {
                                ExportHelper.exportAllScansAsZip(scans, tempFile)
                            }
                            ExportFormat.CSV -> {
                                ExportHelper.exportAllMeasurementsAsCsv(scans, tempFile)
                            }
                            ExportFormat.PDF -> {
                                ExportHelper.exportAllScansAsPdf(scans, tempFile)
                            }
                        }
                        
                        exportResult.getOrElse {
                            throw it
                        }
                        
                        // Save to Downloads
                        val mimeType = FileSaveHelper.getMimeType(fileName)
                        val uri = FileSaveHelper.saveToDownloads(
                            context = context,
                            fileName = fileName,
                            data = tempFile.readBytes(),
                            mimeType = mimeType
                        )
                        
                        tempFile.delete()
                        
                        if (uri != null) {
                            onShowSuccessMessage("Exported ${scans.size} scans to Downloads: $fileName")
                        } else {
                            onShowErrorMessage("Failed to save export to Downloads")
                        }
                    } catch (e: Exception) {
                        onShowErrorMessage("Export failed: ${e.message}")
                    } finally {
                        isExporting = false
                    }
                }
            }
        )
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Scan") },
            text = { Text("Are you sure you want to delete this scan? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val scanToDelete = showDeleteDialog
                        showDeleteDialog = null
                        if (scanToDelete != null) {
                            coroutineScope.launch {
                                try {
                                    scanRepository.deleteScan(scanToDelete)
                                    // Delete mesh file if it exists
                                    try {
                                        val meshFile = File(scanToDelete.meshPath)
                                        if (meshFile.exists()) {
                                            meshFile.delete()
                                        }
                                    } catch (e: Exception) {
                                        // Ignore file deletion errors
                                    }
                                    onShowSuccessMessage("Scan deleted successfully")
                                } catch (e: Exception) {
                                    onShowErrorMessage("Failed to delete scan: ${e.message}")
                                }
                            }
                        }
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Scan card displaying scan information
 */
@Composable
private fun ScanCard(
    scan: Scan,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val measurements = parseMeasurements(scan.measurementsJson)
    
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Thumbnail placeholder (since images aren't stored in Scan entity)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFFE0E0E0), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📷",
                    fontSize = 32.sp
                )
            }
            
            // Scan info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = dateFormat.format(Date(scan.timestamp)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Height: ${String.format("%.1f", scan.heightCm)} cm",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                
                // Key measurements
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    measurements["waist"]?.let {
                        Text(
                            text = "Waist: ${String.format("%.1f", it)} cm",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    measurements["chest"]?.let {
                        Text(
                            text = "Chest: ${String.format("%.1f", it)} cm",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    measurements["hips"]?.let {
                        Text(
                            text = "Hips: ${String.format("%.1f", it)} cm",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            // Delete button
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete scan",
                    tint = Color.Red
                )
            }
        }
    }
}

/**
 * Sort chip component
 */
@Composable
private fun SortChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        modifier = modifier
    )
}

/**
 * Parse measurements from JSON string
 */
private fun parseMeasurements(json: String): Map<String, Float> {
    return try {
        val gson = Gson()
        val type = object : TypeToken<Map<String, Float>>() {}.type
        gson.fromJson(json, type) ?: emptyMap()
    } catch (e: Exception) {
        emptyMap()
    }
}

/**
 * Sort order enum
 */
private enum class SortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST,
    BY_WAIST,
    BY_CHEST,
    BY_HIPS
}

