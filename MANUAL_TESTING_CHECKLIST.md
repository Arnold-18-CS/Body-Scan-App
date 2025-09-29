# Manual Testing Checklist for BodyScanApp Authentication Flow

## Test Environment Requirements
- **Target API Level**: 24+ (Android 7.0+)
- **Test Devices**: 
  - API 24+ Emulator (Android Studio)
  - Physical device (Android 7.0+)
- **Test Credentials**: See `MOCK_AUTH_CREDENTIALS.md`

## Pre-Test Setup
1. [ ] Clean install the app on test device/emulator
2. [ ] Verify app launches without crashes
3. [ ] Confirm initial screen shows Login form
4. [ ] Check that all UI elements are visible and properly aligned

## Login Flow Testing

### Valid Login Scenarios
- [ ] **Username Login**
  - [ ] Enter username: `admin`
  - [ ] Enter password: `admin123`
  - [ ] Tap "Login" button
  - [ ] Verify success message: "Login successful! Please enter your 2FA code."
  - [ ] Verify navigation to 2FA screen

- [ ] **Email Login**
  - [ ] Enter email: `user@example.com`
  - [ ] Enter password: `password123`
  - [ ] Tap "Login" button
  - [ ] Verify success message: "Login successful! Please enter your 2FA code."
  - [ ] Verify navigation to 2FA screen

### Invalid Login Scenarios
- [ ] **Blank Username/Email**
  - [ ] Leave username/email field empty
  - [ ] Enter password: `password123`
  - [ ] Tap "Login" button
  - [ ] Verify error message: "Please enter your username or email"
  - [ ] Verify user stays on login screen

- [ ] **Blank Password**
  - [ ] Enter username: `admin`
  - [ ] Leave password field empty
  - [ ] Tap "Login" button
  - [ ] Verify error message: "Please enter your password"
  - [ ] Verify user stays on login screen

- [ ] **Invalid Username**
  - [ ] Enter username: `nonexistent`
  - [ ] Enter password: `password123`
  - [ ] Tap "Login" button
  - [ ] Verify error message: "User not found. Please check your username or email"
  - [ ] Verify user stays on login screen

- [ ] **Invalid Email**
  - [ ] Enter email: `nonexistent@example.com`
  - [ ] Enter password: `password123`
  - [ ] Tap "Login" button
  - [ ] Verify error message: "User not found. Please check your username or email"
  - [ ] Verify user stays on login screen

- [ ] **Wrong Password**
  - [ ] Enter username: `admin`
  - [ ] Enter password: `wrongpassword`
  - [ ] Tap "Login" button
  - [ ] Verify error message: "Incorrect password. Please try again"
  - [ ] Verify user stays on login screen

- [ ] **Invalid Email Format**
  - [ ] Enter email: `invalid-email`
  - [ ] Enter password: `password123`
  - [ ] Tap "Login" button
  - [ ] Verify error message: "Please enter a valid email address"
  - [ ] Verify user stays on login screen

- [ ] **Short Username**
  - [ ] Enter username: `ab`
  - [ ] Enter password: `password123`
  - [ ] Tap "Login" button
  - [ ] Verify error message: "Username must be at least 3 characters"
  - [ ] Verify user stays on login screen

- [ ] **Short Password**
  - [ ] Enter username: `admin`
  - [ ] Enter password: `12345`
  - [ ] Tap "Login" button
  - [ ] Verify error message: "Password must be at least 6 characters"
  - [ ] Verify user stays on login screen

## 2FA (Two-Factor Authentication) Flow Testing

### Valid 2FA Scenarios
- [ ] **Valid TOTP Code**
  - [ ] Complete successful login first
  - [ ] On 2FA screen, enter a valid 6-digit code (use generated code from app)
  - [ ] Tap "Verify" button
  - [ ] Verify success message: "2FA verification successful! Welcome to Body Scan App."
  - [ ] Verify navigation to Home screen

### Invalid 2FA Scenarios
- [ ] **Blank TOTP Code**
  - [ ] Complete successful login first
  - [ ] On 2FA screen, leave code field empty
  - [ ] Tap "Verify" button
  - [ ] Verify error message: "Please enter the 6-digit code"
  - [ ] Verify user stays on 2FA screen

- [ ] **Short TOTP Code**
  - [ ] Complete successful login first
  - [ ] On 2FA screen, enter 5-digit code: `12345`
  - [ ] Tap "Verify" button
  - [ ] Verify error message: "Code must be exactly 6 digits"
  - [ ] Verify user stays on 2FA screen

- [ ] **Long TOTP Code**
  - [ ] Complete successful login first
  - [ ] On 2FA screen, enter 7-digit code: `1234567`
  - [ ] Tap "Verify" button
  - [ ] Verify error message: "Code must be exactly 6 digits"
  - [ ] Verify user stays on 2FA screen

- [ ] **Non-Numeric TOTP Code**
  - [ ] Complete successful login first
  - [ ] On 2FA screen, enter non-numeric code: `abcdef`
  - [ ] Tap "Verify" button
  - [ ] Verify error message: "Code must contain only numbers"
  - [ ] Verify user stays on 2FA screen

- [ ] **Invalid TOTP Code**
  - [ ] Complete successful login first
  - [ ] On 2FA screen, enter invalid 6-digit code: `000000`
  - [ ] Tap "Verify" button
  - [ ] Verify error message: "Invalid or expired code. Please try again"
  - [ ] Verify user stays on 2FA screen

### 2FA UI Testing
- [ ] **Timer Display**
  - [ ] Verify countdown timer is visible and updating
  - [ ] Verify timer shows remaining seconds (0-30)
  - [ ] Verify progress bar is visible and updating

- [ ] **Resend Button**
  - [ ] Verify "Resend" button is disabled initially
  - [ ] Wait for timer to reach 5 seconds or less
  - [ ] Verify "Resend" button becomes enabled
  - [ ] Tap "Resend" button
  - [ ] Verify success message: "New code sent to your authenticator app"
  - [ ] Verify timer resets

- [ ] **Setup TOTP Button**
  - [ ] Tap "Setup TOTP" button
  - [ ] Verify message: "TOTP setup feature coming soon"

## Home Screen Testing

### Successful Login Flow
- [ ] **Complete Login -> 2FA -> Home Flow**
  - [ ] Login with valid credentials
  - [ ] Enter valid 2FA code
  - [ ] Verify navigation to Home screen
  - [ ] Verify Home screen displays correctly
  - [ ] Verify user is logged in (check for logout option)

### Logout Testing
- [ ] **Logout Functionality**
  - [ ] From Home screen, tap logout button
  - [ ] Verify success message: "Logged out successfully"
  - [ ] Verify navigation back to Login screen
  - [ ] Verify login state is cleared (can't access Home directly)

## Registration Flow Testing

### Valid Registration Scenarios
- [ ] **New User Registration**
  - [ ] From Login screen, tap "Register" button
  - [ ] Enter username: `newuser`
  - [ ] Enter email: `newuser@example.com`
  - [ ] Enter password: `newpass123`
  - [ ] Enter confirm password: `newpass123`
  - [ ] Tap "Register" button
  - [ ] Verify success message and navigation to Home screen

### Invalid Registration Scenarios
- [ ] **Blank Username**
  - [ ] Enter blank username
  - [ ] Fill other fields with valid data
  - [ ] Tap "Register" button
  - [ ] Verify error message: "Please enter a username"

- [ ] **Blank Email**
  - [ ] Enter blank email
  - [ ] Fill other fields with valid data
  - [ ] Tap "Register" button
  - [ ] Verify error message: "Please enter an email address"

- [ ] **Blank Password**
  - [ ] Enter blank password
  - [ ] Fill other fields with valid data
  - [ ] Tap "Register" button
  - [ ] Verify error message: "Please enter a password"

- [ ] **Blank Confirm Password**
  - [ ] Enter blank confirm password
  - [ ] Fill other fields with valid data
  - [ ] Tap "Register" button
  - [ ] Verify error message: "Please confirm your password"

- [ ] **Invalid Email Format**
  - [ ] Enter invalid email: `invalid-email`
  - [ ] Fill other fields with valid data
  - [ ] Tap "Register" button
  - [ ] Verify error message: "Please enter a valid email address"

- [ ] **Short Username**
  - [ ] Enter short username: `ab`
  - [ ] Fill other fields with valid data
  - [ ] Tap "Register" button
  - [ ] Verify error message: "Username must be at least 3 characters"

- [ ] **Short Password**
  - [ ] Enter short password: `12345`
  - [ ] Fill other fields with valid data
  - [ ] Tap "Register" button
  - [ ] Verify error message: "Password must be at least 6 characters"

- [ ] **Mismatched Passwords**
  - [ ] Enter password: `password123`
  - [ ] Enter different confirm password: `different123`
  - [ ] Fill other fields with valid data
  - [ ] Tap "Register" button
  - [ ] Verify error message: "Passwords do not match"

- [ ] **Existing Username**
  - [ ] Enter existing username: `admin`
  - [ ] Fill other fields with valid data
  - [ ] Tap "Register" button
  - [ ] Verify error message: "Username already exists. Please choose a different one"

- [ ] **Existing Email**
  - [ ] Enter existing email: `user@example.com`
  - [ ] Fill other fields with valid data
  - [ ] Tap "Register" button
  - [ ] Verify error message: "Email already registered. Please use a different email"

## UI Responsiveness Testing

### Screen Orientation
- [ ] **Portrait Mode**
  - [ ] Test all screens in portrait orientation
  - [ ] Verify all elements are visible and accessible
  - [ ] Verify text is readable and buttons are tappable

- [ ] **Landscape Mode**
  - [ ] Rotate device to landscape
  - [ ] Test all screens in landscape orientation
  - [ ] Verify all elements are visible and accessible
  - [ ] Verify text is readable and buttons are tappable

### Different Screen Sizes
- [ ] **Small Screen (Phone)**
  - [ ] Test on small screen device/emulator
  - [ ] Verify all elements fit on screen
  - [ ] Verify scrolling works if needed

- [ ] **Large Screen (Tablet)**
  - [ ] Test on large screen device/emulator
  - [ ] Verify layout adapts appropriately
  - [ ] Verify elements are not too spread out

### Touch Interactions
- [ ] **Button Responsiveness**
  - [ ] Verify all buttons respond to touch
  - [ ] Verify button press feedback is visible
  - [ ] Verify buttons are not too small to tap

- [ ] **Text Input Fields**
  - [ ] Verify keyboard appears when tapping input fields
  - [ ] Verify keyboard dismisses appropriately
  - [ ] Verify text input works correctly

- [ ] **Error Message Display**
  - [ ] Verify error messages are clearly visible
  - [ ] Verify error messages don't overlap with other elements
  - [ ] Verify error messages are readable

### Performance Testing
- [ ] **App Launch Time**
  - [ ] Measure time from tap to login screen display
  - [ ] Verify launch time is acceptable (< 3 seconds)

- [ ] **Screen Transition Speed**
  - [ ] Measure time for screen transitions
  - [ ] Verify transitions are smooth and responsive

- [ ] **Memory Usage**
  - [ ] Monitor memory usage during testing
  - [ ] Verify no memory leaks during extended use

## Error Handling Testing

### Network Connectivity
- [ ] **No Network Connection**
  - [ ] Test with network disabled
  - [ ] Verify appropriate error handling
  - [ ] Verify app doesn't crash

### App State Management
- [ ] **Background/Foreground**
  - [ ] Test app behavior when backgrounded
  - [ ] Test app behavior when returning to foreground
  - [ ] Verify login state is maintained

- [ ] **App Restart**
  - [ ] Test app behavior after force close
  - [ ] Verify login state persistence
  - [ ] Verify proper initialization

## Accessibility Testing

### Screen Reader Support
- [ ] **TalkBack/VoiceOver**
  - [ ] Enable screen reader
  - [ ] Navigate through all screens
  - [ ] Verify all elements are accessible
  - [ ] Verify proper labels and descriptions

### High Contrast Mode
- [ ] **High Contrast Display**
  - [ ] Enable high contrast mode
  - [ ] Verify all elements are visible
  - [ ] Verify text is readable

### Font Size Scaling
- [ ] **Large Font Sizes**
  - [ ] Increase system font size to maximum
  - [ ] Verify all text is readable
  - [ ] Verify layout adapts appropriately

## Test Results Documentation

### Pass/Fail Criteria
- [ ] **All Login Scenarios**: Pass/Fail
- [ ] **All 2FA Scenarios**: Pass/Fail
- [ ] **All Registration Scenarios**: Pass/Fail
- [ ] **UI Responsiveness**: Pass/Fail
- [ ] **Error Handling**: Pass/Fail
- [ ] **Accessibility**: Pass/Fail

### Issues Found
- [ ] **Critical Issues**: List any critical issues found
- [ ] **Minor Issues**: List any minor issues found
- [ ] **Suggestions**: List any improvement suggestions

### Test Environment Details
- [ ] **Device Model**: 
- [ ] **Android Version**: 
- [ ] **API Level**: 
- [ ] **Screen Size**: 
- [ ] **Test Date**: 
- [ ] **Tester Name**: 

## Notes
- Use the mock credentials provided in `MOCK_AUTH_CREDENTIALS.md`
- Test on both emulator and physical device
- Document any crashes or unexpected behavior
- Take screenshots of any issues found
- Verify all error messages are user-friendly and helpful
