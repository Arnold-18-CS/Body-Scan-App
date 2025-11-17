#include "image_preprocessor.h"
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/imgcodecs.hpp>

void ImagePreprocessor::run(cv::Mat& img) {
    if (img.empty()) {
        return;
    }

    // Convert to appropriate color space if needed
    cv::Mat processed;
    if (img.channels() == 4) {
        cv::cvtColor(img, processed, cv::COLOR_RGBA2RGB);
    } else if (img.channels() == 1) {
        cv::cvtColor(img, processed, cv::COLOR_GRAY2RGB);
    } else {
        processed = img.clone();
    }

    // Resize if image is too large (target ~640px width for performance)
    const int targetWidth = 640;
    if (processed.cols > targetWidth) {
        double scale = static_cast<double>(targetWidth) / processed.cols;
        int newHeight = static_cast<int>(processed.rows * scale);
        cv::resize(processed, processed, cv::Size(targetWidth, newHeight), 0, 0, cv::INTER_LINEAR);
    }

    // Apply CLAHE (Contrast Limited Adaptive Histogram Equalization)
    // Convert to LAB color space for better contrast enhancement
    // Note: OpenCV uses BGR by default, but we have RGB, so convert RGB->BGR first
    cv::Mat bgr;
    cv::cvtColor(processed, bgr, cv::COLOR_RGB2BGR);
    
    cv::Mat lab;
    cv::cvtColor(bgr, lab, cv::COLOR_BGR2Lab);
    
    std::vector<cv::Mat> labChannels;
    cv::split(lab, labChannels);
    
    // Apply CLAHE to L channel only
    cv::Ptr<cv::CLAHE> clahe = cv::createCLAHE(2.0, cv::Size(8, 8));
    clahe->apply(labChannels[0], labChannels[0]);
    
    // Merge channels back
    cv::merge(labChannels, lab);
    cv::cvtColor(lab, bgr, cv::COLOR_Lab2BGR);
    
    // Convert back to RGB
    cv::cvtColor(bgr, processed, cv::COLOR_BGR2RGB);

    // Optional: Simple background removal using thresholding
    // This is a basic implementation - can be enhanced with GrabCut or more sophisticated methods
    // For now, we'll skip background removal as it requires more complex setup
    // and can be added later if needed

    // Update the input image
    img = processed;
}

