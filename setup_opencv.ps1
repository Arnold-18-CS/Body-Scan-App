# PowerShell script to download and set up OpenCV for Android
# This script downloads OpenCV Android SDK and extracts it to the project

$ErrorActionPreference = "Stop"

Write-Host "OpenCV Android SDK Setup Script" -ForegroundColor Green
Write-Host "=================================" -ForegroundColor Green

# Configuration
$OPENCV_VERSION = "4.9.0"
$OPENCV_URL = "https://github.com/opencv/opencv/releases/download/${OPENCV_VERSION}/opencv-${OPENCV_VERSION}-android-sdk.zip"
$OPENCV_DIR = "$PSScriptRoot\opencv-android-sdk"
$OPENCV_ZIP = "$PSScriptRoot\opencv-android-sdk.zip"

# Check if OpenCV is already set up
if (Test-Path "$OPENCV_DIR\sdk\native\jni\OpenCVConfig.cmake") {
    Write-Host "OpenCV is already set up at: $OPENCV_DIR" -ForegroundColor Yellow
    Write-Host "Skipping download..." -ForegroundColor Yellow
    exit 0
}

# Create directory if it doesn't exist
if (-not (Test-Path $OPENCV_DIR)) {
    New-Item -ItemType Directory -Path $OPENCV_DIR -Force | Out-Null
}

# Download OpenCV Android SDK
Write-Host "Downloading OpenCV Android SDK ${OPENCV_VERSION}..." -ForegroundColor Cyan
Write-Host "URL: $OPENCV_URL" -ForegroundColor Gray
Write-Host "This may take a few minutes..." -ForegroundColor Yellow

try {
    # Use Invoke-WebRequest with progress
    $ProgressPreference = 'Continue'
    Invoke-WebRequest -Uri $OPENCV_URL -OutFile $OPENCV_ZIP -UseBasicParsing
    Write-Host "Download completed!" -ForegroundColor Green
} catch {
    Write-Host "Error downloading OpenCV: $_" -ForegroundColor Red
    Write-Host "Please download manually from: https://opencv.org/releases/" -ForegroundColor Yellow
    Write-Host "Extract to: $OPENCV_DIR" -ForegroundColor Yellow
    exit 1
}

# Extract the zip file
Write-Host "Extracting OpenCV SDK..." -ForegroundColor Cyan
try {
    Expand-Archive -Path $OPENCV_ZIP -DestinationPath $PSScriptRoot -Force
    Write-Host "Extraction completed!" -ForegroundColor Green
    
    # Find the extracted folder (usually named opencv-4.x.x-android-sdk)
    $extractedFolders = Get-ChildItem -Path $PSScriptRoot -Directory -Filter "opencv-*-android-sdk"
    if ($extractedFolders.Count -gt 0) {
        $extractedFolder = $extractedFolders[0].FullName
        Write-Host "Moving extracted folder to: $OPENCV_DIR" -ForegroundColor Cyan
        
        # Move contents to OPENCV_DIR
        if (Test-Path $OPENCV_DIR) {
            Remove-Item $OPENCV_DIR -Recurse -Force
        }
        Move-Item $extractedFolder $OPENCV_DIR -Force
        Write-Host "OpenCV SDK is now at: $OPENCV_DIR" -ForegroundColor Green
    } else {
        Write-Host "Warning: Could not find extracted OpenCV folder" -ForegroundColor Yellow
        Write-Host "Please manually extract $OPENCV_ZIP to $OPENCV_DIR" -ForegroundColor Yellow
    }
    
    # Clean up zip file
    Remove-Item $OPENCV_ZIP -Force
    Write-Host "Cleaned up download file" -ForegroundColor Green
    
} catch {
    Write-Host "Error extracting OpenCV: $_" -ForegroundColor Red
    Write-Host "Please manually extract $OPENCV_ZIP to $OPENCV_DIR" -ForegroundColor Yellow
    exit 1
}

# Verify setup
if (Test-Path "$OPENCV_DIR\sdk\native\jni\OpenCVConfig.cmake") {
    Write-Host "" -ForegroundColor Green
    Write-Host "=================================" -ForegroundColor Green
    Write-Host "OpenCV setup completed successfully!" -ForegroundColor Green
    Write-Host "Location: $OPENCV_DIR" -ForegroundColor Green
    Write-Host "=================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "The CMakeLists.txt will automatically detect OpenCV at this location." -ForegroundColor Cyan
} else {
    Write-Host "Warning: OpenCV setup verification failed" -ForegroundColor Yellow
    Write-Host "Expected file not found: $OPENCV_DIR\sdk\native\jni\OpenCVConfig.cmake" -ForegroundColor Yellow
    Write-Host "Please check the extraction and ensure OpenCV SDK is properly extracted." -ForegroundColor Yellow
    exit 1
}



