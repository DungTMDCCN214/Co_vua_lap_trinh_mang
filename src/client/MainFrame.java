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
    private boolean waitingForInitialLeaderboard;

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
        lobby.onMatchFound();
        window.showScreen(MainWindow.LOBBY);
    }

    public void showLogin() {
        login.setBusy(false);
        login.showLoginPage();
        window.showScreen(MainWindow.LOGIN);
    }

    public boolean confirmSurrender() {
        return window.confirmSurrender();
    }

    public boolean confirmDrawOffer() {
        return window.confirmDrawOffer();
    }

    public boolean confirmAcceptDraw() {
        return window.confirmAcceptDraw();
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
                if (elo != null) {
                    waitingForInitialLeaderboard = true;
                    send(Protocol.LEADERBOARD);
                } else {
                    showLobby();
                }
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
                boolean isBlitz = parts.length >= 5 && Protocol.BLITZ.equals(parts[4]);
                int blitzSeconds = isBlitz && parts.length >= 6 ? parseIntegerOrDefault(parts[5], 300) : 0;
                int incrementSeconds = isBlitz && parts.length >= 7 ? parseIntegerOrDefault(parts[6], 0) : 0;
                lobby.onMatchFound();
                game.startGame(parts[1], parts[2].equals("WHITE"), opponentElo, isBlitz, blitzSeconds, incrementSeconds);
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
            case Protocol.CLOCK:
                if (parts.length >= 5) {
                    try {
                        long msWhite = Long.parseLong(parts[1]);
                        long msBlack = Long.parseLong(parts[2]);
                        boolean isWhiteTurn = "W".equalsIgnoreCase(parts[3]);
                        boolean running = "1".equals(parts[4]);
                        game.onClock(msWhite, msBlack, isWhiteTurn, running);
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
                game.onDrawFailed(parts.length > 1 ? parts[1] : "Yêu cầu hòa bị từ chối.");
                break;
            case Protocol.ONLINE_LIST:
                lobby.showOnlineList(parts);
                break;
            case Protocol.INVITE:
                if (parts.length >= 3) {
                    String inviter = parts[1];
                    String inviterElo = parts[2];
                    boolean isBlitzInvite = parts.length >= 4 && Protocol.BLITZ.equals(parts[3]);
                    lobby.onInviteReceived(inviter, inviterElo, isBlitzInvite);
                }
                break;
            case Protocol.INVITE_SENT:
                lobby.onInviteSent(parts.length > 1 ? parts[1] : "");
                break;
            case Protocol.INVITE_DECLINED:
                lobby.onInviteDeclined(parts.length > 1 ? parts[1] : "");
                break;
            case Protocol.INVITE_CANCELLED:
                lobby.onInviteCancelled(parts.length > 1 ? parts[1] : "");
                break;
            case Protocol.INVITE_EXPIRED:
                lobby.onInviteExpired(parts.length > 1 ? parts[1] : "");
                break;
            case Protocol.INVITE_FAIL:
                lobby.onInviteFailed(parts.length > 1 ? parts[1] : "Không thể mời đấu.");
                break;
            case Protocol.GAME_OVER:
                handleGameOver(parts);
                break;
            case Protocol.LEADERBOARD:
                lobby.showLeaderboard(parts);
                if (waitingForInitialLeaderboard) {
                    waitingForInitialLeaderboard = false;
                    showLobby();
                }
                break;
            case Protocol.PROFILE:
                handleProfile(parts);
                break;
            case Protocol.ERROR:
                if (waitingForInitialLeaderboard) {
                    waitingForInitialLeaderboard = false;
                    showLobby();
                }
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

    private static int parseIntegerOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    @Override
    public void onDisconnected() {
        client = null;
        lobby.onMatchFound();
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