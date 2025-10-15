package com.example.bodyscanapp.data

import com.google.firebase.auth.FirebaseUser

/**
 * Sealed class representing the result of authentication operations
 */
sealed class AuthResult {
    /**
     * Successful authentication result
     * @param message Success message
     * @param user Firebase user (optional, for sign-in operations)
     * @param data Additional data (optional, for sign-in methods, etc.)
     */
    data class Success(
        val message: String,
        val user: FirebaseUser? = null,
        val data: Any? = null
    ) : AuthResult()
    
    /**
     * Error authentication result
     * @param message Error message
     */
    data class Error(
        val message: String
    ) : AuthResult()
    
    /**
     * Loading state for authentication operations
     */
    object Loading : AuthResult()
}

/**
 * Extension function to check if the result is successful
 */
fun AuthResult.isSuccess(): Boolean = this is AuthResult.Success

/**
 * Extension function to check if the result is an error
 */
fun AuthResult.isError(): Boolean = this is AuthResult.Error

/**
 * Extension function to check if the result is loading
 */
fun AuthResult.isLoading(): Boolean = this is AuthResult.Loading

/**
 * Extension function to get the message from the result
 */
fun AuthResult.getMessage(): String? = when (this) {
    is AuthResult.Success -> message
    is AuthResult.Error -> message
    is AuthResult.Loading -> null
}

/**
 * Extension function to get the Firebase user from the result
 */
fun AuthResult.getUser(): FirebaseUser? = when (this) {
    is AuthResult.Success -> user
    is AuthResult.Error -> null
    is AuthResult.Loading -> null
}
