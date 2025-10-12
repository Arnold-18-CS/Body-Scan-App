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
 * Handles email-link (passwordless) and Google sign-in authentication
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
    }
    
    /**
     * Send email link for passwordless authentication
     * @param email User's email address
     * @param actionCodeSettings Settings for the email link
     * @return AuthResult indicating success or failure
     */
    suspend fun sendSignInLinkToEmail(
        email: String,
        actionCodeSettings: ActionCodeSettings
    ): AuthResult {
        return try {
            auth.sendSignInLinkToEmail(email, actionCodeSettings).await()
            AuthResult.Success("Sign-in link sent to $email")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending sign-in link", e)
            AuthResult.Error("Failed to send sign-in link: ${e.message}")
        }
    }
    
    /**
     * Sign in with email link
     * @param email User's email address
     * @param link The sign-in link from email
     * @return AuthResult indicating success or failure
     */
    suspend fun signInWithEmailLink(email: String, link: String): AuthResult {
        return try {
            val result = auth.signInWithEmailLink(email, link).await()
            val user = result.user
            if (user != null) {
                AuthResult.Success("Successfully signed in with email link", user)
            } else {
                AuthResult.Error("Sign-in failed: No user returned")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error signing in with email link", e)
            AuthResult.Error("Failed to sign in with email link: ${e.message}")
        }
    }
    
    /**
     * Create action code settings for email link
     * @param url The URL to redirect to after email verification
     * @param handleCodeInApp Whether to handle the code in the app
     * @return ActionCodeSettings configured for the app
     */
    fun createActionCodeSettings(
        url: String,
        handleCodeInApp: Boolean = true
    ): ActionCodeSettings {
        return ActionCodeSettings.newBuilder()
            .setUrl(url)
            .setHandleCodeInApp(handleCodeInApp)
            .setAndroidPackageName(
                context.packageName,
                true, // installIfNotAvailable
                "1" // minimumVersion
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
}
