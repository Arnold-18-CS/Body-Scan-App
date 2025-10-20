package com.example.bodyscanapp.utils

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Utility class for Firebase operations
 * Provides easy access to Firebase services throughout the app
 */
object FirebaseUtils {
    
    /**
     * Get Firebase App instance
     * @return FirebaseApp instance or null if not initialized
     */
    fun getFirebaseApp(): FirebaseApp? {
        return try {
            FirebaseApp.getInstance()
        } catch (e: IllegalStateException) {
            null
        }
    }
    
    /**
     * Get Firebase Auth instance
     * @return FirebaseAuth instance
     */
    fun getAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
    
    /**
     * Get Firebase Firestore instance
     * @return FirebaseFirestore instance
     */
    fun getFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    /**
     * Get Google Sign-In client
     * @param context Application context
     * @return GoogleSignInClient instance
     */
    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(com.example.bodyscanapp.R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }
    
    /**
     * Check if Firebase is properly initialized
     * @return true if Firebase is initialized, false otherwise
     */
    fun isFirebaseInitialized(): Boolean {
        return getFirebaseApp() != null
    }
}
