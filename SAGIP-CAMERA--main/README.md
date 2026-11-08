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