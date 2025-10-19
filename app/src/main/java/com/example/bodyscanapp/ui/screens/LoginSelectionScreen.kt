package com.example.bodyscanapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bodyscanapp.R
import com.example.bodyscanapp.data.AuthManager
import com.example.bodyscanapp.data.AuthResult
import com.example.bodyscanapp.ui.theme.BodyScanAppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * LoginViewModel
 * 
 * Manages the state and logic for the login selection screen.
 * Handles email link authentication flow including:
 * - Email input validation
 * - Sending email verification links
 * - Storing email for link authentication
 * - Managing UI state (loading, errors, success)
 * 
 * Google Sign-In is handled directly by MainActivity through callbacks,
 * as it requires Activity context for the Activity Result API.
 * 
 * @param authManager The authentication manager for performing auth operations
 */
class LoginViewModel(private val authManager: AuthManager) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onEmailLinkClick() {
        if (_uiState.value.showEmailInput) {
            if (_uiState.value.email.isNotBlank()) {
                sendEmailLink()
            } else {
                _uiState.update { it.copy(errorMessage = "Please enter your email address") }
            }
        } else {
            _uiState.update { it.copy(showEmailInput = true) }
        }
    }

    private fun sendEmailLink() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authManager.saveEmailForLinkAuth(_uiState.value.email)
            authManager.sendEmailLink(_uiState.value.email).collect { result ->
                when (result) {
                    is AuthResult.Success -> {
                        _uiState.update { it.copy(isLoading = false, emailLinkSent = true) }
                    }
                    is AuthResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                    }
                    is AuthResult.Loading -> { }
                }
            }
        }
    }
}

/**
 * LoginUiState
 * Represents the state of the login screen
 */
data class LoginUiState(
    val email: String = "",
    val showEmailInput: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val emailLinkSent: Boolean = false
)

/**
 * LoginSelectionScreen
 * 
 * Entry point for user authentication. Provides two sign-in methods:
 * 
 * 1. Email Link (Passwordless):
 *    - User enters email address
 *    - System sends verification link via email
 *    - User clicks link to complete sign-in
 *    - Managed by LoginViewModel
 * 
 * 2. Google Sign-In:
 *    - User clicks Google button
 *    - System launches Google Sign-In activity via MainActivity
 *    - User selects Google account
 *    - System authenticates with Firebase using Google credential
 *    - Managed by MainActivity (requires Activity context for Activity Result API)
 * 
 * Both methods lead to the same authentication flow after sign-in:
 * New users → Username Selection → TOTP Setup → 2FA Verification → Home
 * Returning users → TOTP Verification → Home
 * 
 * @param modifier Modifier for styling
 * @param viewModel ViewModel managing email link authentication state
 * @param onGoogleSignInClick Callback to launch Google Sign-In from MainActivity
 */
@Composable
fun LoginSelectionScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel,
    onGoogleSignInClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LoginSelectionContent(
        modifier = modifier,
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onEmailLinkClick = viewModel::onEmailLinkClick,
        onGoogleSignInClick = onGoogleSignInClick
    )
}

@Composable
fun LoginSelectionContent(
    modifier: Modifier = Modifier,
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onEmailLinkClick: () -> Unit,
    onGoogleSignInClick: () -> Unit
) {
    val emailFocusRequester = remember { FocusRequester() }

    LaunchedEffect(uiState.showEmailInput) {
        if (uiState.showEmailInput) {
            emailFocusRequester.requestFocus()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_body_scan),
            contentDescription = "Body Scan Logo",
            modifier = Modifier
                .size(200.dp)
                .padding(bottom = 32.dp)
        )

        Text(
            text = if (uiState.emailLinkSent) "Check your inbox" else "Welcome",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = if (uiState.emailLinkSent) "We've sent a sign-in link to ${uiState.email}." else "Choose how you'd like to sign in",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        AnimatedVisibility(
            visible = uiState.showEmailInput,
            enter = slideInVertically(initialOffsetY = { -it }, animationSpec = tween(durationMillis = 300)),
            exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(durationMillis = 200))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = onEmailChange,
                    label = { Text("Email Address") },
                    placeholder = { Text("Enter your email") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = "Email") },
                    isError = uiState.errorMessage != null,
                    supportingText = {
                        if (uiState.errorMessage != null) {
                            Text(uiState.errorMessage, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("We'll send you a secure sign-in link")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onEmailLinkClick() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(emailFocusRequester),
                    singleLine = true,
                    enabled = !uiState.isLoading
                )
            }
        }

        if (uiState.isLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Sending sign-in link...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!uiState.emailLinkSent) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AuthOptionBox(
                    icon = Icons.Default.Email,
                    label = "Email Link",
                    description = "Sign in with email",
                    onClick = onEmailLinkClick,
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading
                )

                AuthOptionBox(
                    painter = painterResource(id = R.drawable.google_logo),
                    label = "Google",
                    description = "Sign in with Google",
                    onClick = onGoogleSignInClick,
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Your data is secure and encrypted. We never store your password.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 32.dp)
        )
    }
}

@Composable
private fun AuthOptionBox(
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Card(
        modifier = modifier
            .clickable(enabled = enabled) { onClick() }
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
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier
                    .size(32.dp)
                    .padding(bottom = 12.dp),
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun AuthOptionBox(
    painter: Painter,
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Card(
        modifier = modifier
            .clickable(enabled = enabled) { onClick() }
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
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painter,
                contentDescription = label,
                modifier = Modifier
                    .size(32.dp)
                    .padding(bottom = 12.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginSelectionScreenPreview() {
    BodyScanAppTheme {
        LoginSelectionContent(
            uiState = LoginUiState(),
            onEmailChange = {},
            onEmailLinkClick = {},
            onGoogleSignInClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginSelectionScreenWithEmailInputPreview() {
    BodyScanAppTheme {
        LoginSelectionContent(
            uiState = LoginUiState(showEmailInput = true, email = "test@example.com"),
            onEmailChange = {},
            onEmailLinkClick = {},
            onGoogleSignInClick = {}
        )
    }
}
