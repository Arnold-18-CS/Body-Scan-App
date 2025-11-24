package com.example.bodyscanapp.repository

import com.example.bodyscanapp.data.dao.UserDao
import com.example.bodyscanapp.data.entity.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing user data
 * 
 * Abstracts data access and provides business logic for:
 * - Linking Firebase users to Room users
 * - Creating users if they don't exist
 * - Updating user information
 */
class UserRepository(private val userDao: UserDao) {
    /**
     * Get user by Firebase UID
     */
    suspend fun getUserByFirebaseUid(firebaseUid: String): User? = 
        userDao.getUserByFirebaseUid(firebaseUid)
    
    /**
     * Get user by Room ID
     */
    suspend fun getUserById(userId: Long): User? = userDao.getUserById(userId)
    
    /**
     * Get or create user from Firebase UID
     * 
     * This is the main method to ensure a Room user exists for a Firebase user.
     * If user doesn't exist, creates a new one.
     * 
     * @param firebaseUid Firebase user UID
     * @param username Username (from Firebase display name or custom)
     * @param email Email address (optional)
     * @return User entity (existing or newly created)
     */
    suspend fun getOrCreateUser(firebaseUid: String, username: String, email: String?): User {
        val existing = getUserByFirebaseUid(firebaseUid)
        return if (existing != null) {
            existing
        } else {
            val newUser = User(
                firebaseUid = firebaseUid,
                username = username,
                email = email,
                createdAt = System.currentTimeMillis()
            )
            val id = userDao.insertUser(newUser)
            newUser.copy(id = id)
        }
    }
    
    /**
     * Update user information
     */
    suspend fun updateUser(user: User) = userDao.updateUser(user)
    
    /**
     * Delete user
     */
    suspend fun deleteUser(user: User) = userDao.deleteUser(user)
    
    /**
     * Get all users (for admin/debugging)
     */
    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()
}

