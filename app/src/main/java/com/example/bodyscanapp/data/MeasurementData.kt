package com.example.bodyscanapp.data

/**
 * Represents a single body measurement
 *
 * @param name The name of the measurement (e.g., "Shoulder Width", "Waist")
 * @param value The measured value as a float
 * @param unit The unit of measurement (e.g., "cm", "inches")
 */
data class BodyMeasurement(
    val name: String,
    val value: Float,
    val unit: String = "cm"
) {
    /**
     * Returns a formatted display string for the measurement
     * Example: "Shoulder Width: 45.2 cm"
     */
    fun getDisplayString(): String {
        return "$name: ${String.format("%.1f", value)} $unit"
    }
}

/**
 * Represents the complete set of body measurements from a scan
 *
 * @param measurements List of individual body measurements
 * @param scanDate The date/time when the scan was taken
 * @param isSuccessful Whether the scan was successful
 * @param errorMessage Optional error message if scan failed
 */
data class MeasurementData(
    val measurements: List<BodyMeasurement>,
    val scanDate: Long = System.currentTimeMillis(),
    val isSuccessful: Boolean = true,
    val errorMessage: String? = null
)

/**
 * Generates mock measurement data for testing
 */
fun generateMockMeasurements(isSuccessful: Boolean = true): MeasurementData {
    return if (isSuccessful) {
        MeasurementData(
            measurements = listOf(
                BodyMeasurement("Shoulder Width", 45.2f),
                BodyMeasurement("Chest", 98.5f),
                BodyMeasurement("Waist", 82.3f),
                BodyMeasurement("Hips", 96.8f),
                BodyMeasurement("Arm Length", 58.4f),
                BodyMeasurement("Leg Length", 92.1f),
                BodyMeasurement("Neck Diameter", 36.7f)
            ),
            isSuccessful = true
        )
    } else {
        MeasurementData(
            measurements = emptyList(),
            isSuccessful = false,
            errorMessage = "Keypoints not detected. Please ensure proper lighting and full body visibility."
        )
    }
}



