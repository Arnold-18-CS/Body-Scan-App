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
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.bodyscanapp.data.HeightData
import com.example.bodyscanapp.data.MeasurementData
import com.example.bodyscanapp.data.generateMockMeasurements
import com.example.bodyscanapp.ui.screens.CaptureSequenceScreen
import com.example.bodyscanapp.ui.screens.CapturedImageData
import com.example.bodyscanapp.ui.screens.HeightInputScreen
import com.example.bodyscanapp.ui.screens.HomeScreen
import com.example.bodyscanapp.ui.screens.ProcessingScreen
import com.example.bodyscanapp.ui.screens.Result3DScreen
import com.example.bodyscanapp.utils.NativeBridge
import com.example.bodyscanapp.utils.PerformanceLogger
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Navigation routes for the Body Scan flow
 */
sealed class BodyScanRoute(val route: String) {
    data object Home : BodyScanRoute("home")
    data object HeightInput : BodyScanRoute("height_input")
    data object CaptureSequence : BodyScanRoute("capture_sequence")
    data object Processing : BodyScanRoute("processing")
    data object Result3D : BodyScanRoute("result_3d")
    // Legacy routes (deprecated, kept for backward compatibility)
    data object ImageCapture : BodyScanRoute("image_capture")
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
    var capturedImagesData by remember { mutableStateOf<List<CapturedImageData>>(emptyList()) }
    var scanResult by remember { mutableStateOf<NativeBridge.ScanResult?>(null) }
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
    
    // Initialize PerformanceLogger
    val context = LocalContext.current
    val performanceLogger = remember { PerformanceLogger.getInstance(context) }
    
    // Track previous route for navigation logging
    var previousRoute by remember { mutableStateOf<String?>(null) }

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
            // Log navigation before it happens
            val from = currentRoute ?: "unknown"
            performanceLogger.logNavigation(from, route)
            
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

    // Track navigation changes and log screen visibility
    LaunchedEffect(currentRoute) {
        currentRoute?.let { route ->
            performanceLogger.markScreenVisible(route)
            previousRoute = route
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
                    capturedImagesData = emptyList()
                    scanResult = null
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
                    // Store height data and navigate to capture sequence
                    heightData = height
                    navigateSafely(BodyScanRoute.CaptureSequence.route)
                }
            )
        }

        // Capture Sequence Screen (NEW - replaces ImageCapture)
        composable(route = BodyScanRoute.CaptureSequence.route) {
            // Handle system back button - allow going back to height input
            BackHandler(enabled = true) {
                popBackStackSafely()
            }

            CaptureSequenceScreen(
                heightData = heightData,
                onBackClick = {
                    // Navigate back to height input
                    popBackStackSafely()
                },
                onCaptureComplete = { capturedImages, userHeightCm ->
                    // Store captured images and navigate to processing
                    capturedImagesData = capturedImages
                    isProcessing = true
                    onShowSuccessMessage("All photos captured! Processing...")

                    // Navigate to processing screen
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastNavigationTime >= navigationDebounceMs) {
                        lastNavigationTime = currentTime
                        try {
                            navController.navigate(BodyScanRoute.Processing.route) {
                                // Don't allow back to capture sequence during processing
                                popUpTo(BodyScanRoute.CaptureSequence.route) {
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

        // Legacy Image Capture Screen (kept for backward compatibility)
        composable(route = BodyScanRoute.ImageCapture.route) {
            // Handle system back button - allow going back to height input
            BackHandler(enabled = true) {
                popBackStackSafely()
            }

            // Redirect to CaptureSequence for now
            // TODO: Remove this route in future versions
            LaunchedEffect(Unit) {
                navigateSafely(BodyScanRoute.CaptureSequence.route)
            }
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
                capturedImages = capturedImagesData.map { it.imageBytes },
                imageWidths = capturedImagesData.map { it.width },
                imageHeights = capturedImagesData.map { it.height },
                userHeightCm = heightData?.toCentimeters() ?: 0f,
                simulateProcessing = false, // Use real NativeBridge processing
                onProcessingComplete = { result ->
                    // Mark processing as complete
                    isProcessing = false
                    scanResult = result

                    onShowSuccessMessage("Processing complete! Results ready.")

                    // Navigate to Result3D screen
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastNavigationTime >= navigationDebounceMs) {
                        lastNavigationTime = currentTime
                        try {
                            navController.navigate(BodyScanRoute.Result3D.route) {
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

                    // Navigate back to capture sequence to try again
                    coroutineScope.launch {
                        onShowSuccessMessage("Processing failed: $errorMsg. Please try again.")
                        delay(300) // Small delay to show message
                        popBackStackSafely(BodyScanRoute.CaptureSequence.route, inclusive = false)
                    }
                },
                onCancelClick = {
                    // Mark processing as cancelled
                    isProcessing = false

                    // Navigate back to capture sequence on cancel
                    popBackStackSafely(BodyScanRoute.CaptureSequence.route, inclusive = false)
                }
            )
        }

        // Result3D Screen (NEW - replaces Results)
        composable(route = BodyScanRoute.Result3D.route) {
            // Handle system back button - prevent going back to processing
            BackHandler(enabled = true) {
                // Go back to home instead
                popBackStackSafely(BodyScanRoute.Home.route, inclusive = false)
            }

            Result3DScreen(
                scanResult = scanResult,
                capturedImages = capturedImagesData.map { it.imageBytes },
                imageWidths = capturedImagesData.map { it.width },
                imageHeights = capturedImagesData.map { it.height },
                userHeightCm = heightData?.toCentimeters() ?: 0f,
                onBackClick = {
                    // Go back to home
                    popBackStackSafely(BodyScanRoute.Home.route, inclusive = false)
                },
                onSaveClick = {
                    // Save is handled internally by Result3DScreen via ViewModel
                    // This callback is kept for backward compatibility but not used
                },
                onExportClick = {
                    // Export is handled internally by Result3DScreen via ViewModel
                    // This callback is kept for backward compatibility but not used
                },
                onNewScanClick = {
                    // Reset scan data
                    heightData = null
                    capturedImageData = null
                    capturedImagesData = emptyList()
                    scanResult = null
                    measurementResults = null
                    isProcessing = false

                    // Navigate back to height input to start a new scan
                    coroutineScope.launch {
                        popBackStackSafely(BodyScanRoute.Home.route, inclusive = false)
                        delay(100) // Small delay before next navigation
                        navigateSafely(BodyScanRoute.HeightInput.route)
                    }
                },
                onShareClick = {
                    // TODO: Implement share functionality
                    onShowSuccessMessage("Share functionality coming soon!")
                },
                onShowSuccessMessage = onShowSuccessMessage,
                onShowErrorMessage = { errorMsg ->
                    onShowSuccessMessage("Error: $errorMsg")
                }
            )
        }

        // Legacy Results Screen (kept for backward compatibility)
        composable(route = BodyScanRoute.Results.route) {
            // Redirect to Result3D for now
            // TODO: Remove this route in future versions
            LaunchedEffect(Unit) {
                navigateSafely(BodyScanRoute.Result3D.route)
            }
        }
    }
}
