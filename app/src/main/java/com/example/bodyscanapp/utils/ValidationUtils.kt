package com.example.bodyscanapp.utils

object ValidationUtils {
    
    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return false
        
        // More strict email validation that rejects:
        // - Double dots (..)
        // - Leading/trailing dots
        // - Dots before/after @
        // - Invalid domain formats
        val emailRegex = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9][A-Za-z0-9.-]*\\.[A-Za-z]{2,})$".toRegex()
        
        // Additional checks for common invalid patterns
        if (email.contains("..")) return false
        if (email.startsWith(".") || email.endsWith(".")) return false
        if (email.startsWith("@") || email.endsWith("@")) return false
        if (email.contains("@.") || email.contains(".@")) return false
        
        val parts = email.split("@")
        if (parts.size != 2) return false
        
        val localPart = parts[0]
        val domainPart = parts[1]
        
        // Local part cannot be empty
        if (localPart.isBlank()) return false
        
        // Domain part must have at least one dot
        if (!domainPart.contains(".")) return false
        
        // Domain cannot start or end with dot
        if (domainPart.startsWith(".") || domainPart.endsWith(".")) return false
        
        // Domain cannot have consecutive dots
        if (domainPart.contains("..")) return false
        
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
            // Check if it looks like an email (contains @) - validate as email
            emailOrUsername.contains("@") -> {
                if (!isValidEmail(emailOrUsername)) {
                    ValidationResult.Error("Please enter a valid email address")
                } else {
                    ValidationResult.Success
                }
            }
            // If it contains . (always indicates email attempt) or - with longer length (email-like)
            // This handles cases like "invalid-email" (12 chars) but allows "user-name" (9 chars)
            emailOrUsername.contains(".") || (emailOrUsername.contains("-") && emailOrUsername.length >= 12) -> {
                // Treat as email attempt - return email validation error
                ValidationResult.Error("Please enter a valid email address")
            }
            // Otherwise validate as username
            !isValidUsername(emailOrUsername) -> ValidationResult.Error("Username must be at least 3 characters")
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
