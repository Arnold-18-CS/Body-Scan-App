#include "multi_view_3d.h"
#include <opencv2/opencv.hpp>
#include <opencv2/calib3d.hpp>
#include <vector>
#include <cmath>
#include <algorithm>
#include <limits>

std::vector<cv::Point3f> MultiView3D::triangulate(
    const std::vector<std::vector<cv::Point2f>>& kpts2d,
    float userHeight) {
    
    const int numKeypoints = 135;
    std::vector<cv::Point3f> kpts3d;
    kpts3d.reserve(numKeypoints);

    // Validate input
    if (kpts2d.size() != 3) {
        // Return zeros if invalid input
        for (int i = 0; i < numKeypoints; ++i) {
            kpts3d.push_back(cv::Point3f(0.0f, 0.0f, 0.0f));
        }
        return kpts3d;
    }

    // Define camera poses for 3 views (front, left, right)
    // Assuming cameras are positioned around the person at equal angles
    const float cameraDistance = 200.0f; // cm - distance from person
    const float angleStep = 120.0f * M_PI / 180.0f; // 120 degrees between views
    
    // Camera 0: Front view (0 degrees)
    cv::Mat R0 = cv::Mat::eye(3, 3, CV_32F);
    cv::Mat t0 = (cv::Mat_<float>(3, 1) << 0.0f, 0.0f, cameraDistance);
    
    // Camera 1: Left view (120 degrees)
    cv::Mat R1 = (cv::Mat_<float>(3, 3) << 
        std::cos(angleStep), 0.0f, std::sin(angleStep),
        0.0f, 1.0f, 0.0f,
        -std::sin(angleStep), 0.0f, std::cos(angleStep));
    cv::Mat t1 = (cv::Mat_<float>(3, 1) << 
        cameraDistance * std::sin(angleStep), 
        0.0f, 
        cameraDistance * std::cos(angleStep));
    
    // Camera 2: Right view (-120 degrees)
    cv::Mat R2 = (cv::Mat_<float>(3, 3) << 
        std::cos(-angleStep), 0.0f, std::sin(-angleStep),
        0.0f, 1.0f, 0.0f,
        -std::sin(-angleStep), 0.0f, std::cos(-angleStep));
    cv::Mat t2 = (cv::Mat_<float>(3, 1) << 
        cameraDistance * std::sin(-angleStep), 
        0.0f, 
        cameraDistance * std::cos(-angleStep));

    // Camera intrinsic matrix (simplified - assumes square pixels, centered principal point)
    // Using a typical phone camera FOV (~60 degrees) and 640px width
    const float focalLength = 640.0f / (2.0f * std::tan(30.0f * M_PI / 180.0f));
    cv::Mat K = (cv::Mat_<float>(3, 3) << 
        focalLength, 0.0f, 320.0f,
        0.0f, focalLength, 320.0f,
        0.0f, 0.0f, 1.0f);

    // Projection matrices for each camera
    cv::Mat P0, P1, P2;
    cv::hconcat(R0, t0, P0);
    P0 = K * P0;
    cv::hconcat(R1, t1, P1);
    P1 = K * P1;
    cv::hconcat(R2, t2, P2);
    P2 = K * P2;

    // Process each keypoint
    for (int i = 0; i < numKeypoints; ++i) {
        // Get 2D keypoints from all 3 views (normalized 0-1, convert to pixel coordinates)
        std::vector<cv::Point2f> points2d;
        bool valid = true;
        
        for (int view = 0; view < 3; ++view) {
            if (i >= static_cast<int>(kpts2d[view].size())) {
                valid = false;
                break;
            }
            
            // Convert normalized coordinates to pixel coordinates (assuming 640x480 image)
            float px = kpts2d[view][i].x * 640.0f;
            float py = kpts2d[view][i].y * 480.0f;
            points2d.push_back(cv::Point2f(px, py));
        }

        if (!valid) {
            kpts3d.push_back(cv::Point3f(0.0f, 0.0f, 0.0f));
            continue;
        }

        // Triangulate using two views (front and left, or front and right)
        cv::Mat point4d;
        
        // Use front and left views for triangulation
        cv::triangulatePoints(P0, P1, 
            cv::Mat(points2d[0]), 
            cv::Mat(points2d[1]), 
            point4d);
        
        // Convert from homogeneous to 3D coordinates
        cv::Point3f point3d;
        if (point4d.at<float>(3, 0) != 0.0f) {
            point3d.x = point4d.at<float>(0, 0) / point4d.at<float>(3, 0);
            point3d.y = point4d.at<float>(1, 0) / point4d.at<float>(3, 0);
            point3d.z = point4d.at<float>(2, 0) / point4d.at<float>(3, 0);
        } else {
            point3d = cv::Point3f(0.0f, 0.0f, 0.0f);
        }

        // Scale using user height as reference
        // Estimate body height from keypoints (head to foot distance)
        // For now, use a simple scaling factor based on typical body proportions
        // A typical person's height in the coordinate system should match userHeight
        
        // Find approximate head and foot keypoints (simplified)
        // Keypoint 0 is typically head, keypoint 134 might be foot
        // We'll use a scaling approach based on the bounding box of all keypoints
        
        // For initial implementation, scale based on Y-axis range
        // This is a simplified approach - can be improved with better keypoint mapping
        static float scaleFactor = 1.0f;
        static bool scaleComputed = false;
        
        if (!scaleComputed && userHeight > 0.0f) {
            // Estimate scale from first few keypoints
            // Assume keypoints span roughly the person's height
            float minY = std::numeric_limits<float>::max();
            float maxY = std::numeric_limits<float>::lowest();
            
            for (int j = 0; j < std::min(50, static_cast<int>(kpts2d[0].size())); ++j) {
                float y = kpts2d[0][j].y;
                if (y > 0.0f && y < 1.0f) {
                    minY = std::min(minY, y);
                    maxY = std::max(maxY, y);
                }
            }
            
            if (maxY > minY) {
                // Estimate height in 3D space from triangulated points
                // Use a reference: if normalized Y spans 0.8 (head to foot), 
                // and userHeight is the real height, calculate scale
                float estimatedHeight3D = std::abs(maxY - minY) * 200.0f; // rough estimate
                if (estimatedHeight3D > 0.0f) {
                    scaleFactor = userHeight / estimatedHeight3D;
                }
            }
            scaleComputed = true;
        }
        
        // Apply scaling
        point3d.x *= scaleFactor;
        point3d.y *= scaleFactor;
        point3d.z *= scaleFactor;
        
        // Adjust coordinate system: Y should be height (positive up)
        // Z should be depth, X should be width
        // Our triangulation gives us camera-relative coordinates, so we adjust
        kpts3d.push_back(cv::Point3f(point3d.x, -point3d.y, point3d.z));
    }

    return kpts3d;
}

