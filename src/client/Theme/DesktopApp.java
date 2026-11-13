package client.Theme;

import javax.swing.*;

/** Swing application bootstrap and event-dispatch helpers. */
public final class DesktopApp {
    private DesktopApp() {}

    public static void launch(Runnable startup) {
        SwingUtilities.invokeLater(() -> {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (UnsupportedLookAndFeelException | ClassNotFoundException |
                     InstantiationException | IllegalAccessException exception) {
                System.err.println("Không thể áp dụng giao diện Nimbus: " + exception.getMessage());
            }
            startup.run();
        });
    }

    public static void runOnUiThread(Runnable action) {
        SwingUtilities.invokeLater(action);
    }
}
