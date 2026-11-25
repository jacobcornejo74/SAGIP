package GUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

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
