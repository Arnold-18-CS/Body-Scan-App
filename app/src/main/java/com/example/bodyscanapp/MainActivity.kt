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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.example.bodyscanapp.ui.screens.LoginViewModel
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

    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize AuthManager
        authManager = AuthManager(this)

        // Check if this is a deep link from email
        handleEmailLinkIfPresent(intent)

        setContent {
            BodyScanAppTheme {
                AuthenticationApp(authManager = authManager)
            }
        }
    }

    /**
     * Handle new intent when app is already running
     * This is called when user clicks email link while app is in background
     */
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleEmailLinkIfPresent(intent)
    }

    /**
     * Check if the intent contains an email link and handle sign-in
     * Automatically signs in user if email is stored, otherwise requires manual entry
     * @param intent The intent to check
     */
    private fun handleEmailLinkIfPresent(intent: android.content.Intent?) {
        intent?.data?.let { uri ->
            val link = uri.toString()

            // Check if this is a sign-in link
            if (authManager.isSignInWithEmailLink(link)) {
                // Retrieve the stored email
                val email = authManager.retrieveEmailForLinkAuth()

                if (email != null) {
                    // Sign in with the email link automatically
                    lifecycleScope.launch {
                        authManager.signInWithEmailLink(email, link).collect { result ->
                            when (result) {
                                is AuthResult.Success -> {
                                    // Clear the stored email after successful sign-in
                                    authManager.clearStoredEmail()
                                    // The authState will be updated automatically
                                    // and navigation will occur through LaunchedEffect
                                }
                                is AuthResult.Error -> {
                                    // Error will be handled by authState observer in the UI
                                    android.util.Log.e("MainActivity", "Email link sign-in failed: ${result.message}")
                                }
                                else -> { /* Handle loading state */ }
                            }
                        }
                    }
                } else {
                    // Email not found in storage
                    // This could happen if:
                    // 1. Link is opened on a different device
                    // 2. App data was cleared
                    // 3. Link has expired (>24 hours)
                    // In this case, user will need to request a new link
                    android.util.Log.w("MainActivity", "Email link authentication attempted but no stored email found")
                    // User will see the login screen and can request a new link
                }
            }
        }
    }
}

class LoginViewModelFactory(private val authManager: AuthManager) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(authManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * Main authentication app composable
 * Manages navigation between different authentication screens
 * @param authManager The authentication manager instance
 */
@Composable
fun AuthenticationApp(authManager: AuthManager) {
    var currentScreen by remember { mutableStateOf(AuthScreen.LOGIN_SELECTION) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var currentUser by remember { mutableStateOf<com.google.firebase.auth.FirebaseUser?>(null) }
    var currentUsername by remember { mutableStateOf<String?>(null) }

    // Get context and services
    val context = LocalContext.current
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
                    val loginViewModel: LoginViewModel = viewModel(factory = LoginViewModelFactory(authManager))
                    LoginSelectionScreen(
                        viewModel = loginViewModel,
                        modifier = Modifier.padding(innerPadding),
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
