# Image Capture Screen - UI Specification

## Screen Layout

```
┌─────────────────────────────────────────┐
│              Image Capture              │  ← Title (White, Bold)
├─────────────────────────────────────────┤
│                                         │
│  ┌───────────────────────────────────┐ │
│  │   Enter Your Height               │ │  ← Section Header
│  │                                   │ │
│  │  [175 cm      ]        175 cm     │ │  ← TextField + Display
│  │                                   │ │
│  │  ←────────●──────────────────→    │ │  ← Slider (100-250)
│  │  100 cm                  250 cm   │ │  ← Range Labels
│  └───────────────────────────────────┘ │
│                                         │
│  ┌───────────────────────────────────┐ │
│  │                                   │ │
│  │  ┌─────────────────────────────┐ │ │
│  │  │                             │ │ │
│  │  │  Align body within frame    │ │ │  ← Feedback Text (Overlay)
│  │  └─────────────────────────────┘ │ │
│  │                                   │ │
│  │        ┌─────────────────┐       │ │
│  │        │                 │       │ │  ← Head Circle Guide
│  │        │        ○        │       │ │
│  │        │                 │       │ │
│  │        │                 │       │ │
│  │   ┌────┼─────────────────┼────┐  │ │  ← Body Frame Guide
│  │   │    │                 │    │  │ │     (Dashed White)
│  │   │    └─────────────────┘    │  │ │
│  │   │                           │  │ │
│  │   │    - - - - - - - - - -    │  │ │  ← Alignment Lines
│  │   │                           │  │ │
│  │   │    - - - - - - - - - -    │  │ │
│  │   │                           │  │ │
│  │   │    - - - - - - - - - -    │  │ │
│  │   │                           │  │ │
│  │   └───────────────────────────┘  │ │
│  │                                   │ │
│  │      [Camera Preview Area]       │ │  ← Live Camera Feed
│  │                                   │ │
│  └───────────────────────────────────┘ │
│                                         │
│  ┌───────────────────────────────────┐ │
│  │       Capture Image               │ │  ← Capture Button (Blue)
│  └───────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

## Component Breakdown

### 1. Title Section
```kotlin
Text(
    text = "Image Capture",
    style = MaterialTheme.typography.headlineMedium,
    fontWeight = FontWeight.Bold,
    color = Color.White
)
```

### 2. Height Input Section
**Container:**
- Background: Dark Gray (#1E1E1E)
- Corner Radius: 12dp
- Padding: 16dp

**Components:**
a) **TextField**
   - Label: "Height (cm)"
   - Input Type: Number
   - Validation: Real-time
   - Error Display: Below field

b) **Height Display**
   - Format: "XXX cm"
   - Style: Bold, White
   - Width: 70dp
   - Alignment: End

c) **Slider**
   - Range: 100f..250f
   - Thumb Color: Blue (#2196F3)
   - Active Track: Blue (#2196F3)
   - Inactive Track: Gray

d) **Range Labels**
   - "100 cm" (left)
   - "250 cm" (right)
   - Color: Gray
   - Size: Small

### 3. Camera Preview Section
**Container:**
- Background: Black
- Fills remaining space
- Contains overlays

**Preview View:**
- Type: AndroidView(PreviewView)
- Aspect: Fill parent
- Camera: Back camera

**Framing Guide Overlay:**
```
Frame Rectangle:
- Width: 60% of screen
- Height: 70% of screen
- Style: Dashed white stroke (3px)
- Dash Pattern: [20f, 10f]
- Position: Centered

Head Circle:
- Radius: 15% of frame width
- Position: Top of frame
- Style: Dashed white stroke (3px)
- Dash Pattern: [15f, 10f]

Alignment Lines:
- Count: 3 horizontal lines
- Spacing: Evenly distributed
- Style: Dashed white (1px, 50% alpha)
- Dash Pattern: [10f, 10f]
```

**Feedback Text Overlay:**
- Position: Top center
- Background: Black 60% alpha
- Corner Radius: 8dp
- Padding: 12dp
- Text: White, Large, Bold

### 4. Capture Button
```kotlin
Button(
    modifier = fillMaxWidth() + height(56.dp),
    colors = containerColor(Blue #2196F3),
    shape = RoundedCornerShape(12.dp),
    enabled = isHeightValid && hasCameraPermission
)
```

**States:**
- Enabled: Blue background, White text
- Disabled: Gray background, White text

## Color Palette

```
Primary Background:   #000000 (Black)
Secondary Background: #1E1E1E (Dark Gray)
Primary Action:       #2196F3 (Blue)
Text Primary:         #FFFFFF (White)
Text Secondary:       #9E9E9E (Gray)
Guide Overlay:        #FFFFFF (White, various alphas)
Error:                Material Error Color
Success:              Material Success Color
```

## Typography

```
Title:          headlineMedium, Bold, White
Section Header: titleMedium, Bold, White
Body Text:      bodyLarge, Regular, White
Labels:         bodySmall, Regular, Gray
Button:         titleMedium, Bold, White
Error:          bodySmall, Regular, Error Color
```

## Spacing & Dimensions

```
Screen Padding:        16dp
Section Padding:       16dp
Element Spacing:       8dp
Button Height:         56dp
Corner Radius (Large): 12dp
Corner Radius (Small): 8dp

Height Input:
- TextField Width:     flex (weight 1f)
- Display Width:       70dp
- Slider Height:       Default
- Label Font:          bodySmall

Camera Preview:
- Aspect:              Fill available space
- Frame Width:         60% of screen
- Frame Height:        70% of screen
- Head Circle Radius:  15% of frame width

Capture Button:
- Width:               100% (fillMaxWidth)
- Height:              56dp
```

## Interaction States

### Height Input
1. **Empty State**
   - TextField: Empty
   - Slider: 175 (middle value)
   - Button: Disabled

2. **Valid Input**
   - TextField: Shows value (100-250)
   - Slider: Synced with TextField
   - Error: None
   - Button: Enabled (if camera permission)

3. **Invalid Input**
   - TextField: Shows error outline
   - Error Text: Displayed below
   - Button: Disabled
   - Examples:
     - "Height must be at least 100 cm" (< 100)
     - "Height must not exceed 250 cm" (> 250)
     - "Please enter a valid number" (non-numeric)

### Camera Permission
1. **Not Requested**
   - Shows: Permission request dialog
   - Preview: Hidden

2. **Granted**
   - Preview: Live camera feed
   - Overlay: Framing guide visible
   - Button: Enabled (if height valid)

3. **Denied**
   - Preview: Message "Camera permission is required"
   - Button: Disabled

### Capture States
1. **Before Capture**
   - Feedback: "Align body within the frame"
   - Button: "Capture Image"
   - Button State: Based on validation

2. **During Capture**
   - Feedback: (no change or "Capturing...")
   - Button: Processing

3. **After Success**
   - Feedback: "Image captured successfully!"
   - Logcat: "Processing image data of size: X bytes"
   - Callback: onCaptureComplete(byteArray)

4. **After Failure**
   - Feedback: "Capture failed: [error message]"
   - Logcat: Error logged
   - User can retry

## Accessibility

### Semantic Labels
- Height TextField: "Enter height in centimeters"
- Slider: "Height selection slider, 100 to 250 centimeters"
- Capture Button: "Capture image button"
- Feedback Text: Announced when changed

### Focus Order
1. Height TextField
2. Height Slider
3. Capture Button

### Screen Reader Support
- All interactive elements have content descriptions
- State changes announced
- Error messages read automatically

## Responsive Behavior

### Portrait Mode (Primary)
- Layout as shown in diagram
- Camera preview fills available space
- All elements vertically stacked

### Landscape Mode (Optional)
- Consider side-by-side layout:
  - Left: Height input + Capture button
  - Right: Camera preview with overlay

### Different Screen Sizes
- Small (< 5"): Compact spacing
- Medium (5-7"): Standard spacing
- Large (> 7"): Expanded spacing
- Tablet: Consider two-column layout

## Animation & Transitions

### Entry Animation
- Fade in: 300ms
- Crossfade from Home Screen

### Height Input
- TextField focus: Smooth transition
- Slider movement: Immediate
- Error appearance: Fade in 200ms

### Capture Button
- Enabled/Disabled: Color transition 300ms
- Press: Scale 0.95, duration 100ms

### Feedback Text
- Update: Fade out/in 200ms
- Color change: 300ms

## Edge Cases Handled

1. **No Camera Hardware**
   - Show error message
   - Disable capture functionality

2. **Camera in Use**
   - Show error: "Camera is in use"
   - Option to retry

3. **Low Memory**
   - Capture at lower quality
   - Show warning message

4. **Rapid Button Presses**
   - Debounce capture action
   - Prevent multiple simultaneous captures

5. **Orientation Changes**
   - Maintain height input value
   - Re-initialize camera if needed

6. **App Background/Foreground**
   - Release camera when backgrounded
   - Re-request camera when foregrounded

## Performance Considerations

- Camera preview: 30 fps target
- Capture latency: < 500ms
- Memory usage: Efficient ByteArray handling
- Image disposal: Immediate after conversion
- Preview disposal: On screen exit

---

**Design Version**: 1.0
**Last Updated**: October 6, 2025
**Status**: Implemented & Ready for Testing

