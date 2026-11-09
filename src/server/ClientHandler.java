package server;

import protocol.Protocol;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Xử lý 1 Client: đọc từng dòng lệnh từ socket, phân tích và chuyển cho RoomManager / GameRoom. */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Server server;
    private BufferedReader in;
    private PrintWriter out;
    private volatile String username;
    private volatile GameRoom room;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    public String getUsername() { return username; }
    public GameRoom getRoom() { return room; }
    public void setRoom(GameRoom room) { this.room = room; }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            String line;
            // readLine() trả về null khi Client đóng kết nối; ném IOException khi rớt mạng đột ngột
            while ((line = in.readLine()) != null) {
                if (!handle(line)) break;
            }
        } catch (IOException e) {
            System.out.println("[Server] Mất kết nối với " + username + ": " + e.getMessage());
        } finally {
            cleanup(); // luôn dọn dẹp dù thoát bình thường hay do lỗi
        }
    }

    /** Gửi 1 gói tin cho Client này (synchronized vì nhiều luồng có thể cùng gửi). */
    public synchronized void send(String msg) {
        if (out != null) out.println(msg); // PrintWriter không ném IOException, lỗi được nuốt an toàn
    }

    /** Xử lý 1 lệnh. Trả về false nếu cần ngắt kết nối. */
    private boolean handle(String line) {
        String[] p = Protocol.parse(line);
        String cmd = p[0];

        if (username == null) { // chưa đăng nhập: chỉ chấp nhận LOGIN
            if (cmd.equals(Protocol.LOGIN)) login(p.length > 1 ? p[1].trim() : "");
            else if (cmd.equals(Protocol.DISCONNECT)) return false;
            else send(Protocol.build(Protocol.ERROR, "Bạn cần đăng nhập trước"));
            return true;
        }

        switch (cmd) {
            case Protocol.FIND_MATCH:
                server.getRoomManager().findMatch(this);
                break;
            case Protocol.CANCEL_FIND:
                server.getRoomManager().cancel(this);
                break;
            case Protocol.MOVE:
                handleMove(p);
                break;
            case Protocol.CHAT: {
                GameRoom r = room;
                if (r != null) r.chat(this, line.length() > 5 ? line.substring(5) : "");
                break;
            }
            case Protocol.SURRENDER: {
                GameRoom r = room;
                if (r != null) r.endGame(this, "SURRENDER");
                break;
            }
            case Protocol.DISCONNECT:
                return false;
            default:
                send(Protocol.build(Protocol.ERROR, "Lệnh không hợp lệ: " + cmd));
        }
        return true;
    }

    private void login(String name) {
        if (name.isEmpty() || name.length() > 20 || name.contains("|")) {
            send(Protocol.build(Protocol.LOGIN_FAIL, "Tên không hợp lệ (1-20 ký tự, không chứa '|')"));
        } else if (!server.register(name, this)) {
            send(Protocol.build(Protocol.LOGIN_FAIL, "Tên đã có người sử dụng"));
        } else {
            username = name;
            send(Protocol.build(Protocol.LOGIN_OK, name));
            System.out.println("[Server] Đăng nhập: " + name);
        }
    }

    private void handleMove(String[] p) {
        GameRoom r = room;
        if (r == null || p.length < 3) {
            send(Protocol.build(Protocol.INVALID_MOVE, "Bạn chưa ở trong trận"));
            return;
        }
        try {
            String[] f = p[1].split(","), t = p[2].split(",");
            r.handleMove(this, Integer.parseInt(f[0]), Integer.parseInt(f[1]),
                    Integer.parseInt(t[0]), Integer.parseInt(t[1]));
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            send(Protocol.build(Protocol.INVALID_MOVE, "Sai định dạng tọa độ"));
        }
    }

    /** Dọn dẹp khi Client rời đi: gỡ khỏi danh sách online, hàng đợi, và xử thua nếu đang trong trận. */
    private void cleanup() {
        server.unregister(username, this);
        server.getRoomManager().cancel(this);
        GameRoom r = room;
        if (r != null) r.endGame(this, "DISCONNECT"); // người rớt mạng thua, đối thủ thắng
        try { socket.close(); } catch (IOException ignored) {}
    }
}
