#!/bin/bash
# Setup script to download and set up OpenCV for Android
# This script downloads OpenCV Android SDK and extracts it to the project

set -e

echo "OpenCV Android SDK Setup Script"
echo "================================"
echo ""

# Configuration
OPENCV_VERSION="4.9.0"
OPENCV_URL="https://github.com/opencv/opencv/releases/download/${OPENCV_VERSION}/opencv-${OPENCV_VERSION}-android-sdk.zip"
OPENCV_DIR="$(pwd)/opencv-android-sdk"
OPENCV_ZIP="$(pwd)/opencv-android-sdk.zip"

# Check if OpenCV is already set up
if [ -f "$OPENCV_DIR/sdk/native/jni/OpenCVConfig.cmake" ]; then
    echo "✓ OpenCV is already set up at: $OPENCV_DIR"
    echo "Skipping download..."
    exit 0
fi

# Create directory if it doesn't exist
mkdir -p "$OPENCV_DIR"

# Download OpenCV Android SDK
echo "Downloading OpenCV Android SDK ${OPENCV_VERSION}..."
echo "URL: $OPENCV_URL"
echo "This may take a few minutes..."
echo ""

if command -v curl &> /dev/null; then
    curl -L -o "$OPENCV_ZIP" "$OPENCV_URL"
elif command -v wget &> /dev/null; then
    wget -O "$OPENCV_ZIP" "$OPENCV_URL"
else
    echo "Error: Neither curl nor wget is available. Please install one of them."
    echo "Or download manually from: https://opencv.org/releases/"
    exit 1
fi

if [ ! -f "$OPENCV_ZIP" ]; then
    echo "Error: Download failed. Please download manually from: https://opencv.org/releases/"
    exit 1
fi

echo ""
echo "Download completed!"
echo ""

# Extract OpenCV
echo "Extracting OpenCV Android SDK..."
if command -v unzip &> /dev/null; then
    unzip -q "$OPENCV_ZIP" -d "$(dirname "$OPENCV_DIR")"
    # OpenCV zip extracts to opencv-{version}-android-sdk, we need to rename it
    EXTRACTED_DIR="$(dirname "$OPENCV_DIR")/opencv-${OPENCV_VERSION}-android-sdk"
    if [ -d "$EXTRACTED_DIR" ]; then
        if [ -d "$OPENCV_DIR" ] && [ "$EXTRACTED_DIR" != "$OPENCV_DIR" ]; then
            rm -rf "$OPENCV_DIR"
        fi
        mv "$EXTRACTED_DIR" "$OPENCV_DIR"
    fi
else
    echo "Error: unzip is not available. Please install unzip or extract manually."
    exit 1
fi

# Clean up zip file
rm -f "$OPENCV_ZIP"

# Verify installation
if [ -f "$OPENCV_DIR/sdk/native/jni/OpenCVConfig.cmake" ]; then
    echo ""
    echo "✓ OpenCV Android SDK setup complete!"
    echo "Location: $OPENCV_DIR"
    echo ""
    echo "The CMakeLists.txt will automatically detect OpenCV at this location."
else
    echo ""
    echo "⚠ Warning: OpenCV setup may be incomplete."
    echo "Please verify the structure: $OPENCV_DIR/sdk/native/jni/OpenCVConfig.cmake"
    exit 1
fi

