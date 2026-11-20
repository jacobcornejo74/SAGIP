@echo off

REM DISASTER HAZARD DETECTION SYSTEM - LAUNCH SCRIPT (Windows)


echo  Starting Disaster Hazard Detection System (Java Swing)


where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java is not installed or not in your PATH.
    echo Please install JDK 17+ from https://adoptium.net/
    pause
    exit /b 1
)

where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Maven is not installed or not in your PATH.
    echo Please install Apache Maven from https://maven.apache.org/
    pause
    exit /b 1
)

echo [OK] Launching real camera YOLO application...
mvn clean compile exec:java
pause
