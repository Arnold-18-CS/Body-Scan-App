package com.example.bodyscanapp.data

import android.content.Context
import android.util.Log
import com.example.bodyscanapp.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

/**
 * Firebase Authentication Service
 * 
 * Handles all Firebase authentication operations including:
 * - Email link (passwordless) authentication
 * - Google Sign-In authentication
 * - User profile management
 * - Account deletion
 * 
 * This service provides a clean abstraction layer over Firebase Auth,
 * handling error cases and providing user-friendly error messages.
 * 
 * @param context Android context for Google Sign-In client initialization
 */
class FirebaseAuthService(private val context: Context) {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }
    
    companion object {
        private const val TAG = "FirebaseAuthService"
        
        /**
         * Validate email format
         * Checks for proper email structure with @ symbol and domain
         * @param email Email address to validate
         * @return true if valid, false otherwise
         */
        fun isValidEmail(email: String): Boolean {
            if (email.isBlank()) return false
            val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+[A-Za-z]{2,}$"
            return email.matches(emailPattern.toRegex())
        }
    }
    
    /**
     * Send email link for passwordless authentication
     * Validates email format before sending
     * @param email User's email address
     * @param actionCodeSettings Settings for the email link
     * @return AuthResult indicating success or failure
     */
    suspend fun sendSignInLinkToEmail(
        email: String,
        actionCodeSettings: ActionCodeSettings
    ): AuthResult {
        // Validate email format first
        if (!isValidEmail(email)) {
            return AuthResult.Error("Please enter a valid email address")
        }
        
        return try {
            auth.sendSignInLinkToEmail(email, actionCodeSettings).await()
            Log.d(TAG, "Sign-in link sent successfully to $email")
            AuthResult.Success("Sign-in link sent to $email")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending sign-in link", e)
            
            // Provide user-friendly error messages based on exception type
            val errorMessage = when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Please check your connection and try again."
                e.message?.contains("invalid-email", ignoreCase = true) == true ->
                    "Invalid email address format"
                e.message?.contains("too-many-requests", ignoreCase = true) == true ->
                    "Too many attempts. Please try again later."
                else ->
                    "Failed to send sign-in link. Please try again."
            }
            
            AuthResult.Error(errorMessage)
        }
    }
    
    /**
     * Sign in with email link
     * Validates email and link before attempting authentication
     * @param email User's email address
     * @param link The sign-in link from email
     * @return AuthResult indicating success or failure
     */
    suspend fun signInWithEmailLink(email: String, link: String): AuthResult {
        // Validate email format
        if (!isValidEmail(email)) {
            return AuthResult.Error("Please enter a valid email address")
        }
        
        // Validate that the link is a sign-in link
        if (!isSignInWithEmailLink(link)) {
            return AuthResult.Error("Invalid sign-in link. Please use the link from your email.")
        }
        
        return try {
            val result = auth.signInWithEmailLink(email, link).await()
            val user = result.user
            if (user != null) {
                Log.d(TAG, "Successfully signed in user: ${user.uid}")
                AuthResult.Success("Successfully signed in", user)
            } else {
                Log.e(TAG, "Sign-in completed but no user returned")
                AuthResult.Error("Sign-in failed: No user returned")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error signing in with email link", e)
            
            // Provide user-friendly error messages
            val errorMessage = when {
                e.message?.contains("invalid-action-code", ignoreCase = true) == true ->
                    "This sign-in link has expired or is invalid. Please request a new one."
                e.message?.contains("expired-action-code", ignoreCase = true) == true ->
                    "This sign-in link has expired. Please request a new one."
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Please check your connection and try again."
                else ->
                    "Failed to sign in. Please try requesting a new link."
            }
            
            AuthResult.Error(errorMessage)
        }
    }
    
    /**
     * Create action code settings for email link
     * Uses Firebase's default hosted authentication page to avoid domain allowlisting issues
     * @param url The URL to redirect to after email verification (defaults to Firebase hosted page)
     * @param handleCodeInApp Whether to handle the code in the app
     * @return ActionCodeSettings configured for the app
     */
    fun createActionCodeSettings(
        url: String = "https://body-scan-app.firebaseapp.com/finishSignIn",
        handleCodeInApp: Boolean = true
    ): ActionCodeSettings {
        return ActionCodeSettings.newBuilder()
            .setUrl(url)
            .setHandleCodeInApp(handleCodeInApp)
            .setAndroidPackageName(
                context.packageName,
                true, // installIfNotAvailable
                null // minimumVersion - null means any version
            )
            .build()
    }
    
    /**
     * Check if the link is a valid sign-in link
     * @param link The link to check
     * @return true if it's a valid sign-in link
     */
    fun isSignInWithEmailLink(link: String): Boolean {
        return auth.isSignInWithEmailLink(link)
    }
    
    /**
     * Get Google Sign-In intent
     * @return Intent for Google Sign-In
     */
    fun getGoogleSignInIntent() = googleSignInClient.signInIntent
    
    /**
     * Handle Google Sign-In result
     * @param data Intent data from Google Sign-In
     * @return AuthResult indicating success or failure
     */
    suspend fun handleGoogleSignInResult(data: android.content.Intent?): AuthResult {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            if (account?.idToken == null) {
                Log.e(TAG, "Google sign-in failed: ID token not found.")
                return AuthResult.Error("Google sign-in failed: ID token not found.")
            }
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            
            val result = auth.signInWithCredential(credential).await()
            val user = result.user
            
            if (user != null) {
                AuthResult.Success("Successfully signed in with Google", user)
            } else {
                AuthResult.Error("Google sign-in failed: No user returned")
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Google sign-in failed", e)
            when (e.statusCode) {
                12501 -> AuthResult.Error("Google sign-in was cancelled")
                7 -> AuthResult.Error("Network error during Google sign-in")
                10 -> AuthResult.Error("Google sign-in failed: Invalid configuration")
                else -> AuthResult.Error("Google sign-in failed: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during Google sign-in", e)
            AuthResult.Error("Google sign-in failed: ${e.message}")
        }
    }
    
    /**
     * Sign out from all providers
     * @return AuthResult indicating success or failure
     */
    suspend fun signOut(): AuthResult {
        return try {
            auth.signOut()
            googleSignInClient.signOut().await()
            AuthResult.Success("Successfully signed out")
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out", e)
            AuthResult.Error("Failed to sign out: ${e.message}")
        }
    }
    
    /**
     * Get current user
     * @return FirebaseUser if signed in, null otherwise
     */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser
    
    /**
     * Check if user is signed in
     * @return true if user is signed in
     */
    fun isSignedIn(): Boolean = auth.currentUser != null
    
    /**
     * Get user display name
     * @return User's display name or email if display name is not set
     */
    fun getUserDisplayName(): String? {
        val user = getCurrentUser()
        return user?.displayName ?: user?.email
    }
    
    /**
     * Get user email
     * @return User's email address
     */
    fun getUserEmail(): String? = getCurrentUser()?.email
    
    /**
     * Update user profile
     * @param displayName New display name
     * @return AuthResult indicating success or failure
     */
    suspend fun updateUserProfile(displayName: String): AuthResult {
        return try {
            val user = getCurrentUser()
            if (user != null) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                user.updateProfile(profileUpdates).await()
                AuthResult.Success("Profile updated successfully")
            } else {
                AuthResult.Error("No user signed in")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile", e)
            AuthResult.Error("Failed to update profile: ${e.message}")
        }
    }
    
    /**
     * Delete user account
     * @return AuthResult indicating success or failure
     */
    suspend fun deleteAccount(): AuthResult {
        return try {
            val user = getCurrentUser()
            if (user != null) {
                user.delete().await()
                AuthResult.Success("Account deleted successfully")
            } else {
                AuthResult.Error("No user signed in")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting account", e)
            AuthResult.Error("Failed to delete account: ${e.message}")
        }
    }
    
    /**
     * Fetch sign-in methods for an email address
     * @param email User's email address
     * @return AuthResult with list of sign-in methods or error
     */
    suspend fun fetchSignInMethodsForEmail(email: String): AuthResult {
        return try {
            if (email.isBlank() || !email.contains("@")) {
                AuthResult.Error("Please enter a valid email address")
            } else {
                val signInMethods = auth.fetchSignInMethodsForEmail(email).await()
                val methods = signInMethods.signInMethods ?: emptyList()
                AuthResult.Success("Sign-in methods fetched", null, methods)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching sign-in methods for email", e)
            when {
                e.message?.contains("network") == true -> 
                    AuthResult.Error("Network error. Please check your connection and try again.")
                e.message?.contains("invalid-email") == true -> 
                    AuthResult.Error("Invalid email address format")
                else -> 
                    AuthResult.Error("Failed to check email: ${e.message}")
            }
        }
    }
}
