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
        JDialog dialog = new JDialog(this, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        dialog.setResizable(false);

        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setBackground(ThemeStyle.PAPER);
        content.setBorder(ThemeStyle.cardBorder(22, 24, 20, 24));

        JLabel icon = new JLabel("!", SwingConstants.CENTER);
        icon.setForeground(ThemeStyle.DANGER);
        icon.setFont(new Font("SansSerif", Font.BOLD, 24));
        icon.setPreferredSize(new Dimension(48, 48));

        JPanel copy = new JPanel(new BorderLayout(0, 6));
        copy.setOpaque(false);
        JLabel heading = new JLabel("Xác nhận đầu hàng");
        ThemeStyle.heading(heading, 18);
        JTextArea description = new JTextArea(message);
        description.setEditable(false);
        description.setFocusable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setOpaque(false);
        description.setBorder(BorderFactory.createEmptyBorder());
        description.setForeground(ThemeStyle.MUTED);
        description.setFont(new Font("SansSerif", Font.PLAIN, 13));
        description.setRows(2);
        copy.add(heading, BorderLayout.NORTH);
        copy.add(description, BorderLayout.CENTER);

        JPanel messagePanel = new JPanel(new BorderLayout(14, 0));
        messagePanel.setOpaque(false);
        messagePanel.add(icon, BorderLayout.WEST);
        messagePanel.add(copy, BorderLayout.CENTER);

        JButton cancelButton = new JButton("Tiếp tục chơi");
        JButton confirmButton = new JButton("Đầu hàng");
        ThemeStyle.button(cancelButton, false);
        ThemeStyle.dangerButton(confirmButton);

        boolean[] confirmed = {false};
        cancelButton.addActionListener(event -> dialog.dispose());
        confirmButton.addActionListener(event -> {
            confirmed[0] = true;
            dialog.dispose();
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(cancelButton);
        actions.add(confirmButton);

        content.add(messagePanel, BorderLayout.CENTER);
        content.add(actions, BorderLayout.SOUTH);
        dialog.setContentPane(content);
        dialog.getRootPane().setDefaultButton(cancelButton);
        dialog.getRootPane().registerKeyboardAction(
                event -> dialog.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        return confirmed[0];
    }
}
