#include "mesh_generator.h"
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc.hpp>
#include <vector>
#include <cstdint>
#include <cmath>
#include <algorithm>
#include <limits>

// Helper function to calculate circumference from ellipse
float calculateEllipseCircumference(float a, float b) {
    // Approximation: Ramanujan's formula for ellipse circumference
    float h = std::pow((a - b) / (a + b), 2);
    return M_PI * (a + b) * (1.0f + (3.0f * h) / (10.0f + std::sqrt(4.0f - 3.0f * h)));
}

// Helper function to fit ellipse to 3D points at a given Y level
float fitEllipseAtY(const std::vector<cv::Point3f>& kpts3d, float targetY, float tolerance) {
    std::vector<cv::Point2f> points2d;
    
    for (const auto& pt : kpts3d) {
        if (std::abs(pt.y - targetY) < tolerance) {
            // Project to XZ plane (width and depth)
            points2d.push_back(cv::Point2f(pt.x, pt.z));
        }
    }
    
    if (points2d.size() < 5) {
        return 0.0f; // Not enough points
    }
    
    // Fit ellipse
    cv::RotatedRect ellipse = cv::fitEllipse(points2d);
    float a = ellipse.size.width / 2.0f;
    float b = ellipse.size.height / 2.0f;
    
    return calculateEllipseCircumference(a, b);
}

std::vector<float> computeCircumferences(const std::vector<cv::Point3f>& kpts3d) {
    std::vector<float> measurements;
    
    if (kpts3d.empty() || kpts3d.size() < 10) {
        // Return zeros if insufficient data
        return std::vector<float>(7, 0.0f); // 7 measurements
    }
    
    // Find bounding box to determine body regions
    float minY = std::numeric_limits<float>::max();
    float maxY = std::numeric_limits<float>::lowest();
    
    for (const auto& pt : kpts3d) {
        minY = std::min(minY, pt.y);
        maxY = std::max(maxY, pt.y);
    }
    
    float height = maxY - minY;
    if (height <= 0.0f) {
        return std::vector<float>(7, 0.0f);
    }
    
    // Define body regions as fractions of height (from top to bottom)
    // These are approximate positions based on typical human proportions
    float headBottomY = minY + height * 0.10f;      // 10% from top
    float chestY = minY + height * 0.25f;             // 25% from top (chest level)
    float waistY = minY + height * 0.50f;             // 50% from top (waist level)
    float hipY = minY + height * 0.60f;               // 60% from top (hip level)
    float thighY = minY + height * 0.70f;            // 70% from top (thigh level)
    float armY = minY + height * 0.30f;                // 30% from top (arm level)
    
    float tolerance = height * 0.05f; // 5% of height tolerance
    
    // 1. Waist circumference
    float waist = fitEllipseAtY(kpts3d, waistY, tolerance);
    measurements.push_back(waist > 0.0f ? waist : 0.0f);
    
    // 2. Chest circumference
    float chest = fitEllipseAtY(kpts3d, chestY, tolerance);
    measurements.push_back(chest > 0.0f ? chest : 0.0f);
    
    // 3. Hip circumference
    float hips = fitEllipseAtY(kpts3d, hipY, tolerance);
    measurements.push_back(hips > 0.0f ? hips : 0.0f);
    
    // 4. Left thigh circumference
    // For thighs, we need to find points on the left side
    std::vector<cv::Point2f> leftThighPoints;
    float leftThighCenterX = 0.0f;
    int leftCount = 0;
    for (const auto& pt : kpts3d) {
        if (std::abs(pt.y - thighY) < tolerance && pt.x < 0.0f) { // Left side
            leftThighPoints.push_back(cv::Point2f(pt.x, pt.z));
            leftThighCenterX += pt.x;
            leftCount++;
        }
    }
    float leftThigh = 0.0f;
    if (leftThighPoints.size() >= 5) {
        cv::RotatedRect ellipse = cv::fitEllipse(leftThighPoints);
        float a = ellipse.size.width / 2.0f;
        float b = ellipse.size.height / 2.0f;
        leftThigh = calculateEllipseCircumference(a, b);
    }
    measurements.push_back(leftThigh);
    
    // 5. Right thigh circumference
    std::vector<cv::Point2f> rightThighPoints;
    for (const auto& pt : kpts3d) {
        if (std::abs(pt.y - thighY) < tolerance && pt.x > 0.0f) { // Right side
            rightThighPoints.push_back(cv::Point2f(pt.x, pt.z));
        }
    }
    float rightThigh = 0.0f;
    if (rightThighPoints.size() >= 5) {
        cv::RotatedRect ellipse = cv::fitEllipse(rightThighPoints);
        float a = ellipse.size.width / 2.0f;
        float b = ellipse.size.height / 2.0f;
        rightThigh = calculateEllipseCircumference(a, b);
    }
    measurements.push_back(rightThigh);
    
    // 6. Left arm circumference
    std::vector<cv::Point2f> leftArmPoints;
    for (const auto& pt : kpts3d) {
        if (std::abs(pt.y - armY) < tolerance && pt.x < 0.0f) { // Left side
            leftArmPoints.push_back(cv::Point2f(pt.x, pt.z));
        }
    }
    float leftArm = 0.0f;
    if (leftArmPoints.size() >= 5) {
        cv::RotatedRect ellipse = cv::fitEllipse(leftArmPoints);
        float a = ellipse.size.width / 2.0f;
        float b = ellipse.size.height / 2.0f;
        leftArm = calculateEllipseCircumference(a, b);
    }
    measurements.push_back(leftArm);
    
    // 7. Right arm circumference
    std::vector<cv::Point2f> rightArmPoints;
    for (const auto& pt : kpts3d) {
        if (std::abs(pt.y - armY) < tolerance && pt.x > 0.0f) { // Right side
            rightArmPoints.push_back(cv::Point2f(pt.x, pt.z));
        }
    }
    float rightArm = 0.0f;
    if (rightArmPoints.size() >= 5) {
        cv::RotatedRect ellipse = cv::fitEllipse(rightArmPoints);
        float a = ellipse.size.width / 2.0f;
        float b = ellipse.size.height / 2.0f;
        rightArm = calculateEllipseCircumference(a, b);
    }
    measurements.push_back(rightArm);
    
    // Ensure we return exactly 7 measurements
    while (measurements.size() < 7) {
        measurements.push_back(0.0f);
    }
    
    return measurements;
}

// Helper function to generate ellipsoid vertices
void generateEllipsoid(const cv::Point3f& center, float radiusX, float radiusY, float radiusZ,
                      int segments, std::vector<float>& vertices, std::vector<uint32_t>& indices,
                      uint32_t& indexOffset) {
    const int rings = segments / 2;
    const int sectors = segments;
    
    // Generate vertices
    for (int i = 0; i <= rings; ++i) {
        float theta = M_PI * i / rings; // 0 to PI
        float sinTheta = std::sin(theta);
        float cosTheta = std::cos(theta);
        
        for (int j = 0; j <= sectors; ++j) {
            float phi = 2.0f * M_PI * j / sectors; // 0 to 2*PI
            float sinPhi = std::sin(phi);
            float cosPhi = std::cos(phi);
            
            float x = center.x + radiusX * sinTheta * cosPhi;
            float y = center.y + radiusY * cosTheta;
            float z = center.z + radiusZ * sinTheta * sinPhi;
            
            vertices.push_back(x);
            vertices.push_back(y);
            vertices.push_back(z);
        }
    }
    
    // Generate indices
    for (int i = 0; i < rings; ++i) {
        for (int j = 0; j < sectors; ++j) {
            uint32_t first = indexOffset + i * (sectors + 1) + j;
            uint32_t second = indexOffset + (i + 1) * (sectors + 1) + j;
            
            indices.push_back(first);
            indices.push_back(second);
            indices.push_back(first + 1);
            
            indices.push_back(first + 1);
            indices.push_back(second);
            indices.push_back(second + 1);
        }
    }
    
    indexOffset += (rings + 1) * (sectors + 1);
}

// Helper function to create a simple GLB structure
// This is a minimal GLB implementation - for production, use tinygltf or similar
std::vector<uint8_t> createSimpleGLB(const std::vector<float>& vertices, 
                                     const std::vector<uint32_t>& indices) {
    // GLB format: 12-byte header + JSON chunk + BIN chunk
    // This is a simplified version - full GLB spec is more complex
    
    std::vector<uint8_t> glb;
    
    // GLB Header (12 bytes)
    // Magic: "glTF"
    glb.push_back(0x67); glb.push_back(0x6C); glb.push_back(0x54); glb.push_back(0x46); // "glTF"
    // Version: 2
    glb.push_back(0x02); glb.push_back(0x00); glb.push_back(0x00); glb.push_back(0x00);
    // Total length (will be updated later)
    uint32_t totalLength = 0;
    glb.insert(glb.end(), 4, 0);
    
    // For now, return a minimal valid structure
    // In production, this should use a proper GLB library like tinygltf
    // For Phase 2, we'll return a placeholder that indicates mesh was generated
    // The actual GLB generation can be completed when tinygltf is integrated
    
    // Placeholder: return a simple marker
    std::string marker = "GLB_MESH_PLACEHOLDER";
    glb.insert(glb.end(), marker.begin(), marker.end());
    
    // Add vertex data size info (for debugging)
    uint32_t vertexCount = vertices.size() / 3;
    uint32_t indexCount = indices.size();
    glb.insert(glb.end(), reinterpret_cast<const uint8_t*>(&vertexCount), 
               reinterpret_cast<const uint8_t*>(&vertexCount) + sizeof(vertexCount));
    glb.insert(glb.end(), reinterpret_cast<const uint8_t*>(&indexCount), 
               reinterpret_cast<const uint8_t*>(&indexCount) + sizeof(indexCount));
    
    return glb;
}

std::vector<uint8_t> MeshGenerator::createFromKeypoints(
    const std::vector<cv::Point3f>& kpts3d) {
    
    if (kpts3d.empty() || kpts3d.size() < 10) {
        return std::vector<uint8_t>(); // Return empty
    }
    
    // Find bounding box
    float minX = std::numeric_limits<float>::max();
    float maxX = std::numeric_limits<float>::lowest();
    float minY = std::numeric_limits<float>::max();
    float maxY = std::numeric_limits<float>::lowest();
    float minZ = std::numeric_limits<float>::max();
    float maxZ = std::numeric_limits<float>::lowest();
    
    for (const auto& pt : kpts3d) {
        minX = std::min(minX, pt.x);
        maxX = std::max(maxX, pt.x);
        minY = std::min(minY, pt.y);
        maxY = std::max(maxY, pt.y);
        minZ = std::min(minZ, pt.z);
        maxZ = std::max(maxZ, pt.z);
    }
    
    float height = maxY - minY;
    if (height <= 0.0f) {
        return std::vector<uint8_t>();
    }
    
    // Define body segments based on keypoint regions
    // Simplified: create ellipsoids for major body parts
    std::vector<float> allVertices;
    std::vector<uint32_t> allIndices;
    uint32_t indexOffset = 0;
    const int segments = 16; // Resolution of ellipsoids
    
    // 1. Head (top 10% of height)
    float headY = minY + height * 0.05f;
    float headRadius = height * 0.08f;
    cv::Point3f headCenter(0.0f, headY, 0.0f);
    generateEllipsoid(headCenter, headRadius, headRadius * 1.2f, headRadius, 
                      segments, allVertices, allIndices, indexOffset);
    
    // 2. Torso (shoulder to waist, 25% to 50% of height)
    float torsoY = minY + height * 0.375f;
    float torsoHeight = height * 0.25f;
    float torsoRadiusX = (maxX - minX) * 0.3f;
    float torsoRadiusZ = (maxZ - minZ) * 0.2f;
    cv::Point3f torsoCenter(0.0f, torsoY, 0.0f);
    generateEllipsoid(torsoCenter, torsoRadiusX, torsoHeight / 2.0f, torsoRadiusZ,
                      segments, allVertices, allIndices, indexOffset);
    
    // 3. Pelvis (waist to hip, 50% to 60% of height)
    float pelvisY = minY + height * 0.55f;
    float pelvisHeight = height * 0.10f;
    float pelvisRadiusX = (maxX - minX) * 0.25f;
    float pelvisRadiusZ = (maxZ - minZ) * 0.25f;
    cv::Point3f pelvisCenter(0.0f, pelvisY, 0.0f);
    generateEllipsoid(pelvisCenter, pelvisRadiusX, pelvisHeight / 2.0f, pelvisRadiusZ,
                      segments, allVertices, allIndices, indexOffset);
    
    // 4. Left thigh (hip to knee)
    float thighY = minY + height * 0.75f;
    float thighHeight = height * 0.15f;
    float thighRadius = height * 0.06f;
    cv::Point3f leftThighCenter(-(maxX - minX) * 0.15f, thighY, 0.0f);
    generateEllipsoid(leftThighCenter, thighRadius, thighHeight / 2.0f, thighRadius,
                      segments, allVertices, allIndices, indexOffset);
    
    // 5. Right thigh
    cv::Point3f rightThighCenter((maxX - minX) * 0.15f, thighY, 0.0f);
    generateEllipsoid(rightThighCenter, thighRadius, thighHeight / 2.0f, thighRadius,
                      segments, allVertices, allIndices, indexOffset);
    
    // 6. Left upper arm
    float armY = minY + height * 0.35f;
    float armHeight = height * 0.20f;
    float armRadius = height * 0.04f;
    cv::Point3f leftArmCenter(-(maxX - minX) * 0.25f, armY, 0.0f);
    generateEllipsoid(leftArmCenter, armRadius, armHeight / 2.0f, armRadius,
                      segments, allVertices, allIndices, indexOffset);
    
    // 7. Right upper arm
    cv::Point3f rightArmCenter((maxX - minX) * 0.25f, armY, 0.0f);
    generateEllipsoid(rightArmCenter, armRadius, armHeight / 2.0f, armRadius,
                      segments, allVertices, allIndices, indexOffset);
    
    // Create GLB from vertices and indices
    return createSimpleGLB(allVertices, allIndices);
}

