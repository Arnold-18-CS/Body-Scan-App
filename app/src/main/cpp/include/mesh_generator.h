#ifndef MESH_GENERATOR_H
#define MESH_GENERATOR_H

#include <opencv2/opencv.hpp>
#include <vector>
#include <cstdint>

class MeshGenerator {
public:
    /**
     * Creates a 3D mesh from 3D keypoints
     * Generates ellipsoids/cylinders for each body segment and serializes to GLB format
     * 
     * @param kpts3d Vector of 135 3D keypoints (in centimeters)
     * @return GLB binary data as vector of bytes
     */
    static std::vector<uint8_t> createFromKeypoints(
        const std::vector<cv::Point3f>& kpts3d
    );
};

/**
 * Computes body circumferences from 3D keypoints
 * Calculates waist, chest, hips, thighs, and arms circumferences
 * 
 * @param kpts3d Vector of 135 3D keypoints (in centimeters)
 * @return Vector of measurements in cm: [waist, chest, hips, left_thigh, right_thigh, left_arm, right_arm]
 */
std::vector<float> computeCircumferences(const std::vector<cv::Point3f>& kpts3d);

#endif

