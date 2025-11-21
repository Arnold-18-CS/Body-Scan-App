#include "pose_estimator.h"
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc.hpp>
#include <vector>
#include <cmath>
#include <algorithm>
#include <limits>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

// TODO: Implementation will use MediaPipe for pose detection
std::vector<cv::Point2f> PoseEstimator::detect(const cv::Mat& img) {
    // Stub implementation - will be replaced with MediaPipe
    const int numKeypoints = 135;
    std::vector<cv::Point2f> keypoints(numKeypoints, cv::Point2f(0.0f, 0.0f));
    
    if (img.empty() || img.cols <= 0 || img.rows <= 0) {
        return keypoints;
    }
    
    // TODO: Replace with MediaPipe pose detection
    // MediaPipe will provide 33 landmarks, which will be interpolated to 135 keypoints
    return keypoints;
}

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
