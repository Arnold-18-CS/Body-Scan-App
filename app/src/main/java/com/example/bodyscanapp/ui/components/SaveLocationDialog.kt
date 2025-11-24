package com.example.bodyscanapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Save location options
 */
enum class SaveLocation {
    DEFAULT,      // Save to app's internal/external files directory
    DOWNLOADS,    // Save to Downloads folder (accessible to user)
    CUSTOM        // Let user choose location via file picker
}

/**
 * Dialog for selecting save location
 * 
 * @param onDismiss Callback when dialog is dismissed
 * @param onLocationSelected Callback when save location is selected
 */
@Composable
fun SaveLocationDialog(
    onDismiss: () -> Unit,
    onLocationSelected: (SaveLocation) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Choose Save Location",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Where would you like to save the scan?",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Default location option
                OutlinedButton(
                    onClick = { onLocationSelected(SaveLocation.DEFAULT) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = androidx.compose.ui.Alignment.Start
                    ) {
                        Text(
                            text = "Default Location",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "App's private storage",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                
                // Downloads folder option
                OutlinedButton(
                    onClick = { onLocationSelected(SaveLocation.DOWNLOADS) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = androidx.compose.ui.Alignment.Start
                    ) {
                        Text(
                            text = "Downloads Folder",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Easily accessible via file manager",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                
                // Custom location option
                OutlinedButton(
                    onClick = { onLocationSelected(SaveLocation.CUSTOM) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = androidx.compose.ui.Alignment.Start
                    ) {
                        Text(
                            text = "Choose Location",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pick a custom folder",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                )
            ) {
                Text("Cancel")
            }
        },
        dismissButton = null
    )
}

