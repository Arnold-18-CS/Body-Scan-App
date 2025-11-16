package com.example.bodyscanapp

import android.content.Context
import android.content.SharedPreferences
import com.example.bodyscanapp.utils.PerformanceLogger
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * Unit tests for PerformanceLogger
 * 
 * Tests verify:
 * - Action logging functionality
 * - Duration tracking
 * - Navigation logging
 * - Statistics calculation
 * - SharedPreferences integration
 */
class PerformanceLoggerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    private lateinit var performanceLogger: PerformanceLogger

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Mock SharedPreferences behavior
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor)
        `when`(mockSharedPreferences.getInt(anyString(), anyInt())).thenReturn(0)
        
        // Create logger with mocked context
        performanceLogger = PerformanceLogger(mockContext, enableConsoleLogging = false)
    }

    @Test
    fun testLogAction() {
        // Test basic action logging
        performanceLogger.logAction("button_click", "test_button", "test metadata")
        
        // Verify SharedPreferences was called
        verify(mockEditor, atLeastOnce()).putString(anyString(), anyString())
        verify(mockEditor, atLeastOnce()).putInt(anyString(), anyInt())
        verify(mockEditor, atLeastOnce()).apply()
    }

    @Test
    fun testStartAndEndAction() {
        // Start an action
        performanceLogger.startAction("test_action")
        
        // Simulate some work
        Thread.sleep(100)
        
        // End the action
        val duration = performanceLogger.endAction("test_action", "test metadata")
        
        // Verify duration is not null and is reasonable
        assertNotNull("Duration should not be null", duration)
        assertTrue("Duration should be >= 100ms", duration!! >= 100)
        assertTrue("Duration should be < 1000ms", duration < 1000)
    }

    @Test
    fun testEndActionWithoutStart() {
        // Try to end an action that was never started
        val duration = performanceLogger.endAction("non_existent_action")
        
        // Should return null
        assertNull("Duration should be null for non-started action", duration)
    }

    @Test
    fun testLogNavigation() {
        // Test navigation logging
        performanceLogger.logNavigation("ScreenA", "ScreenB")
        
        // Verify SharedPreferences was called
        verify(mockEditor, atLeastOnce()).putString(anyString(), anyString())
        verify(mockEditor, atLeastOnce()).apply()
    }

    @Test
    fun testMarkScreenVisible() {
        // Test marking screen as visible
        performanceLogger.markScreenVisible("TestScreen")
        
        // Should not throw any exception
        // This is a simple test to ensure the method works
        assertTrue("markScreenVisible should complete without error", true)
    }

    @Test
    fun testMultipleActionsSimultaneously() {
        // Start multiple actions
        performanceLogger.startAction("action1")
        Thread.sleep(50)
        performanceLogger.startAction("action2")
        Thread.sleep(50)
        performanceLogger.startAction("action3")
        Thread.sleep(50)
        
        // End them in different order
        val duration2 = performanceLogger.endAction("action2")
        val duration1 = performanceLogger.endAction("action1")
        val duration3 = performanceLogger.endAction("action3")
        
        // All durations should be valid
        assertNotNull(duration1)
        assertNotNull(duration2)
        assertNotNull(duration3)
        
        // Check relative durations (action1 should be longest)
        assertTrue("action1 should have longest duration", duration1!! > duration2!!)
        assertTrue("action1 should have longest duration", duration1 > duration3!!)
    }

    @Test
    fun testClearLogs() {
        // Log some actions
        performanceLogger.logAction("test", "action1")
        performanceLogger.logAction("test", "action2")
        
        // Clear logs
        performanceLogger.clearLogs()
        
        // Verify clear was called
        verify(mockEditor, atLeastOnce()).clear()
        verify(mockEditor, atLeastOnce()).apply()
    }

    @Test
    fun testConsoleLoggingDisabled() {
        // Create logger with console logging disabled
        val logger = PerformanceLogger(mockContext, enableConsoleLogging = false)
        
        // Log an action (should not throw exception)
        logger.logAction("test", "action")
        
        // Should complete without error
        assertTrue("Logging should work with console logging disabled", true)
    }

    @Test
    fun testSharedPrefsLoggingDisabled() {
        // Create logger with SharedPrefs logging disabled
        val logger = PerformanceLogger(mockContext, enableSharedPrefsLogging = false)
        
        // Log an action
        logger.logAction("test", "action")
        
        // SharedPreferences should not be called (beyond initialization)
        verify(mockEditor, never()).putString(contains("log_"), anyString())
    }

    @Test
    fun testActionDurationAccuracy() {
        // Start an action
        val startTime = System.currentTimeMillis()
        performanceLogger.startAction("timing_test")
        
        // Sleep for a known duration
        val expectedDuration = 200L
        Thread.sleep(expectedDuration)
        
        // End the action
        val duration = performanceLogger.endAction("timing_test")
        val endTime = System.currentTimeMillis()
        
        // Check duration accuracy (allow 50ms margin for system overhead)
        assertNotNull(duration)
        assertTrue("Duration should be close to expected", duration!! >= expectedDuration)
        assertTrue("Duration should not exceed wall clock time", duration <= (endTime - startTime) + 10)
    }
}



