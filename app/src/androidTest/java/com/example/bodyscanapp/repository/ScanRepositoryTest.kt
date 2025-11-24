package com.example.bodyscanapp.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.bodyscanapp.data.AppDatabase
import com.example.bodyscanapp.data.dao.ScanDao
import com.example.bodyscanapp.data.entity.Scan
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import java.io.File

/**
 * Unit tests for ScanRepository
 * 
 * Tests:
 * - Save scan result
 * - Get scans by user
 * - Delete scan
 * - File storage for mesh
 * - JSON conversion
 */
@RunWith(AndroidJUnit4::class)
class ScanRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var scanDao: ScanDao
    private lateinit var repository: ScanRepository
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        scanDao = database.scanDao()
        repository = ScanRepository(scanDao)
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun `test saveScanResult`() = runBlocking {
        val userId = 1L
        val heightCm = 175f
        val keypoints3d = FloatArray(135 * 3) { 0f }
        val meshGlb = ByteArray(100) { 0 }
        val measurements = floatArrayOf(80f, 100f, 90f, 60f, 30f, 40f)
        
        val scanId = repository.saveScanResult(
            userId = userId,
            heightCm = heightCm,
            keypoints3d = keypoints3d,
            meshGlb = meshGlb,
            measurements = measurements,
            context = context
        )
        
        assertTrue(scanId > 0)
        
        val scan = repository.getScanById(scanId)
        assertNotNull(scan)
        assertEquals(userId, scan?.userId)
        assertEquals(heightCm, scan?.heightCm)
        assertNotNull(scan?.keypoints3d)
        assertNotNull(scan?.meshPath)
        assertNotNull(scan?.measurementsJson)
    }
    
    @Test
    fun `test getScansByUser`() = runBlocking {
        val userId = 1L
        val heightCm = 175f
        val keypoints3d = FloatArray(135 * 3) { 0f }
        val meshGlb = ByteArray(100) { 0 }
        val measurements = floatArrayOf(80f, 100f, 90f)
        
        // Save multiple scans
        val scanId1 = repository.saveScanResult(
            userId, heightCm, keypoints3d, meshGlb, measurements, context
        )
        val scanId2 = repository.saveScanResult(
            userId, heightCm, keypoints3d, meshGlb, measurements, context
        )
        
        val scans = repository.getScansByUser(userId).first()
        assertEquals(2, scans.size)
        assertTrue(scans.any { it.id == scanId1 })
        assertTrue(scans.any { it.id == scanId2 })
    }
    
    @Test
    fun `test deleteScan`() = runBlocking {
        val userId = 1L
        val heightCm = 175f
        val keypoints3d = FloatArray(135 * 3) { 0f }
        val meshGlb = ByteArray(100) { 0 }
        val measurements = floatArrayOf(80f, 100f, 90f)
        
        val scanId = repository.saveScanResult(
            userId, heightCm, keypoints3d, meshGlb, measurements, context
        )
        
        val scan = repository.getScanById(scanId)
        assertNotNull(scan)
        
        repository.deleteScan(scan!!)
        
        val deletedScan = repository.getScanById(scanId)
        assertEquals(null, deletedScan)
    }
    
    @Test
    fun `test convertKeypointsToJson and parseKeypointsFromJson`() {
        val keypoints3d = floatArrayOf(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f)
        
        val json = repository.convertKeypointsToJson(keypoints3d)
        assertNotNull(json)
        assertTrue(json.isNotEmpty())
        
        val parsed = repository.parseKeypointsFromJson(json)
        assertEquals(keypoints3d.size.toLong(), parsed.size.toLong())
        for (i in keypoints3d.indices) {
            assertEquals(keypoints3d[i], parsed[i], 0.01f)
        }
    }
    
    @Test
    fun `test convertMeasurementsToJson and parseMeasurementsFromJson`() {
        val measurements = floatArrayOf(80f, 100f, 90f, 60f, 30f, 40f)
        
        val json = repository.convertMeasurementsToJson(measurements)
        assertNotNull(json)
        assertTrue(json.isNotEmpty())
        
        val parsed = repository.parseMeasurementsFromJson(json)
        assertEquals(6, parsed.size)
        assertEquals(80f, parsed["waist"] ?: 0f, 0.01f)
        assertEquals(100f, parsed["chest"] ?: 0f, 0.01f)
        assertEquals(90f, parsed["hips"] ?: 0f, 0.01f)
    }
    
    @Test
    fun `test mesh file storage`() = runBlocking {
        val userId = 1L
        val heightCm = 175f
        val keypoints3d = FloatArray(135 * 3) { 0f }
        val meshGlb = ByteArray(100) { it.toByte() }
        val measurements = floatArrayOf(80f, 100f, 90f)
        
        val scanId = repository.saveScanResult(
            userId, heightCm, keypoints3d, meshGlb, measurements, context
        )
        
        val scan = repository.getScanById(scanId)
        assertNotNull(scan)
        
        val meshFile = repository.getMeshFile(scan!!, context)
        assertNotNull(meshFile)
        assertTrue(meshFile!!.exists())
        assertEquals(100, meshFile.length())
    }
}

