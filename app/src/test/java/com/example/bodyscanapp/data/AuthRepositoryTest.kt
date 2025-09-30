package com.example.bodyscanapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AuthRepositoryTest {
    
    private lateinit var authRepository: AuthRepository
    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        authRepository = AuthRepository(context)
        sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        // Clear preferences before each test
        sharedPreferences.edit().clear().apply()
    }
    
    // Authentication Tests
    
    @Test
    fun `authenticate with valid username returns success`() {
        // When
        val result = authRepository.authenticate("admin", "admin123")
        
        // Then
        assertTrue("Valid username authentication should succeed", result is AuthResult.Success)
    }
    
    @Test
    fun `authenticate with valid email returns success`() {
        // When
        val result = authRepository.authenticate("user@example.com", "password123")
        
        // Then
        assertTrue("Valid email authentication should succeed", result is AuthResult.Success)
    }
    
    @Test
    fun `authenticate with invalid username returns error`() {
        // When
        val result = authRepository.authenticate("nonexistent", "password123")
        
        // Then
        assertTrue("Invalid username should return error", result is AuthResult.Error)
        assertEquals("User not found. Please check your username or email", (result as AuthResult.Error).message)
    }
    
    @Test
    fun `authenticate with invalid email returns error`() {
        // When
        val result = authRepository.authenticate("nonexistent@example.com", "password123")
        
        // Then
        assertTrue("Invalid email should return error", result is AuthResult.Error)
        assertEquals("User not found. Please check your username or email", (result as AuthResult.Error).message)
    }
    
    @Test
    fun `authenticate with wrong password returns error`() {
        // When
        val result = authRepository.authenticate("admin", "wrongpassword")
        
        // Then
        assertTrue("Wrong password should return error", result is AuthResult.Error)
        assertEquals("Incorrect password. Please try again", (result as AuthResult.Error).message)
    }
    
    @Test
    fun `authenticate with blank username returns error`() {
        // When
        val result = authRepository.authenticate("", "password123")
        
        // Then
        assertTrue("Blank username should return error", result is AuthResult.Error)
        assertEquals("Please enter your username or email", (result as AuthResult.Error).message)
    }
    
    @Test
    fun `authenticate with blank password returns error`() {
        // When
        val result = authRepository.authenticate("admin", "")
        
        // Then
        assertTrue("Blank password should return error", result is AuthResult.Error)
        assertEquals("Please enter your password", (result as AuthResult.Error).message)
    }
    
    @Test
    fun `authenticate with whitespace username returns error`() {
        // When
        val result = authRepository.authenticate("   ", "password123")
        
        // Then
        assertTrue("Whitespace username should return error", result is AuthResult.Error)
        assertEquals("Please enter your username or email", (result as AuthResult.Error).message)
    }
    
    @Test
    fun `authenticate with whitespace password returns error`() {
        // When
        val result = authRepository.authenticate("admin", "   ")
        
        // Then
        assertTrue("Whitespace password should return error", result is AuthResult.Error)
        assertEquals("Please enter your password", (result as AuthResult.Error).message)
    }
    
    @Test
    fun `authenticate saves login state to preferences`() {
        // Given
        val username = "admin"
        val password = "admin123"
        
        // When
        val result = authRepository.authenticate(username, password)
        
        // Then
        assertTrue("Authentication should succeed", result is AuthResult.Success)
        assertTrue("Should be logged in", authRepository.isLoggedIn())
        assertEquals("Username should be saved", username, authRepository.getCurrentUser())
    }
    
    @Test
    fun `authenticate with email saves email to preferences`() {
        // Given
        val email = "user@example.com"
        val password = "password123"
        
        // When
        val result = authRepository.authenticate(email, password)
        
        // Then
        assertTrue("Authentication should succeed", result is AuthResult.Success)
        assertTrue("Should be logged in", authRepository.isLoggedIn())
        assertEquals("Email should be saved as username", email, authRepository.getCurrentUser())
    }
    
    // Registration Tests
    
    @Test
    fun `register new user returns success`() {
        // When
        val result = authRepository.register("newuser", "newuser@example.com", "newpass123")
        
        // Then
        assertTrue("New user registration should succeed", result is AuthResult.Success)
    }
    
    @Test
    fun `register existing username returns error`() {
        // When
        val result = authRepository.register("admin", "admin@example.com", "admin123")
        
        // Then
        assertTrue("Existing username should return error", result is AuthResult.Error)
        assertEquals("Username already exists. Please choose a different one", (result as AuthResult.Error).message)
    }
    
    @Test
    fun `register existing email returns error`() {
        // When
        val result = authRepository.register("newuser", "user@example.com", "password123")
        
        // Then
        assertTrue("Existing email should return error", result is AuthResult.Error)
        assertEquals("Email already registered. Please use a different email", (result as AuthResult.Error).message)
    }
    
    @Test
    fun `register with blank username returns error`() {
        // When
        val result = authRepository.register("", "newuser@example.com", "newpass123")
        
        // Then
        assertTrue("Blank username should return error", result is AuthResult.Error)
        assertEquals("Please enter a username", (result as AuthResult.Error).message)
    }
    
    @Test
    fun `register with blank email returns error`() {
        // When
        val result = authRepository.register("newuser", "", "newpass123")
        
        // Then
        assertTrue("Blank email should return error", result is AuthResult.Error)
        assertEquals("Please enter an email address", (result as AuthResult.Error).message)
    }
    
    @Test
    fun `register with blank password returns error`() {
        // When
        val result = authRepository.register("newuser", "newuser@example.com", "")
        
        // Then
        assertTrue("Blank password should return error", result is AuthResult.Error)
        assertEquals("Please enter a password", (result as AuthResult.Error).message)
    }
    
    @Test
    fun `register with invalid email format returns error`() {
        // When
        val result = authRepository.register("newuser", "invalid-email", "newpass123")
        
        // Then
        assertTrue("Invalid email should return error", result is AuthResult.Error)
        assertEquals("Please enter a valid email address", (result as AuthResult.Error).message)
    }
    
    @Test
    fun `register with short password returns error`() {
        // When
        val result = authRepository.register("newuser", "newuser@example.com", "12345")
        
        // Then
        assertTrue("Short password should return error", result is AuthResult.Error)
        assertEquals("Password must be at least 6 characters long", (result as AuthResult.Error).message)
    }
    
    @Test
    fun `register saves user data to preferences`() {
        // Given
        val username = "newuser"
        val email = "newuser@example.com"
        val password = "newpass123"
        
        // When
        val result = authRepository.register(username, email, password)
        
        // Then
        assertTrue("Registration should succeed", result is AuthResult.Success)
        assertTrue("Should be logged in after registration", authRepository.isLoggedIn())
        assertEquals("Username should be saved", username, authRepository.getCurrentUser())
    }
    
    // Login State Tests
    
    @Test
    fun `isLoggedIn returns false initially`() {
        // Then
        assertFalse("Should not be logged in initially", authRepository.isLoggedIn())
    }
    
    @Test
    fun `getCurrentUser returns null initially`() {
        // Then
        assertNull("Current user should be null initially", authRepository.getCurrentUser())
    }
    
    @Test
    fun `logout clears all preferences`() {
        // Given
        authRepository.authenticate("admin", "admin123")
        assertTrue("Should be logged in", authRepository.isLoggedIn())
        
        // When
        authRepository.logout()
        
        // Then
        assertFalse("Should not be logged in after logout", authRepository.isLoggedIn())
        assertNull("Current user should be null after logout", authRepository.getCurrentUser())
    }
    
    @Test
    fun `logout multiple times is safe`() {
        // Given
        authRepository.authenticate("admin", "admin123")
        
        // When
        authRepository.logout()
        authRepository.logout()
        authRepository.logout()
        
        // Then
        assertFalse("Should not be logged in after multiple logouts", authRepository.isLoggedIn())
        assertNull("Current user should be null after multiple logouts", authRepository.getCurrentUser())
    }
    
    // Edge Cases
    
    @Test
    fun `authenticate with very long username`() {
        // Given
        val longUsername = "a".repeat(1000)
        
        // When
        val result = authRepository.authenticate(longUsername, "password123")
        
        // Then
        assertTrue("Long username should return error", result is AuthResult.Error)
        assertEquals("User not found. Please check your username or email", (result as AuthResult.Error).message)
    }
    
    @Test
    fun `authenticate with very long password`() {
        // Given
        val longPassword = "a".repeat(1000)
        
        // When
        val result = authRepository.authenticate("admin", longPassword)
        
        // Then
        assertTrue("Long password should return error", result is AuthResult.Error)
        assertEquals("Incorrect password. Please try again", (result as AuthResult.Error).message)
    }
    
    @Test
    fun `register with very long username`() {
        // Given
        val longUsername = "a".repeat(1000)
        
        // When
        val result = authRepository.register(longUsername, "newuser@example.com", "newpass123")
        
        // Then
        assertTrue("Long username registration should succeed", result is AuthResult.Success)
    }
    
    @Test
    fun `register with very long email`() {
        // Given
        val longEmail = "a".repeat(1000) + "@example.com"
        
        // When
        val result = authRepository.register("newuser", longEmail, "newpass123")
        
        // Then
        assertTrue("Long email registration should succeed", result is AuthResult.Success)
    }
    
    @Test
    fun `register with very long password`() {
        // Given
        val longPassword = "a".repeat(1000)
        
        // When
        val result = authRepository.register("newuser", "newuser@example.com", longPassword)
        
        // Then
        assertTrue("Long password registration should succeed", result is AuthResult.Success)
    }
}
