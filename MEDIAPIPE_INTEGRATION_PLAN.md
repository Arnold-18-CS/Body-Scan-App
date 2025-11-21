# MediaPipe Integration Plan

This document outlines the phased approach to integrate MediaPipe Pose Detection into the Body Scan App, replacing the current stub implementation.

## Overview

The codebase has been cleaned up and prepared for MediaPipe integration. All custom OpenCV pose estimation code has been removed, and the infrastructure is ready for the new implementation.

---

## Phase 1: MediaPipe SDK Setup and Configuration

### 1.1 Add MediaPipe Dependencies
- [ ] Add MediaPipe Android SDK dependency to `app/build.gradle`
  - Add `com.google.mediapipe:solution_base` dependency
  - Add `com.google.mediapipe:pose` dependency
  - Configure AAR dependencies for native libraries

### 1.2 Update Native Build Configuration
- [ ] Review `app/src/main/jni/CMakeLists.txt` for MediaPipe compatibility
- [ ] Ensure OpenCV remains available for image preprocessing (CLAHE, resizing)
- [ ] Verify native library linking order

### 1.3 Add Required Permissions (if needed)
- [ ] Review AndroidManifest.xml for any additional permissions
- [ ] Ensure camera permissions are properly configured

### 1.4 Test Build After Setup
- [ ] Verify Gradle sync completes successfully
- [ ] Confirm native build compiles without errors
- [ ] Test app launches on device/emulator

**Deliverable:** MediaPipe SDK integrated and building successfully

---

## Phase 2: MediaPipe Pose Detection Implementation

### 2.1 Create MediaPipe Pose Detector Wrapper
- [ ] Create `MediaPipePoseDetector` class in C++ (`app/src/main/cpp/src/mediapipe_pose_detector.cpp`)
  - Initialize MediaPipe pose solution
  - Handle input image conversion (OpenCV Mat → MediaPipe format)
  - Process images through MediaPipe
  - Extract 33 MediaPipe landmarks
  - Convert MediaPipe landmarks to 135 keypoint format (interpolation/mapping)

### 2.2 Update pose_estimator.cpp
- [ ] Replace stub `detect()` function with MediaPipe implementation
  - Call `MediaPipePoseDetector` to get 33 landmarks
  - Map 33 MediaPipe landmarks to 135 keypoint format
  - Return normalized coordinates (0-1 range)
  - Handle edge cases (no person detected, partial detection)

### 2.3 Update pose_estimator.h
- [ ] Add MediaPipe-related includes and forward declarations
- [ ] Update function documentation

### 2.4 Test Pose Detection
- [ ] Test with sample images (front, side profiles)
- [ ] Verify keypoint coordinates are normalized correctly
- [ ] Validate keypoint count (135 keypoints)
- [ ] Test error handling (empty images, no person detected)

**Deliverable:** MediaPipe pose detection working and returning 135 keypoints

---

## Phase 3: Image Validation with MediaPipe

### 3.1 Update validateImage() Function
- [ ] Modify `PoseEstimator::validateImage()` in `pose_estimator.cpp`
  - Use MediaPipe to detect person presence
  - Check if full body is visible (all major landmarks detected)
  - Calculate confidence score from MediaPipe detection
  - Generate appropriate validation messages

### 3.2 Enhance Validation Logic
- [ ] Define which landmarks are required for "full body" detection
  - Head landmarks (nose, eyes, ears)
  - Upper body (shoulders, elbows, wrists)
  - Lower body (hips, knees, ankles)
- [ ] Set confidence thresholds for validation
- [ ] Improve error messages for different failure scenarios

### 3.3 Test Validation
- [ ] Test with images containing no person
- [ ] Test with partial body images (head cropped, legs cropped)
- [ ] Test with full body images
- [ ] Verify validation badges display correctly in `CapturedImagePreviewScreen`

**Deliverable:** Image validation using MediaPipe working correctly

---

## Phase 4: Integration with Processing Pipeline

### 4.1 Update processOneImage() JNI Function
- [ ] Remove TODO comment in `jni_bridge.cpp`
- [ ] Uncomment/update `PoseEstimator::detect()` call
- [ ] Verify keypoint data flows correctly from C++ to Kotlin
- [ ] Test measurement calculation with real MediaPipe keypoints

### 4.2 Update ProcessingScreen.kt
- [ ] Remove TODO comments related to MediaPipe
- [ ] Verify `ScanResult` is populated correctly with MediaPipe keypoints
- [ ] Test processing flow end-to-end

### 4.3 Update Result3DScreen.kt
- [ ] Verify keypoint overlay displays MediaPipe-detected keypoints
- [ ] Test keypoint visualization with real detection results
- [ ] Ensure measurements are calculated from actual keypoints

### 4.4 Test End-to-End Flow
- [ ] Capture image → Preview → Process → Results
- [ ] Verify keypoints are visible and accurate
- [ ] Verify measurements are calculated correctly
- [ ] Test with all three capture phases (front, left, right)

**Deliverable:** Complete processing pipeline using MediaPipe keypoints

---

## Phase 5: Measurement Calculation Refinement

### 5.1 Review Measurement Algorithms
- [ ] Analyze `computeMeasurementsFrom2D()` in `jni_bridge.cpp`
- [ ] Update measurement calculation to use MediaPipe keypoint positions
  - Waist: Use hip landmarks and body width
  - Chest: Use shoulder and chest landmarks
  - Hips: Use hip landmarks
  - Thighs: Use hip and knee landmarks
  - Arms: Use shoulder, elbow, and wrist landmarks

### 5.2 Calibrate Measurements
- [ ] Test measurements against known reference values
- [ ] Adjust scaling factors based on user height input
- [ ] Account for different camera angles (front vs. side profiles)
- [ ] Implement measurement averaging for multiple views (if applicable)

### 5.3 Add Measurement Validation
- [ ] Add sanity checks for measurement values (reasonable ranges)
- [ ] Handle edge cases (keypoints not detected, partial detection)
- [ ] Provide fallback values or error messages

**Deliverable:** Accurate body measurements calculated from MediaPipe keypoints

---

## Phase 6: Multi-Image Processing (Future Enhancement)

### 6.1 Re-enable processThreeImages()
- [ ] Uncomment `processThreeImages()` in `jni_bridge.cpp`
- [ ] Update to use MediaPipe for each image
- [ ] Test 3-image processing flow

### 6.2 3D Reconstruction Integration
- [ ] Integrate MediaPipe keypoints with `MultiView3D::triangulate()`
- [ ] Test 3D keypoint reconstruction from multiple views
- [ ] Verify 3D mesh generation with `MeshGenerator`

### 6.3 Multi-View Measurement Calculation
- [ ] Update measurement calculation to use 3D keypoints
- [ ] Improve accuracy using multiple camera angles
- [ ] Test with front, left, and right profile images

**Deliverable:** 3D reconstruction working with MediaPipe keypoints (optional)

---

## Phase 7: Performance Optimization and Testing

### 7.1 Performance Profiling
- [ ] Measure MediaPipe inference time on target devices
- [ ] Profile memory usage during processing
- [ ] Identify bottlenecks in the processing pipeline
- [ ] Optimize image preprocessing if needed

### 7.2 Error Handling and Edge Cases
- [ ] Test with various image sizes and aspect ratios
- [ ] Test with different lighting conditions
- [ ] Test with multiple people in frame
- [ ] Test with occluded body parts
- [ ] Add appropriate error messages and fallbacks

### 7.3 User Experience Improvements
- [ ] Add loading indicators during MediaPipe processing
- [ ] Improve validation feedback messages
- [ ] Add retry mechanisms for failed detections
- [ ] Optimize UI responsiveness during processing

### 7.4 Testing
- [ ] Unit tests for MediaPipe wrapper functions
- [ ] Integration tests for processing pipeline
- [ ] UI tests for capture and processing flow
- [ ] Test on multiple devices (different screen sizes, Android versions)

**Deliverable:** Optimized, tested, and production-ready MediaPipe integration

---

## Phase 8: Documentation and Cleanup

### 8.1 Code Documentation
- [ ] Add KDoc comments to Kotlin functions
- [ ] Add Doxygen comments to C++ functions
- [ ] Document MediaPipe keypoint mapping (33 → 135)
- [ ] Document measurement calculation algorithms

### 8.2 Remove TODO Comments
- [ ] Remove all MediaPipe-related TODO comments
- [ ] Update comments to reflect actual implementation
- [ ] Clean up any remaining placeholder code

### 8.3 Update README
- [ ] Document MediaPipe integration
- [ ] Update build instructions if needed
- [ ] Add troubleshooting section for common issues

**Deliverable:** Fully documented and production-ready codebase

---

## Key Technical Considerations

### MediaPipe Landmark Mapping (33 → 135)
MediaPipe provides 33 landmarks. The app expects 135 keypoints. Consider:
- Direct mapping for corresponding landmarks
- Interpolation for additional keypoints (e.g., between joints)
- Skeleton structure mapping (BODY_25 format compatibility)

### Coordinate System
- MediaPipe returns normalized coordinates (0-1)
- Ensure consistency with existing coordinate system
- Verify image dimensions are handled correctly

### Performance Targets
- Inference time: < 500ms per image on mid-range devices
- Memory usage: < 200MB during processing
- Battery impact: Minimize processing overhead

### Error Handling Strategy
- Graceful degradation when MediaPipe fails
- Clear error messages for users
- Retry mechanisms for transient failures
- Fallback to basic validation if pose detection fails

---

## Dependencies and Prerequisites

- Android SDK 24+ (Android 7.0+)
- MediaPipe Android SDK (latest stable version)
- OpenCV (for image preprocessing - already integrated)
- NDK (for native code compilation - already configured)

---

## Estimated Timeline

- **Phase 1:** 1-2 days (SDK setup and configuration)
- **Phase 2:** 3-5 days (Core pose detection implementation)
- **Phase 3:** 2-3 days (Image validation)
- **Phase 4:** 2-3 days (Pipeline integration)
- **Phase 5:** 3-4 days (Measurement refinement)
- **Phase 6:** 5-7 days (Multi-image processing - optional)
- **Phase 7:** 3-5 days (Optimization and testing)
- **Phase 8:** 1-2 days (Documentation)

**Total Estimated Time:** 20-31 days (excluding Phase 6: 15-24 days)

---

## Notes

- All phases should be tested incrementally
- Keep the 3D reconstruction infrastructure intact (as per cleanup requirements)
- Maintain backward compatibility with existing UI components
- Consider creating feature flags for gradual rollout

