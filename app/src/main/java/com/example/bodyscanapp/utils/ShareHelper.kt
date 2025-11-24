package com.example.bodyscanapp.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Helper class for sharing scan data via Android ShareSheet
 * 
 * Supports:
 * - Sharing scan results as JSON/CSV/PDF
 * - Sharing images
 * - Sharing via Android ShareSheet
 */
object ShareHelper {
    private const val AUTHORITY = "com.example.bodyscanapp.fileprovider"
    
    /**
     * Get FileProvider URI for a file
     * 
     * @param context Application context
     * @param file File to share
     * @return FileProvider URI
     */
    fun getFileProviderUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            AUTHORITY,
            file
        )
    }
    
    /**
     * Share a file via Android ShareSheet
     * 
     * @param context Application context
     * @param file File to share
     * @param mimeType MIME type of the file
     * @param title Title for the share dialog
     * @throws IllegalArgumentException if file doesn't exist
     * @throws IllegalStateException if FileProvider URI cannot be created
     */
    fun shareFile(
        context: Context,
        file: File,
        mimeType: String,
        title: String = "Share scan results"
    ) {
        // Verify file exists
        if (!file.exists()) {
            throw IllegalArgumentException("File does not exist: ${file.absolutePath}")
        }
        
        // Verify file is readable
        if (!file.canRead()) {
            throw IllegalArgumentException("File is not readable: ${file.absolutePath}")
        }
        
        try {
            val uri = getFileProviderUri(context, file)
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = mimeType
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooserIntent = Intent.createChooser(shareIntent, title)
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            chooserIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(chooserIntent)
        } catch (e: IllegalArgumentException) {
            // Re-throw with more context
            android.util.Log.e("ShareHelper", "Error creating FileProvider URI for file: ${file.absolutePath}", e)
            throw IllegalStateException("Failed to create FileProvider URI. Make sure file_paths.xml is properly configured. File path: ${file.absolutePath}", e)
        } catch (e: Exception) {
            android.util.Log.e("ShareHelper", "Error sharing file: ${file.absolutePath}", e)
            throw e
        }
    }
    
    /**
     * Share text content
     * 
     * @param context Application context
     * @param text Text to share
     * @param title Title for the share dialog
     */
    fun shareText(
        context: Context,
        text: String,
        title: String = "Share scan results"
    ) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        
        val chooserIntent = Intent.createChooser(shareIntent, title)
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    }
}




