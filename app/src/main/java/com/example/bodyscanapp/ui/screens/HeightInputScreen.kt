package com.example.bodyscanapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bodyscanapp.data.HeightData
import com.example.bodyscanapp.data.MeasurementUnit
import com.example.bodyscanapp.data.validateHeight
import com.example.bodyscanapp.ui.theme.BodyScanAppTheme
import com.example.bodyscanapp.ui.theme.BodyScanBackground
import kotlin.math.roundToInt

@Composable
fun HeightInputScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onProceedClick: (HeightData) -> Unit = {}
) {
    var selectedUnit by remember { mutableStateOf(MeasurementUnit.FEET_INCHES) }
    var heightText by remember { mutableStateOf("") } // Empty for feet/inches (slider only)
    var heightSliderValue by remember { mutableFloatStateOf(5.75f) } // Default to 5'9"
    var heightError by remember { mutableStateOf<String?>(null) }
    var isHeightValid by remember { mutableStateOf(false) }
    
    // Update validation when unit or value changes
    fun updateValidation() {
        val (isValid, error) = validateHeight(heightSliderValue, selectedUnit)
        isHeightValid = isValid
        heightError = error
    }
    
    // Initialize validation
    updateValidation()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BodyScanBackground)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = "Enter Your Height",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Choose your preferred measurement unit and enter your height",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // Unit Selection Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Choose your preferred measurement unit:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Column(
                    modifier = Modifier.selectableGroup()
                ) {
                    // Centimeters option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedUnit == MeasurementUnit.CENTIMETERS,
                                onClick = { 
                                    selectedUnit = MeasurementUnit.CENTIMETERS
                                    heightSliderValue = 175f
                                    heightText = "175"
                                    updateValidation()
                                },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedUnit == MeasurementUnit.CENTIMETERS,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Centimeters (cm)",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Feet and Inches option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedUnit == MeasurementUnit.FEET_INCHES,
                                onClick = { 
                                    selectedUnit = MeasurementUnit.FEET_INCHES
                                    heightSliderValue = 5.75f // 5'9"
                                    heightText = "" // Empty for feet/inches since we only use slider
                                    updateValidation()
                                },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedUnit == MeasurementUnit.FEET_INCHES,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Feet and Inches (ft'in\")",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        
        // Height Input Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Enter Your Height",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Show input field only for centimeters
                if (selectedUnit == MeasurementUnit.CENTIMETERS) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // TextField for height (centimeters only)
                        OutlinedTextField(
                            value = heightText,
                            onValueChange = { newValue ->
                                heightText = newValue
                                try {
                                    val floatValue = newValue.toFloatOrNull()
                                    if (floatValue != null) {
                                        heightSliderValue = floatValue
                                        updateValidation()
                                    }
                                } catch (e: NumberFormatException) {
                                    // Handle invalid input
                                }
                            },
                            label = { 
                                Text("Height (cm)")
                            },
                            isError = heightError != null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Display current value
                        Text(
                            text = HeightData(heightSliderValue, selectedUnit).getDisplayValue(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(100.dp),
                            textAlign = TextAlign.End
                        )
                    }
                } else {
                    // For feet/inches, show only the display value
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = HeightData(heightSliderValue, selectedUnit).getDisplayValue(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                // Error message
                if (heightError != null) {
                    Text(
                        text = heightError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Slider for height (only for feet/inches)
                if (selectedUnit == MeasurementUnit.FEET_INCHES) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val (minValue, maxValue) = 3.0f to 8.25f // 3'0" to 8'3"
                        
                        Slider(
                            value = heightSliderValue,
                            onValueChange = { newValue ->
                                heightSliderValue = newValue
                                updateValidation()
                            },
                            valueRange = minValue..maxValue,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Range labels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "3'0\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "8'3\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        
        // Proceed Button
        Button(
            onClick = {
                if (isHeightValid) {
                    val heightData = HeightData(heightSliderValue, selectedUnit)
                    onProceedClick(heightData)
                }
            },
            enabled = isHeightValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.outline,
                disabledContentColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Proceed to Scan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Conversion info
        if (isHeightValid) {
            val heightData = HeightData(heightSliderValue, selectedUnit)
            val cmValue = heightData.toCentimeters()
            
            Text(
                text = "Height: ${heightData.getDisplayValue()} (${cmValue.roundToInt()} cm)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HeightInputScreenPreview() {
    BodyScanAppTheme {
        HeightInputScreen()
    }
}
