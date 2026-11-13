// package disaster.hazard;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SAGIP - Disaster Hazard Detection System
 * Object-Oriented Design:
 *
 * 1. Encapsulation:
 *    - HazardItem stores detection label, confidence, bounding box, and risks in private fields.
 *    - CameraDetectionEngine manages webcam hardware, worker threads, and frame buffers privately.
 *    - YoloOnnxDetector isolates ONNX session loading, image tensor preparation, and NMS math.
 *    - SagipTheme groups dark and light color palettes behind helper methods.
 *
 * 2. Abstraction:
 *    - DisasterScenario defines abstract risk evaluation methods implemented by each disaster type.
 *    - DetectionListener decouples camera frame updates from user interface screens.
 *    - ScreenNavigator and CameraControlListener define clear contracts between screens and hardware.
 *
 * 3. Inheritance:
 *    - EarthquakeScenario, FloodScenario, and FireScenario extend DisasterScenario.
 *    - CameraPage, CameraViewportPanel, MenuBackground, and ContrastPanel extend JPanel.
 *
 * 4. Polymorphism:
 *    - DisasterScenario references invoke subclass-specific risk evaluation methods at runtime.
 */
public class DisasterHazardSystemApp extends JFrame implements ScreenNavigator {

    public static final String SCREEN_HOME = "home";
    public static final String SCREEN_CAMERA = "camera";
    public static final String SCREEN_STORAGE = "storage";
    public static final String SCREEN_MANUAL = "manual";

    private CardLayout cardLayout;
    private JPanel screens;
    private boolean darkMode = true;
    private CameraPage cameraPage;
    private final List<SavedScanRecord> savedAssessments = new ArrayList<>();

    public DisasterHazardSystemApp() {
        super("SAGIP");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 600));
        setSize(1180, 740);
        setLocationRelativeTo(null);

        rebuildScreens(SCREEN_HOME);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                CameraDetectionEngine.getInstance().shutdown();
            }
        });
    }

    private void rebuildScreens(String selectedScreen) {
        if (cameraPage != null) {
            cameraPage.onScreenHidden();
        }

        cardLayout = new CardLayout();
        screens = new JPanel(cardLayout);

        cameraPage = new CameraPage(darkMode, this);
        cameraPage.setBackAction(this::navigateToMainMenu);

        screens.add(createHomeScreen(), SCREEN_HOME);
        screens.add(cameraPage, SCREEN_CAMERA);
        screens.add(createStorageScreen(), SCREEN_STORAGE);
        screens.add(createManualScreen(), SCREEN_MANUAL);

        setContentPane(screens);
        cardLayout.show(screens, selectedScreen);
        if (SCREEN_CAMERA.equals(selectedScreen)) {
            cameraPage.onScreenDisplayed();
        }
        revalidate();
        repaint();
    }

    @Override
    public void navigateToMainMenu() {
        if (cameraPage != null) {
            cameraPage.stopCamera();
            cameraPage.onScreenHidden();
        }
        cardLayout.show(screens, SCREEN_HOME);
    }

    @Override
    public void navigateToLiveDetection() {
        cardLayout.show(screens, SCREEN_CAMERA);
        cameraPage.onScreenDisplayed();
    }

    public void navigateToStorage() {
        rebuildScreens(SCREEN_STORAGE);
    }

    public void navigateToManual() {
        cardLayout.show(screens, SCREEN_MANUAL);
    }

    public void toggleTheme() {
        darkMode = !darkMode;
        rebuildScreens(SCREEN_HOME);
    }

    public void saveAssessment(BufferedImage snapshot, List<HazardItem> hazards, RiskLevel overall) {
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        List<String> summary = new ArrayList<>();
        for (HazardItem item : hazards) {
            summary.add(String.format("%s (%d%%)", item.getObjectName(), item.getConfidencePercentage()));
        }
        savedAssessments.add(0, new SavedScanRecord(time, overall, hazards.size(), snapshot, summary));
    }

    private JComponent createHomeScreen() {
        MenuBackground background = new MenuBackground(darkMode);
        background.setLayout(new BorderLayout());
        background.setBorder(new EmptyBorder(25, 30, 22, 30));

        JLabel edition = SagipTheme.label("SMART INCIDENT PREVENTION", 11, SagipTheme.muted(darkMode), Font.BOLD);
        edition.setHorizontalAlignment(SwingConstants.RIGHT);
        background.add(edition, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        JPanel menu = new ContrastPanel(darkMode);
        menu.setOpaque(false);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBorder(new EmptyBorder(22, 32, 22, 32));

        JLabel title = SagipTheme.label("SAGIP", 58, SagipTheme.text(darkMode), Font.BOLD);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel subtitle = SagipTheme.label("SMART GUARD FOR INCIDENT PREVENTION", 12, SagipTheme.muted(darkMode), Font.PLAIN);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        menu.add(title);
        menu.add(Box.createVerticalStrut(3));
        menu.add(subtitle);
        menu.add(Box.createVerticalStrut(34));
        menu.add(menuButton("START", "Scan a space for hazards", this::navigateToLiveDetection));
        menu.add(Box.createVerticalStrut(9));
        menu.add(menuButton("STORAGE", "View saved assessments (" + savedAssessments.size() + ")", this::navigateToStorage));
        menu.add(Box.createVerticalStrut(9));
        menu.add(menuButton("THEME  \u00B7  " + (darkMode ? "DARK" : "LIGHT"),
                "Switch the appearance", this::toggleTheme));
        menu.add(Box.createVerticalStrut(9));
        menu.add(menuButton("MANUAL", "Learn how to use SAGIP", this::navigateToManual));

        JPanel menuHolder = new JPanel(new GridBagLayout());
        menuHolder.setOpaque(false);
        menuHolder.add(menu);
        center.add(menuHolder, BorderLayout.CENTER);
        background.add(center, BorderLayout.CENTER);

        JLabel footer = SagipTheme.label("SAGIP  \u2022  FIRE & EARTHQUAKE PREPAREDNESS", 10, SagipTheme.text(darkMode), Font.BOLD);
        footer.setHorizontalAlignment(SwingConstants.CENTER);
        background.add(footer, BorderLayout.SOUTH);
        return background;
    }

    private JButton menuButton(String title, String detail, Runnable action) {
        JButton button = new JButton("<html><div style='text-align:center;'>"
                + "<span style='font-family:Segoe UI;font-size:14pt;font-weight:700;color:"
                + SagipTheme.htmlColor(SagipTheme.text(darkMode)) + ";'>"
                + title + "</span><br>"
                + "<span style='font-family:Segoe UI;font-size:9pt;font-weight:400;color:"
                + SagipTheme.htmlColor(SagipTheme.muted(darkMode)) + ";'>"
                + detail + "</span></div></html>");
        button.setFont(new Font(SagipTheme.FONT_FAMILY, Font.BOLD, 14));
        button.setForeground(SagipTheme.text(darkMode));
        button.setBackground(darkMode ? new Color(48, 53, 56) : new Color(244, 235, 215));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(darkMode ? new Color(18, 23, 28) : new Color(99, 78, 55), 1),
                new EmptyBorder(7, 20, 7, 20)));
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(360, 62));
        button.setPreferredSize(new Dimension(360, 62));
        button.addActionListener(event -> action.run());
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                button.setBackground(SagipTheme.hover(darkMode));
            }

            @Override
            public void mouseExited(MouseEvent event) {
                button.setBackground(darkMode ? new Color(48, 53, 56) : new Color(244, 235, 215));
            }
        });
        return button;
    }

    private JComponent createStorageScreen() {
        JPanel page = createSubPage("STORAGE", "Saved assessments");
        if (savedAssessments.isEmpty()) {
            JPanel content = new JPanel();
            content.setOpaque(false);
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBorder(new EmptyBorder(30, 30, 30, 30));

            JLabel icon = SagipTheme.label("\u25A3", 42, SagipTheme.accent(darkMode), Font.BOLD);
            icon.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel empty = SagipTheme.label("No saved assessments yet.", 17, SagipTheme.text(darkMode), Font.BOLD);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel detail = SagipTheme.label("Completed scans will appear here.", 12, SagipTheme.muted(darkMode), Font.PLAIN);
            detail.setAlignmentX(Component.CENTER_ALIGNMENT);

            content.add(Box.createVerticalGlue());
            content.add(icon);
            content.add(Box.createVerticalStrut(12));
            content.add(empty);
            content.add(Box.createVerticalStrut(6));
            content.add(detail);
            content.add(Box.createVerticalGlue());
            page.add(content, BorderLayout.CENTER);
        } else {
            JPanel list = new JPanel();
            list.setOpaque(false);
            list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
            list.setBorder(new EmptyBorder(24, 36, 24, 36));
            for (SavedScanRecord rec : savedAssessments) {
                JPanel row = new JPanel(new BorderLayout(16, 0));
                row.setBackground(SagipTheme.surface(darkMode));
                row.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(SagipTheme.cardLine(darkMode), 1),
                        new EmptyBorder(14, 18, 14, 18)));
                row.setMaximumSize(new Dimension(820, 110));

                JPanel info = new JPanel();
                info.setOpaque(false);
                info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
                info.add(SagipTheme.label(rec.overall.getLabel() + "  ·  " + rec.detectionCount + " Detections",
                        14, rec.overall.getColor(darkMode), Font.BOLD));
                info.add(Box.createVerticalStrut(4));
                info.add(SagipTheme.label(rec.timestamp, 12, SagipTheme.muted(darkMode), Font.PLAIN));
                if (!rec.objectSummaries.isEmpty()) {
                    info.add(Box.createVerticalStrut(6));
                    info.add(SagipTheme.label(String.join(", ", rec.objectSummaries), 12, SagipTheme.text(darkMode), Font.PLAIN));
                }
                row.add(info, BorderLayout.CENTER);
                list.add(row);
                list.add(Box.createVerticalStrut(12));
            }
            JScrollPane scroll = new JScrollPane(list);
            scroll.setBorder(null);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            page.add(scroll, BorderLayout.CENTER);
        }
        return page;
    }

    private JComponent createManualScreen() {
        JPanel page = createSubPage("MANUAL", "Quick guide");
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(35, 55, 35, 55));
        content.add(guideItem("1", "Start", "Open a new assessment and point the camera at the space."));
        content.add(Box.createVerticalStrut(18));
        content.add(guideItem("2", "Review", "Check detected fire, flood, or earthquake hazards and their severity."));
        content.add(Box.createVerticalStrut(18));
        content.add(guideItem("3", "Respond", "Follow the suggested corrective actions and save the assessment."));
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    private JPanel createSubPage(String title, String subtitle) {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(SagipTheme.background(darkMode));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(SagipTheme.surface(darkMode));
        header.setBorder(new EmptyBorder(18, 24, 18, 24));
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(SagipTheme.label(title, 18, SagipTheme.text(darkMode), Font.BOLD));
        titles.add(Box.createVerticalStrut(3));
        titles.add(SagipTheme.label(subtitle, 11, SagipTheme.muted(darkMode), Font.PLAIN));
        header.add(titles, BorderLayout.WEST);
        header.add(SagipTheme.pageButton("BACK TO MENU", darkMode, this::navigateToMainMenu), BorderLayout.EAST);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(SagipTheme.surface(darkMode));
        footer.setBorder(new EmptyBorder(10, 24, 10, 24));
        footer.add(SagipTheme.label("SAGIP  \u2022  FIRE & EARTHQUAKE PREPAREDNESS", 10, SagipTheme.muted(darkMode), Font.BOLD),
                BorderLayout.WEST);

        page.add(header, BorderLayout.NORTH);
        page.add(footer, BorderLayout.SOUTH);
        return page;
    }

    private JComponent guideItem(String number, String title, String detail) {
        JPanel row = new JPanel(new BorderLayout(16, 0));
        row.setOpaque(true);
        row.setBackground(SagipTheme.surface(darkMode));
        row.setBorder(new EmptyBorder(18, 20, 18, 20));
        row.setMaximumSize(new Dimension(760, 92));
        JLabel step = SagipTheme.label(number, 24, SagipTheme.accent(darkMode), Font.BOLD);
        step.setHorizontalAlignment(SwingConstants.CENTER);
        step.setPreferredSize(new Dimension(42, 42));
        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        words.add(SagipTheme.label(title, 15, SagipTheme.text(darkMode), Font.BOLD));
        words.add(Box.createVerticalStrut(6));
        words.add(SagipTheme.label("<html>" + detail + "</html>", 12, SagipTheme.muted(darkMode), Font.PLAIN));
        row.add(step, BorderLayout.WEST);
        row.add(words, BorderLayout.CENTER);
        return row;
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new DisasterHazardSystemApp().setVisible(true);
        });
    }
}

class SavedScanRecord {
    final String timestamp;
    final RiskLevel overall;
    final int detectionCount;
    final BufferedImage snapshot;
    final List<String> objectSummaries;

    SavedScanRecord(String timestamp, RiskLevel overall, int detectionCount,
                    BufferedImage snapshot, List<String> objectSummaries) {
        this.timestamp = timestamp;
        this.overall = overall;
        this.detectionCount = detectionCount;
        this.snapshot = snapshot;
        this.objectSummaries = objectSummaries;
    }
}

// ------------------------------------------------------------------------------
// 1. SAGIP THEME CONSTANTS (ENCAPSULATION)
// ------------------------------------------------------------------------------
final class SagipTheme {
    private SagipTheme() {}

    public static final String FONT_FAMILY = "Segoe UI";
    public static final Font FONT_MONO_MED   = new Font("Monospaced", Font.BOLD, 12);
    public static final Font FONT_MONO_SMALL = new Font("Monospaced", Font.PLAIN, 11);

    public static Color background(boolean darkMode) {
        return darkMode ? new Color(29, 34, 36) : new Color(241, 235, 220);
    }

    public static Color surface(boolean darkMode) {
        return darkMode ? new Color(42, 48, 49) : new Color(224, 214, 193);
    }

    public static Color text(boolean darkMode) {
        return darkMode ? new Color(247, 241, 226) : new Color(49, 42, 34);
    }

    public static Color muted(boolean darkMode) {
        return darkMode ? new Color(205, 198, 180) : new Color(104, 91, 73);
    }

    public static Color accent(boolean darkMode) {
        return darkMode ? new Color(243, 145, 55) : new Color(190, 94, 37);
    }

    public static Color hover(boolean darkMode) {
        return darkMode ? new Color(104, 72, 49) : new Color(255, 190, 105);
    }

    public static Color preview(boolean darkMode) {
        return darkMode ? new Color(31, 34, 33) : new Color(229, 222, 208);
    }

    public static Color cardLine(boolean darkMode) {
        return darkMode ? new Color(255, 255, 255, 42) : new Color(65, 47, 33, 55);
    }

    public static JLabel label(String text, int size, Color color, int style) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(FONT_FAMILY, style, size));
        label.setForeground(color);
        return label;
    }

    public static JButton pageButton(String text, boolean darkMode, Runnable action) {
        JButton button = new JButton(text);
        button.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
        button.setForeground(text(darkMode));
        button.setBackground(surface(darkMode));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent(darkMode), 1),
                new EmptyBorder(10, 16, 10, 16)));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(event -> action.run());
        button.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { button.setBackground(hover(darkMode)); }
            @Override public void mouseExited(MouseEvent e) { button.setBackground(surface(darkMode)); }
        });
        return button;
    }

    public static String htmlColor(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
}

// ------------------------------------------------------------------------------
// 2. MODEL LAYER (ENCAPSULATION, ABSTRACTION, INHERITANCE, POLYMORPHISM)
// ------------------------------------------------------------------------------
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

/**
 * ABSTRACTION: Base contract for disaster scenarios.
 */
abstract class DisasterScenario {
    private final String name;
    private final String description;
    private final String primaryConcern;

    protected DisasterScenario(String name, String description, String primaryConcern) {
        this.name = name;
        this.description = description;
        this.primaryConcern = primaryConcern;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getPrimaryConcern() { return primaryConcern; }

    public abstract RiskLevel evaluateOverallRisk(List<HazardItem> hazards);
    public abstract String getAdvisoryWarning(List<HazardItem> hazards);
    public abstract String getHazardExplanation(HazardItem hazard);
    public abstract RiskLevel getHazardRisk(HazardItem hazard);

    @Override
    public String toString() { return name.toUpperCase() + " PROTOCOL"; }
}

/**
 * INHERITANCE & POLYMORPHISM: Earthquake Scenario
 */
class EarthquakeScenario extends DisasterScenario {
    public EarthquakeScenario() {
        super("Earthquake", "Seismic ground acceleration and structural displacement.", "Toppling mass & shattering glass.");
    }

    @Override
    public RiskLevel evaluateOverallRisk(List<HazardItem> hazards) {
        if (hazards.isEmpty()) return RiskLevel.LOW;
        boolean hasCrit = hazards.stream().anyMatch(h -> getHazardRisk(h) == RiskLevel.CRITICAL);
        if (hasCrit) return RiskLevel.CRITICAL;
        boolean hasHigh = hazards.stream().anyMatch(h -> getHazardRisk(h) == RiskLevel.HIGH);
        return hasHigh ? RiskLevel.HIGH : RiskLevel.MODERATE;
    }

    @Override
    public String getAdvisoryWarning(List<HazardItem> hazards) {
        if (hazards.isEmpty()) return "Field clear of immediate seismic fall or shatter hazards. Maintain open egress.";
        for (HazardItem h : hazards) {
            String n = h.getObjectName().toUpperCase();
            if (n.contains("TV") || n.contains("LAPTOP") || n.contains("MONITOR") || n.contains("REFRIGERATOR")) {
                return "Unanchored heavy equipment (" + h.getObjectName() + ") detected. Secure with seismic straps or lower to floor.";
            }
            if (n.contains("BOTTLE") || n.contains("GLASS") || n.contains("CUP") || n.contains("VASE")) {
                return "Fragile vessel (" + h.getObjectName() + ") detected. Relocate away from elevated edges to prevent glass shrapnel.";
            }
            if (n.contains("CHAIR") || n.contains("COUCH") || n.contains("TABLE") || n.contains("BENCH")) {
                return "Movable furniture (" + h.getObjectName() + ") detected. Verify 36-inch egress path remains unobstructed.";
            }
        }
        return "Objects detected in seismic zone. Maintain drop, cover, and hold-on clearance.";
    }

    @Override
    public String getHazardExplanation(HazardItem hazard) {
        String n = hazard.getObjectName().toUpperCase();
        if (n.contains("PERSON")) return "Occupant inside active monitoring zone; prioritize drop-cover-hold clearance.";
        if (n.contains("TV") || n.contains("LAPTOP") || n.contains("MONITOR")) return "High-mass display unit prone to projectile toppling under lateral tremor.";
        if (n.contains("BOTTLE") || n.contains("CUP") || n.contains("WINE GLASS") || n.contains("VASE")) return "Brittle vessel shatter risk creating laceration debris along exit route.";
        if (n.contains("KNIFE") || n.contains("SCISSORS")) return "Loose sharp implement capable of sliding off surfaces during ground motion.";
        if (n.contains("CHAIR") || n.contains("COUCH") || n.contains("DINING TABLE")) return "Unanchored furniture mass; may slide and jam exit doorways.";
        if (n.contains("BOOK") || n.contains("CLOCK") || n.contains("POTTED PLANT")) return "Overhead or shelf-mounted projectile hazard during shaking.";
        if (n.contains("REFRIGERATOR") || n.contains("OVEN")) return "Heavy appliance capable of tipping or rupturing utility connections.";
        return "Unsecured physical mass subject to displacement during seismic shaking.";
    }

    @Override
    public RiskLevel getHazardRisk(HazardItem hazard) {
        String n = hazard.getObjectName().toUpperCase();
        if (n.contains("KNIFE") || n.contains("SCISSORS") || n.contains("REFRIGERATOR")) return RiskLevel.CRITICAL;
        if (n.contains("TV") || n.contains("LAPTOP") || n.contains("BOTTLE") || n.contains("WINE GLASS") || n.contains("VASE") || n.contains("CUP")) return RiskLevel.HIGH;
        if (n.contains("CHAIR") || n.contains("COUCH") || n.contains("POTTED PLANT") || n.contains("BOOK")) return RiskLevel.MODERATE;
        return RiskLevel.LOW;
    }
}

/**
 * INHERITANCE & POLYMORPHISM: Flood Scenario
 */
class FloodScenario extends DisasterScenario {
    public FloodScenario() {
        super("Flood", "Water inundation and rising conductive surface levels.", "Electrocution & chemical leaching.");
    }

    @Override
    public RiskLevel evaluateOverallRisk(List<HazardItem> hazards) {
        if (hazards.isEmpty()) return RiskLevel.LOW;
        boolean hasCrit = hazards.stream().anyMatch(h -> getHazardRisk(h) == RiskLevel.CRITICAL);
        if (hasCrit) return RiskLevel.CRITICAL;
        boolean hasHigh = hazards.stream().anyMatch(h -> getHazardRisk(h) == RiskLevel.HIGH);
        return hasHigh ? RiskLevel.HIGH : RiskLevel.MODERATE;
    }

    @Override
    public String getAdvisoryWarning(List<HazardItem> hazards) {
        if (hazards.isEmpty()) return "No low-elevation energized electronics or open chemical vessels detected.";
        for (HazardItem h : hazards) {
            String n = h.getObjectName().toUpperCase();
            if (n.contains("TV") || n.contains("LAPTOP") || n.contains("CELL PHONE") || n.contains("MICROWAVE") || n.contains("TOASTER") || n.contains("REFRIGERATOR") || n.contains("KEYBOARD")) {
                return "Energized electronics (" + h.getObjectName() + ") detected! Disconnect main breaker before water contact.";
            }
            if (n.contains("BOTTLE") || n.contains("CUP") || n.contains("SINK")) {
                return "Liquid container or plumbing fixture (" + h.getObjectName() + ") detected. Seal vessels to prevent floodwater contamination.";
            }
        }
        return "Elevate buoyant obstacles to prevent blocked drainage and submerged trip hazards.";
    }

    @Override
    public String getHazardExplanation(HazardItem hazard) {
        String n = hazard.getObjectName().toUpperCase();
        if (n.contains("PERSON")) return "Occupant present; evacuate upward if conductive water enters room.";
        if (n.contains("LAPTOP") || n.contains("TV") || n.contains("MICROWAVE") || n.contains("TOASTER") || n.contains("OVEN") || n.contains("REFRIGERATOR")) {
            return "Energized AC/lithium circuitry creates lethal electrocution path upon inundation.";
        }
        if (n.contains("CELL PHONE") || n.contains("KEYBOARD") || n.contains("MOUSE") || n.contains("REMOTE")) {
            return "Low-voltage battery/peripheral susceptible to short-circuit and lithium outgassing in water.";
        }
        if (n.contains("BOTTLE") || n.contains("VASE") || n.contains("CUP")) {
            return "Container can float, tip, and disperse contents into standing water.";
        }
        if (n.contains("SINK")) return "Backflow vector during municipal storm surge.";
        if (n.contains("CHAIR") || n.contains("COUCH") || n.contains("BACKPACK") || n.contains("BOOK")) {
            return "Porous material absorbs contaminated floodwater and increases structural floor load.";
        }
        return "Submerged obstruction hazard in rising water conditions.";
    }

    @Override
    public RiskLevel getHazardRisk(HazardItem hazard) {
        String n = hazard.getObjectName().toUpperCase();
        if (n.contains("MICROWAVE") || n.contains("TOASTER") || n.contains("OVEN") || n.contains("REFRIGERATOR") || n.contains("TV")) return RiskLevel.CRITICAL;
        if (n.contains("LAPTOP") || n.contains("CELL PHONE") || n.contains("BOTTLE") || n.contains("SINK")) return RiskLevel.HIGH;
        if (n.contains("CHAIR") || n.contains("COUCH") || n.contains("BACKPACK")) return RiskLevel.MODERATE;
        return RiskLevel.LOW;
    }
}

/**
 * INHERITANCE & POLYMORPHISM: Fire Scenario
 */
class FireScenario extends DisasterScenario {
    public FireScenario() {
        super("Fire", "Thermal conflagration, lithium runaway, and smoke toxicity.", "Fuel load & blocked egress.");
    }

    @Override
    public RiskLevel evaluateOverallRisk(List<HazardItem> hazards) {
        if (hazards.isEmpty()) return RiskLevel.LOW;
        boolean hasCrit = hazards.stream().anyMatch(h -> getHazardRisk(h) == RiskLevel.CRITICAL);
        if (hasCrit) return RiskLevel.CRITICAL;
        boolean hasHigh = hazards.stream().anyMatch(h -> getHazardRisk(h) == RiskLevel.HIGH);
        return hasHigh ? RiskLevel.HIGH : RiskLevel.MODERATE;
    }

    @Override
    public String getAdvisoryWarning(List<HazardItem> hazards) {
        if (hazards.isEmpty()) return "Thermal baseline clear. Verify fire extinguisher access and unblocked exit doors.";
        for (HazardItem h : hazards) {
            String n = h.getObjectName().toUpperCase();
            if (n.contains("OVEN") || n.contains("TOASTER") || n.contains("MICROWAVE") || n.contains("HAIR DRIER")) {
                return "High-wattage thermal appliance (" + h.getObjectName() + ") detected! Enforce 36-inch combustible clearance.";
            }
            if (n.contains("LAPTOP") || n.contains("CELL PHONE")) {
                return "Lithium-ion battery device (" + h.getObjectName() + ") detected. Monitor for swelling or thermal runaway.";
            }
            if (n.contains("BOTTLE")) {
                return "Pressurized or liquid vessel (" + h.getObjectName() + ") detected. Isolate solvents/aerosols in fire cabinet.";
            }
        }
        return "Combustible fuel load present in frame. Keep evacuation corridor clear of smoke-producing synthetics.";
    }

    @Override
    public String getHazardExplanation(HazardItem hazard) {
        String n = hazard.getObjectName().toUpperCase();
        if (n.contains("PERSON")) return "Occupant in thermal monitoring zone; ensure immediate smoke egress visibility.";
        if (n.contains("OVEN") || n.contains("TOASTER") || n.contains("MICROWAVE")) return "Primary thermal ignition source capable of igniting adjacent combustibles.";
        if (n.contains("BOTTLE")) return "Potential solvent, alcohol, or pressurized vessel rupture risk under radiant heat.";
        if (n.contains("LAPTOP") || n.contains("CELL PHONE") || n.contains("TV")) return "Lithium cell / capacitor bank vulnerable to self-sustaining thermal runaway.";
        if (n.contains("COUCH") || n.contains("CHAIR") || n.contains("BED") || n.contains("BACKPACK")) return "Synthetic polyurethane foam rapidly accelerates flame spread and dense toxic smoke.";
        if (n.contains("BOOK")) return "Dry cellulose stack acts as sustained solid fuel load.";
        return "Combustible surface contributing to room fire fuel density.";
    }

    @Override
    public RiskLevel getHazardRisk(HazardItem hazard) {
        String n = hazard.getObjectName().toUpperCase();
        if (n.contains("OVEN") || n.contains("TOASTER") || n.contains("MICROWAVE") || n.contains("BOTTLE")) return RiskLevel.CRITICAL;
        if (n.contains("LAPTOP") || n.contains("CELL PHONE") || n.contains("COUCH") || n.contains("BED") || n.contains("BOOK")) return RiskLevel.HIGH;
        if (n.contains("CHAIR") || n.contains("TV") || n.contains("BACKPACK")) return RiskLevel.MODERATE;
        return RiskLevel.LOW;
    }
}

// ------------------------------------------------------------------------------
// 3. REAL YOLOv8 ONNX DETECTOR + HARDWARE WEBCAM ENGINE
// ------------------------------------------------------------------------------
interface DetectionListener {
    void onFrameUpdated(BufferedImage frame, double currentFps, boolean isLive, String engineTelemetry);
    void onHazardsDetected(List<HazardItem> hazards);
    void onCameraStateChanged(boolean isConnected, boolean isDetecting);
}

interface ScreenNavigator {
    void navigateToMainMenu();
    void navigateToLiveDetection();
}

/**
 * Executes real YOLOv8n ONNX neural object detection (80 COCO classes) via
 * Microsoft ONNX Runtime (`ai.onnxruntime`). Uses reflection so the single file
 * also compiles cleanly with plain `javac` even before Maven downloads JARs.
 */
class YoloOnnxDetector {
    private static final String MODEL_FILE = "yolov8n.onnx";
    private static final String[] MODEL_URLS = {
        "https://huggingface.co/cabelo/yolov8/resolve/main/yolov8n.onnx",
        "https://huggingface.co/Kalray/yolov8/resolve/main/yolov8n.onnx"
    };

    private static final String[] COCO_CLASSES = {
        "PERSON", "BICYCLE", "CAR", "MOTORCYCLE", "AIRPLANE", "BUS", "TRAIN", "TRUCK", "BOAT",
        "TRAFFIC LIGHT", "FIRE HYDRANT", "STOP SIGN", "PARKING METER", "BENCH", "BIRD", "CAT",
        "DOG", "HORSE", "SHEEP", "COW", "ELEPHANT", "BEAR", "ZEBRA", "GIRAFFE", "BACKPACK",
        "UMBRELLA", "HANDBAG", "TIE", "SUITCASE", "FRISBEE", "SKIS", "SNOWBOARD", "SPORTS BALL",
        "KITE", "BASEBALL BAT", "BASEBALL GLOVE", "SKATEBOARD", "SURFBOARD", "TENNIS RACKET",
        "BOTTLE", "WINE GLASS", "CUP", "FORK", "KNIFE", "SPOON", "BOWL", "BANANA", "APPLE",
        "SANDWICH", "ORANGE", "BROCCOLI", "CARROT", "HOT DOG", "PIZZA", "DONUT", "CAKE",
        "CHAIR", "COUCH", "POTTED PLANT", "BED", "DINING TABLE", "TOILET", "TV", "LAPTOP",
        "MOUSE", "REMOTE", "KEYBOARD", "CELL PHONE", "MICROWAVE", "OVEN", "TOASTER", "SINK",
        "REFRIGERATOR", "BOOK", "CLOCK", "VASE", "SCISSORS", "TEDDY BEAR", "HAIR DRIER", "TOOTHBRUSH"
    };

    private Object ortEnv = null;
    private Object ortSession = null;
    private String inputName = "images";
    private volatile boolean ready = false;
    private volatile String status = "YOLO INIT";
    private final AtomicBoolean initStarted = new AtomicBoolean(false);

    public String getStatus() { return status; }
    public boolean isReady() { return ready; }

    public void initializeAsync() {
        if (!initStarted.compareAndSet(false, true)) return;
        Thread t = new Thread(() -> {
            try {
                Class<?> envClass = Class.forName("ai.onnxruntime.OrtEnvironment");
                File modelFile = new File(MODEL_FILE);
                if (!modelFile.exists() || modelFile.length() < 1_000_000L) {
                    downloadModel(modelFile.toPath());
                }
                if (!modelFile.exists() || modelFile.length() < 1_000_000L) {
                    status = "YOLOv8 WEIGHTS OFFLINE";
                    return;
                }

                status = "LOADING YOLOv8n ONNX...";
                ortEnv = envClass.getMethod("getEnvironment").invoke(null);
                Class<?> optsClass = Class.forName("ai.onnxruntime.OrtSession$SessionOptions");
                Object opts = optsClass.getConstructor().newInstance();
                ortSession = envClass.getMethod("createSession", String.class, optsClass)
                        .invoke(ortEnv, modelFile.getAbsolutePath(), opts);

                Set<?> names = (Set<?>) ortSession.getClass().getMethod("getInputNames").invoke(ortSession);
                if (names != null && !names.isEmpty()) {
                    inputName = String.valueOf(names.iterator().next());
                }
                ready = true;
                status = "YOLOv8n-ONNX READY";
                System.out.println(">> [YOLO ENGINE] YOLOv8n ONNX Neural Network loaded and active.");
            } catch (ClassNotFoundException e) {
                status = "ONNX JAR PENDING (RUN VIA MAVEN)";
                System.err.println(">> [YOLO NOTICE] ONNX Runtime not on classpath. Run via: mvn clean compile exec:java");
            } catch (Throwable ex) {
                status = "YOLO LOAD ERR";
                System.err.println(">> [YOLO ERROR] " + ex.getMessage());
            }
        }, "YOLOv8-Loader");
        t.setDaemon(true);
        t.start();
    }

    private void downloadModel(Path target) {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        for (String url : MODEL_URLS) {
            try {
                status = "DOWNLOADING YOLOv8n.ONNX...";
                System.out.println(">> [YOLO ENGINE] Downloading YOLOv8n ONNX weights from " + url);
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "Java-Swing-YOLO-Hazard-Detector/3.0")
                        .GET()
                        .build();
                HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
                if (resp.statusCode() == 200) {
                    Path tmp = Path.of(target.toString() + ".part");
                    try (InputStream in = resp.body()) {
                        Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                    }
                    if (Files.size(tmp) > 1_000_000L) {
                        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                        System.out.println(">> [YOLO ENGINE] Saved yolov8n.onnx (" + (Files.size(target) / 1024 / 1024) + " MB)");
                        return;
                    }
                }
            } catch (Exception e) {
                System.err.println(">> [YOLO DOWNLOAD RETRY] " + e.getMessage());
            }
        }
    }

    public List<HazardItem> detect(BufferedImage frame, float confThreshold) {
        if (!ready || ortSession == null || frame == null) return Collections.emptyList();

        Object tensor = null;
        Object result = null;
        try {
            final int SIZE = 640;
            BufferedImage scaled = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = scaled.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(frame, 0, 0, SIZE, SIZE, null);
            g2.dispose();

            int[] rgb = scaled.getRGB(0, 0, SIZE, SIZE, null, 0, SIZE);
            int area = SIZE * SIZE;
            float[] chw = new float[3 * area];
            for (int i = 0; i < area; i++) {
                int p = rgb[i];
                chw[i] = ((p >> 16) & 0xFF) / 255.0f;
                chw[area + i] = ((p >> 8) & 0xFF) / 255.0f;
                chw[2 * area + i] = (p & 0xFF) / 255.0f;
            }

            Class<?> tensorClass = Class.forName("ai.onnxruntime.OnnxTensor");
            Class<?> envClass = Class.forName("ai.onnxruntime.OrtEnvironment");
            tensor = tensorClass.getMethod("createTensor", envClass, FloatBuffer.class, long[].class)
                    .invoke(null, ortEnv, FloatBuffer.wrap(chw), new long[]{1, 3, SIZE, SIZE});

            result = ortSession.getClass().getMethod("run", Map.class)
                    .invoke(ortSession, Collections.singletonMap(inputName, tensor));

            Object onnxVal = result.getClass().getMethod("get", int.class).invoke(result, 0);
            float[][][] out = (float[][][]) onnxVal.getClass().getMethod("getValue").invoke(onnxVal);
            float[][] preds = out[0]; // [84][8400]
            int numAnchors = preds[0].length;
            int numClasses = Math.min(COCO_CLASSES.length, preds.length - 4);

            List<RawBox> candidates = new ArrayList<>();
            for (int a = 0; a < numAnchors; a++) {
                float maxScore = 0f;
                int bestClass = -1;
                for (int c = 0; c < numClasses; c++) {
                    float s = preds[4 + c][a];
                    if (s > maxScore) {
                        maxScore = s;
                        bestClass = c;
                    }
                }
                if (maxScore >= confThreshold && bestClass >= 0) {
                    float cx = preds[0][a];
                    float cy = preds[1][a];
                    float bw = preds[2][a];
                    float bh = preds[3][a];

                    double nx = Math.max(0.0, Math.min(1.0, (cx - bw / 2.0) / SIZE));
                    double ny = Math.max(0.0, Math.min(1.0, (cy - bh / 2.0) / SIZE));
                    double nw = Math.max(0.02, Math.min(1.0 - nx, bw / SIZE));
                    double nh = Math.max(0.02, Math.min(1.0 - ny, bh / SIZE));
                    candidates.add(new RawBox(bestClass, maxScore, nx, ny, nw, nh));
                }
            }

            // Non-Maximum Suppression (NMS)
            candidates.sort((a, b) -> Float.compare(b.score, a.score));
            List<RawBox> kept = new ArrayList<>();
            for (RawBox box : candidates) {
                boolean overlaps = false;
                for (RawBox k : kept) {
                    if (k.classId == box.classId && iou(box, k) > 0.45) {
                        overlaps = true;
                        break;
                    }
                }
                if (!overlaps) {
                    kept.add(box);
                    if (kept.size() >= 10) break;
                }
            }

            List<HazardItem> items = new ArrayList<>();
            int idx = 1;
            for (RawBox k : kept) {
                String label = COCO_CLASSES[k.classId];
                items.add(new HazardItem(
                        String.format("YOLO-%02d", idx++),
                        label,
                        k.score,
                        k.x, k.y, k.w, k.h
                ));
            }
            return items;
        } catch (Throwable t) {
            return Collections.emptyList();
        } finally {
            try { if (result instanceof AutoCloseable) ((AutoCloseable) result).close(); } catch (Exception ignored) {}
            try { if (tensor instanceof AutoCloseable) ((AutoCloseable) tensor).close(); } catch (Exception ignored) {}
        }
    }

    private static double iou(RawBox a, RawBox b) {
        double x1 = Math.max(a.x, b.x);
        double y1 = Math.max(a.y, b.y);
        double x2 = Math.min(a.x + a.w, b.x + b.w);
        double y2 = Math.min(a.y + a.h, b.y + b.h);
        double inter = Math.max(0, x2 - x1) * Math.max(0, y2 - y1);
        double union = a.w * a.h + b.w * b.h - inter;
        return union <= 0 ? 0 : inter / union;
    }

    private static final class RawBox {
        final int classId;
        final float score;
        final double x, y, w, h;
        RawBox(int classId, float score, double x, double y, double w, double h) {
            this.classId = classId;
            this.score = score;
            this.x = x; this.y = y; this.w = w; this.h = h;
        }
    }
}

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

// ------------------------------------------------------------------------------
// 4. SAGIP UI COMPONENTS & CAMERA VIEWPORT (INHERITANCE & POLYMORPHISM)
// ------------------------------------------------------------------------------
class MenuBackground extends JPanel {
    private static final long serialVersionUID = 1L;

    private final boolean darkMode;
    private final javax.swing.Timer animation;
    private int animationFrame;

    public MenuBackground(boolean darkMode) {
        this.darkMode = darkMode;
        setOpaque(true);
        animation = new javax.swing.Timer(45, event -> {
            animationFrame++;
            repaint();
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        animation.start();
    }

    @Override
    public void removeNotify() {
        animation.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int width = getWidth();
        int height = getHeight();
        int skylineBase = (int) (height * 0.77);
        Color skyTop = darkMode ? new Color(18, 29, 48) : new Color(112, 166, 194);
        Color skyBottom = darkMode ? new Color(112, 82, 79) : new Color(246, 190, 143);
        g.setPaint(new GradientPaint(0, 0, skyTop, 0, skylineBase, skyBottom));
        g.fillRect(0, 0, width, skylineBase);

        int sunSize = Math.max(48, height / 11);
        int sunX = (int) (width * 0.79);
        int sunY = (int) (height * 0.19);
        g.setColor(new Color(255, 195, 133, darkMode ? 25 : 45));
        g.fill(new Ellipse2D.Double(sunX - sunSize * 0.7, sunY - sunSize * 0.7,
                sunSize * 2.4, sunSize * 2.4));
        g.setColor(darkMode ? new Color(255, 194, 139) : new Color(255, 231, 183));
        g.fill(new Ellipse2D.Double(sunX, sunY, sunSize, sunSize));

        paintCloud(g, width, height, 0.19, 210, 0);
        paintCloud(g, width, height, 0.32, 155, 1);
        paintCloud(g, width, height, 0.13, 180, 2);

        paintNeighborhood(g, width, height, skylineBase);

        g.setPaint(new GradientPaint(0, skylineBase, darkMode ? new Color(43, 48, 57) : new Color(110, 108, 104),
                0, height, darkMode ? new Color(18, 23, 31) : new Color(65, 68, 70)));
        g.fillRect(0, skylineBase, width, height - skylineBase);
        g.setColor(darkMode ? new Color(255, 183, 111, 100) : new Color(255, 218, 164, 120));
        g.fillRect(0, skylineBase, width, 3);
        g.setColor(darkMode ? new Color(187, 198, 207, 100) : new Color(231, 224, 210, 150));
        g.fill(new Ellipse2D.Double(width * 0.48, skylineBase + height * 0.08,
                width * 0.04, height * 0.018));
        g.fill(new Ellipse2D.Double(width * 0.48, skylineBase + height * 0.15,
                width * 0.04, height * 0.018));
        g.setColor(darkMode ? new Color(255, 191, 119, 115) : new Color(255, 237, 197, 150));
        int lightTravel = (int) (animationFrame * 0.42) % Math.max(1, width + 100);
        g.fill(new Ellipse2D.Double(lightTravel - 50, skylineBase + height * 0.22, 32, 5));
        g.fill(new Ellipse2D.Double(width - lightTravel + 50, skylineBase + height * 0.27, 28, 4));
        g.dispose();
    }

    private void paintNeighborhood(Graphics2D g, int width, int height, int base) {
        int tileWidth = Math.max(145, width / 8);
        int sceneWidth = tileWidth * 6;
        int offset = (int) (animationFrame * 0.32) % sceneWidth;
        int firstX = -sceneWidth + offset;
        int firstIndex = -6;
        for (int x = firstX; x < width + sceneWidth; x += tileWidth) {
            int type = Math.floorMod(firstIndex++, 6);
            switch (type) {
                case 0, 2, 5 -> drawHouse(g, x, base, tileWidth, height, type);
                case 1, 4 -> drawOffice(g, x, base, tileWidth, height, type == 4);
                default -> drawSchool(g, x, base, tileWidth, height);
            }
        }
    }

    private void drawHouse(Graphics2D g, int x, int base, int tileWidth, int height, int variation) {
        int houseWidth = tileWidth * (variation == 2 ? 72 : 80) / 100;
        int houseHeight = (int) (height * (variation == 5 ? 0.15 : 0.18));
        int left = x + (tileWidth - houseWidth) / 2;
        int top = base - houseHeight;
        Color wall = variation % 2 == 0
                ? (darkMode ? new Color(130, 110, 94) : new Color(225, 194, 151))
                : (darkMode ? new Color(121, 119, 104) : new Color(205, 204, 174));
        g.setColor(wall);
        g.fillRect(left, top + houseHeight / 4, houseWidth, houseHeight * 3 / 4);

        Polygon roof = new Polygon(
                new int[] { left - 7, left + houseWidth / 2, left + houseWidth + 7 },
                new int[] { top + houseHeight / 3, top - houseHeight / 5, top + houseHeight / 3 }, 3);
        g.setColor(darkMode ? new Color(73, 55, 51) : new Color(130, 72, 55));
        g.fillPolygon(roof);

        int windowSize = Math.max(8, tileWidth / 16);
        g.setColor(darkMode ? new Color(255, 204, 130) : new Color(248, 224, 172));
        g.fillRect(left + houseWidth / 6, top + houseHeight / 2, windowSize, windowSize);
        g.fillRect(left + houseWidth * 4 / 6, top + houseHeight / 2, windowSize, windowSize);
        g.setColor(darkMode ? new Color(72, 52, 43) : new Color(113, 76, 54));
        g.fillRect(left + houseWidth / 2 - windowSize / 2, base - houseHeight / 2,
                windowSize, houseHeight / 2);
    }

    private void drawOffice(Graphics2D g, int x, int base, int tileWidth, int height, boolean annex) {
        int officeWidth = tileWidth * (annex ? 82 : 88) / 100;
        int officeHeight = (int) (height * (annex ? 0.22 : 0.25));
        int left = x + (tileWidth - officeWidth) / 2;
        int top = base - officeHeight;
        g.setColor(darkMode ? new Color(64, 82, 91) : new Color(135, 158, 158));
        g.fillRect(left, top, officeWidth, officeHeight);
        g.setColor(darkMode ? new Color(45, 59, 68) : new Color(98, 125, 132));
        g.fillRect(left, top, officeWidth, Math.max(6, height / 60));
        int rows = annex ? 3 : 4;
        int columns = 3;
        int windowWidth = Math.max(8, officeWidth / 9);
        int windowHeight = Math.max(7, officeHeight / 9);
        g.setColor(darkMode ? new Color(255, 202, 132) : new Color(255, 231, 183));
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int windowX = left + officeWidth / 8 + column * (officeWidth / 3);
                int windowY = top + officeHeight / 6 + row * (officeHeight / (rows + 1));
                g.fillRect(windowX, windowY, windowWidth, windowHeight);
            }
        }
        g.setColor(darkMode ? new Color(43, 51, 56) : new Color(81, 103, 105));
        g.fillRect(left + officeWidth / 2 - windowWidth / 2, base - officeHeight / 4,
                windowWidth, officeHeight / 4);
    }

    private void drawSchool(Graphics2D g, int x, int base, int tileWidth, int height) {
        int schoolWidth = tileWidth * 94 / 100;
        int schoolHeight = (int) (height * 0.21);
        int left = x + (tileWidth - schoolWidth) / 2;
        int top = base - schoolHeight;
        g.setColor(darkMode ? new Color(177, 139, 94) : new Color(232, 198, 139));
        g.fillRect(left, top + schoolHeight / 5, schoolWidth, schoolHeight * 4 / 5);
        g.setColor(darkMode ? new Color(91, 67, 55) : new Color(154, 87, 60));
        g.fillRect(left - 4, top + schoolHeight / 5 - 5, schoolWidth + 8, 8);

        g.setColor(darkMode ? new Color(239, 226, 198) : new Color(76, 64, 52));
        g.setFont(new Font(SagipTheme.FONT_FAMILY, Font.BOLD, Math.max(8, height / 85)));
        String sign = "SCHOOL";
        int signWidth = g.getFontMetrics().stringWidth(sign);
        g.drawString(sign, left + (schoolWidth - signWidth) / 2, top + schoolHeight / 2);

        g.setColor(darkMode ? new Color(255, 204, 128) : new Color(255, 232, 180));
        int windowWidth = Math.max(7, tileWidth / 18);
        int windowY = top + schoolHeight * 3 / 5;
        g.fillRect(left + schoolWidth / 8, windowY, windowWidth, windowWidth);
        g.fillRect(left + schoolWidth * 7 / 8 - windowWidth, windowY, windowWidth, windowWidth);
        g.setColor(darkMode ? new Color(87, 63, 48) : new Color(118, 78, 48));
        g.fillRect(left + schoolWidth / 2 - windowWidth, base - schoolHeight / 3,
                windowWidth * 2, schoolHeight / 3);
    }

    private void paintCloud(Graphics2D g, int width, int height, double yFraction,
                            int cloudWidth, int cloudIndex) {
        double travel = (animationFrame * (0.035 + cloudIndex * 0.008) + cloudIndex * width * 0.39)
                % (width + cloudWidth * 2.0);
        int x = (int) (travel - cloudWidth);
        int y = (int) (height * yFraction);
        int cloudHeight = Math.max(24, cloudWidth / 5);
        Color cloud = darkMode ? new Color(228, 218, 204, 38) : new Color(255, 255, 255, 92);
        g.setColor(cloud);
        g.fill(new Ellipse2D.Double(x, y + cloudHeight * 0.18, cloudWidth * 0.58, cloudHeight * 0.72));
        g.fill(new Ellipse2D.Double(x + cloudWidth * 0.20, y, cloudWidth * 0.48, cloudHeight));
        g.fill(new Ellipse2D.Double(x + cloudWidth * 0.45, y + cloudHeight * 0.18,
                cloudWidth * 0.55, cloudHeight * 0.72));
    }
}

class ContrastPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final boolean darkMode;

    public ContrastPanel(boolean darkMode) {
        this.darkMode = darkMode;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(darkMode ? new Color(12, 19, 27, 190) : new Color(255, 249, 235, 208));
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
        g.setColor(darkMode ? new Color(255, 255, 255, 42) : new Color(65, 47, 33, 55));
        g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
        g.dispose();
        super.paintComponent(graphics);
    }
}

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

    private String resolveHazardTag(String objName) {
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

/**
 * SAGIP CameraPage with live camera feed, Start/Stop + Save Scan controls, and Legend sidebar.
 */
class CameraPage extends JPanel implements DetectionListener {
    private static final long serialVersionUID = 1L;
    private static final int LEGEND_WIDTH = 380;
    private static final int LEGEND_MIN_WIDTH = 360;

    private final boolean darkMode;
    private final DisasterHazardSystemApp app;
    private final CameraDetectionEngine engine;
    private final DisasterScenario activeScenario;

    private final JPanel tabArea;
    private final JButton backButton;
    private final CameraViewportPanel cameraView;
    private final JButton startStopButton;
    private final JButton saveButton;
    private final JLabel statusLabel;
    private final JLabel detectionsLabel;

    private boolean running = false;

    public CameraPage(boolean darkMode, DisasterHazardSystemApp app) {
        this.darkMode = darkMode;
        this.app = app;
        this.engine = CameraDetectionEngine.getInstance();
        this.activeScenario = new EarthquakeScenario();

        setLayout(new BorderLayout());
        setBackground(SagipTheme.background(darkMode));

        tabArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tabArea.setOpaque(false);
        backButton = pageButton("BACK TO MENU");
        backButton.setVisible(false);
        tabArea.add(backButton);

        cameraView = new CameraViewportPanel(darkMode);
        startStopButton = pageButton("START CAMERA");
        saveButton = pageButton("SAVE SCAN");
        saveButton.setEnabled(false);

        statusLabel = SagipTheme.label("", 13, SagipTheme.muted(darkMode), Font.BOLD);
        detectionsLabel = SagipTheme.label("", 14, SagipTheme.text(darkMode), Font.BOLD);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        startStopButton.addActionListener(event -> {
            if (running) {
                stopCamera();
            } else {
                startCamera();
            }
        });

        saveButton.addActionListener(event -> {
            BufferedImage frame = engine.getCurrentFrame();
            List<HazardItem> hazards = engine.getDetectedHazards();
            if (frame != null) {
                app.saveAssessment(frame, hazards, activeScenario.evaluateOverallRisk(hazards));
                saveButton.setText("SAVED");
                javax.swing.Timer reset = new javax.swing.Timer(1500, e -> saveButton.setText("SAVE SCAN"));
                reset.setRepeats(false);
                reset.start();
            }
        });

        setCameraRunning(false);
        setDetectionCount(0);
    }

    public void startCamera() {
        setCameraRunning(true);
        engine.startCameraStream();
    }

    public void stopCamera() {
        setCameraRunning(false);
        engine.stopCameraStream();
        cameraView.setStandby("Live camera view and hazard boxes will appear here.");
        setDetectionCount(0);
    }

    public void setBackAction(Runnable action) {
        for (ActionListener existing : backButton.getActionListeners()) {
            backButton.removeActionListener(existing);
        }
        if (action != null) {
            backButton.addActionListener(event -> action.run());
        }
        backButton.setVisible(action != null);
    }

    public void setDetectionCount(int count) {
        detectionsLabel.setText(String.format("DETECTIONS  %02d", count));
    }

    public void setCameraRunning(boolean isRunning) {
        this.running = isRunning;
        saveButton.setEnabled(isRunning);
        if (isRunning) {
            startStopButton.setText("STOP CAMERA");
            setButtonBorder(startStopButton, RiskLevel.CRITICAL.getColor(darkMode));
            statusLabel.setText("\u25CF LIVE");
            statusLabel.setForeground(RiskLevel.LOW.getColor(darkMode));
        } else {
            startStopButton.setText("START CAMERA");
            setButtonBorder(startStopButton, SagipTheme.accent(darkMode));
            statusLabel.setText("\u25CF STANDBY");
            statusLabel.setForeground(SagipTheme.muted(darkMode));
        }
    }

    public void onScreenDisplayed() {
        engine.addListener(this);
    }

    public void onScreenHidden() {
        engine.removeListener(this);
    }

    @Override
    public void onFrameUpdated(BufferedImage frame, double currentFps, boolean isLive, String engineTelemetry) {
        if (!running) {
            cameraView.setStandby("Live camera view and hazard boxes will appear here.");
            return;
        }
        cameraView.updateFeed(frame, isLive, engine.isAiDetectionActive(),
                engine.getDetectedHazards(), activeScenario, engine.getHardwareStatusMessage());
    }

    @Override
    public void onHazardsDetected(List<HazardItem> hazards) {
        if (running) {
            setDetectionCount(hazards.size());
        }
    }

    @Override
    public void onCameraStateChanged(boolean isConnected, boolean isDetecting) {
        if (!running) return;
        statusLabel.setText(isConnected ? "\u25CF LIVE" : "\u25CF CONNECTING");
        statusLabel.setForeground(isConnected ? RiskLevel.LOW.getColor(darkMode) : SagipTheme.accent(darkMode));
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(SagipTheme.surface(darkMode));
        header.setBorder(new EmptyBorder(18, 24, 18, 24));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(SagipTheme.label("SAGIP", 20, SagipTheme.text(darkMode), Font.BOLD));
        titles.add(Box.createVerticalStrut(3));
        titles.add(SagipTheme.label("Camera assessment", 13, SagipTheme.muted(darkMode), Font.PLAIN));

        JPanel tabHolder = new JPanel(new GridBagLayout());
        tabHolder.setOpaque(false);
        tabHolder.add(tabArea);

        header.add(titles, BorderLayout.WEST);
        header.add(tabHolder, BorderLayout.EAST);
        return header;
    }

    private JComponent buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(SagipTheme.surface(darkMode));
        footer.setBorder(new EmptyBorder(10, 24, 10, 24));
        footer.add(SagipTheme.label("SAGIP  \u2022  FIRE & EARTHQUAKE PREPAREDNESS", 11, SagipTheme.muted(darkMode), Font.BOLD),
                BorderLayout.WEST);
        return footer;
    }

    private JComponent buildContent() {
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(24, 30, 24, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;

        gbc.gridx = 0;
        gbc.weightx = 4;
        gbc.insets = new Insets(0, 0, 0, 20);
        content.add(buildCameraCard(), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        content.add(buildLegendCard(), gbc);

        return content;
    }

    private JComponent buildCameraCard() {
        Card card = new Card();
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(new EmptyBorder(18, 20, 18, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel eyebrow = SagipTheme.label("LIVE CAMERA FEED", 12, SagipTheme.accent(darkMode), Font.BOLD);
        JLabel title = SagipTheme.label("Live hazard detection", 22, SagipTheme.text(darkMode), Font.BOLD);
        JLabel sub = SagipTheme.label("Environmental risk assessment", 13, SagipTheme.muted(darkMode), Font.PLAIN);
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        titles.add(eyebrow);
        titles.add(Box.createVerticalStrut(3));
        titles.add(title);
        titles.add(Box.createVerticalStrut(3));
        titles.add(sub);

        header.add(titles, BorderLayout.WEST);
        header.add(statusLabel, BorderLayout.EAST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(saveButton);
        actions.add(startStopButton);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(detectionsLabel, BorderLayout.WEST);
        footer.add(actions, BorderLayout.EAST);

        card.add(header, BorderLayout.NORTH);
        card.add(cameraView, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private JComponent buildLegendCard() {
        Card card = new Card();
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(18, 20, 18, 10));
        card.setPreferredSize(new Dimension(LEGEND_WIDTH, 0));
        card.setMinimumSize(new Dimension(LEGEND_MIN_WIDTH, 0));

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setBorder(new EmptyBorder(0, 0, 0, 10));
        JLabel eyebrow = SagipTheme.label("LEGEND", 12, SagipTheme.accent(darkMode), Font.BOLD);
        JLabel sub = SagipTheme.label("How to read the boxes on the camera feed", 13, SagipTheme.muted(darkMode), Font.PLAIN);
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.add(eyebrow);
        head.add(Box.createVerticalStrut(3));
        head.add(sub);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(new EmptyBorder(0, 0, 0, 10));

        list.add(sectionLabel("RISK LEVEL  (BOX COLOR)"));
        list.add(legendItem(RiskLevel.CRITICAL.getColor(darkMode), null, "Critical",
                "Immediate danger. Fix or remove this hazard first."));
        list.add(legendItem(RiskLevel.HIGH.getColor(darkMode), null, "High",
                "Serious hazard likely to cause injury or damage. Address as soon as possible."));
        list.add(legendItem(RiskLevel.MODERATE.getColor(darkMode), null, "Medium",
                "Moderate risk. Schedule a fix and keep monitoring."));
        list.add(legendItem(RiskLevel.LOW.getColor(darkMode), null, "Low",
                "Minor risk. Keep it in mind during routine safety checks."));

        list.add(Box.createVerticalStrut(10));
        list.add(sectionLabel("HAZARD TYPE  (LABEL TAG)"));
        list.add(legendItem(SagipTheme.muted(darkMode), "F", "Fire hazard",
                "Objects that can start or spread a fire, such as an unattended iron or an overloaded outlet."));
        list.add(legendItem(SagipTheme.muted(darkMode), "E", "Earthquake hazard",
                "Objects that can fall or topple during shaking, such as an unsecured shelf or heavy items on high shelves."));

        list.add(Box.createVerticalStrut(10));
        list.add(new WrappedText("Each box shows the hazard name, tag, and detection confidence, with risks listed directly below.",
                12, SagipTheme.muted(darkMode)));

        JPanel holder = new ViewportWidthPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(list, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(holder,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        scroll.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                super.configureScrollBarColors();
                trackColor = SagipTheme.surface(darkMode);
                thumbColor = SagipTheme.muted(darkMode);
            }

            @Override protected JButton createDecreaseButton(int orientation) { return emptyScrollButton(); }
            @Override protected JButton createIncreaseButton(int orientation) { return emptyScrollButton(); }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
                g.setColor(SagipTheme.surface(darkMode));
                g.fillRect(r.x, r.y, r.width, r.height);
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                if (r.isEmpty()) return;
                g.setColor(thumbColor);
                g.fillRect(r.x + 1, r.y, r.width - 2, r.height);
            }
        });

        card.add(head, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JComponent sectionLabel(String text) {
        JLabel l = SagipTheme.label(text, 13, SagipTheme.muted(darkMode), Font.BOLD);
        l.setBorder(new EmptyBorder(12, 0, 4, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JComponent legendItem(Color color, String glyph, String name, String description) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(9, 0, 9, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        Swatch swatch = new Swatch(color, glyph);
        swatch.setPreferredSize(new Dimension(30, 30));
        JPanel swatchWrap = new JPanel(new BorderLayout());
        swatchWrap.setOpaque(false);
        swatchWrap.add(swatch, BorderLayout.NORTH);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        JLabel nameLabel = SagipTheme.label(name, 15, SagipTheme.text(darkMode), Font.BOLD);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(nameLabel);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(new WrappedText(description, 13, SagipTheme.muted(darkMode)));

        row.add(swatchWrap, BorderLayout.WEST);
        row.add(textPanel, BorderLayout.CENTER);
        return row;
    }

    private JButton emptyScrollButton() {
        JButton button = new JButton();
        Dimension none = new Dimension(0, 0);
        button.setPreferredSize(none);
        button.setMinimumSize(none);
        button.setMaximumSize(none);
        return button;
    }

    private JButton pageButton(String text) {
        return SagipTheme.pageButton(text, darkMode, () -> {});
    }

    private void setButtonBorder(JButton button, Color lineColor) {
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(lineColor, 1),
                new EmptyBorder(10, 16, 10, 16)));
    }

    private class Card extends JPanel {
        private static final long serialVersionUID = 1L;
        Card() { setOpaque(false); }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setColor(SagipTheme.surface(darkMode));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(SagipTheme.cardLine(darkMode));
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class ViewportWidthPanel extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;
        ViewportWidthPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(16, r.height - 16); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private class WrappedText extends JComponent {
        private static final long serialVersionUID = 1L;
        private final String text;
        private final Font font;
        private final Color color;
        private int lastWidth = -1;

        WrappedText(String text, int size, Color color) {
            this.text = text;
            this.font = new Font(SagipTheme.FONT_FAMILY, Font.PLAIN, size);
            this.color = color;
            setAlignmentX(Component.LEFT_ALIGNMENT);
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent event) {
                    if (getWidth() != lastWidth) {
                        lastWidth = getWidth();
                        revalidate();
                    }
                }
            });
        }

        private List<String> wrap(int width) {
            FontMetrics metrics = getFontMetrics(font);
            List<String> lines = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            for (String word : text.split(" ")) {
                String candidate = current.length() == 0 ? word : current + " " + word;
                if (current.length() > 0 && metrics.stringWidth(candidate) > width) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(candidate);
                }
            }
            if (current.length() > 0) lines.add(current.toString());
            return lines;
        }

        @Override public Dimension getPreferredSize() {
            int width = getWidth() > 0 ? getWidth() : 240;
            return new Dimension(120, wrap(width).size() * getFontMetrics(font).getHeight());
        }
        @Override public Dimension getMinimumSize() { return new Dimension(0, getPreferredSize().height); }
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(font);
            g.setColor(color);
            FontMetrics metrics = g.getFontMetrics();
            int y = metrics.getAscent();
            for (String line : wrap(getWidth())) {
                g.drawString(line, 0, y);
                y += metrics.getHeight();
            }
            g.dispose();
        }
    }

    private class Swatch extends JComponent {
        private static final long serialVersionUID = 1L;
        private final Color color;
        private final String glyph;

        Swatch(Color color, String glyph) {
            this.color = color;
            this.glyph = glyph;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            if (glyph == null) {
                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
                g.fill(new RoundRectangle2D.Float(2, 2, w - 5, h - 5, 4, 4));
                g.setColor(color);
                g.setStroke(new BasicStroke(2.5f));
                g.draw(new RoundRectangle2D.Float(2, 2, w - 5, h - 5, 4, 4));
            } else {
                g.setColor(color);
                g.setStroke(new BasicStroke(1.8f));
                g.draw(new Ellipse2D.Float(2, 2, w - 5, h - 5));
                g.setFont(new Font(SagipTheme.FONT_FAMILY, Font.BOLD, 15));
                FontMetrics fm = g.getFontMetrics();
                g.setColor(SagipTheme.text(darkMode));
                g.drawString(glyph, (w - fm.stringWidth(glyph)) / 2,
                        (h - fm.getHeight()) / 2 + fm.getAscent());
            }
            g.dispose();
        }
    }
}

