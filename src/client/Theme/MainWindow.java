package client.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

/**
 * Cửa sổ chính dùng khung gốc của hệ điều hành (thanh tiêu đề, kéo giãn, chia đôi/chia 4 màn hình
 * của Windows hoạt động bình thường). Hành vi ứng dụng nằm ở các controller trong package client.
 */
public final class MainWindow extends JFrame {
    public static final String LOGIN = "LOGIN";
    public static final String LOBBY = "LOBBY";
    public static final String GAME = "GAME";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel screens = new JPanel(cardLayout);

    public MainWindow(Component login, Component lobby, Component game, Runnable onClose) {
        super("Khai Cuộc | Cờ vua online");
        setIconImage(createAppIcon());

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

    private static Image createAppIcon() {
        int size = 64;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(ThemeStyle.PANEL);
        g.fillRoundRect(1, 1, size - 2, size - 2, 16, 16);
        g.setFont(new Font("Dialog", Font.PLAIN, 48));
        String knight = "\u2658";
        FontMetrics metrics = g.getFontMetrics();
        int x = (size - metrics.stringWidth(knight)) / 2;
        int y = (size - metrics.getHeight()) / 2 + metrics.getAscent();
        g.setColor(ThemeStyle.GREEN_DARK);
        g.drawString(knight, x - 1, y + 1);
        g.drawString(knight, x + 1, y + 1);
        g.drawString(knight, x, y - 1);
        g.drawString(knight, x, y + 2);
        g.setColor(Color.WHITE);
        g.drawString(knight, x, y);
        g.dispose();
        return image;
    }

    public void showScreen(String screen) {
        cardLayout.show(screens, screen);
    }

    public void showWindow() {
        setVisible(true);
    }

    public void showInfo(String title, String message) {
        ThemedNoticeDialog.showNotice(this, title, title, message, false);
    }

    /** Hiện kết quả ván; trả về true nếu người chơi chọn "Về sảnh". */
    public boolean showGameResult(String result, String reason, String details) {
        GameResultDialog dialog = new GameResultDialog(this, result, reason, details);
        dialog.setVisible(true);
        return dialog.toLobby();
    }

    public void showError(String title, String message) {
        ThemedNoticeDialog.showNotice(this, title, "Thông báo lỗi", message, true);
    }

    /** Đóng mọi hộp thoại đang mở (khi ván kết thúc / mất kết nối) để không bị kẹt hộp thoại cũ. */
    public void closeDialogs() {
        for (Window w : getOwnedWindows()) if (w instanceof JDialog && w.isShowing()) w.dispose();
    }

    public boolean confirmSurrender() {
        return confirmDialog("Đầu hàng", "Xác nhận đầu hàng",
                "Bạn chắc chắn muốn kết thúc ván và đầu hàng?", "Đầu hàng", "Tiếp tục chơi", true, "!", ThemeStyle.DANGER);
    }

    public boolean confirmDrawOffer() {
        return confirmDialog("Xin hòa", "Xác nhận xin hòa",
                "Bạn có chắc chắn muốn gửi lời đề nghị hòa cờ đến đối thủ?", "Xin hòa", "Tiếp tục chơi", false, "½", ThemeStyle.GOLD);
    }

    public boolean confirmAcceptDraw() {
        return confirmDialog("Lời đề nghị hòa", "Đề nghị hòa cờ",
                "Đối thủ gửi lời đề nghị hòa cờ. Bạn có đồng ý kết thúc ván đấu với kết quả hòa không?", "Đồng ý hòa", "Từ chối", false, "½", ThemeStyle.GOLD);
    }

    public boolean confirmLogout() {
        return confirmDialog("Đăng xuất", "Xác nhận đăng xuất",
                "Bạn có chắc chắn muốn đăng xuất?", "Đăng xuất", "Ở lại", true, "!", ThemeStyle.DANGER);
    }

    public boolean confirmDialog(String title, String headingText, String message,
                                 String confirmButtonText, String cancelButtonText,
                                 boolean isDanger, String iconChar, Color iconColor) {
        JDialog dialog = new JDialog(this, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        dialog.setResizable(false);

        JPanel content = ThemeStyle.cardPanel(new BorderLayout(0, 20), ThemeStyle.PAPER);
        content.setBackground(ThemeStyle.PAPER);
        content.setBorder(ThemeStyle.cardBorder(22, 24, 20, 24));

        JLabel icon = new JLabel(iconChar, SwingConstants.CENTER);
        icon.setForeground(iconColor);
        icon.setFont(new Font("Serif", Font.BOLD, 26));
        icon.setPreferredSize(new Dimension(48, 48));

        JPanel copy = new JPanel(new BorderLayout(0, 4));
        copy.setOpaque(false);
        JLabel heading = new JLabel("<html><div style='font-size:14pt; font-weight:bold; color:#2c2c2c; margin-bottom:4px;'>"
                + ThemeStyle.html(headingText) + "</div><div style='font-size:10pt; color:#6b7280; width:250px;'>"
                + ThemeStyle.html(message) + "</div></html>");
        heading.setOpaque(false);
        copy.add(heading, BorderLayout.NORTH);

        JPanel messagePanel = new JPanel(new BorderLayout(14, 0));
        messagePanel.setOpaque(false);
        messagePanel.add(icon, BorderLayout.WEST);
        messagePanel.add(copy, BorderLayout.CENTER);

        JButton cancelButton = new JButton(cancelButtonText);
        JButton confirmButton = new JButton(confirmButtonText);
        ThemeStyle.button(cancelButton, false);
        if (isDanger) {
            ThemeStyle.dangerButton(confirmButton);
        } else {
            ThemeStyle.button(confirmButton, true);
        }

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
