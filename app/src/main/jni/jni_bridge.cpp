#include <jni.h>
#include "image_preprocessor.h"
#include "pose_estimator.h"
#include "multi_view_3d.h"
#include "mesh_generator.h"
#include <opencv2/opencv.hpp>
#include <vector>
#include <cstring>

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

        // 5. Triangulate to 3D keypoints
        std::vector<cv::Point3f> kpts3d = MultiView3D::triangulate(kpts2d, userHeight);

        // 6. Generate mesh
        std::vector<uint8_t> mesh = MeshGenerator::createFromKeypoints(kpts3d);

        // 7. Compute measurements
        std::vector<float> meas = computeCircumferences(kpts3d);

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

