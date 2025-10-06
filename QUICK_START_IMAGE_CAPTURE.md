# Quick Start Guide - Image Capture Screen

## 🚀 Quick Test Steps

### 1. Build & Run (Android Studio)
```bash
# Open in Android Studio
# Click "Run" (Shift + F10) or Green Play button
# Select your device/emulator
```

### 2. Navigate to Image Capture Screen
1. **Login** to the app (or register if first time)
2. **Complete 2FA** authentication
3. On **Home Screen**, tap **"New Scan"** button (Blue)
4. **Image Capture Screen** should open

### 3. Test Height Input
```
Valid Test:
- Enter: 175
- Result: ✅ No error, button enabled

Invalid Tests:
- Enter: 50  → ❌ "Height must be at least 100 cm"
- Enter: 300 → ❌ "Height must not exceed 250 cm"
- Enter: abc → ❌ "Please enter a valid number"
```

### 4. Grant Camera Permission
- When prompted, tap **"Allow"**
- Camera preview should appear
- Framing guide overlay should be visible

### 5. Capture Image
1. Enter valid height (e.g., **170**)
2. Position body within **white dashed frame**
3. Align head with **circle guide** at top
4. Tap **"Capture Image"** button
5. Success message should appear
6. Check logcat for: `Processing image data of size: X bytes`

---

## 📱 Alternative: Command Line Build

```bash
# Navigate to project
cd /Users/applelab4/AndroidStudioProjects/BodyScanApp

# Build APK (if Gradle works)
./gradlew assembleDebug

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk

# Launch app
adb shell am start -n com.example.bodyscanapp/.MainActivity

# Monitor logs
adb logcat -s ImageCaptureScreen:D
```

---

## ✅ Success Indicators

### Height Input Working:
- [ ] TextField accepts numbers
- [ ] Slider moves when text changes
- [ ] Text updates when slider moves
- [ ] Error messages appear for invalid input
- [ ] Button enables only for valid height

### Camera Working:
- [ ] Permission request appears
- [ ] Camera preview shows after granting permission
- [ ] Framing guide (white dashed lines) visible
- [ ] Feedback text displays at top

### Capture Working:
- [ ] Button is blue and enabled (valid height + permission)
- [ ] Image captures when button tapped
- [ ] Success message appears
- [ ] Logcat shows: "Processing image data of size: X bytes"

---

## 🐛 Troubleshooting

### Problem: Camera not showing
**Solution:**
- Check permission granted: Settings → Apps → BodyScanApp → Permissions → Camera
- Try physical device instead of emulator
- Restart app

### Problem: Build fails
**Solution:**
- This may be a Gradle/Java version issue (not code issue)
- Try: File → Invalidate Caches → Restart (in Android Studio)
- Try: `./gradlew clean` then rebuild

### Problem: Button always disabled
**Solution:**
- Ensure height is 100-250
- Check camera permission granted
- Look for error message under height field

### Problem: App crashes on capture
**Solution:**
- Check logcat for error
- Ensure camera preview loaded first
- Try different device/emulator

---

## 📊 Logcat Commands

### Monitor Image Capture:
```bash
adb logcat -s ImageCaptureScreen:D CameraPreview:E
```

### Full App Logs:
```bash
adb logcat | grep bodyscanapp
```

### Clear Logs:
```bash
adb logcat -c
```

---

## 📝 Test Scenarios

### Scenario 1: Happy Path ✅
1. Open app → Login → 2FA
2. Home → Tap "New Scan"
3. Enter height: 175
4. Grant camera permission
5. Align body in frame
6. Tap "Capture Image"
7. **Expected:** Success message + log entry

### Scenario 2: Invalid Height ❌
1. Open Image Capture Screen
2. Enter height: 50
3. **Expected:** Error message, button disabled

### Scenario 3: No Permission ⚠️
1. Open Image Capture Screen
2. Deny camera permission
3. **Expected:** Message "Camera permission is required"
4. Button disabled

### Scenario 4: Slider Only 🎚️
1. Open Image Capture Screen
2. Don't touch TextField
3. Move slider to 200
4. **Expected:** TextField shows "200", button enabled

---

## 🎯 Key Features to Verify

### Height Input:
- ✅ TextField and Slider sync
- ✅ Validation (100-250 cm)
- ✅ Error messages

### CameraX:
- ✅ Live preview
- ✅ Permission handling
- ✅ Back camera used

### Framing Guide:
- ✅ White dashed rectangle (body)
- ✅ White dashed circle (head)
- ✅ Horizontal alignment lines

### Capture:
- ✅ Button enabled/disabled states
- ✅ Image captured as ByteArray
- ✅ Passed to processImageData()

### Feedback:
- ✅ "Align body within the frame"
- ✅ "Image captured successfully!"
- ✅ Error messages on failure

---

## 📂 Files Changed

### Created:
- ✅ `ImageCaptureScreen.kt` - Main screen implementation
- ✅ `IMAGE_CAPTURE_TESTING_GUIDE.md` - Detailed testing
- ✅ `IMAGE_CAPTURE_IMPLEMENTATION_SUMMARY.md` - Summary
- ✅ `IMAGE_CAPTURE_SCREEN_SPEC.md` - UI specification
- ✅ `QUICK_START_IMAGE_CAPTURE.md` - This file

### Modified:
- ✅ `MainActivity.kt` - Added navigation
- ✅ `AndroidManifest.xml` - Added camera permission
- ✅ `build.gradle.kts` - Added CameraX dependencies
- ✅ `libs.versions.toml` - Added CameraX versions

---

## 🔍 What to Look For

### Visual Checks:
1. **Height Section**: Dark gray box with TextField, Slider, and value display
2. **Camera Preview**: Live feed filling middle section
3. **Framing Guide**: White dashed lines overlay on camera
4. **Feedback Text**: White text on semi-transparent black background
5. **Capture Button**: Blue button at bottom (or gray if disabled)

### Functional Checks:
1. **Input Validation**: Immediate feedback on invalid heights
2. **Permission Flow**: Smooth camera permission request
3. **Image Capture**: Quick capture with success confirmation
4. **Navigation**: Can navigate from Home → Image Capture

### Performance Checks:
1. **Preview Smoothness**: No lag in camera feed
2. **Capture Speed**: < 1 second from tap to success
3. **Memory**: No visible memory leaks
4. **Responsiveness**: UI remains responsive during capture

---

## 🆘 Get Help

### Check Documentation:
1. `IMAGE_CAPTURE_TESTING_GUIDE.md` - Comprehensive testing guide
2. `IMAGE_CAPTURE_SCREEN_SPEC.md` - UI/UX specifications
3. `IMAGE_CAPTURE_IMPLEMENTATION_SUMMARY.md` - Technical details

### Review Code:
- Main file: `app/src/main/java/com/example/bodyscanapp/ui/screens/ImageCaptureScreen.kt`
- Functions to check:
  - `ImageCaptureScreen()` - Main composable
  - `CameraPreview()` - Camera setup
  - `FramingGuideOverlay()` - Visual guide
  - `captureImage()` - Capture logic
  - `processImageData()` - Placeholder processing

### Logcat Tags:
- `ImageCaptureScreen` - Capture events
- `CameraPreview` - Camera errors
- `MainActivity` - Navigation events

---

## ✨ Next Steps

After successful testing:

1. **Implement Image Processing**
   - Replace `processImageData()` placeholder
   - Add actual body measurement logic

2. **Add Back Button**
   - UI element to return to Home

3. **Enhance Framing Guide**
   - Real-time body detection
   - Dynamic alignment feedback

4. **Add Image Preview**
   - Show captured image before processing

5. **Implement Retry**
   - Allow re-capture if image is bad

---

**Quick Start Version**: 1.0  
**Last Updated**: October 6, 2025  
**Status**: Ready to Test 🚀

