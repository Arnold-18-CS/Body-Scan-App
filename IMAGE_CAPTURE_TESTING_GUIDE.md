# Image Capture Screen Testing Guide

## Overview
This guide provides instructions for testing the newly implemented Image Capture Screen with height input, CameraX preview, and image capture functionality.

## Features Implemented

### 1. Height Input
- **TextField**: Manual entry of height value
- **Slider**: Visual height selection (100-250 cm range)
- **Validation**: Automatic validation with error messages
  - Rejects values < 100 cm
  - Rejects values > 250 cm
  - Shows inline error messages for invalid input
- **Synchronization**: TextField and Slider values are synchronized in real-time

### 2. CameraX Integration
- **Camera Preview**: Live camera feed display
- **Permission Handling**: Automatic camera permission request
- **Lifecycle Management**: Proper camera lifecycle handling

### 3. Framing Guide Overlay
- **Body Frame**: Dashed rectangle guide for body alignment
- **Head Guide**: Circle guide for head positioning
- **Alignment Lines**: Horizontal lines for better positioning
- **Semi-transparent Design**: White dashed lines for clear visibility

### 4. Real-time Feedback
- **Feedback Text**: Displays alignment instructions
- **Dynamic Updates**: Changes based on user actions
- **Capture Confirmation**: Success/failure messages

### 5. Capture Functionality
- **Capture Button**: Blue button to capture image
- **Enabled State**: Only active when height is valid and camera permission granted
- **Image Processing**: Captured image passed to placeholder processing function
- **Byte Array Output**: Image converted to byte array for processing

## Testing Instructions

### Prerequisites
- Physical Android device (recommended) or emulator with camera support
- Camera permission granted
- minSdk: 24 (Android 7.0)
- targetSdk: 36

### Testing Steps

#### 1. Navigation Test
1. Launch the app
2. Complete authentication (login/registration + 2FA)
3. From Home Screen, tap "New Scan" button
4. Verify navigation to Image Capture Screen

#### 2. Height Input Validation Tests

**Test Case 1: Valid Height Entry**
- Enter height: 175 cm
- Expected: No error message, green outline
- Slider should update to 175

**Test Case 2: Below Minimum**
- Enter height: 95 cm
- Expected: Error message "Height must be at least 100 cm"
- Capture button disabled

**Test Case 3: Above Maximum**
- Enter height: 255 cm
- Expected: Error message "Height must not exceed 250 cm"
- Capture button disabled

**Test Case 4: Non-numeric Input**
- Enter text: "abc"
- Expected: Error message "Please enter a valid number"
- Capture button disabled

**Test Case 5: Slider Synchronization**
- Move slider to various positions
- Expected: TextField updates to match slider value
- All values within 100-250 cm range

#### 3. Camera Permission Tests

**Test Case 1: Permission Granted**
- Grant camera permission when prompted
- Expected: Camera preview displays
- Framing guide overlay visible

**Test Case 2: Permission Denied**
- Deny camera permission
- Expected: Message "Camera permission is required"
- Capture button disabled

**Test Case 3: Permission Re-request**
- If previously denied, go to app settings
- Grant camera permission
- Return to app
- Expected: Camera preview activates

#### 4. Camera Preview Tests

**Test Case 1: Preview Display**
- Verify camera preview shows live feed
- Expected: Real-time camera view
- No lag or stuttering

**Test Case 2: Framing Guide**
- Observe overlay on camera preview
- Expected: White dashed rectangle for body
- White dashed circle for head
- Horizontal alignment lines

**Test Case 3: Orientation**
- Rotate device (if supported)
- Expected: Preview maintains proper orientation
- Guide overlay adjusts accordingly

#### 5. Image Capture Tests

**Test Case 1: Successful Capture**
- Enter valid height (e.g., 170 cm)
- Position body within framing guide
- Tap "Capture Image" button
- Expected: 
  - Success message displayed
  - Logcat shows: "Processing image data of size: X bytes"
  - onCaptureComplete callback invoked

**Test Case 2: Capture with Invalid Height**
- Enter invalid height (e.g., 50 cm)
- Tap capture button
- Expected: Button is disabled (grayed out)
- Feedback text: "Please enter a valid height (100-250 cm)"

**Test Case 3: Multiple Captures**
- Capture multiple images in succession
- Expected: Each capture processes independently
- No memory leaks or crashes

#### 6. UI/UX Tests

**Test Case 1: Feedback Text**
- Observe feedback text changes
- Initial: "Align body within the frame"
- After capture: "Image captured successfully!"
- After error: "Capture failed: [error message]"

**Test Case 2: Button States**
- Valid height + camera permission: Blue button, enabled
- Invalid height: Gray button, disabled
- No camera permission: Gray button, disabled

**Test Case 3: Dark Theme**
- Screen uses dark background (black)
- White text for contrast
- Blue accent color for primary actions

#### 7. Edge Cases

**Test Case 1: Empty Height Field**
- Leave height field empty
- Tap outside field
- Expected: Error message displayed
- Capture button disabled

**Test Case 2: Decimal Input**
- Enter: 175.5
- Expected: May show error (depends on validation logic)
- Should accept only integers

**Test Case 3: Leading Zeros**
- Enter: 0175
- Expected: Accepted as 175 cm
- TextField and slider synchronized

**Test Case 4: Very Long Input**
- Enter: 123456789
- Expected: Error message "Height must not exceed 250 cm"

## Logcat Monitoring

Monitor logcat for the following tags:
```bash
adb logcat -s ImageCaptureScreen:D CameraPreview:E
```

Expected log output:
```
D/ImageCaptureScreen: Processing image data of size: [X] bytes
```

Error scenarios:
```
E/CameraPreview: Use case binding failed
E/ImageCaptureScreen: Capture error
```

## Known Issues / Limitations

1. **Emulator Camera**: Some emulators may not support camera preview properly. Physical device testing recommended.

2. **Image Format**: Currently captures image in raw format from camera plane[0]. May need adjustment for specific processing requirements.

3. **Image Quality**: Using CAPTURE_MODE_MINIMIZE_LATENCY for faster captures. Can be changed to CAPTURE_MODE_MAXIMIZE_QUALITY if needed.

4. **Placeholder Processing**: The `processImageData()` function currently only logs the byte array size. Needs to be implemented with actual processing logic.

5. **Back Navigation**: Currently no back button on the screen. Can be added to return to Home Screen.

## Implementation Details

### Files Modified/Created
1. **ImageCaptureScreen.kt** - Main composable with all UI and camera logic
2. **MainActivity.kt** - Added IMAGE_CAPTURE screen to navigation
3. **AndroidManifest.xml** - Added camera permission
4. **build.gradle.kts** - Added CameraX dependencies
5. **libs.versions.toml** - Added CameraX version definitions

### Key Functions
- `ImageCaptureScreen()` - Main composable
- `CameraPreview()` - CameraX preview setup
- `FramingGuideOverlay()` - Canvas-based guide drawing
- `captureImage()` - Image capture logic
- `processImageData()` - Placeholder processing function

### Dependencies Added
```kotlin
androidx.camera:camera-core:1.4.1
androidx.camera:camera-camera2:1.4.1
androidx.camera:camera-lifecycle:1.4.1
androidx.camera:camera-view:1.4.1
```

## Next Steps

1. **Implement actual image processing logic** in `processImageData()` function
2. **Add back button** for navigation to Home Screen
3. **Implement image storage** if needed for later retrieval
4. **Add loading indicator** during image capture
5. **Enhance framing guide** with dynamic feedback based on body detection
6. **Add image preview** after capture before processing
7. **Implement retry mechanism** for failed captures
8. **Add image quality selector** (low/medium/high)
9. **Implement flash control** for low-light scenarios
10. **Add front/back camera switcher** if needed

## Troubleshooting

### Issue: Camera preview not showing
**Solution**: 
- Check camera permission is granted
- Verify device/emulator has camera support
- Check logcat for binding errors

### Issue: Capture button always disabled
**Solution**:
- Ensure height is between 100-250 cm
- Verify camera permission granted
- Check `isHeightValid` state variable

### Issue: App crashes on capture
**Solution**:
- Check for null imageCapture object
- Verify camera lifecycle is properly initialized
- Review logcat for exception details

### Issue: Framing guide not visible
**Solution**:
- Check canvas drawing is on top of preview
- Verify white color is visible against preview
- Adjust alpha values if needed

## Testing Checklist

- [ ] App builds successfully
- [ ] Navigation from Home to Image Capture works
- [ ] Height input field accepts valid values
- [ ] Height validation rejects invalid values
- [ ] Slider synchronizes with text field
- [ ] Camera permission request appears
- [ ] Camera preview displays after permission granted
- [ ] Framing guide overlay is visible
- [ ] Capture button enables/disables correctly
- [ ] Image capture completes successfully
- [ ] Logcat shows processing message
- [ ] Success message displays after capture
- [ ] Multiple captures work without issues
- [ ] No memory leaks or crashes
- [ ] UI is responsive and smooth

---

**Last Updated**: October 6, 2025
**Version**: 1.0
**Status**: Ready for Testing

