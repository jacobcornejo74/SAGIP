// package disaster.hazard;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.color.ColorSpace;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicScrollBarUI;


public class DisasterHazardSystemApp extends JFrame implements ScreenNavigator {

    public static final String SCREEN_MAIN_MENU = "SCREEN_MAIN_MENU";
    public static final String SCREEN_LIVE_DETECTION = "SCREEN_LIVE_DETECTION";

    private final CardLayout cardLayout;
    private final JPanel cardsContainer;
    private final MainMenuPanel mainMenuPanel;
    private final LiveDetectionPanel liveDetectionPanel;
    private BaseScreenPanel currentScreen;

    public DisasterHazardSystemApp() {
        super("Disaster Hazard Detection System");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1380, 860));
        setMinimumSize(new Dimension(1100, 720));
        setLocationRelativeTo(null);
        getContentPane().setBackground(SafetyTheme.BG_DARKEST);

        this.cardLayout = new CardLayout();
        this.cardsContainer = new JPanel(cardLayout);
        this.cardsContainer.setBackground(SafetyTheme.BG_DARKEST);

        this.mainMenuPanel = new MainMenuPanel(this);
        this.liveDetectionPanel = new LiveDetectionPanel(this);

        this.cardsContainer.add(mainMenuPanel, SCREEN_MAIN_MENU);
        this.cardsContainer.add(liveDetectionPanel, SCREEN_LIVE_DETECTION);

        add(cardsContainer);
        navigateToLiveDetection();
        pack();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                CameraDetectionEngine.getInstance().shutdown();
            }
        });
    }

    @Override
    public void navigateToMainMenu() {
        if (currentScreen != null) currentScreen.onScreenHidden();
        cardLayout.show(cardsContainer, SCREEN_MAIN_MENU);
        currentScreen = mainMenuPanel;
        mainMenuPanel.onScreenDisplayed();
    }

    @Override
    public void navigateToLiveDetection() {
        if (currentScreen != null) currentScreen.onScreenHidden();
        cardLayout.show(cardsContainer, SCREEN_LIVE_DETECTION);
        currentScreen = liveDetectionPanel;
        liveDetectionPanel.onScreenDisplayed();
    }

    private static void installMonochromeSwingDefaults() {
        UIManager.put("Panel.background", SafetyTheme.BG_DARKEST);
        UIManager.put("ComboBox.background", SafetyTheme.BG_INPUT);
        UIManager.put("ComboBox.foreground", SafetyTheme.TEXT_PRIMARY);
        UIManager.put("ComboBox.selectionBackground", SafetyTheme.MONO_WHITE);
        UIManager.put("ComboBox.selectionForeground", SafetyTheme.BG_DARKEST);
        UIManager.put("ComboBox.buttonBackground", SafetyTheme.BG_CARD);
        UIManager.put("ComboBox.buttonShadow", SafetyTheme.BORDER_SUBTLE);
        UIManager.put("ComboBox.buttonDarkShadow", SafetyTheme.BG_DARKEST);
        UIManager.put("ComboBox.buttonHighlight", SafetyTheme.BORDER_STRONG);
        UIManager.put("ScrollBar.thumb", SafetyTheme.BG_CARD_HOVER);
        UIManager.put("ScrollBar.track", SafetyTheme.BG_DARKEST);
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                installMonochromeSwingDefaults();
            } catch (Exception ignored) {}
            new DisasterHazardSystemApp().setVisible(true);
        });
    }
}


final class SafetyTheme {
    private SafetyTheme() {}

    // Pure Architectural Monochrome Surface Hierarchy (Obsidian -> Zinc -> Stark White)
    public static final Color BG_DARKEST    = new Color(9, 9, 11);      // #09090B Obsidian
    public static final Color BG_DARK       = new Color(16, 16, 19);    // #101013 Carbon
    public static final Color BG_PANEL      = new Color(22, 22, 26);    // #16161A Zinc Panel
    public static final Color BG_CARD       = new Color(30, 30, 35);    // #1E1E23 Graphite Card
    public static final Color BG_CARD_HOVER = new Color(44, 44, 51);    // #2C2C33 Elevated Ash
    public static final Color BG_INPUT      = new Color(14, 14, 17);    // #0E0E11 Recessed Input

    // Strict Grayscale Luminance Accents (Zero Chromatic Hue)
    public static final Color MONO_WHITE    = new Color(250, 250, 250); // #FAFAFA Pure Stark White
    public static final Color MONO_PLATINUM = new Color(228, 228, 231); // #E4E4E7 Platinum
    public static final Color MONO_SILVER   = new Color(185, 185, 194); // #B9B9C2 Silver
    public static final Color MONO_ASH      = new Color(138, 138, 148); // #8A8A94 Mid Ash
    public static final Color MONO_SLATE    = new Color(82, 82, 91);    // #52525B Dark Zinc

    public static final Color BORDER_SUBTLE = new Color(63, 63, 70, 175); // #3F3F46 Hairline
    public static final Color BORDER_STRONG = new Color(113, 113, 122);   // #71717A Structural
    public static final Color BORDER_BRIGHT = new Color(228, 228, 231);   // #E4E4E7 High-Contrast

    public static final Color TEXT_PRIMARY   = new Color(250, 250, 250);  // #FAFAFA
    public static final Color TEXT_SECONDARY = new Color(161, 161, 170);  // #A1A1AA
    public static final Color TEXT_MUTED     = new Color(113, 113, 122);  // #71717A

    public static final Font FONT_TITLE_LARGE = new Font("SansSerif", Font.BOLD, 28);
    public static final Font FONT_TITLE_MED   = new Font("SansSerif", Font.BOLD, 18);
    public static final Font FONT_TITLE_SMALL = new Font("SansSerif", Font.BOLD, 13);
    public static final Font FONT_BODY_BOLD   = new Font("SansSerif", Font.BOLD, 12);
    public static final Font FONT_BODY        = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font FONT_BODY_SMALL  = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font FONT_BADGE       = new Font("Monospaced", Font.BOLD, 10);
    public static final Font FONT_MONO_MED    = new Font("Monospaced", Font.BOLD, 12);
    public static final Font FONT_MONO_SMALL  = new Font("Monospaced", Font.PLAIN, 11);
}

// ------------------------------------------------------------------------------
// 2. MODEL LAYER (ENCAPSULATION, ABSTRACTION, INHERITANCE, POLYMORPHISM)
// ------------------------------------------------------------------------------

enum RiskLevel {
    LOW(
        "● LOW RISK",
        new Color(145, 145, 155),
        new Color(32, 32, 36, 160),
        new Color(228, 228, 231),
        false,
        1.2f
    ),
    MODERATE(
        "◆ MODERATE",
        new Color(195, 195, 205),
        new Color(55, 55, 62, 165),
        new Color(244, 244, 245),
        false,
        1.8f
    ),
    HIGH(
        "▲ HIGH RISK",
        new Color(244, 244, 245),
        new Color(250, 250, 250, 38),
        new Color(250, 250, 250),
        false,
        2.6f
    ),
    CRITICAL(
        "✖ CRITICAL",
        new Color(250, 250, 250),
        new Color(250, 250, 250, 235),
        new Color(9, 9, 11),
        true,
        3.2f
    );

    private final String label;
    private final Color strokeColor;
    private final Color bgFill;
    private final Color textColor;
    private final boolean inverted;
    private final float strokeWidth;

    RiskLevel(String label, Color strokeColor, Color bgFill, Color textColor, boolean inverted, float strokeWidth) {
        this.label = label;
        this.strokeColor = strokeColor;
        this.bgFill = bgFill;
        this.textColor = textColor;
        this.inverted = inverted;
        this.strokeWidth = strokeWidth;
    }

    public String getLabel() { return label; }
    public Color getStrokeColor() { return strokeColor; }
    public Color getBgFill() { return bgFill; }
    public Color getTextColor() { return textColor; }
    public boolean isInverted() { return inverted; }
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
    private volatile boolean userPausedCamera = false;
    private volatile boolean isCameraConnected = false;
    private volatile boolean isAiDetectionActive = true;
    private volatile boolean monochromeOptics = false;
    private volatile double currentFps = 0.0;
    private volatile int cameraIndex = 0;
    private float confidenceThreshold = 0.40f;

    private BufferedImage currentFrame = null;
    private final List<HazardItem> detectedHazards = new CopyOnWriteArrayList<>();
    private final List<DetectionListener> listeners = new CopyOnWriteArrayList<>();
    private final YoloOnnxDetector yoloDetector = new YoloOnnxDetector();
    private final ColorConvertOp grayscaleOp = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);

    private Object activeGrabber = null;
    private Object cachedFrameConverter = null;
    private Thread captureWorkerThread = null;
    private String hardwareStatusMessage = "PROBING SYSTEM WEBCAM...";

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

        captureWorkerThread = new Thread(this::runCaptureLoop, "Monochrome-Camera-YOLO-Worker");
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
        initHardwareCamera();

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
                // Run real YOLOv8 inference on crisp RGB frame (~10 Hz to keep UI silky at 30 FPS)
                if (isAiDetectionActive && yoloDetector.isReady() && (loopStart - lastYoloTime >= 95)) {
                    lastYoloTime = loopStart;
                    List<HazardItem> results = yoloDetector.detect(rawFrame, confidenceThreshold);
                    detectedHazards.clear();
                    detectedHazards.addAll(results);
                } else if (!isAiDetectionActive) {
                    detectedHazards.clear();
                }

                // Pure natural RGB camera feed without filter
                displayFrame = rawFrame;
            } else {
                detectedHazards.clear();
                displayFrame = generateDiagnosticOfflineFrame();
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

    private BufferedImage toMonochromeFrame(BufferedImage src) {
        BufferedImage gray = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        grayscaleOp.filter(src, gray);
        return gray;
    }

    private void initHardwareCamera() {
        releaseCameraHardware();

        // Strategy 1 (Primary): JavaCV OpenCVFrameGrabber (OpenCV 4.9 native driver for Win10/11, macOS ARM64/x64, Linux)
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

                    // Grab up to 6 warm-up frames to verify non-null optical RGB image (skips blank IR/virtual devices)
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
                        this.hardwareStatusMessage = "ONLINE: OPENCV CAMERA #" + idx + " (" + testImg.getWidth() + "x" + testImg.getHeight() + ")";
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
            hardwareStatusMessage = "MAVEN DRIVERS NOT LOADED — RUN WITH: mvn clean compile exec:java";
        } catch (Throwable t) {
            hardwareStatusMessage = "OPENCV INIT ERROR: " + t.getMessage();
        }

        // Strategy 2 (Fallback): Sarxos Webcam-Capture with non-null frame verification
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
                    hardwareStatusMessage = "CAMERA RETURNED EMPTY FRAME — CLOSE ZOOM/TEAMS OR CLICK SWITCH CAM";
                }
            } else if (!hardwareStatusMessage.contains("MAVEN")) {
                hardwareStatusMessage = "0 CAMERAS DETECTED BY OS — CHECK PRIVACY SETTINGS & USB CONNECTION";
            }
        } catch (ClassNotFoundException e) {
            hardwareStatusMessage = "MAVEN DRIVERS NOT LOADED — RUN WITH: mvn clean compile exec:java";
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

    private BufferedImage generateDiagnosticOfflineFrame() {
        int w = 960, h = 540;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(SafetyTheme.BG_DARKEST);
        g2.fillRect(0, 0, w, h);

        // Subtle monochrome architectural grid
        g2.setColor(new Color(28, 28, 32));
        for (int x = 0; x < w; x += 48) g2.drawLine(x, 0, x, h);
        for (int y = 0; y < h; y += 48) g2.drawLine(0, y, w, y);

        int cardW = 696, cardH = 248;
        int cardX = (w - cardW) / 2, cardY = (h - cardH) / 2;
        g2.setColor(SafetyTheme.BG_PANEL);
        g2.fillRect(cardX, cardY, cardW, cardH);
        g2.setColor(SafetyTheme.BORDER_BRIGHT);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRect(cardX, cardY, cardW, cardH);

        // Inverted header band
        g2.setColor(SafetyTheme.MONO_WHITE);
        g2.fillRect(cardX, cardY, cardW, 38);
        g2.setColor(SafetyTheme.BG_DARKEST);
        g2.setFont(SafetyTheme.FONT_MONO_MED);
        g2.drawString(userPausedCamera
                ? "■  OPTICAL STREAM PAUSED BY OPERATOR"
                : "▲  HARDWARE WEBCAM STREAM STANDBY", cardX + 20, cardY + 24);

        g2.setFont(SafetyTheme.FONT_MONO_SMALL);
        g2.setColor(SafetyTheme.TEXT_PRIMARY);
        g2.drawString("DIAGNOSTIC: " + (userPausedCamera ? "STREAM MANUALLY HALTED" : hardwareStatusMessage), cardX + 24, cardY + 76);
        g2.drawString("NEURAL NET: " + yoloDetector.getStatus() + "  ·  TARGET DEVICE: CAM #" + cameraIndex, cardX + 24, cardY + 98);

        g2.setColor(SafetyTheme.TEXT_SECONDARY);
        g2.drawString("1. Run with Maven (or press F5 in VS Code / run.bat) so OpenCV & ONNX JARs load:", cardX + 24, cardY + 136);
        g2.setColor(SafetyTheme.MONO_WHITE);
        g2.drawString("   mvn clean compile exec:java", cardX + 24, cardY + 156);
        g2.setColor(SafetyTheme.TEXT_SECONDARY);
        g2.drawString("2. Close Zoom/Teams/Browser camera tabs & enable OS Camera Privacy permission.", cardX + 24, cardY + 184);
        g2.drawString("3. On laptops with IR sensors, click [SWITCH CAM] below to cycle CAM #0 / #1 / #2.", cardX + 24, cardY + 206);
        g2.setColor(SafetyTheme.TEXT_MUTED);
        g2.drawString(userPausedCamera
                ? "Click [RESUME CAMERA] in the bottom bar to reopen sensor."
                : "Auto-polling physical camera bus every 3.0 seconds...", cardX + 24, cardY + 230);

        g2.dispose();
        return img;
    }

    public void addListener(DetectionListener l) { listeners.add(l); }
    public void removeListener(DetectionListener l) { listeners.remove(l); }

    public void toggleCamera() {
        if (isCameraConnected && !userPausedCamera) {
            userPausedCamera = true;
            hardwareStatusMessage = "STREAM MANUALLY HALTED";
        } else {
            userPausedCamera = false;
            hardwareStatusMessage = "RECONNECTING CAMERA #" + cameraIndex + "...";
        }
        reconnectRequested.set(true);
        notifyCameraState();
    }

    public void switchCamera() {
        cameraIndex = (cameraIndex + 1) % 3;
        userPausedCamera = false;
        hardwareStatusMessage = "SWITCHING TO CAMERA #" + cameraIndex + "...";
        reconnectRequested.set(true);
        notifyCameraState();
    }

    public void toggleAiDetection() {
        isAiDetectionActive = !isAiDetectionActive;
        notifyCameraState();
    }

    public void toggleMonochromeOptics() {
        monochromeOptics = !monochromeOptics;
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
    public boolean isAiDetectionActive() { return isAiDetectionActive; }
    public boolean isMonochromeOptics() { return monochromeOptics; }
    public double getCurrentFps() { return currentFps; }
    public BufferedImage getCurrentFrame() { return currentFrame; }
    public String getYoloStatus() { return yoloDetector.getStatus(); }
    public List<HazardItem> getDetectedHazards() { return Collections.unmodifiableList(detectedHazards); }
    public void setConfidenceThreshold(float t) { this.confidenceThreshold = t; }
}

// ------------------------------------------------------------------------------
// 4. CUSTOM MONOCHROMATIC SWING UI COMPONENTS (INHERITANCE)
// ------------------------------------------------------------------------------
class CyberButton extends JButton {
    public enum ButtonStyle { PRIMARY, INVERTED_OUTLINE, SECONDARY }
    private final ButtonStyle style;
    private boolean isHovered = false;
    private boolean isPressed = false;
    private int cornerRadius = 4;

    public CyberButton(String text, ButtonStyle style) {
        super(text);
        this.style = style;
        setOpaque(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setFont(SafetyTheme.FONT_BODY_BOLD);

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { isHovered = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) { isPressed = true; repaint(); }
            @Override public void mouseReleased(MouseEvent e) { isPressed = false; repaint(); }
        });
    }

    public void setCornerRadius(int radius) { this.cornerRadius = radius; repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        Color bg, border, fg;

        switch (style) {
            case PRIMARY:
                // Stark White Monochromatic CTA with Black Typography
                bg = isPressed ? SafetyTheme.MONO_SILVER : isHovered ? SafetyTheme.MONO_PLATINUM : SafetyTheme.MONO_WHITE;
                border = SafetyTheme.MONO_WHITE;
                fg = SafetyTheme.BG_DARKEST;
                break;
            case INVERTED_OUTLINE:
                bg = isPressed ? SafetyTheme.MONO_WHITE : isHovered ? SafetyTheme.BG_CARD_HOVER : SafetyTheme.BG_DARK;
                border = SafetyTheme.BORDER_BRIGHT;
                fg = isPressed ? SafetyTheme.BG_DARKEST : SafetyTheme.MONO_WHITE;
                break;
            default:
                bg = isPressed ? SafetyTheme.BG_DARK : isHovered ? SafetyTheme.BG_CARD_HOVER : SafetyTheme.BG_CARD;
                border = isHovered ? SafetyTheme.BORDER_BRIGHT : SafetyTheme.BORDER_STRONG;
                fg = isHovered ? SafetyTheme.MONO_WHITE : SafetyTheme.TEXT_PRIMARY;
                break;
        }

        g2.setColor(bg);
        g2.fillRoundRect(0, 0, w, h, cornerRadius, cornerRadius);
        g2.setColor(border);
        g2.drawRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);

        g2.setColor(fg);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int tx = (w - fm.stringWidth(getText())) / 2;
        int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(getText(), tx, ty);
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(d.width + 24, 110), Math.max(d.height + 12, 36));
    }
}

class CyberCardPanel extends JPanel {
    private final Color backgroundColor;
    private Color borderColor;
    private final int cornerRadius;

    public CyberCardPanel(Color bgColor, Color borderColor, int cornerRadius) {
        this.backgroundColor = bgColor;
        this.borderColor = borderColor;
        this.cornerRadius = cornerRadius;
        setOpaque(false);
    }

    public CyberCardPanel() { this(SafetyTheme.BG_CARD, SafetyTheme.BORDER_SUBTLE, 6); }

    public void setCardBorderColor(Color c) { this.borderColor = c; repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(backgroundColor != null ? backgroundColor : SafetyTheme.BG_CARD);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}

class RiskBadge extends JLabel {
    private RiskLevel riskLevel;

    public RiskBadge(RiskLevel level) {
        this.riskLevel = level;
        setFont(SafetyTheme.FONT_BADGE);
        setOpaque(false);
    }

    public RiskBadge(String text, RiskLevel level) {
        super(text);
        this.riskLevel = level;
        setFont(SafetyTheme.FONT_BADGE);
        setOpaque(false);
    }

    public void setRiskLevel(RiskLevel level) {
        this.riskLevel = level;
        revalidate();
        repaint();
    }

    private String resolveLabel() {
        if (riskLevel != null) return riskLevel.getLabel();
        return (getText() != null && !getText().isEmpty()) ? getText() : "● NOMINAL";
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        RiskLevel rl = riskLevel != null ? riskLevel : RiskLevel.LOW;

        g2.setColor(rl.isInverted() ? SafetyTheme.MONO_WHITE : rl.getBgFill());
        g2.fillRoundRect(0, 0, w, h, 4, 4);

        g2.setColor(rl.getStrokeColor());
        g2.setStroke(new BasicStroke(rl.isInverted() ? 1.5f : 1.2f));
        g2.drawRoundRect(0, 0, w - 1, h - 1, 4, 4);

        String text = resolveLabel();
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(rl.getTextColor());
        g2.drawString(text, (w - fm.stringWidth(text)) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        String text = resolveLabel();
        FontMetrics fm = getFontMetrics(getFont());
        return new Dimension(fm.stringWidth(text) + 20, Math.max(fm.getHeight() + 6, 22));
    }
}

class MonochromeScrollBarUI extends BasicScrollBarUI {
    @Override
    protected void configureScrollBarColors() {
        this.thumbColor = SafetyTheme.MONO_SLATE;
        this.trackColor = SafetyTheme.BG_PANEL;
    }

    @Override
    protected JButton createDecreaseButton(int orientation) { return zeroButton(); }

    @Override
    protected JButton createIncreaseButton(int orientation) { return zeroButton(); }

    private JButton zeroButton() {
        JButton b = new JButton();
        b.setPreferredSize(new Dimension(0, 0));
        return b;
    }
}

class HudGraphicPanel extends JPanel {
    private double scanAngle = 0.0;

    public HudGraphicPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(340, 195));
        new javax.swing.Timer(40, e -> {
            scanAngle += 0.035;
            if (scanAngle > Math.PI * 2) scanAngle -= Math.PI * 2;
            repaint();
        }).start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int cx = getWidth() / 2, cy = getHeight() / 2;
        int radius = Math.min(getWidth(), getHeight()) / 2 - 14;

        // Concentric monochrome calibration rings
        g2.setColor(new Color(82, 82, 91, 140));
        g2.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);
        g2.drawOval(cx - radius * 2 / 3, cy - radius * 2 / 3, radius * 4 / 3, radius * 4 / 3);
        g2.drawOval(cx - radius / 3, cy - radius / 3, radius * 2 / 3, radius * 2 / 3);

        // Crosshair axes
        g2.setColor(new Color(161, 161, 170, 60));
        g2.drawLine(cx - radius - 8, cy, cx + radius + 8, cy);
        g2.drawLine(cx, cy - radius - 8, cx, cy + radius + 8);

        // Sweep vector
        int sx = (int) (cx + Math.cos(scanAngle) * radius);
        int sy = (int) (cy + Math.sin(scanAngle) * radius);
        g2.setColor(new Color(250, 250, 250, 210));
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawLine(cx, cy, sx, sy);

        // Central optical aperture
        g2.setColor(SafetyTheme.BG_DARKEST);
        g2.fillOval(cx - 14, cy - 14, 28, 28);
        g2.setColor(SafetyTheme.MONO_WHITE);
        g2.setStroke(new BasicStroke(1.4f));
        g2.drawOval(cx - 14, cy - 14, 28, 28);
        g2.fillOval(cx - 4, cy - 4, 8, 8);

        g2.dispose();
    }
}

/**
 * Renders live hardware webcam frames + high-contrast monochromatic YOLOv8 HUD reticles.
 */
class CameraViewportPanel extends JPanel {
    private BufferedImage currentFrame = null;
    private boolean isCameraConnected = false;
    private boolean isAiDetectionActive = true;
    private double currentFps = 0.0;
    private String engineTelemetry = "YOLOv8";
    private List<HazardItem> currentHazards = new ArrayList<>();
    private DisasterScenario currentScenario;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    private long pulseTick = 0;

    public CameraViewportPanel() {
        setBackground(SafetyTheme.BG_DARKEST);
        setBorder(BorderFactory.createLineBorder(SafetyTheme.BORDER_SUBTLE, 1));
        setOpaque(true);
    }

    public void updateFeed(BufferedImage frame, double fps, boolean cameraConnected, boolean aiActive,
                           String telemetry, List<HazardItem> hazards, DisasterScenario scenario) {
        this.currentFrame = frame;
        this.currentFps = fps;
        this.isCameraConnected = cameraConnected;
        this.isAiDetectionActive = aiActive;
        this.engineTelemetry = telemetry != null ? telemetry : "YOLOv8";
        this.currentHazards = (hazards != null) ? hazards : new ArrayList<>();
        this.currentScenario = scenario;
        this.pulseTick++;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        if (currentFrame != null) {
            g2.drawImage(currentFrame, 0, 0, w, h, null);
        } else {
            g2.setColor(SafetyTheme.BG_DARKEST);
            g2.fillRect(0, 0, w, h);
        }

        if (isCameraConnected && isAiDetectionActive && !currentHazards.isEmpty()) {
            drawYoloBoxes(g2, w, h);
        }

        g2.dispose();
    }

    private void drawYoloBoxes(Graphics2D g2, int w, int h) {
        Stroke dashedStroke = new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{5f, 4f}, 0f);

        for (HazardItem item : currentHazards) {
            Rectangle2D.Double norm = item.getNormalizedBoundingBox();
            int bx = (int) (norm.x * w), by = (int) (norm.y * h);
            int bw = (int) (norm.width * w), bh = (int) (norm.height * h);

            RiskLevel risk = (currentScenario != null)
                    ? currentScenario.getHazardRisk(item)
                    : RiskLevel.MODERATE;

            Color strokeCol = risk.getStrokeColor();

            // Frame stroke (crisp solid border)
            g2.setColor(new Color(strokeCol.getRed(), strokeCol.getGreen(), strokeCol.getBlue(), 220));
            g2.setStroke(risk == RiskLevel.LOW ? dashedStroke : new BasicStroke(1.2f));
            g2.drawRect(bx, by, bw, bh);

            // Double outer frame for CRITICAL hazards
            if (risk == RiskLevel.CRITICAL) {
                g2.setColor(SafetyTheme.MONO_WHITE);
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRect(bx - 3, by - 3, bw + 6, bh + 6);
            }

            // Precision corner brackets
            int bLen = Math.min(18, Math.min(bw / 3, bh / 3));
            g2.setColor(strokeCol);
            g2.setStroke(new BasicStroke(risk.getStrokeWidth()));
            g2.drawLine(bx, by, bx + bLen, by);
            g2.drawLine(bx, by, bx, by + bLen);
            g2.drawLine(bx + bw, by, bx + bw - bLen, by);
            g2.drawLine(bx + bw, by, bx + bw, by + bLen);
            g2.drawLine(bx, by + bh, bx + bLen, by + bh);
            g2.drawLine(bx, by + bh, bx, by + bh - bLen);
            g2.drawLine(bx + bw, by + bh, bx + bw - bLen, by + bh);
            g2.drawLine(bx + bw, by + bh, bx + bw, by + bh - bLen);

            // Risk profile list below the detected box
            String objName = item.getObjectName();
            String title = String.format("%s (%d%%)", objName, item.getConfidencePercentage());
            String[] risks = new String[]{
                "may fall during an earthquake",
                "Uneven surface that may caused floods",
                "combustible objects"
            };

            g2.setFont(SafetyTheme.FONT_MONO_SMALL);
            FontMetrics fm = g2.getFontMetrics();
            int maxLineW = fm.stringWidth(title);
            for (String r : risks) {
                int rw = fm.stringWidth("• " + r);
                if (rw > maxLineW) maxLineW = rw;
            }

            int cardPadX = 10, cardPadY = 8, lineH = 16;
            int cardW = maxLineW + cardPadX * 2 + 10;
            int cardH = cardPadY * 2 + 22 + (risks.length + 1) * lineH;

            int cardX = Math.max(4, Math.min(bx, w - cardW - 8));
            int cardY = (by + bh + cardH + 6 <= h) ? (by + bh + 6) : Math.max(4, by - cardH - 6);

            // Card background & border
            g2.setColor(new Color(9, 9, 11, 238));
            g2.fillRect(cardX, cardY, cardW, cardH);
            g2.setColor(SafetyTheme.MONO_WHITE);
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRect(cardX, cardY, cardW, cardH);

            // Header banner
            g2.setColor(SafetyTheme.MONO_WHITE);
            g2.fillRect(cardX, cardY, cardW, 22);
            g2.setColor(SafetyTheme.BG_DARKEST);
            g2.setFont(SafetyTheme.FONT_MONO_MED);
            g2.drawString(title, cardX + cardPadX, cardY + 16);

            // Risks header and bullet points
            g2.setColor(SafetyTheme.TEXT_SECONDARY);
            g2.setFont(SafetyTheme.FONT_MONO_SMALL);
            int textY = cardY + 22 + 16;
            g2.drawString("risks:", cardX + cardPadX, textY);
            textY += lineH;

            g2.setColor(SafetyTheme.TEXT_PRIMARY);
            for (String r : risks) {
                g2.drawString("• " + r, cardX + cardPadX, textY);
                textY += lineH;
            }
        }
    }
}

// ------------------------------------------------------------------------------
// 5. BASE & CONCRETE SCREEN IMPLEMENTATIONS (ABSTRACTION & INHERITANCE)
// ------------------------------------------------------------------------------
abstract class BaseScreenPanel extends JPanel {
    protected final ScreenNavigator navigator;

    public BaseScreenPanel(ScreenNavigator navigator) {
        this.navigator = navigator;
        setLayout(new BorderLayout());
        setBackground(SafetyTheme.BG_DARKEST);
    }

    protected abstract void setupUI();
    public abstract void onScreenDisplayed();
    public abstract void onScreenHidden();
}

/**
 * PAGE 1: MONOCHROMATIC MAIN MENU SCREEN
 */
class MainMenuPanel extends BaseScreenPanel {
    public MainMenuPanel(ScreenNavigator nav) {
        super(nav);
        setupUI();
    }

    @Override
    protected void setupUI() {
        setLayout(new BorderLayout());

        // Top Bar
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, SafetyTheme.BORDER_SUBTLE),
            BorderFactory.createEmptyBorder(14, 28, 14, 28)
        ));
        JLabel title = new JLabel("DISASTER HAZARD DETECTION SYSTEM");
        title.setFont(SafetyTheme.FONT_BODY_BOLD);
        title.setForeground(SafetyTheme.TEXT_PRIMARY);
        JLabel ready = new JLabel("OPTICAL SENSOR · YOLOv8 VISION");
        ready.setFont(SafetyTheme.FONT_MONO_SMALL);
        ready.setForeground(SafetyTheme.TEXT_SECONDARY);
        bar.add(title, BorderLayout.WEST);
        bar.add(ready, BorderLayout.EAST);
        add(bar, BorderLayout.NORTH);

        // Center Stage
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 24, 8, 24);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);

        JLabel tag = new JLabel("SEISMIC  ·  FLOOD  ·  FIRE RISK ASSESSMENT", SwingConstants.CENTER);
        tag.setFont(SafetyTheme.FONT_MONO_SMALL);
        tag.setForeground(SafetyTheme.TEXT_MUTED);
        tag.setAlignmentX(CENTER_ALIGNMENT);

        JLabel mainTitle = new JLabel("Disaster Hazard Detection System", SwingConstants.CENTER);
        mainTitle.setFont(SafetyTheme.FONT_TITLE_LARGE);
        mainTitle.setForeground(SafetyTheme.TEXT_PRIMARY);
        mainTitle.setAlignmentX(CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Real-Time Hardware Camera & YOLOv8 Structural Vulnerability Analysis", SwingConstants.CENTER);
        sub.setFont(SafetyTheme.FONT_TITLE_SMALL);
        sub.setForeground(SafetyTheme.MONO_SILVER);
        sub.setAlignmentX(CENTER_ALIGNMENT);

        JLabel desc1 = new JLabel("Captures live optical camera frames and executes YOLOv8 neural object localization", SwingConstants.CENTER);
        desc1.setFont(SafetyTheme.FONT_BODY);
        desc1.setForeground(SafetyTheme.TEXT_SECONDARY);
        desc1.setAlignmentX(CENTER_ALIGNMENT);

        JLabel desc2 = new JLabel("to evaluate physical hazards across seismic, flood, and thermal emergency scenarios.", SwingConstants.CENTER);
        desc2.setFont(SafetyTheme.FONT_BODY);
        desc2.setForeground(SafetyTheme.TEXT_SECONDARY);
        desc2.setAlignmentX(CENTER_ALIGNMENT);

        titleBox.add(tag);
        titleBox.add(Box.createVerticalStrut(8));
        titleBox.add(mainTitle);
        titleBox.add(Box.createVerticalStrut(6));
        titleBox.add(sub);
        titleBox.add(Box.createVerticalStrut(12));
        titleBox.add(desc1);
        titleBox.add(Box.createVerticalStrut(2));
        titleBox.add(desc2);

        gbc.gridy = 0;
        center.add(titleBox, gbc);

        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        center.add(new HudGraphicPanel(), gbc);

        gbc.gridy = 2;
        CyberButton startBtn = new CyberButton("START LIVE DETECTION", CyberButton.ButtonStyle.PRIMARY);
        startBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        startBtn.setPreferredSize(new Dimension(260, 48));
        startBtn.setCornerRadius(4);
        startBtn.addActionListener(e -> navigator.navigateToLiveDetection());
        center.add(startBtn, gbc);

        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel cardsRow = new JPanel(new GridLayout(1, 3, 18, 0));
        cardsRow.setOpaque(false);
        cardsRow.setPreferredSize(new Dimension(880, 112));

        cardsRow.add(createCard("01. YOLOv8 Neural Vision", "80-class ONNX object recognition with Non-Maximum Suppression and live bounding coordinates."));
        cardsRow.add(createCard("02. Hardware Camera Feed", "Direct webcam stream acquisition at 30 FPS with optional monochrome optical filtering."));
        cardsRow.add(createCard("03. Multi-Hazard Risk Matrix", "Evaluates detected physical objects dynamically against Earthquake, Flood, and Fire protocols."));
        center.add(cardsRow, gbc);

        add(center, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, SafetyTheme.BORDER_SUBTLE),
            BorderFactory.createEmptyBorder(10, 28, 12, 28)
        ));
        JLabel fLeft = new JLabel("Disaster Mitigation & Environmental Safety Monitoring");
        fLeft.setFont(SafetyTheme.FONT_BODY_SMALL);
        fLeft.setForeground(SafetyTheme.TEXT_MUTED);
        JLabel fRight = new JLabel("Real-Time Hazard Assessment");
        fRight.setFont(SafetyTheme.FONT_MONO_SMALL);
        fRight.setForeground(SafetyTheme.TEXT_MUTED);
        footer.add(fLeft, BorderLayout.WEST);
        footer.add(fRight, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
    }

    private CyberCardPanel createCard(String title, String desc) {
        CyberCardPanel card = new CyberCardPanel(SafetyTheme.BG_PANEL, SafetyTheme.BORDER_SUBTLE, 6);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        JLabel t = new JLabel(title);
        t.setFont(SafetyTheme.FONT_BODY_BOLD);
        t.setForeground(SafetyTheme.MONO_WHITE);
        JLabel d = new JLabel("<html><body style='width: 220px; line-height: 1.35; color: #A1A1AA;'>" + desc + "</body></html>");
        d.setFont(SafetyTheme.FONT_BODY_SMALL);
        card.add(t, BorderLayout.NORTH);
        card.add(d, BorderLayout.CENTER);
        return card;
    }

    @Override public void onScreenDisplayed() {}
    @Override public void onScreenHidden() {}
}

/**
 * PAGE 2: MONOCHROMATIC LIVE HAZARD DETECTION SCREEN
 */
class LiveDetectionPanel extends BaseScreenPanel implements DetectionListener {
    private final CameraDetectionEngine engine;
    private CameraViewportPanel cameraViewport;
    private final DisasterScenario[] availableScenarios;
    private DisasterScenario activeScenario;

    public LiveDetectionPanel(ScreenNavigator nav) {
        super(nav);
        this.engine = CameraDetectionEngine.getInstance();
        this.availableScenarios = new DisasterScenario[] {
            new EarthquakeScenario(),
            new FloodScenario(),
            new FireScenario()
        };
        this.activeScenario = availableScenarios[0];
        setupUI();
    }

    @Override
    protected void setupUI() {
        setLayout(new BorderLayout());
        cameraViewport = new CameraViewportPanel();
        add(cameraViewport, BorderLayout.CENTER);
    }

    @Override
    public void onFrameUpdated(BufferedImage frame, double fps, boolean isLive, String telemetry) {
        cameraViewport.updateFeed(frame, fps, isLive, engine.isAiDetectionActive(), telemetry, engine.getDetectedHazards(), activeScenario);
    }

    @Override
    public void onHazardsDetected(List<HazardItem> hazards) {
        // Hazard bounding boxes and risks are rendered directly on the camera viewport
    }

    @Override
    public void onCameraStateChanged(boolean isConnected, boolean isDetecting) {
        // Camera connectivity is visualized on the viewport
    }

    @Override
    public void onScreenDisplayed() {
        engine.addListener(this);
    }

    @Override
    public void onScreenHidden() {
        engine.removeListener(this);
    }
}
