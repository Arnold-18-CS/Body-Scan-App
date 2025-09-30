package com.example.bodyscanapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.bodyscanapp.data.AuthRepository
import com.example.bodyscanapp.data.AuthResult
import com.example.bodyscanapp.data.TotpService
import com.example.bodyscanapp.data.TotpVerificationResult
import com.example.bodyscanapp.services.ShowToast
import com.example.bodyscanapp.services.ToastType
import com.example.bodyscanapp.ui.screens.HomeScreen
import com.example.bodyscanapp.ui.screens.LoginScreen
import com.example.bodyscanapp.ui.screens.RegistrationScreen
import com.example.bodyscanapp.ui.screens.TwoFactorAuthScreen
import com.example.bodyscanapp.ui.theme.BodyScanAppTheme
import com.example.bodyscanapp.ui.theme.BodyScanBackground

enum class AuthScreen {
    LOGIN, REGISTER, TWO_FACTOR, HOME
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BodyScanAppTheme {
                AuthenticationApp()
            }
        }
    }
}

@Composable
fun AuthenticationApp() {
    var currentScreen by remember { mutableStateOf(AuthScreen.LOGIN) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Get context for AuthRepository
    val context = androidx.compose.ui.platform.LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val totpService = remember { TotpService() }

    // Show toast messages
    ShowToast(errorMessage, ToastType.ERROR)
    ShowToast(successMessage, ToastType.SUCCESS)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(BodyScanBackground)) { innerPadding ->
        Crossfade(
            targetState = currentScreen,
            animationSpec = tween(durationMillis = 300),
            label = "screen_transition"
        ) { screen ->
            when (screen) {
                AuthScreen.LOGIN -> {
                    LoginScreen(
                        onLoginClick = { emailOrUsername, password ->
                            // Clear previous messages
                            errorMessage = null
                            successMessage = null
                            
                            // Attempt authentication directly (AuthRepository now handles validation)
                            when (val authResult = authRepository.authenticate(emailOrUsername, password)) {
                                is AuthResult.Success -> {
                                    successMessage = "Login successful! Please enter your 2FA code."
                                    currentScreen = AuthScreen.TWO_FACTOR
                                }
                                is AuthResult.Error -> {
                                    errorMessage = authResult.message
                                }
                            }
                        }, onRegisterClick = {
                            errorMessage = null
                            successMessage = null
                            currentScreen = AuthScreen.REGISTER
                        }, modifier = Modifier.padding(innerPadding)
                    )
                }

                AuthScreen.REGISTER -> {
                    RegistrationScreen(
                        onRegisterClick = { username, email, password ->
                            // Clear previous messages
                            errorMessage = null
                            successMessage = null
                            
                            // Attempt registration (AuthRepository now handles validation)
                            when (val authResult = authRepository.register(username, email, password)) {
                                is AuthResult.Success -> {
                                    successMessage = "Registration successful! Please enter your 2FA code."
                                    currentScreen = AuthScreen.TWO_FACTOR
                                }
                                is AuthResult.Error -> {
                                    errorMessage = authResult.message
                                }
                            }
                        }, onLoginClick = {
                            errorMessage = null
                            successMessage = null
                            currentScreen = AuthScreen.LOGIN
                        }, modifier = Modifier.padding(innerPadding)
                    )
                }

                AuthScreen.TWO_FACTOR -> {
                    TwoFactorAuthScreen(
                        onVerifyClick = { code ->
                            // Clear previous messages
                            errorMessage = null
                            successMessage = null
                            
                            // Verify TOTP code using the enhanced service
                            when (val totpResult = totpService.verifyTotpCode(code)) {
                                is TotpVerificationResult.Success -> {
                                    successMessage = "2FA verification successful! Welcome to Body Scan App."
                                    currentScreen = AuthScreen.HOME
                                }
                                is TotpVerificationResult.Error -> {
                                    errorMessage = totpResult.message
                                }
                            }
                        }, onResendClick = {
                            // Clear previous messages
                            errorMessage = null
                            successMessage = "New code sent to your authenticator app"
                        }, onSetupTotpClick = {
                            // Navigate to TOTP setup
                            errorMessage = null
                            successMessage = "TOTP setup feature coming soon"
                        }, modifier = Modifier.padding(innerPadding)
                    )
                }

                AuthScreen.HOME -> {
                    HomeScreen(
                        onLogoutClick = {
                            authRepository.logout()
                            currentScreen = AuthScreen.LOGIN
                            errorMessage = null
                            successMessage = "Logged out successfully"
                        },
                        onNewScanClick = {
                            // TODO: Navigate to new scan screen
                            successMessage = "New Scan clicked - Feature coming soon!"
                        },
                        onViewHistoryClick = {
                            // TODO: Navigate to scan history screen
                            successMessage = "View History clicked - Feature coming soon!"
                        },
                        onExportScansClick = {
                            // TODO: Navigate to export scans screen
                            successMessage = "Export Scans clicked - Feature coming soon!"
                        },
                        username = authRepository.getCurrentUser(),
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}