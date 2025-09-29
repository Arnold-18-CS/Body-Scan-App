package com.example.bodyscanapp.data

import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordConfig
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordGenerator
import java.util.concurrent.TimeUnit

/**
 * Service for handling TOTP (Time-based One-Time Password) operations
 * This is a mock implementation for demonstration purposes
 */
class TotpService {

    // Mock secret key for demonstration - in real app, this would be user-specific
    private val mockSecretKey = "JBSWY3DPEHPK3PXP".toByteArray()

    // Mock TOTP generator
    private val totpGenerator: TimeBasedOneTimePasswordGenerator

    init {
        val config = TimeBasedOneTimePasswordConfig(
            timeStep = 30,
            timeStepUnit = TimeUnit.SECONDS,
            codeDigits = 6,
            hmacAlgorithm = HmacAlgorithm.SHA1
        )
        totpGenerator = TimeBasedOneTimePasswordGenerator(mockSecretKey, config)
    }

    /**
     * Generates a mock TOTP code for demonstration
     * In a real app, this would use the user's actual secret key
     */
    fun generateMockTotpCode(): String {
        return totpGenerator.generate()
    }

    /**
     * Verifies a TOTP code
     * @param code The 6-digit code to verify
     * @return TotpResult with success status and specific error message
     */
    fun verifyTotpCode(code: String): TotpResult {
        return when {
            code.isBlank() -> TotpResult.Error("Please enter the 6-digit code from your authenticator app")
            code.length != 6 -> TotpResult.Error("Code must be exactly 6 digits")
            !code.all { it.isDigit() } -> TotpResult.Error("Code must contain only numbers")
            !totpGenerator.isValid(code) -> TotpResult.Error("Invalid or expired code. Please try again or request a new code")
            else -> TotpResult.Success
        }
    }
    
    /**
     * Verifies a TOTP code (legacy method for backward compatibility)
     * @param code The 6-digit code to verify
     * @return true if the code is valid, false otherwise
     */
    fun verifyTotpCodeLegacy(code: String): Boolean {
        return verifyTotpCode(code) is TotpResult.Success
    }

    /**
     * Gets the remaining time in the current TOTP cycle
     * @return seconds remaining in current 30-second cycle
     */
    fun getRemainingTime(): Int {
        val currentTime = System.currentTimeMillis()
        val timeStep = TimeUnit.SECONDS.toMillis(30)
        val timeInCurrentStep = currentTime % timeStep
        return ((timeStep - timeInCurrentStep) / 1000).toInt()
    }

    /**
     * Gets the progress of the current TOTP cycle (0.0 to 1.0)
     * @return progress value between 0.0 and 1.0
     */
    fun getCycleProgress(): Float {
        val currentTime = System.currentTimeMillis()
        val timeStep = TimeUnit.SECONDS.toMillis(30)
        val timeInCurrentStep = currentTime % timeStep
        return (timeInCurrentStep.toFloat() / timeStep.toFloat())
    }
}

/**
 * Result class for TOTP verification
 */
sealed class TotpResult {
    object Success : TotpResult()
    data class Error(val message: String) : TotpResult()
}
