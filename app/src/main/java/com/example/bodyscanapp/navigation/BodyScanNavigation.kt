package com.example.bodyscanapp.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.bodyscanapp.data.HeightData
import com.example.bodyscanapp.data.MeasurementData
import com.example.bodyscanapp.data.generateMockMeasurements
import com.example.bodyscanapp.ui.screens.HeightInputScreen
import com.example.bodyscanapp.ui.screens.HomeScreen
import com.example.bodyscanapp.ui.screens.ImageCaptureScreen
import com.example.bodyscanapp.ui.screens.ProcessingScreen
import com.example.bodyscanapp.ui.screens.ResultsScreen
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Navigation routes for the Body Scan flow
 */
sealed class BodyScanRoute(val route: String) {
    data object Home : BodyScanRoute("home")
    data object HeightInput : BodyScanRoute("height_input")
    data object ImageCapture : BodyScanRoute("image_capture")
    data object Processing : BodyScanRoute("processing")
    data object Results : BodyScanRoute("results")
}

/**
 * Navigation graph for the Body Scan flow: Home -> Height Input -> Image Capture -> Processing -> Results
 *
 * Features:
 * - Type-safe navigation with sealed routes
 * - Proper back stack management
 * - Edge case handling (failures, cancellations)
 * - Rapid navigation prevention
 * - System back button handling
 *
 * @param navController The navigation controller to manage navigation
 * @param modifier Modifier for the NavHost
 * @param onLogoutClick Callback when user logs out from home screen
 * @param onShowSuccessMessage Callback to show success messages
 * @param username Optional username to display on home screen
 */
@Composable
fun BodyScanNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onLogoutClick: () -> Unit = {},
    onShowSuccessMessage: (String) -> Unit = {},
    username: String? = null
) {
    // Shared state for height data and image data across screens
    var heightData by remember { mutableStateOf<HeightData?>(null) }
    var capturedImageData by remember { mutableStateOf<ByteArray?>(null) }
    var measurementResults by remember { mutableStateOf<MeasurementData?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Track last navigation time to prevent rapid navigation
    var lastNavigationTime by remember { mutableLongStateOf(0L) }
    val navigationDebounceMs = 500L // Prevent navigation within 500ms

    // Track if processing is in progress to prevent back navigation
    var isProcessing by remember { mutableStateOf(false) }

    // Get current route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    /**
     * Safe navigation function with debouncing to prevent rapid navigation
     */
    fun navigateSafely(route: String, popUpToRoute: String? = null, inclusive: Boolean = false) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastNavigationTime < navigationDebounceMs) {
            // Ignore rapid navigation attempts
            return
        }
        lastNavigationTime = currentTime

        try {
            if (popUpToRoute != null) {
                navController.navigate(route) {
                    popUpTo(popUpToRoute) {
                        this.inclusive = inclusive
                    }
                }
            } else {
                navController.navigate(route)
            }
        } catch (e: Exception) {
            // Handle navigation exception gracefully
            android.util.Log.e("BodyScanNavGraph", "Navigation error: ${e.message}", e)
        }
    }

    /**
     * Safe back navigation with debouncing
     */
    fun popBackStackSafely(route: String? = null, inclusive: Boolean = false): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastNavigationTime < navigationDebounceMs) {
            // Ignore rapid navigation attempts
            return false
        }
        lastNavigationTime = currentTime

        return try {
            if (route != null) {
                navController.popBackStack(route, inclusive)
            } else {
                navController.popBackStack()
            }
        } catch (e: Exception) {
            android.util.Log.e("BodyScanNavGraph", "Back navigation error: ${e.message}", e)
            false
        }
    }

    NavHost(
        navController = navController,
        startDestination = BodyScanRoute.Home.route,
        modifier = modifier
    ) {
        // Home Screen
        composable(route = BodyScanRoute.Home.route) {
            // Handle system back button on home screen - prevent going back to auth flow
            BackHandler(enabled = true) {
                // Do nothing or show exit dialog
                // This prevents system back from going back to auth screens
            }

            HomeScreen(
                onLogoutClick = onLogoutClick,
                onNewScanClick = {
                    // Reset all scan data when starting a new scan
                    heightData = null
                    capturedImageData = null
                    measurementResults = null
                    isProcessing = false

                    // Navigate to height input
                    navigateSafely(BodyScanRoute.HeightInput.route)
                },
                onViewHistoryClick = {
                    onShowSuccessMessage("View Scan History clicked - Feature coming soon!")
                },
                onExportScansClick = {
                    onShowSuccessMessage("Export All Scans clicked - Feature coming soon!")
                },
                onProfileClick = {
                    onShowSuccessMessage("Profile clicked - Feature coming soon!")
                },
                username = username
            )
        }

        // Height Input Screen
        composable(route = BodyScanRoute.HeightInput.route) {
            // Handle system back button - allow going back to home
            BackHandler(enabled = true) {
                popBackStackSafely()
            }

            HeightInputScreen(
                onBackClick = {
                    // Navigate back to home
                    popBackStackSafely()
                },
                onProceedClick = { height ->
                    // Store height data and navigate to image capture
                    heightData = height
                    navigateSafely(BodyScanRoute.ImageCapture.route)
                }
            )
        }

        // Image Capture Screen
        composable(route = BodyScanRoute.ImageCapture.route) {
            // Handle system back button - allow going back to height input
            BackHandler(enabled = true) {
                popBackStackSafely()
            }

            ImageCaptureScreen(
                heightData = heightData,
                onBackClick = {
                    // Navigate back to height input
                    popBackStackSafely()
                },
                onCaptureComplete = { imageByteArray ->
                    // Store captured image and navigate to processing
                    capturedImageData = imageByteArray
                    isProcessing = true
                    onShowSuccessMessage("Image captured successfully! Processing...")

                    // Navigate to processing screen with custom navigation builder
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastNavigationTime >= navigationDebounceMs) {
                        lastNavigationTime = currentTime
                        try {
                            navController.navigate(BodyScanRoute.Processing.route) {
                                // Don't allow back to image capture during processing
                                // User must use cancel button
                                popUpTo(BodyScanRoute.ImageCapture.route) {
                                    inclusive = false
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("BodyScanNavGraph", "Navigation error: ${e.message}", e)
                            isProcessing = false
                        }
                    }
                }
            )
        }

        // Processing Screen
        composable(route = BodyScanRoute.Processing.route) {
            // Handle system back button during processing - disable it
            BackHandler(enabled = isProcessing) {
                // Do nothing - prevent back navigation during processing
                // User must use Cancel button
            }

            // Mark as not processing when leaving this screen
            LaunchedEffect(Unit) {
                try {
                    // This coroutine will suspend indefinitely until it is cancelled when
                    // the composable leaves the screen (is disposed).
                    awaitCancellation()
                } finally {
                    // This cleanup block runs when the effect is cancelled.
                    isProcessing = false
                }
            }

            ProcessingScreen(
                imageData = capturedImageData,
                simulateProcessing = true,
                onProcessingComplete = {
                    // Mark processing as complete
                    isProcessing = false

                    // Generate mock measurements (in production, this would be actual processing results)
                    measurementResults = generateMockMeasurements(isSuccessful = true)
                    onShowSuccessMessage("Processing complete! Results ready.")

                    // Navigate to results with custom navigation builder
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastNavigationTime >= navigationDebounceMs) {
                        lastNavigationTime = currentTime
                        try {
                            navController.navigate(BodyScanRoute.Results.route) {
                                // Remove processing from back stack
                                popUpTo(BodyScanRoute.Processing.route) {
                                    inclusive = true
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("BodyScanNavGraph", "Navigation error: ${e.message}", e)
                        }
                    }
                },
                onProcessingFailed = { errorMsg ->
                    // Mark processing as complete (failed)
                    isProcessing = false

                    // On failure, generate error measurement data
                    measurementResults = generateMockMeasurements(isSuccessful = false)

                    // Navigate back to image capture to try again
                    coroutineScope.launch {
                        onShowSuccessMessage("Processing failed: $errorMsg. Please try again.")
                        delay(300) // Small delay to show message
                        popBackStackSafely(BodyScanRoute.ImageCapture.route, inclusive = false)
                    }
                },
                onCancelClick = {
                    // Mark processing as cancelled
                    isProcessing = false

                    // Navigate back to image capture on cancel
                    popBackStackSafely(BodyScanRoute.ImageCapture.route, inclusive = false)
                }
            )
        }

        // Results Screen
        composable(route = BodyScanRoute.Results.route) {
            // Handle system back button - prevent going back to processing
            BackHandler(enabled = true) {
                // Go back to home instead
                popBackStackSafely(BodyScanRoute.Home.route, inclusive = false)
            }

            ResultsScreen(
                measurementData = measurementResults,
                onSaveClick = {
                    // TODO: Implement save to database/cloud storage
                    onShowSuccessMessage("Measurements saved successfully!")

                    // Navigate back to home and clear back stack
                    coroutineScope.launch {
                        delay(300) // Small delay to show message
                        popBackStackSafely(BodyScanRoute.Home.route, inclusive = false)
                    }
                },
                onRecaptureClick = {
                    // Reset scan data
                    heightData = null
                    capturedImageData = null
                    measurementResults = null
                    isProcessing = false

                    // Navigate back to height input to start a new scan
                    // Clear all the way back to home, then navigate to height input
                    coroutineScope.launch {
                        popBackStackSafely(BodyScanRoute.Home.route, inclusive = false)
                        delay(100) // Small delay before next navigation
                        navigateSafely(BodyScanRoute.HeightInput.route)
                    }
                },
                onExportClick = {
                    // TODO: Implement export functionality (PDF, CSV, etc.)
                    onShowSuccessMessage("Export functionality coming soon!")
                }
            )
        }
    }
}
