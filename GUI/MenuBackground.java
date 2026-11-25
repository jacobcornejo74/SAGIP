package GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;

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
