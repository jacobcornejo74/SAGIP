package FrontEnd;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

public class MainPage {
    private static final String HOME = "home";
    private static final String CAMERA = "camera";
    private static final String STORAGE = "storage";
    private static final String MANUAL = "manual";

    private JFrame frame;
    private JPanel screens;
    private CardLayout cardLayout;
    private boolean darkMode = true;

    private MainPage() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception exception) {
                System.err.println("Could not load the system look and feel: " + exception.getMessage());
            }
            new MainPage().showWindow();
        });
    }

    private void showWindow() {
        frame = new JFrame("SAGIP");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(900, 600));
        frame.setSize(1180, 740);
        frame.setLocationRelativeTo(null);
        rebuildScreens(HOME);
        frame.setVisible(true);
    }

    private void rebuildScreens(String selectedScreen) {
        cardLayout = new CardLayout();
        screens = new JPanel(cardLayout);
        screens.add(createHome(), HOME);
        screens.add(createCameraPage(), CAMERA);
        screens.add(createStoragePage(), STORAGE);
        screens.add(createManualPage(), MANUAL);
        frame.setContentPane(screens);
        cardLayout.show(screens, selectedScreen);
        frame.revalidate();
        frame.repaint();
    }

    private JComponent createHome() {
        MenuBackground background = new MenuBackground(darkMode);
        background.setLayout(new BorderLayout());
        background.setBorder(new EmptyBorder(25, 30, 22, 30));

        JLabel edition = label("SMART INCIDENT PREVENTION", 11, paletteMuted(), Font.BOLD);
        edition.setHorizontalAlignment(SwingConstants.RIGHT);
        background.add(edition, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        JPanel menu = new ContrastPanel(darkMode);
        menu.setOpaque(false);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBorder(new EmptyBorder(22, 32, 22, 32));

        JLabel title = label("SAGIP", 58, menuTextColor(), Font.BOLD);
        title.setFont(new Font("Segoe UI", Font.BOLD, 58));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel subtitle = label("SMART AI GUARD FOR INCIDENT PREVENTION", 12, menuTextColor(), Font.PLAIN);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        menu.add(title);
        menu.add(Box.createVerticalStrut(3));
        menu.add(subtitle);
        menu.add(Box.createVerticalStrut(34));
        menu.add(menuButton("START", "Scan a space for hazards", this::showCamera));
        menu.add(Box.createVerticalStrut(9));
        menu.add(menuButton("STORAGE", "View saved assessments", this::showStorage));
        menu.add(Box.createVerticalStrut(9));
        menu.add(menuButton("THEME  ·  " + (darkMode ? "DARK" : "LIGHT"),
                "Switch the appearance", this::toggleTheme));
        menu.add(Box.createVerticalStrut(9));
        menu.add(menuButton("MANUAL", "Learn how to use SAGIP", this::showManual));

        JPanel menuHolder = new JPanel(new java.awt.GridBagLayout());
        menuHolder.setOpaque(false);
        menuHolder.add(menu);
        center.add(menuHolder, BorderLayout.CENTER);
        background.add(center, BorderLayout.CENTER);

        JLabel footer = label("SAGIP  •  FIRE & EARTHQUAKE PREPAREDNESS", 10, paletteText(), Font.BOLD);
        footer.setHorizontalAlignment(SwingConstants.CENTER);
        background.add(footer, BorderLayout.SOUTH);
        return background;
    }

    private JButton menuButton(String title, String detail, Runnable action) {
        JButton button = new JButton("<html><div style='text-align:center;'>"
                + "<span style='font-family:Segoe UI;font-size:14pt;font-weight:700;color:"
                + htmlColor(menuTextColor()) + ";'>"
                + title + "</span><br>"
                + "<span style='font-family:Segoe UI;font-size:9pt;font-weight:400;color:"
                + htmlColor(menuDetailColor()) + ";'>"
                + detail + "</span></div></html>");
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(menuTextColor());
        button.setBackground(darkMode ? new Color(48, 53, 56) : new Color(244, 235, 215));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(darkMode ? new Color(18, 23, 28) : new Color(99, 78, 55), 1),
                new EmptyBorder(7, 20, 7, 20)));
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(360, 62));
        button.setPreferredSize(new Dimension(360, 62));
        button.addActionListener(event -> action.run());
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                button.setBackground(darkMode ? new Color(104, 72, 49) : new Color(255, 190, 105));
            }

            @Override
            public void mouseExited(MouseEvent event) {
                button.setBackground(darkMode ? new Color(48, 53, 56) : new Color(244, 235, 215));
            }
        });
        return button;
    }

    private JComponent createCameraPage() {
        JPanel page = createPage("NEW ASSESSMENT", "Camera assessment");
        JPanel preview = new JPanel(new BorderLayout());
        preview.setBackground(darkMode ? new Color(31, 34, 33) : new Color(229, 222, 208));
        preview.setBorder(BorderFactory.createLineBorder(paletteAccent(), 2));
        preview.setPreferredSize(new Dimension(600, 340));
        JLabel placeholder = label("CAMERA PREVIEW", 18, paletteMuted(), Font.BOLD);
        placeholder.setHorizontalAlignment(SwingConstants.CENTER);
        preview.add(placeholder, BorderLayout.CENTER);

        JLabel instruction = label("Position your space in view, then capture to begin.", 13, paletteText(), Font.PLAIN);
        instruction.setHorizontalAlignment(SwingConstants.CENTER);
        JButton capture = pageButton("CAPTURE", () -> JOptionPane.showMessageDialog(frame,
                "Camera capture will be connected when the assessment feature is implemented.",
                "Camera not connected", JOptionPane.INFORMATION_MESSAGE));

        JPanel content = centeredContent();
        content.add(preview);
        content.add(Box.createVerticalStrut(14));
        content.add(instruction);
        content.add(Box.createVerticalStrut(14));
        content.add(capture);
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    private JComponent createStoragePage() {
        JPanel page = createPage("STORAGE", "Saved assessments");
        JPanel content = centeredContent();
        JLabel icon = label("▣", 42, paletteAccent(), Font.BOLD);
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel empty = label("No saved assessments yet.", 17, paletteText(), Font.BOLD);
        empty.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel detail = label("Completed scans will appear here.", 12, paletteMuted(), Font.PLAIN);
        detail.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(Box.createVerticalGlue());
        content.add(icon);
        content.add(Box.createVerticalStrut(12));
        content.add(empty);
        content.add(Box.createVerticalStrut(6));
        content.add(detail);
        content.add(Box.createVerticalGlue());
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    private JComponent createManualPage() {
        JPanel page = createPage("MANUAL", "Quick guide");
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(35, 55, 35, 55));
        content.add(guideItem("1", "Start", "Open a new assessment and point the camera at the space."));
        content.add(Box.createVerticalStrut(18));
        content.add(guideItem("2", "Review", "Check detected fire or earthquake hazards and their severity."));
        content.add(Box.createVerticalStrut(18));
        content.add(guideItem("3", "Respond", "Follow the suggested corrective actions and save the assessment."));
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    private JPanel createPage(String title, String subtitle) {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(paletteBackground());
        page.add(createPageHeader(title, subtitle), BorderLayout.NORTH);
        page.add(createPageFooter(), BorderLayout.SOUTH);
        return page;
    }

    private JComponent createPageHeader(String title, String subtitle) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(paletteSurface());
        header.setBorder(new EmptyBorder(18, 24, 18, 24));
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(label(title, 18, paletteText(), Font.BOLD));
        titles.add(Box.createVerticalStrut(3));
        titles.add(label(subtitle, 11, paletteMuted(), Font.PLAIN));
        header.add(titles, BorderLayout.WEST);
        header.add(pageButton("BACK TO MENU", this::showHome), BorderLayout.EAST);
        return header;
    }

    private JComponent createPageFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(paletteSurface());
        footer.setBorder(new EmptyBorder(10, 24, 10, 24));
        footer.add(label("SAGIP  •  FIRE & EARTHQUAKE PREPAREDNESS", 10, paletteMuted(), Font.BOLD),
                BorderLayout.WEST);
        return footer;
    }

    private JPanel centeredContent() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(30, 30, 30, 30));
        return content;
    }

    private JComponent guideItem(String number, String title, String detail) {
        JPanel row = new JPanel(new BorderLayout(16, 0));
        row.setOpaque(true);
        row.setBackground(paletteSurface());
        row.setBorder(new EmptyBorder(18, 20, 18, 20));
        row.setMaximumSize(new Dimension(760, 92));
        JLabel step = label(number, 24, paletteAccent(), Font.BOLD);
        step.setHorizontalAlignment(SwingConstants.CENTER);
        step.setPreferredSize(new Dimension(42, 42));
        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        words.add(label(title, 15, paletteText(), Font.BOLD));
        words.add(Box.createVerticalStrut(6));
        words.add(label("<html>" + detail + "</html>", 12, paletteMuted(), Font.PLAIN));
        row.add(step, BorderLayout.WEST);
        row.add(words, BorderLayout.CENTER);
        return row;
    }

    private JButton pageButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(paletteText());
        button.setBackground(paletteSurface());
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(paletteAccent(), 1),
                new EmptyBorder(10, 16, 10, 16)));
        button.setFocusPainted(false);
        button.addActionListener(event -> action.run());
        return button;
    }

    private JLabel label(String text, int size, Color color, int style) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", style, size));
        label.setForeground(color);
        return label;
    }

    private String htmlColor(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    private void toggleTheme() {
        darkMode = !darkMode;
        rebuildScreens(HOME);
    }

    private void showHome() {
        cardLayout.show(screens, HOME);
    }

    private void showCamera() {
        cardLayout.show(screens, CAMERA);
    }

    private void showStorage() {
        cardLayout.show(screens, STORAGE);
    }

    private void showManual() {
        cardLayout.show(screens, MANUAL);
    }

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

    private Color menuTextColor() {
        return darkMode ? new Color(153, 158, 162) : paletteText();
    }

    private Color menuDetailColor() {
        return darkMode ? new Color(128, 134, 139) : paletteMuted();
    }

    private static class MenuBackground extends JPanel {
        private static final long serialVersionUID = 1L;

        private final boolean darkMode;
        private final Timer animation;
        private int animationFrame;

        private MenuBackground(boolean darkMode) {
            this.darkMode = darkMode;
            setOpaque(true);
            animation = new Timer(45, event -> {
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
                    case 0:
                    case 2:
                    case 5:
                        drawHouse(g, x, base, tileWidth, height, type);
                        break;
                    case 1:
                    case 4:
                        drawOffice(g, x, base, tileWidth, height, type == 4);
                        break;
                    default:
                        drawSchool(g, x, base, tileWidth, height);
                        break;
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

            java.awt.Polygon roof = new java.awt.Polygon(
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
            g.setFont(new Font("Segoe UI", Font.BOLD, Math.max(8, height / 85)));
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

    private static class ContrastPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final boolean darkMode;

        private ContrastPanel(boolean darkMode) {
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
}
