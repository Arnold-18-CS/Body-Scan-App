package com.example.bodyscanapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bodyscanapp.R
import com.example.bodyscanapp.data.BiometricAuthManager
import com.example.bodyscanapp.data.BiometricAuthStatus
import com.example.bodyscanapp.data.BiometricAuthViewModel
import com.example.bodyscanapp.data.BiometricAuthViewModelFactory
import com.example.bodyscanapp.ui.theme.BodyScanAppTheme

/**
 * BiometricAuthScreen
 * 
 * Modern biometric authentication screen with Windows login-like styling.
 * Handles fingerprint and face recognition authentication during session persistence.
 * 
 * Features:
 * - Automatic biometric prompt on screen load
 * - Visual feedback for authentication states
 * - Fallback to TOTP option
 * - Error handling with user-friendly messages
 * - Retry mechanism for failed attempts
 * 
 * Note: Session verification is handled by the parent (MainActivity) in the onAuthSuccess callback.
 * 
 * @param email User's email address for display
 * @param onAuthSuccess Callback invoked when biometric authentication succeeds
 * @param onFallbackToTOTP Callback to navigate to TOTP screen
 * @param modifier Optional modifier for styling
 */
@Composable
fun BiometricAuthScreen(
    email: String,
    onAuthSuccess: () -> Unit,
    onFallbackToTOTP: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val biometricAuthManager = remember { BiometricAuthManager(context) }
    val viewModel: BiometricAuthViewModel = viewModel(
        factory = BiometricAuthViewModelFactory(biometricAuthManager)
    )
    val uiState by viewModel.uiState.collectAsState()
    
    // Get activity context (required for biometric prompt)
    // MainActivity extends AppCompatActivity which extends FragmentActivity
    val activity = context as? FragmentActivity
    
    // If not a FragmentActivity, show error and fallback to TOTP
    if (activity == null) {
        // Show error state
        BiometricAuthContent(
            email = email,
            uiState = com.example.bodyscanapp.data.BiometricAuthUiState(
                isAuthenticated = false,
                isAuthenticating = false,
                isBiometricAvailable = false,
                biometricStatus = BiometricAuthStatus.ERROR_UNKNOWN,
                errorMessage = "Biometric authentication not available. Context is not FragmentActivity."
            ),
            onAuthenticateClick = {},
            onFallbackToTOTP = onFallbackToTOTP,
            modifier = modifier
        )
        return
    }
    
    // Automatically trigger biometric prompt when screen loads (only if biometric is available)
    var hasAutoTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isBiometricAvailable, hasAutoTriggered) {
        if (uiState.isBiometricAvailable && !hasAutoTriggered && !uiState.isAuthenticating && !uiState.isAuthenticated) {
            hasAutoTriggered = true
            viewModel.authenticate(activity)
        }
    }
    
    // Handle successful authentication
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onAuthSuccess()
        }
    }
    
    // Reset authentication state when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetAuthState()
        }
    }
    
    BiometricAuthContent(
        email = email,
        uiState = uiState,
        onAuthenticateClick = { viewModel.authenticate(activity) },
        onFallbackToTOTP = onFallbackToTOTP,
        modifier = modifier
    )
}

/**
 * BiometricAuthContent
 * 
 * Presentational component that displays the biometric authentication UI.
 * Separated from the screen component for easier testing and preview.
 */
@Composable
private fun BiometricAuthContent(
    email: String,
    uiState: com.example.bodyscanapp.data.BiometricAuthUiState,
    onAuthenticateClick: () -> Unit,
    onFallbackToTOTP: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        // Logo
        Image(
            painter = painterResource(id = R.drawable.logo_body_scan),
            contentDescription = "Body Scan Logo",
            modifier = Modifier
                .size(280.dp)
                .padding(bottom = 32.dp)
        )
        
        // Welcome back message
        Text(
            text = "Welcome back",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Email display
        Text(
            text = email,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // Main authentication card
        BiometricAuthCard(
            uiState = uiState,
            onAuthenticateClick = onAuthenticateClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )
        
        // Error message display
        AnimatedVisibility(
            visible = uiState.errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ErrorMessageCard(
                message = uiState.errorMessage ?: "",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }
        
        // Fallback to TOTP button
        if (uiState.isBiometricAvailable) {
            TextButton(
                onClick = onFallbackToTOTP,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 4.dp)
                )
                Text(
                    text = "Use 2FA code instead",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            // If biometric not available, show automatic fallback message
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Biometric authentication is not available. Please use 2FA code.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            Button(
                onClick = onFallbackToTOTP,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(56.dp)
            ) {
                Text("Continue with 2FA Code")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Security message
        Text(
            text = "Your biometric data stays on your device and is never sent to our servers.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

/**
 * BiometricAuthCard
 * 
 * Main card containing the biometric authentication UI.
 * Shows appropriate icon, message, and action button based on current state.
 */
@Composable
private fun BiometricAuthCard(
    uiState: com.example.bodyscanapp.data.BiometricAuthUiState,
    onAuthenticateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Biometric icon with animation
            BiometricIcon(
                isAuthenticating = uiState.isAuthenticating,
                isAuthenticated = uiState.isAuthenticated,
                hasError = uiState.errorMessage != null,
                isBiometricAvailable = uiState.isBiometricAvailable
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Status text
            Text(
                text = when {
                    uiState.isAuthenticated -> "Authentication successful!"
                    uiState.isAuthenticating -> "Authenticating..."
                    uiState.errorMessage != null -> "Authentication failed"
                    !uiState.isBiometricAvailable -> "Biometric unavailable"
                    else -> "Verify it's you"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    uiState.isAuthenticated -> Color(0xFF4CAF50)
                    uiState.errorMessage != null -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Instruction text
            Text(
                text = when {
                    uiState.isAuthenticated -> "Redirecting to home..."
                    uiState.isAuthenticating -> "Place your finger on the sensor or look at the camera"
                    uiState.errorMessage != null -> "Please try again or use 2FA code"
                    !uiState.isBiometricAvailable -> getBiometricUnavailableMessage(uiState.biometricStatus)
                    else -> "Use fingerprint or face recognition to continue"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action button
            if (uiState.isBiometricAvailable && !uiState.isAuthenticated) {
                Button(
                    onClick = onAuthenticateClick,
                    enabled = !uiState.isAuthenticating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (uiState.isAuthenticating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (uiState.errorMessage != null) "Try Again" else "Authenticate",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * BiometricIcon
 * 
 * Animated icon that shows the appropriate biometric symbol based on state.
 * Includes pulse animation during authentication and color changes for states.
 */
@Composable
private fun BiometricIcon(
    isAuthenticating: Boolean,
    isAuthenticated: Boolean,
    hasError: Boolean,
    isBiometricAvailable: Boolean,
    modifier: Modifier = Modifier
) {
    // Pulse animation during authentication
    val scale by animateFloatAsState(
        targetValue = if (isAuthenticating) 1.1f else 1f,
        animationSpec = tween(durationMillis = 600),
        label = "pulse"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isAuthenticating) 0.7f else 1f,
        animationSpec = tween(durationMillis = 600),
        label = "alpha"
    )
    
    Box(
        modifier = modifier
            .size(120.dp)
            .scale(scale)
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        // Background circle
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = when {
                        isAuthenticated -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                        hasError -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        isAuthenticating -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else -> MaterialTheme.colorScheme.primaryContainer
                    },
                    shape = CircleShape
                )
                .border(
                    width = 2.dp,
                    color = when {
                        isAuthenticated -> Color(0xFF4CAF50)
                        hasError -> MaterialTheme.colorScheme.error
                        isAuthenticating -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    },
                    shape = CircleShape
                )
        )
        
        // Icon
        when {
            isAuthenticated -> {
                // Success icon (checkmark styled)
                Text(
                    text = "✓",
                    fontSize = 64.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
            }
            hasError -> {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(56.dp)
                )
            }
            !isBiometricAvailable -> {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Unavailable",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(56.dp)
                )
            }
            else -> {
                // Show fingerprint icon (primary biometric icon)
                Image(
                    painter = painterResource(id = R.drawable.face_id),
                    contentDescription = "Biometric Authentication",
                    modifier = Modifier.size(64.dp),
                    alpha = if (isAuthenticating) 0.6f else 1f
                )
            }
        }
    }
}

/**
 * ErrorMessageCard
 * 
 * Displays error messages in a styled card with icon.
 */
@Composable
private fun ErrorMessageCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

/**
 * Get user-friendly message for biometric unavailability reasons
 */
private fun getBiometricUnavailableMessage(status: BiometricAuthStatus): String {
    return when (status) {
        BiometricAuthStatus.ERROR_NO_HARDWARE -> 
            "This device doesn't support biometric authentication"
        BiometricAuthStatus.ERROR_HW_UNAVAILABLE -> 
            "Biometric hardware is temporarily unavailable"
        BiometricAuthStatus.ERROR_NONE_ENROLLED -> 
            "No biometrics enrolled. Please set up fingerprint or face recognition in device settings"
        else -> 
            "Biometric authentication is not available"
    }
}

// ========== Preview Section ==========

@Preview(showBackground = true)
@Composable
private fun BiometricAuthScreenPreview() {
    BodyScanAppTheme {
        BiometricAuthContent(
            email = "user@example.com",
            uiState = com.example.bodyscanapp.data.BiometricAuthUiState(
                isAuthenticated = false,
                isAuthenticating = false,
                isBiometricAvailable = true,
                biometricStatus = BiometricAuthStatus.SUCCESS,
                errorMessage = null
            ),
            onAuthenticateClick = {},
            onFallbackToTOTP = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BiometricAuthScreenAuthenticatingPreview() {
    BodyScanAppTheme {
        BiometricAuthContent(
            email = "user@example.com",
            uiState = com.example.bodyscanapp.data.BiometricAuthUiState(
                isAuthenticated = false,
                isAuthenticating = true,
                isBiometricAvailable = true,
                biometricStatus = BiometricAuthStatus.SUCCESS,
                errorMessage = null
            ),
            onAuthenticateClick = {},
            onFallbackToTOTP = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BiometricAuthScreenErrorPreview() {
    BodyScanAppTheme {
        BiometricAuthContent(
            email = "user@example.com",
            uiState = com.example.bodyscanapp.data.BiometricAuthUiState(
                isAuthenticated = false,
                isAuthenticating = false,
                isBiometricAvailable = true,
                biometricStatus = BiometricAuthStatus.SUCCESS,
                errorMessage = "Authentication failed. Please try again."
            ),
            onAuthenticateClick = {},
            onFallbackToTOTP = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BiometricAuthScreenUnavailablePreview() {
    BodyScanAppTheme {
        BiometricAuthContent(
            email = "user@example.com",
            uiState = com.example.bodyscanapp.data.BiometricAuthUiState(
                isAuthenticated = false,
                isAuthenticating = false,
                isBiometricAvailable = false,
                biometricStatus = BiometricAuthStatus.ERROR_NONE_ENROLLED,
                errorMessage = "No biometrics enrolled. Please set up fingerprint or face recognition in your device settings."
            ),
            onAuthenticateClick = {},
            onFallbackToTOTP = {}
        )
    }
}

