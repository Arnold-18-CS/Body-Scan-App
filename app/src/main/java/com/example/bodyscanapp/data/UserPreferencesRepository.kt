package com.example.bodyscanapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseUser

/**
 * User Preferences Repository
 * 
 * Repository for managing user preferences and authentication state:
 * - User display names and usernames (by Firebase UID)
 * - First-time user tracking
 * - Last authenticated user information
 * - Pending email link authentication data
 * 
 * Uses Android SharedPreferences for local data storage.
 * All methods include error handling and return appropriate success/failure indicators.
 * 
 * Email link authentication storage includes automatic expiration (24 hours)
 * to ensure security and prevent stale data.
 * 
 * @param context Android context for SharedPreferences access
 */
class UserPreferencesRepository(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_USERNAME_PREFIX = "username_"
        private const val KEY_FIRST_TIME_PREFIX = "first_time_"
        private const val KEY_LAST_AUTH_UID = "last_auth_uid"
        private const val KEY_LAST_AUTH_EMAIL = "last_auth_email"
        private const val KEY_LAST_AUTH_DISPLAY_NAME = "last_auth_display_name"
        // Email link authentication keys
        private const val KEY_PENDING_EMAIL_LINK_AUTH = "pending_email_link_auth"
        private const val KEY_PENDING_EMAIL_TIMESTAMP = "pending_email_timestamp"
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
    
    /**
     * Store email for pending email link authentication
     * Used to verify the email when user clicks the link from their email
     * @param email Email address to store
     * @return true if successful, false otherwise
     */
    fun savePendingEmailForLinkAuth(email: String): Boolean {
        return try {
            if (email.isBlank()) {
                false
            } else {
                prefs.edit {
                    putString(KEY_PENDING_EMAIL_LINK_AUTH, email.trim())
                    putLong(KEY_PENDING_EMAIL_TIMESTAMP, System.currentTimeMillis())
                }
                true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Retrieve stored email for email link authentication
     * @return stored email or null if not found or expired (older than 24 hours)
     */
    fun retrievePendingEmailForLinkAuth(): String? {
        return try {
            val email = prefs.getString(KEY_PENDING_EMAIL_LINK_AUTH, null)
            val timestamp = prefs.getLong(KEY_PENDING_EMAIL_TIMESTAMP, 0L)
            
            // Check if email exists and is not older than 24 hours
            if (email != null && timestamp > 0) {
                val currentTime = System.currentTimeMillis()
                val hoursSinceStored = (currentTime - timestamp) / (1000 * 60 * 60)
                
                if (hoursSinceStored < 24) {
                    email
                } else {
                    // Email link expired, clear it
                    clearPendingEmailForLinkAuth()
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Clear stored email for email link authentication
     * Should be called after successful authentication or when link expires
     * @return true if successful, false otherwise
     */
    fun clearPendingEmailForLinkAuth(): Boolean {
        return try {
            prefs.edit {
                remove(KEY_PENDING_EMAIL_LINK_AUTH)
                remove(KEY_PENDING_EMAIL_TIMESTAMP)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if there is a pending email link authentication
     * @return true if there is a pending email (not expired), false otherwise
     */
    fun hasPendingEmailLinkAuth(): Boolean {
        return retrievePendingEmailForLinkAuth() != null
    }
}


