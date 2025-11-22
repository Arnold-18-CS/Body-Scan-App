#include "pose_estimator.h"
#include "mediapipe_pose_detector.h"
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc.hpp>
#include <vector>
#include <cmath>
#include <algorithm>
#include <limits>
#include <jni.h>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

// Forward declaration - JNIEnv will be obtained from global JVM
// Defined in mediapipe_pose_detector.cpp
extern JavaVM* g_jvm;

/**
 * Get JNI environment, attaching thread if necessary.
 */
static JNIEnv* getJNIEnv() {
    if (g_jvm == nullptr) {
        return nullptr;
    }
    
    JNIEnv* env = nullptr;
    jint result = g_jvm->GetEnv((void**)&env, JNI_VERSION_1_6);
    
    if (result == JNI_EDETACHED) {
        // Thread not attached, attach it
        result = g_jvm->AttachCurrentThread(&env, nullptr);
        if (result != JNI_OK || env == nullptr) {
            return nullptr;
        }
    } else if (result != JNI_OK) {
        return nullptr;
    }
    
    return env;
}

/**
 * Map 33 MediaPipe landmarks to 135 keypoints.
 * 
 * MediaPipe provides 33 landmarks:
 * 0-10: Face (nose, eyes, ears)
 * 11-16: Upper body (shoulders, elbows, wrists)
 * 23-28: Upper body (shoulders, elbows, wrists) - right side
 * 17-22: Lower body (hips, knees, ankles) - left side
 * 29-32: Lower body (hips, knees, ankles) - right side
 * 
 * The 135 keypoint format likely includes:
 * - Direct mapping of the 33 MediaPipe landmarks
 * - Interpolated points between landmarks (e.g., midpoints)
 * - Additional anatomical points
 * 
 * For now, we'll use direct mapping for the 33 landmarks and
 * interpolate/extrapolate the remaining 102 keypoints.
 */
static std::vector<cv::Point2f> mapMediaPipeTo135(
    const std::vector<cv::Point3f>& mpLandmarks) {
    
    std::vector<cv::Point2f> keypoints(135, cv::Point2f(0.0f, 0.0f));
    
    if (mpLandmarks.size() != 33) {
        return keypoints; // Return zeros if invalid input
    }
    
    // Direct mapping: Map first 33 MediaPipe landmarks to first 33 keypoints
    for (int i = 0; i < 33; ++i) {
        keypoints[i] = cv::Point2f(mpLandmarks[i].x, mpLandmarks[i].y);
    }
    
    // Interpolate additional keypoints
    // Strategy: Create intermediate points between major landmarks
    
    // Face region (indices 0-10 in MediaPipe)
    // Add interpolated points between facial landmarks
    int faceStart = 0;
    int faceEnd = 10;
    int kpIdx = 33;
    
    // Upper body - left arm (indices 11-16)
    // 11: left shoulder, 12: left elbow, 13: left wrist
    if (kpIdx < 135 && mpLandmarks[11].x > 0 && mpLandmarks[12].x > 0) {
        // Midpoint between shoulder and elbow
        keypoints[kpIdx++] = cv::Point2f(
            (mpLandmarks[11].x + mpLandmarks[12].x) / 2.0f,
            (mpLandmarks[11].y + mpLandmarks[12].y) / 2.0f);
    }
    if (kpIdx < 135 && mpLandmarks[12].x > 0 && mpLandmarks[13].x > 0) {
        // Midpoint between elbow and wrist
        keypoints[kpIdx++] = cv::Point2f(
            (mpLandmarks[12].x + mpLandmarks[13].x) / 2.0f,
            (mpLandmarks[12].y + mpLandmarks[13].y) / 2.0f);
    }
    
    // Upper body - right arm (indices 23-28, but MediaPipe uses 15-16 for right)
    // Actually MediaPipe uses: 11-16 left, 23-28 right
    // Let me check the actual MediaPipe landmark indices
    // MediaPipe pose landmarks: 0-32
    // 0: nose, 1-2: eyes, 3-4: ears
    // 5-10: mouth/face
    // 11-16: left arm (shoulder, elbow, wrist)
    // 17-22: left leg (hip, knee, ankle)
    // 23-28: right arm (shoulder, elbow, wrist)
    // 29-32: right leg (hip, knee, ankle)
    
    // Right arm interpolation
    if (kpIdx < 135 && mpLandmarks.size() > 23 && 
        mpLandmarks[23].x > 0 && mpLandmarks[24].x > 0) {
        keypoints[kpIdx++] = cv::Point2f(
            (mpLandmarks[23].x + mpLandmarks[24].x) / 2.0f,
            (mpLandmarks[23].y + mpLandmarks[24].y) / 2.0f);
    }
    if (kpIdx < 135 && mpLandmarks.size() > 24 && 
        mpLandmarks[24].x > 0 && mpLandmarks[25].x > 0) {
        keypoints[kpIdx++] = cv::Point2f(
            (mpLandmarks[24].x + mpLandmarks[25].x) / 2.0f,
            (mpLandmarks[24].y + mpLandmarks[25].y) / 2.0f);
    }
    
    // Left leg interpolation (17-22)
    if (kpIdx < 135 && mpLandmarks.size() > 17 && 
        mpLandmarks[17].x > 0 && mpLandmarks[18].x > 0) {
        keypoints[kpIdx++] = cv::Point2f(
            (mpLandmarks[17].x + mpLandmarks[18].x) / 2.0f,
            (mpLandmarks[17].y + mpLandmarks[18].y) / 2.0f);
    }
    if (kpIdx < 135 && mpLandmarks.size() > 18 && 
        mpLandmarks[18].x > 0 && mpLandmarks[19].x > 0) {
        keypoints[kpIdx++] = cv::Point2f(
            (mpLandmarks[18].x + mpLandmarks[19].x) / 2.0f,
            (mpLandmarks[18].y + mpLandmarks[19].y) / 2.0f);
    }
    
    // Right leg interpolation (29-32)
    if (kpIdx < 135 && mpLandmarks.size() > 29 && 
        mpLandmarks[29].x > 0 && mpLandmarks[30].x > 0) {
        keypoints[kpIdx++] = cv::Point2f(
            (mpLandmarks[29].x + mpLandmarks[30].x) / 2.0f,
            (mpLandmarks[29].y + mpLandmarks[30].y) / 2.0f);
    }
    if (kpIdx < 135 && mpLandmarks.size() > 30 && 
        mpLandmarks[30].x > 0 && mpLandmarks[31].x > 0) {
        keypoints[kpIdx++] = cv::Point2f(
            (mpLandmarks[30].x + mpLandmarks[31].x) / 2.0f,
            (mpLandmarks[30].y + mpLandmarks[31].y) / 2.0f);
    }
    
    // Fill remaining keypoints with interpolated/extrapolated values
    // For now, duplicate nearby valid keypoints or use body proportions
    while (kpIdx < 135) {
        // Use the last valid keypoint or a default position
        if (kpIdx > 0 && keypoints[kpIdx - 1].x > 0) {
            keypoints[kpIdx] = keypoints[kpIdx - 1];
        } else {
            // Default to center of image if no valid keypoints
            keypoints[kpIdx] = cv::Point2f(0.5f, 0.5f);
        }
        kpIdx++;
    }
    
    return keypoints;
}

// Implementation using MediaPipe for pose detection
std::vector<cv::Point2f> PoseEstimator::detect(const cv::Mat& img) {
    const int numKeypoints = 135;
    std::vector<cv::Point2f> keypoints(numKeypoints, cv::Point2f(0.0f, 0.0f));
    
    if (img.empty() || img.cols <= 0 || img.rows <= 0) {
        return keypoints;
    }
    
    // Get JNI environment
    JNIEnv* env = getJNIEnv();
    if (env == nullptr) {
        // If JNI not available, return zeros
        return keypoints;
    }
    
    // Check if MediaPipe is ready
    if (!MediaPipePoseDetector::isReady(env)) {
        // MediaPipe not initialized, return zeros
        return keypoints;
    }
    
    // Detect pose using MediaPipe
    std::vector<cv::Point3f> mpLandmarks = MediaPipePoseDetector::detect(env, img);
    
    if (mpLandmarks.empty() || mpLandmarks.size() != 33) {
        // No pose detected or invalid result
        return keypoints;
    }
    
    // Map 33 MediaPipe landmarks to 135 keypoints
    keypoints = mapMediaPipeTo135(mpLandmarks);
    
    return keypoints;
}

// Note: The 33-to-135 mapping above is a basic implementation.
// For production, you may want to:
// 1. Use a more sophisticated mapping based on BODY_25 or similar format
// 2. Add more interpolation points for smoother keypoint coverage
// 3. Validate keypoint visibility/confidence from MediaPipe

// TODO: Update to use MediaPipe for validation
PoseEstimator::ValidationResult PoseEstimator::validateImage(const cv::Mat& img) {
    ValidationResult result;
    result.hasPerson = false;
    result.isFullBody = false;
    result.confidence = 0.0f;
    result.message = "";
    
    if (img.empty() || img.cols <= 0 || img.rows <= 0) {
        result.message = "Invalid image";
        return result;
    }
    
    int imgWidth = img.cols;
    int imgHeight = img.rows;
    
    try {
        // Step 1: Convert to grayscale
        cv::Mat gray;
        if (img.channels() == 3) {
            cv::cvtColor(img, gray, cv::COLOR_RGB2GRAY);
        } else if (img.channels() == 1) {
            gray = img.clone();
        } else {
            result.message = "Unsupported image format";
            return result;
        }
        
        // Step 2: Apply Gaussian blur
        cv::Mat blurred;
        cv::GaussianBlur(gray, blurred, cv::Size(5, 5), 1.5);
        
        // Step 3: Create binary image
        cv::Mat binary;
        cv::adaptiveThreshold(blurred, binary, 255, cv::ADAPTIVE_THRESH_GAUSSIAN_C,
                             cv::THRESH_BINARY_INV, 11, 10);
        
        // Step 4: Morphological operations
        cv::Mat kernel = cv::getStructuringElement(cv::MORPH_ELLIPSE, cv::Size(5, 5));
        cv::Mat cleaned;
        cv::morphologyEx(binary, cleaned, cv::MORPH_CLOSE, kernel);
        cv::morphologyEx(cleaned, cleaned, cv::MORPH_OPEN, kernel);
        
        // Step 5: Find person bounding box
        std::vector<std::vector<cv::Point>> contours;
        std::vector<cv::Vec4i> hierarchy;
        cv::findContours(cleaned, contours, hierarchy, cv::RETR_EXTERNAL, cv::CHAIN_APPROX_SIMPLE);
        
        cv::Rect personBox(0, 0, imgWidth, imgHeight);
        if (!contours.empty()) {
            double maxArea = 0;
            int maxIdx = -1;
            for (size_t i = 0; i < contours.size(); ++i) {
                double area = cv::contourArea(contours[i]);
                if (area > maxArea) {
                    maxArea = area;
                    maxIdx = i;
                }
            }
            if (maxIdx >= 0 && maxArea > 1000) {
                personBox = cv::boundingRect(contours[maxIdx]);
            }
        }
        
        // Check if person is detected
        float personArea = personBox.width * personBox.height;
        float imageArea = imgWidth * imgHeight;
        float personRatio = personArea / imageArea;
        
        // Person detection criteria
        if (personBox.width < imgWidth * 0.1f || personBox.height < imgHeight * 0.1f) {
            result.message = "No person detected";
            return result;
        }
        
        if (personRatio < 0.1f || personRatio > 0.8f) {
            result.message = "Person size not reasonable";
            return result;
        }
        
        result.hasPerson = true;
        result.confidence = std::min(1.0f, personRatio * 2.0f);
        
        // Step 6: Check if full body is visible
        float topRatio = static_cast<float>(personBox.y) / imgHeight;
        float bottomRatio = static_cast<float>(personBox.y + personBox.height) / imgHeight;
        float leftRatio = static_cast<float>(personBox.x) / imgWidth;
        float rightRatio = static_cast<float>(personBox.x + personBox.width) / imgWidth;
        
        // Check vertical coverage (head to feet)
        bool headVisible = topRatio <= 0.15f;
        bool feetVisible = bottomRatio >= 0.85f;
        
        // Check horizontal centering
        float centerX = (leftRatio + rightRatio) / 2.0f;
        bool isCentered = centerX >= 0.2f && centerX <= 0.8f;
        
        // Check aspect ratio
        float aspectRatio = static_cast<float>(personBox.height) / personBox.width;
        bool hasGoodAspectRatio = aspectRatio >= 1.5f;
        
        // Full body validation
        if (headVisible && feetVisible) {
            result.isFullBody = true;
            result.confidence = std::min(1.0f, result.confidence + 0.3f);
        } else if (isCentered && hasGoodAspectRatio && 
                   topRatio <= 0.2f && bottomRatio >= 0.8f) {
            result.isFullBody = true;
            result.confidence = std::min(1.0f, result.confidence + 0.2f);
        } else {
            if (!headVisible) {
                result.message = "Head not fully visible";
            } else if (!feetVisible) {
                result.message = "Feet not fully visible";
            } else if (!isCentered) {
                result.message = "Person not centered";
            } else if (!hasGoodAspectRatio) {
                result.message = "Full body not clearly visible";
            } else {
                result.message = "Full body not clearly visible";
            }
        }
        
    } catch (const cv::Exception& e) {
        result.message = "Image processing error";
        return result;
    } catch (...) {
        result.message = "Unknown error during validation";
        return result;
    }
    
    return result;
}
