cmake_minimum_required(VERSION 3.22.1)
project("bodyscan")

# Basic CMake configuration - OpenCV and OpenPose can be added later
# For now, we'll create a minimal build that compiles

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

# Link Android and logging libraries
target_link_libraries(bodyscan
    log
    android
)

# OpenCV and OpenPose mobile will be added in Phase 2
# find_package(OpenCV REQUIRED CONFIG)
# add_subdirectory(openpose_mobile)
# target_include_directories(bodyscan PRIVATE ${OpenCV_INCLUDE_DIRS})
# target_link_libraries(bodyscan ${OpenCV_LIBS} openpose_mobile)

