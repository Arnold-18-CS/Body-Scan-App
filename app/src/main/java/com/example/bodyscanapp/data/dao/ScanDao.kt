package com.example.bodyscanapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.bodyscanapp.data.entity.Scan
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Scan entity
 * 
 * Provides methods to interact with the scans table in Room database
 * All methods use coroutines for asynchronous operations
 */
@Dao
interface ScanDao {
    /**
     * Get all scans for a specific user, ordered by timestamp (most recent first)
     * Returns Flow for reactive updates
     */
    @Query("SELECT * FROM scans WHERE userId = :userId ORDER BY timestamp DESC")
    fun getScansByUser(userId: Long): Flow<List<Scan>>
    
    /**
     * Insert a new scan
     * Returns the inserted scan with populated ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: Scan): Long
    
    /**
     * Delete a scan
     */
    @Delete
    suspend fun deleteScan(scan: Scan)
    
    /**
     * Get a single scan by ID
     */
    @Query("SELECT * FROM scans WHERE id = :scanId")
    suspend fun getScanById(scanId: Long): Scan?
    
    /**
     * Get N most recent scans for a user
     */
    @Query("SELECT * FROM scans WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentScans(userId: Long, limit: Int): Flow<List<Scan>>
    
    /**
     * Get scans within a date range for a user
     */
    @Query("SELECT * FROM scans WHERE userId = :userId AND timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getScansByDateRange(userId: Long, startTime: Long, endTime: Long): Flow<List<Scan>>
    
    /**
     * Get count of scans for a user
     */
    @Query("SELECT COUNT(*) FROM scans WHERE userId = :userId")
    fun getScanCount(userId: Long): Flow<Int>
}

