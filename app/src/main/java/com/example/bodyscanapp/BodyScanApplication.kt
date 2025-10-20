package com.example.bodyscanapp

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Custom Application class for Body Scan App
 * Initializes Firebase services when the app starts
 */
class BodyScanApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        initializeFirebase()
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
}
