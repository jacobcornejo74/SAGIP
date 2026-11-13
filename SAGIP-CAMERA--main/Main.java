import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Primary entry point for launching the SAGIP application.
 *
 * Configures system font rendering and look-and-feel settings, then creates
 * and displays the main application window on the Swing Event Dispatch Thread.
 */
public class Main {

    /**
     * Starts the SAGIP disaster hazard detection system.
     *
     * @param args optional command-line arguments
     */
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Use the default Swing if system integration fails.
            }
            new DisasterHazardSystemApp().setVisible(true);
        });
    }
}
