package com.example.bodyscanapp.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bodyscanapp.R
import com.example.bodyscanapp.data.TotpService
import com.example.bodyscanapp.data.TotpVerificationResult
import com.example.bodyscanapp.ui.theme.BodyScanAppTheme
import kotlinx.coroutines.delay

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun TotpSetupScreen(
    modifier: Modifier = Modifier,
    username: String,
    onSetupComplete: () -> Unit = {},
    onBackClick: () -> Unit = {},
) {
    var secretKey by remember { mutableStateOf("") }
    var formattedSecretKey by remember { mutableStateOf("") }
    var testCode by remember { mutableStateOf("") }
    var timeLeft by remember { mutableIntStateOf(30) }
    var isLoading by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var showTestSection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val totpService = remember { TotpService(context) }
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Generate secret key on first load
    LaunchedEffect(Unit) {
        if (secretKey.isEmpty()) {
            secretKey = totpService.generateSecretKey(username)
            formattedSecretKey = totpService.formatSecretKey(secretKey)
        }
    }

    // Timer countdown and progress update
    LaunchedEffect(Unit) {
        while (true) {
            timeLeft = totpService.getRemainingTime()
            progress = totpService.getCycleProgress()
            delay(1000)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Logo
        Image(
            painter = painterResource(id = R.drawable.logo_body_scan),
            contentDescription = "Body Scan Logo",
            modifier = Modifier
                .size(200.dp)
                .padding(bottom = 20.dp)
        )

        // Title
        Text(
            text = "Set Up Two-Factor Authentication",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Follow these steps to set up TOTP with Google Authenticator",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Setup Instructions Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Setup Instructions:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                val instructions = listOf(
                    "1. Open Google Authenticator app",
                    "2. Tap the \"+\" button to add an account",
                    "3. Select \"Enter a setup key\"",
                    "4. Enter account name: \"BodyScanApp\"",
                    "5. Enter the secret key below",
                    "6. Tap \"Add\"",
                    "7. Return to this app and test the setup"
                )
                
                instructions.forEach { instruction ->
                    Text(
                        text = instruction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }

        // Secret Key Display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Your Secret Key:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedSecretKey,
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(secretKey))
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                Text(
                    text = "Tap the copy button to copy the key to your clipboard",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Test Section
        if (showTestSection) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Test Your Setup:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Text(
                        text = "Enter the 6-digit code from Google Authenticator to verify your setup:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "Tip: Long-press any box or use the paste button below to paste from clipboard",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // TOTP Code input - 6 individual functional text fields
                    val focusRequesters = remember { List(6) { FocusRequester() } }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .clickable {
                                // Focus on first empty field when row is tapped
                                val firstEmptyIndex = testCode.length.coerceAtMost(5)
                                focusRequesters[firstEmptyIndex].requestFocus()
                            },
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        repeat(6) { index ->
                            val digit = if (index < testCode.length) testCode[index].toString() else ""
                            
                            OutlinedTextField(
                                value = digit,
                                onValueChange = { newValue ->
                                    if (newValue.length <= 1 && (newValue.isEmpty() || newValue.all { it.isDigit() })) {
                                        // Update the specific digit
                                        val newTestCode = testCode.toMutableList()
                                        while (newTestCode.size <= index) {
                                            newTestCode.add(' ')
                                        }
                                        newTestCode[index] = if (newValue.isEmpty()) ' ' else newValue[0]
                                        
                                        // Remove trailing spaces and update
                                        val cleanCode = newTestCode.joinToString("").trimEnd()
                                        testCode = cleanCode
                                        
                                        if (newValue.isNotEmpty()) {
                                            // Move to next field if digit was entered
                                            if (index < 5) {
                                                focusRequesters[index + 1].requestFocus()
                                            } else {
                                                // All digits filled, submit
                                                focusManager.clearFocus()
                                                keyboardController?.hide()
                                            }
                                        } else {
                                            // Handle backspace - move to previous field
                                            if (index > 0) {
                                                focusRequesters[index - 1].requestFocus()
                                            }
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = if (index == 5) ImeAction.Done else ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = {
                                        if (index < 5) {
                                            focusRequesters[index + 1].requestFocus()
                                        }
                                    },
                                    onDone = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                    }
                                ),
                                modifier = Modifier
                                    .size(48.dp)
                                    .focusRequester(focusRequesters[index])
                                    .scale(if (digit.isNotEmpty()) 1.05f else 1f)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = {
                                                try {
                                                    val clipboardText = clipboardManager.getText()?.text ?: ""
                                                    if (clipboardText.length == 6 && clipboardText.all { it.isDigit() }) {
                                                        testCode = clipboardText
                                                        focusManager.clearFocus()
                                                        keyboardController?.hide()
                                                    }
                                                } catch (e: Exception) {
                                                    // Handle clipboard access errors silently
                                                }
                                            }
                                        )
                                    },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                ),
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    // Paste button for test code
                    Button(
                        onClick = {
                            try {
                                val clipboardText = clipboardManager.getText()?.text ?: ""
                                if (clipboardText.length == 6 && clipboardText.all { it.isDigit() }) {
                                    testCode = clipboardText
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                            } catch (e: Exception) {
                                // Handle clipboard access errors silently
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        enabled = testCode.length < 6
                    ) {
                        Text(
                            text = "Paste Code from Authenticator",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Test result message
                    testResult?.let { result ->
                        Text(
                            text = result,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (result.contains("Success")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    // Test button
                    Button(
                        onClick = {
                            isTesting = true
                            testResult = null
                            
                            when (val result = totpService.verifyTotpCode(username, testCode)) {
                                is TotpVerificationResult.Success -> {
                                    testResult = "✅ Success! TOTP setup is working correctly."
                                    totpService.markTotpSetup(username)
                                }
                                is TotpVerificationResult.Error -> {
                                    testResult = "❌ ${result.message}"
                                }
                            }
                            isTesting = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !isTesting && testCode.length == 6
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                text = "Test Setup",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Back button
            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(
                    text = "Back",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Continue button
            Button(
                onClick = {
                    if (!showTestSection) {
                        showTestSection = true
                    } else {
                        onSetupComplete()
                    }
                },
                modifier = Modifier
                    .weight(2f)
                    .height(55.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = if (showTestSection) "Complete Setup" else "Completed my setup",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // Regenerate secret key button
        TextButton(
            onClick = {
                secretKey = totpService.generateSecretKey(username)
                formattedSecretKey = totpService.formatSecretKey(secretKey)
                showTestSection = false
                testCode = ""
                testResult = null
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = "Generate New Secret Key",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TotpSetupScreenPreview() {
    BodyScanAppTheme {
        TotpSetupScreen(
            username = "testuser"
        )
    }
}
