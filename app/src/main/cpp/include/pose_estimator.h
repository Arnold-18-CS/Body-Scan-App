#ifndef POSE_ESTIMATOR_H
#define POSE_ESTIMATOR_H

#include <opencv2/opencv.hpp>
#include <vector>

class PoseEstimator {
public:
    /**
     * Detects 2D keypoints from an image
     * Returns 135 keypoints in normalized coordinates (0-1 range)
     * 
     * @param img Input image (RGB)
     * @return Vector of 135 2D keypoints (normalized x, y coordinates)
     */
    static std::vector<cv::Point2f> detect(const cv::Mat& img);
};

#endif

