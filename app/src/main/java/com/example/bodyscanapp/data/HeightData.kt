package com.example.bodyscanapp.data

import kotlin.math.roundToInt

enum class MeasurementUnit {
    CENTIMETERS, FEET_INCHES, METERS
}

data class HeightData(
    val value: Float,
    val unit: MeasurementUnit
) {
    fun toCentimeters(): Float {
        return when (unit) {
            MeasurementUnit.CENTIMETERS -> value
            MeasurementUnit.METERS -> value * 100
            MeasurementUnit.FEET_INCHES -> {
                val feet = value.toInt()
                val inches = ((value - feet) * 12).roundToInt()
                feet * 30.48f + inches * 2.54f
            }
        }
    }
    
    fun getDisplayValue(): String {
        return when (unit) {
            MeasurementUnit.CENTIMETERS -> "${value.roundToInt()} cm"
            MeasurementUnit.METERS -> "${String.format("%.2f", value)} m"
            MeasurementUnit.FEET_INCHES -> {
                val feet = value.toInt()
                val inches = ((value - feet) * 12).roundToInt()
                "$feet'$inches\""
            }
        }
    }
}

// Validation logic for height input
fun validateHeight(value: Float, unit: MeasurementUnit): Pair<Boolean, String?> {
    val cmValue = when (unit) {
        MeasurementUnit.CENTIMETERS -> value
        MeasurementUnit.METERS -> value * 100
        MeasurementUnit.FEET_INCHES -> {
            val feet = value.toInt()
            val inches = ((value - feet) * 12).roundToInt()
            feet * 30.48f + inches * 2.54f
        }
    }
    
    return when {
        cmValue < 100 -> false to "Height must be at least 100 cm (3'3\")"
        cmValue > 250 -> false to "Height must not exceed 250 cm (8'2\")"
        else -> true to null
    }
}
