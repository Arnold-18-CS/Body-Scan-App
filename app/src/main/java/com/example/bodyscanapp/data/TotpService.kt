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
     * @return true if the code is valid, false otherwise
     */
    fun verifyTotpCode(code: String): Boolean {
        if (code.length != 6 || !code.all { it.isDigit() }) {
            return false
        }

        // The library's isValid method handles clock drift automatically
        return totpGenerator.isValid(code)
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
