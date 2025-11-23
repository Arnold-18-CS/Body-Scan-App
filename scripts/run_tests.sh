#!/bin/bash

# Test Runner Script for BodyScanApp
# This script runs all unit tests and generates a test report

echo "🧪 Running BodyScanApp Unit Tests"
echo "================================="

# Set up colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    if [ $2 -eq 0 ]; then
        echo -e "${GREEN}✅ $1${NC}"
    else
        echo -e "${RED}❌ $1${NC}"
    fi
}

# Function to print warning
print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# Check if we're in the right directory
if [ ! -f "gradlew" ]; then
    echo -e "${RED}❌ Error: gradlew not found. Please run this script from the project root directory.${NC}"
    exit 1
fi

# Make gradlew executable
chmod +x gradlew

echo "📋 Test Plan:"
echo "1. ValidationUtils Tests - Email format, non-empty fields, password validation"
echo "2. TotpService Tests - TOTP validation logic with mocking"
echo "3. AuthRepository Tests - Authentication and registration scenarios"
echo ""

# Run ValidationUtils tests
echo "🔍 Running ValidationUtils Tests..."
./gradlew test --tests "com.example.bodyscanapp.utils.ValidationUtilsTest" --console=plain
VALIDATION_RESULT=$?
print_status "ValidationUtils Tests" $VALIDATION_RESULT

# Run TotpService tests
echo ""
echo "🔐 Running TotpService Tests..."
./gradlew test --tests "com.example.bodyscanapp.data.TotpServiceTest" --console=plain
TOTP_RESULT=$?
print_status "TotpService Tests" $TOTP_RESULT

# Run AuthRepository tests
echo ""
echo "🔑 Running AuthRepository Tests..."
./gradlew test --tests "com.example.bodyscanapp.data.AuthRepositoryTest" --console=plain
AUTH_RESULT=$?
print_status "AuthRepository Tests" $AUTH_RESULT

# Run all tests together
echo ""
echo "🚀 Running All Unit Tests..."
./gradlew test --console=plain
ALL_TESTS_RESULT=$?

echo ""
echo "📊 Test Results Summary"
echo "======================="

# Count test results
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Parse test results (simplified)
if [ $VALIDATION_RESULT -eq 0 ]; then
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

if [ $TOTP_RESULT -eq 0 ]; then
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

if [ $AUTH_RESULT -eq 0 ]; then
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

TOTAL_TESTS=$((PASSED_TESTS + FAILED_TESTS))

echo "Total Test Suites: $TOTAL_TESTS"
echo "Passed: $PASSED_TESTS"
echo "Failed: $FAILED_TESTS"

if [ $ALL_TESTS_RESULT -eq 0 ]; then
    print_status "All Tests Passed!" 0
    echo ""
    echo "🎉 Test execution completed successfully!"
    echo ""
    echo "📝 Next Steps:"
    echo "1. Review the manual testing checklist: MANUAL_TESTING_CHECKLIST.md"
    echo "2. Test on API 24+ emulator/device"
    echo "3. Verify UI responsiveness and error feedback"
    echo "4. Document test results in GitHub issue comments"
else
    print_status "Some Tests Failed" 1
    echo ""
    echo "🔧 Troubleshooting:"
    echo "1. Check the test output above for specific failures"
    echo "2. Ensure all dependencies are properly configured"
    echo "3. Verify test environment setup"
    echo "4. Run individual test suites to isolate issues"
fi

echo ""
echo "📁 Test Reports Location:"
echo "- HTML Report: app/build/reports/tests/test/index.html"
echo "- XML Report: app/build/test-results/test/TEST-*.xml"

echo ""
echo "🔍 To run specific tests:"
echo "./gradlew test --tests 'com.example.bodyscanapp.utils.ValidationUtilsTest'"
echo "./gradlew test --tests 'com.example.bodyscanapp.data.TotpServiceTest'"
echo "./gradlew test --tests 'com.example.bodyscanapp.data.AuthRepositoryTest'"

echo ""
echo "📱 Manual Testing:"
echo "Use the checklist in MANUAL_TESTING_CHECKLIST.md for comprehensive manual testing"

exit $ALL_TESTS_RESULT

