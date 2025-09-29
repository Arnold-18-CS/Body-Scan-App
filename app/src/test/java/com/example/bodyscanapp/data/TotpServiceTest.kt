package com.example.bodyscanapp.data

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TotpServiceTest {

    private lateinit var totpService: TotpService

    @Before
    fun setUp() {
        // Create a real TotpService instance for testing
        // The service will use its own generator, but we can test the validation logic
        totpService = TotpService()
    }

    @Test
    fun `verifyTotpCode with valid 6-digit code returns success`() {
        // Given
        val validCode = "123456"

        // When
        val result = totpService.verifyTotpCode(validCode)

        // Then
        assertTrue("Valid 6-digit code should return success", result is TotpVerificationResult.Success)
    }

    @Test
    fun `verifyTotpCode with blank code returns error`() {
        // Given
        val blankCode = ""

        // When
        val result = totpService.verifyTotpCode(blankCode)

        // Then
        assertTrue("Blank code should return error", result is TotpVerificationResult.Error)
        assertEquals("Please enter the 6-digit code", (result as TotpVerificationResult.Error).message)
    }

    @Test
    fun `verifyTotpCode with whitespace only code returns error`() {
        // Given
        val whitespaceCode = "   "

        // When
        val result = totpService.verifyTotpCode(whitespaceCode)

        // Then
        assertTrue("Whitespace code should return error", result is TotpVerificationResult.Error)
        assertEquals("Please enter the 6-digit code", (result as TotpVerificationResult.Error).message)
    }

    @Test
    fun `verifyTotpCode with code shorter than 6 digits returns error`() {
        // Given
        val shortCode = "12345"

        // When
        val result = totpService.verifyTotpCode(shortCode)

        // Then
        assertTrue("Short code should return error", result is TotpVerificationResult.Error)
        assertEquals("Code must be exactly 6 digits", (result as TotpVerificationResult.Error).message)
    }

    @Test
    fun `verifyTotpCode with code longer than 6 digits returns error`() {
        // Given
        val longCode = "1234567"

        // When
        val result = totpService.verifyTotpCode(longCode)

        // Then
        assertTrue("Long code should return error", result is TotpVerificationResult.Error)
        assertEquals("Code must be exactly 6 digits", (result as TotpVerificationResult.Error).message)
    }

    @Test
    fun `verifyTotpCode with non-numeric code returns error`() {
        // Given
        val nonNumericCodes = listOf(
            "abcdef",
            "12345a",
            "a12345",
            "12a456",
            "12345@",
            "12345!",
            "12 456"
        )

        // When & Then
        nonNumericCodes.forEach { code ->
            val result = totpService.verifyTotpCode(code)
            assertTrue("Non-numeric code '$code' should return error", result is TotpVerificationResult.Error)
            assertEquals("Code must contain only numbers", (result as TotpVerificationResult.Error).message)
        }
    }

    @Test
    fun `verifyTotpCode with mixed valid and invalid characters returns error`() {
        // Given
        val mixedCode = "12a456"

        // When
        val result = totpService.verifyTotpCode(mixedCode)

        // Then
        assertTrue("Mixed code should return error", result is TotpVerificationResult.Error)
        assertEquals("Code must contain only numbers", (result as TotpVerificationResult.Error).message)
    }

    @Test
    fun `isTotpCodeValid with valid code returns true`() {
        // Given
        val validCode = "123456"

        // When
        val result = totpService.isTotpCodeValid(validCode)

        // Then
        assertTrue("Valid code should return true", result)
    }

    @Test
    fun `isTotpCodeValid with invalid code returns false`() {
        // Given
        val invalidCodes = listOf(
            "",
            " ",
            "12345",
            "1234567",
            "abcdef",
            "12345a"
        )

        // When & Then
        invalidCodes.forEach { code ->
            val result = totpService.isTotpCodeValid(code)
            assertFalse("Invalid code '$code' should return false", result)
        }
    }

    @Test
    fun `getRemainingTime returns value between 0 and 30`() {
        // When
        val remainingTime = totpService.getRemainingTime()

        // Then
        assertTrue("Remaining time should be between 0 and 30", remainingTime in 0..30)
    }

    @Test
    fun `getCycleProgress returns value between 0 and 1`() {
        // When
        val progress = totpService.getCycleProgress()

        // Then
        assertTrue("Progress should be between 0.0 and 1.0", progress in 0.0..1.0)
    }

    @Test
    fun `generateMockTotpCode returns 6-digit string`() {
        // When
        val code = totpService.generateMockTotpCode()

        // Then
        assertTrue("Generated code should be 6 digits", code.length == 6)
        assertTrue("Generated code should contain only digits", code.all { it.isDigit() })
    }

    @Test
    fun `verifyTotpCode with edge case codes`() {
        // Test edge cases
        val edgeCases = listOf(
            "000000" to true,  // All zeros
            "999999" to true,  // All nines
            "123456" to true,  // Sequential
            "654321" to true   // Reverse sequential
        )

        edgeCases.forEach { (code, expectedValid) ->
            val result = totpService.verifyTotpCode(code)
            if (expectedValid) {
                assertTrue("Edge case code '$code' should be valid", result is TotpVerificationResult.Success)
            } else {
                assertTrue("Edge case code '$code' should be invalid", result is TotpVerificationResult.Error)
            }
        }
    }

    @Test
    fun `verifyTotpCode with special characters returns error`() {
        // Given
        val specialCharCodes = listOf(
            "12345-",
            "12345.",
            "12345,",
            "12345:",
            "12345;",
            "12345+",
            "12345=",
            "12345(",
            "12345)",
            "12345[",
            "12345]",
            "12345{",
            "12345}",
            "12345|",
            "12345\\",
            "12345/",
            "12345?",
            "12345<",
            "12345>",
            "12345~",
            "12345`",
            "12345!",
            "12345@",
            "12345#",
            "12345$",
            "12345%",
            "12345^",
            "12345&",
            "12345*"
        )

        // When & Then
        specialCharCodes.forEach { code ->
            val result = totpService.verifyTotpCode(code)
            assertTrue("Code with special character '$code' should return error", result is TotpVerificationResult.Error)
            assertEquals("Code must contain only numbers", (result as TotpVerificationResult.Error).message)
        }
    }

    @Test
    fun `verifyTotpCode with unicode characters returns error`() {
        // Given
        val unicodeCodes = listOf(
            "12345α",  // Greek alpha
            "12345中",  // Chinese character
            "12345ñ",  // Spanish ñ
            "12345é",  // French é
            "12345🚀"  // Emoji
        )

        // When & Then
        unicodeCodes.forEach { code ->
            val result = totpService.verifyTotpCode(code)
            assertTrue("Code with unicode character '$code' should return error", result is TotpVerificationResult.Error)
            assertEquals("Code must contain only numbers", (result as TotpVerificationResult.Error).message)
        }
    }
}
