#!/bin/bash
# Bash script to download and set up OpenCV for Android
# This script downloads OpenCV Android SDK and extracts it to the project

set -e

echo "OpenCV Android SDK Setup Script"
echo "================================"

# Configuration
OPENCV_VERSION="4.9.0"
OPENCV_URL="https://github.com/opencv/opencv/releases/download/${OPENCV_VERSION}/opencv-${OPENCV_VERSION}-android-sdk.zip"
OPENCV_DIR="$(pwd)/opencv-android-sdk"
OPENCV_ZIP="$(pwd)/opencv-android-sdk.zip"

# Check if OpenCV is already set up
if [ -f "$OPENCV_DIR/sdk/native/jni/OpenCVConfig.cmake" ]; then
    echo "OpenCV is already set up at: $OPENCV_DIR"
    echo "Skipping download..."
    exit 0
fi

# Create directory if it doesn't exist
mkdir -p "$OPENCV_DIR"

# Download OpenCV Android SDK
echo "Downloading OpenCV Android SDK ${OPENCV_VERSION}..."
echo "URL: $OPENCV_URL"
echo "This may take a few minutes..."

if command -v curl &> /dev/null; then
    curl -L -o "$OPENCV_ZIP" "$OPENCV_URL" --progress-bar
elif command -v wget &> /dev/null; then
    wget -O "$OPENCV_ZIP" "$OPENCV_URL" --progress=bar
else
    echo "Error: Neither curl nor wget is available. Please install one of them."
    echo "Or download manually from: https://opencv.org/releases/"
    exit 1
fi

echo "Download completed!"

# Extract the zip file
echo "Extracting OpenCV SDK..."
unzip -q "$OPENCV_ZIP" -d "$(pwd)"

# Find the extracted folder (usually named opencv-4.x.x-android-sdk)
EXTRACTED_FOLDER=$(find . -maxdepth 1 -type d -name "opencv-*-android-sdk" | head -n 1)

if [ -n "$EXTRACTED_FOLDER" ]; then
    echo "Moving extracted folder to: $OPENCV_DIR"
    rm -rf "$OPENCV_DIR"
    mv "$EXTRACTED_FOLDER" "$OPENCV_DIR"
    echo "OpenCV SDK is now at: $OPENCV_DIR"
else
    echo "Warning: Could not find extracted OpenCV folder"
    echo "Please manually extract $OPENCV_ZIP to $OPENCV_DIR"
    exit 1
fi

# Clean up zip file
rm -f "$OPENCV_ZIP"
echo "Cleaned up download file"

# Verify setup
if [ -f "$OPENCV_DIR/sdk/native/jni/OpenCVConfig.cmake" ]; then
    echo ""
    echo "================================"
    echo "OpenCV setup completed successfully!"
    echo "Location: $OPENCV_DIR"
    echo "================================"
    echo ""
    echo "The CMakeLists.txt will automatically detect OpenCV at this location."
else
    echo "Warning: OpenCV setup verification failed"
    echo "Expected file not found: $OPENCV_DIR/sdk/native/jni/OpenCVConfig.cmake"
    echo "Please check the extraction and ensure OpenCV SDK is properly extracted."
    exit 1
fi



