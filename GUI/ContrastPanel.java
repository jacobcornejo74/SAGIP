package GUI;

import javax.swing.*;
import java.awt.*;

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
