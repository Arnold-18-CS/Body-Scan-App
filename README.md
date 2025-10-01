# Body Scan App
A lightweight, privacy-focused Android application for 3D body scanning using monocular photogrammetry. The app captures the user's body RGB images, processes it on-device with a custom C++ native library, and delivers accurate anthropometric measurements (e.g., shoulder width, waist) with sub-centimetre precision. Built for mid-range Android devices (API 24+), it ensures fast processing (<5s), low memory usage (<100MB), and no cloud dependency. 

## Features

- User Authentication: Secure login with email/password and 2FA (TOTP) using EncryptedSharedPreferences.
- Image Capture: CameraX-based capture with real-time framing guides and height input (100–250cm) for scale calibration.
- On-Device Processing: Native C++ library (body-scan-native-lib) handles image preprocessing, landmark detection, scaling, and measurement calculations using OpenCV and TFLite.
- Data Management: Local Room database stores user profiles, scans, and measurements with CRUD operations
- Export Options: Measurements exportable as JSON, CSV, or PDF.
- UI: Built with Jetpack Compose for responsive, intuitive screens (Login, 2FA, Home, Image Capture, Processing, Results, Scan History, Profile).
- Performance: Optimized for <5s scan processing, <1s UI actions, and <100MB memory usage.
- Privacy: All data processed and stored locally, with images deleted post-processing.

## Project Goals

1. Achieve mean absolute error (MAE) of <1cm in controlled settings and <2cm in real-world conditions.
2. Ensure 95% alignment success rate with user-friendly framing guides.
3. Support applications in healthcare, fitness, and apparel fitting.

##Tech Stack

- Frontend: Jetpack Compose, CameraX, Kotlin Coroutines
- Backend: Room, EncryptedSharedPreferences, Gson, iText
- Native: C++ with NDK, CMake, OpenCV, TensorFlow Lite
- Testing: JUnit, Espresso, manual testing with 5 peers
- Build: Gradle (Kotlin DSL), Android Studio
- Version Control: Git/GitHub with structured branching

## Setup Instructions
### Prerequisites

- Android Studio (Koala or later)
- NDK (25.1.8937393 or later)
- CMake (3.22 or later)
- Java JDK 17+
- Git

### Steps

1. Clone the Repository
```bash
git clone https://github.com/yourusername/body-scan-app.git
cd body-scan-app
```

2. Open in Android Studio
- Launch Android Studio, select "Open an existing project", and choose the body-scan-app folder.
- Sync Gradle (File > Sync Project with Gradle Files).

3. Configure NDK and CMake
- In SDK Manager (Tools > SDK Manager), install:
- Android SDK Platform API 24+.
- NDK (e.g., 25.1.8937393).
- CMake (e.g., 3.22).

4. Add to local.properties:
```plain
ndk.dir=/path/to/ndk
sdk.dir=/path/to/sdk
```

5. Add OpenCV and TFLite
- Download OpenCV Android SDK, extract to project root (e.g., opencv/), and set OPENCV_ANDROID_SDK=/path/to/opencv in local.properties.
- Place TensorFlow Lite C++ libraries in body-scan-native-lib/src/main/jniLibs.


6. Build and Run
- Build: Build > Make Project.
- Run: Create an emulator (AVD Manager, API 24+) or connect a physical device (USB debugging enabled).
- Run app module (app).


## Project Structure
```plain
body-scan-app/
├── .git/                    # Git version control
├── app/                     # Main Android app module
│   ├── src/main/
│   │   ├── java/com/example/bodyscanapp/  # Kotlin: MainActivity.kt, NativeBridge.kt, UI screens
│   │   ├── res/                          # Resources: drawables, icons
│   │   └── AndroidManifest.xml           # App permissions (camera, etc.)
│   └── build.gradle.kts                 # App dependencies (Compose, CameraX, Room)
├── body-scan-native-lib/    # Native C++ library module
│   ├── src/main/
│   │   ├── jni/                         # CMakeLists.txt, .cpp files (ImagePreprocessor, PoseEstimator, etc.)
│   │   └── jniLibs/                     # Prebuilt .so files (if any)
│   └── build.gradle.kts                 # Native build config
├── gradle/                  # Gradle wrapper
├── settings.gradle.kts      # Module includes
└── build.gradle.kts         # Root Gradle config
```

## Testing
- Unit Tests: JUnit for database and logic.
- UI Tests: Espresso for screen flows.
- Manual Tests: Conducted with 5 peers for 95% alignment success.
- Validate on mid-range devices (e.g., Snapdragon 600-series).

## Contributing
1. Fork the repository and create feature branches (feature/issue-#).
2. Submit pull requests with clear descriptions, referencing issues.
3. Follow coding standards: Kotlin style guide, modular C++ with comments.
4. Update documentation (e.g., README, code comments) with changes.

## License
This project is for academic purposes and not licensed for commercial use. Contact the author (Arnold, arnold.oketch@strathmore.edu) for permissions.

## Acknowledgements
- Strathmore University School of Computing and Engineering Sciences
- ICS 4A cohort for feedback
- Library staff for literature support
