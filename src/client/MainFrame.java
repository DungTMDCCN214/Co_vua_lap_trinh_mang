package client;

import protocol.Protocol;

import javax.swing.*;
import java.awt.CardLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

/** Cửa sổ chính, chuyển qua lại 3 màn hình bằng CardLayout và điều phối gói tin nhận từ Server. */
public class MainFrame extends JFrame implements Client.Listener {
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final LoginPanel login = new LoginPanel(this);
    private final LobbyPanel lobby = new LobbyPanel(this);
    private final GamePanel game = new GamePanel(this);
    private volatile Client client;

    public MainFrame() {
        super("Cờ Vua Online");
        root.add(login, "LOGIN");
        root.add(lobby, "LOBBY");
        root.add(game, "GAME");
        add(root);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { exit(); }
        });
        pack();
        setLocationRelativeTo(null);
    }

    /** Kết nối ở luồng riêng để không làm đơ giao diện khi chờ mạng. */
    public void connect(String host, int port, String name) {
        login.setBusy(true);
        Client c = new Client(this);
        client = c;
        new Thread(() -> {
            try {
                c.connect(host, port, name); // đăng nhập thành công/thất bại sẽ báo qua LOGIN_OK / LOGIN_FAIL
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    client = null;
                    login.setBusy(false);
                    login.showError("Không kết nối được server: " + ex.getMessage());
                });
            }
        }, "connector").start();
    }

    public void send(String msg) {
        Client c = client;
        if (c != null) c.send(msg);
    }

    public void showLobby() { lobby.setSearching(false); cards.show(root, "LOBBY"); }
    public void showLogin() { login.setBusy(false); cards.show(root, "LOGIN"); }

    /** Điều phối gói tin từ Server theo loại lệnh. */
    @Override
    public void onMessage(String[] p) {
        switch (p[0]) {
            case Protocol.LOGIN_OK:
                lobby.setUser(p[1]);
                showLobby();
                break;
            case Protocol.LOGIN_FAIL:
                if (client != null) client.close();
                client = null;
                login.setBusy(false);
                login.showError(p.length > 1 ? p[1] : "Đăng nhập thất bại");
                break;
            case Protocol.WAITING:
                lobby.setSearching(true);
                break;
            case Protocol.MATCH_FOUND:
                game.startGame(p[1], p[2].equals("WHITE"));
                cards.show(root, "GAME");
                break;
            case Protocol.MOVE:
                game.onMove(p[1], p[2]);
                break;
            case Protocol.INVALID_MOVE:
                game.onInvalidMove(p.length > 1 ? p[1] : "");
                break;
            case Protocol.CHAT:
                game.addChat(p[1], p.length > 2 ? p[2] : "");
                break;
            case Protocol.GAME_OVER:
                game.onGameOver(p[1], p.length > 2 ? p[2] : "");
                break;
            case Protocol.ERROR:
                JOptionPane.showMessageDialog(this, p.length > 1 ? p[1] : "Lỗi", "Lỗi", JOptionPane.ERROR_MESSAGE);
                break;
            default:
                break;
        }
    }

    @Override
    public void onDisconnected() {
        client = null;
        JOptionPane.showMessageDialog(this, "Mất kết nối đến server!", "Lỗi mạng", JOptionPane.ERROR_MESSAGE);
        showLogin();
    }

    private void exit() {
        Client c = client;
        if (c != null) { c.send(Protocol.DISCONNECT); c.close(); }
        dispose();
        System.exit(0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
