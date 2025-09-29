package com.example.bodyscanapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.bodyscanapp.data.AuthRepository
import com.example.bodyscanapp.data.AuthResult
import com.example.bodyscanapp.data.TotpService
import com.example.bodyscanapp.ui.screens.HomeScreen
import com.example.bodyscanapp.ui.screens.LoginScreen
import com.example.bodyscanapp.ui.screens.RegistrationScreen
import com.example.bodyscanapp.ui.screens.TwoFactorAuthScreen
import com.example.bodyscanapp.ui.theme.BodyScanAppTheme
import com.example.bodyscanapp.ui.theme.BodyScanBackground
import com.example.bodyscanapp.utils.ValidationResult
import com.example.bodyscanapp.utils.ValidationUtils

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
    val snackbarHostState = remember { SnackbarHostState() }

    // Get context for AuthRepository
    val context = androidx.compose.ui.platform.LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val totpService = remember { TotpService() }

    // Show snackbar when error message changes
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Show snackbar when success message changes
    LaunchedEffect(successMessage) {
        successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(BodyScanBackground),
        snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        when (currentScreen) {
            AuthScreen.LOGIN -> {
                LoginScreen(
                    onLoginClick = { emailOrUsername, password ->
                        // Validate input first
                        when (val validation =
                            ValidationUtils.validateLoginInput(emailOrUsername, password)) {
                            is ValidationResult.Success -> {
                                // Attempt authentication
                                when (val authResult =
                                    authRepository.authenticate(emailOrUsername, password)) {
                                    is AuthResult.Success -> {
                                        errorMessage = null
                                        currentScreen = AuthScreen.TWO_FACTOR
                                    }

                                    is AuthResult.Error -> {
                                        errorMessage = authResult.message
                                    }
                                }
                            }

                            is ValidationResult.Error -> {
                                errorMessage = validation.message
                            }
                        }
                    }, onRegisterClick = {
                        errorMessage = null
                        currentScreen = AuthScreen.REGISTER
                    }, errorMessage = errorMessage, modifier = Modifier.padding(innerPadding)
                )
            }

            AuthScreen.REGISTER -> {
                RegistrationScreen(
                    onRegisterClick = { username, email, password ->
                        // Validate input first
                        when (val validation = ValidationUtils.validateRegistrationInput(
                            username, email, password, password
                        )) {
                            is ValidationResult.Success -> {
                                // Attempt registration
                                when (val authResult =
                                    authRepository.register(username, email, password)) {
                                    is AuthResult.Success -> {
                                        errorMessage = null
                                        currentScreen = AuthScreen.TWO_FACTOR
                                    }

                                    is AuthResult.Error -> {
                                        errorMessage = authResult.message
                                    }
                                }
                            }

                            is ValidationResult.Error -> {
                                errorMessage = validation.message
                            }
                        }
                    }, onLoginClick = {
                        errorMessage = null
                        currentScreen = AuthScreen.LOGIN
                    }, errorMessage = errorMessage, modifier = Modifier.padding(innerPadding)
                )
            }

            AuthScreen.TWO_FACTOR -> {
                TwoFactorAuthScreen(
                    onVerifyClick = { code ->
                        // Verify TOTP code using the service
                        if (totpService.verifyTotpCode(code)) {
                            errorMessage = null
                            successMessage = "Login successful! Welcome to Body Scan App."
                            currentScreen = AuthScreen.HOME
                        } else {
                            errorMessage = "Invalid or expired code. Please try again."
                        }
                    }, onResendClick = {
                        // Simulate resend logic
                        errorMessage = null
                        successMessage = "New code sent to your authenticator app"
                    }, onSetupTotpClick = {
                        // Navigate to TOTP setup
                        errorMessage = "TOTP setup feature coming soon"
                    }, errorMessage = errorMessage, modifier = Modifier.padding(innerPadding)
                )
            }

            AuthScreen.HOME -> {
                HomeScreen(
                    onLogoutClick = {
                        currentScreen = AuthScreen.LOGIN
                        errorMessage = null
                        successMessage = null
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}