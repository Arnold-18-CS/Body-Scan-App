#ifndef POSE_ESTIMATOR_H
#define POSE_ESTIMATOR_H

#include <opencv2/opencv.hpp>
#include <vector>

// TODO: Implementation will use MediaPipe for pose detection
class PoseEstimator {
public:
    /**
     * Detects 2D keypoints from an image
     * Returns 135 keypoints in normalized coordinates (0-1 range)
     * 
     * Implementation will use MediaPipe - currently returns stub
     * 
     * @param img Input image (RGB)
     * @return Vector of 135 2D keypoints (normalized x, y coordinates)
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

