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
    
    /**
     * Lazy-initialized Google Sign-In client
     * 
     * Configures Google Sign-In with:
     * - requestIdToken: Required for Firebase Authentication. This token is used to create
     *   a Firebase credential. The web client ID comes from Firebase Console and is stored
     *   in strings.xml (automatically extracted from google-services.json).
     * - requestEmail: Requests user's email address for profile display
     * 
     * The client is created lazily (only when first accessed) to avoid unnecessary
     * initialization if Google Sign-In is never used in a session.
     */
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
     * 
     * Returns the pre-configured intent from GoogleSignInClient that launches
     * the Google Sign-In activity. This activity presents the user with:
     * - List of Google accounts on the device
     * - Option to add a new account
     * - Account selection UI
     * 
     * The intent is configured with the options set in googleSignInClient
     * (ID token request, email request, web client ID).
     * 
     * @return Intent that launches Google Sign-In activity
     */
    suspend fun getGoogleSignInIntent(): android.content.Intent {
        // Sign out from Google Sign-In Client to force account picker
        // This doesn't affect Firebase auth state, only Google's account selection
        try {
            googleSignInClient.signOut().await()
        } catch (e: Exception) {
            Log.d(TAG, "Error clearing Google Sign-In state: ${e.message}")
        }
        return googleSignInClient.signInIntent
    }
    
    /**
     * Handle Google Sign-In result
     * 
     * Processes the result from Google Sign-In activity and authenticates with Firebase.
     * This is a multi-step process that handles various success and error cases.
     * 
     * Process:
     * 1. Extract GoogleSignInAccount from the intent data using GoogleSignIn API
     * 2. Validate that ID token is present (required for Firebase auth)
     * 3. Create Firebase AuthCredential from the Google ID token
     * 4. Sign in to Firebase using the credential
     * 5. Return AuthResult with Firebase user on success
     * 
     * Error Handling:
     * - ApiException status 12501: User cancelled the sign-in (pressed back)
     * - ApiException status 7: Network error (no internet connection)
     * - ApiException status 10: Invalid configuration (wrong web client ID or SHA-1)
     * - Missing ID token: Configuration issue or Google Sign-In not properly set up
     * - Generic Exception: Unexpected errors during Firebase authentication
     * 
     * All errors are logged with detailed information for debugging and return
     * user-friendly error messages in AuthResult.Error.
     * 
     * @param data Intent data from Google Sign-In activity result (contains account info)
     * @return AuthResult.Success with Firebase user on success, AuthResult.Error on failure
     */
    suspend fun handleGoogleSignInResult(data: android.content.Intent?): AuthResult {
        return try {
            // Step 1: Extract Google account from the sign-in intent
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            
            // Step 2: Validate ID token presence (critical for Firebase auth)
            if (account?.idToken == null) {
                Log.e(TAG, "Google sign-in failed: ID token not found. Check Firebase configuration.")
                return AuthResult.Error("Google sign-in failed: ID token not found.")
            }
            
            // Step 3: Create Firebase credential from Google ID token
            // The null parameter is for access token, which we don't need for Firebase auth
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            
            // Step 4: Sign in to Firebase with the Google credential
            val result = auth.signInWithCredential(credential).await()
            val user = result.user
            
            // Step 5: Return success or error based on Firebase result
            if (user != null) {
                Log.d(TAG, "Successfully signed in with Google: ${user.email}")
                AuthResult.Success("Successfully signed in with Google", user)
            } else {
                Log.e(TAG, "Firebase sign-in completed but no user returned")
                AuthResult.Error("Google sign-in failed: No user returned")
            }
        } catch (e: ApiException) {
            // Handle Google Sign-In specific errors
            Log.e(TAG, "Google sign-in API exception: ${e.statusCode} - ${e.message}", e)
            when (e.statusCode) {
                12501 -> AuthResult.Error("Google sign-in was cancelled")
                7 -> AuthResult.Error("Network error during Google sign-in")
                10 -> AuthResult.Error("Google sign-in failed: Invalid configuration")
                else -> AuthResult.Error("Google sign-in failed: ${e.message}")
            }
        } catch (e: Exception) {
            // Handle unexpected errors (Firebase exceptions, etc.)
            Log.e(TAG, "Unexpected error during Google sign-in", e)
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
