#!/bin/bash
# macOS wrapper for gradlew that uses Java from gradle.properties.local

# Get Java path from gradle.properties.local if it exists
if [ -f "gradle.properties.local" ]; then
    JAVA_HOME_FROM_LOCAL=$(grep "^org.gradle.java.home=" gradle.properties.local | cut -d'=' -f2)
    if [ -n "$JAVA_HOME_FROM_LOCAL" ] && [ -d "$JAVA_HOME_FROM_LOCAL" ]; then
        export JAVA_HOME="$JAVA_HOME_FROM_LOCAL"
        export PATH="$JAVA_HOME/bin:$PATH"
    fi
fi

# Call the original gradlew
exec ./gradlew "$@"

