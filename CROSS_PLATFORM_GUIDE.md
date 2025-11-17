# Cross-Platform Development Guide (Mac & Windows)

Complete guide for developing on both **macOS** (office) and **Windows** (home) without configuration conflicts.

---

## 🎯 Quick Start

### Mac (Office)
```bash
# First time setup
./setup_mac.sh
./setup_opencv.sh

# Daily use
./gradlew_mac.sh clean
./gradlew_mac.sh assembleDebug
```

### Windows (Home)
```powershell
# First time setup
.\setup_windows.ps1
.\setup_opencv.ps1

# Daily use
.\gradlew.bat clean
.\gradlew.bat assembleDebug
```

**Key Point**: On Mac, always use `./gradlew_mac.sh` instead of `./gradlew` to ensure Java 21 is used.

---

## 📖 Table of Contents

1. [Overview](#overview)
2. [File Structure](#file-structure)
3. [Setup Instructions](#setup-instructions)
4. [How It Works](#how-it-works)
5. [Switching Between Machines](#switching-between-machines)
6. [File Reference](#file-reference)
7. [Troubleshooting](#troubleshooting)
8. [Checklist](#checklist)
9. [Summary](#summary)

---

## Overview

### The Problem Solved

This project is configured to work seamlessly on both Mac and Windows without conflicts. Platform-specific settings (like Java paths) are kept separate from shared settings.

### Key Principle

- **Shared settings** → `gradle.properties` (committed to git)
- **Platform-specific paths** → `gradle.properties.local` (NOT committed, auto-generated per machine)

### What Changed

1. ✅ `gradle.properties` - Now contains only platform-independent settings
2. ✅ `gradle.properties.local` - Auto-generated with your local Java path (NOT in git)
3. ✅ Setup scripts - `setup_mac.sh` and `setup_windows.ps1` for easy configuration
4. ✅ `gradlew_mac.sh` - Mac Gradle wrapper that uses Java 21 from local config
5. ✅ `.gitignore` - Updated to exclude `gradle.properties.local`

---

## File Structure

### Files to Commit to Git ✅

These files are shared and should be committed:

| File | Purpose | Platform |
|------|---------|----------|
| `gradle.properties` | Shared Gradle settings | Both |
| `gradle.properties.mac.template` | Mac template | Both |
| `gradle.properties.windows.template` | Windows template | Both |
| `setup_mac.sh` | Mac setup script | Both |
| `setup_windows.ps1` | Windows setup script | Both |
| `gradlew_mac.sh` | Mac Gradle wrapper | Both |
| `build.gradle.kts` | Build configuration | Both |
| `settings.gradle.kts` | Project settings | Both |
| All source code | Kotlin, C++, etc. | Both |

### Files NOT to Commit ❌

These files are platform-specific and in `.gitignore`:

| File/Directory | Purpose | Why Not Committed |
|----------------|---------|-------------------|
| `gradle.properties.local` | Your local Java path | Different on each machine |
| `local.properties` | Your Android SDK path | Different on each machine |
| `.java/` | Local Java 21 installation | Large, downloaded separately |
| `opencv-android-sdk/` | OpenCV SDK | Very large (~100MB+) |
| `.gradle/` | Gradle cache | Build artifacts |
| `build/` | Build output | Build artifacts |
| `app/google-services.json` | Firebase config | Contains sensitive keys |

### Directory Structure

```
Body-Scan-App/
├── gradle.properties              ✅ COMMIT - Shared settings
├── gradle.properties.local        ❌ IGNORE - Your local Java path
├── gradle.properties.mac.template ✅ COMMIT - Mac template
├── gradle.properties.windows.template ✅ COMMIT - Windows template
├── setup_mac.sh                   ✅ COMMIT - Mac setup script
├── setup_windows.ps1              ✅ COMMIT - Windows setup script
├── gradlew_mac.sh                 ✅ COMMIT - Mac Gradle wrapper
├── local.properties               ❌ IGNORE - Your SDK path
├── .java/                         ❌ IGNORE - Local Java
├── opencv-android-sdk/            ❌ IGNORE - Large binary
└── ...
```

---

## Setup Instructions

### On macOS (Office)

#### First Time Setup

1. **Clone the repository** (if not already done)
   ```bash
   git clone <your-repo-url>
   cd Body-Scan-App
   ```

2. **Run the Mac setup script**
   ```bash
   ./setup_mac.sh
   ```
   This will:
   - Check for Java 21 (downloads to `.java/` if needed)
   - Create `gradle.properties.local` with Mac-specific paths
   - Check OpenCV setup

3. **Set up OpenCV** (if not already done)
   ```bash
   ./setup_opencv.sh
   ```
   This downloads and extracts OpenCV Android SDK (~100MB).

4. **Test the build**
   ```bash
   ./gradlew_mac.sh clean
   ./gradlew_mac.sh assembleDebug
   ```

5. **Open in Android Studio**
   - Open the project
   - Sync Gradle files
   - Build should work!

#### Daily Use

```bash
# Clean build
./gradlew_mac.sh clean

# Build debug APK
./gradlew_mac.sh assembleDebug

# Install on device
./gradlew_mac.sh installDebug
```

**Important**: Always use `./gradlew_mac.sh` instead of `./gradlew` on Mac to ensure Java 21 is used.

### On Windows (Home)

#### First Time Setup

1. **Clone the repository** (if not already done)
   ```powershell
   git clone <your-repo-url>
   cd Body-Scan-App
   ```

2. **Run the Windows setup script**
   ```powershell
   .\setup_windows.ps1
   ```
   This will:
   - Check for Java 21 (prompts for path if not found)
   - Create `gradle.properties.local` with Windows-specific paths
   - Check OpenCV setup

3. **Set up OpenCV** (if not already done)
   ```powershell
   .\setup_opencv.ps1
   ```
   This downloads and extracts OpenCV Android SDK (~100MB).

4. **Test the build**
   ```powershell
   .\gradlew.bat clean
   .\gradlew.bat assembleDebug
   ```

5. **Open in Android Studio**
   - Open the project
   - Sync Gradle files
   - Build should work!

#### Daily Use

```powershell
# Clean build
.\gradlew.bat clean

# Build debug APK
.\gradlew.bat assembleDebug

# Install on device
.\gradlew.bat installDebug
```

---

## How It Works

### Gradle Properties Loading Order

Gradle loads properties files in this order (later files override earlier ones):

1. `gradle.properties` (shared, committed to git)
2. `gradle.properties.local` (platform-specific, NOT committed)
3. System properties
4. Command-line arguments

This means:
- **Shared settings** go in `gradle.properties` (committed)
- **Platform-specific paths** go in `gradle.properties.local` (NOT committed)
- Each developer has their own `gradle.properties.local`

### Mac Gradle Wrapper

On Mac, `./gradlew_mac.sh` is a wrapper script that:
- Reads Java path from `gradle.properties.local`
- Sets `JAVA_HOME` before running Gradle
- Ensures Java 21 is used even if system Java is 25

**Why?** The system Java might be version 25, which Kotlin doesn't support yet. The wrapper ensures Java 21 is used.

### Platform-Specific Settings

The following settings are platform-specific and go in `gradle.properties.local`:

| Setting | Mac Example | Windows Example |
|---------|-------------|-----------------|
| `org.gradle.java.home` | `/Users/username/.java/jdk-21/Contents/Home` | `C:\Users\username\.java\jdk-21` |
| Path format | Unix-style (`/`) | Windows-style (`\` or `/`) |

### Mac Path Format
- Uses forward slashes: `/`
- Example: `/Users/username/.java/jdk-21/Contents/Home`

### Windows Path Format
- Can use forward slashes: `C:/Users/username/.java/jdk-21`
- Or escaped backslashes: `C:\\Users\\username\\.java\\jdk-21`
- Gradle accepts both formats

---

## Switching Between Machines

### When You Switch from Mac to Windows

1. **Pull latest changes**
   ```powershell
   git pull
   ```

2. **Run Windows setup** (if `gradle.properties.local` doesn't exist or is Mac-specific)
   ```powershell
   .\setup_windows.ps1
   ```
   This creates/updates `gradle.properties.local` with Windows paths.

3. **Set up OpenCV** (if not already done on Windows)
   ```powershell
   .\setup_opencv.ps1
   ```

4. **Open in Android Studio** and sync Gradle

5. **Build**
   ```powershell
   .\gradlew.bat clean
   .\gradlew.bat assembleDebug
   ```

### When You Switch from Windows to Mac

1. **Pull latest changes**
   ```bash
   git pull
   ```

2. **Run Mac setup** (if `gradle.properties.local` doesn't exist or is Windows-specific)
   ```bash
   ./setup_mac.sh
   ```
   This creates/updates `gradle.properties.local` with Mac paths.

3. **Set up OpenCV** (if not already done on Mac)
   ```bash
   ./setup_opencv.sh
   ```

4. **Use Mac Gradle wrapper**
   ```bash
   ./gradlew_mac.sh clean
   ./gradlew_mac.sh assembleDebug
   ```

5. **Open in Android Studio** and sync Gradle

### Daily Workflow

When switching machines daily:
1. Pull latest changes: `git pull`
2. Verify `gradle.properties.local` exists (or run setup script)
3. Open in Android Studio
4. Sync Gradle
5. Build and test

---

## File Reference

### Mac-Specific Files

**Created on Mac:**
- `gradle.properties.local` - Contains Mac Java path
  ```properties
  org.gradle.java.home=/Users/yourusername/.java/jdk-21.0.9+10/Contents/Home
  ```

**Mac Path Format:**
- Uses forward slashes: `/`
- Example: `/Users/username/.java/jdk-21/Contents/Home`

### Windows-Specific Files

**Created on Windows:**
- `gradle.properties.local` - Contains Windows Java path
  ```properties
  org.gradle.java.home=C:\\Users\\yourusername\\.java\\jdk-21.0.9+10
  ```

**Windows Path Format:**
- Can use forward slashes: `C:/Users/username/.java/jdk-21`
- Or escaped backslashes: `C:\\Users\\username\\.java\\jdk-21`

### Verifying Your Setup

**Check if `gradle.properties.local` exists:**
```bash
# Mac/Windows
ls gradle.properties.local
```

**Check Java path in local file:**
```bash
# Mac
cat gradle.properties.local | grep java.home

# Windows
type gradle.properties.local | findstr java.home
```

**Test build:**
```bash
# Mac
./gradlew_mac.sh clean

# Windows
.\gradlew.bat clean
```

---

## Troubleshooting

### Build Fails with "Java home is invalid"

**On Mac:**
```bash
# Check if Java 21 exists
ls -la .java/jdk-21*/

# Update gradle.properties.local with correct path
nano gradle.properties.local

# Use gradlew_mac.sh
./gradlew_mac.sh clean
```

**On Windows:**
```powershell
# Check if Java 21 exists
Test-Path .java\jdk-21*

# Update gradle.properties.local with correct path
notepad gradle.properties.local

# Rebuild
.\gradlew.bat clean
```

### "OpenCV not found" Error

Run the appropriate setup script:
- **Mac**: `./setup_opencv.sh`
- **Windows**: `.\setup_opencv.ps1`

### Gradle Sync Fails

1. **Delete `.gradle` directory**:
   ```bash
   # Mac
   rm -rf .gradle
   
   # Windows
   Remove-Item -Recurse -Force .gradle
   ```

2. **Re-run setup script**
   ```bash
   # Mac
   ./setup_mac.sh
   
   # Windows
   .\setup_windows.ps1
   ```

3. **Sync Gradle in Android Studio**

### Mac: Still Using Java 25

Make sure you're using `./gradlew_mac.sh` instead of `./gradlew`:
```bash
# Wrong (uses system Java)
./gradlew clean

# Correct (uses Java from gradle.properties.local)
./gradlew_mac.sh clean
```

### Build Fails with "25" Error

This means Gradle is using Java 25 instead of Java 21:

**On Mac:**
- Use `./gradlew_mac.sh` instead of `./gradlew`
- Or set `JAVA_HOME` manually:
  ```bash
  export JAVA_HOME=$(cat gradle.properties.local | grep java.home | cut -d'=' -f2)
  ./gradlew clean
  ```

**On Windows:**
- Ensure `gradle.properties.local` has correct Java 21 path
- Restart terminal/Android Studio

### Path Issues on Windows

Windows paths in `gradle.properties.local` can use either:
- Forward slashes: `C:/Users/username/.java/jdk-21`
- Backslashes (escaped): `C:\\Users\\username\\.java\\jdk-21`

Gradle accepts both formats.

---

## Checklist

### First Time Setup (Mac)
- [ ] Clone repository
- [ ] Run `./setup_mac.sh`
- [ ] Run `./setup_opencv.sh`
- [ ] Test: `./gradlew_mac.sh clean`
- [ ] Open in Android Studio
- [ ] Sync Gradle
- [ ] Build project: `./gradlew_mac.sh assembleDebug`

### First Time Setup (Windows)
- [ ] Clone repository
- [ ] Run `.\setup_windows.ps1`
- [ ] Run `.\setup_opencv.ps1`
- [ ] Test: `.\gradlew.bat clean`
- [ ] Open in Android Studio
- [ ] Sync Gradle
- [ ] Build project: `.\gradlew.bat assembleDebug`

### Daily Workflow (Switching Machines)
- [ ] Pull latest changes (`git pull`)
- [ ] Verify `gradle.properties.local` exists (or run setup script)
- [ ] Open in Android Studio
- [ ] Sync Gradle
- [ ] Build and test

---

## Important Notes

### What NOT to Do ❌

1. **Don't commit `gradle.properties.local`** - It's in `.gitignore` for a reason
2. **Don't add platform-specific paths to `gradle.properties`** - Use `gradle.properties.local`
3. **Don't commit `.java/` directory** - Each machine downloads its own Java 21
4. **Don't commit `opencv-android-sdk/`** - Too large, download separately
5. **On Mac, don't use `./gradlew` directly** - Use `./gradlew_mac.sh` instead
6. **Don't edit `gradle.properties.local` manually** - Let setup scripts handle it

### What TO Do ✅

1. **Run setup script on each machine** - Creates platform-specific config
2. **Keep `gradle.properties` shared** - Only platform-independent settings
3. **Use `gradle.properties.local` for paths** - Auto-generated, not committed
4. **Download OpenCV separately** - Use setup scripts on each machine
5. **On Mac, use `./gradlew_mac.sh`** - Ensures correct Java version
6. **Commit templates and scripts** - Help other developers

---

## Additional Resources

- **Java 21 Download**: https://adoptium.net/temurin/releases/?version=21
- **OpenCV Setup**: See `OPENCV_SETUP.md` (if exists)
- **Project Guide**: See `PROJECT_IMPLEMENTATION_GUIDE.md`

---

## Summary

### Key Points

- ✅ `gradle.properties` = Shared (committed)
- ❌ `gradle.properties.local` = Personal (not committed)
- ✅ Templates and scripts = Shared (committed)
- ❌ Platform binaries = Personal (not committed)

### Result

✅ **No conflicts** when switching between Mac and Windows  
✅ **Each machine** has its own local configuration  
✅ **Shared settings** remain consistent across platforms  
✅ **Easy setup** on new machines with setup scripts

### Mac Users Remember

**Always use `./gradlew_mac.sh` instead of `./gradlew`** to ensure Java 21 is used, even if your system Java is version 25.

---

**This ensures seamless cross-platform development! 🎉**

