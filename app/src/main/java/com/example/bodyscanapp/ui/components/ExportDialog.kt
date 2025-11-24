package com.example.bodyscanapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
 * Export format options
 */
enum class ExportFormat {
    JSON,
    CSV,
    PDF
}

/**
 * Export dialog for selecting export format
 * 
 * @param onDismiss Callback when dialog is dismissed
 * @param onExportSelected Callback when export format is selected
 */
@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onExportSelected: (ExportFormat) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Export Scan Results",
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
                    text = "Choose export format:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // JSON option
                OutlinedButton(
                    onClick = { onExportSelected(ExportFormat.JSON) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export as JSON")
                }
                
                // CSV option
                OutlinedButton(
                    onClick = { onExportSelected(ExportFormat.CSV) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export as CSV")
                }
                
                // PDF option
                OutlinedButton(
                    onClick = { onExportSelected(ExportFormat.PDF) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export as PDF")
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

