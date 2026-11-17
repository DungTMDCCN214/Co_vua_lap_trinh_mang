package client;

import client.Theme.LoginView;

/** Login and registration input handling; visual components live under Theme. */
public final class LoginPanel implements LoginView.Listener {
    private final MainFrame frame;
    private final LoginView view = new LoginView();

    public LoginPanel(MainFrame frame) {
        this.frame = frame;
        view.setListener(this);
    }

    public java.awt.Component view() { return view; }

    @Override
    public void onLogin(String host, String portText, String username, String password) {
        Integer port = validate(host, portText, username, false);
        if (port == null) return;
        if (password.isEmpty()) {
            frame.connect(host.trim(), port, username.trim());
        } else {
            frame.connect(host.trim(), port, username.trim(), password, true);
        }
    }

    @Override
    public void onRegister(String host, String portText, String username, String password) {
        Integer port = validate(host, portText, username, true);
        if (port == null) return;
        if (password.length() < 6 || password.length() > 64) {
            view.showRegisterError("Mật khẩu phải có từ 6 đến 64 ký tự.");
            return;
        }
        frame.register(host.trim(), port, username.trim(), password);
    }

    /** Kiểm tra host/port/tên; lỗi hiện đúng trang (đăng nhập hoặc đăng ký). Trả về port hợp lệ hoặc null. */
    private Integer validate(String host, String portText, String username, boolean register) {
        String error = null;
        int port = 0;
        if (host.trim().isEmpty()) {
            error = "Vui lòng nhập địa chỉ server.";
        } else {
            try {
                port = Integer.parseInt(portText.trim());
                if (port < 1 || port > 65535) error = "Port phải nằm trong khoảng 1 đến 65535.";
            } catch (NumberFormatException e) {
                error = "Port phải là số.";
            }
        }
        if (error == null && username.trim().isEmpty()) {
            error = register ? "Vui lòng nhập tên tài khoản." : "Vui lòng nhập tên người chơi.";
        }
        if (error == null) return port;
        if (register) view.showRegisterError(error); else view.showError(error);
        return null;
    }

    public void setBusy(boolean busy) { view.setBusy(busy); }
    public void showLoginPage() { view.showLoginPage(); }
    public void showError(String message) { view.showError(message); }
    public void showRegisterError(String message) { view.showRegisterError(message); }
    public void showSuccess(String message) { view.showSuccess(message); }
}
