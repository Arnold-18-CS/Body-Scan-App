package com.example.bodyscanapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class AuthRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_PASSWORD = "password"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
    
    // Mock user credentials for testing
    private val mockUsers = mapOf(
        "admin" to "admin123",
        "user@example.com" to "password123",
        "testuser" to "testpass"
    )
    
    fun authenticate(emailOrUsername: String, password: String): AuthResult {
        // Check against mock users
        val storedPassword = mockUsers[emailOrUsername]
        return if (storedPassword == password) {
            // Save login state
            prefs.edit {
                putString(KEY_USERNAME, emailOrUsername)
                putString(KEY_EMAIL, if (emailOrUsername.contains("@")) emailOrUsername else "")
                putBoolean(KEY_IS_LOGGED_IN, true)
            }
            AuthResult.Success
        } else {
            AuthResult.Error("Invalid credentials. Please try again.")
        }
    }
    
    fun register(username: String, email: String, password: String): AuthResult {
        // Check if user already exists
        if (mockUsers.containsKey(username) || mockUsers.containsKey(email)) {
            return AuthResult.Error("User already exists. Please use different credentials.")
        }
        
        // In a real app, this would save to a database
        // For mock purposes, we'll just save to SharedPreferences
        prefs.edit {
            putString(KEY_USERNAME, username)
            putString(KEY_EMAIL, email)
            putString(KEY_PASSWORD, password)
            putBoolean(KEY_IS_LOGGED_IN, true)
        }
        
        return AuthResult.Success
    }
    
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    fun logout() {
        prefs.edit {
            clear()
        }
    }
    
    fun getCurrentUser(): String? {
        return prefs.getString(KEY_USERNAME, null)
    }
}

sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}
