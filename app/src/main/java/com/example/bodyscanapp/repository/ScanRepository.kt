package com.example.bodyscanapp.repository

import android.content.Context
import com.example.bodyscanapp.data.dao.ScanDao
import com.example.bodyscanapp.data.entity.Scan
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Repository for managing scan data
 * 
 * Abstracts data access and provides business logic for:
 * - Saving scan results to database
 * - Managing mesh file storage
 * - Converting data formats (FloatArray to JSON, etc.)
 */
class ScanRepository(private val scanDao: ScanDao) {
    private val gson = Gson()
    
    /**
     * Get all scans for a user
     */
    fun getScansByUser(userId: Long): Flow<List<Scan>> = scanDao.getScansByUser(userId)
    
    /**
     * Insert a scan
     */
    suspend fun insertScan(scan: Scan): Long = scanDao.insertScan(scan)
    
    /**
     * Delete a scan
     */
    suspend fun deleteScan(scan: Scan) = scanDao.deleteScan(scan)
    
    /**
     * Get a scan by ID
     */
    suspend fun getScanById(scanId: Long): Scan? = scanDao.getScanById(scanId)
    
    /**
     * Get recent scans for a user
     */
    fun getRecentScans(userId: Long, limit: Int): Flow<List<Scan>> = 
        scanDao.getRecentScans(userId, limit)
    
    /**
     * Get scans by date range
     */
    fun getScansByDateRange(userId: Long, startTime: Long, endTime: Long): Flow<List<Scan>> = 
        scanDao.getScansByDateRange(userId, startTime, endTime)
    
    /**
     * Get scan count for a user
     */
    fun getScanCount(userId: Long): Flow<Int> = scanDao.getScanCount(userId)
    
    /**
     * Save scan result to database and storage
     * 
     * This is the main method to save a complete scan result:
     * - Converts keypoints3d FloatArray to JSON
     * - Saves mesh GLB to internal storage
     * - Converts measurements FloatArray to JSON
     * - Creates and inserts Scan entity
     * 
     * @param userId User ID from Room User entity
     * @param heightCm User height in centimeters
     * @param keypoints3d 3D keypoints array (135*3)
     * @param meshGlb GLB mesh binary data
     * @param measurements Measurement array (waist, chest, hips, etc.)
     * @param context Application context for file operations
     * @return Inserted scan ID
     */
    suspend fun saveScanResult(
        userId: Long,
        heightCm: Float,
        keypoints3d: FloatArray,
        meshGlb: ByteArray,
        measurements: FloatArray,
        context: Context
    ): Long {
        // Convert keypoints3d to JSON
        val keypointsJson = convertKeypointsToJson(keypoints3d)
        
        // Save mesh GLB to internal storage
        val meshPath = saveMeshToFile(meshGlb, context)
        
        // Convert measurements to JSON
        val measurementsJson = convertMeasurementsToJson(measurements)
        
        // Create Scan entity
        val scan = Scan(
            userId = userId,
            timestamp = System.currentTimeMillis(),
            heightCm = heightCm,
            keypoints3d = keypointsJson,
            meshPath = meshPath,
            measurementsJson = measurementsJson
        )
        
        return insertScan(scan)
    }
    
    /**
     * Save mesh GLB file to internal storage
     * 
     * @param meshGlb GLB binary data
     * @param context Application context
     * @return Absolute path to saved file
     */
    private fun saveMeshToFile(meshGlb: ByteArray, context: Context): String {
        val fileName = "mesh_${System.currentTimeMillis()}.glb"
        val meshesDir = File(context.filesDir, "meshes")
        meshesDir.mkdirs()
        val file = File(meshesDir, fileName)
        file.writeBytes(meshGlb)
        return file.absolutePath
    }
    
    /**
     * Convert keypoints FloatArray to JSON string
     */
    fun convertKeypointsToJson(keypoints3d: FloatArray): String {
        return gson.toJson(keypoints3d.toList())
    }
    
    /**
     * Parse keypoints from JSON string
     */
    fun parseKeypointsFromJson(json: String): FloatArray {
        val type = object : com.google.gson.reflect.TypeToken<List<Double>>() {}.type
        val list: List<Double> = gson.fromJson(json, type) ?: emptyList()
        return list.map { it.toFloat() }.toFloatArray()
    }
    
    /**
     * Convert measurements FloatArray to JSON string
     * Uses standard measurement labels
     * 
     * Measurement array indices (7 total):
     * [0] Chest
     * [1] Waist
     * [2] Hips
     * [3] Thigh Left
     * [4] Thigh Right
     * [5] Arm Left
     * [6] Arm Right
     */
    fun convertMeasurementsToJson(measurements: FloatArray): String {
        val measurementsMap = mutableMapOf<String, Float>()
        
        // Map individual measurements
        if (measurements.size > 0) measurementsMap["chest"] = measurements[0]
        if (measurements.size > 1) measurementsMap["waist"] = measurements[1]
        if (measurements.size > 2) measurementsMap["hips"] = measurements[2]
        
        // Calculate average for thighs (left and right)
        if (measurements.size > 3 && measurements.size > 4) {
            val leftThigh = measurements[3]
            val rightThigh = measurements[4]
            if (leftThigh > 0 && rightThigh > 0) {
                measurementsMap["thighs"] = (leftThigh + rightThigh) / 2.0f
            } else if (leftThigh > 0) {
                measurementsMap["thighs"] = leftThigh
            } else if (rightThigh > 0) {
                measurementsMap["thighs"] = rightThigh
            }
        } else if (measurements.size > 3) {
            if (measurements[3] > 0) measurementsMap["thighs"] = measurements[3]
        } else if (measurements.size > 4) {
            if (measurements[4] > 0) measurementsMap["thighs"] = measurements[4]
        }
        
        // Calculate average for arms (left and right)
        if (measurements.size > 5 && measurements.size > 6) {
            val leftArm = measurements[5]
            val rightArm = measurements[6]
            if (leftArm > 0 && rightArm > 0) {
                measurementsMap["arms"] = (leftArm + rightArm) / 2.0f
            } else if (leftArm > 0) {
                measurementsMap["arms"] = leftArm
            } else if (rightArm > 0) {
                measurementsMap["arms"] = rightArm
            }
        } else if (measurements.size > 5) {
            if (measurements[5] > 0) measurementsMap["arms"] = measurements[5]
        } else if (measurements.size > 6) {
            if (measurements[6] > 0) measurementsMap["arms"] = measurements[6]
        }
        
        // Store individual left/right measurements for detailed tracking
        if (measurements.size > 3) measurementsMap["thigh_left"] = measurements[3]
        if (measurements.size > 4) measurementsMap["thigh_right"] = measurements[4]
        if (measurements.size > 5) measurementsMap["arm_left"] = measurements[5]
        if (measurements.size > 6) measurementsMap["arm_right"] = measurements[6]
        
        return gson.toJson(measurementsMap)
    }
    
    /**
     * Parse measurements from JSON string
     */
    fun parseMeasurementsFromJson(json: String): Map<String, Float> {
        val type = object : com.google.gson.reflect.TypeToken<Map<String, Float>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }
    
    /**
     * Get mesh file for a scan
     */
    fun getMeshFile(scan: Scan, context: Context): File? {
        val file = File(scan.meshPath)
        return if (file.exists()) file else null
    }
    
    /**
     * Delete mesh file for a scan
     */
    fun deleteMeshFile(scan: Scan, context: Context): Boolean {
        val file = File(scan.meshPath)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }
    
    /**
     * Cleanup old mesh files
     * 
     * @param context Application context
     * @param maxAge Maximum age in milliseconds (files older than this will be deleted)
     */
    fun cleanupOldMeshes(context: Context, maxAge: Long) {
        val meshesDir = File(context.filesDir, "meshes")
        if (meshesDir.exists() && meshesDir.isDirectory) {
            val currentTime = System.currentTimeMillis()
            meshesDir.listFiles()?.forEach { file ->
                if (file.lastModified() < currentTime - maxAge) {
                    file.delete()
                }
            }
        }
    }
}

