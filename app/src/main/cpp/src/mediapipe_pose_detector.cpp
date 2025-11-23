#include "mediapipe_pose_detector.h"
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc.hpp>
#include <android/bitmap.h>
#include <android/log.h>

#define LOG_TAG "MediaPipePoseDetector"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Static member initialization
jclass MediaPipePoseDetector::helperClass = nullptr;
jclass MediaPipePoseDetector::bitmapClass = nullptr;
jclass MediaPipePoseDetector::configClass = nullptr;
jmethodID MediaPipePoseDetector::initMethod = nullptr;
jmethodID MediaPipePoseDetector::detectMethod = nullptr;
jmethodID MediaPipePoseDetector::extractMethod = nullptr;
jmethodID MediaPipePoseDetector::extractMaskMethod = nullptr;
jmethodID MediaPipePoseDetector::countPosesMethod = nullptr;
jmethodID MediaPipePoseDetector::isReadyMethod = nullptr;
jmethodID MediaPipePoseDetector::releaseMethod = nullptr;
jmethodID MediaPipePoseDetector::createBitmapMethod = nullptr;
bool MediaPipePoseDetector::jniInitialized = false;
jobject MediaPipePoseDetector::lastDetectionResult = nullptr;

bool MediaPipePoseDetector::initializeJNI(JNIEnv* env) {
    if (jniInitialized) {
        return true;
    }
    
    // Get MediaPipePoseHelper class
    helperClass = (jclass)env->NewGlobalRef(
        env->FindClass("com/example/bodyscanapp/utils/MediaPipePoseHelper"));
    if (helperClass == nullptr) {
        LOGE("Failed to find MediaPipePoseHelper class");
        return false;
    }
    
    // Get Bitmap class
    bitmapClass = (jclass)env->NewGlobalRef(env->FindClass("android/graphics/Bitmap"));
    if (bitmapClass == nullptr) {
        LOGE("Failed to find Bitmap class");
        return false;
    }
    
    // Get Bitmap.Config class
    configClass = (jclass)env->NewGlobalRef(env->FindClass("android/graphics/Bitmap$Config"));
    if (configClass == nullptr) {
        LOGE("Failed to find Bitmap.Config class");
        return false;
    }
    
    // Get MediaPipePoseHelper methods
    initMethod = env->GetStaticMethodID(helperClass, "initialize", 
                                         "(Landroid/content/Context;)Z");
    if (initMethod == nullptr) {
        LOGE("Failed to find initialize method");
        return false;
    }
    
    detectMethod = env->GetStaticMethodID(helperClass, "detectPose", 
                                          "(Landroid/graphics/Bitmap;)Lcom/google/mediapipe/tasks/vision/poselandmarker/PoseLandmarkerResult;");
    if (detectMethod == nullptr) {
        LOGE("Failed to find detectPose method");
        return false;
    }
    
    extractMethod = env->GetStaticMethodID(helperClass, "extractLandmarks", 
                                          "(Lcom/google/mediapipe/tasks/vision/poselandmarker/PoseLandmarkerResult;)[F");
    if (extractMethod == nullptr) {
        LOGE("Failed to find extractLandmarks method");
        return false;
    }
    
    extractMaskMethod = env->GetStaticMethodID(helperClass, "extractSegmentationMaskData", 
                                             "(Lcom/google/mediapipe/tasks/vision/poselandmarker/PoseLandmarkerResult;)[F");
    if (extractMaskMethod == nullptr) {
        LOGE("Failed to find extractSegmentationMaskData method");
        // Not critical - segmentation may not be available
    }
    
    countPosesMethod = env->GetStaticMethodID(helperClass, "countDetectedPoses", 
                                             "(Lcom/google/mediapipe/tasks/vision/poselandmarker/PoseLandmarkerResult;)I");
    if (countPosesMethod == nullptr) {
        LOGE("Failed to find countDetectedPoses method");
        return false;
    }
    
    // Get mask width/height methods
    jmethodID getMaskWidthMethod = env->GetStaticMethodID(helperClass, "getSegmentationMaskWidth", 
                                                         "(Lcom/google/mediapipe/tasks/vision/poselandmarker/PoseLandmarkerResult;)I");
    jmethodID getMaskHeightMethod = env->GetStaticMethodID(helperClass, "getSegmentationMaskHeight", 
                                                          "(Lcom/google/mediapipe/tasks/vision/poselandmarker/PoseLandmarkerResult;)I");
    // Store these if needed, or call directly
    
    isReadyMethod = env->GetStaticMethodID(helperClass, "isReady", "()Z");
    if (isReadyMethod == nullptr) {
        LOGE("Failed to find isReady method");
        return false;
    }
    
    releaseMethod = env->GetStaticMethodID(helperClass, "release", "()V");
    if (releaseMethod == nullptr) {
        LOGE("Failed to find release method");
        return false;
    }
    
    // Get Bitmap.createBitmap method
    createBitmapMethod = env->GetStaticMethodID(bitmapClass, "createBitmap", 
                                                "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    if (createBitmapMethod == nullptr) {
        LOGE("Failed to find createBitmap method");
        return false;
    }
    
    jniInitialized = true;
    LOGI("JNI initialization successful");
    return true;
}

// Global context reference (set once during app initialization)
static jobject g_context = nullptr;
JavaVM* g_jvm = nullptr; // Exported for use in pose_estimator.cpp

bool MediaPipePoseDetector::initialize(JNIEnv* env, jobject context) {
    if (!initializeJNI(env)) {
        return false;
    }
    
    if (context == nullptr) {
        LOGE("Context is null");
        return false;
    }
    
    // Store context globally
    if (g_context != nullptr) {
        env->DeleteGlobalRef(g_context);
    }
    g_context = env->NewGlobalRef(context);
    
    // Store JVM for later use
    env->GetJavaVM(&g_jvm);
    
    jboolean result = env->CallStaticBooleanMethod(helperClass, initMethod, context);
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        return false;
    }
    
    return result == JNI_TRUE;
}

bool MediaPipePoseDetector::isReady(JNIEnv* env) {
    if (!jniInitialized || helperClass == nullptr || isReadyMethod == nullptr) {
        return false;
    }
    
    jboolean result = env->CallStaticBooleanMethod(helperClass, isReadyMethod);
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return false;
    }
    
    return result == JNI_TRUE;
}

void MediaPipePoseDetector::release(JNIEnv* env) {
    if (!jniInitialized || helperClass == nullptr || releaseMethod == nullptr) {
        return;
    }
    
    env->CallStaticVoidMethod(helperClass, releaseMethod);
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
    }
}

jobject MediaPipePoseDetector::matToBitmap(JNIEnv* env, const cv::Mat& img) {
    if (img.empty() || img.cols <= 0 || img.rows <= 0) {
        LOGE("Invalid image for bitmap conversion");
        return nullptr;
    }
    
    // Ensure image is RGB (3 channels)
    cv::Mat rgbImg;
    if (img.channels() == 1) {
        cv::cvtColor(img, rgbImg, cv::COLOR_GRAY2RGB);
    } else if (img.channels() == 3) {
        rgbImg = img.clone();
    } else if (img.channels() == 4) {
        cv::cvtColor(img, rgbImg, cv::COLOR_RGBA2RGB);
    } else {
        LOGE("Unsupported image format: %d channels", img.channels());
        return nullptr;
    }
    
    // Get ARGB_8888 config
    jfieldID argb8888Field = env->GetStaticFieldID(configClass, "ARGB_8888", 
                                                    "Landroid/graphics/Bitmap$Config;");
    if (argb8888Field == nullptr) {
        LOGE("Failed to find ARGB_8888 field");
        return nullptr;
    }
    
    jobject config = env->GetStaticObjectField(configClass, argb8888Field);
    if (config == nullptr) {
        LOGE("Failed to get ARGB_8888 config");
        return nullptr;
    }
    
    // Create bitmap
    jobject bitmap = env->CallStaticObjectMethod(bitmapClass, createBitmapMethod, 
                                                 rgbImg.cols, rgbImg.rows, config);
    if (bitmap == nullptr || env->ExceptionCheck()) {
        if (env->ExceptionCheck()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
        }
        LOGE("Failed to create bitmap");
        env->DeleteLocalRef(config);
        return nullptr;
    }
    
    // Copy pixel data to bitmap
    void* pixels;
    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("Failed to get bitmap info");
        env->DeleteLocalRef(config);
        env->DeleteLocalRef(bitmap);
        return nullptr;
    }
    
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("Failed to lock bitmap pixels");
        env->DeleteLocalRef(config);
        env->DeleteLocalRef(bitmap);
        return nullptr;
    }
    
    // Convert RGB to ARGB
    uint8_t* src = rgbImg.data;
    uint32_t* dst = (uint32_t*)pixels;
    for (int y = 0; y < rgbImg.rows; ++y) {
        for (int x = 0; x < rgbImg.cols; ++x) {
            int idx = y * rgbImg.cols + x;
            uint8_t r = src[idx * 3 + 0];
            uint8_t g = src[idx * 3 + 1];
            uint8_t b = src[idx * 3 + 2];
            // ARGB format: 0xAARRGGBB
            dst[idx] = (0xFF << 24) | (r << 16) | (g << 8) | b;
        }
    }
    
    AndroidBitmap_unlockPixels(env, bitmap);
    env->DeleteLocalRef(config);
    
    return bitmap;
}

jfloatArray MediaPipePoseDetector::detectInternal(JNIEnv* env, const cv::Mat& img) {
    if (!isReady(env)) {
        LOGE("MediaPipe not ready");
        return nullptr;
    }
    
    // Convert Mat to Bitmap
    jobject bitmap = matToBitmap(env, img);
    if (bitmap == nullptr) {
        LOGE("Failed to convert Mat to Bitmap");
        return nullptr;
    }
    
    // Call MediaPipe detection
    jobject result = env->CallStaticObjectMethod(helperClass, detectMethod, bitmap);
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        env->DeleteLocalRef(bitmap);
        return nullptr;
    }
    
    if (result == nullptr) {
        LOGE("MediaPipe detection returned null");
        env->DeleteLocalRef(bitmap);
        return nullptr;
    }
    
    // Store result for mask extraction (create global ref)
    if (lastDetectionResult != nullptr) {
        env->DeleteGlobalRef(lastDetectionResult);
    }
    lastDetectionResult = env->NewGlobalRef(result);
    
    // Extract landmarks
    jfloatArray landmarks = (jfloatArray)env->CallStaticObjectMethod(
        helperClass, extractMethod, result);
    
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
    
    env->DeleteLocalRef(bitmap);
    env->DeleteLocalRef(result);
    
    return landmarks;
}

std::vector<cv::Point3f> MediaPipePoseDetector::detect(JNIEnv* env, const cv::Mat& img) {
    std::vector<cv::Point3f> landmarks;
    
    if (img.empty() || img.cols <= 0 || img.rows <= 0) {
        return landmarks;
    }
    
    // Ensure we have a valid JNI environment
    if (env == nullptr && g_jvm != nullptr) {
        g_jvm->AttachCurrentThread(&env, nullptr);
    }
    if (env == nullptr) {
        LOGE("Failed to get JNI environment");
        return landmarks;
    }
    
    jfloatArray jLandmarks = detectInternal(env, img);
    if (jLandmarks == nullptr) {
        return landmarks;
    }
    
    jsize len = env->GetArrayLength(jLandmarks);
    if (len != 33 * 3) {
        LOGE("Unexpected landmark array length: %d (expected %d)", len, 33 * 3);
        env->DeleteLocalRef(jLandmarks);
        return landmarks;
    }
    
    // Extract landmarks
    jfloat* data = env->GetFloatArrayElements(jLandmarks, nullptr);
    if (data == nullptr) {
        env->DeleteLocalRef(jLandmarks);
        return landmarks;
    }
    
    landmarks.reserve(33);
    for (int i = 0; i < 33; ++i) {
        float x = data[i * 3 + 0];
        float y = data[i * 3 + 1];
        float z = data[i * 3 + 2];
        landmarks.push_back(cv::Point3f(x, y, z));
    }
    
    env->ReleaseFloatArrayElements(jLandmarks, data, JNI_ABORT);
    env->DeleteLocalRef(jLandmarks);
    
    return landmarks;
}

cv::Mat MediaPipePoseDetector::getSegmentationMask(JNIEnv* env, const cv::Mat& img) {
    cv::Mat mask;
    
    if (lastDetectionResult == nullptr || extractMaskMethod == nullptr) {
        return mask; // Return empty Mat
    }
    
    if (img.empty() || img.cols <= 0 || img.rows <= 0) {
        return mask;
    }
    
    // Ensure we have a valid JNI environment
    if (env == nullptr && g_jvm != nullptr) {
        g_jvm->AttachCurrentThread(&env, nullptr);
    }
    if (env == nullptr) {
        LOGE("Failed to get JNI environment for mask extraction");
        return mask;
    }
    
    // Get mask width and height
    jmethodID getMaskWidthMethod = env->GetStaticMethodID(helperClass, "getSegmentationMaskWidth", 
                                                         "(Lcom/google/mediapipe/tasks/vision/poselandmarker/PoseLandmarkerResult;)I");
    jmethodID getMaskHeightMethod = env->GetStaticMethodID(helperClass, "getSegmentationMaskHeight", 
                                                          "(Lcom/google/mediapipe/tasks/vision/poselandmarker/PoseLandmarkerResult;)I");
    
    if (getMaskWidthMethod == nullptr || getMaskHeightMethod == nullptr) {
        LOGE("Failed to find mask dimension methods");
        return mask;
    }
    
    jint maskWidth = env->CallStaticIntMethod(helperClass, getMaskWidthMethod, lastDetectionResult);
    jint maskHeight = env->CallStaticIntMethod(helperClass, getMaskHeightMethod, lastDetectionResult);
    
    if (maskWidth <= 0 || maskHeight <= 0) {
        return mask; // No mask available
    }
    
    // Extract mask data
    jfloatArray jMaskData = (jfloatArray)env->CallStaticObjectMethod(
        helperClass, extractMaskMethod, lastDetectionResult);
    
    if (jMaskData == nullptr || env->ExceptionCheck()) {
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
        }
        return mask;
    }
    
    jsize maskSize = env->GetArrayLength(jMaskData);
    if (maskSize != maskWidth * maskHeight) {
        LOGE("Mask size mismatch: got %d, expected %d", maskSize, maskWidth * maskHeight);
        env->DeleteLocalRef(jMaskData);
        return mask;
    }
    
    // Get mask data
    jfloat* maskData = env->GetFloatArrayElements(jMaskData, nullptr);
    if (maskData == nullptr) {
        env->DeleteLocalRef(jMaskData);
        return mask;
    }
    
    // Create OpenCV Mat from mask data
    mask = cv::Mat(maskHeight, maskWidth, CV_32FC1, maskData).clone();
    
    env->ReleaseFloatArrayElements(jMaskData, maskData, JNI_ABORT);
    env->DeleteLocalRef(jMaskData);
    
    return mask;
}

int MediaPipePoseDetector::countDetectedPoses(JNIEnv* env, const cv::Mat& img) {
    if (!isReady(env)) {
        LOGE("MediaPipe not ready");
        return 0;
    }
    
    // Convert Mat to Bitmap
    jobject bitmap = matToBitmap(env, img);
    if (bitmap == nullptr) {
        LOGE("Failed to convert Mat to Bitmap");
        return 0;
    }
    
    // Call MediaPipe detection
    jobject result = env->CallStaticObjectMethod(helperClass, detectMethod, bitmap);
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        env->DeleteLocalRef(bitmap);
        return 0;
    }
    
    if (result == nullptr) {
        LOGE("MediaPipe detection returned null");
        env->DeleteLocalRef(bitmap);
        return 0;
    }
    
    // Count detected poses
    jint count = env->CallStaticIntMethod(helperClass, countPosesMethod, result);
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        env->DeleteLocalRef(bitmap);
        env->DeleteLocalRef(result);
        return 0;
    }
    
    env->DeleteLocalRef(bitmap);
    env->DeleteLocalRef(result);
    
    return count;
}

