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
        // Check if user exists
        val storedPassword = mockUsers[emailOrUsername]
        return when {
            emailOrUsername.isBlank() -> {
                AuthResult.Error("Please enter your username or email")
            }
            password.isBlank() -> {
                AuthResult.Error("Please enter your password")
            }
            storedPassword == null -> {
                AuthResult.Error("User not found. Please check your username or email")
            }
            storedPassword != password -> {
                AuthResult.Error("Incorrect password. Please try again")
            }
            else -> {
                // Save login state
                prefs.edit {
                    putString(KEY_USERNAME, emailOrUsername)
                    putString(KEY_EMAIL, if (emailOrUsername.contains("@")) emailOrUsername else "")
                    putBoolean(KEY_IS_LOGGED_IN, true)
                }
                AuthResult.Success
            }
        }
    }
    
    fun register(username: String, email: String, password: String): AuthResult {
        return when {
            username.isBlank() -> {
                AuthResult.Error("Please enter a username")
            }
            email.isBlank() -> {
                AuthResult.Error("Please enter an email address")
            }
            password.isBlank() -> {
                AuthResult.Error("Please enter a password")
            }
            !email.contains("@") -> {
                AuthResult.Error("Please enter a valid email address")
            }
            password.length < 6 -> {
                AuthResult.Error("Password must be at least 6 characters long")
            }
            mockUsers.containsKey(username) -> {
                AuthResult.Error("Username already exists. Please choose a different one")
            }
            mockUsers.containsKey(email) -> {
                AuthResult.Error("Email already registered. Please use a different email")
            }
            else -> {
                // In a real app, this would save to a database
                // For mock purposes, we'll just save to SharedPreferences
                prefs.edit {
                    putString(KEY_USERNAME, username)
                    putString(KEY_EMAIL, email)
                    putString(KEY_PASSWORD, password)
                    putBoolean(KEY_IS_LOGGED_IN, true)
                }
                AuthResult.Success
            }
        }
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
