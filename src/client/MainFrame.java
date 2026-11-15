package client;

import client.Theme.DesktopApp;
import client.Theme.MainWindow;
import protocol.Protocol;

import java.io.IOException;
import java.util.function.Consumer;

/** Application coordinator: network events, client actions, and screen state. */
public final class MainFrame {
    private final LoginPanel login;
    private final LobbyPanel lobby;
    private final GamePanel game;
    private final MainWindow window;
    private volatile Client client;
    private boolean waitingForInitialLeaderboard;

    public MainFrame() {
        login = new LoginPanel(this);
        lobby = new LobbyPanel(this);
        game = new GamePanel(this);
        window = new MainWindow(login.view(), lobby.view(), game.view(), this::exit);
    }

    // ---------------------------------------------------------------- kết nối

    public void connect(String host, int port, String name, String password, boolean accountLogin) {
        start(host, port, c -> {
            if (accountLogin && password != null && !password.isEmpty()) c.loginAccount(name, password);
            else c.loginGuest(name);
        }, login::showError);
    }

    public void connect(String host, int port, String name) {
        connect(host, port, name, "", false);
    }

    public void register(String host, int port, String name, String password) {
        start(host, port, c -> c.registerAccount(name, password), login::showRegisterError);
    }

    /** Tạo Client mới, kết nối ở luồng nền rồi chạy hành động đầu tiên (đăng nhập/đăng ký). */
    private void start(String host, int port, Consumer<Client> firstAction, Consumer<String> onFail) {
        login.setBusy(true);
        Client current = newClient();
        new Thread(() -> {
            try {
                current.connect(host, port);
                firstAction.accept(current);
            } catch (IOException | RuntimeException exception) {
                DesktopApp.runOnUiThread(() -> {
                    if (client != current) return; // kết nối này đã bị thay thế/đóng
                    closeClient();
                    login.setBusy(false);
                    onFail.accept("Không kết nối được server: " + exception.getMessage());
                });
            }
        }, "connector").start();
    }

    /** Client mới gắn với callback chỉ có hiệu lực khi nó vẫn là kết nối hiện tại (bỏ qua tin của kết nối cũ). */
    private Client newClient() {
        closeClient();
        Client[] self = new Client[1];
        self[0] = new Client(new Client.Listener() {
            @Override public void onMessage(String[] parts) { if (client == self[0]) handle(parts); }
            @Override public void onDisconnected() { if (client == self[0]) handleDisconnect(); }
        });
        return client = self[0];
    }

    private void closeClient() {
        Client current = client;
        client = null;
        if (current != null) current.close();
    }

    public void send(String message) {
        Client current = client;
        if (current != null) current.send(message);
    }

    // ---------------------------------------------------------------- màn hình

    public void showLobby() {
        lobby.onMatchFound(); // tắt hộp thoại mời + hủy trạng thái tìm trận
        window.showScreen(MainWindow.LOBBY);
    }

    public void showLogin() {
        login.setBusy(false);
        login.showLoginPage();
        window.showScreen(MainWindow.LOGIN);
    }

    public boolean confirmSurrender() { return window.confirmSurrender(); }
    public boolean confirmDrawOffer() { return window.confirmDrawOffer(); }
    public boolean confirmAcceptDraw() { return window.confirmAcceptDraw(); }

    /** @return true nếu người chơi chọn "Về sảnh" trong hộp thoại kết quả. */
    public boolean showGameOverMessage(String result, String reason, String message) {
        window.closeDialogs(); // đóng hộp thoại xin hòa/đầu hàng còn mở
        return window.showGameResult(result, reason, message);
    }

    public void showWindow() { window.showWindow(); }

    // ---------------------------------------------------------------- xử lý gói tin

    private void handle(String[] p) {
        if (p.length == 0) return;
        switch (p[0]) {
            case Protocol.LOGIN_OK: {
                if (p.length < 2) return;
                Integer elo = p.length > 2 ? toInteger(p[2]) : null;
                lobby.setUser(p[1], elo);
                if (elo == null) {
                    showLobby();
                } else {
                    waitingForInitialLeaderboard = true;
                    send(Protocol.LEADERBOARD);
                    javax.swing.Timer timeout = new javax.swing.Timer(4000, e -> finishInitialLoad());
                    timeout.setRepeats(false); // server không trả bảng xếp hạng thì vẫn vào sảnh
                    timeout.start();
                }
                break;
            }
            case Protocol.LOGIN_FAIL:
                closeClient();
                login.setBusy(false);
                login.showError(arg(p, 1, "Đăng nhập thất bại."));
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
                login.showRegisterError(arg(p, 1, "Đăng ký thất bại."));
                break;
            case Protocol.WAITING:
                lobby.setSearching(true);
                break;
            case Protocol.MATCH_FOUND: {
                if (p.length < 3) return;
                boolean blitz = p.length >= 5 && Protocol.BLITZ.equals(p[4]);
                int seconds = blitz ? toInt(arg(p, 5, "300"), 300) : 0;
                int increment = blitz ? toInt(arg(p, 6, "0"), 0) : 0;
                lobby.onMatchFound();
                window.closeDialogs(); // đóng hộp thoại kết quả ván trước nếu còn mở
                game.startGame(p[1], p[2].equals("WHITE"), arg(p, 3, "-"), blitz, seconds, increment);
                window.showScreen(MainWindow.GAME);
                break;
            }
            case Protocol.MOVE:
                if (p.length >= 3) game.onMove(p[1], p[2]);
                break;
            case Protocol.INVALID_MOVE:
                game.onInvalidMove(arg(p, 1, ""));
                break;
            case Protocol.CHAT:
                if (p.length >= 2) game.addChat(p[1], arg(p, 2, ""));
                break;
            case Protocol.CLOCK:
                if (p.length >= 5) {
                    try {
                        game.onClock(Long.parseLong(p[1]), Long.parseLong(p[2]), "W".equalsIgnoreCase(p[3]), "1".equals(p[4]));
                    } catch (NumberFormatException ignored) {}
                }
                break;
            case Protocol.DRAW_OFFER:
                game.onDrawOfferReceived();
                break;
            case Protocol.DRAW_DECLINE:
                game.onDrawDeclined();
                break;
            case Protocol.DRAW_FAIL:
                game.onDrawFailed(arg(p, 1, "Yêu cầu hòa bị từ chối."));
                break;
            case Protocol.ONLINE_LIST:
                lobby.showOnlineList(p);
                break;
            case Protocol.INVITE:
                if (p.length >= 3) lobby.onInviteReceived(p[1], p[2], p.length >= 4 && Protocol.BLITZ.equals(p[3]));
                break;
            case Protocol.INVITE_SENT:
                lobby.onInviteSent(arg(p, 1, ""));
                break;
            case Protocol.INVITE_DECLINED:
                lobby.onInviteDeclined(arg(p, 1, ""));
                break;
            case Protocol.INVITE_CANCELLED:
                lobby.onInviteCancelled(arg(p, 1, ""));
                break;
            case Protocol.INVITE_EXPIRED:
                lobby.onInviteExpired(arg(p, 1, ""));
                break;
            case Protocol.INVITE_FAIL:
                lobby.onInviteFailed(arg(p, 1, "Không thể mời đấu."));
                break;
            case Protocol.GAME_OVER:
                handleGameOver(p);
                break;
            case Protocol.LEADERBOARD:
                lobby.showLeaderboard(p);
                finishInitialLoad();
                break;
            case Protocol.PROFILE:
                handleProfile(p);
                break;
            case Protocol.ERROR:
                finishInitialLoad();
                window.showError("Lỗi", arg(p, 1, "Lỗi không xác định."));
                break;
            default:
                break;
        }
    }

    /** Vào sảnh sau khi đã (hoặc không thể) tải bảng xếp hạng đầu tiên. */
    private void finishInitialLoad() {
        if (!waitingForInitialLeaderboard) return;
        waitingForInitialLeaderboard = false;
        showLobby();
    }

    private void handleGameOver(String[] p) {
        if (p.length < 2) return;
        String reason = arg(p, 2, "");
        if (p.length >= 5) {
            Integer newElo = toInteger(p[4]);
            if (newElo != null) lobby.updateElo(newElo);
            send(Protocol.PROFILE);
            send(Protocol.LEADERBOARD);
            game.onGameOver(p[1], reason, p[3], p[4]);
        } else {
            if (lobby.isAccountMode()) send(Protocol.PROFILE);
            send(Protocol.LEADERBOARD);
            game.onGameOver(p[1], reason);
        }
    }

    private void handleProfile(String[] p) {
        if (p.length < 6) return;
        try {
            lobby.showProfile(p[1], Integer.parseInt(p[2]), Integer.parseInt(p[3]),
                    Integer.parseInt(p[4]), Integer.parseInt(p[5]));
        } catch (NumberFormatException exception) {
            lobby.showError("Không đọc được dữ liệu thành tích.");
        }
    }

    private void handleDisconnect() {
        client = null;
        waitingForInitialLeaderboard = false;
        game.reset();          // dừng đồng hồ, khóa bàn cờ nếu đang giữa ván
        window.closeDialogs(); // đóng hộp thoại xin hòa/đầu hàng còn mở
        lobby.onMatchFound();
        showLogin();
        window.showError("Lỗi mạng", "Mất kết nối đến server.");
    }

    private static String arg(String[] p, int index, String fallback) {
        return p.length > index ? p[index] : fallback;
    }

    private static Integer toInteger(String value) {
        try { return Integer.valueOf(value); } catch (NumberFormatException e) { return null; }
    }

    private static int toInt(String value, int fallback) {
        Integer v = toInteger(value);
        return v == null ? fallback : v;
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

    public static void main(String[] args) {
        DesktopApp.launch(() -> new MainFrame().showWindow());
    }
}
