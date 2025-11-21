cmake_minimum_required(VERSION 3.22.1)
project("bodyscan")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# OpenCV Configuration
# OpenCV Android SDK should be placed in the project root as "opencv-android-sdk"
# Run setup_opencv.ps1 (Windows) or setup_opencv.sh (Linux/Mac) to download and set it up

# Try to find OpenCV in the project root (relative to this CMakeLists.txt)
# CMakeLists.txt is at app/src/main/jni, so go up 4 levels to project root
get_filename_component(PROJECT_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../../../.." ABSOLUTE)
set(OPENCV_SDK_PATH "${PROJECT_ROOT}/opencv-android-sdk")

# Debug: Print paths for troubleshooting
message(STATUS "CMake current source dir: ${CMAKE_CURRENT_SOURCE_DIR}")
message(STATUS "Project root calculated: ${PROJECT_ROOT}")
message(STATUS "OpenCV SDK path: ${OPENCV_SDK_PATH}")

# Also check if OpenCV_DIR is set as an environment variable or cache
if(NOT OpenCV_DIR)
    set(OpenCV_DIR "${OPENCV_SDK_PATH}" CACHE PATH "Path to OpenCV Android SDK")
endif()

# Try to find OpenCV
if(EXISTS "${OpenCV_DIR}/sdk/native/jni/OpenCVConfig.cmake")
    set(OpenCV_STATIC ON)
    include("${OpenCV_DIR}/sdk/native/jni/OpenCVConfig.cmake")
    message(STATUS "OpenCV found at ${OpenCV_DIR}")
    set(OPENCV_FOUND TRUE)
elseif(EXISTS "${OPENCV_SDK_PATH}/sdk/native/jni/OpenCVConfig.cmake")
    set(OpenCV_DIR "${OPENCV_SDK_PATH}" CACHE PATH "Path to OpenCV Android SDK" FORCE)
    set(OpenCV_STATIC ON)
    include("${OPENCV_SDK_PATH}/sdk/native/jni/OpenCVConfig.cmake")
    message(STATUS "OpenCV found at ${OPENCV_SDK_PATH}")
    set(OPENCV_FOUND TRUE)
else()
    message(WARNING "OpenCV not found at ${OpenCV_DIR} or ${OPENCV_SDK_PATH}")
    message(WARNING "Please run setup_opencv.ps1 (Windows) or setup_opencv.sh (Linux/Mac) to download OpenCV")
    message(WARNING "Or set OpenCV_DIR to the path of your OpenCV Android SDK")
    message(WARNING "Download from: https://opencv.org/releases/")
    # Define a flag to indicate OpenCV is not available
    add_definitions(-DOPENCV_NOT_AVAILABLE)
    set(OPENCV_FOUND FALSE)
endif()

# Add all C++ source files
add_library(bodyscan SHARED
    jni_bridge.cpp
    ../cpp/src/image_preprocessor.cpp
    ../cpp/src/pose_estimator.cpp
    ../cpp/src/multi_view_3d.cpp
    ../cpp/src/mesh_generator.cpp
)

target_include_directories(bodyscan PRIVATE
    ../cpp/include
)

# tinygltf is a header-only library
# tiny_gltf.h is located in ../cpp/include/
# No additional linking required

# Add OpenCV include directories if available
if(OPENCV_FOUND)
    target_include_directories(bodyscan PRIVATE
        ${OpenCV_INCLUDE_DIRS}
    )
endif()

# Link libraries
target_link_libraries(bodyscan
    log
    android
)

# Link OpenCV libraries if available
if(OPENCV_FOUND)
    target_link_libraries(bodyscan
        ${OpenCV_LIBS}
    )
else()
    message(FATAL_ERROR "OpenCV is required for this project. Please set it up using the setup script.")
endif()

# MediaPipe will be integrated via Gradle dependency (no native library needed)
# OpenPose mobile was previously considered but MediaPipe is the chosen solution

