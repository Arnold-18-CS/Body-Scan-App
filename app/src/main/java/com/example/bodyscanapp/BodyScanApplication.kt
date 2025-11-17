package com.example.bodyscanapp

import android.app.Application
import com.example.bodyscanapp.data.AppDatabase
import com.example.bodyscanapp.data.DatabaseModule
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
}
