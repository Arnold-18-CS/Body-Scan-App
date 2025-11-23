package com.example.bodyscanapp.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a user
 * 
 * Links Room users to Firebase users via firebaseUid
 * This allows local data persistence while using Firebase for authentication
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["firebaseUid"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val firebaseUid: String,
    val username: String,
    val email: String?,
    val createdAt: Long
)

