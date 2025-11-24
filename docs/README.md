# Documentation

This directory contains all project documentation for the Body Scan Application.

## Technical Reports

### [Technical_Report.md](Technical_Report.md)

**Concise Technical Report** - A streamlined overview of the project implementation covering all major steps without excessive detail. Ideal for quick reference and understanding the project at a high level.

**Contents:**

- Executive Summary
- Implementation Overview (6 major components)
- Performance Characteristics
- Key Challenges and Solutions
- System Architecture
- Privacy and Security
- Use Cases
- Conclusion

---

### [Detailed_Technical_Report.md](Detailed_Technical_Report.md)

**Comprehensive Technical Report** - Complete technical documentation with in-depth explanations of every aspect of the project. Includes detailed algorithms, architecture decisions, and implementation specifics.

**Contents:**

- Complete implementation details for all components
- Detailed algorithm descriptions
- Performance metrics and optimization strategies
- Comprehensive challenge analysis
- Full system architecture documentation
- Technical specifications

---

### [MEDIAPIPE_INTEGRATION_PLAN.md](MEDIAPIPE_INTEGRATION_PLAN.md)

**MediaPipe Integration Plan** - Original planning document for MediaPipe integration, detailing the approach and strategy for incorporating MediaPipe pose detection into the application.

---

### [Scan_Process_Flowchart.md](Scan_Process_Flowchart.md)

**Scan Process Flowchart** - Detailed visual flowchart showing the complete body scanning process from image capture to final measurements and GLB file output.

**Contents:**

- Complete process overview with decision points
- Detailed sub-processes for each stage:
  - Image capture and preprocessing pipeline
  - MediaPipe pose detection flow
  - Keypoint mapping (33 → 135)
  - Measurement calculation process
  - Multi-view 3D triangulation
  - 3D mesh generation and GLB export
  - Data storage process
- Error handling flows
- Performance timelines for single and multi-view scans
- Output file specifications

---

## Images

The `images/` directory contains visual documentation referenced in the technical reports:

- **pose_landmark_topology.svg** - MediaPipe landmark topology diagram
- **landmarked_pose.jpeg** - Real-world pose detection example
- **sample_measurement_on_ui.jpeg** - Application UI screenshot showing measurements

See [images/README.md](images/README.md) for detailed descriptions of each image.

---

## Documentation Structure

```

docs/
├── README.md                          # This file
├── Technical_Report.md                # Concise technical report
├── Detailed_Technical_Report.md      # Comprehensive technical report
├── MEDIAPIPE_INTEGRATION_PLAN.md      # MediaPipe integration planning
├── Scan_Process_Flowchart.md         # Process flowcharts and timelines
└── images/                            # Documentation images
    ├── README.md                      # Image descriptions
    ├── pose_landmark_topology.svg     # Landmark topology diagram
    ├── landmarked_pose.jpeg           # Pose detection example
    └── sample_measurement_on_ui.jpeg  # UI measurement display
```

---

## Quick Reference

**For Quick Overview:** Start with [Technical_Report.md](Technical_Report.md)

**For Deep Dive:** Read [Detailed_Technical_Report.md](Detailed_Technical_Report.md)

**For Visual Reference:** Check [images/](images/) directory

**For Integration Planning:** See [MEDIAPIPE_INTEGRATION_PLAN.md](MEDIAPIPE_INTEGRATION_PLAN.md)

**For Process Flow:** See [Scan_Process_Flowchart.md](Scan_Process_Flowchart.md)
