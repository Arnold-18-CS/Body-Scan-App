package com.example.bodyscanapp.utils

import org.junit.Assert.*
import org.junit.Test

class ValidationUtilsTest {

    @Test
    fun `isValidEmail with valid email returns true`() {
        // Given
        val validEmails = listOf(
            "test@example.com",
            "user.name@domain.co.uk",
            "user+tag@example.org",
            "test123@test-domain.com",
            "a@b.co"
        )

        // When & Then
        validEmails.forEach { email ->
            assertTrue("Email '$email' should be valid", ValidationUtils.isValidEmail(email))
        }
    }

    @Test
    fun `isValidEmail with invalid email returns false`() {
        // Given
        val invalidEmails = listOf(
            "",
            " ",
            "invalid-email",
            "@example.com",
            "test@",
            "test..test@example.com",
            "test@.com",
            "test@example.",
            "test@example..com",
            "test@example.com.",
            "test@example..com"
        )

        // When & Then
        invalidEmails.forEach { email ->
            assertFalse("Email '$email' should be invalid", ValidationUtils.isValidEmail(email))
        }
    }

    @Test
    fun `isValidUsername with valid username returns true`() {
        // Given
        val validUsernames = listOf(
            "user",
            "test123",
            "user_name",
            "user-name",
            "a" + "b".repeat(100) // 101 characters
        )

        // When & Then
        validUsernames.forEach { username ->
            assertTrue("Username '$username' should be valid", ValidationUtils.isValidUsername(username))
        }
    }

    @Test
    fun `isValidUsername with invalid username returns false`() {
        // Given
        val invalidUsernames = listOf(
            "",
            " ",
            "ab", // less than 3 characters
            "a",
            "  " // only spaces
        )

        // When & Then
        invalidUsernames.forEach { username ->
            assertFalse("Username '$username' should be invalid", ValidationUtils.isValidUsername(username))
        }
    }

    @Test
    fun `isValidPassword with valid password returns true`() {
        // Given
        val validPasswords = listOf(
            "123456", // exactly 6 characters
            "password123",
            "P@ssw0rd",
            "a" + "b".repeat(100) // 101 characters
        )

        // When & Then
        validPasswords.forEach { password ->
            assertTrue("Password should be valid", ValidationUtils.isValidPassword(password))
        }
    }

    @Test
    fun `isValidPassword with invalid password returns false`() {
        // Given
        val invalidPasswords = listOf(
            "",
            " ",
            "12345", // less than 6 characters
            "a",
            "  " // only spaces
        )

        // When & Then
        invalidPasswords.forEach { password ->
            assertFalse("Password should be invalid", ValidationUtils.isValidPassword(password))
        }
    }

    @Test
    fun `validateLoginInput with valid email returns success`() {
        // Given
        val email = "test@example.com"
        val password = "password123"

        // When
        val result = ValidationUtils.validateLoginInput(email, password)

        // Then
        assertTrue("Login validation should succeed", result is ValidationResult.Success)
    }

    @Test
    fun `validateLoginInput with valid username returns success`() {
        // Given
        val username = "testuser"
        val password = "password123"

        // When
        val result = ValidationUtils.validateLoginInput(username, password)

        // Then
        assertTrue("Login validation should succeed", result is ValidationResult.Success)
    }

    @Test
    fun `validateLoginInput with blank emailOrUsername returns error`() {
        // Given
        val emailOrUsername = ""
        val password = "password123"

        // When
        val result = ValidationUtils.validateLoginInput(emailOrUsername, password)

        // Then
        assertTrue("Should return error", result is ValidationResult.Error)
        assertEquals("Please enter your email or username", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateLoginInput with blank password returns error`() {
        // Given
        val emailOrUsername = "test@example.com"
        val password = ""

        // When
        val result = ValidationUtils.validateLoginInput(emailOrUsername, password)

        // Then
        assertTrue("Should return error", result is ValidationResult.Error)
        assertEquals("Please enter your password", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateLoginInput with invalid email format returns error`() {
        // Given
        val emailOrUsername = "invalid-email"
        val password = "password123"

        // When
        val result = ValidationUtils.validateLoginInput(emailOrUsername, password)

        // Then
        assertTrue("Should return error", result is ValidationResult.Error)
        assertEquals("Please enter a valid email address", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateLoginInput with short username returns error`() {
        // Given
        val emailOrUsername = "ab"
        val password = "password123"

        // When
        val result = ValidationUtils.validateLoginInput(emailOrUsername, password)

        // Then
        assertTrue("Should return error", result is ValidationResult.Error)
        assertEquals("Username must be at least 3 characters", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateLoginInput with short password returns error`() {
        // Given
        val emailOrUsername = "test@example.com"
        val password = "12345"

        // When
        val result = ValidationUtils.validateLoginInput(emailOrUsername, password)

        // Then
        assertTrue("Should return error", result is ValidationResult.Error)
        assertEquals("Password must be at least 6 characters", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateRegistrationInput with valid input returns success`() {
        // Given
        val username = "testuser"
        val email = "test@example.com"
        val password = "password123"
        val confirmPassword = "password123"

        // When
        val result = ValidationUtils.validateRegistrationInput(username, email, password, confirmPassword)

        // Then
        assertTrue("Registration validation should succeed", result is ValidationResult.Success)
    }

    @Test
    fun `validateRegistrationInput with blank username returns error`() {
        // Given
        val username = ""
        val email = "test@example.com"
        val password = "password123"
        val confirmPassword = "password123"

        // When
        val result = ValidationUtils.validateRegistrationInput(username, email, password, confirmPassword)

        // Then
        assertTrue("Should return error", result is ValidationResult.Error)
        assertEquals("Please enter a username", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateRegistrationInput with blank email returns error`() {
        // Given
        val username = "testuser"
        val email = ""
        val password = "password123"
        val confirmPassword = "password123"

        // When
        val result = ValidationUtils.validateRegistrationInput(username, email, password, confirmPassword)

        // Then
        assertTrue("Should return error", result is ValidationResult.Error)
        assertEquals("Please enter an email address", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateRegistrationInput with blank password returns error`() {
        // Given
        val username = "testuser"
        val email = "test@example.com"
        val password = ""
        val confirmPassword = "password123"

        // When
        val result = ValidationUtils.validateRegistrationInput(username, email, password, confirmPassword)

        // Then
        assertTrue("Should return error", result is ValidationResult.Error)
        assertEquals("Please enter a password", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateRegistrationInput with blank confirmPassword returns error`() {
        // Given
        val username = "testuser"
        val email = "test@example.com"
        val password = "password123"
        val confirmPassword = ""

        // When
        val result = ValidationUtils.validateRegistrationInput(username, email, password, confirmPassword)

        // Then
        assertTrue("Should return error", result is ValidationResult.Error)
        assertEquals("Please confirm your password", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateRegistrationInput with invalid email format returns error`() {
        // Given
        val username = "testuser"
        val email = "invalid-email"
        val password = "password123"
        val confirmPassword = "password123"

        // When
        val result = ValidationUtils.validateRegistrationInput(username, email, password, confirmPassword)

        // Then
        assertTrue("Should return error", result is ValidationResult.Error)
        assertEquals("Please enter a valid email address", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateRegistrationInput with short username returns error`() {
        // Given
        val username = "ab"
        val email = "test@example.com"
        val password = "password123"
        val confirmPassword = "password123"

        // When
        val result = ValidationUtils.validateRegistrationInput(username, email, password, confirmPassword)

        // Then
        assertTrue("Should return error", result is ValidationResult.Error)
        assertEquals("Username must be at least 3 characters", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateRegistrationInput with short password returns error`() {
        // Given
        val username = "testuser"
        val email = "test@example.com"
        val password = "12345"
        val confirmPassword = "12345"

        // When
        val result = ValidationUtils.validateRegistrationInput(username, email, password, confirmPassword)

        // Then
        assertTrue("Should return error", result is ValidationResult.Error)
        assertEquals("Password must be at least 6 characters", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateRegistrationInput with mismatched passwords returns error`() {
        // Given
        val username = "testuser"
        val email = "test@example.com"
        val password = "password123"
        val confirmPassword = "different123"

        // When
        val result = ValidationUtils.validateRegistrationInput(username, email, password, confirmPassword)

        // Then
        assertTrue("Should return error", result is ValidationResult.Error)
        assertEquals("Passwords do not match", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateRegistrationInput with multiple validation errors returns first error`() {
        // Given
        val username = "" // blank username
        val email = "invalid-email" // invalid email
        val password = "123" // short password
        val confirmPassword = "different" // mismatched password

        // When
        val result = ValidationUtils.validateRegistrationInput(username, email, password, confirmPassword)

        // Then
        assertTrue("Should return error", result is ValidationResult.Error)
        assertEquals("Please enter a username", (result as ValidationResult.Error).message)
    }
}

