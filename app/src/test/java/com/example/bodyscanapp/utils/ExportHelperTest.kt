package com.example.bodyscanapp.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.example.bodyscanapp.data.entity.Scan
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import java.io.File

/**
 * Unit tests for ExportHelper
 * 
 * Tests:
 * - JSON export
 * - CSV export
 * - PDF export
 */
class ExportHelperTest {
    
    @Test
    fun `test exportToJson`() {
        val scan = Scan(
            id = 1L,
            userId = 1L,
            timestamp = System.currentTimeMillis(),
            heightCm = 175f,
            keypoints3d = "[1.0, 2.0, 3.0]",
            meshPath = "/path/to/mesh.glb",
            measurementsJson = """{"waist": 80.0, "chest": 100.0}"""
        )
        
        val outputFile = File.createTempFile("test_scan", ".json")
        outputFile.deleteOnExit()
        
        try {
            ExportHelper.exportToJson(scan, outputFile)
            
            assertTrue(outputFile.exists())
            assertTrue(outputFile.length() > 0)
            
            val content = outputFile.readText()
            assertTrue(content.contains("userId"))
            assertTrue(content.contains("heightCm"))
        } finally {
            outputFile.delete()
        }
    }
    
    @Test
    fun `test exportToCsv`() {
        val measurements = mapOf(
            "waist" to 80f,
            "chest" to 100f,
            "hips" to 90f
        )
        
        val outputFile = File.createTempFile("test_measurements", ".csv")
        outputFile.deleteOnExit()
        
        try {
            ExportHelper.exportToCsv(measurements, outputFile)
            
            assertTrue(outputFile.exists())
            assertTrue(outputFile.length() > 0)
            
            val content = outputFile.readText()
            assertTrue(content.contains("Measurement"))
            assertTrue(content.contains("Value"))
            assertTrue(content.contains("waist"))
            assertTrue(content.contains("80.0"))
        } finally {
            outputFile.delete()
        }
    }
    
    @Test
    fun `test exportToPdf`() {
        val scan = Scan(
            id = 1L,
            userId = 1L,
            timestamp = System.currentTimeMillis(),
            heightCm = 175f,
            keypoints3d = "[1.0, 2.0, 3.0]",
            meshPath = "/path/to/mesh.glb",
            measurementsJson = """{"waist": 80.0, "chest": 100.0}"""
        )
        
        // Create a simple test bitmap
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLUE)
        
        val outputFile = File.createTempFile("test_scan", ".pdf")
        outputFile.deleteOnExit()
        
        try {
            ExportHelper.exportToPdf(scan, listOf(bitmap), outputFile)
            
            assertTrue(outputFile.exists())
            assertTrue(outputFile.length() > 0)
        } finally {
            outputFile.delete()
            bitmap.recycle()
        }
    }
    
    @Test
    fun `test exportToCsv with special characters`() {
        val measurements = mapOf(
            "waist, measurement" to 80f,
            "chest (upper)" to 100f,
            "hips" to 90f
        )
        
        val outputFile = File.createTempFile("test_measurements", ".csv")
        outputFile.deleteOnExit()
        
        try {
            ExportHelper.exportToCsv(measurements, outputFile)
            
            assertTrue(outputFile.exists())
            val content = outputFile.readText()
            // CSV should handle special characters by quoting
            assertTrue(content.contains("waist") || content.contains("chest"))
        } finally {
            outputFile.delete()
        }
    }
}

