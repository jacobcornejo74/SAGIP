package GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * Renders live natural RGB webcam frames with color-coded YOLO risk bounding boxes
 * and risk lists below each box, or the CameraPlaceholder graphic when in standby.
 */
class CameraViewportPanel extends JPanel {
    private final boolean darkMode;
    private BufferedImage currentFrame = null;
    private boolean isCameraConnected = false;
    private boolean isAiDetectionActive = true;
    private boolean showPlaceholder = true;
    private String statusHint = "Live camera view and hazard boxes will appear here.";
    private List<HazardItem> currentHazards = new ArrayList<>();
    private DisasterScenario currentScenario;

    public CameraViewportPanel(boolean darkMode) {
        this.darkMode = darkMode;
        setOpaque(false);
        setMinimumSize(new Dimension(360, 260));
        setPreferredSize(new Dimension(800, 480));
    }

    public void setStandby(String hint) {
        this.showPlaceholder = true;
        this.currentFrame = null;
        this.statusHint = (hint != null && !hint.isEmpty())
                ? hint
                : "Live camera view and hazard boxes will appear here.";
        repaint();
    }

    public void updateFeed(BufferedImage frame, boolean cameraConnected, boolean aiActive,
                           List<HazardItem> hazards, DisasterScenario scenario, String fallbackHint) {
        this.currentFrame = frame;
        this.isCameraConnected = cameraConnected;
        this.isAiDetectionActive = aiActive;
        this.currentHazards = (hazards != null) ? hazards : new ArrayList<>();
        this.currentScenario = scenario;
        this.showPlaceholder = (frame == null);
        if (fallbackHint != null && !fallbackHint.isEmpty()) {
            this.statusHint = fallbackHint;
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        if (!showPlaceholder && currentFrame != null) {
            g2.drawImage(currentFrame, 0, 0, w, h, null);

            if (isCameraConnected && isAiDetectionActive && !currentHazards.isEmpty()) {
                drawYoloBoxes(g2, w, h);
            }
        } else {
            drawPlaceholder(g2, w, h);
        }

        // Accent border
        g2.setColor(SagipTheme.accent(darkMode));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(1, 1, w - 3, h - 3);

        g2.dispose();
    }

    private void drawPlaceholder(Graphics2D g2, int w, int h) {
        g2.setColor(SagipTheme.preview(darkMode));
        g2.fillRect(0, 0, w, h);

        Color muted = SagipTheme.muted(darkMode);
        g2.setColor(new Color(muted.getRed(), muted.getGreen(), muted.getBlue(), 26));
        g2.setStroke(new BasicStroke(1.2f));
        for (int x = -h; x < w; x += 28) {
            g2.drawLine(x, h, x + h, 0);
        }

        int cx = w / 2;
        int cy = h / 2 - 30;
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(muted);
        g2.draw(new RoundRectangle2D.Float(cx - 30, cy - 16, 60, 40, 8, 8));
        g2.draw(new Ellipse2D.Float(cx - 12, cy - 8, 24, 24));
        g2.drawLine(cx - 10, cy - 21, cx + 10, cy - 21);

        drawCentered(g2, "CAMERA PREVIEW", new Font(SagipTheme.FONT_FAMILY, Font.BOLD, 20), muted, w, cy + 62);
        drawCentered(g2, statusHint, new Font(SagipTheme.FONT_FAMILY, Font.PLAIN, 15), SagipTheme.text(darkMode), w, cy + 88);
    }

    private void drawCentered(Graphics2D g, String s, Font f, Color c, int width, int baselineY) {
        g.setFont(f);
        g.setColor(c);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, (width - fm.stringWidth(s)) / 2, baselineY);
    }

    public BufferedImage captureAnnotatedSnapshot() {
        if (currentFrame == null) {
            return null;
        }
        int w = (getWidth() > 0) ? getWidth() : currentFrame.getWidth();
        int h = (getHeight() > 0) ? getHeight() : currentFrame.getHeight();
        BufferedImage snapshot = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = snapshot.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.drawImage(currentFrame, 0, 0, w, h, null);
        if (isCameraConnected && isAiDetectionActive && !currentHazards.isEmpty()) {
            drawYoloBoxes(g2, w, h);
        }
        g2.dispose();
        return snapshot;
    }

    public static String resolveHazardTag(String objName) {
        String n = objName.toUpperCase();
        if (n.contains("OVEN") || n.contains("TOASTER") || n.contains("MICROWAVE")
                || n.contains("LAPTOP") || n.contains("CELL PHONE") || n.contains("TV")
                || n.contains("BOOK") || n.contains("BACKPACK") || n.contains("HAIR DRIER")) {
            return "F";
        }
        return "E";
    }

    private void drawYoloBoxes(Graphics2D g2, int w, int h) {
        for (HazardItem item : currentHazards) {
            Rectangle2D.Double norm = item.getNormalizedBoundingBox();
            int bx = (int) (norm.x * w), by = (int) (norm.y * h);
            int bw = (int) (norm.width * w), bh = (int) (norm.height * h);

            RiskLevel risk = (currentScenario != null)
                    ? currentScenario.getHazardRisk(item)
                    : RiskLevel.MODERATE;

            Color boxCol = risk.getColor(darkMode);

            // Crisp risk-colored bounding box
            g2.setColor(boxCol);
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawRect(bx, by, bw, bh);

            // Corner brackets
            int bLen = Math.min(18, Math.min(bw / 3, bh / 3));
            g2.setStroke(new BasicStroke(risk.getStrokeWidth() + 0.6f));
            g2.drawLine(bx, by, bx + bLen, by);
            g2.drawLine(bx, by, bx, by + bLen);
            g2.drawLine(bx + bw, by, bx + bw - bLen, by);
            g2.drawLine(bx + bw, by, bx + bw, by + bLen);
            g2.drawLine(bx, by + bh, bx + bLen, by + bh);
            g2.drawLine(bx, by + bh, bx, by + bh - bLen);
            g2.drawLine(bx + bw, by + bh, bx + bw - bLen, by + bh);
            g2.drawLine(bx + bw, by + bh, bx + bw, by + bh - bLen);

            // Risk list directly below the detected box
            String objName = item.getObjectName();
            String tag = resolveHazardTag(objName);
            String title = String.format("[%s] %s (%d%%)", tag, objName, item.getConfidencePercentage());
            String[] risks = new String[]{
                "may fall during an earthquake",
                "Uneven surface that may caused floods",
                "combustible objects"
            };

            g2.setFont(SagipTheme.FONT_MONO_SMALL);
            FontMetrics fm = g2.getFontMetrics();
            int maxLineW = fm.stringWidth(title);
            for (String r : risks) {
                int rw = fm.stringWidth("\u2022 " + r);
                if (rw > maxLineW) maxLineW = rw;
            }

            int cardPadX = 10, cardPadY = 8, lineH = 16;
            int cardW = maxLineW + cardPadX * 2 + 12;
            int cardH = cardPadY * 2 + 22 + (risks.length + 1) * lineH;

            int cardX = Math.max(4, Math.min(bx, w - cardW - 8));
            int cardY = (by + bh + cardH + 6 <= h) ? (by + bh + 6) : Math.max(4, by - cardH - 6);

            g2.setColor(new Color(29, 34, 36, 236));
            g2.fillRect(cardX, cardY, cardW, cardH);
            g2.setColor(boxCol);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRect(cardX, cardY, cardW, cardH);

            g2.setColor(boxCol);
            g2.fillRect(cardX, cardY, cardW, 22);
            g2.setColor(new Color(29, 34, 36));
            g2.setFont(SagipTheme.FONT_MONO_MED);
            g2.drawString(title, cardX + cardPadX, cardY + 16);

            g2.setColor(new Color(205, 198, 180));
            g2.setFont(SagipTheme.FONT_MONO_SMALL);
            int textY = cardY + 22 + 16;
            g2.drawString("risks:", cardX + cardPadX, textY);
            textY += lineH;

            g2.setColor(new Color(247, 241, 226));
            for (String r : risks) {
                g2.drawString("\u2022 " + r, cardX + cardPadX, textY);
                textY += lineH;
            }
        }
    }
}
