package com.example.bodyscanapp.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.bodyscanapp.data.AuthManager
import com.example.bodyscanapp.repository.ScanRepository
import com.example.bodyscanapp.repository.UserRepository
import com.example.bodyscanapp.ui.components.SaveLocation
import com.example.bodyscanapp.utils.ExportHelper
import com.example.bodyscanapp.utils.FileSaveHelper
import com.example.bodyscanapp.utils.NativeBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel for managing result 3D screen state
 * 
 * Manages:
 * - Saving scan results to database
 * - Export operations (JSON, CSV, PDF)
 * - User linking (Firebase UID to Room User)
 */
sealed class SaveState {
    data object Idle : SaveState()
    data object Saving : SaveState()
    data class Success(val scanId: Long) : SaveState()
    data class Error(val message: String) : SaveState()
}

sealed class ExportState {
    data object Idle : ExportState()
    data object Exporting : ExportState()
    data class Success(val file: File) : ExportState()
    data class Error(val message: String) : ExportState()
}

class Result3DViewModel(
    private val scanRepository: ScanRepository,
    private val userRepository: UserRepository,
    private val authManager: AuthManager,
    private val context: Context
) : ViewModel() {
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()
    
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()
    
    /**
     * Save scan result to database
     * Kept for future reference - will be re-enabled after MediaPipe integration
     * 
     * @param scanResult The scan result from NativeBridge
     * @param heightCm User height in centimeters
     * @param saveLocation Where to save the mesh file (default, downloads, or custom URI)
     * @param customUri Optional URI for custom location (required if saveLocation is CUSTOM)
     * @return Result containing scan ID on success
     */
    // suspend fun saveScan(
    //     scanResult: NativeBridge.ScanResult,
    //     heightCm: Float,
    //     saveLocation: SaveLocation = SaveLocation.DEFAULT,
    //     customUri: Uri? = null
    // ): Result<Long> {
    //     return try {
    //         _saveState.value = SaveState.Saving
    //         
    //         // Get current Firebase user
    //         val firebaseUser = authManager.getCurrentUser()
    //         if (firebaseUser == null) {
    //             return Result.failure(IllegalStateException("User not authenticated"))
    //         }
    //         
    //         // Get or create Room user
    //         val user = userRepository.getOrCreateUser(
    //             firebaseUid = firebaseUser.uid,
    //             username = firebaseUser.displayName ?: "User",
    //             email = firebaseUser.email
    //         )
    //         
    //         // Save mesh file based on location preference
    //         val meshPath = when (saveLocation) {
    //             SaveLocation.DEFAULT -> {
    //                 // Save to default location (app's internal storage)
    //                 val fileName = "mesh_${System.currentTimeMillis()}.glb"
    //                 val file = FileSaveHelper.saveToDefaultLocation(
    //                     context = context,
    //                     fileName = fileName,
    //                     data = scanResult.meshGlb,
    //                     subdirectory = "meshes"
    //                 )
    //                 file.absolutePath
    //             }
    //             SaveLocation.DOWNLOADS -> {
    //                 // Save to Downloads folder
    //                 val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    //                 val fileName = "bodyscan_mesh_$timestamp.glb"
    //                 val uri = FileSaveHelper.saveToDownloads(
    //                     context = context,
    //                     fileName = fileName,
    //                     data = scanResult.meshGlb,
    //                     mimeType = FileSaveHelper.getMimeType(fileName)
    //                 )
    //                 uri?.toString() ?: throw IllegalStateException("Failed to save mesh to Downloads")
    //             }
    //             SaveLocation.CUSTOM -> {
    //                 // Save to custom location via URI
    //                 if (customUri == null) {
    //                     throw IllegalArgumentException("Custom URI required for CUSTOM save location")
    //                 }
    //                 val success = FileSaveHelper.saveToCustomLocation(
    //                     context = context,
    //                     uri = customUri,
    //                     data = scanResult.meshGlb
    //                 )
    //                 if (!success) {
    //                     throw IllegalStateException("Failed to save mesh to custom location")
    //                 }
    //                 customUri.toString()
    //             }
    //         }
    //         
    //         // Save scan result to database (mesh path is stored in database)
    //         val scanId = scanRepository.saveScanResult(
    //             userId = user.id,
    //             heightCm = heightCm,
    //             keypoints3d = scanResult.keypoints3d,
    //             meshGlb = scanResult.meshGlb, // Still pass for internal storage backup
    //             measurements = scanResult.measurements,
    //             context = context
    //         )
    //         
    //         // Update mesh path in database if saved to external location
    //         if (saveLocation != SaveLocation.DEFAULT) {
    //             val scan = scanRepository.getScanById(scanId)
    //             scan?.let {
    //                 // Note: This would require updating the Scan entity's meshPath
    //                 // For now, we store the path in the database as-is
    //             }
    //         }
    //         
    //         _saveState.value = SaveState.Success(scanId)
    //         Result.success(scanId)
    //         
    //     } catch (e: Exception) {
    //         android.util.Log.e("Result3DViewModel", "Error saving scan", e)
    //         _saveState.value = SaveState.Error(e.message ?: "Failed to save scan")
    //         Result.failure(e)
    //     }
    // }
    
    /**
     * Export scan to JSON
     * Kept for future reference - will be re-enabled after MediaPipe integration
     * 
     * @param scanResult The scan result
     * @param heightCm User height in centimeters
     * @param outputFile Output file
     * @return Result indicating success or failure
     */
    // suspend fun exportToJson(
    //     scanResult: NativeBridge.ScanResult,
    //     heightCm: Float,
    //     outputFile: File
    // ): Result<Unit> {
    //     return try {
    //         _exportState.value = ExportState.Exporting
    //         
    //         // Get current Firebase user
    //         val firebaseUser = authManager.getCurrentUser()
    //         if (firebaseUser == null) {
    //             return Result.failure(IllegalStateException("User not authenticated"))
    //         }
    //         
    //         // Get or create Room user
    //         val user = userRepository.getOrCreateUser(
    //             firebaseUid = firebaseUser.uid,
    //             username = firebaseUser.displayName ?: "User",
    //             email = firebaseUser.email
    //         )
    //         
    //         // Save scan first to get Scan entity
    //         val scanId = scanRepository.saveScanResult(
    //             userId = user.id,
    //             heightCm = heightCm,
    //             keypoints3d = scanResult.keypoints3d,
    //             meshGlb = scanResult.meshGlb,
    //             measurements = scanResult.measurements,
    //             context = context
    //         )
    //         
    //         // Get scan entity
    //         val scan = scanRepository.getScanById(scanId)
    //         if (scan == null) {
    //             return Result.failure(IllegalStateException("Failed to retrieve saved scan"))
    //         }
    //         
    //         // Export to JSON
    //         ExportHelper.exportToJson(scan, outputFile)
    //         
    //         _exportState.value = ExportState.Success(outputFile)
    //         Result.success(Unit)
    //         
    //     } catch (e: Exception) {
    //         android.util.Log.e("Result3DViewModel", "Error exporting to JSON", e)
    //         _exportState.value = ExportState.Error(e.message ?: "Failed to export to JSON")
    //         Result.failure(e)
    //     }
    // }
    
    /**
     * Export measurements to CSV
     * 
     * @param measurements Map of measurement names to values
     * @param outputFile Output file
     * @return Result indicating success or failure
     */
    suspend fun exportToCsv(
        measurements: Map<String, Float>,
        outputFile: File
    ): Result<Unit> {
        return try {
            _exportState.value = ExportState.Exporting
            
            ExportHelper.exportToCsv(measurements, outputFile)
            
            _exportState.value = ExportState.Success(outputFile)
            Result.success(Unit)
            
        } catch (e: Exception) {
            android.util.Log.e("Result3DViewModel", "Error exporting to CSV", e)
            _exportState.value = ExportState.Error(e.message ?: "Failed to export to CSV")
            Result.failure(e)
        }
    }
    
    /**
     * Export scan to PDF
     * Kept for future reference - will be re-enabled after MediaPipe integration
     * 
     * @param scanResult The scan result
     * @param heightCm User height in centimeters
     * @param capturedImages List of captured images (ByteArray)
     * @param outputFile Output file
     * @return Result indicating success or failure
     */
    // suspend fun exportToPdf(
    //     scanResult: NativeBridge.ScanResult,
    //     heightCm: Float,
    //     capturedImages: List<ByteArray>,
    //     outputFile: File
    // ): Result<Unit> {
    //     return try {
    //         _exportState.value = ExportState.Exporting
    //         
    //         // Get current Firebase user
    //         val firebaseUser = authManager.getCurrentUser()
    //         if (firebaseUser == null) {
    //             return Result.failure(IllegalStateException("User not authenticated"))
    //         }
    //         
    //         // Get or create Room user
    //         val user = userRepository.getOrCreateUser(
    //             firebaseUid = firebaseUser.uid,
    //             username = firebaseUser.displayName ?: "User",
    //             email = firebaseUser.email
    //         )
    //         
    //         // Save scan first to get Scan entity
    //         val scanId = scanRepository.saveScanResult(
    //             userId = user.id,
    //             heightCm = heightCm,
    //             keypoints3d = scanResult.keypoints3d,
    //             meshGlb = scanResult.meshGlb,
    //             measurements = scanResult.measurements,
    //             context = context
    //         )
    //         
    //         // Get scan entity
    //         val scan = scanRepository.getScanById(scanId)
    //         if (scan == null) {
    //             return Result.failure(IllegalStateException("Failed to retrieve saved scan"))
    //         }
    //         
    //         // Convert ByteArrays to Bitmaps
    //         val bitmaps = capturedImages.mapNotNull { imageBytes ->
    //             try {
    //                 BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    //             } catch (e: Exception) {
    //                 android.util.Log.e("Result3DViewModel", "Error decoding image", e)
    //                 null
    //             }
    //         }
    //         
    //         // Export to PDF
    //         ExportHelper.exportToPdf(scan, bitmaps, outputFile)
    //         
    //         _exportState.value = ExportState.Success(outputFile)
    //         Result.success(Unit)
    //         
    //     } catch (e: Exception) {
    //         android.util.Log.e("Result3DViewModel", "Error exporting to PDF", e)
    //         _exportState.value = ExportState.Error(e.message ?: "Failed to export to PDF")
    //         Result.failure(e)
    //     }
    // }
    
    /**
     * Reset save state
     */
    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }
    
    /**
     * Reset export state
     */
    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }
}

