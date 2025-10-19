package com.example.bodyscanapp.data

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Authentication Manager
 * 
 * Centralized authentication service that orchestrates all authentication operations:
 * - Email link (passwordless) authentication with email storage
 * - Google Sign-In authentication
 * - Authentication state management
 * - User profile and account management
 * - Username selection flow
 * 
 * This manager coordinates between FirebaseAuthService and UserPreferencesRepository
 * to provide a complete authentication solution with state tracking.
 * 
 * The auth state is exposed through a StateFlow that can be observed by UI components
 * to react to authentication changes.
 * 
 * @param context Android context for service initialization
 */
class AuthManager(private val context: Context) {
    
    private val firebaseAuthService = FirebaseAuthService(context)
    private val userPrefsRepo = UserPreferencesRepository(context)
    private val _authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    /**
     * Send email link for passwordless authentication
     * Uses Firebase's default hosted authentication page to prevent domain allowlisting errors
     * @param email User's email address
     * @param continueUrl URL to redirect to after email verification (defaults to Firebase hosted page)
     * @return Flow of AuthResult
     */
    suspend fun sendEmailLink(
        email: String, 
        continueUrl: String = "https://body-scan-app.firebaseapp.com/finishSignIn"
    ): Flow<AuthResult> {
        _authState.value = AuthState.Loading
        
        // Create action code settings with Firebase default hosted page
        val actionCodeSettings = firebaseAuthService.createActionCodeSettings(
            url = continueUrl,
            handleCodeInApp = true
        )
        
        val result = firebaseAuthService.sendSignInLinkToEmail(email, actionCodeSettings)
        
        if (result is AuthResult.Success) {
            _authState.value = AuthState.EmailLinkSent(email)
        } else {
            _authState.value = AuthState.SignedOut
        }
        
        return kotlinx.coroutines.flow.flowOf(result)
    }
    
    /**
     * Sign in with email link
     * @param email User's email address
     * @param link The sign-in link from email
     * @return Flow of AuthResult
     */
    suspend fun signInWithEmailLink(email: String, link: String): Flow<AuthResult> {
        _authState.value = AuthState.Loading
        
        val result = firebaseAuthService.signInWithEmailLink(email, link)
        
        _authState.value = if (result is AuthResult.Success && result.user != null) {
            AuthState.SignedIn(result.user)
        } else {
            AuthState.SignedOut
        }
        
        return kotlinx.coroutines.flow.flowOf(result)
    }
    
    /**
     * Check if the link is a valid sign-in link
     * @param link The link to check
     * @return true if it's a valid sign-in link
     */
    fun isSignInWithEmailLink(link: String): Boolean {
        return firebaseAuthService.isSignInWithEmailLink(link)
    }
    
    /**
     * Get Google Sign-In intent
     * 
     * Returns a pre-configured intent that launches the Google Sign-In activity.
     * The intent is configured with:
     * - Request for ID token (required for Firebase Authentication)
     * - Request for email address
     * - Web client ID from Firebase configuration (from strings.xml)
     * 
     * This intent should be launched using Activity Result API in MainActivity.
     * The result will be handled by handleGoogleSignInResult().
     * 
     * @return Intent for Google Sign-In activity
     */
    fun getGoogleSignInIntent(): Intent {
        return firebaseAuthService.getGoogleSignInIntent()
    }
    
    /**
     * Handle Google Sign-In result
     * 
     * Processes the result from Google Sign-In activity and authenticates with Firebase.
     * 
     * Flow:
     * 1. Extract Google account from intent data
     * 2. Get ID token from Google account
     * 3. Create Firebase credential using the ID token
     * 4. Sign in to Firebase with the credential
     * 5. Update auth state based on result
     * 
     * On success:
     * - Updates authState to SignedIn with the Firebase user
     * - UI observes this change and navigates appropriately:
     *   - New users go to username selection
     *   - Returning users go to TOTP setup/verification
     * 
     * On error:
     * - Updates authState to SignedOut
     * - Returns AuthResult.Error with user-friendly message
     * - Common errors: User cancelled, network issues, invalid configuration
     * 
     * @param data Intent data from Google Sign-In activity result
     * @return Flow of AuthResult containing success/error information
     */
    suspend fun handleGoogleSignInResult(data: Intent?): Flow<AuthResult> {
        _authState.value = AuthState.Loading
        
        val result = firebaseAuthService.handleGoogleSignInResult(data)
        
        _authState.value = if (result is AuthResult.Success && result.user != null) {
            AuthState.SignedIn(result.user)
        } else {
            AuthState.SignedOut
        }
        
        return kotlinx.coroutines.flow.flowOf(result)
    }
    
    /**
     * Sign out from all providers
     * @return Flow of AuthResult
     */
    suspend fun signOut(): Flow<AuthResult> {
        _authState.value = AuthState.Loading
        
        val result = firebaseAuthService.signOut()
        
        if (result is AuthResult.Success) {
            _authState.value = AuthState.SignedOut
        }
        
        return kotlinx.coroutines.flow.flowOf(result)
    }
    
    /**
     * Get current user
     * @return FirebaseUser if signed in, null otherwise
     */
    fun getCurrentUser() = firebaseAuthService.getCurrentUser()
    
    /**
     * Check if user is signed in
     * @return true if user is signed in
     */
    fun isSignedIn() = firebaseAuthService.isSignedIn()
    
    /**
     * Get user display name
     * @return User's display name or email if display name is not set
     */
    fun getUserDisplayName() = firebaseAuthService.getUserDisplayName()
    
    /**
     * Get user email
     * @return User's email address
     */
    fun getUserEmail() = firebaseAuthService.getUserEmail()
    
    /**
     * Update user profile
     * @param displayName New display name
     * @return Flow of AuthResult
     */
    suspend fun updateUserProfile(displayName: String): Flow<AuthResult> {
        val result = firebaseAuthService.updateUserProfile(displayName)
        return kotlinx.coroutines.flow.flowOf(result)
    }
    
    /**
     * Delete user account
     * @return Flow of AuthResult
     */
    suspend fun deleteAccount(): Flow<AuthResult> {
        _authState.value = AuthState.Loading
        
        val result = firebaseAuthService.deleteAccount()
        
        if (result is AuthResult.Success) {
            _authState.value = AuthState.SignedOut
        }
        
        return kotlinx.coroutines.flow.flowOf(result)
    }
    
    /**
     * Initialize auth state on app start
     */
    fun initializeAuthState() {
        val user = getCurrentUser()
        _authState.value = if (user != null) {
            AuthState.SignedIn(user)
        } else {
            AuthState.SignedOut
        }
    }
    
    /**
     * Check if user needs username selection (first-time user)
     * Checks if the user has a stored username in preferences
     * @param user Firebase user
     * @return true if username selection is needed (first-time user or no stored username)
     */
    fun needsUsernameSelection(user: com.google.firebase.auth.FirebaseUser): Boolean {
        // Check if user has a stored username in preferences
        val hasUsername = userPrefsRepo.getUsername(user.uid) != null
        // Also check if user is marked as first-time
        val isFirstTime = userPrefsRepo.isFirstTimeUser(user.uid)
        
        return !hasUsername || isFirstTime
    }
    
    /**
     * Set username selection state
     * @param user Firebase user
     */
    fun setUsernameSelectionRequired(user: com.google.firebase.auth.FirebaseUser) {
        _authState.value = AuthState.UsernameSelectionRequired(user)
    }
    
    /**
     * Complete username selection and proceed to signed in state
     * @param user Firebase user
     */
    fun completeUsernameSelection(user: com.google.firebase.auth.FirebaseUser) {
        _authState.value = AuthState.SignedIn(user)
    }
    
    /**
     * Fetch sign-in methods for email
     * @param email User's email address
     * @return Flow of AuthResult
     */
    suspend fun fetchSignInMethodsForEmail(email: String): Flow<AuthResult> {
        val result = firebaseAuthService.fetchSignInMethodsForEmail(email)
        return kotlinx.coroutines.flow.flowOf(result)
    }
    
    /**
     * Save email for email link authentication
     * Stores the email so it can be retrieved when user clicks the link
     * @param email User's email address
     * @return true if successful, false otherwise
     */
    fun saveEmailForLinkAuth(email: String): Boolean {
        return userPrefsRepo.savePendingEmailForLinkAuth(email)
    }
    
    /**
     * Retrieve stored email for email link authentication
     * @return stored email or null if not found or expired
     */
    fun retrieveEmailForLinkAuth(): String? {
        return userPrefsRepo.retrievePendingEmailForLinkAuth()
    }
    
    /**
     * Clear stored email after successful authentication or expiration
     * @return true if successful, false otherwise
     */
    fun clearStoredEmail(): Boolean {
        return userPrefsRepo.clearPendingEmailForLinkAuth()
    }
    
    /**
     * Check if there is a pending email link authentication
     * @return true if pending, false otherwise
     */
    fun hasPendingEmailLinkAuth(): Boolean {
        return userPrefsRepo.hasPendingEmailLinkAuth()
    }
}

/**
 * Authentication state sealed class
 */
sealed class AuthState {
    object SignedOut : AuthState()
    object Loading : AuthState()
    data class EmailLinkSent(val email: String) : AuthState()
    data class SignedIn(val user: com.google.firebase.auth.FirebaseUser) : AuthState()
    data class UsernameSelectionRequired(val user: com.google.firebase.auth.FirebaseUser) : AuthState()
}
