package com.example.bodyscanapp.data

import android.content.Context
import android.content.SharedPreferences
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordConfig
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordGenerator
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

/**
 * Service for handling TOTP (Time-based One-Time Password) operations
 * Implements real TOTP with user-specific secret keys and secure storage
 */
class TotpService(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "totp_prefs"
        private const val SECRET_KEY_PREFIX = "totp_secret_"
        private const val TOTP_SETUP_PREFIX = "totp_setup_"
        private const val SECRET_LENGTH = 32 // 32 bytes = 256 bits
    }

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val secureRandom = SecureRandom()

    // TOTP configuration
    private val totpConfig = TimeBasedOneTimePasswordConfig(
        timeStep = 30,
        timeStepUnit = TimeUnit.SECONDS,
        codeDigits = 6,
        hmacAlgorithm = HmacAlgorithm.SHA1
    )

    /**
     * Generates a cryptographically secure TOTP secret for a user
     * @param username The username to generate the secret for
     * @return Base32-encoded secret key
     */
    fun generateSecretKey(username: String): String {
        val secretBytes = ByteArray(SECRET_LENGTH)
        secureRandom.nextBytes(secretBytes)
        val secretKey = Base32.encode(secretBytes)
        storeSecretKey(username, secretKey)
        return secretKey
    }

    /**
     * Stores a TOTP secret key for a user
     * @param username The username
     * @param secretKey The Base32-encoded secret key
     */
    private fun storeSecretKey(username: String, secretKey: String) {
        val key = SECRET_KEY_PREFIX + username
        sharedPreferences.edit()
            .putString(key, secretKey)
            .apply()
    }

    /**
     * Retrieves the stored TOTP secret key for a user
     * @param username The username
     * @return Base32-encoded secret key or null if not found
     */
    private fun getSecretKey(username: String): String? {
        val key = SECRET_KEY_PREFIX + username
        return sharedPreferences.getString(key, null)
    }

    /**
     * Checks if a user has TOTP set up
     * @param username The username
     * @return true if TOTP is set up, false otherwise
     */
    fun isTotpSetup(username: String): Boolean {
        val key = TOTP_SETUP_PREFIX + username
        return sharedPreferences.getBoolean(key, false)
    }

    /**
     * Marks TOTP as set up for a user
     * @param username The username
     */
    fun markTotpSetup(username: String) {
        val key = TOTP_SETUP_PREFIX + username
        sharedPreferences.edit()
            .putBoolean(key, true)
            .apply()
    }

    /**
     * Generates a TOTP code for a user
     * @param username The username
     * @return Generated TOTP code or null if user has no secret key
     */
    fun generateTotpCode(username: String): String? {
        val secretKey = getSecretKey(username) ?: return null
        val secretBytes = Base32.decode(secretKey)
        val totpGenerator = TimeBasedOneTimePasswordGenerator(secretBytes, totpConfig)
        return totpGenerator.generate()
    }

    /**
     * Verifies a TOTP code for a user
     * @param username The username
     * @param code The 6-digit code to verify
     * @return TotpVerificationResult with success status and error message
     */
    fun verifyTotpCode(username: String, code: String): TotpVerificationResult {
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
            else -> {
                val secretKey = getSecretKey(username)
                if (secretKey == null) {
                    TotpVerificationResult.Error("TOTP not set up for this user")
                } else {
                    val secretBytes = Base32.decode(secretKey)
                    val totpGenerator = TimeBasedOneTimePasswordGenerator(secretBytes, totpConfig)
                    
                    if (totpGenerator.isValid(code)) {
                        TotpVerificationResult.Success
                    } else {
                        TotpVerificationResult.Error("Invalid or expired code. Please try again")
                    }
                }
            }
        }
    }

    /**
     * Verifies a TOTP code (backward compatibility method)
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
            else -> {
                TotpVerificationResult.Error("TOTP not set up. Please set up TOTP first.")
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

    /**
     * Formats a secret key for easy manual entry
     * @param secretKey The Base32-encoded secret key
     * @return Formatted secret key with spaces for readability
     */
    fun formatSecretKey(secretKey: String): String {
        return secretKey.chunked(4).joinToString(" ")
    }

    /**
     * Removes TOTP setup for a user
     * @param username The username
     */
    fun removeTotpSetup(username: String) {
        val secretKey = SECRET_KEY_PREFIX + username
        val setupKey = TOTP_SETUP_PREFIX + username
        sharedPreferences.edit()
            .remove(secretKey)
            .remove(setupKey)
            .apply()
    }
}

/**
 * Result of TOTP code verification
 */
sealed class TotpVerificationResult {
    object Success : TotpVerificationResult()
    data class Error(val message: String) : TotpVerificationResult()
}

/**
 * Base32 encoding utility
 */
object Base32 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    
    fun encode(data: ByteArray): String {
        if (data.isEmpty()) return ""
        
        val output = StringBuilder()
        var buffer = 0
        var bufferLength = 0
        
        for (byte in data) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
            bufferLength += 8
            
            while (bufferLength >= 5) {
                val index = (buffer shr (bufferLength - 5)) and 0x1F
                output.append(ALPHABET[index])
                bufferLength -= 5
            }
        }
        
        if (bufferLength > 0) {
            val index = (buffer shl (5 - bufferLength)) and 0x1F
            output.append(ALPHABET[index])
        }
        
        return output.toString()
    }
    
    fun decode(encoded: String): ByteArray {
        if (encoded.isEmpty()) return ByteArray(0)
        
        val input = encoded.uppercase().replace(" ", "")
        val output = mutableListOf<Byte>()
        var buffer = 0
        var bufferLength = 0
        
        for (char in input) {
            val index = ALPHABET.indexOf(char)
            if (index == -1) continue
            
            buffer = (buffer shl 5) or index
            bufferLength += 5
            
            while (bufferLength >= 8) {
                output.add(((buffer shr (bufferLength - 8)) and 0xFF).toByte())
                bufferLength -= 8
            }
        }
        
        return output.toByteArray()
    }
}
