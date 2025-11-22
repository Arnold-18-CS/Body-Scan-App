# Prompt for Updating Measurement Calibration System

## Overview

Update the measurement calibration in the Body Scan App to compute **8 linear measurements** instead of the current **7 circumference-based measurements**.

---

## Current Implementation Context

The measurement calculation is in `app/src/main/jni/jni_bridge.cpp` in the `computeMeasurementsFrom2D()` function (lines 244-587). Currently it returns 7 circumference measurements. Replace this with 8 linear measurements.

### Key Context:

- **Keypoints are in normalized coordinates** (0.0-1.0)
- **MediaPipe provides 33 landmarks** (indices 0-32) mapped to 135 keypoints (indices 0-134)
- **MediaPipe landmark mapping:**
  - **11**: left shoulder, **12**: right shoulder
  - **13**: left elbow, **14**: right elbow
  - **15**: left wrist, **16**: right wrist
  - **23**: left hip, **24**: right hip
  - **25**: left knee, **26**: right knee
  - **27**: left ankle, **28**: right ankle
  - **2**: left eye, **5**: right eye
- The system uses `cmPerPixel` for scaling (calculated from user height)
- Use `isValidKeypoint()` to validate keypoints before use
- Use `keypointDistance()` to calculate distances between keypoints (returns normalized distance 0-1)

### Important Notes:

- **Landmarks 35 and 134**: These are NOT guaranteed to exist in the 135-keypoint mapping. Always calculate midpoints manually from the base MediaPipe landmarks (23-24 for hips, 27-28 for ankles).
- **Distance calculations**: `keypointDistance()` returns normalized distance. For conversion to centimeters:
  - Horizontal distances: `distance * imgWidth * cmPerPixel`
  - Vertical distances: `distance * imgHeight * cmPerPixel`
  - Diagonal distances: Use `keypointDistance()` then multiply by `std::max(imgWidth, imgHeight) * cmPerPixel` as approximation, or use `sqrt((dx*imgWidth)^2 + (dy*imgHeight)^2) * cmPerPixel` for more accuracy

---

## Required Measurements

Update `computeMeasurementsFrom2D()` to return **8 linear measurements** (in centimeters) in this order:

### 1. **Shoulder Width** (measurement[0])

- **Distance between landmarks 11 and 12** (left and right shoulders)
- **Formula**: `keypointDistance(kpts2d[11], kpts2d[12]) * imgWidth * cmPerPixel`
- **Validation**: Validate both keypoints exist using `isValidKeypoint()` before calculating
- **Range**: 30-60 cm (use `validateMeasurement()`)

### 2. **Arm Length** (measurement[1])

- **Calculate for each arm:**
  - **Left arm**: Sum of distances from shoulder (11) → elbow (13) → wrist (15)
    - `leftArmLength = keypointDistance(kpts2d[11], kpts2d[13]) + keypointDistance(kpts2d[13], kpts2d[15])`
  - **Right arm**: Sum of distances from shoulder (12) → elbow (14) → wrist (16)
    - `rightArmLength = keypointDistance(kpts2d[12], kpts2d[14]) + keypointDistance(kpts2d[14], kpts2d[16])`
- **Average the two arms**: `(leftArmLength + rightArmLength) / 2.0f`
- **Convert to centimeters**: 
  - For each segment distance (normalized), multiply by `std::max(imgWidth, imgHeight) * cmPerPixel` to account for diagonal distances
  - Or use: `distance * sqrt(imgWidth^2 + imgHeight^2) * cmPerPixel` for more accuracy
- **Validation**: Validate all required keypoints (11, 13, 15, 12, 14, 16) exist
- **Range**: 50-80 cm

### 3. **Leg Length** (measurement[2])

- **Calculate for each leg:**
  - **Left leg**: Sum of distances from hip (23) → knee (25) → ankle (27)
    - `leftLegLength = keypointDistance(kpts2d[23], kpts2d[25]) + keypointDistance(kpts2d[25], kpts2d[27])`
  - **Right leg**: Sum of distances from hip (24) → knee (26) → ankle (28)
    - `rightLegLength = keypointDistance(kpts2d[24], kpts2d[26]) + keypointDistance(kpts2d[26], kpts2d[28])`
- **Average the two legs**: `(leftLegLength + rightLegLength) / 2.0f`
- **Convert to centimeters**: Same approach as arm length (use `std::max(imgWidth, imgHeight)` or `sqrt(imgWidth^2 + imgHeight^2)`)
- **Validation**: Validate all required keypoints (23, 25, 27, 24, 26, 28) exist
- **Range**: 70-120 cm

### 4. **Hip Width** (measurement[3])

- **Distance between landmarks 23 and 24** (left and right hips)
- **Formula**: `keypointDistance(kpts2d[23], kpts2d[24]) * imgWidth * cmPerPixel`
- **Validation**: Validate both keypoints exist
- **Range**: 25-50 cm

### 5. **Upper Body Length** (measurement[4])

- **Distance from hip midpoint to the highest keypoint** (top of head)
- **Calculate hip midpoint**: `(kpts2d[23] + kpts2d[24]) / 2.0f`
  - **Note**: Do NOT assume landmark 35 exists. Always calculate the midpoint manually.
- **Find highest keypoint**: Iterate through keypoints 0-32 and find **minimum Y value** (top of image = smaller Y)
  - Start with `float highestY = 1.0f` (max normalized value)
  - Loop through keypoints 0-32, find minimum Y: `highestY = std::min(highestY, kpts2d[i].y)`
  - Use the keypoint with minimum Y as the highest point
- **Formula**: `(hipMidpoint.y - highestKeypoint.y) * imgHeight * cmPerPixel`
  - **Note**: Since Y increases downward in image coordinates, subtract to get positive length
- **Validation**: Validate hip keypoints (23, 24) and at least one head keypoint exist
- **Range**: 40-80 cm

### 6. **Lower Body Length** (measurement[5])

- **Distance from hip midpoint to ankle midpoint**
- **Calculate hip midpoint**: `(kpts2d[23] + kpts2d[24]) / 2.0f`
  - **Note**: Do NOT assume landmark 35 exists. Always calculate manually.
- **Calculate ankle midpoint**: `(kpts2d[27] + kpts2d[28]) / 2.0f` (left and right ankles)
  - **Note**: Do NOT assume landmark 134 exists. Always calculate manually.
- **Formula**: `keypointDistance(hipMidpoint, ankleMidpoint) * imgHeight * cmPerPixel`
  - Use `imgHeight` since this is primarily a vertical measurement
- **Validation**: Validate hip keypoints (23, 24) and ankle keypoints (27, 28) exist
- **Range**: 60-100 cm

### 7. **Neck Width** (measurement[6])

- **Distance from left eye to right eye**
- **Use landmarks**: left eye (2) and right eye (5) from MediaPipe
- **Formula**: `keypointDistance(kpts2d[2], kpts2d[5]) * imgWidth * cmPerPixel`
- **Validation**: Validate both eye keypoints exist
- **Range**: 8-15 cm

### 8. **Thigh Width** (measurement[7])

- **Calculate pixel distance from midpoint of hip-to-knee to the edges of each leg, then average**
- **For each leg:**
  - Calculate midpoint between hip and knee (Y coordinate)
    - Left: `midpointY = (kpts2d[23].y + kpts2d[25].y) / 2.0f`
    - Right: `midpointY = (kpts2d[24].y + kpts2d[26].y) / 2.0f`
  - Use segmentation mask (if available) to find leftmost and rightmost edges at that Y level
  - Calculate width in pixels, convert to centimeters
- **Average the left and right thigh widths**: `(leftThighWidth + rightThighWidth) / 2.0f`
- **If segmentation mask not available**: Estimate using keypoint-based width estimation
  - Reference existing thigh measurement code (lines 374-530) for pixel-level edge detection approach
  - Fallback: Use body proportions (similar to current implementation's fallback logic)
- **Validation**: Validate hip and knee keypoints exist for both legs
- **Range**: 15-40 cm

---

## Implementation Requirements

### 1. Update Return Type
- Change `std::vector<float> measurements(7, 0.0f)` to `std::vector<float> measurements(8, 0.0f)`

### 2. Remove Circumference Calculations
- **Remove all circumference/ellipse approximation code**
- Remove constants: `CHEST_CIRCUMFERENCE_FACTOR`, `WAIST_CIRCUMFERENCE_FACTOR`, `HIP_CIRCUMFERENCE_FACTOR`, `THIGH_CIRCUMFERENCE_FACTOR`, `ARM_CIRCUMFERENCE_FACTOR`
- Remove all depth estimation code (e.g., `chestDepthEstimate`, `waistDepthEstimate`, etc.)
- Remove all ellipse circumference formulas: `3.14159f * std::sqrt(2.0f * (a * a + b * b))`

### 3. Use Linear Distance Calculations
- **All measurements should be straight-line distances**, not circumferences
- Use `keypointDistance()` for normalized distance calculations
- Convert normalized distances to centimeters using appropriate image dimension and `cmPerPixel`

### 4. Handle Missing Keypoints
- For each measurement, validate required keypoints exist using `isValidKeypoint()` before calculating
- Return `0.0f` if keypoints are missing (measurement will be invalid)
- Use `validateMeasurement()` helper with reasonable min/max ranges for each measurement type

### 5. Coordinate Conversion
- Remember keypoints are normalized (0-1), so:
  - **For horizontal distances**: multiply by `imgWidth` then by `cmPerPixel`
  - **For vertical distances**: multiply by `imgHeight` then by `cmPerPixel`
  - **For diagonal distances**: 
    - Option 1 (simpler): Use `std::max(imgWidth, imgHeight) * cmPerPixel`
    - Option 2 (more accurate): Use `sqrt((dx*imgWidth)^2 + (dy*imgHeight)^2) * cmPerPixel`
    - Where `dx = p2.x - p1.x`, `dy = p2.y - p1.y` (normalized)

### 6. Validation
- Use `validateMeasurement()` helper with reasonable min/max ranges for each measurement type:
  - Shoulder Width: 30-60 cm
  - Arm Length: 50-80 cm
  - Leg Length: 70-120 cm
  - Hip Width: 25-50 cm
  - Upper Body Length: 40-80 cm
  - Lower Body Length: 60-100 cm
  - Neck Width: 8-15 cm
  - Thigh Width: 15-40 cm

### 7. Update Comments
- Update the measurement array indices comment (lines 236-243) to reflect the new 8 measurements:
```cpp
// Measurement array indices:
// [0] Shoulder width
// [1] Arm length (average of left and right)
// [2] Leg length (average of left and right)
// [3] Hip width
// [4] Upper body length (hip midpoint to highest point)
// [5] Lower body length (hip midpoint to ankle midpoint)
// [6] Neck width (eye to eye)
// [7] Thigh width (average of left and right)
```

### 8. Thigh Width Implementation
- Reuse the existing pixel-level edge detection logic (lines 374-530) for thigh width measurement
- The current implementation already has segmentation mask support and fallback logic
- Adapt it to calculate width (not circumference) and average left/right thighs

---

## Files to Modify

- **`app/src/main/jni/jni_bridge.cpp`** - Update `computeMeasurementsFrom2D()` function (lines 244-587)

---

## Testing Considerations

- **Test with images where all keypoints are detected** - Verify all 8 measurements are calculated
- **Test with partial keypoint detection** (some missing) - Verify measurements return 0.0f when keypoints are missing
- **Verify measurements are in reasonable ranges** (e.g., shoulder width 30-60cm, arm length 50-80cm)
- **Ensure measurements scale correctly** with different user heights
- **Test thigh width** with and without segmentation mask available
- **Verify midpoint calculations** work correctly when landmarks 35/134 don't exist

---

## Implementation Checklist

- [ ] Change measurements array size from 7 to 8
- [ ] Remove all circumference calculation code
- [ ] Remove all circumference factor constants
- [ ] Implement Shoulder Width (measurement[0])
- [ ] Implement Arm Length (measurement[1])
- [ ] Implement Leg Length (measurement[2])
- [ ] Implement Hip Width (measurement[3])
- [ ] Implement Upper Body Length (measurement[4])
- [ ] Implement Lower Body Length (measurement[5])
- [ ] Implement Neck Width (measurement[6])
- [ ] Implement Thigh Width (measurement[7])
- [ ] Update measurement array indices comment
- [ ] Add validation ranges for all measurements
- [ ] Test with various images and keypoint configurations

---

This prompt provides the context and requirements to update the measurement system. Share it with the agent implementing the changes.

