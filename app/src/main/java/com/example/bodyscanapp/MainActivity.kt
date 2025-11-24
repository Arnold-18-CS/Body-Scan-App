package com.example.bodyscanapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
import androidx.navigation.compose.rememberNavController
import com.example.bodyscanapp.data.AuthManager
import com.example.bodyscanapp.data.AuthResult
import com.example.bodyscanapp.data.AuthState
import com.example.bodyscanapp.data.BiometricAuthManager
import com.example.bodyscanapp.data.BiometricAuthStatus
import com.example.bodyscanapp.data.TotpService
import com.example.bodyscanapp.data.TotpVerificationResult
import com.example.bodyscanapp.data.UserPreferencesRepository
import com.example.bodyscanapp.navigation.BodyScanNavGraph
import com.example.bodyscanapp.services.ShowToast
import com.example.bodyscanapp.services.ToastType
import com.example.bodyscanapp.ui.screens.BiometricAuthScreen
import com.example.bodyscanapp.ui.screens.LoginSelectionScreen
import com.example.bodyscanapp.ui.screens.LoginViewModel
import com.example.bodyscanapp.ui.screens.TotpSetupScreen
import com.example.bodyscanapp.ui.screens.TwoFactorAuthScreen
import com.example.bodyscanapp.ui.screens.UsernameSelectionScreen
import com.example.bodyscanapp.ui.theme.BodyScanAppTheme
import com.example.bodyscanapp.ui.theme.BodyScanBackground
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

enum class AuthScreen {
    LOGIN_SELECTION, USERNAME_SELECTION, TOTP_SETUP, TWO_FACTOR, BIOMETRIC_AUTH, HOME
}

/**
 * MainActivity
 *
 * Main entry point for the application that manages:
 * - Authentication flow and state management
 * - Google Sign-In integration using Activity Result API
 * - Email link authentication (passwordless)
 * - Deep link handling for email verification
 * - Navigation between authentication screens
 * - Biometric authentication (fingerprint/face recognition)
 *
 * This activity initializes the AuthManager and sets up the Google Sign-In launcher
 * to handle authentication results. It also handles deep links from email verification.
 *
 * Note: Extends AppCompatActivity (which extends FragmentActivity) to support
 * BiometricPrompt API which requires FragmentActivity context.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager

    /**
     * Google Sign-In launcher using Activity Result API
     *
     * This launcher is registered before onCreate and handles the result from
     * Google Sign-In activity. When user completes Google Sign-In (or cancels),
     * the result is passed to AuthManager which processes it and updates auth state.
     *
     * The Activity Result API is the modern Android approach replacing deprecated
     * startActivityForResult/onActivityResult pattern.
     */
    private lateinit var googleSignInLauncher: ActivityResultLauncher<android.content.Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize AuthManager - must be done before registering launchers
        authManager = AuthManager(this)

        // Initialize auth state to check if user is already logged in
        // This enables session persistence - user stays logged in after closing the app
        authManager.initializeAuthState()

        // Register Google Sign-In launcher using Activity Result API
        // This must be done before the activity is created (before super.onCreate or in onCreate)
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            // Handle the result from Google Sign-In activity
            // The result contains the intent data with Google account information
            handleGoogleSignInResult(result.data)
        }

        // Check if this is a deep link from email
        handleEmailLinkIfPresent(intent)

        setContent {
            BodyScanAppTheme {
                // Pass both AuthManager and the Google Sign-In launcher callback
                AuthenticationApp(
                    authManager = authManager,
                    onGoogleSignInClick = { launchGoogleSignIn() }
                )
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
     * Clear session verification when app goes to background
     * This ensures biometric authentication is required when app reopens
     */
    override fun onStop() {
        super.onStop()
        // Clear session verification for current user when app goes to background
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.uid?.let { uid ->
            val userPrefs = UserPreferencesRepository(this)
            userPrefs.clearSessionVerification(uid)
        }
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

    /**
     * Handle Google Sign-In result
     *
     * This method is called by the Activity Result API launcher when Google Sign-In completes.
     * It processes the result intent and passes it to AuthManager for credential validation.
     *
     * Flow:
     * 1. Receives intent data containing Google account info (ID token, email, etc.)
     * 2. Passes to AuthManager.handleGoogleSignInResult() which:
     *    - Extracts the Google ID token from the intent
     *    - Creates Firebase credential from the token
     *    - Signs in to Firebase with the credential
     * 3. AuthManager updates its authState based on success/failure
     * 4. UI observes authState changes and navigates accordingly:
     *    - New users → Username selection
     *    - Returning users → TOTP setup/verification → Home
     *
     * @param data Intent containing Google Sign-In result data
     */
    private fun handleGoogleSignInResult(data: android.content.Intent?) {
        lifecycleScope.launch {
            // Pass the result to AuthManager which handles:
            // - Extracting the Google account from intent
            // - Creating Firebase credential
            // - Signing in to Firebase
            // - Updating auth state
            authManager.handleGoogleSignInResult(data).collect { result ->
                when (result) {
                    is AuthResult.Success -> {
                        // Success! AuthManager has updated authState to SignedIn
                        // The UI will automatically navigate based on authState observer
                        android.util.Log.d("MainActivity", "Google Sign-In successful: ${result.user?.email}")
                    }
                    is AuthResult.Error -> {
                        // Error will be displayed by the UI through error state management
                        android.util.Log.e("MainActivity", "Google Sign-In failed: ${result.message}")
                    }
                    is AuthResult.Loading -> {
                        // Loading state is handled by AuthManager's authState
                        android.util.Log.d("MainActivity", "Google Sign-In in progress...")
                    }
                }
            }
        }
    }

    /**
     * Launch Google Sign-In flow
     *
     * This method initiates the Google Sign-In process by:
     * 1. Getting the pre-configured Google Sign-In intent from AuthManager
     *    (which clears Google Sign-In state to force account picker)
     * 2. Launching it using the Activity Result API launcher
     *
     * The intent is configured in FirebaseAuthService with:
     * - Request for ID token (required for Firebase authentication)
     * - Request for email address
     * - Web client ID from Firebase configuration
     * - Always shows account picker for user choice
     *
     * When user completes sign-in, the result is delivered to handleGoogleSignInResult()
     */
    fun launchGoogleSignIn() {
        lifecycleScope.launch {
            try {
                // Get the pre-configured Google Sign-In intent from AuthManager
                // This will show the account picker for all available Gmail accounts
                val signInIntent = authManager.getGoogleSignInIntent()

                // Launch the Google Sign-In activity
                // Result will be delivered to googleSignInLauncher callback
                googleSignInLauncher.launch(signInIntent)

                android.util.Log.d("MainActivity", "Google Sign-In intent launched with account picker")
            } catch (e: Exception) {
                // Log any errors during launch
                android.util.Log.e("MainActivity", "Failed to launch Google Sign-In: ${e.message}", e)
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
 *
 * Manages the authentication flow and navigation between screens:
 * - Login Selection: Choose between Email Link or Google Sign-In
 * - Username Selection: New users choose their username
 * - TOTP Setup: Set up two-factor authentication
 * - Two-Factor Verification: Enter TOTP code
 * - Home: Main app screen
 *
 * The navigation is driven by observing AuthManager's authState, which is updated
 * by authentication events (email link, Google Sign-In, etc.)
 *
 * @param authManager The authentication manager instance that handles all auth operations
 * @param onGoogleSignInClick Callback to launch Google Sign-In flow from MainActivity
 */
@Composable
fun AuthenticationApp(
    authManager: AuthManager,
    onGoogleSignInClick: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(AuthScreen.LOGIN_SELECTION) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var currentUser by remember { mutableStateOf<com.google.firebase.auth.FirebaseUser?>(null) }
    var currentUsername by remember { mutableStateOf<String?>(null) }

    // Get context and services
    val context = LocalContext.current
    val totpService = remember { TotpService(context) }
    val userPrefsRepo = remember { UserPreferencesRepository(context) }
    val biometricAuthManager = remember { BiometricAuthManager(context) }
    val coroutineScope = rememberCoroutineScope()

    // Track initial auth state to detect session persistence
    val initialAuthState = remember { mutableStateOf<AuthState?>(null) }

    // Track if this is a session persistence scenario (user was already signed in)
    var isSessionPersistence by remember { mutableStateOf(false) }

    // Observe auth state
    val authState by authManager.authState.collectAsState()

    // Show toast messages for success/error feedback
    ShowToast(errorMessage, ToastType.ERROR)
    ShowToast(successMessage, ToastType.SUCCESS)

    // Handle auth state changes
    LaunchedEffect(authState) {
        // Capture initial state
        if (initialAuthState.value == null) {
            initialAuthState.value = authState
        }

        when (val state = authState) {
            is AuthState.SignedIn -> {
                currentUser = state.user
                currentUsername = userPrefsRepo.getDisplayName(state.user)

                // Show session persistence message only if initial state was also SignedIn
                // This means user was already logged in when app started
                if (initialAuthState.value is AuthState.SignedIn &&
                    (initialAuthState.value as AuthState.SignedIn).user.uid == state.user.uid) {
                    val email = state.user.email ?: "user"
                    successMessage = "Already signed in as $email"
                    isSessionPersistence = true
                    // Reset to prevent showing again
                    initialAuthState.value = null
                }

                // Try to migrate legacy TOTP data if it exists
                if (currentUsername != null) {
                    totpService.migrateTotpData(currentUsername!!, state.user.uid)
                }

                // Check if user needs username selection first
                if (authManager.needsUsernameSelection(state.user)) {
                    currentScreen = AuthScreen.USERNAME_SELECTION
                } else if (totpService.isTotpSetup(state.user.uid)) {
                    // Session persistence: Check if biometric auth is available and session needs verification
                    if (isSessionPersistence && !userPrefsRepo.isSessionVerified(state.user.uid)) {
                        // Check if biometric is available and enabled
                        val biometricStatus = biometricAuthManager.checkBiometricSupport()
                        currentScreen = if (biometricStatus == BiometricAuthStatus.SUCCESS &&
                            userPrefsRepo.isBiometricEnabled(state.user.uid)) {
                            AuthScreen.BIOMETRIC_AUTH
                        } else {
                            // Fallback to TOTP if biometric not available
                            AuthScreen.TWO_FACTOR
                        }
                    } else if (userPrefsRepo.isSessionVerified(state.user.uid)) {
                        // Session already verified, go directly to home
                        currentScreen = AuthScreen.HOME
                    } else {
                        // Normal flow: first login after setup requires TOTP
                        currentScreen = AuthScreen.TWO_FACTOR
                    }
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
                        onGoogleSignInClick = onGoogleSignInClick
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
                                    // Update current username immediately
                                    currentUsername = username
                                    // Complete username selection triggers SignedIn state
                                    authManager.completeUsernameSelection(currentUser!!)
                                    successMessage = "Welcome, $username!"
                                    // Navigate to TOTP setup after username selection
                                    currentScreen = AuthScreen.TOTP_SETUP
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
                        uid = currentUser?.uid ?: "",
                        username = currentUsername ?: "",
                        onSetupComplete = {
                            if (currentUser != null) {
                                // Mark TOTP as setup and navigate to verification
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
                                        // Mark session as verified
                                        userPrefsRepo.setSessionVerified(currentUser!!.uid, true)
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
                    // Use Navigation Compose for the scan flow
                    val navController = rememberNavController()
                    
                    BodyScanNavGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding),
                        onLogoutClick = {
                            coroutineScope.launch {
                                // Clear session verification before signing out
                                currentUser?.uid?.let { uid ->
                                    userPrefsRepo.clearSessionVerification(uid)
                                }

                                authManager.signOut().collect { result ->
                                    when (result) {
                                        is AuthResult.Success -> {
                                            currentUser = null
                                            currentUsername = null
                                            isSessionPersistence = false
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
                        onShowSuccessMessage = { message ->
                            successMessage = message
                        },
                        username = currentUsername
                    )
                }

                AuthScreen.BIOMETRIC_AUTH -> {
                    BiometricAuthScreen(
                        email = currentUser?.email ?: "user@example.com",
                        onAuthSuccess = {
                            // Mark session as verified after successful biometric auth
                            currentUser?.uid?.let { uid ->
                                userPrefsRepo.setSessionVerified(uid, true)
                            }
                            successMessage = "Authentication successful! Welcome back."
                            currentScreen = AuthScreen.HOME
                        },
                        onFallbackToTOTP = {
                            // User chose to use TOTP instead of biometric
                            currentScreen = AuthScreen.TWO_FACTOR
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
