package com.example.bodyscanapp.data

import android.content.Context
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
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        authRepository = AuthRepository(context)
    }
    
    @Test
    fun `authenticate with valid credentials returns success`() {
        val result = authRepository.authenticate("admin", "admin123")
        assertTrue(result is AuthResult.Success)
    }
    
    @Test
    fun `authenticate with invalid credentials returns error`() {
        val result = authRepository.authenticate("admin", "wrongpassword")
        assertTrue(result is AuthResult.Error)
        assertEquals("Invalid credentials. Please try again.", (result as AuthResult.Error).message)
    }
    
    @Test
    fun `authenticate with email returns success`() {
        val result = authRepository.authenticate("user@example.com", "password123")
        assertTrue(result is AuthResult.Success)
    }
    
    @Test
    fun `register new user returns success`() {
        val result = authRepository.register("newuser", "newuser@example.com", "newpass123")
        assertTrue(result is AuthResult.Success)
    }
    
    @Test
    fun `register existing user returns error`() {
        val result = authRepository.register("admin", "admin@example.com", "admin123")
        assertTrue(result is AuthResult.Error)
        assertEquals("User already exists. Please use different credentials.", (result as AuthResult.Error).message)
    }
}
