#include <jni.h>
#include "image_preprocessor.h"
#include "pose_estimator.h"
#include "multi_view_3d.h"
#include "mesh_generator.h"
#include "mediapipe_pose_detector.h"
#include <opencv2/opencv.hpp>
#include <vector>
#include <cstring>
#include <cmath>
#include <limits>

// Kept for future reference - will be re-enabled after MediaPipe integration for 3D reconstruction
/*
extern "C" JNIEXPORT jobject JNICALL
Java_com_example_bodyscanapp_utils_NativeBridge_processThreeImages(
        JNIEnv* env, jclass, jobjectArray jImages, jintArray jWidths,
        jintArray jHeights, jfloat userHeight) {

    // Find the ScanResult class (Kotlin data class)
    jclass resultClass = env->FindClass("com/example/bodyscanapp/utils/NativeBridge$ScanResult");
    if (resultClass == nullptr) {
        return nullptr;
    }
    
    // Get constructor for Kotlin data class: ScanResult(FloatArray, ByteArray, FloatArray)
    jmethodID constructor = env->GetMethodID(resultClass, "<init>", "([F[B[F)V");
    if (constructor == nullptr) {
        return nullptr;
    }

    // Initialize result arrays (will be populated or set to empty on error)
    jfloatArray keypoints3d = nullptr;
    jbyteArray meshGlb = nullptr;
    jfloatArray measurements = nullptr;

    try {
        // 1. Validate input
        if (jImages == nullptr || jWidths == nullptr || jHeights == nullptr) {
            // Return empty result
            keypoints3d = env->NewFloatArray(135 * 3);
            meshGlb = env->NewByteArray(0);
            measurements = env->NewFloatArray(0);
            jobject result = env->NewObject(resultClass, constructor, keypoints3d, meshGlb, measurements);
            return result;
        }

        jsize numImages = env->GetArrayLength(jImages);
        if (numImages != 3) {
            // Return empty result
            keypoints3d = env->NewFloatArray(135 * 3);
            meshGlb = env->NewByteArray(0);
            measurements = env->NewFloatArray(0);
            jobject result = env->NewObject(resultClass, constructor, keypoints3d, meshGlb, measurements);
            return result;
        }

        // Get image dimensions
        jint widths[3], heights[3];
        env->GetIntArrayRegion(jWidths, 0, 3, widths);
        env->GetIntArrayRegion(jHeights, 0, 3, heights);

        // 2. Convert Java byte[][] → std::vector<cv::Mat>
        std::vector<cv::Mat> imgs(3);
        for (int i = 0; i < 3; ++i) {
            jbyteArray jImg = (jbyteArray)env->GetObjectArrayElement(jImages, i);
            if (jImg == nullptr) {
                // Return empty result
                keypoints3d = env->NewFloatArray(135 * 3);
                meshGlb = env->NewByteArray(0);
                measurements = env->NewFloatArray(0);
                jobject result = env->NewObject(resultClass, constructor, keypoints3d, meshGlb, measurements);
                return result;
            }

            jsize len = env->GetArrayLength(jImg);
            jint expectedSize = widths[i] * heights[i] * 4; // RGBA = 4 bytes per pixel
            
            // Validate buffer size matches expected dimensions
            if (len < expectedSize) {
                // Return empty result - invalid image data
                keypoints3d = env->NewFloatArray(135 * 3);
                meshGlb = env->NewByteArray(0);
                measurements = env->NewFloatArray(0);
                jobject result = env->NewObject(resultClass, constructor, keypoints3d, meshGlb, measurements);
                env->DeleteLocalRef(jImg);
                return result;
            }
            
            jbyte* buf = env->GetByteArrayElements(jImg, nullptr);
            
            // Create RGBA Mat from buffer (rows=height, cols=width, type=CV_8UC4)
            // Note: OpenCV Mat constructor is Mat(rows, cols, type, data)
            cv::Mat rgba(heights[i], widths[i], CV_8UC4, buf);
            
            // Convert RGBA to RGB and clone to ensure contiguous memory
            cv::Mat rgb;
            cv::cvtColor(rgba, rgb, cv::COLOR_RGBA2RGB);
            
            // Clone to ensure the Mat owns its data and is contiguous
            // This is critical to ensure the full image is processed correctly
            imgs[i] = rgb.clone();
            
            env->ReleaseByteArrayElements(jImg, buf, JNI_ABORT);
            env->DeleteLocalRef(jImg);
        }

        // 3. Pre-process images (CLAHE + resizing)
        for (auto& img : imgs) {
            ImagePreprocessor::run(img);
        }

        // 4. Detect 2D keypoints per view
        std::vector<std::vector<cv::Point2f>> kpts2d(3);
        for (int i = 0; i < 3; ++i) {
            kpts2d[i] = PoseEstimator::detect(imgs[i]);
        }

        // 5. Triangulate to 3D keypoints - Kept for future reference (3D reconstruction)
        // std::vector<cv::Point3f> kpts3d = MultiView3D::triangulate(kpts2d, userHeight);

        // 6. Generate mesh - Kept for future reference (3D reconstruction)
        // std::vector<uint8_t> mesh = MeshGenerator::createFromKeypoints(kpts3d);

        // 7. Compute measurements - Kept for future reference (3D reconstruction)
        // std::vector<float> meas = computeCircumferences(kpts3d);
        
        // For single image, we'll use 2D keypoints only
        // Create empty 3D keypoints and mesh
        std::vector<cv::Point3f> kpts3d(135, cv::Point3f(0, 0, 0));
        std::vector<uint8_t> mesh;
        std::vector<float> meas(7, 0.0f); // Empty measurements for multi-view

        // 8. Pack results into Java arrays
        
        // Pack keypoints3d: 135 * 3 = 405 floats
        keypoints3d = env->NewFloatArray(135 * 3);
        if (keypoints3d != nullptr && kpts3d.size() == 135) {
            float* kptsArray = new float[135 * 3];
            for (size_t i = 0; i < 135; ++i) {
                kptsArray[i * 3 + 0] = kpts3d[i].x;
                kptsArray[i * 3 + 1] = kpts3d[i].y;
                kptsArray[i * 3 + 2] = kpts3d[i].z;
            }
            env->SetFloatArrayRegion(keypoints3d, 0, 135 * 3, kptsArray);
            delete[] kptsArray;
        } else {
            // Create zero-filled array if conversion failed
            keypoints3d = env->NewFloatArray(135 * 3);
            float zeros[135 * 3] = {0};
            env->SetFloatArrayRegion(keypoints3d, 0, 135 * 3, zeros);
        }

        // Pack meshGlb: GLB binary data
        meshGlb = env->NewByteArray(mesh.size());
        if (meshGlb != nullptr && !mesh.empty()) {
            env->SetByteArrayRegion(meshGlb, 0, mesh.size(), 
                                   reinterpret_cast<const jbyte*>(mesh.data()));
        } else {
            meshGlb = env->NewByteArray(0);
        }

        // Pack measurements: float array
        measurements = env->NewFloatArray(meas.size());
        if (measurements != nullptr && !meas.empty()) {
            env->SetFloatArrayRegion(measurements, 0, meas.size(), meas.data());
        } else {
            measurements = env->NewFloatArray(0);
        }

    } catch (...) {
        // Exception occurred - return empty result
        if (keypoints3d == nullptr) {
            keypoints3d = env->NewFloatArray(135 * 3);
            float zeros[135 * 3] = {0};
            env->SetFloatArrayRegion(keypoints3d, 0, 135 * 3, zeros);
        }
        if (meshGlb == nullptr) {
            meshGlb = env->NewByteArray(0);
        }
        if (measurements == nullptr) {
            measurements = env->NewFloatArray(0);
        }
    }

    // Create and return ScanResult object
    jobject result = env->NewObject(resultClass, constructor, keypoints3d, meshGlb, measurements);
    
    // Clean up local references
    if (keypoints3d != nullptr) env->DeleteLocalRef(keypoints3d);
    if (meshGlb != nullptr) env->DeleteLocalRef(meshGlb);
    if (measurements != nullptr) env->DeleteLocalRef(measurements);
    if (resultClass != nullptr) env->DeleteLocalRef(resultClass);
    
    return result;
}
*/

// Helper function to compute measurements from 2D keypoints
// Uses user's known height as scaling factor for accurate measurements
// If processedImg and segmentationMask are provided, uses pixel-level edge detection for thigh measurements
std::vector<float> computeMeasurementsFrom2D(
    const std::vector<cv::Point2f>& kpts2d, 
    float userHeight, 
    int imgWidth, 
    int imgHeight,
    const cv::Mat& processedImg = cv::Mat(),
    const cv::Mat& segmentationMask = cv::Mat()) {
    std::vector<float> measurements(7, 0.0f); // 7 measurements
    
    if (kpts2d.empty() || kpts2d.size() < 33 || userHeight <= 0.0f || imgWidth <= 0 || imgHeight <= 0) {
        return measurements;
    }
    
    // MediaPipe landmark indices (first 33 keypoints map directly to MediaPipe landmarks):
    // 0: nose (head)
    // 27-28: ankles (feet)
    // 29-30: heels
    // 31-32: foot_index
    
    // Find head keypoint (nose - index 0)
    float headY = 1.0f; // Start with max (top of image)
    bool hasHead = false;
    if (kpts2d[0].x >= 0.0f && kpts2d[0].x <= 1.0f && kpts2d[0].y >= 0.0f && kpts2d[0].y <= 1.0f) {
        headY = kpts2d[0].y;
        hasHead = true;
    }
    
    // Find feet keypoints (ankles, heels, foot_index - indices 27-32)
    float feetY = 0.0f; // Start with min (bottom of image)
    bool hasFeet = false;
    for (int i = 27; i <= 32 && i < static_cast<int>(kpts2d.size()); ++i) {
        if (kpts2d[i].x >= 0.0f && kpts2d[i].x <= 1.0f && kpts2d[i].y >= 0.0f && kpts2d[i].y <= 1.0f) {
            feetY = std::max(feetY, kpts2d[i].y);
            hasFeet = true;
        }
    }
    
    // If feet not found, use bottommost valid keypoint as fallback
    if (!hasFeet) {
        for (size_t i = 0; i < kpts2d.size(); ++i) {
            if (kpts2d[i].x >= 0.0f && kpts2d[i].x <= 1.0f && kpts2d[i].y >= 0.0f && kpts2d[i].y <= 1.0f) {
                feetY = std::max(feetY, kpts2d[i].y);
            }
        }
    }
    
    // Calculate body height in normalized coordinates
    float bodyHeightNormalized = feetY - headY;
    if (bodyHeightNormalized <= 0.0f || !hasHead) {
        return measurements; // Invalid body height
    }
    
    // Convert normalized height to pixels using processed image height
    float bodyHeightPixels = bodyHeightNormalized * imgHeight;
    
    if (bodyHeightPixels <= 0.0f) {
        return measurements;
    }
    
    // Calculate scale factor: user's known height / body height in pixels
    // This gives us cm per pixel for this specific image
    float cmPerPixel = userHeight / bodyHeightPixels;
    
    // Now calculate measurements using actual keypoint positions
    // MediaPipe landmark indices:
    // 11-12: shoulders (chest measurement)
    // 23-24: hips (hip measurement)
    // 25-26: knees (thigh measurement)
    // 15-16: wrists (arm measurement)
    
    // [0] Chest: Use shoulder landmarks (11-12)
    float chestWidthNormalized = 0.0f;
    if (kpts2d.size() > 12 &&
        kpts2d[11].x >= 0.0f && kpts2d[11].x <= 1.0f && kpts2d[11].y >= 0.0f && kpts2d[11].y <= 1.0f &&
        kpts2d[12].x >= 0.0f && kpts2d[12].x <= 1.0f && kpts2d[12].y >= 0.0f && kpts2d[12].y <= 1.0f) {
        chestWidthNormalized = std::abs(kpts2d[12].x - kpts2d[11].x);
    } else {
        chestWidthNormalized = 0.18f; // Default estimate
    }
    float chestWidthPixels = chestWidthNormalized * imgWidth;
    measurements[0] = chestWidthPixels * cmPerPixel * 3.14159f;
    
    // [1] Hips: Use hip landmarks (23-24)
    float hipWidthNormalized = 0.0f;
    if (kpts2d.size() > 24 && 
        kpts2d[23].x >= 0.0f && kpts2d[23].x <= 1.0f && kpts2d[23].y >= 0.0f && kpts2d[23].y <= 1.0f &&
        kpts2d[24].x >= 0.0f && kpts2d[24].x <= 1.0f && kpts2d[24].y >= 0.0f && kpts2d[24].y <= 1.0f) {
        hipWidthNormalized = std::abs(kpts2d[24].x - kpts2d[23].x);
    } else {
        // Fallback: estimate from bounding box
        float minX = std::numeric_limits<float>::max();
        float maxX = std::numeric_limits<float>::lowest();
        for (size_t i = 23; i <= 24 && i < kpts2d.size(); ++i) {
            if (kpts2d[i].x >= 0.0f && kpts2d[i].x <= 1.0f) {
                minX = std::min(minX, kpts2d[i].x);
                maxX = std::max(maxX, kpts2d[i].x);
            }
        }
        if (maxX > minX) {
            hipWidthNormalized = maxX - minX;
        } else {
            hipWidthNormalized = 0.16f; // Default estimate
        }
    }
    float hipWidthPixels = hipWidthNormalized * imgWidth;
    measurements[1] = hipWidthPixels * cmPerPixel * 3.14159f;
    
    // [2] & [3] Thigh measurements using pixel-level edge detection
    // MediaPipe landmarks: 23=left hip, 24=right hip, 25=left knee, 26=right knee
    
    float leftThighWidthPixels = 0.0f;
    float rightThighWidthPixels = 0.0f;
    bool leftThighValid = false;
    bool rightThighValid = false;
    
    // Use pixel-level edge detection if segmentation mask is available
    bool usePixelDetection = !segmentationMask.empty() && !processedImg.empty() &&
                             segmentationMask.cols == processedImg.cols &&
                             segmentationMask.rows == processedImg.rows;
    
    // Left thigh: Calculate midpoint between left hip (23) and left knee (25)
    if (kpts2d.size() > 25 &&
        kpts2d[23].x >= 0.0f && kpts2d[23].x <= 1.0f && kpts2d[23].y >= 0.0f && kpts2d[23].y <= 1.0f &&
        kpts2d[25].x >= 0.0f && kpts2d[25].x <= 1.0f && kpts2d[25].y >= 0.0f && kpts2d[25].y <= 1.0f) {
        
        // Calculate vertical midpoint Y coordinate between hip and knee
        float midpointYNormalized = (kpts2d[23].y + kpts2d[25].y) / 2.0f;
        int midpointY = static_cast<int>(midpointYNormalized * imgHeight);
        
        if (usePixelDetection && midpointY >= 0 && midpointY < imgHeight) {
            // Pixel-level edge detection: traverse horizontally at midpoint Y
            // Find leftmost and rightmost edges of the left thigh
            int leftEdge = -1;
            int rightEdge = -1;
            float leftHipXNormalized = kpts2d[23].x;
            int leftHipX = static_cast<int>(leftHipXNormalized * imgWidth);
            
            // Search from left edge of image towards right to find leftmost edge
            for (int x = 0; x < imgWidth; ++x) {
                if (x >= 0 && x < segmentationMask.cols && midpointY >= 0 && midpointY < segmentationMask.rows) {
                    float maskValue = segmentationMask.at<float>(midpointY, x);
                    if (maskValue > 0.5f) { // Threshold for person pixels
                        leftEdge = x;
                        break;
                    }
                }
            }
            
            // Search from right edge of image towards left to find rightmost edge
            // But only search in the left half (left of body centerline)
            int bodyCenterX = static_cast<int>((kpts2d[23].x + kpts2d[24].x) / 2.0f * imgWidth);
            int searchEnd = std::min(leftHipX + (bodyCenterX - leftHipX) * 2, imgWidth - 1);
            
            for (int x = searchEnd; x >= leftHipX; --x) {
                if (x >= 0 && x < segmentationMask.cols && midpointY >= 0 && midpointY < segmentationMask.rows) {
                    float maskValue = segmentationMask.at<float>(midpointY, x);
                    if (maskValue > 0.5f) {
                        rightEdge = x;
                        break;
                    }
                }
            }
            
            if (leftEdge >= 0 && rightEdge >= 0 && rightEdge > leftEdge) {
                leftThighWidthPixels = static_cast<float>(rightEdge - leftEdge);
                leftThighValid = true;
            }
        }
        
        // Fallback to estimation if pixel detection failed
        if (!leftThighValid) {
            float bodyCenterX = 0.5f;
            if (kpts2d.size() > 24 &&
                kpts2d[23].x >= 0.0f && kpts2d[23].x <= 1.0f &&
                kpts2d[24].x >= 0.0f && kpts2d[24].x <= 1.0f) {
                bodyCenterX = (kpts2d[23].x + kpts2d[24].x) / 2.0f;
            }
            float hipHalfWidth = std::abs(kpts2d[23].x - bodyCenterX);
            float thighExpansionFactor = 1.5f;
            float leftThighHalfWidth = hipHalfWidth * thighExpansionFactor;
            float leftThighWidthNormalized = leftThighHalfWidth * 2.0f;
            leftThighWidthPixels = leftThighWidthNormalized * imgWidth;
            leftThighValid = true;
        }
    }
    
    // Right thigh: Calculate midpoint between right hip (24) and right knee (26)
    if (kpts2d.size() > 26 &&
        kpts2d[24].x >= 0.0f && kpts2d[24].x <= 1.0f && kpts2d[24].y >= 0.0f && kpts2d[24].y <= 1.0f &&
        kpts2d[26].x >= 0.0f && kpts2d[26].x <= 1.0f && kpts2d[26].y >= 0.0f && kpts2d[26].y <= 1.0f) {
        
        // Calculate vertical midpoint Y coordinate between hip and knee
        float midpointYNormalized = (kpts2d[24].y + kpts2d[26].y) / 2.0f;
        int midpointY = static_cast<int>(midpointYNormalized * imgHeight);
        
        if (usePixelDetection && midpointY >= 0 && midpointY < imgHeight) {
            // Pixel-level edge detection: traverse horizontally at midpoint Y
            // Find leftmost and rightmost edges of the right thigh
            int leftEdge = -1;
            int rightEdge = -1;
            float rightHipXNormalized = kpts2d[24].x;
            int rightHipX = static_cast<int>(rightHipXNormalized * imgWidth);
            
            // Search from body centerline towards right to find leftmost edge of right thigh
            int bodyCenterX = static_cast<int>((kpts2d[23].x + kpts2d[24].x) / 2.0f * imgWidth);
            for (int x = bodyCenterX; x < imgWidth; ++x) {
                if (x >= 0 && x < segmentationMask.cols && midpointY >= 0 && midpointY < segmentationMask.rows) {
                    float maskValue = segmentationMask.at<float>(midpointY, x);
                    if (maskValue > 0.5f) {
                        leftEdge = x;
                        break;
                    }
                }
            }
            
            // Search from right edge of image towards left to find rightmost edge
            for (int x = imgWidth - 1; x >= rightHipX; --x) {
                if (x >= 0 && x < segmentationMask.cols && midpointY >= 0 && midpointY < segmentationMask.rows) {
                    float maskValue = segmentationMask.at<float>(midpointY, x);
                    if (maskValue > 0.5f) {
                        rightEdge = x;
                        break;
                    }
                }
            }
            
            if (leftEdge >= 0 && rightEdge >= 0 && rightEdge > leftEdge) {
                rightThighWidthPixels = static_cast<float>(rightEdge - leftEdge);
                rightThighValid = true;
            }
        }
        
        // Fallback to estimation if pixel detection failed
        if (!rightThighValid) {
            float bodyCenterX = 0.5f;
            if (kpts2d.size() > 24 &&
                kpts2d[23].x >= 0.0f && kpts2d[23].x <= 1.0f &&
                kpts2d[24].x >= 0.0f && kpts2d[24].x <= 1.0f) {
                bodyCenterX = (kpts2d[23].x + kpts2d[24].x) / 2.0f;
            }
            float hipHalfWidth = std::abs(kpts2d[24].x - bodyCenterX);
            float thighExpansionFactor = 1.5f;
            float rightThighHalfWidth = hipHalfWidth * thighExpansionFactor;
            float rightThighWidthNormalized = rightThighHalfWidth * 2.0f;
            rightThighWidthPixels = rightThighWidthNormalized * imgWidth;
            rightThighValid = true;
        }
    }
    
    // Step 4: Convert thigh pixel widths to centimeters using the pixels-per-cm ratio
    // The pixels-per-cm ratio was calculated from: userHeightCm / bodyHeightPixels
    // This gives us accurate scaling based on the known person height
    
    if (leftThighValid && rightThighValid) {
        // Both thighs detected: calculate individual measurements and average
        float leftThighCm = leftThighWidthPixels * cmPerPixel * 3.14159f; // Circumference = π * diameter
        float rightThighCm = rightThighWidthPixels * cmPerPixel * 3.14159f;
        
        measurements[2] = leftThighCm;
        measurements[3] = rightThighCm;
        
        // Optionally store average in a separate field if needed
        // For now, we keep them separate as requested
    } else if (leftThighValid) {
        measurements[2] = leftThighWidthPixels * cmPerPixel * 3.14159f;
        measurements[3] = 0.0f;
    } else if (rightThighValid) {
        measurements[2] = 0.0f;
        measurements[3] = rightThighWidthPixels * cmPerPixel * 3.14159f;
    } else {
        // Neither thigh detected
        measurements[2] = 0.0f;
        measurements[3] = 0.0f;
    }
    
    // [4] Arm Left: Use left shoulder (11) to left wrist (15)
    float leftArmWidthNormalized = 0.0f;
    if (kpts2d.size() > 15 &&
        kpts2d[11].x >= 0.0f && kpts2d[11].x <= 1.0f && kpts2d[11].y >= 0.0f && kpts2d[11].y <= 1.0f &&
        kpts2d[15].x >= 0.0f && kpts2d[15].x <= 1.0f && kpts2d[15].y >= 0.0f && kpts2d[15].y <= 1.0f) {
        // Estimate arm width from shoulder to wrist distance
        float shoulderWristDistance = std::sqrt(
            std::pow(kpts2d[15].x - kpts2d[11].x, 2.0f) + 
            std::pow(kpts2d[15].y - kpts2d[11].y, 2.0f)
        );
        leftArmWidthNormalized = shoulderWristDistance * 0.25f; // Estimate width as 25% of length
    } else {
        leftArmWidthNormalized = 0.08f; // Default estimate
    }
    float leftArmWidthPixels = leftArmWidthNormalized * imgWidth;
    measurements[4] = leftArmWidthPixels * cmPerPixel * 3.14159f;
    
    // [5] Arm Right: Use right shoulder (12) to right wrist (16)
    float rightArmWidthNormalized = 0.0f;
    if (kpts2d.size() > 16 &&
        kpts2d[12].x >= 0.0f && kpts2d[12].x <= 1.0f && kpts2d[12].y >= 0.0f && kpts2d[12].y <= 1.0f &&
        kpts2d[16].x >= 0.0f && kpts2d[16].x <= 1.0f && kpts2d[16].y >= 0.0f && kpts2d[16].y <= 1.0f) {
        // Estimate arm width from shoulder to wrist distance
        float shoulderWristDistance = std::sqrt(
            std::pow(kpts2d[16].x - kpts2d[12].x, 2.0f) + 
            std::pow(kpts2d[16].y - kpts2d[12].y, 2.0f)
        );
        rightArmWidthNormalized = shoulderWristDistance * 0.25f; // Estimate width as 25% of length
    } else {
        rightArmWidthNormalized = 0.08f; // Default estimate
    }
    float rightArmWidthPixels = rightArmWidthNormalized * imgWidth;
    measurements[5] = rightArmWidthPixels * cmPerPixel * 3.14159f;
    
    // [6] Not used (kept for compatibility, set to 0)
    measurements[6] = 0.0f;
    
    return measurements;
}

// Single image processing function
extern "C" JNIEXPORT jobject JNICALL
Java_com_example_bodyscanapp_utils_NativeBridge_processOneImage(
        JNIEnv* env, jclass, jbyteArray jImage, jint width, jint height, jfloat userHeight) {

    // Find the ScanResult class (Kotlin data class)
    jclass resultClass = env->FindClass("com/example/bodyscanapp/utils/NativeBridge$ScanResult");
    if (resultClass == nullptr) {
        return nullptr;
    }
    
    // Get constructor for Kotlin data class
    // Try 4-parameter constructor first (with keypoints2d)
    jmethodID constructor = env->GetMethodID(resultClass, "<init>", "([F[B[F[F)V");
    if (constructor == nullptr) {
        // Fall back to 3-parameter constructor (without keypoints2d, will use default null)
        constructor = env->GetMethodID(resultClass, "<init>", "([F[B[F)V");
        if (constructor == nullptr) {
            return nullptr;
        }
    }

    // Initialize result arrays
    jfloatArray keypoints3d = nullptr;
    jbyteArray meshGlb = nullptr;
    jfloatArray measurements = nullptr;
    jfloatArray keypoints2d = nullptr;

    try {
        // 1. Validate input
        if (jImage == nullptr || width <= 0 || height <= 0) {
            // Return empty result
            keypoints3d = env->NewFloatArray(135 * 3);
            meshGlb = env->NewByteArray(0);
            measurements = env->NewFloatArray(7);
            keypoints2d = env->NewFloatArray(135 * 2);
            float zeros2d[135 * 2] = {0};
            env->SetFloatArrayRegion(keypoints2d, 0, 135 * 2, zeros2d);
            
            // Try 4-parameter constructor first, fall back to 3-parameter
            jobject result = nullptr;
            jmethodID newConstructor = env->GetMethodID(resultClass, "<init>", "([F[B[F[F)V");
            if (newConstructor != nullptr && keypoints2d != nullptr) {
                result = env->NewObject(resultClass, newConstructor, keypoints3d, meshGlb, measurements, keypoints2d);
            } else {
                result = env->NewObject(resultClass, constructor, keypoints3d, meshGlb, measurements);
            }
            return result;
        }

        jsize len = env->GetArrayLength(jImage);
        jint expectedSize = width * height * 4; // RGBA = 4 bytes per pixel
        
        // Validate buffer size
        if (len < expectedSize) {
            keypoints3d = env->NewFloatArray(135 * 3);
            meshGlb = env->NewByteArray(0);
            measurements = env->NewFloatArray(7);
            keypoints2d = env->NewFloatArray(135 * 2);
            float zeros2d[135 * 2] = {0};
            env->SetFloatArrayRegion(keypoints2d, 0, 135 * 2, zeros2d);
            
            jmethodID newConstructor = env->GetMethodID(resultClass, "<init>", "([F[B[F[F)V");
            if (newConstructor != nullptr && keypoints2d != nullptr) {
                return env->NewObject(resultClass, newConstructor, keypoints3d, meshGlb, measurements, keypoints2d);
            } else {
                return env->NewObject(resultClass, constructor, keypoints3d, meshGlb, measurements);
            }
        }
        
        jbyte* buf = env->GetByteArrayElements(jImage, nullptr);
        
        // Create RGBA Mat from buffer
        cv::Mat rgba(height, width, CV_8UC4, buf);
        
        // Convert RGBA to RGB and clone
        cv::Mat rgb;
        cv::cvtColor(rgba, rgb, cv::COLOR_RGBA2RGB);
        cv::Mat img = rgb.clone();
        
        env->ReleaseByteArrayElements(jImage, buf, JNI_ABORT);

        // 2. Pre-process image (CLAHE + resizing)
        ImagePreprocessor::run(img);

        // 3. Detect 2D keypoints using MediaPipe
        std::vector<cv::Point2f> kpts2d = PoseEstimator::detect(img);
        
        // Get segmentation mask for pixel-level measurements
        cv::Mat segmentationMask = MediaPipePoseDetector::getSegmentationMask(env, img);
        
        // Resize segmentation mask to match processed image dimensions if needed
        int processedWidth = img.cols;
        int processedHeight = img.rows;
        if (!segmentationMask.empty() && 
            (segmentationMask.cols != processedWidth || segmentationMask.rows != processedHeight)) {
            cv::Mat resizedMask;
            cv::resize(segmentationMask, resizedMask, cv::Size(processedWidth, processedHeight), 0, 0, cv::INTER_LINEAR);
            segmentationMask = resizedMask;
        }

        // 4. Compute measurements from 2D keypoints
        // Use processed image dimensions (after preprocessing/resizing)
        std::vector<float> meas = computeMeasurementsFrom2D(kpts2d, userHeight, processedWidth, processedHeight, img, segmentationMask);

        // 5. Pack results into Java arrays
        
        // Pack keypoints3d: 135 * 3 = 405 floats (empty for single image)
        keypoints3d = env->NewFloatArray(135 * 3);
        float zeros3d[135 * 3] = {0};
        env->SetFloatArrayRegion(keypoints3d, 0, 135 * 3, zeros3d);

        // Pack meshGlb: empty for single image
        meshGlb = env->NewByteArray(0);

        // Pack measurements: float array
        measurements = env->NewFloatArray(meas.size());
        if (measurements != nullptr && !meas.empty()) {
            env->SetFloatArrayRegion(measurements, 0, meas.size(), meas.data());
        } else {
            measurements = env->NewFloatArray(7);
            float zeros[7] = {0};
            env->SetFloatArrayRegion(measurements, 0, 7, zeros);
        }

        // Pack keypoints2d: 135 * 2 = 270 floats (normalized x, y coordinates)
        keypoints2d = env->NewFloatArray(135 * 2);
        if (keypoints2d != nullptr && kpts2d.size() == 135) {
            float* kpts2dArray = new float[135 * 2];
            for (size_t i = 0; i < 135; ++i) {
                kpts2dArray[i * 2 + 0] = kpts2d[i].x; // normalized x (0-1)
                kpts2dArray[i * 2 + 1] = kpts2d[i].y; // normalized y (0-1)
            }
            env->SetFloatArrayRegion(keypoints2d, 0, 135 * 2, kpts2dArray);
            delete[] kpts2dArray;
        } else {
            // Create zero-filled array if conversion failed
            float zeros2d[135 * 2] = {0};
            env->SetFloatArrayRegion(keypoints2d, 0, 135 * 2, zeros2d);
        }

    } catch (...) {
        // Exception occurred - return empty result
        if (keypoints3d == nullptr) {
            keypoints3d = env->NewFloatArray(135 * 3);
            float zeros[135 * 3] = {0};
            env->SetFloatArrayRegion(keypoints3d, 0, 135 * 3, zeros);
        }
        if (meshGlb == nullptr) {
            meshGlb = env->NewByteArray(0);
        }
        if (measurements == nullptr) {
            measurements = env->NewFloatArray(7);
            float zeros[7] = {0};
            env->SetFloatArrayRegion(measurements, 0, 7, zeros);
        }
        if (keypoints2d == nullptr) {
            keypoints2d = env->NewFloatArray(135 * 2);
            float zeros2d[135 * 2] = {0};
            env->SetFloatArrayRegion(keypoints2d, 0, 135 * 2, zeros2d);
        }
    }

    // Create and return ScanResult object
    jobject result = nullptr;
    jmethodID newConstructor = env->GetMethodID(resultClass, "<init>", "([F[B[F[F)V");
    if (newConstructor != nullptr && keypoints2d != nullptr) {
        result = env->NewObject(resultClass, newConstructor, keypoints3d, meshGlb, measurements, keypoints2d);
    } else {
        // Fall back to 3-parameter constructor (keypoints2d will be null/default)
        result = env->NewObject(resultClass, constructor, keypoints3d, meshGlb, measurements);
    }
    
    // Clean up local references
    if (keypoints3d != nullptr) env->DeleteLocalRef(keypoints3d);
    if (meshGlb != nullptr) env->DeleteLocalRef(meshGlb);
    if (measurements != nullptr) env->DeleteLocalRef(measurements);
    if (keypoints2d != nullptr) env->DeleteLocalRef(keypoints2d);
    if (resultClass != nullptr) env->DeleteLocalRef(resultClass);
    
    return result;
}

// Initialize MediaPipe Pose Detector with Android context
extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_bodyscanapp_utils_NativeBridge_initializeMediaPipe(
        JNIEnv* env, jclass, jobject context) {
    return MediaPipePoseDetector::initialize(env, context) ? JNI_TRUE : JNI_FALSE;
}

// TODO: Update to use MediaPipe for validation
extern "C" JNIEXPORT jobject JNICALL
Java_com_example_bodyscanapp_utils_NativeBridge_validateImage(
        JNIEnv* env, jclass, jbyteArray jImage, jint width, jint height) {
    
    // Find the ImageValidationResult class
    jclass resultClass = env->FindClass("com/example/bodyscanapp/utils/NativeBridge$ImageValidationResult");
    if (resultClass == nullptr) {
        return nullptr;
    }
    
    // Get constructor
    jmethodID constructor = env->GetMethodID(resultClass, "<init>", "(ZZFLjava/lang/String;)V");
    if (constructor == nullptr) {
        return nullptr;
    }
    
    // Initialize result
    bool hasPerson = false;
    bool isFullBody = false;
    float confidence = 0.0f;
    std::string message = "";
    
    try {
        if (jImage == nullptr || width <= 0 || height <= 0) {
            message = "Invalid input";
        } else {
            jsize len = env->GetArrayLength(jImage);
            jint expectedSize = width * height * 4; // RGBA
            
            if (len >= expectedSize * 0.9) {  // Allow some tolerance
                jbyte* buf = env->GetByteArrayElements(jImage, nullptr);
                
                // Create RGBA Mat
                cv::Mat rgba(height, width, CV_8UC4, buf);
                
                // Convert RGBA to RGB
                cv::Mat rgb;
                cv::cvtColor(rgba, rgb, cv::COLOR_RGBA2RGB);
                cv::Mat img = rgb.clone();
                
                env->ReleaseByteArrayElements(jImage, buf, JNI_ABORT);
                
                // Validate image
                PoseEstimator::ValidationResult result = PoseEstimator::validateImage(img);
                hasPerson = result.hasPerson;
                isFullBody = result.isFullBody;
                confidence = result.confidence;
                message = result.message;
            } else {
                message = "Image size mismatch";
            }
        }
    } catch (...) {
        message = "Processing error";
    }
    
    // Create Java string
    jstring jMessage = env->NewStringUTF(message.c_str());
    
    // Create and return result object
    jobject result = env->NewObject(resultClass, constructor, 
                                     hasPerson, isFullBody, confidence, jMessage);
    
    // Clean up
    if (jMessage != nullptr) env->DeleteLocalRef(jMessage);
    if (resultClass != nullptr) env->DeleteLocalRef(resultClass);
    
    return result;
}

// Detect keypoints for preview overlay
extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_example_bodyscanapp_utils_NativeBridge_detectKeypoints(
        JNIEnv* env, jclass, jbyteArray jImage, jint width, jint height) {
    
    // Initialize result array
    jfloatArray keypoints2d = env->NewFloatArray(135 * 2);
    if (keypoints2d == nullptr) {
        return nullptr;
    }
    
    // Fill with zeros as default
    float zeros2d[135 * 2] = {0};
    env->SetFloatArrayRegion(keypoints2d, 0, 135 * 2, zeros2d);
    
    try {
        if (jImage == nullptr || width <= 0 || height <= 0) {
            return keypoints2d; // Return zeros
        }
        
        jsize len = env->GetArrayLength(jImage);
        jint expectedSize = width * height * 4; // RGBA
        
        if (len < expectedSize * 0.9) {  // Allow some tolerance
            return keypoints2d; // Return zeros
        }
        
        jbyte* buf = env->GetByteArrayElements(jImage, nullptr);
        
        // Create RGBA Mat
        cv::Mat rgba(height, width, CV_8UC4, buf);
        
        // Convert RGBA to RGB
        cv::Mat rgb;
        cv::cvtColor(rgba, rgb, cv::COLOR_RGBA2RGB);
        cv::Mat img = rgb.clone();
        
        env->ReleaseByteArrayElements(jImage, buf, JNI_ABORT);
        
        // Detect keypoints using MediaPipe
        std::vector<cv::Point2f> kpts2d = PoseEstimator::detect(img);
        
        // Pack keypoints2d: 135 * 2 = 270 floats (normalized x, y coordinates)
        if (kpts2d.size() == 135) {
            float* kpts2dArray = new float[135 * 2];
            for (size_t i = 0; i < 135; ++i) {
                kpts2dArray[i * 2 + 0] = kpts2d[i].x; // normalized x (0-1)
                kpts2dArray[i * 2 + 1] = kpts2d[i].y; // normalized y (0-1)
            }
            env->SetFloatArrayRegion(keypoints2d, 0, 135 * 2, kpts2dArray);
            delete[] kpts2dArray;
        }
        
    } catch (...) {
        // Exception occurred - return zeros
        float zeros[135 * 2] = {0};
        env->SetFloatArrayRegion(keypoints2d, 0, 135 * 2, zeros);
    }
    
    return keypoints2d;
}

