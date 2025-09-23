package com.example.bodyscanapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.bodyscanapp.ui.theme.BodyScanBackground
import com.example.bodyscanapp.ui.screens.LoginScreen
import com.example.bodyscanapp.ui.screens.RegistrationScreen
import com.example.bodyscanapp.ui.screens.TwoFactorAuthScreen
import com.example.bodyscanapp.ui.theme.BodyScanAppTheme

enum class AuthScreen {
    LOGIN,
    REGISTER,
    TWO_FACTOR
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

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(BodyScanBackground)
    ) { innerPadding ->
        when (currentScreen) {
            AuthScreen.LOGIN -> {
                LoginScreen(
                    onLoginClick = { email, password ->
                        // Simulate login logic
                        if (email.isNotEmpty() && password.isNotEmpty()) {
                            errorMessage = null
                            currentScreen = AuthScreen.TWO_FACTOR
                        } else {
                            errorMessage = "Please enter valid credentials"
                        }
                    },
                    onRegisterClick = {
                        errorMessage = null
                        currentScreen = AuthScreen.REGISTER
                    },
                    errorMessage = errorMessage,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AuthScreen.REGISTER -> {
                RegistrationScreen(
                    onRegisterClick = { username, email, password ->
                        // Simulate registration logic
                        if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                            errorMessage = null
                            currentScreen = AuthScreen.TWO_FACTOR
                        } else {
                            errorMessage = "Please fill in all fields"
                        }
                    },
                    onLoginClick = {
                        errorMessage = null
                        currentScreen = AuthScreen.LOGIN
                    },
                    errorMessage = errorMessage,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AuthScreen.TWO_FACTOR -> {
                TwoFactorAuthScreen(
                    onVerifyClick = { code ->
                        // Simulate 2FA verification
                        if (code.length == 6) {
                            errorMessage = null
                            // Navigate to main app or show success
                        } else {
                            errorMessage = "Please enter a valid 6-digit code"
                        }
                    },
                    onResendClick = {
                        // Simulate resend logic
                        errorMessage = "New code sent to your authenticator app"
                    },
                    onSetupTotpClick = {
                        // Navigate to TOTP setup
                        errorMessage = "TOTP setup feature coming soon"
                    },
                    errorMessage = errorMessage,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}