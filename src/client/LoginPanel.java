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

    public java.awt.Component view() {
        return view;
    }

    @Override
    public void onLogin(String host, String portText, String username, String password) {
        Integer port = parsePort(portText);
        if (port == null) return;
        String name = username.trim();
        if (name.isEmpty()) {
            view.showError("Vui lòng nhập tên người chơi.");
            return;
        }
        if (password.isEmpty()) {
            frame.connect(host.trim(), port, name);
        } else {
            frame.connect(host.trim(), port, name, password, true);
        }
    }

    @Override
    public void onRegister(String host, String portText, String username, String password) {
        Integer port = parsePort(portText);
        if (port == null) return;
        String name = username.trim();
        if (name.isEmpty()) {
            view.showRegisterError("Vui lòng nhập tên tài khoản.");
            return;
        }
        if (password.length() < 6 || password.length() > 64) {
            view.showRegisterError("Mật khẩu phải có từ 6 đến 64 ký tự.");
            return;
        }
        frame.register(host.trim(), port, name, password);
    }

    private Integer parsePort(String text) {
        try {
            int port = Integer.parseInt(text.trim());
            if (port < 1 || port > 65535) {
                view.showError("Port phải nằm trong khoảng 1 đến 65535.");
                return null;
            }
            return port;
        } catch (NumberFormatException exception) {
            view.showError("Port phải là số.");
            return null;
        }
    }

    public void setBusy(boolean busy) {
        view.setBusy(busy);
    }

    public void showLoginPage() {
        view.showLoginPage();
    }

    public void showError(String message) {
        view.showError(message);
    }

    public void showRegisterError(String message) {
        view.showRegisterError(message);
    }

    public void showSuccess(String message) {
        view.showSuccess(message);
    }
}
