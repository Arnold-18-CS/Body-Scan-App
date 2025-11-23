# Scripts Directory

This directory contains all setup, build, and utility scripts for the Body Scan App project.

## Windows Scripts (PowerShell)

### `setup_windows.ps1`
**Purpose:** Initial project setup for Windows development environment.

**Functionality:**
- Checks for Java 21 installation
- Downloads Java 21 if missing
- Creates `gradle.properties.local` with Java 21 path
- Configures project-specific settings

**Usage:**
```powershell
.\scripts\setup_windows.ps1
```

---

### `setup_opencv.ps1`
**Purpose:** Downloads and configures OpenCV Android SDK for image preprocessing.

**Functionality:**
- Downloads OpenCV Android SDK
- Extracts to `opencv-android-sdk/` directory
- Configures OpenCV for use in the project

**Usage:**
```powershell
.\scripts\setup_opencv.ps1
```

**Note:** OpenCV is used only for image preprocessing (CLAHE, resizing). Pose detection is handled by MediaPipe.

---

### `build_with_env_fix.ps1`
**Purpose:** Build script with environment variable fix for Room database on Windows.

**Functionality:**
- Sets TMP and TEMP environment variables to user-writable directory
- Runs Gradle build commands with proper environment configuration
- Fixes Room SQLite native library extraction issues on Windows

**Usage:**
```powershell
# Run as Administrator (if needed)
.\scripts\build_with_env_fix.ps1 clean assembleDebug

# Or for release builds
.\scripts\build_with_env_fix.ps1 clean assembleRelease
```

**Note:** This script addresses the Room database issue where SQLite native libraries need to be extracted to a user-writable temp directory on Windows.

---

## macOS/Linux Scripts (Bash)

### `setup_mac.sh`
**Purpose:** Initial project setup for macOS/Linux development environment.

**Functionality:**
- Checks for Java 21 installation
- Downloads Java 21 if missing (via Homebrew or direct download)
- Creates `gradle.properties.local` with Java 21 path
- Configures project-specific settings

**Usage:**
```bash
# Make executable (first time only)
chmod +x scripts/setup_mac.sh

# Run setup
./scripts/setup_mac.sh
```

---

### `setup_opencv.sh`
**Purpose:** Downloads and configures OpenCV Android SDK for image preprocessing.

**Functionality:**
- Downloads OpenCV Android SDK
- Extracts to `opencv-android-sdk/` directory
- Configures OpenCV for use in the project

**Usage:**
```bash
# Make executable (first time only)
chmod +x scripts/setup_opencv.sh

# Run setup
./scripts/setup_opencv.sh
```

**Note:** OpenCV is used only for image preprocessing (CLAHE, resizing). Pose detection is handled by MediaPipe.

---

### `run_tests.sh`
**Purpose:** Convenience script for running all project tests.

**Functionality:**
- Runs unit tests
- Runs integration tests
- Provides test coverage information

**Usage:**
```bash
# Make executable (first time only)
chmod +x scripts/run_tests.sh

# Run all tests
./scripts/run_tests.sh
```

---

## Script Execution Order

### First-Time Setup (Windows)
1. `setup_windows.ps1` - Configure Java and project settings
2. `setup_opencv.ps1` - Download and configure OpenCV
3. `build_with_env_fix.ps1` - Build project (if Room issues occur)

### First-Time Setup (macOS/Linux)
1. `setup_mac.sh` - Configure Java and project settings
2. `setup_opencv.sh` - Download and configure OpenCV
3. `./gradlew clean assembleDebug` - Build project normally

---

## Troubleshooting

### Script Execution Permissions
**macOS/Linux:** If scripts are not executable, run:
```bash
chmod +x scripts/*.sh
```

### PowerShell Execution Policy (Windows)
If scripts fail to run, you may need to adjust PowerShell execution policy:
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### Java Version Issues
If Java 21 is not detected correctly:
- Windows: Re-run `setup_windows.ps1`
- macOS: Re-run `setup_mac.sh` or manually set Java path in `gradle.properties.local`

---

## Notes

- All scripts should be run from the project root directory
- Scripts create/modify `gradle.properties.local` which is gitignored
- OpenCV setup is optional if you only need MediaPipe pose detection
- Build scripts handle platform-specific issues automatically

