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
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private volatile boolean connected;

    public Client(Listener listener) { this.listener = listener; }

    /** Kết nối tới Server (timeout 5s), gửi LOGIN và khởi động luồng nhận dữ liệu. */
    public void connect(String host, int port, String username) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 5000);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        connected = true;
        send(Protocol.build(Protocol.LOGIN, username));
        Thread t = new Thread(this::listen, "client-listener");
        t.setDaemon(true);
        t.start();
    }

    /** Luồng nền: đọc từng dòng từ Server, chuyển lên EDT để cập nhật GUI an toàn. */
    private void listen() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                String[] parts = Protocol.parse(line);
                SwingUtilities.invokeLater(() -> listener.onMessage(parts));
            }
        } catch (IOException e) {
            // Mất kết nối đột ngột -> xử lý ở finally, không để ứng dụng crash
        } finally {
            if (connected) { // chỉ báo nếu không phải do mình chủ động đóng
                connected = false;
                SwingUtilities.invokeLater(listener::onDisconnected);
            }
        }
    }

    public synchronized void send(String msg) {
        if (out != null && connected) out.println(msg);
    }

    /** Đóng kết nối chủ động (không kích hoạt onDisconnected). */
    public void close() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}
