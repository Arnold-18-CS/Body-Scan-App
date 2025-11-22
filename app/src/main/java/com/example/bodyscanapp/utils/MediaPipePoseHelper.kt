package com.example.bodyscanapp.utils

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Helper class to interface with MediaPipe Pose Landmarker from C++ via JNI.
 * This class manages the MediaPipe PoseLandmarker instance and provides methods
 * to detect pose landmarks from images.
 */
object MediaPipePoseHelper {
    private var poseLandmarker: PoseLandmarker? = null
    private var isInitialized = false
    
    /**
     * Initialize MediaPipe Pose Landmarker with the model from assets.
     * The model file should be placed in assets/pose_landmarker.task
     * 
     * @param context Android context
     * @return true if initialization successful, false otherwise
     */
    @JvmStatic
    fun initialize(context: Context): Boolean {
        if (isInitialized && poseLandmarker != null) {
            return true
        }
        
        return try {
            // Try to load model from assets
            // First, copy model to internal storage if needed
            val modelFile = File(context.filesDir, "pose_landmarker.task")
            var modelPath: String? = null
            
            if (modelFile.exists()) {
                // Use existing file
                modelPath = modelFile.absolutePath
            } else {
                // Try to copy from assets
                try {
                    context.assets.open("pose_landmarker.task").use { input ->
                        FileOutputStream(modelFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    modelPath = modelFile.absolutePath
                } catch (e: Exception) {
                    // Model not in assets - try to use asset path directly
                    // Some MediaPipe setups allow direct asset path
                    try {
                        modelPath = "pose_landmarker.task" // Try asset path
                    } catch (e2: Exception) {
                        android.util.Log.e("MediaPipePoseHelper", "Failed to find pose model", e2)
                        return false
                    }
                }
            }
            
            if (modelPath == null) {
                android.util.Log.e("MediaPipePoseHelper", "Model path is null")
                return false
            }
            
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(modelPath)
                .build()
            
            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .setMinPoseDetectionConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setOutputSegmentationMasks(true) // Enable segmentation masks for pixel-level measurements
                .build()
            
            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
            isInitialized = true
            android.util.Log.i("MediaPipePoseHelper", "MediaPipe Pose Landmarker initialized successfully")
            true
        } catch (e: Exception) {
            android.util.Log.e("MediaPipePoseHelper", "Failed to initialize MediaPipe", e)
            e.printStackTrace()
            isInitialized = false
            false
        }
    }
    
    /**
     * Detect pose landmarks from a bitmap image.
     * 
     * @param bitmap Input image bitmap (must be ARGB_8888 format)
     * @return PoseLandmarkerResult or null if detection fails
     */
    @JvmStatic
    fun detectPose(bitmap: Bitmap): PoseLandmarkerResult? {
        if (!isInitialized || poseLandmarker == null) {
            return null
        }
        
        return try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            poseLandmarker?.detect(mpImage)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Extract 33 MediaPipe landmarks as a flat float array [x1, y1, z1, x2, y2, z2, ...]
     * Returns normalized coordinates (0-1 range).
     * 
     * @param result PoseLandmarkerResult from detection
     * @return FloatArray of 33*3 = 99 floats, or null if no pose detected
     */
    @JvmStatic
    fun extractLandmarks(result: PoseLandmarkerResult?): FloatArray? {
        if (result == null) {
            return null
        }
        
        val landmarks = result.landmarks()
        if (landmarks.isEmpty()) {
            return null
        }
        
        // Get first detected pose (MediaPipe can detect multiple poses)
        val pose = landmarks[0]
        if (pose.size < 33) {
            return null
        }
        
        // Extract 33 landmarks as [x1, y1, z1, x2, y2, z2, ...]
        val output = FloatArray(33 * 3)
        for (i in 0 until 33) {
            val landmark = pose[i]
            output[i * 3 + 0] = landmark.x()  // normalized x (0-1)
            output[i * 3 + 1] = landmark.y()    // normalized y (0-1)
            output[i * 3 + 2] = landmark.z()   // depth (relative)
        }
        
        return output
    }
    
    /**
     * Extract segmentation mask data from PoseLandmarkerResult.
     * Returns a FloatArray representing the mask (0.0 = background, 1.0 = person).
     * 
     * @param result PoseLandmarkerResult from detection
     * @return FloatArray of mask data, or null if no mask
     */
    @JvmStatic
    fun extractSegmentationMaskData(result: PoseLandmarkerResult?): FloatArray? {
        if (result == null) {
            return null
        }
        
        val masksOpt = result.segmentationMasks()
        if (!masksOpt.isPresent) {
            return null
        }
        
        val masks = masksOpt.get()
        if (masks.isEmpty()) {
            return null
        }
        
        val mask = masks[0] // Get first mask
        val width = mask.width
        val height = mask.height
        
        // Extract mask data as float array
        // MediaPipe segmentation masks are typically VEC32F1 format (single channel float)
        val maskData = FloatArray(width * height)
        
        try {
            // Get the mask buffer using ByteBufferExtractor
            val byteBuffer = ByteBufferExtractor.extract(mask)
            byteBuffer.order(ByteOrder.nativeOrder())
            byteBuffer.rewind()
            
            // Read float values from buffer
            val floatBuffer = byteBuffer.asFloatBuffer()
            floatBuffer.get(maskData)
        } catch (e: Exception) {
            android.util.Log.e("MediaPipePoseHelper", "Error extracting segmentation mask: ${e.message}", e)
            return null
        }
        
        return maskData
    }
    
    /**
     * Get segmentation mask width from PoseLandmarkerResult.
     * 
     * @param result PoseLandmarkerResult from detection
     * @return Width of mask, or 0 if no mask
     */
    @JvmStatic
    fun getSegmentationMaskWidth(result: PoseLandmarkerResult?): Int {
        if (result == null) {
            return 0
        }
        val masksOpt = result.segmentationMasks()
        return if (masksOpt.isPresent && masksOpt.get().isNotEmpty()) {
            masksOpt.get()[0].width
        } else {
            0
        }
    }
    
    /**
     * Get segmentation mask height from PoseLandmarkerResult.
     * 
     * @param result PoseLandmarkerResult from detection
     * @return Height of mask, or 0 if no mask
     */
    @JvmStatic
    fun getSegmentationMaskHeight(result: PoseLandmarkerResult?): Int {
        if (result == null) {
            return 0
        }
        val masksOpt = result.segmentationMasks()
        return if (masksOpt.isPresent && masksOpt.get().isNotEmpty()) {
            masksOpt.get()[0].height
        } else {
            0
        }
    }
    
    /**
     * Check if MediaPipe is initialized and ready to use.
     */
    @JvmStatic
    fun isReady(): Boolean {
        return isInitialized && poseLandmarker != null
    }
    
    /**
     * Release MediaPipe resources.
     */
    @JvmStatic
    fun release() {
        try {
            poseLandmarker?.close()
            poseLandmarker = null
            isInitialized = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

