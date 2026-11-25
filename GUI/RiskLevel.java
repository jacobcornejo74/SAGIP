package GUI;

import java.awt.*;

enum RiskLevel {
    LOW(
        "LOW RISK",
        new Color(104, 190, 116),
        new Color(52, 132, 68),
        1.8f
    ),
    MODERATE(
        "MEDIUM RISK",
        new Color(240, 196, 60),
        new Color(190, 142, 16),
        2.2f
    ),
    HIGH(
        "HIGH RISK",
        new Color(243, 145, 55),
        new Color(204, 104, 28),
        2.6f
    ),
    CRITICAL(
        "CRITICAL RISK",
        new Color(226, 72, 63),
        new Color(184, 48, 42),
        3.2f
    );

    private final String label;
    private final Color darkColor;
    private final Color lightColor;
    private final float strokeWidth;

    RiskLevel(String label, Color darkColor, Color lightColor, float strokeWidth) {
        this.label = label;
        this.darkColor = darkColor;
        this.lightColor = lightColor;
        this.strokeWidth = strokeWidth;
    }

    public String getLabel() { return label; }
    public Color getStrokeColor() { return darkColor; }
    public Color getColor(boolean darkMode) { return darkMode ? darkColor : lightColor; }
    public float getStrokeWidth() { return strokeWidth; }
}
