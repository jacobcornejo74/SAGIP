import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;

public class CameraPage extends JPanel {
    private static final long serialVersionUID = 1L;

    private static final String FONT = "Segoe UI";
    private static final int LEGEND_WIDTH = 380;
    private static final int LEGEND_MIN_WIDTH = 360;

    public interface CameraControlListener {
        void onStartRequested();

        void onStopRequested();
    }

    private final boolean darkMode;
    private final JPanel topBarSlot;
    private final JPanel tabArea;
    private final JButton backButton;
    private final CameraPlaceholder cameraView;
    private final JButton startStopButton;
    private final JLabel statusLabel;
    private final JLabel detectionsLabel;

    private boolean running = false;
    private CameraControlListener listener;

    public CameraPage() {
        this(true);
    }

    public CameraPage(boolean darkMode) {
        this.darkMode = darkMode;
        setLayout(new BorderLayout());
        setBackground(paletteBackground());

        tabArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tabArea.setOpaque(false);
        backButton = pageButton("BACK TO MENU");
        backButton.setVisible(false);
        tabArea.add(backButton);

        topBarSlot = new JPanel(new BorderLayout());
        topBarSlot.setOpaque(false);
        topBarSlot.add(buildHeader(), BorderLayout.CENTER);

        cameraView = new CameraPlaceholder();
        startStopButton = pageButton("START CAMERA");
        statusLabel = label("", 13, paletteMuted(), Font.BOLD);
        detectionsLabel = label("", 14, paletteText(), Font.BOLD);

        add(topBarSlot, BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        startStopButton.addActionListener(event -> {
            boolean next = !running;
            setCameraRunning(next);
            if (listener != null) {
                if (next) {
                    listener.onStartRequested();
                } else {
                    listener.onStopRequested();
                }
            }
        });

        setCameraRunning(false);
        setDetectionCount(0);
    }

    /** PLaceholder, add the real camera here. */
    public JPanel getCameraView() {
        return cameraView;
    }

    /** Right side of the header. Add page tab buttons here if needed. */
    public JPanel getTabArea() {
        return tabArea;
    }

    /** BACK TO MENU button in the header. */
    public void setBackAction(Runnable action) {
        for (ActionListener existing : backButton.getActionListeners()) {
            backButton.removeActionListener(existing);
        }
        if (action != null) {
            backButton.addActionListener(event -> action.run());
        }
        backButton.setVisible(action != null);
    }

    public void setTopBar(JComponent bar) {
        topBarSlot.removeAll();
        topBarSlot.add(bar, BorderLayout.CENTER);
        topBarSlot.revalidate();
        topBarSlot.repaint();
    }

    public void setCameraControlListener(CameraControlListener l) {
        this.listener = l;
    }

    public void setDetectionCount(int count) {
        detectionsLabel.setText(String.format("DETECTIONS  %02d", count));
    }

    public void setCameraRunning(boolean isRunning) {
        this.running = isRunning;
        if (isRunning) {
            startStopButton.setText("STOP CAMERA");
            setButtonBorder(startStopButton, riskCritical());
            statusLabel.setText("\u25CF LIVE");
            statusLabel.setForeground(riskLow());
        } else {
            startStopButton.setText("START CAMERA");
            setButtonBorder(startStopButton, paletteAccent());
            statusLabel.setText("\u25CF STANDBY");
            statusLabel.setForeground(paletteMuted());
        }
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(paletteSurface());
        header.setBorder(new EmptyBorder(18, 24, 18, 24));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(label("SAGIP", 20, paletteText(), Font.BOLD));
        titles.add(Box.createVerticalStrut(3));
        titles.add(label("Camera assessment", 13, paletteMuted(), Font.PLAIN));

        JPanel tabHolder = new JPanel(new GridBagLayout());
        tabHolder.setOpaque(false);
        tabHolder.add(tabArea);

        header.add(titles, BorderLayout.WEST);
        header.add(tabHolder, BorderLayout.EAST);
        return header;
    }

    private JComponent buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(paletteSurface());
        footer.setBorder(new EmptyBorder(10, 24, 10, 24));
        footer.add(label("SAGIP  \u2022  FIRE & EARTHQUAKE PREPAREDNESS", 11, paletteMuted(), Font.BOLD),
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

        // Header: titles (left) + status (right)
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel eyebrow = label("LIVE CAMERA FEED", 12, paletteAccent(), Font.BOLD);
        JLabel title = label("Live hazard detection", 22, paletteText(), Font.BOLD);
        JLabel sub = label("Environmental risk assessment", 13, paletteMuted(), Font.PLAIN);
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

        // Footer: detections + start/stop
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(detectionsLabel, BorderLayout.WEST);
        footer.add(startStopButton, BorderLayout.EAST);

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
        JLabel eyebrow = label("LEGEND", 12, paletteAccent(), Font.BOLD);
        JLabel sub = label("How to read the boxes on the camera feed", 13, paletteMuted(), Font.PLAIN);
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
        list.add(legendItem(riskCritical(), null, "Critical",
                "Immediate danger. Fix or remove this hazard first."));
        list.add(legendItem(riskHigh(), null, "High",
                "Serious hazard likely to cause injury or damage. Address as soon as possible."));
        list.add(legendItem(riskMedium(), null, "Medium",
                "Moderate risk. Schedule a fix and keep monitoring."));
        list.add(legendItem(riskLow(), null, "Low",
                "Minor risk. Keep it in mind during routine safety checks."));

        list.add(Box.createVerticalStrut(10));
        list.add(sectionLabel("HAZARD TYPE  (LABEL TAG)"));
        list.add(legendItem(paletteMuted(), "F", "Fire hazard",
                "Objects that can start or spread a fire, such as an unattended iron or an overloaded outlet."));
        list.add(legendItem(paletteMuted(), "E", "Earthquake hazard",
                "Objects that can fall or topple during shaking, such as an unsecured shelf or heavy items on high shelves."));

        list.add(Box.createVerticalStrut(10));
        list.add(new WrappedText("Each box shows the hazard name and detection confidence. "
                + "Recommendations appear in the hazard analysis panel.", 12, paletteMuted()));

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
                trackColor = paletteSurface();
                thumbColor = paletteMuted();
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return emptyScrollButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return emptyScrollButton();
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
                g.setColor(paletteSurface());
                g.fillRect(r.x, r.y, r.width, r.height);
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                if (r.isEmpty()) {
                    return;
                }
                g.setColor(thumbColor);
                g.fillRect(r.x + 1, r.y, r.width - 2, r.height);
            }
        });

        card.add(head, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JComponent sectionLabel(String text) {
        JLabel l = label(text, 13, paletteMuted(), Font.BOLD);
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
        JLabel nameLabel = label(name, 15, paletteText(), Font.BOLD);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(nameLabel);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(new WrappedText(description, 13, paletteMuted()));

        row.add(swatchWrap, BorderLayout.WEST);
        row.add(textPanel, BorderLayout.CENTER);
        return row;
    }

    // =====================================================================
    // Helpers (same style as MainPage.pageButton / label)
    // =====================================================================

    private JButton emptyScrollButton() {
        JButton button = new JButton();
        Dimension none = new Dimension(0, 0);
        button.setPreferredSize(none);
        button.setMinimumSize(none);
        button.setMaximumSize(none);
        return button;
    }

    private JButton pageButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font(FONT, Font.BOLD, 13));
        button.setForeground(paletteText());
        button.setBackground(paletteSurface());
        setButtonBorder(button, paletteAccent());
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                button.setBackground(paletteHover());
            }

            @Override
            public void mouseExited(MouseEvent event) {
                button.setBackground(paletteSurface());
            }
        });
        return button;
    }

    private void setButtonBorder(JButton button, Color lineColor) {
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(lineColor, 1),
                new EmptyBorder(10, 16, 10, 16)));
    }

    private JLabel label(String text, int size, Color color, int style) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(FONT, style, size));
        label.setForeground(color);
        return label;
    }

    // Palette
    private Color paletteBackground() {
        return darkMode ? new Color(29, 34, 36) : new Color(241, 235, 220);
    }

    private Color paletteSurface() {
        return darkMode ? new Color(42, 48, 49) : new Color(224, 214, 193);
    }

    private Color paletteText() {
        return darkMode ? new Color(247, 241, 226) : new Color(49, 42, 34);
    }

    private Color paletteMuted() {
        return darkMode ? new Color(205, 198, 180) : new Color(104, 91, 73);
    }

    private Color paletteAccent() {
        return darkMode ? new Color(243, 145, 55) : new Color(190, 94, 37);
    }

    // Button hover color
    private Color paletteHover() {
        return darkMode ? new Color(104, 72, 49) : new Color(255, 190, 105);
    }

    // Camera preview background
    private Color palettePreview() {
        return darkMode ? new Color(31, 34, 33) : new Color(229, 222, 208);
    }

    // Card outline
    private Color paletteCardLine() {
        return darkMode ? new Color(255, 255, 255, 42) : new Color(65, 47, 33, 55);
    }

    // Risk level colors
    private Color riskCritical() {
        return darkMode ? new Color(226, 72, 63) : new Color(184, 48, 42);
    }

    private Color riskHigh() {
        return darkMode ? new Color(243, 145, 55) : new Color(204, 104, 28);
    }

    private Color riskMedium() {
        return darkMode ? new Color(240, 196, 60) : new Color(190, 142, 16);
    }

    private Color riskLow() {
        return darkMode ? new Color(104, 190, 116) : new Color(52, 132, 68);
    }

    private class Card extends JPanel {
        private static final long serialVersionUID = 1L;

        Card() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setColor(paletteSurface());
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(paletteCardLine());
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    // So nothing hides behind the scrollbar.
    private static class ViewportWidthPanel extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;

        ViewportWidthPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(16, visibleRect.height - 16);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    // So text doesn't clip with border.
    private class WrappedText extends JComponent {
        private static final long serialVersionUID = 1L;
        private final String text;
        private final Font font;
        private final Color color;
        private int lastWidth = -1;

        WrappedText(String text, int size, Color color) {
            this.text = text;
            this.font = new Font(FONT, Font.PLAIN, size);
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
            if (current.length() > 0) {
                lines.add(current.toString());
            }
            return lines;
        }

        @Override
        public Dimension getPreferredSize() {
            int width = getWidth() > 0 ? getWidth() : 240;
            return new Dimension(120, wrap(width).size() * getFontMetrics(font).getHeight());
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(0, getPreferredSize().height);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

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

    // Small color box for hazard types.
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
            int w = getWidth();
            int h = getHeight();

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
                g.setFont(new Font(FONT, Font.BOLD, 15));
                FontMetrics fm = g.getFontMetrics();
                g.setColor(paletteText());
                g.drawString(glyph, (w - fm.stringWidth(glyph)) / 2,
                        (h - fm.getHeight()) / 2 + fm.getAscent());
            }
            g.dispose();
        }
    }

    /**
     * Placeholder for the live camera feed.
     * The real camera component can be added:
     *     page.getCameraView().add(myCameraComponent, BorderLayout.CENTER);
     * Then call setPlaceholderVisible(false).
     */
    public class CameraPlaceholder extends JPanel {
        private static final long serialVersionUID = 1L;
        private boolean placeholderVisible = true;

        CameraPlaceholder() {
            super(new BorderLayout());
            setOpaque(false);
            setMinimumSize(new Dimension(360, 260));
            setPreferredSize(new Dimension(800, 480));
        }

        public void setPlaceholderVisible(boolean visible) {
            this.placeholderVisible = visible;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            if (!placeholderVisible) {
                return;
            }
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            g.setColor(palettePreview());
            g.fillRect(0, 0, w, h);

            Color muted = paletteMuted();
            g.setColor(new Color(muted.getRed(), muted.getGreen(), muted.getBlue(), 26));
            g.setStroke(new BasicStroke(1.2f));
            for (int x = -h; x < w; x += 28) {
                g.drawLine(x, h, x + h, 0);
            }

            // Accent border
            g.setColor(paletteAccent());
            g.setStroke(new BasicStroke(2f));
            g.drawRect(1, 1, w - 3, h - 3);

            // Camera icon
            int cx = w / 2;
            int cy = h / 2 - 30;
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(muted);
            g.draw(new RoundRectangle2D.Float(cx - 30, cy - 16, 60, 40, 8, 8));
            g.draw(new Ellipse2D.Float(cx - 12, cy - 8, 24, 24));
            g.drawLine(cx - 10, cy - 21, cx + 10, cy - 21);

            drawCentered(g, "CAMERA PREVIEW", new Font(FONT, Font.BOLD, 20), muted, w, cy + 62);
            drawCentered(g, "Live camera view and hazard boxes will appear here.",
                    new Font(FONT, Font.PLAIN, 15), paletteText(), w, cy + 88);
            g.dispose();
        }

        private void drawCentered(Graphics2D g, String s, Font f, Color c, int width, int baselineY) {
            g.setFont(f);
            g.setColor(c);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(s, (width - fm.stringWidth(s)) / 2, baselineY);
        }
    }

    // Standalone preview (delete or ignore when integrating into MainPage)
    // Run with the argument "light" to preview the light theme.
    public static void main(String[] args) {
        boolean dark = !(args.length > 0 && args[0].equalsIgnoreCase("light"));
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException
                    | UnsupportedLookAndFeelException exception) {
                System.err.println("Could not load the system look and feel: " + exception.getMessage());
            }
            JFrame frame = new JFrame("SAGIP - Camera Page");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            CameraPage page = new CameraPage(dark);
            page.setBackAction(() -> System.out.println("Back to menu"));
            frame.setContentPane(page);
            frame.setMinimumSize(new Dimension(900, 600));
            frame.setSize(1180, 740);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}