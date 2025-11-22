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
#include <algorithm>

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

// Helper function to check if a keypoint is valid (detected and within bounds)
inline bool isValidKeypoint(const cv::Point2f& pt) {
    return pt.x >= 0.0f && pt.x <= 1.0f && pt.y >= 0.0f && pt.y <= 1.0f;
}

// Helper function to calculate distance between two keypoints in normalized coordinates
inline float keypointDistance(const cv::Point2f& p1, const cv::Point2f& p2) {
    float dx = p2.x - p1.x;
    float dy = p2.y - p1.y;
    return std::sqrt(dx * dx + dy * dy);
}

// Helper function to validate measurement value (sanity check)
inline float validateMeasurement(float value, float minVal, float maxVal) {
    if (value < minVal || value > maxVal || std::isnan(value) || std::isinf(value)) {
        return 0.0f; // Invalid measurement
    }
    return value;
}

// Helper function to compute measurements from 2D keypoints
// Uses user's known height as scaling factor for accurate measurements
// If processedImg and segmentationMask are provided, uses pixel-level edge detection for thigh measurements
// 
// MediaPipe landmark indices (33 total):
// 0: nose, 1-6: eyes, 7-8: ears, 9-10: mouth
// 11-12: shoulders (left, right)
// 13-14: elbows (left, right)
// 15-16: wrists (left, right)
// 17-22: hands (pinky, index, thumb for left/right)
// 23-24: hips (left, right)
// 25-26: knees (left, right)
// 27-28: ankles (left, right)
// 29-30: heels (left, right)
// 31-32: foot_index (left, right)
//
// Measurement array indices:
// [0] Shoulder width
// [1] Arm length (average of left and right)
// [2] Leg length (average of left and right)
// [3] Hip width
// [4] Upper body length (hip midpoint to highest point)
// [5] Lower body length (hip midpoint to ankle midpoint)
// [6] Neck width (eye to eye)
// [7] Thigh width (average of left and right)
std::vector<float> computeMeasurementsFrom2D(
    const std::vector<cv::Point2f>& kpts2d, 
    float userHeight, 
    int imgWidth, 
    int imgHeight,
    const cv::Mat& processedImg = cv::Mat(),
    const cv::Mat& segmentationMask = cv::Mat()) {
    std::vector<float> measurements(8, 0.0f); // 8 measurements
    
    // Validation: Check input parameters
    if (kpts2d.empty() || kpts2d.size() < 33 || userHeight <= 0.0f || 
        userHeight > 300.0f || imgWidth <= 0 || imgHeight <= 0) {
        return measurements; // Return zeros for invalid input
    }
    
    // Find head keypoint (nose - index 0)
    float headY = 1.0f; // Start with max (top of image)
    bool hasHead = false;
    if (isValidKeypoint(kpts2d[0])) {
        headY = kpts2d[0].y;
        hasHead = true;
    }
    
    // Find feet keypoints (ankles, heels, foot_index - indices 27-32)
    float feetY = 0.0f; // Start with min (bottom of image)
    bool hasFeet = false;
    for (int i = 27; i <= 32 && i < static_cast<int>(kpts2d.size()); ++i) {
        if (isValidKeypoint(kpts2d[i])) {
            feetY = std::max(feetY, kpts2d[i].y);
            hasFeet = true;
        }
    }
    
    // If feet not found, use bottommost valid keypoint as fallback
    if (!hasFeet) {
        for (size_t i = 0; i < kpts2d.size(); ++i) {
            if (isValidKeypoint(kpts2d[i])) {
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
    
    // [0] Shoulder Width: Distance between landmarks 11 and 12 (left and right shoulders)
    if (kpts2d.size() > 12 && isValidKeypoint(kpts2d[11]) && isValidKeypoint(kpts2d[12])) {
        float shoulderWidthNormalized = keypointDistance(kpts2d[11], kpts2d[12]);
        float shoulderWidthCm = shoulderWidthNormalized * imgWidth * cmPerPixel;
        measurements[0] = validateMeasurement(shoulderWidthCm, 30.0f, 60.0f);
    }
    
    // [1] Arm Length: Average of left and right arm lengths
    // Left arm: shoulder (11) → elbow (13) → wrist (15)
    // Right arm: shoulder (12) → elbow (14) → wrist (16)
    if (kpts2d.size() > 16 && 
        isValidKeypoint(kpts2d[11]) && isValidKeypoint(kpts2d[13]) && isValidKeypoint(kpts2d[15]) &&
        isValidKeypoint(kpts2d[12]) && isValidKeypoint(kpts2d[14]) && isValidKeypoint(kpts2d[16])) {
        // Calculate left arm length (sum of segments)
        float leftArmSegment1 = keypointDistance(kpts2d[11], kpts2d[13]);
        float leftArmSegment2 = keypointDistance(kpts2d[13], kpts2d[15]);
        float leftArmLengthNormalized = leftArmSegment1 + leftArmSegment2;
        
        // Calculate right arm length (sum of segments)
        float rightArmSegment1 = keypointDistance(kpts2d[12], kpts2d[14]);
        float rightArmSegment2 = keypointDistance(kpts2d[14], kpts2d[16]);
        float rightArmLengthNormalized = rightArmSegment1 + rightArmSegment2;
        
        // Average the two arms
        float avgArmLengthNormalized = (leftArmLengthNormalized + rightArmLengthNormalized) / 2.0f;
        
        // Convert to centimeters using max dimension for diagonal distances
        float avgArmLengthCm = avgArmLengthNormalized * std::max(imgWidth, imgHeight) * cmPerPixel;
        measurements[1] = validateMeasurement(avgArmLengthCm, 50.0f, 80.0f);
    }
    
    // [2] Leg Length: Average of left and right leg lengths
    // Left leg: hip (23) → knee (25) → ankle (27)
    // Right leg: hip (24) → knee (26) → ankle (28)
    if (kpts2d.size() > 28 && 
        isValidKeypoint(kpts2d[23]) && isValidKeypoint(kpts2d[25]) && isValidKeypoint(kpts2d[27]) &&
        isValidKeypoint(kpts2d[24]) && isValidKeypoint(kpts2d[26]) && isValidKeypoint(kpts2d[28])) {
        // Calculate left leg length (sum of segments)
        float leftLegSegment1 = keypointDistance(kpts2d[23], kpts2d[25]);
        float leftLegSegment2 = keypointDistance(kpts2d[25], kpts2d[27]);
        float leftLegLengthNormalized = leftLegSegment1 + leftLegSegment2;
        
        // Calculate right leg length (sum of segments)
        float rightLegSegment1 = keypointDistance(kpts2d[24], kpts2d[26]);
        float rightLegSegment2 = keypointDistance(kpts2d[26], kpts2d[28]);
        float rightLegLengthNormalized = rightLegSegment1 + rightLegSegment2;
        
        // Average the two legs
        float avgLegLengthNormalized = (leftLegLengthNormalized + rightLegLengthNormalized) / 2.0f;
        
        // Convert to centimeters using max dimension for diagonal distances
        float avgLegLengthCm = avgLegLengthNormalized * std::max(imgWidth, imgHeight) * cmPerPixel;
        measurements[2] = validateMeasurement(avgLegLengthCm, 70.0f, 120.0f);
    }
    
    // [3] Hip Width: Distance between landmarks 23 and 24 (left and right hips)
    if (kpts2d.size() > 24 && isValidKeypoint(kpts2d[23]) && isValidKeypoint(kpts2d[24])) {
        float hipWidthNormalized = keypointDistance(kpts2d[23], kpts2d[24]);
        float hipWidthCm = hipWidthNormalized * imgWidth * cmPerPixel;
        measurements[3] = validateMeasurement(hipWidthCm, 25.0f, 50.0f);
    }
    
    // [4] Upper Body Length: Distance from hip midpoint to highest keypoint
    if (kpts2d.size() > 24 && isValidKeypoint(kpts2d[23]) && isValidKeypoint(kpts2d[24])) {
        // Calculate hip midpoint manually (do NOT assume landmark 35 exists)
        cv::Point2f hipMidpoint;
        hipMidpoint.x = (kpts2d[23].x + kpts2d[24].x) / 2.0f;
        hipMidpoint.y = (kpts2d[23].y + kpts2d[24].y) / 2.0f;
        
        // Find highest keypoint (minimum Y value) from keypoints 0-32
        float highestY = 1.0f;
        cv::Point2f highestKeypoint;
        bool foundHighest = false;
        for (int i = 0; i <= 32 && i < static_cast<int>(kpts2d.size()); ++i) {
            if (isValidKeypoint(kpts2d[i])) {
                if (kpts2d[i].y < highestY) {
                    highestY = kpts2d[i].y;
                    highestKeypoint = kpts2d[i];
                    foundHighest = true;
                }
            }
        }
        
        if (foundHighest) {
            // Calculate vertical distance (Y increases downward, so subtract to get positive length)
            float upperBodyLengthNormalized = hipMidpoint.y - highestKeypoint.y;
            float upperBodyLengthCm = upperBodyLengthNormalized * imgHeight * cmPerPixel;
            measurements[4] = validateMeasurement(upperBodyLengthCm, 40.0f, 80.0f);
        }
    }
    
    // [5] Lower Body Length: Distance from hip midpoint to ankle midpoint
    if (kpts2d.size() > 28 && 
        isValidKeypoint(kpts2d[23]) && isValidKeypoint(kpts2d[24]) &&
        isValidKeypoint(kpts2d[27]) && isValidKeypoint(kpts2d[28])) {
        // Calculate hip midpoint manually (do NOT assume landmark 35 exists)
        cv::Point2f hipMidpoint;
        hipMidpoint.x = (kpts2d[23].x + kpts2d[24].x) / 2.0f;
        hipMidpoint.y = (kpts2d[23].y + kpts2d[24].y) / 2.0f;
        
        // Calculate ankle midpoint manually (do NOT assume landmark 134 exists)
        cv::Point2f ankleMidpoint;
        ankleMidpoint.x = (kpts2d[27].x + kpts2d[28].x) / 2.0f;
        ankleMidpoint.y = (kpts2d[27].y + kpts2d[28].y) / 2.0f;
        
        // Calculate distance (primarily vertical, so use imgHeight)
        float lowerBodyLengthNormalized = keypointDistance(hipMidpoint, ankleMidpoint);
        float lowerBodyLengthCm = lowerBodyLengthNormalized * imgHeight * cmPerPixel;
        measurements[5] = validateMeasurement(lowerBodyLengthCm, 60.0f, 100.0f);
    }
    
    // [6] Neck Width: Distance from left eye (2) to right eye (5)
    if (kpts2d.size() > 5 && isValidKeypoint(kpts2d[2]) && isValidKeypoint(kpts2d[5])) {
        float neckWidthNormalized = keypointDistance(kpts2d[2], kpts2d[5]);
        float neckWidthCm = neckWidthNormalized * imgWidth * cmPerPixel;
        measurements[6] = validateMeasurement(neckWidthCm, 8.0f, 15.0f);
    }
    
    // [7] Thigh Width: Average of left and right thigh widths
    // Use pixel-level edge detection if segmentation mask is available
    float leftThighWidthPixels = 0.0f;
    float rightThighWidthPixels = 0.0f;
    bool leftThighValid = false;
    bool rightThighValid = false;
    
    bool usePixelDetection = !segmentationMask.empty() && !processedImg.empty() &&
                             segmentationMask.cols == processedImg.cols &&
                             segmentationMask.rows == processedImg.rows;
    
    // Left thigh: Calculate midpoint between left hip (23) and left knee (25)
    if (kpts2d.size() > 25 && isValidKeypoint(kpts2d[23]) && isValidKeypoint(kpts2d[25])) {
        float midpointYNormalized = (kpts2d[23].y + kpts2d[25].y) / 2.0f;
        int midpointY = static_cast<int>(midpointYNormalized * imgHeight);
        
        if (usePixelDetection && midpointY >= 0 && midpointY < imgHeight) {
            // Pixel-level edge detection: traverse horizontally at midpoint Y
            int leftEdge = -1;
            int rightEdge = -1;
            float leftHipXNormalized = kpts2d[23].x;
            int leftHipX = static_cast<int>(leftHipXNormalized * imgWidth);
            
            // Search from left edge of image towards right to find leftmost edge
            for (int x = 0; x < imgWidth; ++x) {
                if (x >= 0 && x < segmentationMask.cols && midpointY >= 0 && midpointY < segmentationMask.rows) {
                    float maskValue = segmentationMask.at<float>(midpointY, x);
                    if (maskValue > 0.5f) {
                        leftEdge = x;
                        break;
                    }
                }
            }
            
            // Search from body centerline towards left hip to find rightmost edge of left thigh
            int bodyCenterX = 0;
            if (kpts2d.size() > 24 && isValidKeypoint(kpts2d[23]) && isValidKeypoint(kpts2d[24])) {
                bodyCenterX = static_cast<int>((kpts2d[23].x + kpts2d[24].x) / 2.0f * imgWidth);
            }
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
            if (kpts2d.size() > 24 && isValidKeypoint(kpts2d[23]) && isValidKeypoint(kpts2d[24])) {
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
    if (kpts2d.size() > 26 && isValidKeypoint(kpts2d[24]) && isValidKeypoint(kpts2d[26])) {
        float midpointYNormalized = (kpts2d[24].y + kpts2d[26].y) / 2.0f;
        int midpointY = static_cast<int>(midpointYNormalized * imgHeight);
        
        if (usePixelDetection && midpointY >= 0 && midpointY < imgHeight) {
            // Pixel-level edge detection: traverse horizontally at midpoint Y
            int leftEdge = -1;
            int rightEdge = -1;
            float rightHipXNormalized = kpts2d[24].x;
            int rightHipX = static_cast<int>(rightHipXNormalized * imgWidth);
            
            // Search from body centerline towards right to find leftmost edge of right thigh
            int bodyCenterX = 0;
            if (kpts2d.size() > 24 && isValidKeypoint(kpts2d[23]) && isValidKeypoint(kpts2d[24])) {
                bodyCenterX = static_cast<int>((kpts2d[23].x + kpts2d[24].x) / 2.0f * imgWidth);
            }
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
            if (kpts2d.size() > 24 && isValidKeypoint(kpts2d[23]) && isValidKeypoint(kpts2d[24])) {
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
    
    // Calculate average thigh width in centimeters
    if (leftThighValid && rightThighValid) {
        float avgThighWidthPixels = (leftThighWidthPixels + rightThighWidthPixels) / 2.0f;
        float avgThighWidthCm = avgThighWidthPixels * cmPerPixel;
        measurements[7] = validateMeasurement(avgThighWidthCm, 15.0f, 40.0f);
    } else if (leftThighValid) {
        float leftThighWidthCm = leftThighWidthPixels * cmPerPixel;
        measurements[7] = validateMeasurement(leftThighWidthCm, 15.0f, 40.0f);
    } else if (rightThighValid) {
        float rightThighWidthCm = rightThighWidthPixels * cmPerPixel;
        measurements[7] = validateMeasurement(rightThighWidthCm, 15.0f, 40.0f);
    }
    
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
            measurements = env->NewFloatArray(8);
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
            measurements = env->NewFloatArray(8);
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
            measurements = env->NewFloatArray(8);
            float zeros[8] = {0};
            env->SetFloatArrayRegion(measurements, 0, 8, zeros);
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
            measurements = env->NewFloatArray(8);
            float zeros[8] = {0};
            env->SetFloatArrayRegion(measurements, 0, 8, zeros);
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

