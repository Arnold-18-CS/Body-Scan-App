package com.example.bodyscanapp

import android.app.Application
import com.example.bodyscanapp.data.AppDatabase
import com.example.bodyscanapp.data.DatabaseModule
import com.example.bodyscanapp.utils.MediaPipePoseHelper
import com.example.bodyscanapp.utils.NativeBridge
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Custom Application class for Body Scan App
 * Initializes Firebase services and Room database when the app starts
 */
class BodyScanApplication : Application() {
    
    // Database instance - accessible throughout the app
    val database: AppDatabase by lazy {
        DatabaseModule.getDatabase(this)
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        initializeFirebase()
        
        // Initialize Room database
        // Database is lazy-loaded, so it will be created on first access
        // This ensures it's initialized early in the app lifecycle
        initializeDatabase()
        
        // Initialize MediaPipe Pose Detector
        initializeMediaPipe()
    }
    
    /**
     * Initialize Firebase services
     * This method sets up Firebase App and Analytics
     */
    private fun initializeFirebase() {
        try {
            // Initialize Firebase App
            FirebaseApp.initializeApp(this)
            
            // Initialize Firebase Analytics
            FirebaseAnalytics.getInstance(this)
            
        } catch (e: Exception) {
            // Log error if Firebase initialization fails
            android.util.Log.e("BodyScanApp", "Firebase initialization failed", e)
        }
    }
    
    /**
     * Initialize Room database
     * Database is lazy-loaded, so this just ensures it's ready
     */
    private fun initializeDatabase() {
        try {
            // Access database to trigger initialization
            // This uses lazy initialization, so it's thread-safe
            database
            android.util.Log.d("BodyScanApp", "Database initialized successfully")
        } catch (e: Exception) {
            // Log error if database initialization fails
            android.util.Log.e("BodyScanApp", "Database initialization failed", e)
        }
    }
    
    /**
     * Initialize MediaPipe Pose Detector
     * This initializes both the Kotlin helper and the native C++ bridge
     * Initialization is done asynchronously to avoid blocking app startup
     */
    private fun initializeMediaPipe() {
        // Initialize MediaPipe on a background thread to avoid blocking app startup
        // This prevents crashes if native libraries are not immediately available
        Thread {
            try {
                // Initialize Kotlin MediaPipe helper
                val kotlinInitSuccess = MediaPipePoseHelper.initialize(this)
                
                // Initialize native C++ MediaPipe bridge
                val nativeInitSuccess = NativeBridge.initializeMediaPipe(this)
                
                if (kotlinInitSuccess && nativeInitSuccess) {
                    android.util.Log.i("BodyScanApp", "MediaPipe Pose Detector initialized successfully")
                } else {
                    android.util.Log.w("BodyScanApp", 
                        "MediaPipe initialization partially failed: Kotlin=$kotlinInitSuccess, Native=$nativeInitSuccess")
                }
            } catch (e: UnsatisfiedLinkError) {
                // Native library not found - log but don't crash
                android.util.Log.e("BodyScanApp", "MediaPipe native library not found. " +
                    "This may be due to missing native libraries in the APK. " +
                    "MediaPipe features will not be available.", e)
            } catch (e: Exception) {
                // Log error but don't crash - MediaPipe can be initialized later if needed
                android.util.Log.e("BodyScanApp", "MediaPipe initialization failed", e)
            }
        }.start()
    }
}
