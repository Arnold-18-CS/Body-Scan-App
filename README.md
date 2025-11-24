# Body Scan App

**Status:** ✅ Project Complete

A lightweight, privacy-focused Android application for 3D body scanning using monocular photogrammetry. The app captures the user's body RGB images, processes it on-device using **MediaPipe** for pose detection and **OpenCV** for image preprocessing, delivering accurate anthropometric measurements with sub-centimetre precision. Built for mid-range Android devices (API 24+), it ensures fast processing (<5s), low memory usage (<100MB), and no cloud dependency.

**For detailed technical information, see:**

- [Technical Report](docs/Technical_Report.md) - Concise technical overview
- [Detailed Technical Report](docs/Detailed_Technical_Report.md) - Comprehensive technical documentation
- [Diagrammatic Flowchart of Application Process](docs/Scan_Process_Flowchart.md) - Comprehensive and executive summaries of the application processing pipeline using flowcharts

## Features

- **User Authentication**: Secure login with email/password and 2FA (TOTP) using EncryptedSharedPreferences
- **Image Capture**: CameraX-based capture with real-time framing guides and height input (100–250cm) for scale calibration
- **On-Device Processing**: 
  - **MediaPipe** for accurate pose detection (33 landmarks mapped to 135 keypoints)
  - **OpenCV** for image preprocessing (CLAHE, resizing, color space conversion)
  - Native C++ library handles measurement calculations from MediaPipe landmarks
- **Data Management**: Local Room database stores user profiles, scans, and measurements with CRUD operations
- **Export Options**: Measurements exportable as JSON, CSV, or PDF
- **UI**: Built with Jetpack Compose for responsive, intuitive screens
- **Performance**: Optimized for <5s scan processing, <1s UI actions, and <100MB memory usage
- **Privacy**: All data processed and stored locally, with images deleted post-processing

## Tech Stack

- **Frontend**: Jetpack Compose, CameraX, Kotlin Coroutines
- **Backend**: Room 2.8.4, EncryptedSharedPreferences, Gson, iText7
- **Pose Detection**: MediaPipe Tasks Vision API 0.10.14 (33 landmarks → 135 keypoints mapping)
- **Image Preprocessing**: OpenCV 4.9.0 (CLAHE, resizing, color conversion)
- **Native**: C++ with NDK, CMake, JNI bridge between MediaPipe (Kotlin) and C++
- **Testing**: JUnit, Espresso, Robolectric
- **Build**: Gradle (Kotlin DSL), Android Studio

## Prerequisites

### Required Software

- **Android Studio** (Koala or later)
- **Java JDK 21** (required for Room 2.8.4 and Kotlin 2.2)
- **Android SDK** (API 24+)
- **NDK** (26.1.10909125 or later)
- **CMake** (3.22 or later)
- **Git**

### Platform-Specific Requirements

**Windows:**

- PowerShell 5.1+ or PowerShell 7+
- Administrator privileges (for Room SQLite native library extraction)

**macOS:**

- Homebrew (optional, for Java installation)
- Terminal with bash

---

## Setup Instructions

### Windows Setup

#### Step 1: Clone the Repository

```powershell
git clone https://github.com/yourusername/body-scan-app.git
cd body-scan-app
```

#### Step 2: Run Setup Script

```powershell
# Run the Windows setup script
.\scripts\setup_windows.ps1
```

This script will:

- Check for Java 21 and download if needed
- Create `gradle.properties.local` with Java 21 path
- Configure project-specific settings

#### Step 3: Setup OpenCV (for Image Preprocessing)

```powershell
# Download and configure OpenCV Android SDK
.\scripts\setup_opencv.ps1
```

This downloads OpenCV Android SDK to `opencv-android-sdk/` directory. **Note:** OpenCV is used only for image preprocessing (CLAHE, resizing). Pose detection is handled by MediaPipe.

#### Step 4: Verify MediaPipe Model File

The MediaPipe pose detection model should be in `app/src/main/assets/pose_landmarker.task`. If missing:

1. Download from [MediaPipe Model Zoo](https://developers.google.com/mediapipe/solutions/vision/pose_landmarker)
2. Place in `app/src/main/assets/pose_landmarker.task`

#### Step 5: Configure Environment Variables (IMPORTANT for Room)

Room's annotation processor requires proper temp directory configuration on Windows:

**Option A: Set System-Wide Environment Variables (Recommended)**

1. Open **System Properties** → **Environment Variables**
2. Under **System variables**, add/edit:
   - `TMP` = `C:\Users\<YourUsername>\AppData\Local\Temp\gradle-temp`
   - `TEMP` = `C:\Users\<YourUsername>\AppData\Local\Temp\gradle-temp`
3. Click **OK** and **restart your terminal/IDE**

**Option B: Use Build Script (Quick Fix)**

```powershell
# Run as Administrator
.\scripts\build_with_env_fix.ps1 clean assembleDebug
```

#### Step 6: Open in Android Studio

1. Launch Android Studio
2. Select **Open an existing project**
3. Choose the `body-scan-app` folder
4. Wait for Gradle sync to complete

#### Step 7: Build and Run

```powershell
# Build the project
.\gradlew.bat clean assembleDebug

# Or use the environment fix script
.\scripts\build_with_env_fix.ps1 clean assembleDebug
```

---

### macOS Setup

#### Step 1: Clone the Repository

```bash
git clone https://github.com/yourusername/body-scan-app.git
cd body-scan-app
```

#### Step 2: Run Setup Script

```bash
# Make scripts executable
chmod +x scripts/setup_mac.sh scripts/setup_opencv.sh

# Run the macOS setup script
./scripts/setup_mac.sh
```

This script will:

- Check for Java 21 and download if needed
- Create `gradle.properties.local` with Java 21 path
- Configure project-specific settings

#### Step 3: Setup OpenCV (for Image Preprocessing)

```bash
# Download and configure OpenCV Android SDK
./scripts/setup_opencv.sh
```

**Note:** OpenCV is used only for image preprocessing (CLAHE, resizing). Pose detection is handled by MediaPipe.

#### Step 4: Verify MediaPipe Model File

The MediaPipe pose detection model should be in `app/src/main/assets/pose_landmarker.task`. If missing:

1. Download from [MediaPipe Model Zoo](https://developers.google.com/mediapipe/solutions/vision/pose_landmarker)
2. Place in `app/src/main/assets/pose_landmarker.task`

#### Step 5: Open in Android Studio

1. Launch Android Studio
2. Select **Open an existing project**
3. Choose the `body-scan-app` folder
4. Wait for Gradle sync to complete

#### Step 6: Build and Run

```bash
# Build the project
./gradlew clean assembleDebug
```

---

## Project Structure

```
body-scan-app/
├── app/                          # Main Android app module
│   ├── src/main/
│   │   ├── java/                 # Kotlin source code
│   │   │   └── com/example/bodyscanapp/
│   │   │       ├── data/         # Room database, repositories
│   │   │       ├── ui/           # Jetpack Compose screens
│   │   │       └── utils/        # Utilities, MediaPipe helper
│   │   ├── cpp/                  # C++ native code
│   │   │   ├── include/         # Header files
│   │   │   └── src/             # Implementation files
│   │   ├── jni/                 # JNI bridge and CMakeLists.txt
│   │   ├── assets/              # MediaPipe model files
│   │   └── res/                 # Resources
│   ├── libs/                    # Local AAR files (if any)
│   └── build.gradle.kts         # App dependencies
├── gradle/                       # Gradle wrapper and version catalog
├── opencv-android-sdk/          # OpenCV Android SDK
├── scripts/                     # Setup and build scripts
│   ├── setup_windows.ps1       # Windows setup script
│   ├── setup_mac.sh            # macOS setup script
│   ├── setup_opencv.ps1        # OpenCV setup (Windows)
│   ├── setup_opencv.sh         # OpenCV setup (macOS)
│   ├── build_with_env_fix.ps1  # Build script with environment fix (Windows)
│   └── run_tests.sh            # Test runner script
├── docs/                        # Documentation
│   ├── images/                 # Documentation images
│   │   ├── pose_landmark_topology.svg
│   │   ├── landmarked_pose.jpeg
│   │   ├── sample_measurement_on_ui.jpeg
│   │   └── README.md
│   ├── Technical_Report.md      # Concise technical report
│   └── Detailed_Technical_Report.md  # Detailed technical report
└── README.md                    # This file
```

---

## Common Issues and Solutions

### Issue 1: Room SQLite Native Library Error (Windows)

**Error:**
```
java.nio.file.AccessDeniedException: C:\WINDOWS\sqlite-*.dll.lck
No native library found for os.name=Windows, os.arch=x86_64
```

**Cause:** Room's annotation processor tries to extract SQLite native libraries to `C:\WINDOWS`, which requires administrator privileges. Gradle worker processes don't inherit environment variables.

**Solutions:**

1. **Set System-Wide Environment Variables (Recommended)**
   - Open **System Properties** → **Environment Variables**
   - Add `TMP` and `TEMP` pointing to `C:\Users\<YourUsername>\AppData\Local\Temp\gradle-temp`
   - **Restart your terminal/IDE** completely
   - Run build normally

2. **Run as Administrator**

   ```powershell

   # Right-click PowerShell → Run as Administrator
   cd "C:\Users\arnol\Desktop\Body-Scan-App"
   .\scripts\build_with_env_fix.ps1 clean assembleDebug
   ```

3. **Use Build Script**

   ```powershell
   .\scripts\build_with_env_fix.ps1 clean assembleDebug
   ```

**Note:** Room 2.8.4 is already configured. The issue is with environment variable inheritance by Gradle workers.

---

### Issue 2: MediaPipe Native Library Not Found

**Error:**
```
java.lang.UnsatisfiedLinkError: dlopen failed: library "libmediapipe_tasks_vision_jni.so" not found
```

**Cause:** MediaPipe native libraries aren't being packaged in the APK or device architecture mismatch.

**Solutions:**

1. **Verify Native Libraries in APK**

   ```powershell
   # Extract APK and check
   Expand-Archive -Path "app\build\outputs\apk\debug\app-debug.apk" -DestinationPath "apk_check"
   Get-ChildItem "apk_check\lib" -Recurse -Filter "*mediapipe*.so"
   ```

2. **Check Device Architecture**
   - Ensure your device/emulator architecture matches: `arm64-v8a`, `armeabi-v7a`, or `x86`
   - MediaPipe AAR includes native libraries for these architectures

3. **Rebuild Project**

   ```powershell
   .\gradlew.bat clean assembleDebug
   ```

**Note:** Native libraries are automatically included from MediaPipe Maven dependencies. The build configuration is already set up correctly.

---

### Issue 3: Java Version Mismatch

**Error:**
```
Unsupported class file major version 65
```

**Cause:** Project requires Java 21, but a different version is being used.

**Solutions:**

1. **Windows:**
   - Run `.\scripts\setup_windows.ps1` to configure Java 21
   - Verify `gradle.properties.local` has correct Java path

2. **macOS:**
   - Run `./scripts/setup_mac.sh` to configure Java 21
   - Or install via Homebrew: `brew install openjdk@21`

3. **Verify Java Version**

   ```bash
   java -version  # Should show version 21
   ```

---

### Issue 4: OpenCV Not Found

**Error:**
```
OpenCV not found at ${OpenCV_DIR}
```

**Cause:** OpenCV Android SDK not downloaded or configured. **Note:** OpenCV is only used for image preprocessing (CLAHE, resizing), not for pose detection.

**Solutions:**

1. **Windows:**

   ```powershell
   .\scripts\setup_opencv.ps1
   ```

2. **macOS:**

   ```bash
   ./scripts/setup_opencv.sh
   ```

3. **Manual Setup:**
   - Download OpenCV Android SDK from https://opencv.org/releases/
   - Extract to `opencv-android-sdk/` in project root
   - Verify structure: `opencv-android-sdk/sdk/native/jni/OpenCVConfig.cmake`

---

### Issue 5: MediaPipe Model File Missing

**Error:**
```
Failed to find pose model
Model path is null
```

**Cause:** MediaPipe pose detection model (`pose_landmarker.task`) not found in assets.

**Solutions:**

1. **Download MediaPipe Model:**
   - Visit [MediaPipe Model Zoo](https://developers.google.com/mediapipe/solutions/vision/pose_landmarker)
   - Download `pose_landmarker.task` (full or lite version)
   - Place in `app/src/main/assets/pose_landmarker.task`

2. **Verify Model File:**

   ```bash
   # Check if model exists
   ls app/src/main/assets/pose_landmarker.task
   ```

3. **Rebuild Project:**
   After adding the model file, rebuild the project to include it in the APK.

---

### Issue 6: Gradle Sync Fails

**Error:**
```
Gradle sync failed
```

**Solutions:**

1. **Clean Gradle Cache**

   ```bash
   # Windows
   Remove-Item -Recurse -Force .gradle
   
   # macOS
   rm -rf .gradle
   ```

2. **Invalidate Caches in Android Studio**
   - File → Invalidate Caches → Invalidate and Restart

3. **Re-run Setup Script**

   ```powershell
   # Windows
   .\scripts\setup_windows.ps1
   
   # macOS
   ./scripts/setup_mac.sh
   ```

---

### Issue 7: Build Fails with "25" Error (macOS)

**Error:**
```
Error: Kotlin compiler version mismatch
```

**Cause:** Using Java 25 instead of Java 21.

**Solution:**

- Use `./gradlew` (not `./gradlew_mac.sh` - that script is removed)
- Ensure `gradle.properties.local` has correct Java 21 path
- Run `./scripts/setup_mac.sh` to reconfigure

---

## Testing

### Running Tests

**Unit Tests:**

```bash
# Windows
.\gradlew.bat test

# macOS
./gradlew test
```

**Integration Tests:**

```bash
# Windows
.\gradlew.bat connectedAndroidTest

# macOS
./gradlew connectedAndroidTest
```

**All Tests:**

```bash
# Windows
.\gradlew.bat test connectedAndroidTest

# macOS
./gradlew test connectedAndroidTest
```

### Test Coverage

- **Unit Tests**: JUnit for database, repositories, and utilities
- **Integration Tests**: AndroidJUnit4 for database and native bridge
- **UI Tests**: Espresso/Compose Testing
- **Performance Tests**: Validated on mid-range devices

---

## Building the Project

### Debug Build

```bash
# Windows
.\gradlew.bat clean assembleDebug

# macOS
./gradlew clean assembleDebug
```

### Release Build

```bash
# Windows
.\gradlew.bat clean assembleRelease

# macOS
./gradlew clean assembleRelease
```

**Note:** For Windows, if you encounter Room SQLite issues, use:

```powershell
.\scripts\build_with_env_fix.ps1 clean assembleDebug
```

---

## Configuration Files

### `gradle.properties.local`

Platform-specific Gradle configuration (auto-generated by setup scripts):

- Java 21 home path
- JVM arguments
- Platform-specific settings

**Note:** This file is gitignored and should not be committed.

### `gradle.properties`

Shared Gradle configuration (platform-independent):

- JVM arguments with temp directory
- AndroidX settings
- Kotlin code style

### `app/build.gradle.kts`

App-level dependencies and configuration:

- Room 2.8.4
- MediaPipe Tasks API 0.10.14
- Jetpack Compose
- CameraX
- Native build configuration

---

## Dependencies

### Key Dependencies

- **Room**: 2.8.4 (database)
- **MediaPipe Tasks Vision**: 0.10.14 (pose detection - 33 landmarks)
- **MediaPipe Tasks Core**: 0.10.14 (core MediaPipe functionality)
- **OpenCV**: 4.9.0 (image preprocessing only - CLAHE, resizing)
- **Jetpack Compose**: Latest BOM
- **CameraX**: 1.4.1
- **Kotlin**: 2.2.20
- **Android Gradle Plugin**: 8.13.1

**Note:** MediaPipe handles all pose detection. OpenCV is used only for image preprocessing (contrast enhancement, resizing) before MediaPipe processing.

See `gradle/libs.versions.toml` for complete version catalog.

---

## Architecture Overview

### Pose Detection Pipeline

1. **Image Capture**: CameraX captures RGB image
2. **Image Preprocessing** (OpenCV):
   - Resize to target width (~640px)
   - Apply CLAHE (Contrast Limited Adaptive Histogram Equalization)
   - Color space conversion (RGB ↔ BGR ↔ LAB)
3. **Pose Detection** (MediaPipe):
   - MediaPipe Pose Landmarker detects 33 body landmarks
   - Landmarks converted to normalized coordinates (0-1 range)
   - Segmentation masks extracted for pixel-level measurements
4. **Keypoint Mapping** (C++):
   - 33 MediaPipe landmarks mapped to 135 keypoints
   - Interpolation for intermediate body points
5. **Measurement Calculation** (C++):
   - Measurements calculated from keypoint positions
   - Uses segmentation masks for pixel-level accuracy
   - Calibrated using user height input

### MediaPipe Integration

- **Kotlin Layer**: `MediaPipePoseHelper` manages MediaPipe PoseLandmarker lifecycle
- **JNI Bridge**: `MediaPipePoseDetector` (C++) converts OpenCV Mat ↔ Android Bitmap
- **Native Processing**: `PoseEstimator` uses MediaPipe via JNI for detection
- **Model**: `pose_landmarker.task` loaded from assets at runtime

---

## Project Status

✅ **Project Complete** - All core functionality implemented and tested:

- ✅ Image capture and preprocessing pipeline
- ✅ MediaPipe pose detection with 33→135 keypoint mapping
- ✅ Anthropometric measurement calculation (8 measurements)
- ✅ 3D reconstruction from multi-view images
- ✅ Local data management with Room database
- ✅ User authentication (Firebase, 2FA, Biometric)
- ✅ Export functionality (JSON, CSV, PDF)
- ✅ Performance optimization (<5s processing, <100MB memory)

See [Technical Report](docs/Technical_Report.md) for comprehensive technical documentation.

---

## Contributing

1. Fork the repository and create feature branches (`feature/issue-#`)
2. Submit pull requests with clear descriptions, referencing issues
3. Follow coding standards: Kotlin style guide, modular C++ with comments
4. Update documentation with changes

---

## License

This project is for academic purposes and not licensed for commercial use. Contact the author (Arnold, arnold.oketch@strathmore.edu) for permissions.

---

## Acknowledgements

- Strathmore University School of Computing and Engineering Sciences
- ICS 4A cohort for feedback
- Library staff for literature support

---

## Additional Resources

- [Android Developer Documentation](https://developer.android.com)
- [MediaPipe Pose Landmarker](https://developers.google.com/mediapipe/solutions/vision/pose_landmarker)
- [MediaPipe Tasks API](https://ai.google.dev/edge/mediapipe/solutions/tasks)
- [OpenCV Documentation](https://docs.opencv.org) (for image preprocessing)
- [Room Database Guide](https://developer.android.com/training/data-storage/room)
