# PowerShell script to build with proper environment variables for Room/SQLite
# This script sets TMP/TEMP before running Gradle to fix Room annotation processor issues

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Gradle Build with Environment Fix" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Get the script directory
$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $SCRIPT_DIR

# Set custom temp directory (user-writable)
$USER_TEMP = Join-Path $env:USERPROFILE "AppData\Local\Temp\gradle-temp"

# Create temp directory if it doesn't exist
if (-not (Test-Path $USER_TEMP)) {
    New-Item -ItemType Directory -Path $USER_TEMP -Force | Out-Null
    Write-Host "Created temp directory: $USER_TEMP" -ForegroundColor Green
}

# Set environment variables for this session
$env:TMP = $USER_TEMP
$env:TEMP = $USER_TEMP
$env:TMPDIR = $USER_TEMP

# Also set java.io.tmpdir as a system property
$env:GRADLE_OPTS = "-Djava.io.tmpdir=$USER_TEMP"

Write-Host "Environment variables set:" -ForegroundColor Yellow
Write-Host "  TMP: $env:TMP" -ForegroundColor White
Write-Host "  TEMP: $env:TEMP" -ForegroundColor White
Write-Host "  TMPDIR: $env:TMPDIR" -ForegroundColor White
Write-Host "  GRADLE_OPTS: $env:GRADLE_OPTS" -ForegroundColor White
Write-Host ""

# Check if running as administrator
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if ($isAdmin) {
    Write-Host "✅ Running as Administrator" -ForegroundColor Green
} else {
    Write-Host "⚠️  NOT running as Administrator" -ForegroundColor Yellow
    Write-Host "   Room's SQLite JDBC driver may need admin privileges to extract native libraries" -ForegroundColor Yellow
    Write-Host "   Consider running this script as Administrator if the build fails" -ForegroundColor Yellow
}
Write-Host ""

# Get build arguments (everything after script name)
$buildArgs = $args

# If no arguments provided, default to clean assembleDebug
if ($buildArgs.Count -eq 0) {
    $buildArgs = @("clean", "assembleDebug")
    Write-Host "No arguments provided, using default: clean assembleDebug" -ForegroundColor Cyan
} else {
    Write-Host "Build arguments: $($buildArgs -join ' ')" -ForegroundColor Cyan
}
Write-Host ""

# Run Gradle build
Write-Host "Starting Gradle build..." -ForegroundColor Cyan
Write-Host ""

try {
    & .\gradlew.bat @buildArgs
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "BUILD SUCCESSFUL!" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
        exit 0
    } else {
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Red
        Write-Host "BUILD FAILED (exit code: $LASTEXITCODE)" -ForegroundColor Red
        Write-Host "========================================" -ForegroundColor Red
        exit $LASTEXITCODE
    }
} catch {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "BUILD ERROR: $_" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    exit 1
}

