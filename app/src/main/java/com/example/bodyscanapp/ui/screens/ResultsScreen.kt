package com.example.bodyscanapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bodyscanapp.data.BodyMeasurement
import com.example.bodyscanapp.data.MeasurementData
import com.example.bodyscanapp.data.generateMockMeasurements
import com.example.bodyscanapp.ui.theme.BodyScanAppTheme
import com.example.bodyscanapp.ui.theme.BodyScanBackground

/**
 * ResultsScreen - Displays body scan measurement results
 *
 * Shows a list of measurements with options to save, recapture, or export the results.
 * Handles error states when measurements fail.
 *
 * @param modifier Modifier for the screen
 * @param measurementData The measurement data to display (can be null for loading state)
 * @param onSaveClick Callback when save button is clicked
 * @param onRecaptureClick Callback when recapture button is clicked
 * @param onExportClick Callback when export button is clicked
 */
@Composable
fun ResultsScreen(
    modifier: Modifier = Modifier,
    measurementData: MeasurementData? = generateMockMeasurements(),
    onSaveClick: () -> Unit = {},
    onRecaptureClick: () -> Unit = {},
    onExportClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BodyScanBackground)
    ) {
        if (measurementData == null) {
            // Loading state
            LoadingResultsContent()
        } else if (!measurementData.isSuccessful) {
            // Error state
            ErrorResultsContent(
                errorMessage = measurementData.errorMessage ?: "Unknown error occurred",
                onRecaptureClick = onRecaptureClick
            )
        } else {
            // Success state with measurements
            SuccessResultsContent(
                measurements = measurementData.measurements,
                onSaveClick = onSaveClick,
                onRecaptureClick = onRecaptureClick,
                onExportClick = onExportClick
            )
        }
    }
}

/**
 * Content displayed when results are successfully loaded
 */
@Composable
private fun SuccessResultsContent(
    measurements: List<BodyMeasurement>,
    onSaveClick: () -> Unit,
    onRecaptureClick: () -> Unit,
    onExportClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Your Results are here!!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(vertical = 16.dp)
                .semantics { contentDescription = "Results are ready" }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Measurement List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .semantics { contentDescription = "List of body measurements" },
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(measurements) { measurement ->
                MeasurementCard(measurement = measurement)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Recapture Button
            OutlinedButton(
                onClick = onRecaptureClick,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .semantics { contentDescription = "Recapture body scan" },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF424242)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Recapture",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Export Button
            Button(
                onClick = onExportClick,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .semantics { contentDescription = "Export measurements" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF424242),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Export",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Save Button (Full Width)
        Button(
            onClick = onSaveClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .semantics { contentDescription = "Save measurements to history" },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2196F3),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Save",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Individual measurement card component
 */
@Composable
private fun MeasurementCard(
    measurement: BodyMeasurement,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { 
                contentDescription = "${measurement.name}: ${measurement.value} ${measurement.unit}"
            },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Measurement Name
            Text(
                text = measurement.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF424242)
            )

            // Measurement Value
            Text(
                text = "${String.format("%.1f", measurement.value)} ${measurement.unit}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3),
                fontSize = 18.sp
            )
        }
    }
}

/**
 * Content displayed when there's an error in processing
 */
@Composable
private fun ErrorResultsContent(
    errorMessage: String,
    onRecaptureClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Error Icon
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error",
            tint = Color(0xFFF44336),
            modifier = Modifier
                .width(80.dp)
                .height(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Error Title
        Text(
            text = "Detection Failed",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Error Message
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFEBEE)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFC62828),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(20.dp)
                    .semantics { contentDescription = "Error: $errorMessage" }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Try Again Button
        Button(
            onClick = onRecaptureClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .semantics { contentDescription = "Try capturing again" },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2196F3),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Try Again",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Loading state content (optional - for future use)
 */
@Composable
private fun LoadingResultsContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Loading results...",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center
        )
    }
}

// Preview Composables
@Preview(showBackground = true, name = "Results Screen - Success")
@Composable
fun ResultsScreenSuccessPreview() {
    BodyScanAppTheme {
        ResultsScreen(
            measurementData = generateMockMeasurements(isSuccessful = true)
        )
    }
}

@Preview(showBackground = true, name = "Results Screen - Error")
@Composable
fun ResultsScreenErrorPreview() {
    BodyScanAppTheme {
        ResultsScreen(
            measurementData = generateMockMeasurements(isSuccessful = false)
        )
    }
}

@Preview(showBackground = true, name = "Results Screen - Loading")
@Composable
fun ResultsScreenLoadingPreview() {
    BodyScanAppTheme {
        ResultsScreen(
            measurementData = null
        )
    }
}

@Preview(showBackground = true, name = "Measurement Card")
@Composable
fun MeasurementCardPreview() {
    BodyScanAppTheme {
        MeasurementCard(
            measurement = BodyMeasurement("Shoulder Width", 45.2f),
            modifier = Modifier.padding(16.dp)
        )
    }
}

