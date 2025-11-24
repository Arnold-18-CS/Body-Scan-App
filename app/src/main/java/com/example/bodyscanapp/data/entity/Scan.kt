package com.example.bodyscanapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a body scan result
 * 
 * Stores:
 * - User ID linking to User entity
 * - Timestamp of scan
 * - Height in centimeters
 * - 3D keypoints as JSON string
 * - Path to mesh GLB file in internal storage
 * - Measurements as JSON string
 */
@Entity(tableName = "scans")
data class Scan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val timestamp: Long,
    val heightCm: Float,
    val keypoints3d: String,   // JSON string
    val meshPath: String,      // internal file path
    val measurementsJson: String // JSON string
)

