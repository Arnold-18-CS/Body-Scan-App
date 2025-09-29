package com.example.bodyscanapp.utils

object ValidationUtils {
    
    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return false
        
        val emailRegex = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$".toRegex()
        return emailRegex.matches(email)
    }
    
    fun isValidUsername(username: String): Boolean {
        return username.isNotBlank() && username.length >= 3
    }
    
    fun isValidPassword(password: String): Boolean {
        return password.isNotBlank() && password.length >= 6
    }
    
    fun validateLoginInput(emailOrUsername: String, password: String): ValidationResult {
        return when {
            emailOrUsername.isBlank() -> ValidationResult.Error("Please enter your email or username")
            password.isBlank() -> ValidationResult.Error("Please enter your password")
            !isValidPassword(password) -> ValidationResult.Error("Password must be at least 6 characters")
            emailOrUsername.contains("@") && !isValidEmail(emailOrUsername) -> ValidationResult.Error("Please enter a valid email address")
            !emailOrUsername.contains("@") && !isValidUsername(emailOrUsername) -> ValidationResult.Error("Username must be at least 3 characters")
            else -> ValidationResult.Success
        }
    }
    
    fun validateRegistrationInput(username: String, email: String, password: String, confirmPassword: String): ValidationResult {
        return when {
            username.isBlank() -> ValidationResult.Error("Please enter a username")
            email.isBlank() -> ValidationResult.Error("Please enter an email address")
            password.isBlank() -> ValidationResult.Error("Please enter a password")
            confirmPassword.isBlank() -> ValidationResult.Error("Please confirm your password")
            !isValidUsername(username) -> ValidationResult.Error("Username must be at least 3 characters")
            !isValidEmail(email) -> ValidationResult.Error("Please enter a valid email address")
            !isValidPassword(password) -> ValidationResult.Error("Password must be at least 6 characters")
            password != confirmPassword -> ValidationResult.Error("Passwords do not match")
            else -> ValidationResult.Success
        }
    }
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
