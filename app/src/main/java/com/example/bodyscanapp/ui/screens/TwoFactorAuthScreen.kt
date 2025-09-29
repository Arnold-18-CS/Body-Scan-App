package com.example.bodyscanapp.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bodyscanapp.R
import com.example.bodyscanapp.data.TotpService
import com.example.bodyscanapp.ui.theme.BodyScanAppTheme
import kotlinx.coroutines.delay

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoFactorAuthScreen(
    onVerifyClick: (String) -> Unit = {},
    onResendClick: () -> Unit = {},
    onSetupTotpClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var totpCode by remember { mutableStateOf("") }
    var timeLeft by remember { mutableIntStateOf(30) }
    var isLoading by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }

    val totpService = remember { TotpService() }
    val focusManager = LocalFocusManager.current

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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Logo
        Image(
            painter = painterResource(id = R.drawable.logo_body_scan),
            contentDescription = "Body Scan Logo",
            modifier = Modifier
                .size(360.dp)
                .padding(bottom = 20.dp)
        )

        // Title
        Text(
            text = "Two-Factor Authentication",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Enter the 6-digit code from your authenticator app",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Mock TOTP code for testing (remove in production)
        val mockCode = totpService.generateMockTotpCode()
        Text(
            text = "Test code: $mockCode (tap to use)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .clickable {
                    totpCode = mockCode
                    onVerifyClick(mockCode)
                }
        )

        // TOTP Code input - 6 individual functional text fields
        val focusRequesters = remember { List(6) { FocusRequester() } }
        val keyboardController = LocalSoftwareKeyboardController.current
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clickable {
                    // Focus on first empty field when row is tapped
                    val firstEmptyIndex = totpCode.length.coerceAtMost(5)
                    focusRequesters[firstEmptyIndex].requestFocus()
                },
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            
            repeat(6) { index ->
                val digit = if (index < totpCode.length) totpCode[index].toString() else ""
                val isError = false
                
                OutlinedTextField(
                    value = digit,
                    onValueChange = { newValue ->
                        if (newValue.length <= 1 && (newValue.isEmpty() || newValue.all { it.isDigit() })) {
                            // Update the specific digit
                            val newTotpCode = totpCode.toMutableList()
                            while (newTotpCode.size <= index) {
                                newTotpCode.add(' ')
                            }
                            newTotpCode[index] = if (newValue.isEmpty()) ' ' else newValue[0]
                            
                            // Remove trailing spaces and update
                            val cleanCode = newTotpCode.joinToString("").trimEnd()
                            totpCode = cleanCode
                            
                            if (newValue.isNotEmpty()) {
                                // Move to next field if digit was entered
                                if (index < 5) {
                                    focusRequesters[index + 1].requestFocus()
                                } else {
                                    // All digits filled, submit
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    onVerifyClick(cleanCode)
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
                            if (totpCode.length == 6) {
                                onVerifyClick(totpCode)
                            }
                        }
                    ),
                    modifier = Modifier
                        .size(56.dp)
                        .focusRequester(focusRequesters[index])
                        .scale(if (digit.isNotEmpty()) 1.05f else 1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    isError = isError,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        errorBorderColor = MaterialTheme.colorScheme.error,
                        focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                        errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                        unfocusedContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Progress indicator and timer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular progress indicator
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                // ---> THIS IS THE FIX <---
                CircularProgressIndicator(
                    progress = { progress }, // Changed from 'progress = progress'
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )
                Text(
                    text = timeLeft.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

//            Spacer(modifier = Modifier.width(16.dp))
//
//            Text(
//                text = "Time left: ${String.format("%02d:%02d", timeLeft / 60, timeLeft % 60)}",
//                style = MaterialTheme.typography.bodyMedium,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
        }


        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Resend button
            OutlinedButton(
                onClick = {
                    isResending = true
                    onResendClick()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                enabled = !isResending && timeLeft <= 5
            ) {
                if (isResending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "Resend",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Verify button
            Button(
                onClick = {
                    isLoading = true
                    onVerifyClick(totpCode)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                enabled = !isLoading && totpCode.length == 6
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "Verify",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Setup TOTP button
        TextButton(
            onClick = onSetupTotpClick,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = "Set up TOTP",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TwoFactorAuthScreenPreview() {
    BodyScanAppTheme {
        TwoFactorAuthScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun TwoFactorAuthScreenWithErrorPreview() {
    BodyScanAppTheme {
        TwoFactorAuthScreen()
    }
}
