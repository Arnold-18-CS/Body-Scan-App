package com.example.bodyscanapp.data

import android.content.Context
import androidx.room.Room

/**
 * Database module providing singleton AppDatabase instance
 * 
 * Uses double-checked locking pattern for thread-safe singleton
 * For production: consider using Dependency Injection (Hilt/Koin)
 */
object DatabaseModule {
    @Volatile
    private var INSTANCE: AppDatabase? = null
    
    /**
     * Get or create database instance
     * 
     * @param context Application context (use applicationContext to avoid leaks)
     * @return AppDatabase singleton instance
     */
    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "bodyscan_database"
            )
                .fallbackToDestructiveMigration(true) // For development only - drops all tables on migration
                .build()
            INSTANCE = instance
            instance
        }
    }
}

