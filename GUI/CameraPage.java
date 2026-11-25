package GUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

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
            BufferedImage annotated = cameraView.captureAnnotatedSnapshot();
            if (annotated == null) {
                annotated = engine.getCurrentFrame();
            }
            List<HazardItem> hazards = engine.getDetectedHazards();
            if (annotated != null) {
                app.saveAssessment(annotated, hazards, activeScenario);
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
