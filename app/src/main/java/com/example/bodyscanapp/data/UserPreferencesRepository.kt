package com.example.bodyscanapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseUser

/**
 * Repository for managing user preferences and display information
 * Stores user-selected display names and preferences by Firebase UID
 */
class UserPreferencesRepository(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_USERNAME_PREFIX = "username_"
        private const val KEY_FIRST_TIME_PREFIX = "first_time_"
        private const val KEY_LAST_AUTH_UID = "last_auth_uid"
        private const val KEY_LAST_AUTH_EMAIL = "last_auth_email"
        private const val KEY_LAST_AUTH_DISPLAY_NAME = "last_auth_display_name"
    }
    
    /**
     * Store selected username for a Firebase UID
     * @param uid Firebase user UID
     * @param username Selected display username
     * @return true if successful, false otherwise
     */
    fun setUsername(uid: String, username: String): Boolean {
        return try {
            if (uid.isBlank() || username.isBlank()) {
                false
            } else {
                prefs.edit {
                    putString(KEY_USERNAME_PREFIX + uid, username.trim())
                }
                true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get stored username for a Firebase UID
     * @param uid Firebase user UID
     * @return stored username or null if not found
     */
    fun getUsername(uid: String): String? {
        return try {
            if (uid.isBlank()) {
                null
            } else {
                prefs.getString(KEY_USERNAME_PREFIX + uid, null)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get display name for a Firebase user (stored username or Firebase display name)
     * @param user Firebase user
     * @return display name to show in UI
     */
    fun getDisplayName(user: FirebaseUser?): String? {
        return try {
            if (user == null) return null
            
            val storedUsername = getUsername(user.uid)
            if (!storedUsername.isNullOrBlank()) {
                storedUsername
            } else {
                user.displayName ?: user.email
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Check if this is the first time a user is signing in
     * @param uid Firebase user UID
     * @return true if first time, false otherwise
     */
    fun isFirstTimeUser(uid: String): Boolean {
        return try {
            if (uid.isBlank()) {
                true
            } else {
                prefs.getBoolean(KEY_FIRST_TIME_PREFIX + uid, true)
            }
        } catch (e: Exception) {
            true
        }
    }
    
    /**
     * Mark user as no longer first-time
     * @param uid Firebase user UID
     * @return true if successful, false otherwise
     */
    fun markUserAsReturning(uid: String): Boolean {
        return try {
            if (uid.isBlank()) {
                false
            } else {
                prefs.edit {
                    putBoolean(KEY_FIRST_TIME_PREFIX + uid, false)
                }
                true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Store last authenticated user information
     * @param user Firebase user
     * @return true if successful, false otherwise
     */
    fun setLastAuthenticatedUser(user: FirebaseUser?): Boolean {
        return try {
            if (user == null) {
                prefs.edit {
                    remove(KEY_LAST_AUTH_UID)
                    remove(KEY_LAST_AUTH_EMAIL)
                    remove(KEY_LAST_AUTH_DISPLAY_NAME)
                }
                true
            } else {
                prefs.edit {
                    putString(KEY_LAST_AUTH_UID, user.uid)
                    putString(KEY_LAST_AUTH_EMAIL, user.email)
                    putString(KEY_LAST_AUTH_DISPLAY_NAME, user.displayName)
                }
                true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get last authenticated user UID
     * @return UID of last authenticated user or null
     */
    fun getLastAuthenticatedUid(): String? {
        return try {
            prefs.getString(KEY_LAST_AUTH_UID, null)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get last authenticated user email
     * @return email of last authenticated user or null
     */
    fun getLastAuthenticatedEmail(): String? {
        return try {
            prefs.getString(KEY_LAST_AUTH_EMAIL, null)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get last authenticated user display name
     * @return display name of last authenticated user or null
     */
    fun getLastAuthenticatedDisplayName(): String? {
        return try {
            prefs.getString(KEY_LAST_AUTH_DISPLAY_NAME, null)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Clear all user data for a specific UID
     * @param uid Firebase user UID
     * @return true if successful, false otherwise
     */
    fun clearUserData(uid: String): Boolean {
        return try {
            if (uid.isBlank()) {
                false
            } else {
                prefs.edit {
                    remove(KEY_USERNAME_PREFIX + uid)
                    remove(KEY_FIRST_TIME_PREFIX + uid)
                }
                true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Clear all stored data
     * @return true if successful, false otherwise
     */
    fun clearAllData(): Boolean {
        return try {
            prefs.edit {
                clear()
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if user has any stored preferences
     * @param uid Firebase user UID
     * @return true if user has stored data, false otherwise
     */
    fun hasUserData(uid: String): Boolean {
        return try {
            if (uid.isBlank()) {
                false
            } else {
                prefs.contains(KEY_USERNAME_PREFIX + uid) || 
                prefs.contains(KEY_FIRST_TIME_PREFIX + uid)
            }
        } catch (e: Exception) {
            false
        }
    }
}


