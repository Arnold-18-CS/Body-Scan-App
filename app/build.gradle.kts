plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    kotlin("kapt")  // For Room
}

// Configure Java toolchain to use Java 21 (automatically downloads if needed)
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}

android {
    namespace = "com.example.bodyscanapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.bodyscanapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        ndkVersion = "26.1.10909125"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    externalNativeBuild {
        cmake {
            path = file("src/main/jni/CMakeLists.txt")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
    }
    
    // Dependency resolution strategy to prefer Android variants
    configurations.all {
        resolutionStrategy {
            force("com.google.guava:guava:32.1.3-android")
        }
    }
}

dependencies {
    // Firebase BOM - manages all Firebase library versions
    implementation(platform(libs.firebase.bom))

    // Firebase dependencies
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.tools.core)

    // TOTP library for 2FA
    implementation(libs.kotlin.onetimepassword)

    // CameraX dependencies
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    implementation(libs.kotlinx.coroutines.guava)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Biometric authentication
    implementation("androidx.biometric:biometric-ktx:1.4.0-alpha02")

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // Google Sign-In (using version from BOM)
    implementation(libs.play.services.auth.v2070)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.appcompat)
    
    // Room (database) - using 2.7.0 for better Kotlin 2.2 compatibility
    implementation("androidx.room:room-ktx:2.7.0")
    kapt("androidx.room:room-compiler:2.7.0")
    
    // Coroutines (if not already present)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    
    // 3D rendering – Filament (lightweight)
    implementation("com.google.android.filament:filament-android:1.41.0")
    implementation("com.google.android.filament:filament-utils-android:1.41.0")
    implementation("com.google.android.filament:gltfio-android:1.41.0")
    
    // Export libraries
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.itextpdf:itext7-core:8.0.5")
    
    // Encrypted prefs
    implementation("androidx.security:security-crypto:1.1.0")
    
    // MediaPipe dependencies
    // Using Maven Central for MediaPipe Tasks API
    implementation("com.google.mediapipe:tasks-vision:0.10.14")
    implementation("com.google.mediapipe:tasks-core:0.10.14")
    implementation("com.google.mediapipe:tasks-audio:0.10.14")
    
    // MediaPipe required dependencies
    implementation("com.google.flogger:flogger:0.7.4")
    implementation("com.google.flogger:flogger-system-backend:0.7.4")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    // Guava version aligned with other dependencies to avoid conflicts
    // Using 32.1.3-android to match androidx.credentials dependency
    implementation("com.google.guava:guava:32.1.3-android")
    implementation("com.google.protobuf:protobuf-javalite:3.21.12")
    
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.core)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}