package com.example.bodyscanapp.data

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * BiometricAuthManager
 * 
 * Manages biometric authentication (fingerprint, face recognition) for the app.
 * Provides methods to check device biometric capabilities and show authentication prompts.
 * 
 * This manager is designed to replace TOTP verification during session persistence,
 * offering a faster and more convenient authentication method while maintaining security.
 * 
 * Usage:
 * 1. Check biometric support with checkBiometricSupport()
 * 2. Show authentication prompt with showBiometricPrompt()
 * 3. Observe authentication results via authResultFlow
 * 
 * @param context Android context for biometric manager initialization
 */
class BiometricAuthManager(private val context: Context) {

    // A coroutine channel to emit a single authentication result.
    // Using Channel instead of StateFlow ensures we don't miss one-time events.
    private val authResultChannel = Channel<BiometricAuthStatus>()
    
    /**
     * Flow that emits biometric authentication results.
     * ViewModels should collect from this flow to handle auth outcomes.
     */
    val authResultFlow = authResultChannel.receiveAsFlow()

    /**
     * Checks if biometric authentication is available on the device.
     * 
     * Verifies:
     * - Device has biometric hardware
     * - Hardware is currently available
     * - User has enrolled at least one biometric
     * 
     * Uses BIOMETRIC_STRONG authenticator which includes:
     * - Fingerprint
     * - Face recognition (if meets strong security requirements)
     * - Iris scanning (if available)
     * 
     * @return BiometricAuthStatus indicating the availability status
     */
    fun checkBiometricSupport(): BiometricAuthStatus {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAuthStatus.SUCCESS // Ready to authenticate
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAuthStatus.ERROR_NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAuthStatus.ERROR_HW_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAuthStatus.ERROR_NONE_ENROLLED
            else -> BiometricAuthStatus.ERROR_UNKNOWN
        }
    }

    /**
     * Displays the biometric authentication prompt to the user.
     * 
     * The prompt will show the system's native biometric dialog with the provided
     * title and subtitle. Users can authenticate using enrolled biometrics or cancel.
     * 
     * Results are emitted through authResultFlow:
     * - SUCCESS: Biometric authentication succeeded
     * - ERROR_AUTH_FAILED: Authentication failed (wrong biometric, user cancelled, timeout)
     * 
     * Note: This method requires a FragmentActivity context to attach the prompt.
     * 
     * @param activity The FragmentActivity hosting the authentication prompt
     * @param title Title displayed in the biometric prompt dialog
     * @param subtitle Subtitle providing context for the authentication request
     * @param negativeButtonText Text for the cancel/negative button (default: "Cancel")
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButtonText: String = "Cancel"
    ) {
        // Verify biometric support before showing prompt
        if (checkBiometricSupport() != BiometricAuthStatus.SUCCESS) {
            // Emit error status if biometrics not available
            authResultChannel.trySend(BiometricAuthStatus.ERROR_NO_HARDWARE)
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                /**
                 * Called when biometric authentication succeeds.
                 * Emits SUCCESS status to authResultFlow.
                 */
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    authResultChannel.trySend(BiometricAuthStatus.SUCCESS)
                }

                /**
                 * Called when authentication encounters an error.
                 * This includes user cancellation, timeout, lockout, etc.
                 * Emits ERROR_AUTH_FAILED status to authResultFlow.
                 * 
                 * @param errorCode System error code identifying the error type
                 * @param errString Human-readable error message
                 */
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Map specific error codes to appropriate status
                    val status = when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> BiometricAuthStatus.ERROR_USER_CANCELLED
                        BiometricPrompt.ERROR_LOCKOUT,
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> BiometricAuthStatus.ERROR_LOCKOUT
                        else -> BiometricAuthStatus.ERROR_AUTH_FAILED
                    }
                    authResultChannel.trySend(status)
                }

                /**
                 * Called when biometric is recognized but doesn't match enrolled biometrics.
                 * The prompt remains on screen for retry. No status emitted here.
                 */
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Don't send error here - prompt stays active for retry
                }
            }
        )

        // Build and show the biometric prompt with specified configuration
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}

/**
 * Enum representing possible biometric authentication outcomes.
 * Used to communicate authentication status between BiometricAuthManager and ViewModels.
 */
enum class BiometricAuthStatus {
    /** Biometric authentication succeeded */
    SUCCESS,
    
    /** Device has no biometric hardware */
    ERROR_NO_HARDWARE,
    
    /** Biometric hardware exists but is currently unavailable */
    ERROR_HW_UNAVAILABLE,
    
    /** User hasn't enrolled any biometrics on their device */
    ERROR_NONE_ENROLLED,
    
    /** Authentication failed, wrong biometric, or general error */
    ERROR_AUTH_FAILED,
    
    /** User cancelled the authentication prompt */
    ERROR_USER_CANCELLED,
    
    /** Too many failed attempts, biometric authentication is locked */
    ERROR_LOCKOUT,
    
    /** Unknown or unexpected error occurred */
    ERROR_UNKNOWN
}

