package com.example.bodyscanapp.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Helper class for saving files to different locations
 * 
 * Supports:
 * - Default location (app's internal/external files directory)
 * - Downloads folder (accessible via file manager)
 * - Custom location (via Storage Access Framework)
 */
object FileSaveHelper {
    private const val TAG = "FileSaveHelper"
    
    /**
     * Save file to default location (app's external files directory)
     * 
     * @param context Application context
     * @param fileName Name of the file
     * @param data File data as ByteArray
     * @param subdirectory Optional subdirectory (e.g., "exports", "meshes")
     * @return File object representing the saved file
     */
    fun saveToDefaultLocation(
        context: Context,
        fileName: String,
        data: ByteArray,
        subdirectory: String? = null
    ): File {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val targetDir = if (subdirectory != null) {
            File(baseDir, subdirectory)
        } else {
            baseDir
        }
        targetDir.mkdirs()
        
        val file = File(targetDir, fileName)
        file.writeBytes(data)
        
        Log.d(TAG, "Saved to default location: ${file.absolutePath}")
        return file
    }
    
    /**
     * Save file to Downloads folder (accessible via file manager)
     * 
     * For Android 10+ (API 29+), uses MediaStore API
     * For older versions, uses direct file access
     * 
     * @param context Application context
     * @param fileName Name of the file
     * @param data File data as ByteArray
     * @param mimeType MIME type of the file (e.g., "application/json", "application/pdf")
     * @return Uri of the saved file, or null if failed
     */
    fun saveToDownloads(
        context: Context,
        fileName: String,
        data: ByteArray,
        mimeType: String
    ): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (API 29+) - Use MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                
                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(data)
                    }
                    Log.d(TAG, "Saved to Downloads via MediaStore: $uri")
                    it
                }
            } else {
                // Android 9 and below - Direct file access
                val downloadsDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )
                downloadsDir.mkdirs()
                
                val file = File(downloadsDir, fileName)
                file.writeBytes(data)
                
                // Get URI using FileProvider or direct file URI
                val uri = Uri.fromFile(file)
                Log.d(TAG, "Saved to Downloads (legacy): ${file.absolutePath}")
                uri
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to Downloads folder", e)
            null
        }
    }
    
    /**
     * Get MIME type from file extension
     */
    fun getMimeType(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "json" -> "application/json"
            "csv" -> "text/csv"
            "pdf" -> "application/pdf"
            "glb" -> "model/gltf-binary"
            else -> "application/octet-stream"
        }
    }
    
    /**
     * Save file to a custom location via URI (from Storage Access Framework)
     * 
     * @param context Application context
     * @param uri URI from file picker
     * @param data File data as ByteArray
     * @return true if successful, false otherwise
     */
    fun saveToCustomLocation(
        context: Context,
        uri: Uri,
        data: ByteArray
    ): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri, "w")?.use { outputStream ->
                outputStream.write(data)
            }
            Log.d(TAG, "Saved to custom location: $uri")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to custom location", e)
            false
        }
    }
}

