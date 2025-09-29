# Mock Authentication Credentials

This document lists the mock authentication credentials available for testing the Body Scan App login functionality.

## Available Test Accounts

### Username-based Login

- **Username:** `admin`
- **Password:** `admin123`

- **Username:** `testuser`
- **Password:** `testpass`

### Email-based Login

- **Email:** `user@example.com`
- **Password:** `password123`

## Testing Instructions

1. **Valid Login Test:**
   - Enter any of the above credentials
   - Click "Login"
   - Should navigate to 2FA screen

2. **Invalid Login Test:**
   - Enter incorrect credentials
   - Click "Login"
   - Should show error message via Snackbar

3. **Validation Test:**
   - Try empty fields
   - Try invalid email format
   - Try short password
   - Should show appropriate validation errors

4. **Registration Test:**
   - Register with new credentials
   - Should navigate to 2FA screen
   - Try registering with existing credentials
   - Should show error message

## Features Implemented

✅ Login Screen with email/username and password fields
✅ Login button and register link
✅ Basic validation (non-empty fields, valid email format)
✅ Mock authentication logic using SharedPreferences
✅ Navigation to 2FA screen on successful login
✅ Error message display using Snackbar
✅ Password visibility toggle
✅ Enhanced validation with proper error messages
