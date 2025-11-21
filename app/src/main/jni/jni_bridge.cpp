#include <jni.h>
#include "image_preprocessor.h"
#include "pose_estimator.h"
#include "multi_view_3d.h"
#include "mesh_generator.h"
#include <opencv2/opencv.hpp>
#include <vector>
#include <cstring>

// Kept for future reference - will be re-enabled after MediaPipe integration for 3D reconstruction
/*
extern "C" JNIEXPORT jobject JNICALL
Java_com_example_bodyscanapp_utils_NativeBridge_processThreeImages(
        JNIEnv* env, jclass, jobjectArray jImages, jintArray jWidths,
        jintArray jHeights, jfloat userHeight) {

    // Find the ScanResult class (Kotlin data class)
    jclass resultClass = env->FindClass("com/example/bodyscanapp/utils/NativeBridge$ScanResult");
    if (resultClass == nullptr) {
        return nullptr;
    }
    
    // Get constructor for Kotlin data class: ScanResult(FloatArray, ByteArray, FloatArray)
    jmethodID constructor = env->GetMethodID(resultClass, "<init>", "([F[B[F)V");
    if (constructor == nullptr) {
        return nullptr;
    }

    // Initialize result arrays (will be populated or set to empty on error)
    jfloatArray keypoints3d = nullptr;
    jbyteArray meshGlb = nullptr;
    jfloatArray measurements = nullptr;

    try {
        // 1. Validate input
        if (jImages == nullptr || jWidths == nullptr || jHeights == nullptr) {
            // Return empty result
            keypoints3d = env->NewFloatArray(135 * 3);
            meshGlb = env->NewByteArray(0);
            measurements = env->NewFloatArray(0);
            jobject result = env->NewObject(resultClass, constructor, keypoints3d, meshGlb, measurements);
            return result;
        }

        jsize numImages = env->GetArrayLength(jImages);
        if (numImages != 3) {
            // Return empty result
            keypoints3d = env->NewFloatArray(135 * 3);
            meshGlb = env->NewByteArray(0);
            measurements = env->NewFloatArray(0);
            jobject result = env->NewObject(resultClass, constructor, keypoints3d, meshGlb, measurements);
            return result;
        }

        // Get image dimensions
        jint widths[3], heights[3];
        env->GetIntArrayRegion(jWidths, 0, 3, widths);
        env->GetIntArrayRegion(jHeights, 0, 3, heights);

        // 2. Convert Java byte[][] → std::vector<cv::Mat>
        std::vector<cv::Mat> imgs(3);
        for (int i = 0; i < 3; ++i) {
            jbyteArray jImg = (jbyteArray)env->GetObjectArrayElement(jImages, i);
            if (jImg == nullptr) {
                // Return empty result
                keypoints3d = env->NewFloatArray(135 * 3);
                meshGlb = env->NewByteArray(0);
                measurements = env->NewFloatArray(0);
                jobject result = env->NewObject(resultClass, constructor, keypoints3d, meshGlb, measurements);
                return result;
            }

            jsize len = env->GetArrayLength(jImg);
            jint expectedSize = widths[i] * heights[i] * 4; // RGBA = 4 bytes per pixel
            
            // Validate buffer size matches expected dimensions
            if (len < expectedSize) {
                // Return empty result - invalid image data
                keypoints3d = env->NewFloatArray(135 * 3);
                meshGlb = env->NewByteArray(0);
                measurements = env->NewFloatArray(0);
                jobject result = env->NewObject(resultClass, constructor, keypoints3d, meshGlb, measurements);
                env->DeleteLocalRef(jImg);
                return result;
            }
            
            jbyte* buf = env->GetByteArrayElements(jImg, nullptr);
            
            // Create RGBA Mat from buffer (rows=height, cols=width, type=CV_8UC4)
            // Note: OpenCV Mat constructor is Mat(rows, cols, type, data)
            cv::Mat rgba(heights[i], widths[i], CV_8UC4, buf);
            
            // Convert RGBA to RGB and clone to ensure contiguous memory
            cv::Mat rgb;
            cv::cvtColor(rgba, rgb, cv::COLOR_RGBA2RGB);
            
            // Clone to ensure the Mat owns its data and is contiguous
            // This is critical to ensure the full image is processed correctly
            imgs[i] = rgb.clone();
            
            env->ReleaseByteArrayElements(jImg, buf, JNI_ABORT);
            env->DeleteLocalRef(jImg);
        }

        // 3. Pre-process images (CLAHE + resizing)
        for (auto& img : imgs) {
            ImagePreprocessor::run(img);
        }

        // 4. Detect 2D keypoints per view
        std::vector<std::vector<cv::Point2f>> kpts2d(3);
        for (int i = 0; i < 3; ++i) {
            kpts2d[i] = PoseEstimator::detect(imgs[i]);
        }

        // 5. Triangulate to 3D keypoints - Kept for future reference (3D reconstruction)
        // std::vector<cv::Point3f> kpts3d = MultiView3D::triangulate(kpts2d, userHeight);

        // 6. Generate mesh - Kept for future reference (3D reconstruction)
        // std::vector<uint8_t> mesh = MeshGenerator::createFromKeypoints(kpts3d);

        // 7. Compute measurements - Kept for future reference (3D reconstruction)
        // std::vector<float> meas = computeCircumferences(kpts3d);
        
        // For single image, we'll use 2D keypoints only
        // Create empty 3D keypoints and mesh
        std::vector<cv::Point3f> kpts3d(135, cv::Point3f(0, 0, 0));
        std::vector<uint8_t> mesh;
        std::vector<float> meas(7, 0.0f); // Empty measurements for multi-view

        // 8. Pack results into Java arrays
        
        // Pack keypoints3d: 135 * 3 = 405 floats
        keypoints3d = env->NewFloatArray(135 * 3);
        if (keypoints3d != nullptr && kpts3d.size() == 135) {
            float* kptsArray = new float[135 * 3];
            for (size_t i = 0; i < 135; ++i) {
                kptsArray[i * 3 + 0] = kpts3d[i].x;
                kptsArray[i * 3 + 1] = kpts3d[i].y;
                kptsArray[i * 3 + 2] = kpts3d[i].z;
            }
            env->SetFloatArrayRegion(keypoints3d, 0, 135 * 3, kptsArray);
            delete[] kptsArray;
        } else {
            // Create zero-filled array if conversion failed
            keypoints3d = env->NewFloatArray(135 * 3);
            float zeros[135 * 3] = {0};
            env->SetFloatArrayRegion(keypoints3d, 0, 135 * 3, zeros);
        }

        // Pack meshGlb: GLB binary data
        meshGlb = env->NewByteArray(mesh.size());
        if (meshGlb != nullptr && !mesh.empty()) {
            env->SetByteArrayRegion(meshGlb, 0, mesh.size(), 
                                   reinterpret_cast<const jbyte*>(mesh.data()));
        } else {
            meshGlb = env->NewByteArray(0);
        }

        // Pack measurements: float array
        measurements = env->NewFloatArray(meas.size());
        if (measurements != nullptr && !meas.empty()) {
            env->SetFloatArrayRegion(measurements, 0, meas.size(), meas.data());
        } else {
            measurements = env->NewFloatArray(0);
        }

    } catch (...) {
        // Exception occurred - return empty result
        if (keypoints3d == nullptr) {
            keypoints3d = env->NewFloatArray(135 * 3);
            float zeros[135 * 3] = {0};
            env->SetFloatArrayRegion(keypoints3d, 0, 135 * 3, zeros);
        }
        if (meshGlb == nullptr) {
            meshGlb = env->NewByteArray(0);
        }
        if (measurements == nullptr) {
            measurements = env->NewFloatArray(0);
        }
    }

    // Create and return ScanResult object
    jobject result = env->NewObject(resultClass, constructor, keypoints3d, meshGlb, measurements);
    
    // Clean up local references
    if (keypoints3d != nullptr) env->DeleteLocalRef(keypoints3d);
    if (meshGlb != nullptr) env->DeleteLocalRef(meshGlb);
    if (measurements != nullptr) env->DeleteLocalRef(measurements);
    if (resultClass != nullptr) env->DeleteLocalRef(resultClass);
    
    return result;
}
*/

// Helper function to compute measurements from 2D keypoints
std::vector<float> computeMeasurementsFrom2D(const std::vector<cv::Point2f>& kpts2d, float userHeight) {
    std::vector<float> measurements(7, 0.0f); // 7 measurements
    
    if (kpts2d.empty() || kpts2d.size() < 10) {
        return measurements;
    }
    
    // Find bounding box
    float minY = std::numeric_limits<float>::max();
    float maxY = std::numeric_limits<float>::lowest();
    float minX = std::numeric_limits<float>::max();
    float maxX = std::numeric_limits<float>::lowest();
    
    for (const auto& pt : kpts2d) {
        if (pt.x >= 0.0f && pt.x <= 1.0f && pt.y >= 0.0f && pt.y <= 1.0f) {
            minY = std::min(minY, pt.y);
            maxY = std::max(maxY, pt.y);
            minX = std::min(minX, pt.x);
            maxX = std::max(maxX, pt.x);
        }
    }
    
    float height = maxY - minY;
    float width = maxX - minX;
    
    if (height <= 0.0f || width <= 0.0f) {
        return measurements;
    }
    
    // Estimate measurements using proportions (assuming front view)
    // These are rough estimates based on typical body proportions
    // Using image dimensions and user height to scale
    
    // Estimate pixel-to-cm ratio (assuming image shows full body)
    float pixelHeight = height; // normalized height
    float cmPerPixel = userHeight / pixelHeight; // approximate
    
    // Waist (around 50% of body height, width at that level)
    float waistWidth = width * 0.15f; // approximate waist width in normalized coords
    measurements[0] = waistWidth * cmPerPixel * 3.14159f * 2.0f; // approximate circumference
    
    // Chest (around 25% of body height)
    float chestWidth = width * 0.18f;
    measurements[1] = chestWidth * cmPerPixel * 3.14159f * 2.0f;
    
    // Hips (around 60% of body height)
    float hipWidth = width * 0.16f;
    measurements[2] = hipWidth * cmPerPixel * 3.14159f * 2.0f;
    
    // Thighs (around 70% of body height)
    float thighWidth = width * 0.12f;
    measurements[3] = thighWidth * cmPerPixel * 3.14159f * 2.0f; // left
    measurements[4] = thighWidth * cmPerPixel * 3.14159f * 2.0f; // right
    
    // Arms (around 30% of body height)
    float armWidth = width * 0.08f;
    measurements[5] = armWidth * cmPerPixel * 3.14159f * 2.0f; // left
    measurements[6] = armWidth * cmPerPixel * 3.14159f * 2.0f; // right
    
    return measurements;
}

// Single image processing function
extern "C" JNIEXPORT jobject JNICALL
Java_com_example_bodyscanapp_utils_NativeBridge_processOneImage(
        JNIEnv* env, jclass, jbyteArray jImage, jint width, jint height, jfloat userHeight) {

    // Find the ScanResult class (Kotlin data class)
    jclass resultClass = env->FindClass("com/example/bodyscanapp/utils/NativeBridge$ScanResult");
    if (resultClass == nullptr) {
        return nullptr;
    }
    
    // Get constructor for Kotlin data class
    // Try 4-parameter constructor first (with keypoints2d)
    jmethodID constructor = env->GetMethodID(resultClass, "<init>", "([F[B[F[F)V");
    if (constructor == nullptr) {
        // Fall back to 3-parameter constructor (without keypoints2d, will use default null)
        constructor = env->GetMethodID(resultClass, "<init>", "([F[B[F)V");
        if (constructor == nullptr) {
            return nullptr;
        }
    }

    // Initialize result arrays
    jfloatArray keypoints3d = nullptr;
    jbyteArray meshGlb = nullptr;
    jfloatArray measurements = nullptr;
    jfloatArray keypoints2d = nullptr;

    try {
        // 1. Validate input
        if (jImage == nullptr || width <= 0 || height <= 0) {
            // Return empty result
            keypoints3d = env->NewFloatArray(135 * 3);
            meshGlb = env->NewByteArray(0);
            measurements = env->NewFloatArray(7);
            keypoints2d = env->NewFloatArray(135 * 2);
            float zeros2d[135 * 2] = {0};
            env->SetFloatArrayRegion(keypoints2d, 0, 135 * 2, zeros2d);
            
            // Try 4-parameter constructor first, fall back to 3-parameter
            jobject result = nullptr;
            jmethodID newConstructor = env->GetMethodID(resultClass, "<init>", "([F[B[F[F)V");
            if (newConstructor != nullptr && keypoints2d != nullptr) {
                result = env->NewObject(resultClass, newConstructor, keypoints3d, meshGlb, measurements, keypoints2d);
            } else {
                result = env->NewObject(resultClass, constructor, keypoints3d, meshGlb, measurements);
            }
            return result;
        }

        jsize len = env->GetArrayLength(jImage);
        jint expectedSize = width * height * 4; // RGBA = 4 bytes per pixel
        
        // Validate buffer size
        if (len < expectedSize) {
            keypoints3d = env->NewFloatArray(135 * 3);
            meshGlb = env->NewByteArray(0);
            measurements = env->NewFloatArray(7);
            keypoints2d = env->NewFloatArray(135 * 2);
            float zeros2d[135 * 2] = {0};
            env->SetFloatArrayRegion(keypoints2d, 0, 135 * 2, zeros2d);
            
            jmethodID newConstructor = env->GetMethodID(resultClass, "<init>", "([F[B[F[F)V");
            if (newConstructor != nullptr && keypoints2d != nullptr) {
                return env->NewObject(resultClass, newConstructor, keypoints3d, meshGlb, measurements, keypoints2d);
            } else {
                return env->NewObject(resultClass, constructor, keypoints3d, meshGlb, measurements);
            }
        }
        
        jbyte* buf = env->GetByteArrayElements(jImage, nullptr);
        
        // Create RGBA Mat from buffer
        cv::Mat rgba(height, width, CV_8UC4, buf);
        
        // Convert RGBA to RGB and clone
        cv::Mat rgb;
        cv::cvtColor(rgba, rgb, cv::COLOR_RGBA2RGB);
        cv::Mat img = rgb.clone();
        
        env->ReleaseByteArrayElements(jImage, buf, JNI_ABORT);

        // 2. Pre-process image (CLAHE + resizing)
        ImagePreprocessor::run(img);

        // 3. Detect 2D keypoints
        // TODO: Replace with MediaPipe pose detection
        // MediaPipe will provide 33 landmarks, which will be interpolated to 135 keypoints
        std::vector<cv::Point2f> kpts2d(135, cv::Point2f(0.0f, 0.0f));
        // std::vector<cv::Point2f> kpts2d = PoseEstimator::detect(img); // Removed - will use MediaPipe

        // 4. Compute measurements from 2D keypoints
        std::vector<float> meas = computeMeasurementsFrom2D(kpts2d, userHeight);

        // 5. Pack results into Java arrays
        
        // Pack keypoints3d: 135 * 3 = 405 floats (empty for single image)
        keypoints3d = env->NewFloatArray(135 * 3);
        float zeros3d[135 * 3] = {0};
        env->SetFloatArrayRegion(keypoints3d, 0, 135 * 3, zeros3d);

        // Pack meshGlb: empty for single image
        meshGlb = env->NewByteArray(0);

        // Pack measurements: float array
        measurements = env->NewFloatArray(meas.size());
        if (measurements != nullptr && !meas.empty()) {
            env->SetFloatArrayRegion(measurements, 0, meas.size(), meas.data());
        } else {
            measurements = env->NewFloatArray(7);
            float zeros[7] = {0};
            env->SetFloatArrayRegion(measurements, 0, 7, zeros);
        }

        // Pack keypoints2d: 135 * 2 = 270 floats (normalized x, y coordinates)
        keypoints2d = env->NewFloatArray(135 * 2);
        if (keypoints2d != nullptr && kpts2d.size() == 135) {
            float* kpts2dArray = new float[135 * 2];
            for (size_t i = 0; i < 135; ++i) {
                kpts2dArray[i * 2 + 0] = kpts2d[i].x; // normalized x (0-1)
                kpts2dArray[i * 2 + 1] = kpts2d[i].y; // normalized y (0-1)
            }
            env->SetFloatArrayRegion(keypoints2d, 0, 135 * 2, kpts2dArray);
            delete[] kpts2dArray;
        } else {
            // Create zero-filled array if conversion failed
            float zeros2d[135 * 2] = {0};
            env->SetFloatArrayRegion(keypoints2d, 0, 135 * 2, zeros2d);
        }

    } catch (...) {
        // Exception occurred - return empty result
        if (keypoints3d == nullptr) {
            keypoints3d = env->NewFloatArray(135 * 3);
            float zeros[135 * 3] = {0};
            env->SetFloatArrayRegion(keypoints3d, 0, 135 * 3, zeros);
        }
        if (meshGlb == nullptr) {
            meshGlb = env->NewByteArray(0);
        }
        if (measurements == nullptr) {
            measurements = env->NewFloatArray(7);
            float zeros[7] = {0};
            env->SetFloatArrayRegion(measurements, 0, 7, zeros);
        }
        if (keypoints2d == nullptr) {
            keypoints2d = env->NewFloatArray(135 * 2);
            float zeros2d[135 * 2] = {0};
            env->SetFloatArrayRegion(keypoints2d, 0, 135 * 2, zeros2d);
        }
    }

    // Create and return ScanResult object
    jobject result = nullptr;
    jmethodID newConstructor = env->GetMethodID(resultClass, "<init>", "([F[B[F[F)V");
    if (newConstructor != nullptr && keypoints2d != nullptr) {
        result = env->NewObject(resultClass, newConstructor, keypoints3d, meshGlb, measurements, keypoints2d);
    } else {
        // Fall back to 3-parameter constructor (keypoints2d will be null/default)
        result = env->NewObject(resultClass, constructor, keypoints3d, meshGlb, measurements);
    }
    
    // Clean up local references
    if (keypoints3d != nullptr) env->DeleteLocalRef(keypoints3d);
    if (meshGlb != nullptr) env->DeleteLocalRef(meshGlb);
    if (measurements != nullptr) env->DeleteLocalRef(measurements);
    if (keypoints2d != nullptr) env->DeleteLocalRef(keypoints2d);
    if (resultClass != nullptr) env->DeleteLocalRef(resultClass);
    
    return result;
}

// TODO: Update to use MediaPipe for validation
extern "C" JNIEXPORT jobject JNICALL
Java_com_example_bodyscanapp_utils_NativeBridge_validateImage(
        JNIEnv* env, jclass, jbyteArray jImage, jint width, jint height) {
    
    // Find the ImageValidationResult class
    jclass resultClass = env->FindClass("com/example/bodyscanapp/utils/NativeBridge$ImageValidationResult");
    if (resultClass == nullptr) {
        return nullptr;
    }
    
    // Get constructor
    jmethodID constructor = env->GetMethodID(resultClass, "<init>", "(ZZFLjava/lang/String;)V");
    if (constructor == nullptr) {
        return nullptr;
    }
    
    // Initialize result
    bool hasPerson = false;
    bool isFullBody = false;
    float confidence = 0.0f;
    std::string message = "";
    
    try {
        if (jImage == nullptr || width <= 0 || height <= 0) {
            message = "Invalid input";
        } else {
            jsize len = env->GetArrayLength(jImage);
            jint expectedSize = width * height * 4; // RGBA
            
            if (len >= expectedSize * 0.9) {  // Allow some tolerance
                jbyte* buf = env->GetByteArrayElements(jImage, nullptr);
                
                // Create RGBA Mat
                cv::Mat rgba(height, width, CV_8UC4, buf);
                
                // Convert RGBA to RGB
                cv::Mat rgb;
                cv::cvtColor(rgba, rgb, cv::COLOR_RGBA2RGB);
                cv::Mat img = rgb.clone();
                
                env->ReleaseByteArrayElements(jImage, buf, JNI_ABORT);
                
                // Validate image
                PoseEstimator::ValidationResult result = PoseEstimator::validateImage(img);
                hasPerson = result.hasPerson;
                isFullBody = result.isFullBody;
                confidence = result.confidence;
                message = result.message;
            } else {
                message = "Image size mismatch";
            }
        }
    } catch (...) {
        message = "Processing error";
    }
    
    // Create Java string
    jstring jMessage = env->NewStringUTF(message.c_str());
    
    // Create and return result object
    jobject result = env->NewObject(resultClass, constructor, 
                                     hasPerson, isFullBody, confidence, jMessage);
    
    // Clean up
    if (jMessage != nullptr) env->DeleteLocalRef(jMessage);
    if (resultClass != nullptr) env->DeleteLocalRef(resultClass);
    
    return result;
}

