# Disaster Hazard Detection System

Real-time webcam object detection and disaster vulnerability risk analysis.

This project includes:
1. **Java Desktop Application**: Built with Java Swing, OpenCV, and YOLOv8 via ONNX Runtime (`DisasterHazardSystemApp.java` + `pom.xml`).
2. **Web Application**: Built with React, TypeScript, and TensorFlow.js object detection (`src/App.tsx`).

---

## 1. Running the Java Desktop Application

### Prerequisites
- Java Development Kit (JDK) 17 or higher
- Apache Maven 3.8 or higher
- Built-in webcam or external USB camera

### Running in VS Code
1. Open the project root folder in VS Code.
2. Install the **Extension Pack for Java** if not already installed.
3. Open `DisasterHazardSystemApp.java`.
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
Open [http://localhost:3000](http://localhost:3000) in your web browser.

---

## 3. Object-Oriented Programming (OOP) Architecture

The application design follows four core object-oriented principles:

### Encapsulation
- `HazardItem`: Encapsulates detection state including class label, confidence score, bounding coordinates, and risk messages behind private member variables with public accessors and mutators.
- `CameraDetectionEngine`: Manages camera hardware resources, background threads, and image buffers privately. External callers interact exclusively through public control methods (`toggleCamera`, `switchCamera`).
- `YoloOnnxDetector`: Isolates tensor memory allocation, model execution, and non-maximum suppression (NMS) calculations from the rest of the application.
- `SafetyTheme`: Consolidates color values and font definitions into private-constructor constant containers.

### Abstraction
- `DisasterScenario`: An abstract base class defining the contract for disaster risk analysis:
  - `evaluateOverallRisk(List<HazardItem>)`: Computes risk category based on detected items.
  - `getAdvisoryWarning(List<HazardItem>)`: Generates safety advisories.
  - `getHazardRisk(HazardItem)`: Evaluates risk for an individual item.
  - `getHazardExplanation(HazardItem)`: Explains why an item presents a risk.
- `DetectionListener`: An interface that allows the camera engine to push frame updates without depending on specific UI implementations.
- `ScreenNavigator`: An interface defining screen transition contracts.

### Inheritance
- `EarthquakeScenario`, `FloodScenario`, and `FireScenario` extend the base `DisasterScenario` class, reusing common metadata while supplying specialized disaster logic.
- `MainMenuPanel` and `LiveDetectionPanel` extend `BaseScreenPanel`, which in turn extends `JPanel`.
- Custom UI widgets (`CyberButton`, `CyberCardPanel`, `RiskBadge`) extend their respective Swing base components (`JButton`, `JPanel`, `JLabel`).

### Polymorphism
- `DisasterScenario` method invocations are resolved dynamically at runtime based on the selected disaster mode. The user interface calls methods through the abstract `DisasterScenario` reference without hardcoding scenario-specific branching logic.
