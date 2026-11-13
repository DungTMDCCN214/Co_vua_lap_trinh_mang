package client.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Desktop window and screen navigation; application behavior remains in client controllers. */
public final class MainWindow extends JFrame {
    public static final String LOGIN = "LOGIN";
    public static final String LOBBY = "LOBBY";
    public static final String GAME = "GAME";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel screens = new JPanel(cardLayout);

    public MainWindow(Component login, Component lobby, Component game, Runnable onClose) {
        super("Khai Cuộc | Cờ vua online");
        screens.setBackground(ThemeStyle.PAPER);
        screens.add(login, LOGIN);
        screens.add(lobby, LOBBY);
        screens.add(game, GAME);
        setContentPane(screens);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                onClose.run();
            }
        });
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setMinimumSize(new Dimension(720, 560));
        setSize(
                Math.max(720, Math.min(1100, screen.width - 60)),
                Math.max(560, Math.min(760, screen.height - 80))
        );
        setLocationRelativeTo(null);
    }

    public void showScreen(String screen) {
        cardLayout.show(screens, screen);
    }

    public void showWindow() {
        setVisible(true);
    }

    public void showInfo(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public void showGameResult(String result, String reason, String details) {
        new GameResultDialog(this, result, reason, details).setVisible(true);
    }

    public void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public boolean confirm(String title, String message) {
        return JOptionPane.showConfirmDialog(
                this, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE
        ) == JOptionPane.YES_OPTION;
    }
}
