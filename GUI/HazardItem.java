package GUI;

import java.awt.geom.Rectangle2D;
import java.util.*;

/**
 * Encapsulated detected YOLO object & hazard entity.
 */
class HazardItem {
    private final String id;
    private final String objectName;
    private double confidence;
    private final Rectangle2D.Double normalizedBoundingBox;
    private final Map<String, String> scenarioExplanations;
    private final Map<String, RiskLevel> scenarioRisks;
    private String defaultWarning;

    public HazardItem(String id, String objectName, double confidence,
                      double normX, double normY, double normW, double normH) {
        this.id = Objects.requireNonNull(id);
        this.objectName = Objects.requireNonNull(objectName);
        setConfidence(confidence);
        this.normalizedBoundingBox = new Rectangle2D.Double(normX, normY, normW, normH);
        this.scenarioExplanations = new HashMap<>();
        this.scenarioRisks = new HashMap<>();
        this.defaultWarning = "Physical object localized within active camera field.";
    }

    public void registerScenarioContext(String scenarioName, RiskLevel risk, String explanation) {
        scenarioRisks.put(scenarioName.toUpperCase(), risk);
        scenarioExplanations.put(scenarioName.toUpperCase(), explanation);
    }

    public String getId() { return id; }
    public String getObjectName() { return objectName; }
    public double getConfidence() { return confidence; }
    public int getConfidencePercentage() { return (int) Math.round(confidence * 100.0); }

    public void setConfidence(double confidence) {
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
    }

    public Rectangle2D.Double getNormalizedBoundingBox() {
        return new Rectangle2D.Double(normalizedBoundingBox.x, normalizedBoundingBox.y,
                                      normalizedBoundingBox.width, normalizedBoundingBox.height);
    }

    public RiskLevel getRiskForScenario(String scenarioName) {
        return scenarioRisks.getOrDefault(scenarioName.toUpperCase(), RiskLevel.MODERATE);
    }

    public String getExplanationForScenario(String scenarioName) {
        return scenarioExplanations.getOrDefault(scenarioName.toUpperCase(), defaultWarning);
    }

    public String getDefaultWarning() { return defaultWarning; }
    public void setDefaultWarning(String defaultWarning) { this.defaultWarning = defaultWarning; }
}
