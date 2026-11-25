package GUI;

import java.util.*;
import java.util.List;

class SavedDetectionEntry {
    final String label;
    final String tag;
    final int confidence;
    final String severity;
    final double x;
    final double y;
    final double width;
    final double height;
    final List<String> risks;

    SavedDetectionEntry(String label, String tag, int confidence, String severity,
                        double x, double y, double width, double height, List<String> risks) {
        this.label = label;
        this.tag = tag;
        this.confidence = confidence;
        this.severity = severity;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.risks = risks;
    }
}
