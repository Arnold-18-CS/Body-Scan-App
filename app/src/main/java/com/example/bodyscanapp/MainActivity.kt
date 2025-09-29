package com.example.bodyscanapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.example.bodyscanapp.data.TotpResult
import com.example.bodyscanapp.services.ShowToast
import com.example.bodyscanapp.services.ToastService
import com.example.bodyscanapp.services.ToastType
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
    var infoMessage by remember { mutableStateOf<String?>(null) }

    // Get context for AuthRepository
    val context = androidx.compose.ui.platform.LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val totpService = remember { TotpService() }
    val toastService = remember { ToastService(context) }

    // Show toast messages
    ShowToast(errorMessage, ToastType.ERROR)
    ShowToast(successMessage, ToastType.SUCCESS)
    ShowToast(infoMessage, ToastType.INFO)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(BodyScanBackground)) { innerPadding ->
        when (currentScreen) {
            AuthScreen.LOGIN -> {
                LoginScreen(
                    onLoginClick = { emailOrUsername, password ->
                        // Clear previous messages
                        errorMessage = null
                        successMessage = null
                        infoMessage = null
                        
                        // Validate input first
                        when (val validation =
                            ValidationUtils.validateLoginInput(emailOrUsername, password)) {
                            is ValidationResult.Success -> {
                                // Attempt authentication
                                when (val authResult =
                                    authRepository.authenticate(emailOrUsername, password)) {
                                    is AuthResult.Success -> {
                                        successMessage = "Login successful! Please enter your 2FA code."
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
                        successMessage = null
                        infoMessage = null
                        currentScreen = AuthScreen.REGISTER
                    }, errorMessage = errorMessage, modifier = Modifier.padding(innerPadding)
                )
            }

            AuthScreen.REGISTER -> {
                RegistrationScreen(
                    onRegisterClick = { username, email, password ->
                        // Clear previous messages
                        errorMessage = null
                        successMessage = null
                        infoMessage = null
                        
                        // Validate input first
                        when (val validation = ValidationUtils.validateRegistrationInput(
                            username, email, password, password
                        )) {
                            is ValidationResult.Success -> {
                                // Attempt registration
                                when (val authResult =
                                    authRepository.register(username, email, password)) {
                                    is AuthResult.Success -> {
                                        successMessage = "Registration successful! Please enter your 2FA code."
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
                        successMessage = null
                        infoMessage = null
                        currentScreen = AuthScreen.LOGIN
                    }, errorMessage = errorMessage, modifier = Modifier.padding(innerPadding)
                )
            }

            AuthScreen.TWO_FACTOR -> {
                TwoFactorAuthScreen(
                    onVerifyClick = { code ->
                        // Clear previous messages
                        errorMessage = null
                        successMessage = null
                        infoMessage = null
                        
                        // Verify TOTP code using the enhanced service
                        when (val totpResult = totpService.verifyTotpCode(code)) {
                            is TotpResult.Success -> {
                                successMessage = "Login successful! Welcome to Body Scan App."
                                currentScreen = AuthScreen.HOME
                            }
                            is TotpResult.Error -> {
                                errorMessage = totpResult.message
                            }
                        }
                    }, onResendClick = {
                        // Clear previous messages
                        errorMessage = null
                        successMessage = null
                        infoMessage = null
                        
                        // Simulate resend logic
                        infoMessage = "New code sent to your authenticator app"
                    }, onSetupTotpClick = {
                        // Clear previous messages
                        errorMessage = null
                        successMessage = null
                        infoMessage = null
                        
                        // Navigate to TOTP setup
                        infoMessage = "TOTP setup feature coming soon"
                    }, errorMessage = errorMessage, modifier = Modifier.padding(innerPadding)
                )
            }

            AuthScreen.HOME -> {
                HomeScreen(
                    onLogoutClick = {
                        currentScreen = AuthScreen.LOGIN
                        errorMessage = null
                        successMessage = null
                        infoMessage = null
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}