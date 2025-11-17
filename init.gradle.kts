// Gradle init script to work around Java 25 compatibility issue
// This script patches the Java version detection for Kotlin compiler

import org.gradle.api.JavaVersion

// Force Java version to be treated as Java 21 for compatibility
System.setProperty("java.version", "21")
System.setProperty("java.specification.version", "21")

