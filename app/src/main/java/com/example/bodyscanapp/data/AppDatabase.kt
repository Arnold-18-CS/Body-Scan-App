package com.example.bodyscanapp.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.bodyscanapp.data.dao.ScanDao
import com.example.bodyscanapp.data.dao.UserDao
import com.example.bodyscanapp.data.entity.Scan
import com.example.bodyscanapp.data.entity.User

/**
 * Room database for Body Scan App
 * 
 * Manages:
 * - User entities (linked to Firebase)
 * - Scan entities (body scan results)
 * 
 * Version 1 - initial schema
 * For development: uses fallbackToDestructiveMigration()
 * For production: implement proper migrations
 */
@Database(
    entities = [User::class, Scan::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun scanDao(): ScanDao
}

