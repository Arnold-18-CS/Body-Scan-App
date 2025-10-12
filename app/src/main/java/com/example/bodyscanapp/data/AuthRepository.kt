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
        return when {
            emailOrUsername.isBlank() -> {
                AuthResult.Error("Please enter your username or email")
            }
            password.isBlank() -> {
                AuthResult.Error("Please enter your password")
            }
            else -> {
                // First check hardcoded mock users for backward compatibility
                val mockPassword = mockUsers[emailOrUsername]
                if (mockPassword != null) {
                    if (mockPassword == password) {
                        // Save login state
                        prefs.edit {
                            putString(KEY_USERNAME, emailOrUsername)
                            putString(KEY_EMAIL, if (emailOrUsername.contains("@")) emailOrUsername else "")
                            putBoolean(KEY_IS_LOGGED_IN, true)
                        }
                        AuthResult.Success("Login successful")
                    } else {
                        AuthResult.Error("Incorrect password. Please try again")
                    }
                } else {
                    // Check SharedPreferences for newly registered users
                    val registeredUser = getRegisteredUser(emailOrUsername)
                    if (registeredUser != null) {
                        if (registeredUser.password == password) {
                            // Save login state
                            prefs.edit {
                                putString(KEY_USERNAME, registeredUser.username)
                                putString(KEY_EMAIL, registeredUser.email)
                                putBoolean(KEY_IS_LOGGED_IN, true)
                            }
                            AuthResult.Success("Login successful")
                        } else {
                            AuthResult.Error("Incorrect password. Please try again")
                        }
                    } else {
                        AuthResult.Error("User not found. Please check your username or email")
                    }
                }
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
                AuthResult.Success("Registration successful")
            }
        }
    }
    
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    fun logout() {
        prefs.edit {
            // Only clear login state, preserve registered user data
            remove(KEY_IS_LOGGED_IN)
            // Keep KEY_USERNAME, KEY_EMAIL, KEY_PASSWORD for re-login capability
        }
    }
    
    fun getCurrentUser(): String? {
        return prefs.getString(KEY_USERNAME, null)
    }
    
    private fun getRegisteredUser(emailOrUsername: String): RegisteredUser? {
        // Check if the input is an email or username
        val isEmail = emailOrUsername.contains("@")
        
        if (isEmail) {
            // Search by email
            val storedEmail = prefs.getString(KEY_EMAIL, "")
            val storedUsername = prefs.getString(KEY_USERNAME, "")
            val storedPassword = prefs.getString(KEY_PASSWORD, "")
            
            if (storedEmail == emailOrUsername && !storedUsername.isNullOrBlank() && !storedPassword.isNullOrBlank()) {
                return RegisteredUser(storedUsername, storedEmail, storedPassword)
            }
        } else {
            // Search by username
            val storedUsername = prefs.getString(KEY_USERNAME, "")
            val storedEmail = prefs.getString(KEY_EMAIL, "")
            val storedPassword = prefs.getString(KEY_PASSWORD, "")
            
            if (storedUsername == emailOrUsername && !storedEmail.isNullOrBlank() && !storedPassword.isNullOrBlank()) {
                return RegisteredUser(storedUsername, storedEmail, storedPassword)
            }
        }
        
        return null
    }
}

data class RegisteredUser(
    val username: String,
    val email: String,
    val password: String
)

