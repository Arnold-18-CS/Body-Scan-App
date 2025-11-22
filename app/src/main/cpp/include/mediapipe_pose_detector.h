#ifndef MEDIAPIPE_POSE_DETECTOR_H
#define MEDIAPIPE_POSE_DETECTOR_H

#include <opencv2/opencv.hpp>
#include <vector>
#include <jni.h>

/**
 * MediaPipe Pose Detector wrapper class.
 * 
 * This class provides a C++ interface to MediaPipe Pose Landmarker
 * by using JNI to call the Kotlin MediaPipePoseHelper.
 * 
 * MediaPipe provides 33 landmarks, which are mapped to 135 keypoints
 * for compatibility with the existing codebase.
 */
class MediaPipePoseDetector {
public:
    /**
     * Initialize MediaPipe Pose Detector.
     * Must be called before using detect().
     * 
     * @param env JNI environment
     * @param context Android context (jobject)
     * @return true if initialization successful, false otherwise
     */
    static bool initialize(JNIEnv* env, jobject context);
    
    /**
     * Detect pose landmarks from an OpenCV Mat image.
     * 
     * @param env JNI environment
     * @param img Input image (RGB format, OpenCV Mat)
     * @return Vector of 33 MediaPipe landmarks as normalized coordinates (x, y, z)
     *         Returns empty vector if detection fails or no person detected
     */
    static std::vector<cv::Point3f> detect(JNIEnv* env, const cv::Mat& img);
    
    /**
     * Check if MediaPipe is initialized and ready.
     * 
     * @param env JNI environment
     * @return true if ready, false otherwise
     */
    static bool isReady(JNIEnv* env);
    
    /**
     * Release MediaPipe resources.
     * 
     * @param env JNI environment
     */
    static void release(JNIEnv* env);

private:
    /**
     * Convert OpenCV Mat to Android Bitmap and call MediaPipe detection.
     * 
     * @param env JNI environment
     * @param img Input OpenCV Mat (RGB)
     * @return FloatArray of 33*3 = 99 floats [x1, y1, z1, x2, y2, z2, ...], or null
     */
    static jfloatArray detectInternal(JNIEnv* env, const cv::Mat& img);
    
    /**
     * Convert OpenCV Mat to Android Bitmap.
     * 
     * @param env JNI environment
     * @param img Input OpenCV Mat (RGB)
     * @return Bitmap object, or null on failure
     */
    static jobject matToBitmap(JNIEnv* env, const cv::Mat& img);
    
    // Cached JNI class and method IDs
    static jclass helperClass;
    static jclass bitmapClass;
    static jclass configClass;
    static jmethodID initMethod;
    static jmethodID detectMethod;
    static jmethodID extractMethod;
    static jmethodID isReadyMethod;
    static jmethodID releaseMethod;
    static jmethodID createBitmapMethod;
    static bool jniInitialized;
    
    /**
     * Initialize JNI method IDs (called once).
     * 
     * @param env JNI environment
     * @return true if successful
     */
    static bool initializeJNI(JNIEnv* env);
};

#endif // MEDIAPIPE_POSE_DETECTOR_H

