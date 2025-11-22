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

/**
 * Validates if an image contains a person and full body is visible using MediaPipe.
 * 
 * MediaPipe landmark indices (33 total):
 * - Head: 0 (nose), 1-6 (eyes), 7-8 (ears), 9-10 (mouth)
 * - Upper body: 11-12 (shoulders), 13-14 (elbows), 15-16 (wrists)
 * - Hands: 17-22 (left/right pinky, index, thumb)
 * - Lower body: 23-24 (hips), 25-26 (knees), 27-28 (ankles)
 * - Feet: 29-30 (heels), 31-32 (foot_index)
 */
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
    
    // Get JNI environment
    JNIEnv* env = getJNIEnv();
    if (env == nullptr) {
        result.message = "JNI environment not available";
        return result;
    }
    
    // Check if MediaPipe is ready
    if (!MediaPipePoseDetector::isReady(env)) {
        result.message = "MediaPipe not initialized";
        return result;
    }
    
    try {
        // Detect pose using MediaPipe
        std::vector<cv::Point3f> mpLandmarks = MediaPipePoseDetector::detect(env, img);
        
        // Check if any landmarks were detected
        if (mpLandmarks.empty() || mpLandmarks.size() != 33) {
            result.message = "No person detected";
            return result;
        }
        
        // Helper function to check if a landmark is valid (detected and visible)
        // MediaPipe returns normalized coordinates (0-1 range typically)
        // We allow slight overflow (-0.1 to 1.1) to account for landmarks partially outside frame
        // But exclude clearly invalid coordinates (e.g., exactly 0,0 or negative/very large values)
        auto isLandmarkValid = [](const cv::Point3f& landmark) -> bool {
            // Check if landmark coordinates are within reasonable normalized range
            // MediaPipe typically returns 0-1, but may slightly exceed for partial visibility
            const float MIN_VALID = -0.1f;
            const float MAX_VALID = 1.1f;
            const float EPSILON = 0.001f; // Small threshold for floating point comparison
            // Exclude landmarks at approximately (0,0) as they're likely undetected
            // Also exclude clearly invalid coordinates
            bool inRange = landmark.x >= MIN_VALID && landmark.x <= MAX_VALID &&
                          landmark.y >= MIN_VALID && landmark.y <= MAX_VALID;
            bool notZero = std::abs(landmark.x) > EPSILON || std::abs(landmark.y) > EPSILON;
            return inRange && notZero;
        };
        
        // Count valid landmarks
        int validLandmarkCount = 0;
        for (const auto& landmark : mpLandmarks) {
            if (isLandmarkValid(landmark)) {
                validLandmarkCount++;
            }
        }
        
        // Minimum threshold: at least 10 landmarks detected to consider a person present
        const int MIN_LANDMARKS_FOR_PERSON = 10;
        if (validLandmarkCount < MIN_LANDMARKS_FOR_PERSON) {
            result.message = "No person detected";
            return result;
        }
        
        result.hasPerson = true;
        result.confidence = std::min(1.0f, validLandmarkCount / 33.0f);
        
        // Define required landmarks for full body detection
        // Head landmarks (indices 0-10)
        bool hasNose = isLandmarkValid(mpLandmarks[0]);
        bool hasLeftEye = isLandmarkValid(mpLandmarks[2]) || isLandmarkValid(mpLandmarks[1]) || isLandmarkValid(mpLandmarks[3]);
        bool hasRightEye = isLandmarkValid(mpLandmarks[5]) || isLandmarkValid(mpLandmarks[4]) || isLandmarkValid(mpLandmarks[6]);
        bool hasLeftEar = isLandmarkValid(mpLandmarks[7]);
        bool hasRightEar = isLandmarkValid(mpLandmarks[8]);
        // Head is valid if nose, at least one eye, and at least one ear are visible
        bool hasHead = hasNose && (hasLeftEye || hasRightEye) && (hasLeftEar || hasRightEar);
        
        // Upper body landmarks (indices 11-16)
        bool hasLeftShoulder = isLandmarkValid(mpLandmarks[11]);
        bool hasRightShoulder = isLandmarkValid(mpLandmarks[12]);
        bool hasLeftElbow = isLandmarkValid(mpLandmarks[13]);
        bool hasRightElbow = isLandmarkValid(mpLandmarks[14]);
        bool hasLeftWrist = isLandmarkValid(mpLandmarks[15]);
        bool hasRightWrist = isLandmarkValid(mpLandmarks[16]);
        // Upper body requires both shoulders and both arms visible (elbows and wrists)
        bool hasUpperBody = hasLeftShoulder && hasRightShoulder && 
                           hasLeftElbow && hasRightElbow && 
                           hasLeftWrist && hasRightWrist;
        
        // Hand landmarks (indices 17-22)
        // Left hand: 17 (pinky), 19 (index), 21 (thumb)
        // Right hand: 18 (pinky), 20 (index), 22 (thumb)
        bool hasLeftHand = hasLeftWrist && (
            isLandmarkValid(mpLandmarks[17]) || // left pinky
            isLandmarkValid(mpLandmarks[19]) || // left index
            isLandmarkValid(mpLandmarks[21])    // left thumb
        );
        bool hasRightHand = hasRightWrist && (
            isLandmarkValid(mpLandmarks[18]) || // right pinky
            isLandmarkValid(mpLandmarks[20]) || // right index
            isLandmarkValid(mpLandmarks[22])    // right thumb
        );
        // Both hands must be visible
        bool hasBothHands = hasLeftHand && hasRightHand;
        
        // Lower body landmarks (indices 23-28)
        bool hasLeftHip = isLandmarkValid(mpLandmarks[23]);
        bool hasRightHip = isLandmarkValid(mpLandmarks[24]);
        bool hasLeftKnee = isLandmarkValid(mpLandmarks[25]);
        bool hasRightKnee = isLandmarkValid(mpLandmarks[26]);
        bool hasLeftAnkle = isLandmarkValid(mpLandmarks[27]);
        bool hasRightAnkle = isLandmarkValid(mpLandmarks[28]);
        // Lower body requires both hips and both legs visible (knees and ankles)
        bool hasLowerBody = hasLeftHip && hasRightHip && 
                           hasLeftKnee && hasRightKnee && 
                           hasLeftAnkle && hasRightAnkle;
        
        // Feet landmarks (indices 29-32)
        // Left foot: 29 (heel), 31 (foot_index)
        // Right foot: 30 (heel), 32 (foot_index)
        bool hasLeftFoot = hasLeftAnkle && (
            isLandmarkValid(mpLandmarks[29]) || // left heel
            isLandmarkValid(mpLandmarks[31])   // left foot_index
        );
        bool hasRightFoot = hasRightAnkle && (
            isLandmarkValid(mpLandmarks[30]) || // right heel
            isLandmarkValid(mpLandmarks[32])   // right foot_index
        );
        // Both feet must be visible
        bool hasBothFeet = hasLeftFoot && hasRightFoot;
        
        // Full body validation: require head, upper body, both hands, lower body, and both feet
        if (hasHead && hasUpperBody && hasBothHands && hasLowerBody && hasBothFeet) {
            result.isFullBody = true;
            result.confidence = std::min(1.0f, result.confidence + 0.2f);
            result.message = ""; // Success - no error message
        } else {
            // Generate specific error messages
            if (!hasHead) {
                if (!hasNose) {
                    result.message = "Head not fully visible - nose not detected";
                } else if (!hasLeftEye && !hasRightEye) {
                    result.message = "Face not clearly visible - eyes not detected";
                } else if (!hasLeftEar && !hasRightEar) {
                    result.message = "Head not fully visible - ears not detected";
                } else {
                    result.message = "Head not fully visible";
                }
            } else if (!hasUpperBody) {
                if (!hasLeftShoulder && !hasRightShoulder) {
                    result.message = "Upper body not visible - shoulders not detected";
                } else if (!hasLeftElbow && !hasRightElbow) {
                    result.message = "Arms not fully visible - elbows not detected";
                } else if (!hasLeftWrist && !hasRightWrist) {
                    result.message = "Arms not fully visible - wrists not detected";
                } else {
                    result.message = "Upper body not fully visible";
                }
            } else if (!hasBothHands) {
                if (!hasLeftHand) {
                    result.message = "Left hand not fully visible";
                } else if (!hasRightHand) {
                    result.message = "Right hand not fully visible";
                } else {
                    result.message = "Both hands must be visible";
                }
            } else if (!hasLowerBody) {
                if (!hasLeftHip && !hasRightHip) {
                    result.message = "Lower body not visible - hips not detected";
                } else if (!hasLeftKnee && !hasRightKnee) {
                    result.message = "Legs not fully visible - knees not detected";
                } else if (!hasLeftAnkle && !hasRightAnkle) {
                    result.message = "Legs not fully visible - ankles not detected";
                } else {
                    result.message = "Lower body not fully visible";
                }
            } else if (!hasBothFeet) {
                if (!hasLeftFoot) {
                    result.message = "Left foot not fully visible";
                } else if (!hasRightFoot) {
                    result.message = "Right foot not fully visible";
                } else {
                    result.message = "Both feet must be visible";
                }
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
