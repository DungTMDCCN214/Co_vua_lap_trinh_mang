package client.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

/** Desktop window and screen navigation; application behavior remains in client controllers. */
public final class MainWindow extends JFrame {
    public static final String LOGIN = "LOGIN";
    public static final String LOBBY = "LOBBY";
    public static final String GAME = "GAME";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel screens = new JPanel(cardLayout);
    private Point dragOffset;
    private Rectangle restoreBounds;
    private JButton maximizeButton;

    public MainWindow(Component login, Component lobby, Component game, Runnable onClose) {
        super("Khai Cuộc | Cờ vua online");
        setUndecorated(true);
        setIconImage(createAppIcon());

        screens.setBackground(ThemeStyle.PAPER);
        screens.add(login, LOGIN);
        screens.add(lobby, LOBBY);
        screens.add(game, GAME);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(ThemeStyle.PAPER);
        root.setBorder(BorderFactory.createLineBorder(ThemeStyle.LINE));
        root.add(createTitleBar(onClose), BorderLayout.NORTH);
        root.add(screens, BorderLayout.CENTER);
        setContentPane(root);
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

    private JPanel createTitleBar(Runnable onClose) {
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(ThemeStyle.GREEN_DARK);
        titleBar.setBorder(new EmptyBorder(0, 10, 0, 0));
        titleBar.setPreferredSize(new Dimension(0, 38));

        Image titleIcon = createAppIcon().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        JLabel icon = new JLabel(new ImageIcon(titleIcon));
        icon.setBorder(new EmptyBorder(0, 0, 0, 8));
        JLabel title = new JLabel("KHAI CUỘC  ·  CỜ VUA ONLINE");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 12));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        brand.setOpaque(false);
        brand.add(icon);
        brand.add(title);
        titleBar.add(brand, BorderLayout.WEST);

        JButton minimizeButton = createTitleButton(TitleBarButton.MINIMIZE, "Thu nhỏ");
        maximizeButton = createTitleButton(TitleBarButton.MAXIMIZE, "Phóng to");
        JButton closeButton = createTitleButton(TitleBarButton.CLOSE, "Đóng");
        closeButton.addActionListener(event -> onClose.run());
        minimizeButton.addActionListener(event -> setState(Frame.ICONIFIED));
        maximizeButton.addActionListener(event -> toggleMaximized());

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        controls.setOpaque(false);
        controls.add(minimizeButton);
        controls.add(maximizeButton);
        controls.add(closeButton);
        titleBar.add(controls, BorderLayout.EAST);

        MouseAdapter dragHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                if (event.getButton() == MouseEvent.BUTTON1 && !isMaximized()) {
                    dragOffset = event.getLocationOnScreen();
                    Point location = getLocation();
                    dragOffset.translate(-location.x, -location.y);
                }
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                if (dragOffset != null && !isMaximized()) {
                    Point location = event.getLocationOnScreen();
                    setLocation(location.x - dragOffset.x, location.y - dragOffset.y);
                }
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                dragOffset = null;
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(event)) {
                    toggleMaximized();
                }
            }
        };
        titleBar.addMouseListener(dragHandler);
        titleBar.addMouseMotionListener(dragHandler);
        brand.addMouseListener(dragHandler);
        brand.addMouseMotionListener(dragHandler);
        title.addMouseListener(dragHandler);
        title.addMouseMotionListener(dragHandler);
        icon.addMouseListener(dragHandler);
        icon.addMouseMotionListener(dragHandler);

        addWindowStateListener(event -> updateMaximizeButton());
        return titleBar;
    }

    private JButton createTitleButton(int type, String tooltip) {
        JButton button = new TitleBarButton(type);
        button.setToolTipText(tooltip);
        return button;
    }

    private void toggleMaximized() {
        if (isMaximized()) {
            setExtendedState(Frame.NORMAL);
            if (restoreBounds != null) setBounds(restoreBounds);
        } else {
            restoreBounds = getBounds();
            GraphicsConfiguration gc = getGraphicsConfiguration(); // không che thanh taskbar
            if (gc != null) {
                Rectangle b = gc.getBounds();
                Insets in = Toolkit.getDefaultToolkit().getScreenInsets(gc);
                setMaximizedBounds(new Rectangle(in.left, in.top,
                        b.width - in.left - in.right, b.height - in.top - in.bottom));
            }
            setExtendedState(Frame.MAXIMIZED_BOTH);
        }
        updateMaximizeButton();
    }

    private boolean isMaximized() {
        return (getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
    }

    private void updateMaximizeButton() {
        if (maximizeButton == null) return;
        maximizeButton.repaint();
        maximizeButton.setToolTipText(isMaximized() ? "Khôi phục" : "Phóng to");
    }

    private final class TitleBarButton extends JButton {
        private static final int MINIMIZE = 0;
        private static final int MAXIMIZE = 1;
        private static final int CLOSE = 2;
        private final int type;

        private TitleBarButton(int type) {
            this.type = type;
            setPreferredSize(new Dimension(46, 38));
            setBorderPainted(false);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (getModel().isPressed()) {
                g.setColor(type == CLOSE ? ThemeStyle.DANGER_DARK : new Color(38, 58, 47));
                g.fillRect(0, 0, getWidth(), getHeight());
            } else if (getModel().isRollover()) {
                g.setColor(type == CLOSE ? ThemeStyle.DANGER : new Color(65, 92, 75));
                g.fillRect(0, 0, getWidth(), getHeight());
            }

            g.setColor(new Color(245, 248, 245));
            g.setStroke(new BasicStroke(1.2f));
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            if (type == MINIMIZE) {
                g.drawLine(cx - 5, cy + 3, cx + 5, cy + 3);
            } else if (type == MAXIMIZE && isMaximized()) {
                g.drawRect(cx - 4, cy - 3, 8, 8);
                g.drawLine(cx - 2, cy - 5, cx + 5, cy - 5);
                g.drawLine(cx + 5, cy - 5, cx + 5, cy + 2);
            } else if (type == MAXIMIZE) {
                g.drawRect(cx - 5, cy - 5, 10, 10);
            } else {
                g.drawLine(cx - 4, cy - 4, cx + 4, cy + 4);
                g.drawLine(cx + 4, cy - 4, cx - 4, cy + 4);
            }
            g.dispose();
        }
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
