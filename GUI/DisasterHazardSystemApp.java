package GUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * SAGIP - Disaster Hazard Detection System
 *
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
    private final ScanStorageManager storageManager = new ScanStorageManager();

    public DisasterHazardSystemApp() {
        super("SAGIP");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 600));
        setSize(1180, 740);
        setLocationRelativeTo(null);

        try {
            savedAssessments.addAll(storageManager.loadAll());
        } catch (IOException e) {
            showStorageError("load saved assessments", e);
        }
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
        rebuildScreens(SCREEN_HOME);
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

    public void saveAssessment(BufferedImage annotatedSnapshot, List<HazardItem> hazards, DisasterScenario scenario)
            throws IOException {
        String id = String.valueOf(System.currentTimeMillis());
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        RiskLevel overall = (scenario != null) ? scenario.evaluateOverallRisk(hazards) : RiskLevel.LOW;

        List<SavedDetectionEntry> items = new ArrayList<>();
        for (HazardItem item : hazards) {
            RiskLevel risk = (scenario != null) ? scenario.getHazardRisk(item) : RiskLevel.MODERATE;
            String tag = CameraViewportPanel.resolveHazardTag(item.getObjectName());
            Rectangle2D.Double box = item.getNormalizedBoundingBox();
            List<String> risks = Arrays.asList(
                    "may fall during an earthquake",
                    "Uneven surface that may caused floods",
                    "combustible objects"
            );
            items.add(new SavedDetectionEntry(
                    item.getObjectName(),
                    tag,
                    item.getConfidencePercentage(),
                    risk.name(),
                    box.x, box.y, box.width, box.height,
                    risks
            ));
        }

        SavedScanRecord record = storageManager.saveNewScan(
                id, time, overall.name(), overall.getLabel(), items.size(), annotatedSnapshot, items);
        savedAssessments.add(0, record);
    }

    public void deleteAssessment(SavedScanRecord record) {
        List<SavedScanRecord> remaining = new ArrayList<>(savedAssessments);
        remaining.remove(record);
        try {
            storageManager.deleteScan(record, remaining);
            savedAssessments.remove(record);
            rebuildScreens(SCREEN_STORAGE);
        } catch (IOException e) {
            showStorageError("delete this assessment", e);
        }
    }

    public void clearAllAssessments() {
        try {
            storageManager.clearAll(savedAssessments);
            savedAssessments.clear();
            rebuildScreens(SCREEN_STORAGE);
        } catch (IOException e) {
            showStorageError("clear saved assessments", e);
        }
    }

    private void showStorageError(String action, IOException exception) {
        JOptionPane.showMessageDialog(this,
                "Could not " + action + ". Check that the storage folder is writable.\n" + exception.getMessage(),
                "Storage Error",
                JOptionPane.ERROR_MESSAGE);
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

        JLabel title = SagipTheme.label("SAGIP", 58, menuBrandColor(), Font.BOLD);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel subtitle = SagipTheme.label("SMART GUARD FOR INCIDENT PREVENTION", 12, menuBrandColor(), Font.PLAIN);
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

        JLabel footer = SagipTheme.label("SAGIP  \u2022  FIRE & EARTHQUAKE PREPAREDNESS", 10,
                darkMode ? SagipTheme.text(darkMode) : Color.WHITE, Font.BOLD);
        footer.setHorizontalAlignment(SwingConstants.CENTER);
        background.add(footer, BorderLayout.SOUTH);
        return background;
    }

    private JButton menuButton(String title, String detail, Runnable action) {
        JButton button = new JButton("<html><div style='text-align:center;'>"
                + "<span style='font-family:Segoe UI;font-size:14pt;font-weight:700;color:"
                + SagipTheme.htmlColor(menuTextColor()) + ";'>"
                + title + "</span><br>"
                + "<span style='font-family:Segoe UI;font-size:9pt;font-weight:400;color:"
                + SagipTheme.htmlColor(menuDetailColor()) + ";'>"
                + detail + "</span></div></html>") {
            @Override
            protected void paintComponent(Graphics graphics) {
                if (darkMode) {
                    Graphics2D g2 = (Graphics2D) graphics.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                    g2.dispose();
                }
                super.paintComponent(graphics);
            }
        };
        button.setFont(new Font(SagipTheme.FONT_FAMILY, Font.BOLD, 14));
        button.setForeground(menuTextColor());
        button.setBackground(menuButtonBackground());
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        darkMode ? new Color(255, 255, 255, 70) : new Color(99, 78, 55), 1),
                new EmptyBorder(7, 20, 7, 20)));
        button.setFocusPainted(false);
        button.setContentAreaFilled(!darkMode);
        button.setOpaque(!darkMode);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(360, 62));
        button.setPreferredSize(new Dimension(360, 62));
        button.addActionListener(event -> action.run());
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                button.setBackground(menuButtonHoverBackground());
            }

            @Override
            public void mouseExited(MouseEvent event) {
                button.setBackground(menuButtonBackground());
            }
        });
        return button;
    }

    private Color menuButtonBackground() {
        return darkMode ? new Color(0, 0, 0, 180) : new Color(244, 235, 215);
    }

    private Color menuButtonHoverBackground() {
        return darkMode ? new Color(55, 55, 55, 220) : SagipTheme.hover(darkMode);
    }

    private Color menuBrandColor() {
        return darkMode ? Color.WHITE : SagipTheme.text(darkMode);
    }

    private Color menuTextColor() {
        return darkMode ? Color.WHITE : SagipTheme.text(darkMode);
    }

    private Color menuDetailColor() {
        return darkMode ? Color.WHITE : SagipTheme.muted(darkMode);
    }

    private JComponent createStorageScreen() {
        JButton clearAllBtn = savedAssessments.isEmpty()
                ? null
                : SagipTheme.pageButton("CLEAR ALL", darkMode, this::clearAllAssessments);
        JPanel page = createSubPage("STORAGE",
                "Saved assessments (" + savedAssessments.size() + ")  \u2022  sagip-storage/scans.json",
                clearAllBtn);

        if (savedAssessments.isEmpty()) {
            JPanel content = new JPanel();
            content.setOpaque(false);
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBorder(new EmptyBorder(30, 30, 30, 30));

            JLabel icon = SagipTheme.label("\u25A3", 42, SagipTheme.accent(darkMode), Font.BOLD);
            icon.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel empty = SagipTheme.label("No saved assessments yet.", 17, SagipTheme.text(darkMode), Font.BOLD);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel detail = SagipTheme.label(
                    "Click Save Scan during camera detection to save the capture with boxes and date to scans.json.",
                    12, SagipTheme.muted(darkMode), Font.PLAIN);
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
                JPanel row = new JPanel(new BorderLayout(18, 0));
                row.setBackground(SagipTheme.surface(darkMode));
                row.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(SagipTheme.cardLine(darkMode), 1),
                        new EmptyBorder(14, 16, 14, 16)));
                row.setMaximumSize(new Dimension(980, 170));

                // Left: Captured Image Thumbnail (including bounding boxes)
                JComponent thumbComp = createScanThumbnail(rec);
                row.add(thumbComp, BorderLayout.WEST);

                // Center: Metadata, Capture Date & Detections
                JPanel info = new JPanel();
                info.setOpaque(false);
                info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

                RiskLevel level = rec.resolveRiskLevel();
                JLabel riskLbl = SagipTheme.label(level.getLabel() + "  \u00B7  " + rec.detectionCount + " Detections",
                        14, level.getColor(darkMode), Font.BOLD);
                riskLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
                info.add(riskLbl);
                info.add(Box.createVerticalStrut(4));

                JLabel dateLbl = SagipTheme.label("Captured: " + rec.timestamp, 12, SagipTheme.text(darkMode), Font.BOLD);
                dateLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
                info.add(dateLbl);

                if (rec.imageFile != null && !rec.imageFile.isEmpty()) {
                    info.add(Box.createVerticalStrut(3));
                    JLabel pathLbl = SagipTheme.label("File: " + rec.imageFile, 11, SagipTheme.muted(darkMode), Font.PLAIN);
                    pathLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
                    info.add(pathLbl);
                }

                info.add(Box.createVerticalStrut(8));
                String summaryText = rec.detections.isEmpty()
                        ? "No hazard objects detected in this frame."
                        : rec.formatSummaries(4);
                JLabel summaryLbl = SagipTheme.label("<html>" + summaryText + "</html>", 12, SagipTheme.text(darkMode), Font.PLAIN);
                summaryLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
                info.add(summaryLbl);

                row.add(info, BorderLayout.CENTER);

                // Right: View + Delete actions
                JPanel actions = new JPanel();
                actions.setOpaque(false);
                actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
                JButton viewBtn = SagipTheme.pageButton("VIEW", darkMode, () -> showSnapshotPreviewDialog(rec));
                viewBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
                JButton deleteBtn = SagipTheme.pageButton("DELETE", darkMode, () -> deleteAssessment(rec));
                deleteBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
                actions.add(viewBtn);
                actions.add(Box.createVerticalStrut(8));
                actions.add(deleteBtn);
                row.add(actions, BorderLayout.EAST);

                list.add(row);
                list.add(Box.createVerticalStrut(12));
            }
            JScrollPane scroll = new JScrollPane(list);
            scroll.setBorder(null);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            page.add(scroll, BorderLayout.CENTER);
        }
        return page;
    }

    private JComponent createScanThumbnail(SavedScanRecord rec) {
        JPanel box = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (rec.snapshot != null) {
                    g.drawImage(rec.snapshot, 2, 2, getWidth() - 4, getHeight() - 4, null);
                } else {
                    g.setColor(SagipTheme.preview(darkMode));
                    g.fillRect(2, 2, getWidth() - 4, getHeight() - 4);
                }
            }
        };
        box.setPreferredSize(new Dimension(220, 132));
        box.setMinimumSize(new Dimension(220, 132));
        box.setMaximumSize(new Dimension(220, 132));
        box.setBorder(BorderFactory.createLineBorder(SagipTheme.accent(darkMode), 2));
        box.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        box.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showSnapshotPreviewDialog(rec);
            }
        });
        return box;
    }

    private void showSnapshotPreviewDialog(SavedScanRecord rec) {
        JDialog dialog = new JDialog(this, "Saved Capture - " + rec.timestamp, true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(SagipTheme.background(darkMode));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(SagipTheme.surface(darkMode));
        top.setBorder(new EmptyBorder(12, 18, 12, 18));
        RiskLevel level = rec.resolveRiskLevel();
        top.add(SagipTheme.label(level.getLabel() + "  \u00B7  Captured: " + rec.timestamp,
                14, level.getColor(darkMode), Font.BOLD), BorderLayout.WEST);
        top.add(SagipTheme.pageButton("CLOSE", darkMode, dialog::dispose), BorderLayout.EAST);

        JPanel canvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (rec.snapshot != null) {
                    int pw = getWidth();
                    int ph = getHeight();
                    double scale = Math.min((double) pw / rec.snapshot.getWidth(), (double) ph / rec.snapshot.getHeight());
                    int dw = (int) (rec.snapshot.getWidth() * scale);
                    int dh = (int) (rec.snapshot.getHeight() * scale);
                    int dx = (pw - dw) / 2;
                    int dy = (ph - dh) / 2;
                    g.drawImage(rec.snapshot, dx, dy, dw, dh, null);
                }
            }
        };
        canvas.setBackground(SagipTheme.preview(darkMode));
        canvas.setPreferredSize(new Dimension(840, 500));

        dialog.add(top, BorderLayout.NORTH);
        dialog.add(canvas, BorderLayout.CENTER);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
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
        return createSubPage(title, subtitle, null);
    }

    private JPanel createSubPage(String title, String subtitle, JComponent extraButton) {
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

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightActions.setOpaque(false);
        if (extraButton != null) {
            rightActions.add(extraButton);
        }
        rightActions.add(SagipTheme.pageButton("BACK TO MENU", darkMode, this::navigateToMainMenu));
        header.add(rightActions, BorderLayout.EAST);

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
