#include "mesh_generator.h"
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc.hpp>
#include <vector>
#include <cstdint>
#include <cmath>
#include <algorithm>
#include <limits>
#include <string>
#include <sstream>
#include <cstring>
#include <iomanip>

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

// Helper function to calculate distance between two 3D points
float distance3D(const cv::Point3f& a, const cv::Point3f& b) {
    float dx = a.x - b.x;
    float dy = a.y - b.y;
    float dz = a.z - b.z;
    return std::sqrt(dx * dx + dy * dy + dz * dz);
}

// Helper function to check if keypoint is valid (not zero or NaN)
bool isValidKeypoint(const cv::Point3f& pt) {
    return !(std::isnan(pt.x) || std::isnan(pt.y) || std::isnan(pt.z) ||
             std::isinf(pt.x) || std::isinf(pt.y) || std::isinf(pt.z) ||
             (pt.x == 0.0f && pt.y == 0.0f && pt.z == 0.0f));
}

// Helper function to generate ellipsoid vertices with normals
void generateEllipsoid(const cv::Point3f& center, float radiusX, float radiusY, float radiusZ,
                      int segments, std::vector<float>& vertices, std::vector<float>& normals,
                      std::vector<uint32_t>& indices, uint32_t& indexOffset) {
    const int rings = segments / 2;
    const int sectors = segments;
    
    // Generate vertices and normals
    for (int i = 0; i <= rings; ++i) {
        float theta = M_PI * i / rings; // 0 to PI
        float sinTheta = std::sin(theta);
        float cosTheta = std::cos(theta);
        
        for (int j = 0; j <= sectors; ++j) {
            float phi = 2.0f * M_PI * j / sectors; // 0 to 2*PI
            float sinPhi = std::sin(phi);
            float cosPhi = std::cos(phi);
            
            // Vertex position
            float x = center.x + radiusX * sinTheta * cosPhi;
            float y = center.y + radiusY * cosTheta;
            float z = center.z + radiusZ * sinTheta * sinPhi;
            
            vertices.push_back(x);
            vertices.push_back(y);
            vertices.push_back(z);
            
            // Normal (normalized direction from center to vertex)
            float nx = sinTheta * cosPhi;
            float ny = cosTheta;
            float nz = sinTheta * sinPhi;
            float norm = std::sqrt(nx * nx + ny * ny + nz * nz);
            if (norm > 0.0001f) {
                nx /= norm;
                ny /= norm;
                nz /= norm;
            }
            
            normals.push_back(nx);
            normals.push_back(ny);
            normals.push_back(nz);
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

// Helper function to write uint32_t in little-endian
void writeUint32LE(std::vector<uint8_t>& data, uint32_t value) {
    data.push_back(static_cast<uint8_t>(value & 0xFF));
    data.push_back(static_cast<uint8_t>((value >> 8) & 0xFF));
    data.push_back(static_cast<uint8_t>((value >> 16) & 0xFF));
    data.push_back(static_cast<uint8_t>((value >> 24) & 0xFF));
}

// Helper function to write float in little-endian
void writeFloatLE(std::vector<uint8_t>& data, float value) {
    uint32_t bits;
    std::memcpy(&bits, &value, sizeof(float));
    writeUint32LE(data, bits);
}

// Helper function to create GLB manually (without tinygltf dependency)
std::vector<uint8_t> createGLBManually(
    const std::vector<float>& vertices,
    const std::vector<float>& normals,
    const std::vector<uint32_t>& indices) {
    
    if (vertices.empty() || indices.empty()) {
        return std::vector<uint8_t>();
    }
    
    size_t vertexCount = vertices.size() / 3;
    size_t normalCount = normals.size() / 3;
    size_t indexCount = indices.size();
    
    // Calculate bounding box
    float minX = vertices[0], maxX = vertices[0];
    float minY = vertices[1], maxY = vertices[1];
    float minZ = vertices[2], maxZ = vertices[2];
    
    for (size_t i = 0; i < vertices.size(); i += 3) {
        minX = std::min(minX, vertices[i]);
        maxX = std::max(maxX, vertices[i]);
        minY = std::min(minY, vertices[i + 1]);
        maxY = std::max(maxY, vertices[i + 1]);
        minZ = std::min(minZ, vertices[i + 2]);
        maxZ = std::max(maxZ, vertices[i + 2]);
    }
    
    // Build binary buffer: vertices + normals + indices
    std::vector<uint8_t> binaryBuffer;
    binaryBuffer.resize(vertices.size() * sizeof(float) + 
                       normals.size() * sizeof(float) + 
                       indices.size() * sizeof(uint32_t));
    
    size_t offset = 0;
    std::memcpy(binaryBuffer.data() + offset, vertices.data(), vertices.size() * sizeof(float));
    offset += vertices.size() * sizeof(float);
    std::memcpy(binaryBuffer.data() + offset, normals.data(), normals.size() * sizeof(float));
    offset += normals.size() * sizeof(float);
    std::memcpy(binaryBuffer.data() + offset, indices.data(), indices.size() * sizeof(uint32_t));
    
    // Build JSON string manually
    std::ostringstream jsonStream;
    jsonStream << std::fixed << std::setprecision(6);
    jsonStream << "{\n";
    jsonStream << "  \"asset\": {\"version\": \"2.0\"},\n";
    jsonStream << "  \"scene\": 0,\n";
    jsonStream << "  \"scenes\": [{\"nodes\": [0]}],\n";
    jsonStream << "  \"nodes\": [{\"mesh\": 0}],\n";
    jsonStream << "  \"meshes\": [{\n";
    jsonStream << "    \"primitives\": [{\n";
    jsonStream << "      \"attributes\": {\"POSITION\": 0, \"NORMAL\": 1},\n";
    jsonStream << "      \"indices\": 2,\n";
    jsonStream << "      \"material\": 0\n";
    jsonStream << "    }]\n";
    jsonStream << "  }],\n";
    jsonStream << "  \"materials\": [{\n";
    jsonStream << "    \"pbrMetallicRoughness\": {\n";
    jsonStream << "      \"baseColorFactor\": [0.8, 0.8, 0.8, 1.0],\n";
    jsonStream << "      \"metallicFactor\": 0.0,\n";
    jsonStream << "      \"roughnessFactor\": 0.5\n";
    jsonStream << "    },\n";
    jsonStream << "    \"doubleSided\": true\n";
    jsonStream << "  }],\n";
    jsonStream << "  \"buffers\": [{\"byteLength\": " << binaryBuffer.size() << "}],\n";
    jsonStream << "  \"bufferViews\": [\n";
    jsonStream << "    {\"buffer\": 0, \"byteOffset\": 0, \"byteLength\": " 
               << (vertices.size() * sizeof(float)) << ", \"target\": 34962},\n";
    jsonStream << "    {\"buffer\": 0, \"byteOffset\": " << (vertices.size() * sizeof(float))
               << ", \"byteLength\": " << (normals.size() * sizeof(float)) << ", \"target\": 34962},\n";
    jsonStream << "    {\"buffer\": 0, \"byteOffset\": " 
               << (vertices.size() * sizeof(float) + normals.size() * sizeof(float))
               << ", \"byteLength\": " << (indices.size() * sizeof(uint32_t)) << ", \"target\": 34963}\n";
    jsonStream << "  ],\n";
    jsonStream << "  \"accessors\": [\n";
    jsonStream << "    {\"bufferView\": 0, \"byteOffset\": 0, \"componentType\": 5126, \"count\": " 
               << vertexCount << ", \"type\": \"VEC3\", \"min\": [" 
               << minX << "," << minY << "," << minZ << "], \"max\": [" 
               << maxX << "," << maxY << "," << maxZ << "]},\n";
    jsonStream << "    {\"bufferView\": 1, \"byteOffset\": 0, \"componentType\": 5126, \"count\": " 
               << normalCount << ", \"type\": \"VEC3\"},\n";
    jsonStream << "    {\"bufferView\": 2, \"byteOffset\": 0, \"componentType\": 5125, \"count\": " 
               << indexCount << ", \"type\": \"SCALAR\"}\n";
    jsonStream << "  ]\n";
    jsonStream << "}\n";
    
    std::string jsonString = jsonStream.str();
    
    // Pad JSON to 4-byte boundary
    size_t jsonPadding = (4 - (jsonString.size() % 4)) % 4;
    jsonString.append(jsonPadding, ' ');
    
    // Pad binary to 4-byte boundary
    size_t binaryPadding = (4 - (binaryBuffer.size() % 4)) % 4;
    binaryBuffer.insert(binaryBuffer.end(), binaryPadding, 0);
    
    // Build GLB
    std::vector<uint8_t> glb;
    
    // GLB Header (12 bytes)
    // Magic: "glTF"
    glb.push_back(0x67); glb.push_back(0x6C); glb.push_back(0x54); glb.push_back(0x46);
    // Version: 2
    writeUint32LE(glb, 2);
    // Total length (will be updated later)
    uint32_t totalLengthPos = glb.size();
    writeUint32LE(glb, 0); // Placeholder
    
    // JSON Chunk (12 bytes header + JSON data)
    uint32_t jsonChunkLength = 12 + jsonString.size();
    writeUint32LE(glb, jsonString.size());
    // Chunk type: JSON (0x4E4F534A = "JSON")
    glb.push_back(0x4A); glb.push_back(0x4F); glb.push_back(0x53); glb.push_back(0x4E);
    // JSON data
    glb.insert(glb.end(), jsonString.begin(), jsonString.end());
    
    // BIN Chunk (12 bytes header + binary data)
    uint32_t binChunkLength = 12 + binaryBuffer.size();
    writeUint32LE(glb, binaryBuffer.size());
    // Chunk type: BIN (0x004E4942 = "BIN\0")
    glb.push_back(0x42); glb.push_back(0x49); glb.push_back(0x4E); glb.push_back(0x00);
    // Binary data
    glb.insert(glb.end(), binaryBuffer.begin(), binaryBuffer.end());
    
    // Update total length
    uint32_t totalLength = glb.size();
    std::memcpy(glb.data() + totalLengthPos, &totalLength, sizeof(uint32_t));
    
    return glb;
}

std::vector<uint8_t> MeshGenerator::createFromKeypoints(
    const std::vector<cv::Point3f>& kpts3d) {
    
    // BODY_25 keypoint indices (assuming kpts3d has at least 25 keypoints)
    // 0: nose, 1: neck, 2: right_shoulder, 3: right_elbow, 4: right_wrist,
    // 5: left_shoulder, 6: left_elbow, 7: left_wrist,
    // 8: mid_hip, 9: right_hip, 10: right_knee, 11: right_ankle,
    // 12: left_hip, 13: left_knee, 14: left_ankle,
    // 15-18: eyes and ears
    
    if (kpts3d.empty() || kpts3d.size() < 19) {
        return std::vector<uint8_t>(); // Return empty if insufficient keypoints
    }
    
    // Validate keypoints
    int validKeypoints = 0;
    for (const auto& pt : kpts3d) {
        if (isValidKeypoint(pt)) {
            validKeypoints++;
        }
    }
    
    if (validKeypoints < 10) {
        return std::vector<uint8_t>(); // Not enough valid keypoints
    }
    
    // Extract key BODY_25 keypoints (assuming first 25 are BODY_25)
    const size_t BODY_25_COUNT = 25;
    size_t keypointCount = std::min(kpts3d.size(), BODY_25_COUNT);
    
    cv::Point3f nose = (keypointCount > 0 && isValidKeypoint(kpts3d[0])) ? kpts3d[0] : cv::Point3f(0, 0, 0);
    cv::Point3f neck = (keypointCount > 1 && isValidKeypoint(kpts3d[1])) ? kpts3d[1] : cv::Point3f(0, 0, 0);
    cv::Point3f rightShoulder = (keypointCount > 2 && isValidKeypoint(kpts3d[2])) ? kpts3d[2] : cv::Point3f(0, 0, 0);
    cv::Point3f rightElbow = (keypointCount > 3 && isValidKeypoint(kpts3d[3])) ? kpts3d[3] : cv::Point3f(0, 0, 0);
    cv::Point3f rightWrist = (keypointCount > 4 && isValidKeypoint(kpts3d[4])) ? kpts3d[4] : cv::Point3f(0, 0, 0);
    cv::Point3f leftShoulder = (keypointCount > 5 && isValidKeypoint(kpts3d[5])) ? kpts3d[5] : cv::Point3f(0, 0, 0);
    cv::Point3f leftElbow = (keypointCount > 6 && isValidKeypoint(kpts3d[6])) ? kpts3d[6] : cv::Point3f(0, 0, 0);
    cv::Point3f leftWrist = (keypointCount > 7 && isValidKeypoint(kpts3d[7])) ? kpts3d[7] : cv::Point3f(0, 0, 0);
    cv::Point3f midHip = (keypointCount > 8 && isValidKeypoint(kpts3d[8])) ? kpts3d[8] : cv::Point3f(0, 0, 0);
    cv::Point3f rightHip = (keypointCount > 9 && isValidKeypoint(kpts3d[9])) ? kpts3d[9] : cv::Point3f(0, 0, 0);
    cv::Point3f rightKnee = (keypointCount > 10 && isValidKeypoint(kpts3d[10])) ? kpts3d[10] : cv::Point3f(0, 0, 0);
    cv::Point3f rightAnkle = (keypointCount > 11 && isValidKeypoint(kpts3d[11])) ? kpts3d[11] : cv::Point3f(0, 0, 0);
    cv::Point3f leftHip = (keypointCount > 12 && isValidKeypoint(kpts3d[12])) ? kpts3d[12] : cv::Point3f(0, 0, 0);
    cv::Point3f leftKnee = (keypointCount > 13 && isValidKeypoint(kpts3d[13])) ? kpts3d[13] : cv::Point3f(0, 0, 0);
    cv::Point3f leftAnkle = (keypointCount > 14 && isValidKeypoint(kpts3d[14])) ? kpts3d[14] : cv::Point3f(0, 0, 0);
    
    // Calculate body dimensions from keypoints
    float bodyHeight = 0.0f;
    if (isValidKeypoint(nose) && isValidKeypoint(midHip)) {
        bodyHeight = distance3D(nose, midHip) * 2.0f; // Approximate full height
    } else {
        // Fallback: use bounding box
        float minY = std::numeric_limits<float>::max();
        float maxY = std::numeric_limits<float>::lowest();
        for (const auto& pt : kpts3d) {
            if (isValidKeypoint(pt)) {
                minY = std::min(minY, pt.y);
                maxY = std::max(maxY, pt.y);
            }
        }
        bodyHeight = maxY - minY;
    }
    
    if (bodyHeight <= 0.0f) {
        return std::vector<uint8_t>();
    }
    
    // Generate mesh segments using actual keypoint positions
    std::vector<float> allVertices;
    std::vector<float> allNormals;
    std::vector<uint32_t> allIndices;
    uint32_t indexOffset = 0;
    const int segments = 16; // Resolution of ellipsoids
    
    // 1. Head: from nose to neck
    if (isValidKeypoint(nose) && isValidKeypoint(neck)) {
        float headHeight = distance3D(nose, neck);
        float headRadius = headHeight * 0.4f; // Head is roughly spherical
        cv::Point3f headCenter = (nose + neck) * 0.5f;
        headCenter.y = nose.y - headHeight * 0.3f; // Position above neck
        generateEllipsoid(headCenter, headRadius, headHeight * 0.6f, headRadius,
                         segments, allVertices, allNormals, allIndices, indexOffset);
    }
    
    // 2. Torso: from neck to mid-hip
    if (isValidKeypoint(neck) && isValidKeypoint(midHip)) {
        float torsoHeight = distance3D(neck, midHip);
        cv::Point3f torsoCenter = (neck + midHip) * 0.5f;
        
        // Calculate torso width from shoulders
        float torsoWidth = 0.0f;
        if (isValidKeypoint(rightShoulder) && isValidKeypoint(leftShoulder)) {
            torsoWidth = distance3D(rightShoulder, leftShoulder) * 0.6f;
        } else {
            torsoWidth = bodyHeight * 0.15f; // Fallback
        }
        
        // Calculate torso depth (approximate)
        float torsoDepth = torsoWidth * 0.7f;
        
        generateEllipsoid(torsoCenter, torsoWidth * 0.5f, torsoHeight * 0.5f, torsoDepth * 0.5f,
                         segments, allVertices, allNormals, allIndices, indexOffset);
    }
    
    // 3. Pelvis: around mid-hip
    if (isValidKeypoint(midHip)) {
        float pelvisHeight = bodyHeight * 0.08f;
        float pelvisWidth = 0.0f;
        if (isValidKeypoint(rightHip) && isValidKeypoint(leftHip)) {
            pelvisWidth = distance3D(rightHip, leftHip) * 0.8f;
        } else {
            pelvisWidth = bodyHeight * 0.12f; // Fallback
        }
        float pelvisDepth = pelvisWidth * 0.8f;
        
        cv::Point3f pelvisCenter = midHip;
        pelvisCenter.y += pelvisHeight * 0.3f; // Slightly below mid-hip
        generateEllipsoid(pelvisCenter, pelvisWidth * 0.5f, pelvisHeight * 0.5f, pelvisDepth * 0.5f,
                         segments, allVertices, allNormals, allIndices, indexOffset);
    }
    
    // 4. Right thigh: from right hip to right knee
    if (isValidKeypoint(rightHip) && isValidKeypoint(rightKnee)) {
        float thighLength = distance3D(rightHip, rightKnee);
        cv::Point3f thighCenter = (rightHip + rightKnee) * 0.5f;
        float thighRadius = thighLength * 0.15f; // Thigh is roughly cylindrical
        
        generateEllipsoid(thighCenter, thighRadius, thighLength * 0.5f, thighRadius,
                         segments, allVertices, allNormals, allIndices, indexOffset);
    }
    
    // 5. Left thigh: from left hip to left knee
    if (isValidKeypoint(leftHip) && isValidKeypoint(leftKnee)) {
        float thighLength = distance3D(leftHip, leftKnee);
        cv::Point3f thighCenter = (leftHip + leftKnee) * 0.5f;
        float thighRadius = thighLength * 0.15f;
        
        generateEllipsoid(thighCenter, thighRadius, thighLength * 0.5f, thighRadius,
                         segments, allVertices, allNormals, allIndices, indexOffset);
    }
    
    // 6. Right lower leg: from right knee to right ankle
    if (isValidKeypoint(rightKnee) && isValidKeypoint(rightAnkle)) {
        float legLength = distance3D(rightKnee, rightAnkle);
        cv::Point3f legCenter = (rightKnee + rightAnkle) * 0.5f;
        float legRadius = legLength * 0.12f;
        
        generateEllipsoid(legCenter, legRadius, legLength * 0.5f, legRadius,
                         segments, allVertices, allNormals, allIndices, indexOffset);
    }
    
    // 7. Left lower leg: from left knee to left ankle
    if (isValidKeypoint(leftKnee) && isValidKeypoint(leftAnkle)) {
        float legLength = distance3D(leftKnee, leftAnkle);
        cv::Point3f legCenter = (leftKnee + leftAnkle) * 0.5f;
        float legRadius = legLength * 0.12f;
        
        generateEllipsoid(legCenter, legRadius, legLength * 0.5f, legRadius,
                         segments, allVertices, allNormals, allIndices, indexOffset);
    }
    
    // 8. Right upper arm: from right shoulder to right elbow
    if (isValidKeypoint(rightShoulder) && isValidKeypoint(rightElbow)) {
        float armLength = distance3D(rightShoulder, rightElbow);
        cv::Point3f armCenter = (rightShoulder + rightElbow) * 0.5f;
        float armRadius = armLength * 0.12f;
        
        generateEllipsoid(armCenter, armRadius, armLength * 0.5f, armRadius,
                         segments, allVertices, allNormals, allIndices, indexOffset);
    }
    
    // 9. Left upper arm: from left shoulder to left elbow
    if (isValidKeypoint(leftShoulder) && isValidKeypoint(leftElbow)) {
        float armLength = distance3D(leftShoulder, leftElbow);
        cv::Point3f armCenter = (leftShoulder + leftElbow) * 0.5f;
        float armRadius = armLength * 0.12f;
        
        generateEllipsoid(armCenter, armRadius, armLength * 0.5f, armRadius,
                         segments, allVertices, allNormals, allIndices, indexOffset);
    }
    
    // 10. Right forearm: from right elbow to right wrist
    if (isValidKeypoint(rightElbow) && isValidKeypoint(rightWrist)) {
        float forearmLength = distance3D(rightElbow, rightWrist);
        cv::Point3f forearmCenter = (rightElbow + rightWrist) * 0.5f;
        float forearmRadius = forearmLength * 0.10f;
        
        generateEllipsoid(forearmCenter, forearmRadius, forearmLength * 0.5f, forearmRadius,
                         segments, allVertices, allNormals, allIndices, indexOffset);
    }
    
    // 11. Left forearm: from left elbow to left wrist
    if (isValidKeypoint(leftElbow) && isValidKeypoint(leftWrist)) {
        float forearmLength = distance3D(leftElbow, leftWrist);
        cv::Point3f forearmCenter = (leftElbow + leftWrist) * 0.5f;
        float forearmRadius = forearmLength * 0.10f;
        
        generateEllipsoid(forearmCenter, forearmRadius, forearmLength * 0.5f, forearmRadius,
                         segments, allVertices, allNormals, allIndices, indexOffset);
    }
    
    // Create GLB from vertices, normals, and indices
    if (allVertices.empty() || allIndices.empty()) {
        return std::vector<uint8_t>();
    }
    
    return createGLBManually(allVertices, allNormals, allIndices);
}

