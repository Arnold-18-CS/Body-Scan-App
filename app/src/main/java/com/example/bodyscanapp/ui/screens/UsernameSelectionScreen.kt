package com.example.bodyscanapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bodyscanapp.ui.theme.BodyScanAppTheme
import com.google.firebase.auth.FirebaseUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsernameSelectionScreen(
    modifier: Modifier = Modifier,
    user: FirebaseUser? = null,
    onUsernameSelected: (String) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    var username by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showError by remember { mutableStateOf(false) }
    
    // Focus requester for keyboard navigation
    val usernameFocusRequester = remember { FocusRequester() }
    
    // Initialize username with Firebase defaults
    LaunchedEffect(user) {
        username = user?.displayName ?: user?.email?.substringBefore("@") ?: ""
        if (username.isNotBlank()) {
            usernameFocusRequester.requestFocus()
        }
    }
    
    // Show error with animation
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            showError = true
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
        // Title
        Text(
            text = "Choose Your Display Name",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "This will be shown on your home screen",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // Username input field
        OutlinedTextField(
            value = username,
            onValueChange = { 
                username = it.trim()
                if (showError) {
                    showError = false
                    errorMessage = null
                }
            },
            label = { Text("Display Name") },
            placeholder = { Text("Enter your display name") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Display Name"
                )
            },
            isError = showError,
            supportingText = if (showError) {
                { Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error) }
            } else {
                { Text("You can change this later in settings") }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { 
                    if (username.isNotBlank() && !isLoading) {
                        onUsernameSelected(username)
                    }
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .focusRequester(usernameFocusRequester),
            singleLine = true,
            enabled = !isLoading
        )
        
        // Error message with animation
        AnimatedVisibility(
            visible = showError,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(durationMillis = 300)
            ),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(durationMillis = 200)
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Back button
            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                enabled = !isLoading
            ) {
                Text("Back")
            }
            
            // Continue button
            Button(
                onClick = {
                    when {
                        username.isBlank() -> {
                            errorMessage = "Please enter a display name"
                            showError = true
                        }
                        username.length < 2 -> {
                            errorMessage = "Display name must be at least 2 characters"
                            showError = true
                        }
                        username.length > 30 -> {
                            errorMessage = "Display name must be less than 30 characters"
                            showError = true
                        }
                        !username.matches(Regex("^[a-zA-Z0-9._-]+$")) -> {
                            errorMessage = "Display name can only contain letters, numbers, dots, underscores, and hyphens"
                            showError = true
                        }
                        else -> {
                            isLoading = true
                            onUsernameSelected(username)
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                enabled = !isLoading && username.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Continue")
                }
            }
        }
        
        // Help text
        Text(
            text = "This name will be visible to you on the home screen. You can always change it later.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun UsernameSelectionScreenPreview() {
    BodyScanAppTheme {
        UsernameSelectionScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun UsernameSelectionScreenWithErrorPreview() {
    BodyScanAppTheme {
        UsernameSelectionScreen(
            user = null,
            onUsernameSelected = {},
            onBackClick = {}
        )
    }
}


