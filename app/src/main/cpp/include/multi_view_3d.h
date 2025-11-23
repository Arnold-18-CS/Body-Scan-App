#ifndef MULTI_VIEW_3D_H
#define MULTI_VIEW_3D_H

#include <opencv2/opencv.hpp>
#include <vector>

class MultiView3D {
public:
    /**
     * Triangulates 3D keypoints from multiple 2D views
     * Uses multi-view stereo triangulation and scales using user height
     * 
     * @param kpts2d Vector of 3 views, each containing 135 2D keypoints (normalized 0-1)
     * @param userHeight User height in centimeters (for scaling)
     * @return Vector of 135 3D keypoints in real-world coordinates (centimeters)
     */
    static std::vector<cv::Point3f> triangulate(
        const std::vector<std::vector<cv::Point2f>>& kpts2d,
        float userHeight
    );
};

#endif

