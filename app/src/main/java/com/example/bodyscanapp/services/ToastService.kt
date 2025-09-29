package com.example.bodyscanapp.services

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Unified Toast Service for showing success, error, and info messages
 * Provides consistent user feedback across the app
 */
class ToastService(private val context: Context) {
    
    /**
     * Shows a success toast message
     * @param message The success message to display
     * @param duration Toast duration (default: Toast.LENGTH_SHORT)
     */
    fun showSuccess(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, "✅ $message", duration).show()
    }
    
    /**
     * Shows an error toast message
     * @param message The error message to display
     * @param duration Toast duration (default: Toast.LENGTH_LONG)
     */
    fun showError(message: String, duration: Int = Toast.LENGTH_LONG) {
        Toast.makeText(context, "❌ $message", duration).show()
    }
    
    /**
     * Shows an info toast message
     * @param message The info message to display
     * @param duration Toast duration (default: Toast.LENGTH_SHORT)
     */
    fun showInfo(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, "ℹ️ $message", duration).show()
    }
    
    /**
     * Shows a warning toast message
     * @param message The warning message to display
     * @param duration Toast duration (default: Toast.LENGTH_SHORT)
     */
    fun showWarning(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, "⚠️ $message", duration).show()
    }
}

/**
 * Composable function to show toast messages in Compose UI
 * @param message The message to display
 * @param type The type of toast (success, error, info, warning)
 * @param duration Toast duration (default: Toast.LENGTH_SHORT)
 */
@Composable
fun ShowToast(
    message: String?,
    type: ToastType = ToastType.INFO,
    duration: Int = Toast.LENGTH_SHORT
) {
    val context = LocalContext.current
    val toastService = remember { ToastService(context) }
    
    LaunchedEffect(message) {
        message?.let { msg ->
            when (type) {
                ToastType.SUCCESS -> toastService.showSuccess(msg, duration)
                ToastType.ERROR -> toastService.showError(msg, duration)
                ToastType.INFO -> toastService.showInfo(msg, duration)
                ToastType.WARNING -> toastService.showWarning(msg, duration)
            }
        }
    }
}

/**
 * Enum for different toast types
 */
enum class ToastType {
    SUCCESS, ERROR, INFO, WARNING
}
