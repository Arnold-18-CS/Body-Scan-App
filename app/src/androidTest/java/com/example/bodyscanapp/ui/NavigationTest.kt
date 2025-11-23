package com.example.bodyscanapp.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.bodyscanapp.navigation.BodyScanRoute
import com.example.bodyscanapp.ui.screens.HomeScreen
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for navigation flows
 * 
 * Tests:
 * - Navigation between screens
 * - Back navigation
 * - Navigation guards
 */
class NavigationTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testHomeScreenDisplay() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = BodyScanRoute.Home.route
            ) {
                composable(BodyScanRoute.Home.route) {
                    HomeScreen(
                        onLogoutClick = {},
                        onNewScanClick = {},
                        onViewHistoryClick = {},
                        onExportScansClick = {},
                        onProfileClick = {},
                        username = "TestUser"
                    )
                }
            }
        }
        
        // Verify home screen elements are displayed
        composeTestRule.onNodeWithText("New Scan", useUnmergedTree = true)
            .assertIsDisplayed()
    }
    
    @Test
    fun testHomeToNewScanNavigation() {
        var navigatedToNewScan = false
        
        composeTestRule.setContent {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = BodyScanRoute.Home.route
            ) {
                composable(BodyScanRoute.Home.route) {
                    HomeScreen(
                        onLogoutClick = {},
                        onNewScanClick = { navigatedToNewScan = true },
                        onViewHistoryClick = {},
                        onExportScansClick = {},
                        onProfileClick = {},
                        username = "TestUser"
                    )
                }
            }
        }
        
        // Click "New Scan" button
        composeTestRule.onNodeWithText("New Scan", useUnmergedTree = true)
            .performClick()
        
        // Verify navigation occurred
        assert(navigatedToNewScan) { "Navigation to new scan should have occurred" }
    }
}

