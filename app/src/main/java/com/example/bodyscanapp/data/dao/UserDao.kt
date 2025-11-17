package com.example.bodyscanapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.bodyscanapp.data.entity.User
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for User entity
 * 
 * Provides methods to interact with the users table in Room database
 * All methods use coroutines for asynchronous operations
 */
@Dao
interface UserDao {
    /**
     * Get user by Firebase UID
     * This is the primary way to link Room users to Firebase users
     */
    @Query("SELECT * FROM users WHERE firebaseUid = :firebaseUid")
    suspend fun getUserByFirebaseUid(firebaseUid: String): User?
    
    /**
     * Get user by Room ID
     */
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: Long): User?
    
    /**
     * Insert a new user
     * Returns the inserted user ID (row ID)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long
    
    /**
     * Update an existing user
     */
    @Update
    suspend fun updateUser(user: User)
    
    /**
     * Delete a user
     */
    @Delete
    suspend fun deleteUser(user: User)
    
    /**
     * Get all users (for admin/debugging purposes)
     */
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>
}

