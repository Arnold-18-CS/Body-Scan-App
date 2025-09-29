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
     * @return TotpVerificationResult with success status and error message
     */
    fun verifyTotpCode(code: String): TotpVerificationResult {
        return when {
            code.isBlank() -> {
                TotpVerificationResult.Error("Please enter the 6-digit code")
            }
            code.length != 6 -> {
                TotpVerificationResult.Error("Code must be exactly 6 digits")
            }
            !code.all { it.isDigit() } -> {
                TotpVerificationResult.Error("Code must contain only numbers")
            }
            !totpGenerator.isValid(code) -> {
                TotpVerificationResult.Error("Invalid or expired code. Please try again")
            }
            else -> {
                TotpVerificationResult.Success
            }
        }
    }
    
    /**
     * Simple boolean verification for backward compatibility
     * @param code The 6-digit code to verify
     * @return true if the code is valid, false otherwise
     */
    fun isTotpCodeValid(code: String): Boolean {
        return verifyTotpCode(code) is TotpVerificationResult.Success
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
 * Result of TOTP code verification
 */
sealed class TotpVerificationResult {
    object Success : TotpVerificationResult()
    data class Error(val message: String) : TotpVerificationResult()
}
