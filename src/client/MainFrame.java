package client;

import client.Theme.MainWindow;
import client.Theme.DesktopApp;
import protocol.Protocol;

import java.io.IOException;

/** Application coordinator: network events, client actions, and screen state. */
public final class MainFrame implements Client.Listener {
    private final LoginPanel login;
    private final LobbyPanel lobby;
    private final GamePanel game;
    private final MainWindow window;
    private volatile Client client;

    public MainFrame() {
        login = new LoginPanel(this);
        lobby = new LobbyPanel(this);
        game = new GamePanel(this);
        window = new MainWindow(login.view(), lobby.view(), game.view(), this::exit);
    }

    public void connect(String host, int port, String name, String password, boolean accountLogin) {
        login.setBusy(true);
        Client current = new Client(this);
        client = current;
        new Thread(() -> {
            try {
                current.connect(host, port);
                if (accountLogin && password != null && !password.isEmpty()) {
                    current.loginAccount(name, password);
                } else {
                    current.loginGuest(name);
                }
            } catch (IOException exception) {
                DesktopApp.runOnUiThread(() -> {
                    client = null;
                    login.setBusy(false);
                    login.showError("Không kết nối được server: " + exception.getMessage());
                });
            }
        }, "connector").start();
    }

    public void connect(String host, int port, String name) {
        connect(host, port, name, "", false);
    }

    public void register(String host, int port, String name, String password) {
        login.setBusy(true);
        Client current = new Client(this);
        client = current;
        new Thread(() -> {
            try {
                current.connect(host, port);
                current.registerAccount(name, password);
            } catch (IOException exception) {
                DesktopApp.runOnUiThread(() -> {
                    client = null;
                    login.setBusy(false);
                    login.showRegisterError("Không kết nối được server: " + exception.getMessage());
                });
            }
        }, "connector").start();
    }

    public void send(String message) {
        Client current = client;
        if (current != null) current.send(message);
    }

    public void showLobby() {
        lobby.setSearching(false);
        window.showScreen(MainWindow.LOBBY);
    }

    public void showLogin() {
        login.setBusy(false);
        login.showLoginPage();
        window.showScreen(MainWindow.LOGIN);
    }

    public boolean confirmSurrender() {
        return window.confirm("Đầu hàng", "Bạn chắc chắn muốn kết thúc ván và đầu hàng?");
    }

    public void showGameOverMessage(String result, String reason, String message) {
        window.showGameResult(result, reason, message);
    }

    @Override
    public void onMessage(String[] parts) {
        if (parts.length == 0) return;
        switch (parts[0]) {
            case Protocol.LOGIN_OK:
                if (parts.length < 2) return;
                Integer elo = parseOptionalInteger(parts, 2);
                lobby.setUser(parts[1], elo);
                lobby.setAccountMode(elo != null);
                showLobby();
                break;
            case Protocol.LOGIN_FAIL:
                closeClient();
                login.setBusy(false);
                login.showError(parts.length > 1 ? parts[1] : "Đăng nhập thất bại.");
                break;
            case Protocol.REGISTER_OK:
                closeClient();
                login.setBusy(false);
                login.showSuccess("Đăng ký thành công. Bạn có thể đăng nhập ngay.");
                showLogin();
                break;
            case Protocol.REGISTER_FAIL:
                closeClient();
                login.setBusy(false);
                login.showRegisterError(parts.length > 1 ? parts[1] : "Đăng ký thất bại.");
                break;
            case Protocol.WAITING:
                lobby.setSearching(true);
                break;
            case Protocol.MATCH_FOUND:
                if (parts.length < 3) return;
                String opponentElo = parts.length > 3 ? parts[3] : "-";
                game.startGame(parts[1], parts[2].equals("WHITE"), opponentElo);
                window.showScreen(MainWindow.GAME);
                break;
            case Protocol.MOVE:
                if (parts.length >= 3) game.onMove(parts[1], parts[2]);
                break;
            case Protocol.INVALID_MOVE:
                game.onInvalidMove(parts.length > 1 ? parts[1] : "");
                break;
            case Protocol.CHAT:
                if (parts.length >= 2) game.addChat(parts[1], parts.length > 2 ? parts[2] : "");
                break;
            case Protocol.GAME_OVER:
                handleGameOver(parts);
                break;
            case Protocol.LEADERBOARD:
                lobby.showLeaderboard(parts);
                break;
            case Protocol.PROFILE:
                handleProfile(parts);
                break;
            case Protocol.ERROR:
                window.showError("Lỗi", parts.length > 1 ? parts[1] : "Lỗi không xác định.");
                break;
            default:
                break;
        }
    }

    private void handleGameOver(String[] parts) {
        if (parts.length < 2) return;
        String reason = parts.length > 2 ? parts[2] : "";
        if (parts.length >= 5) {
            Integer newElo = parseInteger(parts[4]);
            if (newElo != null) lobby.updateElo(newElo);
            send(Protocol.PROFILE);
            send(Protocol.LEADERBOARD);
            game.onGameOver(parts[1], reason, parts[3], parts[4]);
        } else {
            if (lobby.isAccountMode()) send(Protocol.PROFILE);
            send(Protocol.LEADERBOARD);
            game.onGameOver(parts[1], reason);
        }
    }

    private void handleProfile(String[] parts) {
        if (parts.length < 6) return;
        try {
            lobby.showProfile(parts[1], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]),
                    Integer.parseInt(parts[4]), Integer.parseInt(parts[5]));
        } catch (NumberFormatException exception) {
            lobby.showError("Không đọc được dữ liệu thành tích.");
        }
    }

    private static Integer parseOptionalInteger(String[] parts, int index) {
        return parts.length > index ? parseInteger(parts[index]) : null;
    }

    private static Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    @Override
    public void onDisconnected() {
        client = null;
        window.showError("Lỗi mạng", "Mất kết nối đến server.");
        showLogin();
    }

    private void closeClient() {
        Client current = client;
        if (current != null) current.close();
        client = null;
    }

    private void exit() {
        Client current = client;
        if (current != null) {
            current.send(Protocol.DISCONNECT);
            current.close();
        }
        window.dispose();
        System.exit(0);
    }

    public void showWindow() {
        window.showWindow();
    }

    public static void main(String[] args) {
        DesktopApp.launch(() -> new MainFrame().showWindow());
    }
}
