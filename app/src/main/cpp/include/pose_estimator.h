#ifndef POSE_ESTIMATOR_H
#define POSE_ESTIMATOR_H

#include <opencv2/opencv.hpp>
#include <vector>

/**
 * Pose Estimator class using MediaPipe for pose detection.
 * 
 * This class uses MediaPipe Pose Landmarker to detect 33 landmarks,
 * which are then mapped to 135 keypoints for compatibility with the
 * existing codebase.
 */
class PoseEstimator {
public:
    /**
     * Detects 2D keypoints from an image using MediaPipe.
     * Returns 135 keypoints in normalized coordinates (0-1 range).
     * 
     * MediaPipe provides 33 landmarks which are mapped to 135 keypoints
     * through interpolation and direct mapping.
     * 
     * @param img Input image (RGB, OpenCV Mat)
     * @return Vector of 135 2D keypoints (normalized x, y coordinates)
     *         Returns zeros if MediaPipe not initialized or no person detected
     */
    static std::vector<cv::Point2f> detect(const cv::Mat& img);
    
    /**
     * Validation result structure
     */
    struct ValidationResult {
        bool hasPerson;
        bool isFullBody;
        float confidence;
        std::string message;
    };
    
    /**
     * Validates if an image contains a person and full body is visible
     * 
     * @param img Input image (RGB)
     * @return ValidationResult with validation status and message
     */
    static ValidationResult validateImage(const cv::Mat& img);
};

#endif

