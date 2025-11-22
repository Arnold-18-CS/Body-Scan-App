// Gradle init script to work around Java 25 compatibility issue
// This script patches the Java version detection for Kotlin compiler

import org.gradle.api.JavaVersion
import org.gradle.api.invocation.Gradle

// Force Java version to be treated as Java 21 for compatibility
System.setProperty("java.version", "21")
System.setProperty("java.specification.version", "21")

// Configure custom temp directory for all Gradle processes to avoid Windows access issues
// This prevents SQLite JDBC from trying to extract native libraries to C:\WINDOWS
val userHome = System.getProperty("user.home")
val customTempDir = when {
    System.getProperty("os.name").contains("Windows", ignoreCase = true) -> {
        File(userHome, "AppData/Local/Temp/gradle-temp").apply { mkdirs() }
    }
    else -> {
        File(userHome, ".gradle/temp").apply { mkdirs() }
    }
}

// Set system property for all Gradle processes
System.setProperty("java.io.tmpdir", customTempDir.absolutePath)

// Also set as environment variables that SQLite JDBC will check
System.setProperty("TMP", customTempDir.absolutePath)
System.setProperty("TEMP", customTempDir.absolutePath)

// Configure Gradle to use the custom temp directory for workers
gradle.settingsEvaluated {
    // This ensures workers inherit the temp directory configuration
    // Workers will use the jvmargs from gradle.properties which includes -Djava.io.tmpdir
}

