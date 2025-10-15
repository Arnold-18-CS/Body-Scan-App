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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.bodyscanapp.data.AuthManager
import com.example.bodyscanapp.data.AuthResult
import com.example.bodyscanapp.data.AuthState
import com.example.bodyscanapp.data.TotpService
import com.example.bodyscanapp.data.TotpVerificationResult
import com.example.bodyscanapp.data.UserPreferencesRepository
import com.example.bodyscanapp.services.ShowToast
import com.example.bodyscanapp.services.ToastType
import com.example.bodyscanapp.ui.screens.HomeScreen
import com.example.bodyscanapp.ui.screens.LoginSelectionScreen
import com.example.bodyscanapp.ui.screens.UsernameSelectionScreen
import com.example.bodyscanapp.ui.screens.TwoFactorAuthScreen
import com.example.bodyscanapp.ui.screens.TotpSetupScreen
import kotlinx.coroutines.launch
import com.example.bodyscanapp.ui.theme.BodyScanAppTheme
import com.example.bodyscanapp.ui.theme.BodyScanBackground

enum class AuthScreen {
    LOGIN_SELECTION, USERNAME_SELECTION, TOTP_SETUP, TWO_FACTOR, HOME
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Firebase is now initialized in BodyScanApplication
        // You can access Firebase services using FirebaseUtils:
        // val auth = FirebaseUtils.getAuth()
        // val firestore = FirebaseUtils.getFirestore()
        // val analytics = FirebaseUtils.getAnalytics()
        
        setContent {
            BodyScanAppTheme {
                AuthenticationApp()
            }
        }
    }
}

@Composable
fun AuthenticationApp() {
    var currentScreen by remember { mutableStateOf(AuthScreen.LOGIN_SELECTION) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var currentUser by remember { mutableStateOf<com.google.firebase.auth.FirebaseUser?>(null) }
    var currentUsername by remember { mutableStateOf<String?>(null) }

    // Get context and services
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    val totpService = remember { TotpService(context) }
    val userPrefsRepo = remember { UserPreferencesRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    
    // Observe auth state
    val authState by authManager.authState.collectAsState()

    // Show toast messages
    ShowToast(errorMessage, ToastType.ERROR)
    ShowToast(successMessage, ToastType.SUCCESS)
    
    // Handle auth state changes
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.SignedIn -> {
                currentUser = state.user
                currentUsername = userPrefsRepo.getDisplayName(state.user)
                if (totpService.isTotpSetup(state.user.uid)) {
                    currentScreen = AuthScreen.TWO_FACTOR
                } else {
                    currentScreen = AuthScreen.TOTP_SETUP
                }
            }
            is AuthState.UsernameSelectionRequired -> {
                currentUser = state.user
                currentScreen = AuthScreen.USERNAME_SELECTION
            }
            is AuthState.SignedOut -> {
                currentUser = null
                currentUsername = null
                currentScreen = AuthScreen.LOGIN_SELECTION
            }
            else -> { /* Handle other states if needed */ }
        }
    }

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
                AuthScreen.LOGIN_SELECTION -> {
                    LoginSelectionScreen(
                        modifier = Modifier.padding(innerPadding),
                        onEmailLinkClick = { email ->
                            coroutineScope.launch {
                                errorMessage = null
                                successMessage = null
                                
                                authManager.sendEmailLink(email).collect { result ->
                                    when (result) {
                                        is AuthResult.Success -> {
                                            successMessage = "Sign-in link sent to $email"
                                        }
                                        is AuthResult.Error -> {
                                            errorMessage = result.message
                                        }
                                        is AuthResult.Loading -> {
                                            successMessage = "Sending sign-in link..."
                                        }
                                    }
                                }
                            }
                        },
                        onGoogleSignInClick = {
                            // TODO: Implement Google Sign-In
                            errorMessage = "Google Sign-In not implemented yet"
                        }
                    )
                }

                AuthScreen.USERNAME_SELECTION -> {
                    UsernameSelectionScreen(
                        modifier = Modifier.padding(innerPadding),
                        user = currentUser,
                        onUsernameSelected = { username ->
                            coroutineScope.launch {
                                if (currentUser != null) {
                                    userPrefsRepo.setUsername(currentUser!!.uid, username)
                                    userPrefsRepo.markUserAsReturning(currentUser!!.uid)
                                    authManager.completeUsernameSelection(currentUser!!)
                                    successMessage = "Welcome, $username!"
                                }
                            }
                        },
                        onBackClick = {
                            coroutineScope.launch {
                                authManager.signOut().collect { result ->
                                    when (result) {
                                        is AuthResult.Success -> {
                                            currentScreen = AuthScreen.LOGIN_SELECTION
                                        }
                                        is AuthResult.Error -> {
                                            errorMessage = result.message
                                        }
                                        else -> { /* Handle loading */ }
                                    }
                                }
                            }
                        }
                    )
                }

                AuthScreen.TOTP_SETUP -> {
                    TotpSetupScreen(
                        username = currentUsername ?: "",
                        onSetupComplete = {
                            if (currentUser != null) {
                                totpService.markTotpSetup(currentUser!!.uid)
                                successMessage = "TOTP setup complete! Please verify with your authenticator."
                                currentScreen = AuthScreen.TWO_FACTOR
                            }
                        },
                        onBackClick = {
                            coroutineScope.launch {
                                authManager.signOut().collect { result ->
                                    when (result) {
                                        is AuthResult.Success -> {
                                            currentScreen = AuthScreen.LOGIN_SELECTION
                                        }
                                        is AuthResult.Error -> {
                                            errorMessage = result.message
                                        }
                                        else -> { /* Handle loading */ }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                AuthScreen.TWO_FACTOR -> {
                    TwoFactorAuthScreen(
                        username = currentUsername ?: "",
                        onVerifyClick = { code ->
                            // Clear previous messages
                            errorMessage = null
                            successMessage = null
                            
                            // Verify TOTP code using Firebase UID
                            if (currentUser != null) {
                                when (val totpResult = totpService.verifyTotpCode(currentUser!!.uid, code)) {
                                    is TotpVerificationResult.Success -> {
                                        successMessage = "2FA verification successful! Welcome to Body Scan App."
                                        currentScreen = AuthScreen.HOME
                                    }
                                    is TotpVerificationResult.Error -> {
                                        errorMessage = totpResult.message
                                    }
                                }
                            } else {
                                errorMessage = "User not authenticated"
                            }
                        }, onResendClick = {
                            // Clear previous messages
                            errorMessage = null
                            successMessage = "New code sent to your authenticator app"
                        }, onSetupTotpClick = {
                            // Navigate to TOTP setup
                            errorMessage = null
                            successMessage = "Redirecting to TOTP setup..."
                            currentScreen = AuthScreen.TOTP_SETUP
                        }, modifier = Modifier.padding(innerPadding)
                    )
                }

                AuthScreen.HOME -> {
                    HomeScreen(
                        onLogoutClick = {
                            coroutineScope.launch {
                                authManager.signOut().collect { result ->
                                    when (result) {
                                        is AuthResult.Success -> {
                                            currentScreen = AuthScreen.LOGIN_SELECTION
                                            errorMessage = null
                                            successMessage = "Logged out successfully"
                                        }
                                        is AuthResult.Error -> {
                                            errorMessage = result.message
                                        }
                                        else -> { /* Handle loading */ }
                                    }
                                }
                            }
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
                        username = currentUsername,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}