package com.example.bodyscanapp.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bodyscanapp.R
import com.example.bodyscanapp.ui.theme.BodyScanAppTheme
import kotlinx.coroutines.delay

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoFactorAuthScreen(
    onVerifyClick: (String) -> Unit = {},
    onResendClick: () -> Unit = {},
    onSetupTotpClick: () -> Unit = {},
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    var totpCode by remember { mutableStateOf("") }
    var timeLeft by remember { mutableIntStateOf(30) }
    var isLoading by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }

    // Timer countdown
    LaunchedEffect(timeLeft) {
        if (timeLeft > 0) {
            delay(1000)
            timeLeft--
        } else {
            timeLeft = 30 // Reset timer
        }
    }

    val progress = timeLeft / 30f

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
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // TOTP Code input (6 digits)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(6) { index ->
                val digit = if (index < totpCode.length) totpCode[index].toString() else ""
                val isFocused = index == totpCode.length
                
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = when {
                                isFocused -> MaterialTheme.colorScheme.primary
                                digit.isNotEmpty() -> MaterialTheme.colorScheme.outline
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            },
                            shape = CircleShape
                        )
                        .background(
                            if (isFocused) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                            else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = digit,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

//        // Hidden text field for input
//        OutlinedTextField(
//            value = totpCode,
//            onValueChange = { newValue ->
//                if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
//                    totpCode = newValue
//                }
//            },
//            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(0.dp),
//            singleLine = true
//        )

        // Timer and Progress
            Text(
                text = "Time left: ${String.format("%02d:%02d", timeLeft / 60, timeLeft % 60)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )


        // Error message
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )
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
        TwoFactorAuthScreen(
            errorMessage = "Invalid code. Please try again."
        )
    }
}
