package client.Theme;

import protocol.Protocol;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Indie-inspired login and account-creation screens. */
public final class LoginView extends JPanel {
    public interface Listener {
        void onLogin(String host, String port, String username, String password);
        void onRegister(String host, String port, String username, String password);
    }

    private static final String LOGIN = "LOGIN";
    private static final String REGISTER = "REGISTER";

    private final CardLayout formsLayout = new CardLayout();
    private final JPanel forms = new JPanel(formsLayout);

    private final JTextField serverHost = field("localhost");
    private final JTextField serverPort = field(String.valueOf(Protocol.DEFAULT_PORT));
    private final JTextField loginUsername = field("");
    private final JPasswordField loginPassword = passwordField();
    private final JLabel loginStatus = new JLabel(" ");
    private final JButton loginButton = new JButton("Vào sảnh");
    private final JButton createAccountButton = new JButton("Tạo tài khoản");

    private final JTextField registerUsername = field("");
    private final JPasswordField registerPassword = passwordField();
    private final JLabel registerStatus = new JLabel(" ");
    private final JButton registerButton = new JButton("Tạo tài khoản");
    private final JButton backButton = new JButton("Quay lại");

    private Listener listener;

    public LoginView() {
        super(new BorderLayout());
        setBackground(ThemeStyle.PAPER);
        add(createBrandPanel(), BorderLayout.WEST);

        JPanel right = new JPanel(new BorderLayout(0, 20));
        right.setBackground(ThemeStyle.PAPER);
        right.setBorder(new EmptyBorder(18, 54, 36, 54));
        right.add(createServerSettings(), BorderLayout.NORTH);
        forms.setOpaque(false);
        forms.add(createForm(LOGIN, "Chào mừng trở lại", "Đăng nhập để tiếp tục ván cờ.",
                loginUsername, loginPassword, loginButton, createAccountButton, loginStatus), LOGIN);
        forms.add(createForm(REGISTER, "Tạo tài khoản", "Lưu thành tích và tăng Elo qua từng ván.",
                registerUsername, registerPassword, registerButton, backButton,
                registerStatus), REGISTER);
        JPanel formCenter = new JPanel(new GridBagLayout());
        formCenter.setOpaque(false);
        formCenter.add(forms);
        right.add(formCenter, BorderLayout.CENTER);
        add(right, BorderLayout.CENTER);

        loginButton.addActionListener(event -> fireLogin());
        createAccountButton.addActionListener(event -> {
            registerUsername.setText(loginUsername.getText());
            clearStatus(registerStatus);
            showRegisterPage();
        });
        registerButton.addActionListener(event -> fireRegister());
        backButton.addActionListener(event -> {
            loginUsername.setText(registerUsername.getText());
            clearStatus(loginStatus);
            showLoginPage();
        });
        loginPassword.addActionListener(event -> fireLogin());
        registerPassword.addActionListener(event -> fireRegister());
    }

    private JPanel createBrandPanel() {
        JPanel brand = new JPanel(new BorderLayout());
        brand.setPreferredSize(new Dimension(320, 0));
        brand.setBackground(ThemeStyle.GREEN_DARK);
        brand.setBorder(new EmptyBorder(42, 34, 34, 34));

        JLabel mark = new JLabel("KHAI CUỘC");
        mark.setForeground(new Color(238, 221, 180));
        mark.setFont(new Font("SansSerif", Font.BOLD, 13));
        JLabel emblem = new JLabel("\u2658", SwingConstants.CENTER);
        emblem.setForeground(new Color(238, 221, 180));
        emblem.setFont(new Font("Serif", Font.PLAIN, 104));
        JLabel title = new JLabel("<html>Chậm một nhịp.<br>Thấy cả bàn cờ.</html>");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Serif", Font.BOLD, 28));
        JLabel footer = new JLabel("<html>CHESS CLUB &nbsp; / &nbsp; ONLINE<br>Gặp nhau qua từng nước đi.</html>");
        footer.setForeground(new Color(196, 204, 188));
        footer.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        emblem.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(Box.createVerticalGlue());
        center.add(emblem);
        center.add(Box.createVerticalStrut(24));
        center.add(title);
        center.add(Box.createVerticalGlue());
        brand.add(mark, BorderLayout.NORTH);
        brand.add(center, BorderLayout.CENTER);
        brand.add(footer, BorderLayout.SOUTH);
        return brand;
    }

    private JPanel createForm(String key, String titleText, String subtitle,
                              JTextField username, JPasswordField password,
                              JButton primary, JButton secondary, JLabel status) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(5, 0, 5, 0);

        JLabel eyebrow = new JLabel("CỜ VUA ONLINE  /  " + (key.equals(LOGIN) ? "ĐĂNG NHẬP" : "ĐĂNG KÝ"));
        eyebrow.setForeground(ThemeStyle.GOLD);
        eyebrow.setFont(new Font("SansSerif", Font.BOLD, 11));
        addRow(form, gc, 0, eyebrow);

        JLabel title = new JLabel(titleText);
        ThemeStyle.heading(title, 29);
        addRow(form, gc, 1, title);
        JLabel hint = new JLabel(subtitle);
        ThemeStyle.body(hint);
        addRow(form, gc, 2, hint);

        addRow(form, gc, 3, labeledField("TÊN NGƯỜI CHƠI", username));
        addRow(form, gc, 4, labeledField("MẬT KHẨU", password));

        ThemeStyle.button(primary, true);
        ThemeStyle.button(secondary, false);
        addRow(form, gc, 5, primary);
        addRow(form, gc, 6, secondary);
        status.setFont(new Font("SansSerif", Font.PLAIN, 12));
        addRow(form, gc, 7, status);
        return form;
    }

    private JPanel createServerSettings() {
        JPanel corner = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        corner.setOpaque(false);
        JLabel label = new JLabel("SERVER");
        label.setForeground(ThemeStyle.MUTED);
        label.setFont(new Font("SansSerif", Font.BOLD, 10));
        corner.add(label);
        corner.add(compactSetting("ĐỊA CHỈ", serverHost, 185));
        corner.add(compactSetting("CỔNG", serverPort, 82));
        return corner;
    }

    private JPanel compactSetting(String labelText, JTextField field, int width) {
        JPanel setting = new JPanel(new BorderLayout(0, 3));
        setting.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setForeground(ThemeStyle.MUTED);
        label.setFont(new Font("SansSerif", Font.BOLD, 9));
        ThemeStyle.field(field);
        field.setPreferredSize(new Dimension(width, 31));
        setting.add(label, BorderLayout.NORTH);
        setting.add(field, BorderLayout.CENTER);
        return setting;
    }

    private JPanel labeledField(String labelText, JComponent field) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setForeground(ThemeStyle.MUTED);
        label.setFont(new Font("SansSerif", Font.BOLD, 10));
        ThemeStyle.field(field);
        panel.add(label, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private static void addRow(JPanel panel, GridBagConstraints gc, int row, Component component) {
        gc.gridy = row;
        gc.insets = new Insets(row == 0 ? 8 : 10, 0, 8, 0);
        panel.add(component, gc);
    }

    private static JTextField field(String text) {
        JTextField field = new JTextField(text);
        ThemeStyle.field(field);
        return field;
    }

    private static JPasswordField passwordField() {
        JPasswordField field = new JPasswordField();
        ThemeStyle.field(field);
        return field;
    }

    private void fireLogin() {
        if (listener != null) {
            listener.onLogin(serverHost.getText(), serverPort.getText(),
                    loginUsername.getText(), new String(loginPassword.getPassword()));
        }
    }

    private void fireRegister() {
        if (listener != null) {
            listener.onRegister(serverHost.getText(), serverPort.getText(),
                    registerUsername.getText(), new String(registerPassword.getPassword()));
        }
    }

    private void clearStatus(JLabel status) {
        status.setForeground(ThemeStyle.MUTED);
        status.setText(" ");
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setBusy(boolean busy) {
        loginButton.setEnabled(!busy);
        createAccountButton.setEnabled(!busy);
        registerButton.setEnabled(!busy);
        backButton.setEnabled(!busy);
        loginButton.setText(busy ? "Đang kết nối..." : "Vào sảnh");
        registerButton.setText(busy ? "Đang gửi..." : "Tạo tài khoản");
    }

    public void showLoginPage() {
        formsLayout.show(forms, LOGIN);
    }

    public void showRegisterPage() {
        formsLayout.show(forms, REGISTER);
    }

    public void showError(String message) {
        showLoginPage();
        loginStatus.setForeground(new Color(164, 67, 55));
        loginStatus.setText(message);
    }

    public void showRegisterError(String message) {
        showRegisterPage();
        registerStatus.setForeground(new Color(164, 67, 55));
        registerStatus.setText(message);
    }

    public void showSuccess(String message) {
        showLoginPage();
        loginStatus.setForeground(ThemeStyle.GREEN);
        loginStatus.setText(message);
    }
}
