# Body Scan App - Complete Implementation Guide

This document serves as the **master reference** for implementing the multi-photo 3D body reconstruction feature with C++ NDK integration, OpenPose keypoint detection, 3D mesh generation, and measurement calculations.

---

## 1. HIGH-LEVEL VISION

Create a single-module Android app (Kotlin + Jetpack Compose) that:

• Captures **3 user photos** (front, left-side, right-side) with on-screen framing guides.

• Sends the 3 images to a **C++ NDK library** (OpenPose + 3D reconstruction).

• Returns **135 2D keypoints per view** → **135 3D keypoints** + a **lightweight 3D mesh** (SMPL-X style or simple cylinder/ellipsoid).

• Overlays the **detected 2D keypoints** on each captured photo.

• Shows the **3D reconstruction** (rotatable mesh + keypoints) in a dedicated result screen.

• Computes **circumferences** (waist, chest, hips, thighs…) from the 3D points and displays them.

• Stores everything locally (Room) and allows export (JSON/CSV/PDF).

**Performance Requirements:**
- All processing runs **on-device**
- Total processing time: **<5 seconds**
- Memory usage: **<100 MB RAM**

---

## 2. PROJECT STRUCTURE (Exact Paths)

```
app/
 └─ src/
     └─ main/
         ├─ java/com/example/bodyscanapp/
         │   ├─ ui/
         │   │   ├─ screens/          
         │   │   │   ├─ LoginSelectionScreen.kt (existing)
         │   │   │   ├─ HomeScreen.kt (existing)
         │   │   │   ├─ CaptureSequenceScreen.kt (NEW - replaces ImageCaptureScreen)
         │   │   │   ├─ ProcessingScreen.kt (existing - update)
         │   │   │   ├─ Result3DScreen.kt (NEW - replaces ResultsScreen)
         │   │   │   ├─ HistoryScreen.kt (NEW)
         │   │   │   └─ ProfileScreen.kt (NEW)
         │   │   └─ components/       
         │   │       ├─ KeypointOverlay.kt (NEW)
         │   │       ├─ FilamentMeshViewer.kt (NEW)
         │   │       └─ FramingOverlay.kt (NEW)
         │   ├─ data/
         │   │   ├─ entity/           
         │   │   │   ├─ User.kt (NEW)
         │   │   │   ├─ Scan.kt (NEW)
         │   │   │   └─ Measurement.kt (NEW)
         │   │   └─ dao/
         │   │       ├─ UserDao.kt (NEW)
         │   │       ├─ ScanDao.kt (NEW)
         │   │       └─ MeasurementDao.kt (NEW)
         │   ├─ repository/
         │   │   ├─ ScanRepository.kt (NEW)
         │   │   └─ UserRepository.kt (NEW)
         │   ├─ utils/
         │   │   ├─ NativeBridge.kt (NEW)
         │   │   └─ ExportHelper.kt (NEW)
         │   └─ MainActivity.kt (existing - update)
         ├─ cpp/                     (C++ source – will be moved to jni later)
         │   ├─ include/
         │   │   ├─ image_preprocessor.h
         │   │   ├─ pose_estimator.h
         │   │   ├─ multi_view_3d.h
         │   │   └─ mesh_generator.h
         │   └─ src/
         │       ├─ image_preprocessor.cpp
         │       ├─ pose_estimator.cpp
         │       ├─ multi_view_3d.cpp
         │       └─ mesh_generator.cpp
         ├─ jni/
         │   ├─ CMakeLists.txt
         │   ├─ jni_bridge.cpp
         │   └─ openpose_mobile/ (subdirectory - clone from repo)
         ├─ assets/
         │   └─ models/
         │       ├─ openpose_body25.tflite
         │       └─ smpl_x_params.bin   (optional for SMPL-X fitting)
         └─ res/
             └─ drawable/ (framing overlays, icons)
```

---

## 3. BUILD CONFIG (app/build.gradle.kts)

### Required Updates:

```kotlin
plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")  // ADD for Room
}

android {
    compileSdk = 35  // Update if needed
    defaultConfig {
        applicationId = "com.example.bodyscanapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        ndkVersion = "26.1.10909125"  // ADD
    }

    // ADD externalNativeBuild block
    externalNativeBuild {
        cmake {
            path = file("src/main/jni/CMakeLists.txt")
        }
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    // Existing dependencies...
    
    // ADD these new dependencies:
    
    // Room
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Coroutines (if not already present)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // 3D rendering – Filament (lightweight)
    implementation("com.google.android.filament:filament-android:1.41.0")
    implementation("com.google.android.filament:filament-utils-android:1.41.0")

    // Export libraries
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.itextpdf:itext7-core:8.0.5")

    // Encrypted prefs (if not already present)
    implementation("androidx.security:security-crypto:1.1.0")
}
```

---

## 4. CMAKE CONFIGURATION (src/main/jni/CMakeLists.txt)

**CREATE THIS FILE:**

```cmake
cmake_minimum_required(VERSION 3.22.1)
project("bodyscan")

# OpenCV (NDK package)
find_package(OpenCV REQUIRED CONFIG)

# OpenPose mobile (copy source from https://github.com/lujintaozju/OpenPoseForMobile)
add_subdirectory(openpose_mobile)

add_library(bodyscan SHARED
    jni_bridge.cpp
    ../cpp/src/image_preprocessor.cpp
    ../cpp/src/pose_estimator.cpp
    ../cpp/src/multi_view_3d.cpp
    ../cpp/src/mesh_generator.cpp
)

target_include_directories(bodyscan PRIVATE
    ../cpp/include
    ${OpenCV_INCLUDE_DIRS}
)

target_link_libraries(bodyscan
    ${OpenCV_LIBS}
    openpose_mobile
    log
    android
)
```

**TASKS:**
- Create the `openpose_mobile` folder by cloning the repo inside `jni/`
- Add the `add_subdirectory` line
- Ensure OpenCV NDK package is available

---

## 5. JNI BRIDGE (jni/jni_bridge.cpp)

**CREATE THIS FILE:**

```cpp
#include <jni.h>
#include "image_preprocessor.h"
#include "pose_estimator.h"
#include "multi_view_3d.h"
#include "mesh_generator.h"
#include <vector>

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_example_bodyscanapp_utils_NativeBridge_processThreeImages(
        JNIEnv* env, jclass, jobjectArray jImages, jintArray jWidths,
        jintArray jHeights, jfloat userHeight) {

    // 1. Convert Java byte[][] → std::vector<cv::Mat>
    std::vector<cv::Mat> imgs(3);
    jint widths[3], heights[3];
    env->GetIntArrayRegion(jWidths, 0, 3, widths);
    env->GetIntArrayRegion(jHeights, 0, 3, heights);

    for (int i = 0; i < 3; ++i) {
        jbyteArray jImg = (jbyteArray)env->GetObjectArrayElement(jImages, i);
        jsize len = env->GetArrayLength(jImg);
        jbyte* buf = env->GetByteArrayElements(jImg, nullptr);
        cv::Mat rgba(heights[i], widths[i], CV_8UC4, buf);
        cv::cvtColor(rgba, imgs[i], cv::COLOR_RGBA2RGB);
        env->ReleaseByteArrayElements(jImg, buf, JNI_ABORT);
    }

    // 2. Pre-process (CLAHE + bg removal)
    for (auto& img : imgs) ImagePreprocessor::run(img);

    // 3. 2D keypoints per view
    std::vector<std::vector<cv::Point2f>> kpts2d(3);
    for (int i = 0; i < 3; ++i) kpts2d[i] = PoseEstimator::detect(imgs[i]);

    // 4. 3D triangulation
    auto kpts3d = MultiView3D::triangulate(kpts2d, userHeight);

    // 5. Simple mesh (ellipsoid per segment)
    auto mesh = MeshGenerator::createFromKeypoints(kpts3d);

    // 6. Compute measurements
    auto measurements = computeCircumferences(kpts3d);

    // 7. Pack results:
    //   - float[] keypoints3d (135*3)
    //   - byte[] glb mesh (Filament can load GLB)
    //   - float[] measurements (waist, chest, …)
    // Return a Java Object[] { float[] kpts, byte[] glb, float[] meas }
    
    // TODO: Implement packing logic (float arrays + ByteBuffer for GLB)
}
```

**TASKS:**
- Flesh out the packing logic (float arrays + ByteBuffer for GLB)
- Implement proper JNI object creation and return

---

## 6. KOTLIN NATIVE BRIDGE (utils/NativeBridge.kt)

**CREATE THIS FILE:**

```kotlin
package com.example.bodyscanapp.utils

object NativeBridge {
    init { 
        System.loadLibrary("bodyscan") 
    }

    data class ScanResult(
        val keypoints3d: FloatArray,   // 135*3
        val meshGlb: ByteArray,        // GLB binary
        val measurements: FloatArray   // e.g. waist, chest, hips, …
    )

    external fun processThreeImages(
        images: Array<ByteArray>,
        widths: IntArray,
        heights: IntArray,
        userHeightCm: Float
    ): ScanResult
}
```

---

## 7. UI FLOW (New Screens)

### Screen Implementation Table

| Screen | Composable | Key Tasks |
|--------|------------|-----------|
| **CaptureSequenceScreen** | `CaptureSequenceScreen.kt` | 1. Height input → store in ViewModel<br>2. Loop: Front → Left → Right (CameraX preview + **FramingOverlay**)<br>3. Save each `ImageProxy` → `ByteArray` |
| **ProcessingScreen** | `ProcessingScreen.kt` | Show animated progress, call `NativeBridge.processThreeImages` on `Dispatchers.IO` |
| **Result3DScreen** | `Result3DScreen.kt` | • **Top half**: 3 captured photos with **KeypointOverlay** (draw circles/lines)<br>• **Bottom half**: **FilamentMeshViewer** (load GLB, orbit controls)<br>• List of measurements<br>• Save / Export buttons |

### Navigation Updates

Update `BodyScanNavigation.kt` to include:
- `CaptureSequenceScreen` route
- `Result3DScreen` route
- Remove or update old `ImageCaptureScreen` and `ResultsScreen`

---

## 8. KEYPOINT OVERLAY (components/KeypointOverlay.kt)

**CREATE THIS FILE:**

```kotlin
package com.example.bodyscanapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawImage
import androidx.compose.ui.graphics.toComposeImageBitmap

@Composable
fun KeypointOverlay(
    imageBitmap: ImageBitmap,
    keypoints2d: List<Pair<Float, Float>>, // normalized 0..1
    modifier: Modifier = Modifier
) {
    Canvas(modifier.fillMaxSize()) {
        drawImage(imageBitmap, dstSize = size)
        val paint = Paint().apply { 
            color = Color.Red
            strokeWidth = 6f 
        }
        keypoints2d.forEach { (nx, ny) ->
            drawCircle(
                center = Offset(nx * size.width, ny * size.height), 
                radius = 8f, 
                paint = paint
            )
        }
        // TODO: optional - draw skeleton lines (BODY_25 connections)
    }
}
```

---

## 9. 3D MESH VIEWER (components/FilamentMeshViewer.kt)

**CREATE THIS FILE:**

```kotlin
package com.example.bodyscanapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun FilamentMeshViewer(glbBytes: ByteArray, modifier: Modifier) {
    AndroidView(
        factory = { ctx ->
            FilamentView(ctx).apply {
                // load GLB from ByteArray
                val asset = AssetLoader.loadGlbFromBytes(ctx, glbBytes)
                scene.addEntity(asset.root)
                // enable orbit controller
                setupOrbitController()
            }
        },
        modifier = modifier
    )
}
```

**TASKS:**
- Generate `FilamentView` wrapper that implements `AssetLoader.loadGlbFromBytes`
- Implement orbit controller for rotation/zoom

---

## 10. MESH GENERATION (C++ – cpp/src/mesh_generator.cpp)

**CREATE THIS FILE:**

```cpp
#include "mesh_generator.h"
#include <vector>
#include <cstdint>

// Simple ellipsoid per body segment (torso, pelvis, thighs…)
std::vector<uint8_t> MeshGenerator::createFromKeypoints(
    const std::vector<cv::Point3f>& kpts3d) {
    
    // 1. Identify segment endpoints (shoulder-hip, hip-knee, etc.)
    // 2. For each segment create a cylinder/ellipsoid
    // 3. Merge into one glTF/GLB using tinygltf
    // 4. Return serialized bytes
    
    // TODO: Implement mesh generation logic
}
```

**TASKS:**
- Use **tinygltf** (header-only) – add to CMake
- Implement segment identification
- Generate ellipsoid/cylinder meshes per body segment
- Serialize to GLB format

---

## 11. MEASUREMENT CALCULATION (C++ → Kotlin)

**ADD TO cpp/src/mesh_generator.cpp or separate file:**

```cpp
std::vector<float> computeCircumferences(const std::vector<cv::Point3f>& kpts3d) {
    // waist: fit ellipse to torso points → π * avg_radius*2
    // chest, hips, thighs similarly
    
    std::vector<float> measurements;
    
    // TODO: Implement circumference calculations
    // - Waist circumference
    // - Chest circumference
    // - Hip circumference
    // - Thigh circumference
    // - etc.
    
    return measurements;
}
```

Return inside `ScanResult.measurements` from JNI bridge.

---

## 12. DATABASE (Room)

### Entity: Scan.kt

**CREATE app/src/main/java/com/example/bodyscanapp/data/entity/Scan.kt:**

```kotlin
package com.example.bodyscanapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Scan(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    val userId: Long,
    val timestamp: Long,
    val heightCm: Float,
    val keypoints3d: String,   // JSON
    val meshPath: String,      // internal file path
    val measurementsJson: String
)
```

### Entity: User.kt

**CREATE app/src/main/java/com/example/bodyscanapp/data/entity/User.kt:**

```kotlin
package com.example.bodyscanapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val firebaseUid: String,
    val username: String,
    val email: String?,
    val createdAt: Long
)
```

### DAO: ScanDao.kt

**CREATE app/src/main/java/com/example/bodyscanapp/data/dao/ScanDao.kt:**

```kotlin
package com.example.bodyscanapp.data.dao

import androidx.room.*
import com.example.bodyscanapp.data.entity.Scan
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM Scan WHERE userId = :userId ORDER BY timestamp DESC")
    fun getScansByUser(userId: Long): Flow<List<Scan>>
    
    @Insert
    suspend fun insertScan(scan: Scan): Long
    
    @Delete
    suspend fun deleteScan(scan: Scan)
    
    @Query("SELECT * FROM Scan WHERE id = :scanId")
    suspend fun getScanById(scanId: Long): Scan?
}
```

### Database: AppDatabase.kt

**CREATE app/src/main/java/com/example/bodyscanapp/data/AppDatabase.kt:**

```kotlin
package com.example.bodyscanapp.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.bodyscanapp.data.dao.ScanDao
import com.example.bodyscanapp.data.dao.UserDao
import com.example.bodyscanapp.data.entity.Scan
import com.example.bodyscanapp.data.entity.User

@Database(
    entities = [User::class, Scan::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun scanDao(): ScanDao
}
```

---

## 13. EXPORT (utils/ExportHelper.kt)

**CREATE THIS FILE:**

```kotlin
package com.example.bodyscanapp.utils

import com.google.gson.Gson
import java.io.File
import java.io.FileWriter

object ExportHelper {
    private val gson = Gson()
    
    // JSON export
    fun exportToJson(scan: Scan, outputFile: File) {
        val json = gson.toJson(scan)
        FileWriter(outputFile).use { it.write(json) }
    }
    
    // CSV export
    fun exportToCsv(measurements: Map<String, Float>, outputFile: File) {
        // TODO: Implement CSV export
    }
    
    // PDF export
    fun exportToPdf(scan: Scan, images: List<Bitmap>, outputFile: File) {
        // TODO: Implement PDF export using iText
        // Include: table of measurements, embedded images, 3D screenshot
    }
}
```

---

## 14. C++ HEADER FILES

### cpp/include/image_preprocessor.h

```cpp
#ifndef IMAGE_PREPROCESSOR_H
#define IMAGE_PREPROCESSOR_H

#include <opencv2/opencv.hpp>

class ImagePreprocessor {
public:
    static void run(cv::Mat& img);
};

#endif
```

### cpp/include/pose_estimator.h

```cpp
#ifndef POSE_ESTIMATOR_H
#define POSE_ESTIMATOR_H

#include <opencv2/opencv.hpp>
#include <vector>

class PoseEstimator {
public:
    static std::vector<cv::Point2f> detect(const cv::Mat& img);
};

#endif
```

### cpp/include/multi_view_3d.h

```cpp
#ifndef MULTI_VIEW_3D_H
#define MULTI_VIEW_3D_H

#include <opencv2/opencv.hpp>
#include <vector>

class MultiView3D {
public:
    static std::vector<cv::Point3f> triangulate(
        const std::vector<std::vector<cv::Point2f>>& kpts2d,
        float userHeight
    );
};

#endif
```

### cpp/include/mesh_generator.h

```cpp
#ifndef MESH_GENERATOR_H
#define MESH_GENERATOR_H

#include <opencv2/opencv.hpp>
#include <vector>
#include <cstdint>

class MeshGenerator {
public:
    static std::vector<uint8_t> createFromKeypoints(
        const std::vector<cv::Point3f>& kpts3d
    );
};

#endif
```

---

## 15. TESTING & PROFILING

### Unit Tests (Kotlin)

**CREATE test files:**

```kotlin
// Example: app/src/test/java/com/example/bodyscanapp/utils/NativeBridgeTest.kt
@Test 
fun `scaling correct`() { 
    // Test measurement scaling
}
```

### GoogleTest (C++)

**CREATE test files:**

```cpp
// Example: cpp/test/pose_estimator_test.cpp
TEST(Pose, MultiView) { 
    // Test multi-view pose estimation
}
```

### Performance Targets

- **Total processing time**: <5 seconds
- **Memory usage**: <100 MB RAM
- **Accuracy**: MAE < 1 cm for measurements

**Profiling:**
- Use Android Profiler
- Test on 3 mid-range devices
- Monitor CPU, Memory, and GPU usage

---

## 16. FINAL CHECK-LIST

```
[ ] Add CMake + OpenPose mobile sub-dir
[ ] Implement jni_bridge.cpp with 3-image pipeline
[ ] Create NativeBridge.kt + ScanResult data class
[ ] Add CaptureSequenceScreen (3 photos with framing guides)
[ ] Add KeypointOverlay composable
[ ] Add FilamentMeshViewer composable
[ ] Implement mesh_generator.cpp → GLB
[ ] Compute circumferences in C++
[ ] Room entities + DAO + Database
[ ] Export JSON/CSV/PDF
[ ] Update UI wireframes (Result3DScreen)
[ ] Write unit / integration tests
[ ] Profile on 3 mid-range devices
[ ] Update navigation graph
[ ] Implement FramingOverlay component
[ ] Add HistoryScreen and ProfileScreen
[ ] Implement ScanRepository
[ ] Wire up all screens in navigation
```

---

## 17. KEY IMPLEMENTATION NOTES

### Multi-Photo Capture Flow

1. **Height Input**: User enters height (cm) → stored in ViewModel
2. **Front Photo**: CameraX preview with framing overlay → capture → save to ByteArray
3. **Left Photo**: Same process, guide user to turn left
4. **Right Photo**: Same process, guide user to turn right
5. **Processing**: Show progress, call NativeBridge on background thread
6. **Results**: Display keypoints on photos + 3D mesh + measurements

### Keypoint Detection

- Use OpenPose Body-25 model (135 keypoints total)
- Each view (front, left, right) produces 135 2D keypoints
- Triangulate to get 135 3D keypoints

### 3D Reconstruction

- Use multi-view triangulation
- Scale using user height as reference
- Generate simple mesh (ellipsoids/cylinders per body segment)

### Measurements

- Calculate circumferences from 3D keypoints
- Fit ellipses to body cross-sections
- Return as FloatArray: [waist, chest, hips, thighs, ...]

---

## 18. DEPENDENCIES TO ADD

### CMake Dependencies
- OpenCV (NDK package)
- OpenPose Mobile (from GitHub repo)
- tinygltf (for GLB generation)

### Gradle Dependencies
- Room (database)
- Filament (3D rendering)
- Gson (JSON export)
- iText (PDF export)

---

## 19. FILE CREATION PRIORITY

### Phase 1: Foundation
1. CMakeLists.txt
2. NativeBridge.kt
3. jni_bridge.cpp (stub)
4. C++ header files

### Phase 2: Core Processing
1. C++ implementation files (stubs)
2. Image preprocessing
3. Pose estimation
4. 3D triangulation
5. Mesh generation

### Phase 3: UI Components
1. CaptureSequenceScreen
2. KeypointOverlay
3. FilamentMeshViewer
4. Result3DScreen

### Phase 4: Data & Export
1. Room entities & DAOs
2. Repository classes
3. ExportHelper

### Phase 5: Integration & Testing
1. Wire up navigation
2. Connect all components
3. Write tests
4. Profile & optimize

---

**END OF IMPLEMENTATION GUIDE**

This document should be referenced throughout the development process to ensure all requirements are met and the implementation follows the specified architecture.

