package client.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * Standardized themed notice dialog for info, success, and error alerts.
 * Replaces default JOptionPane popups with the unified Paper/Card theme.
 */
final class ThemedNoticeDialog extends JDialog {
    ThemedNoticeDialog(Window owner, String title, String headingText, String messageText, boolean isError) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel content = ThemeStyle.cardPanel(new BorderLayout(0, 18), ThemeStyle.PAPER);
        content.setBackground(ThemeStyle.PAPER);
        content.setBorder(ThemeStyle.cardBorder(22, 24, 20, 24));

        Color iconColor = isError ? ThemeStyle.DANGER : ThemeStyle.GREEN;
        String iconChar = isError ? "!" : "i";

        JLabel icon = new JLabel(iconChar, SwingConstants.CENTER);
        icon.setForeground(iconColor);
        icon.setFont(new Font("Serif", Font.BOLD, 26));
        icon.setPreferredSize(new Dimension(44, 44));

        JPanel copy = new JPanel(new BorderLayout(0, 4));
        copy.setOpaque(false);

        JLabel heading = new JLabel("<html><div style='font-size:13pt; font-weight:bold; color:#2c2c2c; margin-bottom:4px;'>"
                + ThemeStyle.html(headingText) + "</div><div style='font-size:10pt; color:#6b7280; width:250px;'>"
                + ThemeStyle.html(messageText) + "</div></html>");
        heading.setOpaque(false);

        copy.add(heading, BorderLayout.NORTH);

        JPanel messagePanel = new JPanel(new BorderLayout(14, 0));
        messagePanel.setOpaque(false);
        messagePanel.add(icon, BorderLayout.WEST);
        messagePanel.add(copy, BorderLayout.CENTER);

        JButton okButton = new JButton("Đồng ý");
        ThemeStyle.button(okButton, true);
        okButton.setPreferredSize(new Dimension(96, 36));
        okButton.addActionListener(e -> dispose());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(okButton);

        content.add(messagePanel, BorderLayout.CENTER);
        content.add(actions, BorderLayout.SOUTH);

        setContentPane(content);
        getRootPane().setDefaultButton(okButton);
        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        pack();
        setLocationRelativeTo(owner);
    }

    static void showNotice(Component parent, String title, String heading, String message, boolean isError) {
        Window window = parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
        new ThemedNoticeDialog(window, title, heading, message, isError).setVisible(true);
    }
}
