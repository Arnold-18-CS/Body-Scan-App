#!/bin/bash

# MediaPipe AAR Download and Build Script
# This script attempts to download or build MediaPipe AAR files for Android

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIBS_DIR="${SCRIPT_DIR}/app/libs"
MEDIAPIPE_DIR="${SCRIPT_DIR}/mediapipe_build"

echo "MediaPipe AAR Download/Build Script"
echo "===================================="
echo ""

# Create libs directory if it doesn't exist
mkdir -p "${LIBS_DIR}"

# Check if Bazel is installed
if ! command -v bazel &> /dev/null; then
    echo "⚠️  Bazel is not installed. MediaPipe requires Bazel to build AAR files."
    echo ""
    echo "To install Bazel:"
    echo "  macOS: brew install bazel"
    echo "  Linux: Follow instructions at https://bazel.build/install"
    echo ""
    echo "Alternatively, you can:"
    echo "  1. Download pre-built AAR files from MediaPipe community repositories"
    echo "  2. Use MediaPipe's example apps that include pre-built AARs"
    echo ""
    read -p "Do you want to continue with Bazel installation check? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Function to clone MediaPipe repository
clone_mediapipe() {
    if [ -d "${MEDIAPIPE_DIR}" ]; then
        echo "MediaPipe directory already exists. Updating..."
        cd "${MEDIAPIPE_DIR}"
        git pull
    else
        echo "Cloning MediaPipe repository..."
        git clone https://github.com/google/mediapipe.git "${MEDIAPIPE_DIR}"
        cd "${MEDIAPIPE_DIR}"
    fi
}

# Function to build pose detection AAR
build_pose_aar() {
    echo ""
    echo "Building MediaPipe Pose Detection AAR..."
    echo "This may take 10-30 minutes depending on your system..."
    echo ""
    
    cd "${MEDIAPIPE_DIR}"
    
    # Create BUILD file for pose detection AAR
    BUILD_FILE="${MEDIAPIPE_DIR}/mediapipe/examples/android/src/java/com/google/mediapipe/apps/pose_detection_aar/BUILD"
    mkdir -p "$(dirname "${BUILD_FILE}")"
    
    cat > "${BUILD_FILE}" << 'EOF'
load("//mediapipe/java/com/google/mediapipe:mediapipe_aar.bzl", "mediapipe_aar")

mediapipe_aar(
    name = "mediapipe_pose",
    calculators = [
        "//mediapipe/graphs/pose_tracking:pose_tracking_gpu",
    ],
)
EOF

    echo "Building AAR file..."
    bazel build -c opt --strip=ALWAYS \
        --host_crosstool_top=@bazel_tools//tools/cpp:toolchain \
        --fat_apk_cpu=arm64-v8a,armeabi-v7a \
        //mediapipe/examples/android/src/java/com/google/mediapipe/apps/pose_detection_aar:mediapipe_pose.aar
    
    if [ -f "bazel-bin/mediapipe/examples/android/src/java/com/google/mediapipe/apps/pose_detection_aar/mediapipe_pose.aar" ]; then
        echo "✅ AAR built successfully!"
        cp bazel-bin/mediapipe/examples/android/src/java/com/google/mediapipe/apps/pose_detection_aar/mediapipe_pose.aar \
           "${LIBS_DIR}/mediapipe_pose.aar"
        echo "✅ Copied to ${LIBS_DIR}/mediapipe_pose.aar"
        return 0
    else
        echo "❌ AAR build failed"
        return 1
    fi
}

# Function to build solution_base AAR
build_solution_base_aar() {
    echo ""
    echo "Building MediaPipe Solution Base AAR..."
    echo "This may take 10-30 minutes depending on your system..."
    echo ""
    
    cd "${MEDIAPIPE_DIR}"
    
    # Try to find existing solution_base BUILD file
    if [ -f "mediapipe/java/com/google/mediapipe/BUILD" ]; then
        echo "Building solution_base AAR..."
        bazel build -c opt --strip=ALWAYS \
            --host_crosstool_top=@bazel_tools//tools/cpp:toolchain \
            --fat_apk_cpu=arm64-v8a,armeabi-v7a \
            //mediapipe/java/com/google/mediapipe:solution_base.aar
        
        if [ -f "bazel-bin/mediapipe/java/com/google/mediapipe/solution_base.aar" ]; then
            echo "✅ Solution base AAR built successfully!"
            cp bazel-bin/mediapipe/java/com/google/mediapipe/solution_base.aar \
               "${LIBS_DIR}/solution_base.aar"
            echo "✅ Copied to ${LIBS_DIR}/solution_base.aar"
            return 0
        fi
    fi
    
    echo "⚠️  Could not find solution_base BUILD target"
    return 1
}

# Main execution
echo "Step 1: Checking prerequisites..."
if command -v bazel &> /dev/null; then
    echo "✅ Bazel is installed: $(bazel version | head -1)"
else
    echo "❌ Bazel is not installed"
    echo ""
    echo "Please install Bazel first, then run this script again."
    echo "For macOS: brew install bazel"
    exit 1
fi

if command -v git &> /dev/null; then
    echo "✅ Git is installed"
else
    echo "❌ Git is not installed"
    exit 1
fi

echo ""
echo "Step 2: Setting up MediaPipe repository..."
clone_mediapipe

echo ""
echo "Step 3: Building AAR files..."
echo "Note: This process requires significant time and resources."
echo ""

# Try to build pose AAR
if build_pose_aar; then
    echo ""
    echo "✅ Pose detection AAR ready!"
else
    echo ""
    echo "⚠️  Pose AAR build encountered issues"
    echo "You may need to check the MediaPipe documentation for the correct BUILD target"
fi

# Try to build solution_base AAR (optional)
if build_solution_base_aar; then
    echo ""
    echo "✅ Solution base AAR ready!"
else
    echo ""
    echo "ℹ️  Solution base AAR not built (may not be required)"
fi

echo ""
echo "===================================="
echo "Build process complete!"
echo ""
echo "AAR files are in: ${LIBS_DIR}"
ls -lh "${LIBS_DIR}"/*.aar 2>/dev/null || echo "No AAR files found"
echo ""
echo "Next steps:"
echo "1. Update app/build.gradle.kts to include the AAR files"
echo "2. Sync Gradle in Android Studio"
echo "3. Build and test your app"

