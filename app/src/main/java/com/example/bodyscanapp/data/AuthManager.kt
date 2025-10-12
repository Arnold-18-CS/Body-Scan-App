package com.example.bodyscanapp.data

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Authentication Manager
 * Centralized authentication service that manages both email-link and Google sign-in
 */
class AuthManager(private val context: Context) {
    
    private val firebaseAuthService = FirebaseAuthService(context)
    private val _authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    /**
     * Send email link for passwordless authentication
     * @param email User's email address
     * @param continueUrl URL to redirect to after email verification
     * @return Flow of AuthResult
     */
    suspend fun sendEmailLink(email: String, continueUrl: String = "https://bodyscanapp.page.link/signin"): Flow<AuthResult> {
        _authState.value = AuthState.Loading
        
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
        
        if (result is AuthResult.Success && result.user != null) {
            _authState.value = AuthState.SignedIn(result.user!!)
        } else {
            _authState.value = AuthState.SignedOut
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
     * @return Intent for Google Sign-In
     */
    fun getGoogleSignInIntent(): Intent {
        return firebaseAuthService.getGoogleSignInIntent()
    }
    
    /**
     * Handle Google Sign-In result
     * @param data Intent data from Google Sign-In
     * @return Flow of AuthResult
     */
    suspend fun handleGoogleSignInResult(data: Intent?): Flow<AuthResult> {
        _authState.value = AuthState.Loading
        
        val result = firebaseAuthService.handleGoogleSignInResult(data)
        
        if (result is AuthResult.Success && result.user != null) {
            _authState.value = AuthState.SignedIn(result.user!!)
        } else {
            _authState.value = AuthState.SignedOut
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
}

/**
 * Authentication state sealed class
 */
sealed class AuthState {
    object SignedOut : AuthState()
    object Loading : AuthState()
    data class EmailLinkSent(val email: String) : AuthState()
    data class SignedIn(val user: com.google.firebase.auth.FirebaseUser) : AuthState()
}
