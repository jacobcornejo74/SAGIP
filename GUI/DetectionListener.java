package GUI;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

interface DetectionListener {
    void onFrameUpdated(BufferedImage frame, double currentFps, boolean isLive, String engineTelemetry);
    void onHazardsDetected(List<HazardItem> hazards);
    void onCameraStateChanged(boolean isConnected, boolean isDetecting);
}
