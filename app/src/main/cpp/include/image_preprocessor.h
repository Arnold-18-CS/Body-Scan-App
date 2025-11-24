#ifndef IMAGE_PREPROCESSOR_H
#define IMAGE_PREPROCESSOR_H

#include <opencv2/opencv.hpp>

class ImagePreprocessor {
public:
    /**
     * Preprocesses an image for pose estimation
     * Applies CLAHE, optional background removal, and resizing
     * Modifies the input image in-place
     * 
     * @param img Input/output image (will be modified)
     */
    static void run(cv::Mat& img);
};

#endif

