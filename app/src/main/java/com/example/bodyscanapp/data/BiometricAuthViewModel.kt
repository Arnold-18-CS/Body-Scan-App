package com.example.bodyscanapp.data

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * BiometricAuthViewModel
 * 
 * Manages the state and logic for biometric authentication during session persistence.
 * 
 * This ViewModel:
 * - Tracks whether the user has been authenticated via biometrics
 * - Manages the biometric authentication flow
 * - Exposes biometric availability status
 * - Handles authentication results from BiometricAuthManager
 * 
 * Flow:
 * 1. User reopens app with active session
 * 2. Show toast "Already signed in as $email"
 * 3. Show biometric auth screen instead of TOTP
 * 4. On successful biometric auth, navigate to Home
 * 5. On failure, allow retry or fallback options
 * 
 * @param biometricAuthManager Manager handling biometric operations
 */
class BiometricAuthViewModel(
    private val biometricAuthManager: BiometricAuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BiometricAuthUiState())
    val uiState: StateFlow<BiometricAuthUiState> = _uiState.asStateFlow()

    init {
        // Observe authentication results from the BiometricAuthManager
        viewModelScope.launch {
            biometricAuthManager.authResultFlow.collect { status ->
                handleAuthResult(status)
            }
        }
        
        // Check biometric availability on initialization
        checkBiometricAvailability()
    }

    /**
     * Checks if biometric authentication is available on the device.
     * Updates UI state with availability status and appropriate messaging.
     */
    private fun checkBiometricAvailability() {
        val status = biometricAuthManager.checkBiometricSupport()
        _uiState.update { 
            it.copy(
                biometricStatus = status,
                isBiometricAvailable = status == BiometricAuthStatus.SUCCESS
            )
        }
    }

    /**
     * Initiates the biometric authentication flow.
     * Shows the system biometric prompt dialog.
     * 
     * @param activity FragmentActivity context required for showing biometric prompt
     */
    fun authenticate(activity: FragmentActivity) {
        _uiState.update { 
            it.copy(
                isAuthenticating = true,
                errorMessage = null
            )
        }
        
        biometricAuthManager.showBiometricPrompt(
            activity = activity,
            title = "Verify it's you",
            subtitle = "Use biometrics to continue to your account"
        )
    }

    /**
     * Handles the result from biometric authentication.
     * Updates UI state based on success or various error conditions.
     * 
     * @param status The authentication result status
     */
    private fun handleAuthResult(status: BiometricAuthStatus) {
        when (status) {
            BiometricAuthStatus.SUCCESS -> {
                _uiState.update { 
                    it.copy(
                        isAuthenticated = true,
                        isAuthenticating = false,
                        errorMessage = null
                    )
                }
            }
            BiometricAuthStatus.ERROR_USER_CANCELLED -> {
                _uiState.update { 
                    it.copy(
                        isAuthenticating = false,
                        errorMessage = "Authentication cancelled"
                    )
                }
            }
            BiometricAuthStatus.ERROR_LOCKOUT -> {
                _uiState.update { 
                    it.copy(
                        isAuthenticating = false,
                        errorMessage = "Too many failed attempts. Please try again later."
                    )
                }
            }
            BiometricAuthStatus.ERROR_NO_HARDWARE,
            BiometricAuthStatus.ERROR_HW_UNAVAILABLE,
            BiometricAuthStatus.ERROR_NONE_ENROLLED -> {
                _uiState.update { 
                    it.copy(
                        isAuthenticating = false,
                        isBiometricAvailable = false,
                        errorMessage = getBiometricErrorMessage(status)
                    )
                }
            }
            else -> {
                _uiState.update { 
                    it.copy(
                        isAuthenticating = false,
                        errorMessage = "Authentication failed. Please try again."
                    )
                }
            }
        }
    }

    /**
     * Provides user-friendly error messages for biometric error states.
     * 
     * @param status The biometric error status
     * @return User-friendly error message
     */
    private fun getBiometricErrorMessage(status: BiometricAuthStatus): String {
        return when (status) {
            BiometricAuthStatus.ERROR_NO_HARDWARE -> 
                "This device doesn't support biometric authentication."
            BiometricAuthStatus.ERROR_HW_UNAVAILABLE -> 
                "Biometric authentication is currently unavailable."
            BiometricAuthStatus.ERROR_NONE_ENROLLED -> 
                "No biometrics enrolled. Please set up fingerprint or face recognition in your device settings."
            else -> "Biometric authentication unavailable."
        }
    }

    /**
     * Resets the authentication state.
     * Useful when user navigates away from the auth screen.
     */
    fun resetAuthState() {
        _uiState.update { 
            it.copy(
                isAuthenticated = false,
                isAuthenticating = false,
                errorMessage = null
            )
        }
    }

    /**
     * Clears any error messages.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * BiometricAuthUiState
 * 
 * Represents the UI state for biometric authentication screen.
 * 
 * @property isAuthenticated Whether user has successfully authenticated via biometrics
 * @property isAuthenticating Whether authentication is currently in progress
 * @property isBiometricAvailable Whether biometric hardware is available and enrolled
 * @property biometricStatus Detailed status of biometric availability
 * @property errorMessage Error message to display to user, if any
 */
data class BiometricAuthUiState(
    val isAuthenticated: Boolean = false,
    val isAuthenticating: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val biometricStatus: BiometricAuthStatus = BiometricAuthStatus.ERROR_UNKNOWN,
    val errorMessage: String? = null
)

/**
 * Factory for creating BiometricAuthViewModel instances.
 * Required because ViewModel needs BiometricAuthManager parameter.
 * 
 * Usage:
 * val viewModel: BiometricAuthViewModel = viewModel(
 *     factory = BiometricAuthViewModelFactory(biometricAuthManager)
 * )
 */
class BiometricAuthViewModelFactory(
    private val biometricAuthManager: BiometricAuthManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BiometricAuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BiometricAuthViewModel(biometricAuthManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

