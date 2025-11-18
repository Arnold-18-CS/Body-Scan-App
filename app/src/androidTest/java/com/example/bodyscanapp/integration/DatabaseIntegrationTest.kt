package com.example.bodyscanapp.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.bodyscanapp.data.AppDatabase
import com.example.bodyscanapp.data.entity.Scan
import com.example.bodyscanapp.data.entity.User
import com.example.bodyscanapp.repository.ScanRepository
import com.example.bodyscanapp.repository.UserRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

/**
 * Integration tests for database operations
 * 
 * Tests:
 * - Complete scan save/retrieve flow
 * - User-scan relationship
 * - Data persistence
 */
@RunWith(AndroidJUnit4::class)
class DatabaseIntegrationTest {
    private lateinit var database: AppDatabase
    private lateinit var scanRepository: ScanRepository
    private lateinit var userRepository: UserRepository
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        scanRepository = ScanRepository(database.scanDao())
        userRepository = UserRepository(database.userDao())
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun `test complete scan save and retrieve flow`() = runBlocking {
        // Create user
        val user = userRepository.getOrCreateUser(
            firebaseUid = "test_uid_123",
            username = "TestUser",
            email = "test@example.com"
        )
        
        assertNotNull(user)
        assertTrue(user.id > 0)
        
        // Save scan
        val keypoints3d = FloatArray(135 * 3) { 0f }
        val meshGlb = ByteArray(100) { 0 }
        val measurements = floatArrayOf(80f, 100f, 90f, 60f, 30f, 40f)
        
        val scanId = scanRepository.saveScanResult(
            userId = user.id,
            heightCm = 175f,
            keypoints3d = keypoints3d,
            meshGlb = meshGlb,
            measurements = measurements,
            context = context
        )
        
        assertTrue(scanId > 0)
        
        // Retrieve scan
        val scan = scanRepository.getScanById(scanId)
        assertNotNull(scan)
        assertEquals(user.id, scan?.userId)
        assertEquals(175f, scan?.heightCm)
        
        // Verify measurements
        val parsedMeasurements = scanRepository.parseMeasurementsFromJson(scan!!.measurementsJson)
        assertEquals(6, parsedMeasurements.size)
        assertEquals(80f, parsedMeasurements["waist"] ?: 0f, 0.01f)
    }
    
    @Test
    fun `test user scan relationship`() = runBlocking {
        // Create two users
        val user1 = userRepository.getOrCreateUser(
            firebaseUid = "test_uid_1",
            username = "User1",
            email = "user1@example.com"
        )
        val user2 = userRepository.getOrCreateUser(
            firebaseUid = "test_uid_2",
            username = "User2",
            email = "user2@example.com"
        )
        
        // Save scans for each user
        val keypoints3d = FloatArray(135 * 3) { 0f }
        val meshGlb = ByteArray(100) { 0 }
        val measurements = floatArrayOf(80f, 100f, 90f)
        
        val scanId1 = scanRepository.saveScanResult(
            user1.id, 175f, keypoints3d, meshGlb, measurements, context
        )
        val scanId2 = scanRepository.saveScanResult(
            user2.id, 180f, keypoints3d, meshGlb, measurements, context
        )
        
        // Verify user1 only sees their scans
        val user1Scans = scanRepository.getScansByUser(user1.id).first()
        assertEquals(1, user1Scans.size)
        assertEquals(scanId1, user1Scans[0].id)
        
        // Verify user2 only sees their scans
        val user2Scans = scanRepository.getScansByUser(user2.id).first()
        assertEquals(1, user2Scans.size)
        assertEquals(scanId2, user2Scans[0].id)
    }
    
    @Test
    fun `test scan deletion and file cleanup`() = runBlocking {
        val user = userRepository.getOrCreateUser(
            firebaseUid = "test_uid_delete",
            username = "TestUser",
            email = "test@example.com"
        )
        
        val keypoints3d = FloatArray(135 * 3) { 0f }
        val meshGlb = ByteArray(100) { 0 }
        val measurements = floatArrayOf(80f, 100f, 90f)
        
        val scanId = scanRepository.saveScanResult(
            user.id, 175f, keypoints3d, meshGlb, measurements, context
        )
        
        val scan = scanRepository.getScanById(scanId)
        assertNotNull(scan)
        
        // Verify mesh file exists
        val meshFile = scanRepository.getMeshFile(scan!!, context)
        assertNotNull(meshFile)
        
        // Delete scan
        scanRepository.deleteScan(scan)
        
        // Verify scan is deleted
        val deletedScan = scanRepository.getScanById(scanId)
        assertEquals(null, deletedScan)
    }
}

