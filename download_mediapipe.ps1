# MediaPipe AAR Download and Build Script for Windows
# This script attempts to download or build MediaPipe AAR files for Android

$ErrorActionPreference = "Stop"

$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$LIBS_DIR = Join-Path $SCRIPT_DIR "app\libs"
$MEDIAPIPE_DIR = Join-Path $SCRIPT_DIR "mediapipe_build"

Write-Host "MediaPipe AAR Download/Build Script" -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan
Write-Host ""

# Create libs directory if it doesn't exist
if (-not (Test-Path $LIBS_DIR)) {
    New-Item -ItemType Directory -Path $LIBS_DIR -Force | Out-Null
    Write-Host "Created libs directory: $LIBS_DIR" -ForegroundColor Green
}

# Function to check if a command exists
function Test-Command {
    param([string]$CommandName)
    $null -ne (Get-Command $CommandName -ErrorAction SilentlyContinue)
}

# Check if Bazel is installed
if (-not (Test-Command "bazel")) {
    Write-Host "⚠️  Bazel is not installed. MediaPipe requires Bazel to build AAR files." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "To install Bazel on Windows:" -ForegroundColor Yellow
    Write-Host "  1. Download from: https://github.com/bazelbuild/bazel/releases" -ForegroundColor Yellow
    Write-Host "  2. Or use Chocolatey: choco install bazel" -ForegroundColor Yellow
    Write-Host "  3. Or use Scoop: scoop install bazel" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Alternatively, you can:" -ForegroundColor Yellow
    Write-Host "  1. Download pre-built AAR files from MediaPipe community repositories" -ForegroundColor Yellow
    Write-Host "  2. Use MediaPipe's example apps that include pre-built AARs" -ForegroundColor Yellow
    Write-Host ""
    $response = Read-Host "Do you want to continue with Bazel installation check? (y/n)"
    if ($response -notmatch "^[Yy]$") {
        exit 1
    }
}

# Function to clone MediaPipe repository
function Clone-MediaPipe {
    if (Test-Path $MEDIAPIPE_DIR) {
        Write-Host "MediaPipe directory already exists. Updating..." -ForegroundColor Yellow
        Set-Location $MEDIAPIPE_DIR
        git pull
    } else {
        Write-Host "Cloning MediaPipe repository..." -ForegroundColor Cyan
        git clone https://github.com/google/mediapipe.git $MEDIAPIPE_DIR
        Set-Location $MEDIAPIPE_DIR
    }
}

# Function to build pose detection AAR
function Build-PoseAAR {
    Write-Host ""
    Write-Host "Building MediaPipe Pose Detection AAR..." -ForegroundColor Cyan
    Write-Host "This may take 10-30 minutes depending on your system..." -ForegroundColor Yellow
    Write-Host ""
    
    Set-Location $MEDIAPIPE_DIR
    
    # Create BUILD file for pose detection AAR
    $BUILD_DIR = Join-Path $MEDIAPIPE_DIR "mediapipe\examples\android\src\java\com\google\mediapipe\apps\pose_detection_aar"
    $BUILD_FILE = Join-Path $BUILD_DIR "BUILD"
    
    if (-not (Test-Path $BUILD_DIR)) {
        New-Item -ItemType Directory -Path $BUILD_DIR -Force | Out-Null
    }
    
    $BUILD_CONTENT = @"
load("//mediapipe/java/com/google/mediapipe:mediapipe_aar.bzl", "mediapipe_aar")

mediapipe_aar(
    name = "mediapipe_pose",
    calculators = [
        "//mediapipe/graphs/pose_tracking:pose_tracking_gpu",
    ],
)
"@
    
    Set-Content -Path $BUILD_FILE -Value $BUILD_CONTENT
    Write-Host "Created BUILD file: $BUILD_FILE" -ForegroundColor Green
    
    Write-Host "Building AAR file..." -ForegroundColor Cyan
    $buildOutput = bazel build -c opt --strip=ALWAYS `
        --host_crosstool_top=@bazel_tools//tools/cpp:toolchain `
        --fat_apk_cpu=arm64-v8a,armeabi-v7a `
        //mediapipe/examples/android/src/java/com/google/mediapipe/apps/pose_detection_aar:mediapipe_pose.aar 2>&1
    
    $aarPath = Join-Path $MEDIAPIPE_DIR "bazel-bin\mediapipe\examples\android\src\java\com\google\mediapipe\apps\pose_detection_aar\mediapipe_pose.aar"
    
    if (Test-Path $aarPath) {
        Write-Host "✅ AAR built successfully!" -ForegroundColor Green
        $destPath = Join-Path $LIBS_DIR "mediapipe_pose.aar"
        Copy-Item -Path $aarPath -Destination $destPath -Force
        Write-Host "✅ Copied to $destPath" -ForegroundColor Green
        return $true
    } else {
        Write-Host "❌ AAR build failed" -ForegroundColor Red
        Write-Host "Build output:" -ForegroundColor Yellow
        Write-Host $buildOutput
        return $false
    }
}

# Function to build solution_base AAR
function Build-SolutionBaseAAR {
    Write-Host ""
    Write-Host "Building MediaPipe Solution Base AAR..." -ForegroundColor Cyan
    Write-Host "This may take 10-30 minutes depending on your system..." -ForegroundColor Yellow
    Write-Host ""
    
    Set-Location $MEDIAPIPE_DIR
    
    # Try to find existing solution_base BUILD file
    $buildFile = Join-Path $MEDIAPIPE_DIR "mediapipe\java\com\google\mediapipe\BUILD"
    if (Test-Path $buildFile) {
        Write-Host "Building solution_base AAR..." -ForegroundColor Cyan
        $buildOutput = bazel build -c opt --strip=ALWAYS `
            --host_crosstool_top=@bazel_tools//tools/cpp:toolchain `
            --fat_apk_cpu=arm64-v8a,armeabi-v7a `
            //mediapipe/java/com/google/mediapipe:solution_base.aar 2>&1
        
        $aarPath = Join-Path $MEDIAPIPE_DIR "bazel-bin\mediapipe\java\com\google\mediapipe\solution_base.aar"
        
        if (Test-Path $aarPath) {
            Write-Host "✅ Solution base AAR built successfully!" -ForegroundColor Green
            $destPath = Join-Path $LIBS_DIR "solution_base.aar"
            Copy-Item -Path $aarPath -Destination $destPath -Force
            Write-Host "✅ Copied to $destPath" -ForegroundColor Green
            return $true
        }
    }
    
    Write-Host "⚠️  Could not find solution_base BUILD target" -ForegroundColor Yellow
    return $false
}

# Main execution
Write-Host "Step 1: Checking prerequisites..." -ForegroundColor Cyan

if (Test-Command "bazel") {
    $bazelVersion = bazel version 2>&1 | Select-Object -First 1
    Write-Host "✅ Bazel is installed: $bazelVersion" -ForegroundColor Green
} else {
    Write-Host "❌ Bazel is not installed" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please install Bazel first, then run this script again." -ForegroundColor Yellow
    Write-Host "For Windows:" -ForegroundColor Yellow
    Write-Host "  - Download from: https://github.com/bazelbuild/bazel/releases" -ForegroundColor Yellow
    Write-Host "  - Or use Chocolatey: choco install bazel" -ForegroundColor Yellow
    Write-Host "  - Or use Scoop: scoop install bazel" -ForegroundColor Yellow
    exit 1
}

if (Test-Command "git") {
    Write-Host "✅ Git is installed" -ForegroundColor Green
} else {
    Write-Host "❌ Git is not installed" -ForegroundColor Red
    Write-Host "Please install Git from: https://git-scm.com/download/win" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "Step 2: Setting up MediaPipe repository..." -ForegroundColor Cyan
Clone-MediaPipe

Write-Host ""
Write-Host "Step 3: Building AAR files..." -ForegroundColor Cyan
Write-Host "Note: This process requires significant time and resources." -ForegroundColor Yellow
Write-Host ""

# Try to build pose AAR
if (Build-PoseAAR) {
    Write-Host ""
    Write-Host "✅ Pose detection AAR ready!" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "⚠️  Pose AAR build encountered issues" -ForegroundColor Yellow
    Write-Host "You may need to check the MediaPipe documentation for the correct BUILD target" -ForegroundColor Yellow
}

# Try to build solution_base AAR (optional)
if (Build-SolutionBaseAAR) {
    Write-Host ""
    Write-Host "✅ Solution base AAR ready!" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "ℹ️  Solution base AAR not built (may not be required)" -ForegroundColor Cyan
}

Write-Host ""
Write-Host "====================================" -ForegroundColor Cyan
Write-Host "Build process complete!" -ForegroundColor Green
Write-Host ""
Write-Host "AAR files are in: $LIBS_DIR" -ForegroundColor Cyan
$aarFiles = Get-ChildItem -Path $LIBS_DIR -Filter "*.aar" -ErrorAction SilentlyContinue
if ($aarFiles) {
    $aarFiles | ForEach-Object {
        $size = [math]::Round($_.Length / 1MB, 2)
        Write-Host "  - $($_.Name) ($size MB)" -ForegroundColor Green
    }
} else {
    Write-Host "No AAR files found" -ForegroundColor Yellow
}
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "1. Update app/build.gradle.kts to include the AAR files" -ForegroundColor White
Write-Host "2. Sync Gradle in Android Studio" -ForegroundColor White
Write-Host "3. Build and test your app" -ForegroundColor White

