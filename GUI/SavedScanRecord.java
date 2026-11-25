package GUI;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

class SavedScanRecord {
    final String id;
    final String timestamp;
    final String overallSeverity;
    final String overallLabel;
    final int detectionCount;
    final String imageFile;
    final BufferedImage snapshot;
    final List<SavedDetectionEntry> detections;

    SavedScanRecord(String id, String timestamp, String overallSeverity, String overallLabel,
                    int detectionCount, String imageFile, BufferedImage snapshot,
                    List<SavedDetectionEntry> detections) {
        this.id = id;
        this.timestamp = timestamp;
        this.overallSeverity = overallSeverity;
        this.overallLabel = overallLabel;
        this.detectionCount = detectionCount;
        this.imageFile = imageFile;
        this.snapshot = snapshot;
        this.detections = detections;
    }

    RiskLevel resolveRiskLevel() {
        try {
            return RiskLevel.valueOf(overallSeverity);
        } catch (Exception ignored) {
            return RiskLevel.LOW;
        }
    }

    String formatSummaries(int maxItems) {
        List<String> parts = new ArrayList<>();
        int limit = Math.min(maxItems, detections.size());
        for (int i = 0; i < limit; i++) {
            SavedDetectionEntry d = detections.get(i);
            parts.add(String.format("[%s] %s (%d%%)", d.tag, d.label, d.confidence));
        }
        if (detections.size() > maxItems) {
            parts.add("+" + (detections.size() - maxItems) + " more");
        }
        return String.join("  &bull;  ", parts);
    }
}
