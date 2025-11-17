# OpenCV Setup for Body Scan App

OpenCV has been successfully downloaded and set up for this project.

## Location
OpenCV Android SDK is located at: `opencv-android-sdk/` (project root)

## Verification
The CMakeLists.txt will automatically detect OpenCV at this location when building.

## Manual Setup (if needed)

If you need to set up OpenCV manually:

### Windows
```powershell
.\setup_opencv.ps1
```

### Linux/Mac
```bash
chmod +x setup_opencv.sh
./setup_opencv.sh
```

### Manual Download
1. Download OpenCV Android SDK from: https://opencv.org/releases/
2. Extract to: `opencv-android-sdk/` in the project root
3. Ensure the structure is: `opencv-android-sdk/sdk/native/jni/OpenCVConfig.cmake`

## CMake Configuration
The CMakeLists.txt automatically looks for OpenCV at:
- `opencv-android-sdk/` (project root)
- Or the path specified in `OpenCV_DIR` environment variable

## Building the Project
Once OpenCV is set up, you can build the project:

```bash
./gradlew clean
./gradlew build
```

The build will automatically link OpenCV libraries.

## Troubleshooting

If you see errors about OpenCV not being found:
1. Verify `opencv-android-sdk/sdk/native/jni/OpenCVConfig.cmake` exists
2. Check that the path in CMakeLists.txt is correct
3. Try running the setup script again
4. Manually set `OpenCV_DIR` in Android Studio's CMake settings




