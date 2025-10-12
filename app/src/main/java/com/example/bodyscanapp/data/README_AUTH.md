# Firebase Authentication Backend

This directory contains the backend authentication functionality for the Body Scan App, supporting email-link (passwordless) and Google sign-in methods.

## Files Overview

### Core Authentication Classes

1. **`FirebaseAuthService.kt`** - Low-level Firebase authentication operations
2. **`AuthManager.kt`** - High-level authentication management with state handling
3. **`AuthResult.kt`** - Result types for authentication operations
4. **`DeepLinkHandler.kt`** - Handles deep links for email authentication

### Utility Classes

1. **`FirebaseUtils.kt`** - Firebase service utilities and Google Sign-In client setup

### Example Usage

1. **`AuthUsageExample.kt`** - Comprehensive examples of how to use the authentication services

## Authentication Methods

### 1. Email-Link (Passwordless) Authentication

```kotlin
val authManager = AuthManager(context)

// Send email link
authManager.sendEmailLink("user@example.com").collect { result ->
    when (result) {
        is AuthResult.Success -> println("Email sent")
        is AuthResult.Error -> println("Error: ${result.message}")
        is AuthResult.Loading -> println("Sending...")
    }
}

// Sign in with email link (handled by DeepLinkHandler)
authManager.signInWithEmailLink(email, link).collect { result ->
    // Handle result
}
```

### 2. Google Sign-In Authentication

```kotlin
val authManager = AuthManager(context)

// Get Google Sign-In intent
val signInIntent = authManager.getGoogleSignInIntent()
activity.startActivityForResult(signInIntent, REQUEST_CODE)

// Handle result
authManager.handleGoogleSignInResult(data).collect { result ->
    // Handle result
}
```

## State Management

The `AuthManager` provides a `StateFlow<AuthState>` for observing authentication state:

```kotlin
val authState by authManager.authState.collectAsState()

when (authState) {
    is AuthState.SignedOut -> { /* Show sign-in options */ }
    is AuthState.Loading -> { /* Show loading */ }
    is AuthState.EmailLinkSent -> { /* Show email sent confirmation */ }
    is AuthState.SignedIn -> { /* Show main app content */ }
}
```

## Deep Link Handling

The `DeepLinkHandler` automatically handles email authentication links:

```kotlin
val deepLinkHandler = DeepLinkHandler(context)

// Handle incoming deep link
val handled = deepLinkHandler.handleDeepLink(intent)
```

## Setup Requirements

### 1. Firebase Configuration

- Ensure `google-services.json` is in the `app/` directory
- Update `default_web_client_id` in `strings.xml` with your Firebase web client ID

### 2. Dependencies

All required dependencies are already configured in `build.gradle.kts`:
- Firebase BOM
- Firebase Auth
- Google Sign-In
- Google Play Services Auth

### 3. Deep Link Configuration

Configure your Firebase project to handle deep links:
- Add your app's package name to Firebase console
- Set up dynamic links or custom URL schemes

## Security Notes

1. **Email Link Security**: Email links are single-use and expire after a certain time
2. **Google Sign-In**: Uses OAuth 2.0 with secure token exchange
3. **Deep Links**: Always validate deep links before processing
4. **User Data**: Firebase handles user data securely with proper encryption

## Error Handling

All authentication methods return `AuthResult` which can be:
- `AuthResult.Success` - Operation completed successfully
- `AuthResult.Error` - Operation failed with error message
- `AuthResult.Loading` - Operation in progress

## Future Enhancements

1. **Phone Authentication**: Add SMS-based authentication
2. **Biometric Authentication**: Integrate with device biometrics
3. **Multi-Factor Authentication**: Add additional security layers
4. **Social Providers**: Add Facebook, Twitter, etc.

## Testing

Use the `AuthUsageExample.kt` file as a reference for testing different authentication flows. The examples show proper error handling and state management patterns.
