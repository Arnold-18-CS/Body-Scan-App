package com.example.bodyscanapp.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for NativeBridge
 * 
 * Tests:
 * - ScanResult data class
 * - Data validation
 * - Edge cases
 */
class NativeBridgeTest {
    
    @Test
    fun `test ScanResult data class creation`() {
        val keypoints3d = FloatArray(135 * 3) { 0f }
        val meshGlb = ByteArray(100) { 0 }
        val measurements = floatArrayOf(80f, 100f, 90f, 60f, 30f, 40f)
        
        val result = NativeBridge.ScanResult(
            keypoints3d = keypoints3d,
            meshGlb = meshGlb,
            measurements = measurements
        )
        
        assertNotNull(result)
        assertEquals(135 * 3, result.keypoints3d.size)
        assertEquals(100, result.meshGlb.size)
        assertEquals(6, result.measurements.size)
    }
    
    @Test
    fun `test ScanResult with empty arrays`() {
        val result = NativeBridge.ScanResult(
            keypoints3d = FloatArray(0),
            meshGlb = ByteArray(0),
            measurements = FloatArray(0)
        )
        
        assertNotNull(result)
        assertEquals(0, result.keypoints3d.size)
        assertEquals(0, result.meshGlb.size)
        assertEquals(0, result.measurements.size)
    }
    
    @Test
    fun `test ScanResult measurements validation`() {
        val keypoints3d = FloatArray(135 * 3) { 0f }
        val meshGlb = ByteArray(100) { 0 }
        
        // Test with valid measurements
        val validMeasurements = floatArrayOf(80f, 100f, 90f)
        val result = NativeBridge.ScanResult(
            keypoints3d = keypoints3d,
            meshGlb = meshGlb,
            measurements = validMeasurements
        )
        
        assertNotNull(result)
        assertEquals(3, result.measurements.size)
        assertEquals(80f, result.measurements[0], 0.01f)
        assertEquals(100f, result.measurements[1], 0.01f)
        assertEquals(90f, result.measurements[2], 0.01f)
    }
}

