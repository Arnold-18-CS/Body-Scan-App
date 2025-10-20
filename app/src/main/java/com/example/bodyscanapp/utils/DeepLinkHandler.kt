package com.example.bodyscanapp.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.bodyscanapp.data.AuthManager
import com.example.bodyscanapp.data.AuthResult

/**
 * Deep Link Handler
 * Handles deep links for email authentication and other app navigation
 */
class DeepLinkHandler(private val context: Context) {
    
    private val authManager = AuthManager(context)
    
    /**
     * Handle incoming deep link
     * @param intent The intent containing the deep link
     * @return true if the link was handled, false otherwise
     */
    suspend fun handleDeepLink(intent: Intent): Boolean {
        val data: Uri? = intent.data
        
        if (data == null) return false
        
        return when {
            // Handle email sign-in links
            authManager.isSignInWithEmailLink(data.toString()) -> {
                handleEmailSignInLink(data)
                true
            }
            // Handle other deep links here in the future
            else -> false
        }
    }
    
    /**
     * Handle email sign-in link
     * @param uri The URI containing the sign-in link
     * @return true if handled successfully
     */
    private suspend fun handleEmailSignInLink(uri: Uri): Boolean {
        return try {
            // Extract email from the link or use stored email
            val email = extractEmailFromLink(uri) ?: getStoredEmail()
            
            if (email != null) {
                val result = authManager.signInWithEmailLink(email, uri.toString())
                // Handle the result (you might want to emit this to a flow or callback)
                result.collect { authResult ->
                    when (authResult) {
                        is AuthResult.Success -> {
                            // Successfully signed in
                            android.util.Log.d("DeepLinkHandler", "Email sign-in successful")
                        }
                        is AuthResult.Error -> {
                            // Handle error
                            android.util.Log.e("DeepLinkHandler", "Email sign-in failed: ${authResult.message}")
                        }
                        is AuthResult.Loading -> {
                            // Still loading
                        }
                    }
                }
                true
            } else {
                android.util.Log.e("DeepLinkHandler", "No email found for sign-in link")
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("DeepLinkHandler", "Error handling email sign-in link", e)
            false
        }
    }
    
    /**
     * Extract email from the sign-in link
     * @param uri The URI containing the sign-in link
     * @return email address if found, null otherwise
     */
    private fun extractEmailFromLink(uri: Uri): String? {
        return uri.getQueryParameter("email")
    }
    
    /**
     * Get stored email from SharedPreferences
     * @return stored email if available, null otherwise
     */
    private fun getStoredEmail(): String? {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        return prefs.getString("pending_email", null)
    }
    
    /**
     * Store email for later use in sign-in process
     * @param email Email to store
     */
    fun storeEmailForSignIn(email: String) {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("pending_email", email).apply()
    }
    
    /**
     * Clear stored email
     */
    fun clearStoredEmail() {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("pending_email").apply()
    }
    
    /**
     * Check if the app was launched from an email link
     * @param intent The launch intent
     * @return true if launched from email link
     */
    fun isLaunchedFromEmailLink(intent: Intent): Boolean {
        val data = intent.data
        return data != null && authManager.isSignInWithEmailLink(data.toString())
    }
}
