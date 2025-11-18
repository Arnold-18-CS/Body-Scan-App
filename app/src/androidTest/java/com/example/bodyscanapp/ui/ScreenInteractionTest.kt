package com.example.bodyscanapp.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.bodyscanapp.ui.screens.Result3DScreen
import com.example.bodyscanapp.utils.NativeBridge
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for screen interactions
 * 
 * Tests:
 * - Button clicks
 * - Dialog displays
 * - Error handling
 */
class ScreenInteractionTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testResult3DScreenDisplay() {
        val mockScanResult = NativeBridge.ScanResult(
            keypoints3d = FloatArray(135 * 3) { 0f },
            meshGlb = ByteArray(100) { 0 },
            measurements = floatArrayOf(80f, 100f, 90f, 60f, 30f, 40f)
        )
        
        composeTestRule.setContent {
            Result3DScreen(
                scanResult = mockScanResult,
                capturedImages = listOf(ByteArray(100), ByteArray(100), ByteArray(100)),
                imageWidths = listOf(640, 640, 640),
                imageHeights = listOf(480, 480, 480),
                userHeightCm = 175f,
                onBackClick = {},
                onSaveClick = {},
                onExportClick = {},
                onNewScanClick = {},
                onShareClick = {},
                onShowSuccessMessage = {},
                onShowErrorMessage = {}
            )
        }
        
        // Verify screen elements are displayed
        composeTestRule.onNodeWithText("3D Scan Results", useUnmergedTree = true)
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Measurements", useUnmergedTree = true)
            .assertIsDisplayed()
    }
    
    @Test
    fun testResult3DScreenSaveButton() {
        val mockScanResult = NativeBridge.ScanResult(
            keypoints3d = FloatArray(135 * 3) { 0f },
            meshGlb = ByteArray(100) { 0 },
            measurements = floatArrayOf(80f, 100f, 90f)
        )
        
        composeTestRule.setContent {
            Result3DScreen(
                scanResult = mockScanResult,
                capturedImages = listOf(ByteArray(100), ByteArray(100), ByteArray(100)),
                imageWidths = listOf(640, 640, 640),
                imageHeights = listOf(480, 480, 480),
                userHeightCm = 175f,
                onBackClick = {},
                onSaveClick = {},
                onExportClick = {},
                onNewScanClick = {},
                onShareClick = {},
                onShowSuccessMessage = {},
                onShowErrorMessage = {}
            )
        }
        
        // Verify Save button exists and is clickable
        composeTestRule.onNodeWithText("Save", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()
        
        // After clicking, a dialog should appear (SaveLocationDialog)
        // Note: Dialog may not be immediately visible due to async state
    }
    
    @Test
    fun testResult3DScreenExportButton() {
        val mockScanResult = NativeBridge.ScanResult(
            keypoints3d = FloatArray(135 * 3) { 0f },
            meshGlb = ByteArray(100) { 0 },
            measurements = floatArrayOf(80f, 100f, 90f)
        )
        
        composeTestRule.setContent {
            Result3DScreen(
                scanResult = mockScanResult,
                capturedImages = listOf(ByteArray(100), ByteArray(100), ByteArray(100)),
                imageWidths = listOf(640, 640, 640),
                imageHeights = listOf(480, 480, 480),
                userHeightCm = 175f,
                onBackClick = {},
                onSaveClick = {},
                onExportClick = {},
                onNewScanClick = {},
                onShareClick = {},
                onShowSuccessMessage = {},
                onShowErrorMessage = {}
            )
        }
        
        // Verify Export button exists
        composeTestRule.onNodeWithText("Export", useUnmergedTree = true)
            .assertIsDisplayed()
    }
    
    @Test
    fun testResult3DScreenBackButton() {
        val mockScanResult = NativeBridge.ScanResult(
            keypoints3d = FloatArray(135 * 3) { 0f },
            meshGlb = ByteArray(100) { 0 },
            measurements = floatArrayOf(80f, 100f, 90f)
        )
        
        composeTestRule.setContent {
            Result3DScreen(
                scanResult = mockScanResult,
                capturedImages = listOf(ByteArray(100), ByteArray(100), ByteArray(100)),
                imageWidths = listOf(640, 640, 640),
                imageHeights = listOf(480, 480, 480),
                userHeightCm = 175f,
                onBackClick = {},
                onSaveClick = {},
                onExportClick = {},
                onNewScanClick = {},
                onShareClick = {},
                onShowSuccessMessage = {},
                onShowErrorMessage = {}
            )
        }
        
        // Verify back button exists
        composeTestRule.onNodeWithContentDescription("Back", useUnmergedTree = true)
            .assertIsDisplayed()
    }
    
    @Test
    fun testResult3DScreenWithoutScanResult() {
        composeTestRule.setContent {
            Result3DScreen(
                scanResult = null,
                capturedImages = emptyList(),
                imageWidths = emptyList(),
                imageHeights = emptyList(),
                userHeightCm = 0f,
                onBackClick = {},
                onSaveClick = {},
                onExportClick = {},
                onNewScanClick = {},
                onShareClick = {},
                onShowSuccessMessage = {},
                onShowErrorMessage = {}
            )
        }
        
        // Verify screen still displays (with empty state)
        composeTestRule.onNodeWithText("3D Scan Results", useUnmergedTree = true)
            .assertIsDisplayed()
    }
}

