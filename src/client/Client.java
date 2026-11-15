package client;

import protocol.Protocol;

import javax.swing.SwingUtilities;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Lớp mạng phía Client: giữ Socket, gửi lệnh và chạy 1 luồng nền để nhận gói tin từ Server. */
public class Client {
    /** Callback báo về giao diện (luôn được gọi trên luồng Swing EDT). */
    public interface Listener {
        void onMessage(String[] parts);
        void onDisconnected();
    }

    private final Listener listener;
    private final Socket socket = new Socket();
    private BufferedReader in;
    private PrintWriter out;
    private volatile boolean connected, closed;

    public Client(Listener listener) { this.listener = listener; }

    /** Kết nối tới Server (timeout 5s) và khởi động luồng nhận dữ liệu. */
    public void connect(String host, int port) throws IOException {
        try {
            socket.connect(new InetSocketAddress(host, port), 5000);
            if (closed) throw new IOException("Đã hủy kết nối");
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        } catch (IOException e) {
            close();
            throw e;
        }
        connected = true;
        Thread t = new Thread(this::listen, "client-listener");
        t.setDaemon(true);
        t.start();
    }

    public void loginGuest(String username) { send(Protocol.build(Protocol.LOGIN, username)); }

    public void loginAccount(String username, String password) {
        send(Protocol.build(Protocol.LOGIN, username, password));
    }

    public void registerAccount(String username, String password) {
        send(Protocol.build(Protocol.REGISTER, username, password));
    }

    /** Luồng nền: đọc từng dòng từ Server, chuyển lên EDT; bỏ qua mọi thứ sau khi đã chủ động close(). */
    private void listen() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                String[] parts = Protocol.parse(line);
                SwingUtilities.invokeLater(() -> { if (!closed) listener.onMessage(parts); });
            }
        } catch (IOException | RuntimeException ignored) {
            // Mất kết nối đột ngột -> xử lý ở finally
        } finally {
            connected = false;
            if (!closed) SwingUtilities.invokeLater(() -> { if (!closed) listener.onDisconnected(); });
        }
    }

    public synchronized void send(String msg) {
        if (connected && !closed) out.println(msg);
    }

    /** Đóng kết nối chủ động (không kích hoạt onDisconnected). */
    public void close() {
        closed = true;
        connected = false;
        try { socket.close(); } catch (IOException ignored) {}
    }
}
