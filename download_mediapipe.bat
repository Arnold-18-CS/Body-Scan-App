@echo off
REM MediaPipe AAR Download and Build Script Wrapper for Windows
REM This batch file launches the PowerShell script

echo MediaPipe AAR Download/Build Script for Windows
echo ================================================
echo.

REM Check if PowerShell is available
where powershell >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: PowerShell is not installed or not in PATH.
    echo Please install PowerShell or add it to your PATH.
    pause
    exit /b 1
)

REM Get the directory where this batch file is located
set SCRIPT_DIR=%~dp0

REM Check PowerShell execution policy and run the script
echo Launching PowerShell script...
echo.

powershell.exe -ExecutionPolicy Bypass -File "%SCRIPT_DIR%download_mediapipe.ps1"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Script execution failed.
    echo.
    echo If you see an execution policy error, you may need to run:
    echo   Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
    echo.
    pause
    exit /b 1
)

echo.
echo Script completed successfully.
pause

