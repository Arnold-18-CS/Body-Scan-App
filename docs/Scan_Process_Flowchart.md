# Body Scan Process Flowchart

This document provides a detailed flowchart of the complete body scanning process from image capture to final measurements and GLB file output.

## Executive Summary - Process Overview

**High-level overview suitable for panel presentations:**

```mermaid
flowchart TD
    Start([User Initiates Body Scan]) --> Input[User Inputs Height<br/>Calibration: 100-250cm]
    Input --> Mode{Scan Mode Selection}
    
    Mode -->|Quick Scan| Single[Single Image Mode]
    Mode -->|3D Reconstruction| Multi[Multi-View Mode<br/>3 Images]
    
    Single --> Capture[Image Capture<br/>CameraX]
    Multi --> Capture
    
    Capture --> Preprocess[Image Preprocessing<br/>OpenCV: CLAHE, Resize,<br/>Color Optimization]
    Preprocess --> PoseDetect[Pose Detection<br/>MediaPipe: 33 Landmarks]
    PoseDetect --> KeypointMap[Keypoint Mapping<br/>33 → 135 Keypoints<br/>Interpolation & Extrapolation]
    
    KeypointMap --> CheckMode{Scan Mode?}
    CheckMode -->|Single Image| Measurements[Calculate Measurements<br/>8 Anthropometric Values<br/>Shoulder, Arm, Leg, Hip, etc.]
    Measurements --> Validate[Validate Measurements<br/>Physiological Range Check]
    Validate --> Store[Store in Database<br/>Room Database]
    Store --> Export[Export Results<br/>JSON, CSV, PDF]
    Export --> EndSingle([Complete<br/>~1 second])
    
    CheckMode -->|Multi-View| CheckViews{All 3 Views<br/>Processed?}
    CheckViews -->|No| Capture
    CheckViews -->|Yes| Triangulate[3D Triangulation<br/>Multi-View Stereo<br/>135 3D Keypoints]
    Triangulate --> Mesh[Generate 3D Mesh<br/>Delaunay Triangulation<br/>BODY_25 Format]
    Mesh --> GLB[Export GLB File<br/>3D Model for Visualization]
    GLB --> Measurements3D[Calculate 3D Measurements<br/>Enhanced Accuracy]
    Measurements3D --> Validate3D[Validate 3D Measurements]
    Validate3D --> Store3D[Store in Database<br/>+ GLB File Path]
    Store3D --> Export3D[Export Results<br/>JSON, CSV, PDF + GLB]
    Export3D --> EndMulti([Complete<br/>~3 seconds])
    
    style Start fill:#e1f5ff,stroke:#0066cc,stroke-width:3px
    style EndSingle fill:#d4edda,stroke:#28a745,stroke-width:2px
    style EndMulti fill:#d4edda,stroke:#28a745,stroke-width:2px
    style Single fill:#fff3cd,stroke:#ffc107,stroke-width:2px
    style Multi fill:#fff3cd,stroke:#ffc107,stroke-width:2px
    style GLB fill:#f8d7da,stroke:#dc3545,stroke-width:2px
    style Store fill:#d1ecf1,stroke:#17a2b8,stroke-width:2px
    style Store3D fill:#d1ecf1,stroke:#17a2b8,stroke-width:2px
```

### Key Process Stages

1. **User Input & Mode Selection** (100ms)
   - User provides height calibration (100-250cm)
   - Selects scan mode: Single Image (quick) or Multi-View (3D)

2. **Image Capture** (200ms per image)
   - CameraX integration with real-time framing guides
   - RGB image capture with validation

3. **Image Preprocessing** (200ms per image)
   - OpenCV pipeline: CLAHE enhancement, resizing (~640px), color optimization
   - Optimizes images for MediaPipe pose detection

4. **Pose Detection** (500ms per image)
   - MediaPipe Tasks Vision API detects 33 anatomical landmarks
   - Extracts body structure: head, torso, limbs, hands, feet

5. **Keypoint Mapping** (50ms)
   - Expands 33 MediaPipe landmarks to 135 keypoints
   - Uses interpolation and anatomical extrapolation

6. **Measurement Calculation** (100ms)
   - Calculates 8 anthropometric measurements:
     - Shoulder Width, Arm Length, Leg Length, Hip Width
     - Upper/Lower Body Length, Neck Width, Thigh Width
   - Applies user height calibration for accurate scaling

7. **3D Reconstruction** (Multi-View Only, 300ms)
   - Multi-view stereo triangulation from 3 camera angles
   - Generates 135 3D keypoints with depth information

8. **3D Mesh Generation** (Multi-View Only, 500ms)
   - Converts to BODY_25 format (25 keypoints)
   - Delaunay triangulation creates 3D surface mesh
   - Exports GLB file for 3D visualization

9. **Data Storage & Export** (100ms)
   - Stores scan data in local Room database
   - Exports measurements in JSON, CSV, or PDF formats
   - Saves GLB file path (multi-view scans)

### Performance Summary

| Scan Type | Total Time | Output Files | Use Case |
|-----------|------------|--------------|----------|
| **Single Image** | < 1.5 seconds | Measurements (JSON/CSV/PDF)<br/>2D Keypoints (JSON) | Quick body measurements<br/>Fitness tracking |
| **Multi-View** | < 5 seconds | Measurements (JSON/CSV/PDF)<br/>3D Keypoints (JSON)<br/>GLB 3D Model | 3D body reconstruction<br/>Virtual avatars<br/>Precise measurements |

### Key Features

- ✅ **On-Device Processing**: All computation performed locally, no cloud dependency
- ✅ **Privacy-First**: Images deleted immediately after processing
- ✅ **Sub-Centimetre Accuracy**: Precise measurements validated against physiological ranges
- ✅ **Real-Time Performance**: Fast processing suitable for mobile devices
- ✅ **Memory Efficient**: < 100MB peak memory usage
- ✅ **Offline Capable**: Complete functionality without internet connection

---

## Detailed Process Overview

```mermaid
flowchart TD
    Start([User Initiates New Scan]) --> InputHeight[User Inputs Height<br/>100-250cm]
    InputHeight --> SelectMode{Scan Mode?}
    
    SelectMode -->|Single Image| SingleFlow[Single Image Flow]
    SelectMode -->|Multi-View| MultiFlow[Multi-View Flow<br/>3 Images]
    
    SingleFlow --> Capture1[Capture Image 1]
    MultiFlow --> Capture1
    
    Capture1 --> Preprocess1[Image Preprocessing]
    Preprocess1 --> PoseDetect1[MediaPipe Pose Detection]
    PoseDetect1 --> KeypointMap1[33→135 Keypoint Mapping]
    
    KeypointMap1 --> CheckModeType{Scan Mode?}
    CheckModeType -->|Single Image| CalcMeasure[Calculate Measurements]
    CalcMeasure --> ValidateMeasure[Validate Measurements]
    ValidateMeasure --> StoreSingle[Store in Database]
    StoreSingle --> ExportSingle[Export Results]
    ExportSingle --> End([Complete])
    
    CheckModeType -->|Multi-View| CheckImages{All 3 Images<br/>Processed?}
    CheckImages -->|No| Capture2[Capture Image 2]
    CheckImages -->|No| Capture3[Capture Image 3]
    Capture2 --> Preprocess2[Preprocess Image 2]
    Capture3 --> Preprocess3[Preprocess Image 3]
    Preprocess2 --> PoseDetect2[Pose Detection Image 2]
    Preprocess3 --> PoseDetect3[Pose Detection Image 3]
    PoseDetect2 --> KeypointMap2[Keypoint Mapping Image 2]
    PoseDetect3 --> KeypointMap3[Keypoint Mapping Image 3]
    KeypointMap2 --> CheckImages
    KeypointMap3 --> CheckImages
    
    CheckImages -->|Yes| Triangulate[3D Triangulation<br/>135 Keypoints]
    Triangulate --> ConvertBODY25[Convert to BODY_25 Format]
    ConvertBODY25 --> GenerateMesh[Generate 3D Mesh<br/>Delaunay Triangulation]
    GenerateMesh --> ExportGLB[Export GLB File]
    ExportGLB --> CalcMeasure3D[Calculate 3D Measurements]
    CalcMeasure3D --> ValidateMeasure3D[Validate 3D Measurements]
    ValidateMeasure3D --> StoreMulti[Store in Database<br/>+ GLB Path]
    StoreMulti --> ExportMulti[Export Results]
    ExportMulti --> End
    
    style Start fill:#e1f5ff
    style End fill:#d4edda
    style SingleFlow fill:#fff3cd
    style MultiFlow fill:#fff3cd
    style ExportGLB fill:#f8d7da
    style StoreSingle fill:#d1ecf1
    style StoreMulti fill:#d1ecf1
```

## Detailed Sub-Processes

### 1. Image Capture Process

```mermaid
flowchart TD
    StartCapture([Start Image Capture]) --> InitCamera[Initialize CameraX]
    InitCamera --> ShowPreview[Display Camera Preview<br/>with Framing Guides]
    ShowPreview --> CheckPosition{Body Positioned<br/>Correctly?}
    CheckPosition -->|No| ShowPreview
    CheckPosition -->|Yes| CaptureImage[Capture RGB Image]
    CaptureImage --> ConvertBitmap[Convert to Android Bitmap]
    ConvertBitmap --> ValidateImage{Image Valid?}
    ValidateImage -->|No| ShowError[Display Error Message]
    ShowError --> ShowPreview
    ValidateImage -->|Yes| DeletePreview[Delete Preview Image]
    DeletePreview --> EndCapture([Image Captured])
    
    style StartCapture fill:#e1f5ff
    style EndCapture fill:#d4edda
    style ShowError fill:#f8d7da
```

### 2. Image Preprocessing Pipeline

```mermaid
flowchart TD
    StartPreprocess([Raw Image Input]) --> LoadImage[Load Image to Memory]
    LoadImage --> CheckFormat{Image Format?}
    CheckFormat -->|RGBA| ConvertRGB[Convert RGBA → RGB]
    CheckFormat -->|RGB| Resize[Resize to ~640px Width<br/>Maintain Aspect Ratio]
    ConvertRGB --> Resize
    Resize --> ApplyCLAHE[Apply CLAHE<br/>Contrast Limited Adaptive<br/>Histogram Equalization]
    ApplyCLAHE --> ConvertBGR[Convert RGB → BGR]
    ConvertBGR --> ConvertLAB[Convert BGR → LAB<br/>Color Space]
    ConvertLAB --> Optimize[Optimize for MediaPipe]
    Optimize --> EndPreprocess([Preprocessed Image Ready])
    
    style StartPreprocess fill:#e1f5ff
    style EndPreprocess fill:#d4edda
    style ApplyCLAHE fill:#fff3cd
```

### 3. MediaPipe Pose Detection

```mermaid
flowchart TD
    StartPose([Preprocessed Image]) --> LoadModel{Model Loaded?}
    LoadModel -->|No| LoadModelFile[Load pose_landmarker.task<br/>from Assets]
    LoadModelFile --> InitPoseLandmarker[Initialize PoseLandmarker]
    LoadModel -->|Yes| InitPoseLandmarker
    InitPoseLandmarker --> ConvertToMP[Convert Bitmap to<br/>MediaPipe Image Format]
    ConvertToMP --> DetectPose[Run Pose Detection<br/>MediaPipe API]
    DetectPose --> ExtractLandmarks[Extract 33 Landmarks<br/>Normalized Coordinates 0-1]
    ExtractLandmarks --> GetSegmentation[Extract Segmentation Mask<br/>if Available]
    GetSegmentation --> ValidatePose{Minimum 10<br/>Landmarks Detected?}
    ValidatePose -->|No| PoseError[Pose Detection Failed]
    PoseError --> EndPoseError([Error: Insufficient Landmarks])
    ValidatePose -->|Yes| EndPose([33 Landmarks + Mask Ready])
    
    style StartPose fill:#e1f5ff
    style EndPose fill:#d4edda
    style EndPoseError fill:#f8d7da
    style PoseError fill:#f8d7da
```

### 4. Keypoint Mapping (33 → 135)

```mermaid
flowchart TD
    StartMap([33 MediaPipe Landmarks]) --> DirectMap[Direct Mapping<br/>Map First 33 Landmarks<br/>to Keypoint Indices 0-32]
    DirectMap --> Interpolate[Linear Interpolation<br/>Generate Intermediate Keypoints]
    Interpolate --> InterpolateShoulder[Shoulder-Elbow Midpoints]
    InterpolateShoulder --> InterpolateElbow[Elbow-Wrist Midpoints]
    InterpolateElbow --> InterpolateHip[Hip-Knee Midpoints]
    InterpolateHip --> InterpolateKnee[Knee-Ankle Midpoints]
    InterpolateKnee --> Extrapolate[Anatomical Extrapolation<br/>Estimate Remaining Keypoints<br/>Using Body Proportions]
    Extrapolate --> ValidateKeypoints{All 135 Keypoints<br/>Valid?}
    ValidateKeypoints -->|No| FillMissing[Fill Missing Keypoints<br/>Nearest Valid or Default]
    FillMissing --> ValidateKeypoints
    ValidateKeypoints -->|Yes| EndMap([135 Keypoints Ready])
    
    style StartMap fill:#e1f5ff
    style EndMap fill:#d4edda
    style FillMissing fill:#fff3cd
```

### 5. Measurement Calculation

```mermaid
flowchart TD
    StartMeasure([135 Keypoints Ready]) --> GetHeight[Get User Height Input<br/>Calibration Factor]
    GetHeight --> CalcBodyHeight[Calculate Body Height<br/>in Pixels<br/>Head-to-Feet Distance]
    CalcBodyHeight --> CalcScale[Calculate Scale Factor<br/>cm/pixel = User Height /<br/>Pixel Height]
    CalcScale --> CalcShoulder[Calculate Shoulder Width<br/>Distance: Left-Right Shoulder]
    CalcShoulder --> CalcArm[Calculate Arm Length<br/>Average: Left + Right<br/>Shoulder-Elbow-Wrist]
    CalcArm --> CalcLeg[Calculate Leg Length<br/>Average: Left + Right<br/>Hip-Knee-Ankle]
    CalcLeg --> CalcHip[Calculate Hip Width<br/>Distance: Left-Right Hip]
    CalcHip --> CalcUpperBody[Calculate Upper Body Length<br/>Vertical: Hip Midpoint to Head]
    CalcUpperBody --> CalcLowerBody[Calculate Lower Body Length<br/>Vertical: Hip Midpoint to Ankle]
    CalcLowerBody --> CalcNeck[Calculate Neck Width<br/>Horizontal: Eye Landmarks]
    CalcNeck --> CalcThigh[Calculate Thigh Width<br/>Segmentation Mask or<br/>Depth Approximation]
    CalcThigh --> ApplyScale[Apply Scale Factor<br/>to All Measurements]
    ApplyScale --> ValidateRange{Measurements in<br/>Physiological Range?}
    ValidateRange -->|No| FlagInvalid[Flag Invalid Measurements<br/>Set to Zero]
    FlagInvalid --> StoreMeasure[Store 8 Measurements]
    ValidateRange -->|Yes| StoreMeasure
    StoreMeasure --> EndMeasure([Measurements Ready])
    
    style StartMeasure fill:#e1f5ff
    style EndMeasure fill:#d4edda
    style FlagInvalid fill:#fff3cd
```

### 6. Multi-View 3D Triangulation

```mermaid
flowchart TD
    Start3D([3 Views with 135 Keypoints Each]) --> SetupCameras[Setup Camera Configuration<br/>Front: 0°, Left: 120°, Right: -120°<br/>Distance: 200cm]
    SetupCameras --> ConvertToPixels[Convert Normalized Coordinates<br/>0-1 → Pixel Coordinates<br/>for Each View]
    ConvertToPixels --> BuildProjection[Build Projection Matrices<br/>Rotation + Translation<br/>for Each Camera]
    BuildProjection --> LoopKeypoints[For Each of 135 Keypoints]
    LoopKeypoints --> Extract2D[Extract 2D Coordinates<br/>from All 3 Views]
    Extract2D --> TriangulatePoint[OpenCV triangulatePoints<br/>Front + Left Views]
    TriangulatePoint --> ConvertHomogeneous[Convert from Homogeneous<br/>4D → 3D Cartesian]
    ConvertHomogeneous --> Validate3D{Point Valid?<br/>No NaN/Infinity/Zero}
    Validate3D -->|No| SkipPoint[Skip Invalid Point]
    Validate3D -->|Yes| Scale3D[Scale 3D Coordinates<br/>Using User Height]
    Scale3D --> Store3D[Store 3D Keypoint]
    SkipPoint --> CheckNext{More Keypoints?}
    Store3D --> CheckNext
    CheckNext -->|Yes| LoopKeypoints
    CheckNext -->|No| End3D([135 3D Keypoints Ready])
    
    style Start3D fill:#e1f5ff
    style End3D fill:#d4edda
    style SkipPoint fill:#fff3cd
```

### 7. 3D Mesh Generation

```mermaid
flowchart TD
    StartMesh([135 3D Keypoints]) --> ConvertBODY25[Convert to BODY_25 Format<br/>25 Keypoints]
    ConvertBODY25 --> MapDirect[Direct Mapping<br/>Corresponding Landmarks]
    MapDirect --> InterpolateNeck[Interpolate Neck Point]
    InterpolateNeck --> InterpolateMidHip[Interpolate Mid-Hip Point]
    InterpolateMidHip --> InitMeshGenerator[Initialize MeshGenerator]
    InitMeshGenerator --> DelaunayTri[Apply Delaunay Triangulation<br/>Surface Reconstruction]
    DelaunayTri --> GenerateFaces[Generate Triangular Faces]
    GenerateFaces --> CalculateNormals[Calculate Vertex Normals]
    CalculateNormals --> ValidateMesh{Mesh Valid?}
    ValidateMesh -->|No| MeshError[Mesh Generation Failed]
    MeshError --> EndMeshError([Error: Cannot Generate Mesh])
    ValidateMesh -->|Yes| PrepareGLB[Prepare GLB Structure<br/>Vertices, Faces, Normals]
    PrepareGLB --> EndMesh([3D Mesh Ready])
    
    style StartMesh fill:#e1f5ff
    style EndMesh fill:#d4edda
    style EndMeshError fill:#f8d7da
    style MeshError fill:#f8d7da
```

### 8. GLB Export Process

```mermaid
flowchart TD
    StartGLB([3D Mesh Ready]) --> CreateGLTF[Create GLTF Structure<br/>Scene, Nodes, Meshes]
    CreateGLTF --> AddVertices[Add Vertex Positions<br/>135 3D Keypoints]
    AddVertices --> AddFaces[Add Face Indices<br/>Triangular Faces]
    AddFaces --> AddNormals[Add Vertex Normals]
    AddNormals --> SerializeJSON[Serialize to JSON<br/>GLTF Format]
    SerializeJSON --> ConvertBinary[Convert to Binary<br/>GLB Format]
    ConvertBinary --> GeneratePath[Generate File Path<br/>Internal Storage]
    GeneratePath --> WriteFile[Write GLB File<br/>to Storage]
    WriteFile --> ValidateFile{File Written<br/>Successfully?}
    ValidateFile -->|No| FileError[File Write Failed]
    FileError --> EndGLBError([Error: Cannot Write GLB])
    ValidateFile -->|Yes| StorePath[Store GLB Path<br/>in Database]
    StorePath --> EndGLB([GLB File Exported])
    
    style StartGLB fill:#e1f5ff
    style EndGLB fill:#d4edda
    style EndGLBError fill:#f8d7da
    style FileError fill:#f8d7da
```

### 9. Data Storage Process

```mermaid
flowchart TD
    StartStore([Scan Data Ready]) --> GetUser[Get Current User ID<br/>from Authentication]
    GetUser --> CreateScan[Create Scan Entity]
    CreateScan --> SetTimestamp[Set Timestamp<br/>Current Time]
    SetTimestamp --> SetHeight[Set User Height<br/>Calibration Value]
    SetHeight --> StoreKeypoints{Scan Type?}
    StoreKeypoints -->|Single Image| Store2D[Store 2D Keypoints<br/>JSON Format]
    StoreKeypoints -->|Multi-View| Store3DKeypoints[Store 3D Keypoints<br/>135 x, y, z coordinates<br/>JSON Format]
    Store2D --> StoreMeasurements[Store Measurements<br/>8 Values with Labels<br/>JSON Format]
    Store3DKeypoints --> StoreGLBPath[Store GLB File Path<br/>if Available]
    StoreGLBPath --> StoreMeasurements
    StoreMeasurements --> InsertDB[Insert into Room Database<br/>Scan Table]
    InsertDB --> ValidateDB{Insert Successful?}
    ValidateDB -->|No| DBError[Database Error]
    DBError --> EndStoreError([Error: Cannot Store Scan])
    ValidateDB -->|Yes| LinkUser[Link to User Entity<br/>Foreign Key]
    LinkUser --> EndStore([Scan Stored Successfully])
    
    style StartStore fill:#e1f5ff
    style EndStore fill:#d4edda
    style EndStoreError fill:#f8d7da
    style DBError fill:#f8d7da
```

## Complete Process Timeline

### Single Image Scan Timeline

```
┌─────────────────────────────────────────────────────────────────┐
│                    SINGLE IMAGE SCAN PROCESS                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [0ms]     User Inputs Height (100-250cm)                      │
│  [100ms]   Camera Initialization                                │
│  [200ms]   Image Capture (CameraX)                              │
│  [300ms]   Image Preprocessing (OpenCV)                         │
│             ├─ RGBA → RGB Conversion                            │
│             ├─ Resize to ~640px                                │
│             ├─ CLAHE Enhancement                                │
│             └─ Color Space Optimization                         │
│  [500ms]   MediaPipe Pose Detection                             │
│             └─ Extract 33 Landmarks                            │
│  [550ms]   Keypoint Mapping (33 → 135)                         │
│             ├─ Direct Mapping                                  │
│             ├─ Linear Interpolation                             │
│             └─ Anatomical Extrapolation                        │
│  [600ms]   Measurement Calculation                             │
│             ├─ Calculate 8 Measurements                        │
│             ├─ Apply Scale Factor                               │
│             └─ Validate Ranges                                  │
│  [650ms]   Store in Database                                    │
│  [700ms]   Export Results (JSON/CSV/PDF)                      │
│                                                                 │
│  Total: ~700ms (< 1 second)                                     │
└─────────────────────────────────────────────────────────────────┘
```

### Multi-View Scan Timeline

```
┌─────────────────────────────────────────────────────────────────┐
│                    MULTI-VIEW SCAN PROCESS                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [0ms]      User Inputs Height (100-250cm)                      │
│  [100ms]    Camera Initialization                               │
│                                                                 │
│  ┌─ IMAGE 1 ───────────────────────────────────────────────┐  │
│  │ [200ms]   Capture Image 1 (Front View)                   │  │
│  │ [300ms]   Preprocess Image 1                             │  │
│  │ [500ms]   Pose Detection Image 1                         │  │
│  │ [550ms]   Keypoint Mapping Image 1                        │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌─ IMAGE 2 ───────────────────────────────────────────────┐  │
│  │ [600ms]   Capture Image 2 (Left Profile)                 │  │
│  │ [700ms]   Preprocess Image 2                             │  │
│  │ [900ms]   Pose Detection Image 2                         │  │
│  │ [950ms]   Keypoint Mapping Image 2                        │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌─ IMAGE 3 ───────────────────────────────────────────────┐  │
│  │ [1000ms]  Capture Image 3 (Right Profile)                │  │
│  │ [1100ms]  Preprocess Image 3                             │  │
│  │ [1300ms]  Pose Detection Image 3                         │  │
│  │ [1350ms]  Keypoint Mapping Image 3                       │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  [1400ms]   3D Triangulation                                    │
│             ├─ Build Projection Matrices                       │
│             ├─ Triangulate 135 Keypoints                       │
│             └─ Scale 3D Coordinates                            │
│  [1700ms]   Convert to BODY_25 Format                          │
│  [1800ms]   Generate 3D Mesh (Delaunay)                        │
│  [2300ms]   Export GLB File                                    │
│  [2400ms]   Calculate 3D Measurements                          │
│  [2500ms]   Validate Measurements                              │
│  [2600ms]   Store in Database (+ GLB Path)                     │
│  [2700ms]   Export Results (JSON/CSV/PDF)                      │
│                                                                 │
│  Total: ~2700ms (< 3 seconds)                                   │
└─────────────────────────────────────────────────────────────────┘
```

## Error Handling Flow

```mermaid
flowchart TD
    ProcessStep([Any Process Step]) --> CheckSuccess{Success?}
    CheckSuccess -->|Yes| Continue[Continue to Next Step]
    CheckSuccess -->|No| IdentifyError{Error Type?}
    
    IdentifyError -->|Image Capture| ImageError[Image Capture Error<br/>- Body not in frame<br/>- Camera failure]
    IdentifyError -->|Pose Detection| PoseError[Pose Detection Error<br/>- < 10 landmarks<br/>- Low confidence]
    IdentifyError -->|Measurement| MeasureError[Measurement Error<br/>- Out of range<br/>- Invalid calculation]
    IdentifyError -->|3D Reconstruction| ReconstructionError[3D Error<br/>- Triangulation failed<br/>- Invalid coordinates]
    IdentifyError -->|Mesh Generation| MeshError[Mesh Error<br/>- Delaunay failed<br/>- Invalid geometry]
    IdentifyError -->|Storage| StorageError[Storage Error<br/>- Database failure<br/>- File write failed]
    
    ImageError --> UserFeedback1[Show User-Friendly Message<br/>Guide User to Retry]
    PoseError --> UserFeedback2[Show User-Friendly Message<br/>Suggest Better Positioning]
    MeasureError --> UserFeedback3[Flag Invalid Measurements<br/>Continue with Valid Ones]
    ReconstructionError --> UserFeedback4[Fallback to 2D Measurements<br/>Skip 3D Reconstruction]
    MeshError --> UserFeedback5[Skip GLB Generation<br/>Store 3D Keypoints Only]
    StorageError --> UserFeedback6[Show Error Message<br/>Allow Retry]
    
    UserFeedback1 --> Retry{User Retries?}
    UserFeedback2 --> Retry
    UserFeedback3 --> Continue
    UserFeedback4 --> Continue
    UserFeedback5 --> Continue
    UserFeedback6 --> Retry
    
    Retry -->|Yes| ProcessStep
    Retry -->|No| Abort([Abort Scan])
    Continue --> NextStep([Next Process Step])
    
    style ProcessStep fill:#e1f5ff
    style NextStep fill:#d4edda
    style Abort fill:#f8d7da
```

## Key Decision Points

1. **Scan Mode Selection**: User chooses between single-image quick scan or multi-view 3D reconstruction
2. **Image Validation**: System checks if body is fully visible and properly positioned
3. **Pose Detection Validation**: Minimum 10 landmarks must be detected for valid scan
4. **Measurement Validation**: All measurements checked against physiological ranges
5. **3D Reconstruction**: Only performed for multi-view scans with all 3 images successfully processed
6. **GLB Generation**: Only generated if 3D reconstruction succeeds and mesh is valid

## Performance Targets

- **Single Image Scan**: < 1.5 seconds total
- **Multi-View Scan**: < 5 seconds total
- **Memory Usage**: < 100MB peak
- **Image Processing**: < 200ms per image
- **Pose Detection**: < 500ms per image
- **3D Triangulation**: < 300ms for 135 keypoints
- **Mesh Generation**: < 500ms
- **GLB Export**: < 100ms

## Output Files

### Single Image Scan Output
- **Measurements JSON**: 8 anthropometric measurements
- **2D Keypoints JSON**: 135 normalized keypoints (x, y)
- **Export Formats**: JSON, CSV, PDF

### Multi-View Scan Output
- **Measurements JSON**: 8 anthropometric measurements (3D-enhanced)
- **3D Keypoints JSON**: 135 3D keypoints (x, y, z)
- **GLB File**: 3D mesh for visualization
- **Export Formats**: JSON, CSV, PDF

---

**Note**: All processing is performed on-device with no cloud dependencies. Images are deleted immediately after processing completion to ensure user privacy.

