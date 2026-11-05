#!/usr/bin/env bash
# ==============================================================================
# DISASTER HAZARD DETECTION SYSTEM - LAUNCH SCRIPT (macOS / Linux)
# ==============================================================================

echo "=========================================================="
echo " Starting Disaster Hazard Detection System (Java Swing)  "
echo "=========================================================="

# Check Java
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed or not in your PATH."
    echo "Please install JDK 17 or higher from https://adoptium.net/"
    exit 1
fi

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo " Error: Maven is not installed."
    echo " Install Maven via: 'brew install maven' (macOS) or 'sudo apt install maven' (Ubuntu)"
    exit 1
fi

echo "Environment check passed: $(java -version 2>&1 | head -n 1)"
echo "Compiling and launching real camera YOLO application..."
echo ""

mvn clean compile exec:java
