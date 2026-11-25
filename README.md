<div align="center">
  <img src="docs/sagip-banner.svg" alt="SAGIP - Smart Guard for Incident Prevention" width="100%" />
  <h1>SAGIP — Disaster Hazard Detection System</h1>
  <p>
    <strong>Real-time camera object detection and fire, flood, and earthquake risk assessment.</strong>
  </p>
  <p>
    <a href="#1-running-the-java-desktop-application">Desktop App (Java Swing)</a> &bull;
    <a href="#2-running-the-web-application">Web App (React)</a> &bull;
    <a href="#3-local-storage-and-json-archive">JSON &amp; Capture Storage</a> &bull;
    <a href="#4-object-oriented-programming-oop-architecture">OOP Architecture</a>
  </p>
  <p>
    <img src="https://img.shields.io/badge/Java-17%2B-F39137?style=for-the-badge&labelColor=1D2224" alt="Java 17+" />
    <img src="https://img.shields.io/badge/Maven-3.8%2B-2A3031?style=for-the-badge&labelColor=1D2224" alt="Maven" />
    <img src="https://img.shields.io/badge/YOLOv8-ONNX_Runtime-E2483F?style=for-the-badge&labelColor=1D2224" alt="YOLOv8 ONNX" />
    <img src="https://img.shields.io/badge/React-TypeScript-68BE74?style=for-the-badge&labelColor=1D2224" alt="React TypeScript" />
  </p>
</div>

---

Real-time webcam object detection and disaster vulnerability risk analysis with color-coded bounding boxes (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`), `[F]` Fire / `[E]` Earthquake hazard tags, per-object risk lists, and persistent scan storage.

This project includes:
1. **Java Desktop Application**: Built with Java Swing, OpenCV/JavaCV, Sarxos Webcam, and YOLOv8 via ONNX Runtime. `SAGIP.java` launches the application; the desktop implementation is organized into individual Java classes in `GUI/`.
2. **Web Application**: Built with React, TypeScript, and TensorFlow.js COCO-SSD object detection (`src/App.tsx`).

---

## 1. Running the Java Desktop Application

### Prerequisites
- Java Development Kit (JDK) 17 or higher
- Apache Maven 3.8 or higher
- Built-in webcam or external USB camera

### Running in VS Code
1. Open the project root folder in VS Code.
2. Install the **Extension Pack for Java** if not already installed.
3. Open `SAGIP.java`.
4. Press `F5` or click **Run**.

### Running via Terminal
- **Windows**:
  Run `run.bat` or execute:
  ```cmd
  mvn clean compile exec:java
  ```

- **macOS / Linux**:
  Run `run.sh` or execute:
  ```bash
  chmod +x run.sh
  ./run.sh
  ```

---

## 2. Running the Web Application

### Prerequisites
- Node.js 18 or higher
- npm

### Commands
```bash
npm install
npm run dev
```

---

## 3. Local Storage and JSON Archive

Clicking **SAVE SCAN** in the Camera page stores:
- `sagip-storage/scans.json`: Capture timestamp, overall risk severity, detection count, bounding box coordinates, `[F]` / `[E]` hazard tags, and risk descriptions.
- `sagip-storage/captures/scan_<id>.png`: Full-color camera capture with the detection bounding boxes and risk cards drawn directly on the image.

All saved scans load automatically inside the **STORAGE** page with thumbnail previews, full-size inspection, delete/clear controls, and JSON/PNG export.

---

## 4. Object-Oriented Programming (OOP) Architecture

The desktop application structure applies four core object-oriented principles:

### Encapsulation
- `HazardItem`: Encapsulates detection state including class label, confidence score, normalized bounding box, and scenario risks behind private fields with accessors and mutators.
- `CameraDetectionEngine`: Manages webcam hardware resources, capture threads, and frame buffers privately, exposing `startCameraStream()`, `stopCameraStream()`, and `toggleCamera()`.
- `YoloOnnxDetector`: Isolates ONNX Runtime tensor preparation, inference execution, and non-maximum suppression (NMS).
- `SagipTheme`: Groups dark and light UI palette values and styling helpers behind static utility methods.
- `ScanStorageManager`: Handles reading and writing `sagip-storage/scans.json` and PNG snapshots on disk.

### Abstraction
- `DisasterScenario`: Abstract base class defining risk evaluation methods:
  - `evaluateOverallRisk(List<HazardItem>)`: Computes overall severity across detected hazards.
  - `getAdvisoryWarning(List<HazardItem>)`: Generates safety advisories.
  - `getHazardRisk(HazardItem)`: Evaluates risk for an individual object.
  - `getHazardExplanation(HazardItem)`: Explains why a detected item presents a risk.
- `DetectionListener`: Decouples camera frame updates from UI views.
- `ScreenNavigator`: Defines navigation contracts between screens.

### Inheritance
- `EarthquakeScenario`, `FloodScenario`, and `FireScenario` extend `DisasterScenario`.
- `CameraPage`, `CameraViewportPanel`, `MenuBackground`, and `ContrastPanel` extend `JPanel`.

### Polymorphism
- `DisasterScenario` references resolve scenario-specific risk evaluation methods dynamically at runtime without hardcoding scenario branches inside the viewport.
