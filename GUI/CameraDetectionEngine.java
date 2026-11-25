package GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Encapsulates real hardware webcam streaming + YOLOv8 inference loop.
 */
class CameraDetectionEngine {
    private static CameraDetectionEngine instance;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean reconnectRequested = new AtomicBoolean(false);
    private volatile boolean userPausedCamera = true;
    private volatile boolean isCameraConnected = false;
    private volatile boolean isAiDetectionActive = true;
    private volatile double currentFps = 0.0;
    private volatile int cameraIndex = 0;
    private float confidenceThreshold = 0.40f;

    private BufferedImage currentFrame = null;
    private final List<HazardItem> detectedHazards = new CopyOnWriteArrayList<>();
    private final List<DetectionListener> listeners = new CopyOnWriteArrayList<>();
    private final YoloOnnxDetector yoloDetector = new YoloOnnxDetector();

    private Object activeGrabber = null;
    private Object cachedFrameConverter = null;
    private Thread captureWorkerThread = null;
    private String hardwareStatusMessage = "CAMERA STANDBY";

    private CameraDetectionEngine() {
        yoloDetector.initializeAsync();
        startEngine();
    }

    public static synchronized CameraDetectionEngine getInstance() {
        if (instance == null) instance = new CameraDetectionEngine();
        return instance;
    }

    public synchronized void startEngine() {
        if (isRunning.get()) return;
        isRunning.set(true);

        captureWorkerThread = new Thread(this::runCaptureLoop, "Camera-YOLO-Worker");
        captureWorkerThread.setDaemon(true);
        captureWorkerThread.start();
    }

    public synchronized void shutdown() {
        isRunning.set(false);
        releaseCameraHardware();
        if (captureWorkerThread != null) {
            captureWorkerThread.interrupt();
        }
    }

    private void runCaptureLoop() {
        if (!userPausedCamera) {
            initHardwareCamera();
        }

        long lastFpsTime = System.currentTimeMillis();
        long lastReconnectTime = System.currentTimeMillis();
        long lastYoloTime = 0;
        int frameCounter = 0;

        while (isRunning.get()) {
            long loopStart = System.currentTimeMillis();

            if (reconnectRequested.getAndSet(false)) {
                releaseCameraHardware();
                if (!userPausedCamera) {
                    initHardwareCamera();
                }
                lastReconnectTime = loopStart;
            } else if (!userPausedCamera && !isCameraConnected && (loopStart - lastReconnectTime >= 3000)) {
                lastReconnectTime = loopStart;
                initHardwareCamera();
            }

            BufferedImage rawFrame = null;
            if (!userPausedCamera && isCameraConnected && activeGrabber != null) {
                rawFrame = grabHardwareFrame();
            }

            BufferedImage displayFrame;
            if (rawFrame != null) {
                if (isAiDetectionActive && yoloDetector.isReady() && (loopStart - lastYoloTime >= 95)) {
                    lastYoloTime = loopStart;
                    List<HazardItem> results = yoloDetector.detect(rawFrame, confidenceThreshold);
                    detectedHazards.clear();
                    detectedHazards.addAll(results);
                } else if (!isAiDetectionActive) {
                    detectedHazards.clear();
                }
                displayFrame = rawFrame;
            } else {
                detectedHazards.clear();
                displayFrame = null;
            }

            this.currentFrame = displayFrame;

            frameCounter++;
            long now = System.currentTimeMillis();
            if (now - lastFpsTime >= 500) {
                currentFps = isCameraConnected
                        ? Math.round((frameCounter * 1000.0 / (now - lastFpsTime)) * 10.0) / 10.0
                        : 0.0;
                frameCounter = 0;
                lastFpsTime = now;
            }

            final BufferedImage outFrame = displayFrame;
            final double outFps = currentFps;
            final boolean live = isCameraConnected && !userPausedCamera;
            final String telemetry = yoloDetector.getStatus();
            final List<HazardItem> snapshot = new ArrayList<>(detectedHazards);

            SwingUtilities.invokeLater(() -> {
                for (DetectionListener l : listeners) {
                    l.onFrameUpdated(outFrame, outFps, live, telemetry);
                    l.onHazardsDetected(snapshot);
                }
            });

            long elapsed = System.currentTimeMillis() - loopStart;
            try {
                Thread.sleep(Math.max(8, 33 - elapsed));
            } catch (InterruptedException e) {
                break;
            }
        }

        releaseCameraHardware();
    }

    private void initHardwareCamera() {
        releaseCameraHardware();

        // Strategy 1 (Primary): JavaCV OpenCVFrameGrabber
        try {
            Class<?> grabberClass = Class.forName("org.bytedeco.javacv.OpenCVFrameGrabber");
            if (cachedFrameConverter == null) {
                cachedFrameConverter = Class.forName("org.bytedeco.javacv.Java2DFrameConverter")
                        .getConstructor().newInstance();
            }

            int[] probeOrder = new int[]{cameraIndex, 0, 1, 2};
            Set<Integer> tried = new HashSet<>();
            for (int idx : probeOrder) {
                if (!tried.add(idx)) continue;
                Object grabber = null;
                try {
                    grabber = grabberClass.getConstructor(int.class).newInstance(idx);
                    try {
                        grabberClass.getMethod("setImageWidth", int.class).invoke(grabber, 1280);
                        grabberClass.getMethod("setImageHeight", int.class).invoke(grabber, 720);
                    } catch (Throwable ignored) {}
                    grabberClass.getMethod("start").invoke(grabber);

                    BufferedImage testImg = null;
                    for (int attempt = 0; attempt < 6; attempt++) {
                        testImg = convertOpenCvGrab(grabber);
                        if (testImg != null && testImg.getWidth() > 0) break;
                        Thread.sleep(40);
                    }

                    if (testImg != null && testImg.getWidth() > 0) {
                        this.activeGrabber = grabber;
                        this.cameraIndex = idx;
                        this.isCameraConnected = true;
                        this.hardwareStatusMessage = "ONLINE: CAMERA #" + idx + " (" + testImg.getWidth() + "x" + testImg.getHeight() + ")";
                        notifyCameraState();
                        return;
                    } else {
                        try { grabberClass.getMethod("stop").invoke(grabber); } catch (Throwable ignored) {}
                        try { grabberClass.getMethod("release").invoke(grabber); } catch (Throwable ignored) {}
                    }
                } catch (Throwable inner) {
                    if (grabber != null) {
                        try { grabberClass.getMethod("stop").invoke(grabber); } catch (Throwable ignored) {}
                        try { grabberClass.getMethod("release").invoke(grabber); } catch (Throwable ignored) {}
                    }
                    hardwareStatusMessage = "CAMERA #" + idx + " UNAVAILABLE (" + inner.getClass().getSimpleName() + ")";
                }
            }
        } catch (ClassNotFoundException e) {
            hardwareStatusMessage = "RUN VIA MAVEN: mvn clean compile exec:java";
        } catch (Throwable t) {
            hardwareStatusMessage = "OPENCV INIT ERROR: " + t.getMessage();
        }

        // Strategy 2 (Fallback): Sarxos Webcam-Capture
        try {
            Class<?> webcamClass = Class.forName("com.github.sarxos.webcam.Webcam");
            List<?> cams = (List<?>) webcamClass.getMethod("getWebcams").invoke(null);
            if (cams != null && !cams.isEmpty()) {
                Object cam = cams.get(Math.floorMod(cameraIndex, cams.size()));
                Boolean isOpen = (Boolean) webcamClass.getMethod("isOpen").invoke(cam);
                if (!isOpen) {
                    try {
                        Dimension[] sizes = (Dimension[]) webcamClass.getMethod("getViewSizes").invoke(cam);
                        if (sizes != null && sizes.length > 0) {
                            Dimension best = sizes[sizes.length - 1];
                            webcamClass.getMethod("setViewSize", Dimension.class).invoke(cam, best);
                        }
                    } catch (Throwable ignored) {}
                    webcamClass.getMethod("open").invoke(cam);
                }
                BufferedImage verify = (BufferedImage) webcamClass.getMethod("getImage").invoke(cam);
                if (verify != null && verify.getWidth() > 0) {
                    this.activeGrabber = cam;
                    this.isCameraConnected = true;
                    this.hardwareStatusMessage = "ONLINE: " + cam;
                    notifyCameraState();
                    return;
                } else {
                    try { webcamClass.getMethod("close").invoke(cam); } catch (Throwable ignored) {}
                    hardwareStatusMessage = "CAMERA RETURNED EMPTY FRAME";
                }
            } else if (!hardwareStatusMessage.contains("MAVEN")) {
                hardwareStatusMessage = "NO CAMERAS DETECTED BY OS";
            }
        } catch (ClassNotFoundException e) {
            hardwareStatusMessage = "RUN VIA MAVEN: mvn clean compile exec:java";
        } catch (Throwable t) {
            hardwareStatusMessage = "WEBCAM BUSY / PERMISSION DENIED: " + t.getMessage();
        }

        this.isCameraConnected = false;
        notifyCameraState();
    }

    private BufferedImage convertOpenCvGrab(Object grabber) throws Exception {
        Object frameObj = grabber.getClass().getMethod("grab").invoke(grabber);
        if (frameObj == null) return null;
        if (cachedFrameConverter == null) {
            cachedFrameConverter = Class.forName("org.bytedeco.javacv.Java2DFrameConverter")
                    .getConstructor().newInstance();
        }
        return (BufferedImage) cachedFrameConverter.getClass()
                .getMethod("getBufferedImage", Class.forName("org.bytedeco.javacv.Frame"))
                .invoke(cachedFrameConverter, frameObj);
    }

    private BufferedImage grabHardwareFrame() {
        if (activeGrabber == null) return null;
        try {
            if (activeGrabber.getClass().getName().contains("OpenCVFrameGrabber")) {
                return convertOpenCvGrab(activeGrabber);
            }
            if (activeGrabber.getClass().getName().contains("Webcam")) {
                return (BufferedImage) activeGrabber.getClass().getMethod("getImage").invoke(activeGrabber);
            }
        } catch (Throwable e) {
            isCameraConnected = false;
            hardwareStatusMessage = "STREAM INTERRUPTED: " + e.getMessage();
        }
        return null;
    }

    private void releaseCameraHardware() {
        if (activeGrabber == null) return;
        try {
            if (activeGrabber.getClass().getName().contains("OpenCVFrameGrabber")) {
                try { activeGrabber.getClass().getMethod("stop").invoke(activeGrabber); } catch (Throwable ignored) {}
                try { activeGrabber.getClass().getMethod("release").invoke(activeGrabber); } catch (Throwable ignored) {}
            } else if (activeGrabber.getClass().getName().contains("Webcam")) {
                activeGrabber.getClass().getMethod("close").invoke(activeGrabber);
            }
        } catch (Throwable ignored) {}
        activeGrabber = null;
        isCameraConnected = false;
    }

    public void addListener(DetectionListener l) { listeners.add(l); }
    public void removeListener(DetectionListener l) { listeners.remove(l); }

    public void startCameraStream() {
        userPausedCamera = false;
        hardwareStatusMessage = "CONNECTING CAMERA #" + cameraIndex + "...";
        reconnectRequested.set(true);
        notifyCameraState();
    }

    public void stopCameraStream() {
        userPausedCamera = true;
        hardwareStatusMessage = "CAMERA STANDBY";
        reconnectRequested.set(true);
        notifyCameraState();
    }

    public void toggleCamera() {
        if (!userPausedCamera) {
            stopCameraStream();
        } else {
            startCameraStream();
        }
    }

    public void switchCamera() {
        cameraIndex = (cameraIndex + 1) % 3;
        userPausedCamera = false;
        hardwareStatusMessage = "SWITCHING TO CAMERA #" + cameraIndex + "...";
        reconnectRequested.set(true);
        notifyCameraState();
    }

    private void notifyCameraState() {
        boolean live = isCameraConnected && !userPausedCamera;
        SwingUtilities.invokeLater(() -> {
            for (DetectionListener l : listeners) {
                l.onCameraStateChanged(live, isAiDetectionActive);
            }
        });
    }

    public boolean isCameraConnected() { return isCameraConnected && !userPausedCamera; }
    public boolean isUserPaused() { return userPausedCamera; }
    public boolean isAiDetectionActive() { return isAiDetectionActive; }
    public String getHardwareStatusMessage() { return hardwareStatusMessage; }
    public double getCurrentFps() { return currentFps; }
    public BufferedImage getCurrentFrame() { return currentFrame; }
    public String getYoloStatus() { return yoloDetector.getStatus(); }
    public List<HazardItem> getDetectedHazards() { return Collections.unmodifiableList(detectedHazards); }
    public void setConfidenceThreshold(float t) { this.confidenceThreshold = t; }
}
