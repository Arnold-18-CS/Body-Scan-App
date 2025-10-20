package com.example.bodyscanapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bodyscanapp.R
import com.example.bodyscanapp.data.BiometricAuthManager
import com.example.bodyscanapp.data.BiometricAuthStatus
import com.example.bodyscanapp.data.BiometricAuthUiState
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
            uiState = BiometricAuthUiState(
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

    // Note: Removed auto-trigger logic since user now manually selects authentication method

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
 * Shows three authentication method options: Fingerprint, Face ID, and TOTP.
 */
@Composable
private fun BiometricAuthContent(
    email: String,
    uiState: BiometricAuthUiState,
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
                .size(200.dp)
                .padding(bottom = 24.dp)
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
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Instruction text
        Text(
            text = "Choose how you'd like to verify",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Authentication method options
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Top
        ) {
            AuthMethodOptionBox(
                painter = painterResource(id = R.drawable.face),
                label = "Face ID",
                onClick = onAuthenticateClick,
                enabled = uiState.isBiometricAvailable && !uiState.isAuthenticating,
                isLoading = uiState.isAuthenticating,
                iconSize = 50.dp
            )

            AuthMethodOptionBox(
                painter = painterResource(id = R.drawable.fingerprint),
                label = "Fingerprint",
                onClick = onAuthenticateClick,
                enabled = uiState.isBiometricAvailable && !uiState.isAuthenticating,
                isLoading = uiState.isAuthenticating
            )

            AuthMethodOptionBox(
                painter = painterResource(id = R.drawable.pin),
                label = "TOTP",
                onClick = onFallbackToTOTP,
                enabled = !uiState.isAuthenticating,
                isLoading = false
            )
        }

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

        // Status message for biometric unavailability
        if (!uiState.isBiometricAvailable && uiState.biometricStatus != BiometricAuthStatus.SUCCESS) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
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
                        text = getBiometricUnavailableMessage(uiState.biometricStatus),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
 * AuthMethodOptionBox
 *
 * Reusable card for authentication method options with icon and label.
 * This version is arranged vertically to fit better in a Row.
 */
@Composable
private fun AuthMethodOptionBox(
    modifier: Modifier = Modifier,
    painter: Painter,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    iconSize: Dp = 40.dp
) {
    Card(
        modifier = modifier
            .clickable(enabled = enabled && !isLoading) { onClick() }
            .border(
                width = 1.dp,
                color = if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Box(
                modifier = Modifier.size(iconSize + 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(iconSize),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                } else {
                    Image(
                        painter = painter,
                        contentDescription = label,
                        modifier = Modifier.size(iconSize),
                        alpha = if (enabled) 1f else 0.5f
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Text content
            Text(
                text = if (isLoading) "Authenticating..." else label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
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
            uiState = BiometricAuthUiState(
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
            uiState = BiometricAuthUiState(
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
            uiState = BiometricAuthUiState(
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
            uiState = BiometricAuthUiState(
                isAuthenticated = false,
                isAuthenticating = false,
                isBiometricAvailable = false,
                biometricStatus = BiometricAuthStatus.ERROR_NONE_ENROLLED,
                errorMessage = null
            ),
            onAuthenticateClick = {},
            onFallbackToTOTP = {}
        )
    }
}
