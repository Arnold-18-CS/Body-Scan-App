package com.example.bodyscanapp.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * PerformanceLogger - Track and log UI action times and navigation latency
 *
 * Features:
 * - Non-blocking logging using coroutines
 * - Dual logging to console (logcat) and SharedPreferences
 * - Track action duration between start and end events
 * - Track navigation latency between screens
 * - Automatic timestamp formatting
 * - Thread-safe operation using ConcurrentHashMap
 * - No impact on UI responsiveness (all I/O on background threads)
 *
 * Usage Examples:
 * ```kotlin
 * // Track a single action
 * performanceLogger.logAction("button_click", "capture_button")
 *
 * // Track action duration
 * performanceLogger.startAction("image_capture")
 * // ... perform capture ...
 * performanceLogger.endAction("image_capture")
 *
 * // Track navigation
 * performanceLogger.logNavigation("HomeScreen", "HeightInputScreen")
 * ```
 *
 * @param context Application or Activity context for SharedPreferences access
 * @param enableSharedPrefsLogging Enable/disable persistent logging to SharedPreferences (default: true)
 * @param enableConsoleLogging Enable/disable console logging to logcat (default: true)
 */
class PerformanceLogger(
    private val context: Context,
    private val enableSharedPrefsLogging: Boolean = true,
    private val enableConsoleLogging: Boolean = true
) {
    private val sharedPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Use SupervisorJob to ensure one failed log doesn't cancel all logging
    private val loggingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Track ongoing actions to measure duration
    private val activeActions = ConcurrentHashMap<String, Long>()

    // Track navigation start times
    private val navigationStartTimes = ConcurrentHashMap<String, Long>()

    // Date formatter for human-readable timestamps
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    companion object {
        private const val TAG = "PerformanceLogger"
        private const val PREFS_NAME = "performance_logs"
        private const val KEY_LOG_COUNT = "log_count"
        private const val KEY_LOG_PREFIX = "log_"
        private const val MAX_LOGS = 100 // Keep last 100 logs in SharedPreferences

        // Singleton instance
        @Volatile
        private var INSTANCE: PerformanceLogger? = null

        /**
         * Get singleton instance of PerformanceLogger
         * @param context Application context
         */
        fun getInstance(context: Context): PerformanceLogger {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PerformanceLogger(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    /**
     * Log a single action with timestamp
     * Non-blocking - returns immediately
     *
     * @param actionType Type of action (e.g., "button_click", "navigation", "capture")
     * @param actionName Name of the action (e.g., "capture_button", "HomeScreen")
     * @param metadata Optional additional metadata
     */
    fun logAction(actionType: String, actionName: String, metadata: String? = null) {
        val timestamp = System.currentTimeMillis()
        val formattedTime = dateFormatter.format(Date(timestamp))
        val metadataStr = metadata?.let { " | metadata: $it" } ?: ""

        val logEntry = "[$formattedTime] $actionType: $actionName$metadataStr"

        // Console logging (synchronous, fast)
        if (enableConsoleLogging) {
            Log.d(TAG, logEntry)
        }

        // SharedPreferences logging (async, non-blocking)
        if (enableSharedPrefsLogging) {
            loggingScope.launch {
                try {
                    saveLogToPrefs(logEntry)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save log to SharedPreferences", e)
                }
            }
        }
    }

    /**
     * Start tracking an action's duration
     * Call endAction() with the same actionId to log the duration
     *
     * @param actionId Unique identifier for the action
     */
    fun startAction(actionId: String) {
        val timestamp = System.currentTimeMillis()
        activeActions[actionId] = timestamp

        if (enableConsoleLogging) {
            Log.d(TAG, "Action started: $actionId at ${dateFormatter.format(Date(timestamp))}")
        }
    }

    /**
     * End tracking an action and log its duration
     * Must be called after startAction() with the same actionId
     *
     * @param actionId Unique identifier for the action
     * @param metadata Optional additional metadata
     * @return Duration in milliseconds, or null if action was not started
     */
    fun endAction(actionId: String, metadata: String? = null): Long? {
        val endTime = System.currentTimeMillis()
        val startTime = activeActions.remove(actionId)

        if (startTime == null) {
            if (enableConsoleLogging) {
                Log.w(TAG, "Action end called for non-started action: $actionId")
            }
            return null
        }

        val duration = endTime - startTime
        val formattedEndTime = dateFormatter.format(Date(endTime))
        val metadataStr = metadata?.let { " | metadata: $it" } ?: ""

        val logEntry = "[$formattedEndTime] Action completed: $actionId | duration: ${duration}ms$metadataStr"

        // Console logging
        if (enableConsoleLogging) {
            Log.d(TAG, logEntry)
        }

        // SharedPreferences logging (async)
        if (enableSharedPrefsLogging) {
            loggingScope.launch {
                try {
                    saveLogToPrefs(logEntry)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save log to SharedPreferences", e)
                }
            }
        }

        return duration
    }

    /**
     * Log navigation from one screen to another
     * Tracks the latency of screen transitions
     *
     * @param fromScreen Source screen name
     * @param toScreen Destination screen name
     */
    fun logNavigation(fromScreen: String, toScreen: String) {
        val timestamp = System.currentTimeMillis()
        val navigationKey = "$fromScreen->$toScreen"

        // Check if we have a previous navigation end time to calculate latency
        val previousEndTime = navigationStartTimes[fromScreen]
        val latencyStr = if (previousEndTime != null) {
            val latency = timestamp - previousEndTime
            " | latency: ${latency}ms"
        } else {
            ""
        }

        // Store this navigation's start time
        navigationStartTimes[toScreen] = timestamp

        val formattedTime = dateFormatter.format(Date(timestamp))
        val logEntry = "[$formattedTime] Navigation: $navigationKey$latencyStr"

        // Console logging
        if (enableConsoleLogging) {
            Log.d(TAG, logEntry)
        }

        // SharedPreferences logging (async)
        if (enableSharedPrefsLogging) {
            loggingScope.launch {
                try {
                    saveLogToPrefs(logEntry)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save log to SharedPreferences", e)
                }
            }
        }
    }

    /**
     * Mark when a screen becomes visible
     * Used to track screen load times and navigation latency
     *
     * @param screenName Name of the screen
     */
    fun markScreenVisible(screenName: String) {
        val timestamp = System.currentTimeMillis()
        navigationStartTimes[screenName] = timestamp

        if (enableConsoleLogging) {
            val formattedTime = dateFormatter.format(Date(timestamp))
            Log.d(TAG, "[$formattedTime] Screen visible: $screenName")
        }
    }

    /**
     * Save a log entry to SharedPreferences (called on background thread)
     * Implements circular buffer to keep only the last MAX_LOGS entries
     */
    private fun saveLogToPrefs(logEntry: String) {
        val editor = sharedPrefs.edit()

        // Get current log count
        val currentCount = sharedPrefs.getInt(KEY_LOG_COUNT, 0)
        val nextIndex = currentCount % MAX_LOGS

        // Save the log entry
        editor.putString("$KEY_LOG_PREFIX$nextIndex", logEntry)
        editor.putInt(KEY_LOG_COUNT, currentCount + 1)

        // Use apply() instead of commit() for async, non-blocking write
        editor.apply()
    }

    /**
     * Retrieve all performance logs from SharedPreferences
     * Sorted by timestamp (oldest first)
     *
     * @return List of log entries
     */
    fun getAllLogs(): List<String> {
        val logs = mutableListOf<String>()
        val totalCount = sharedPrefs.getInt(KEY_LOG_COUNT, 0)
        val logsToRead = minOf(totalCount, MAX_LOGS)

        // Calculate starting index for circular buffer
        val startIndex = if (totalCount > MAX_LOGS) {
            totalCount % MAX_LOGS
        } else {
            0
        }

        // Read logs in order (oldest to newest)
        for (i in 0 until logsToRead) {
            val index = (startIndex + i) % MAX_LOGS
            val log = sharedPrefs.getString("$KEY_LOG_PREFIX$index", null)
            if (log != null) {
                logs.add(log)
            }
        }

        return logs
    }

    /**
     * Get the most recent N logs
     *
     * @param count Number of logs to retrieve
     * @return List of recent log entries (newest first)
     */
    fun getRecentLogs(count: Int): List<String> {
        val allLogs = getAllLogs()
        return allLogs.takeLast(count).reversed()
    }

    /**
     * Clear all performance logs from SharedPreferences
     */
    fun clearLogs() {
        loggingScope.launch {
            try {
                sharedPrefs.edit().clear().apply()
                if (enableConsoleLogging) {
                    Log.d(TAG, "Performance logs cleared")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear logs", e)
            }
        }
    }

    /**
     * Print all logs to console
     * Useful for debugging
     */
    fun printAllLogs() {
        val logs = getAllLogs()
        Log.d(TAG, "========== Performance Logs (${logs.size}) ==========")
        logs.forEach { log ->
            Log.d(TAG, log)
        }
        Log.d(TAG, "========== End Performance Logs ==========")
    }

    /**
     * Get statistics about tracked actions
     *
     * @return Map of action types to count
     */
    fun getStatistics(): Map<String, Int> {
        val logs = getAllLogs()
        val stats = mutableMapOf<String, Int>()

        logs.forEach { log ->
            // Extract action type from log entry
            val match = Regex("""\] (\w+):""").find(log)
            match?.groupValues?.getOrNull(1)?.let { actionType ->
                stats[actionType] = stats.getOrDefault(actionType, 0) + 1
            }
        }

        return stats
    }
}


