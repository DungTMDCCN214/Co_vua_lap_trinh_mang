package server;

import protocol.Protocol;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

/** Xử lý 1 Client: đọc từng dòng lệnh từ socket, phân tích và chuyển cho AuthService / RoomManager / GameRoom. */
public class ClientHandler implements Runnable {
    private static final int MAX_FAILED_LOGINS = 5;           // đăng nhập sai quá số lần này thì ngắt kết nối
    private static final int MAX_REGISTERS_PER_CONNECTION = 3; // chống spam tạo tài khoản

    private final Socket socket;
    private final Server server;
    private BufferedReader in;
    private PrintWriter out;
    private volatile String username;
    private volatile GameRoom room;
    private volatile Integer userId;   // null = người chơi "khách" (không lưu điểm)
    private volatile int elo;          // Elo hiện tại (chỉ có ý nghĩa với tài khoản)
    private int failedLogins, registerCount; // chỉ luồng của handler này dùng nên không cần volatile

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    public String getUsername() { return username; }
    public GameRoom getRoom() { return room; }
    public void setRoom(GameRoom room) { this.room = room; }
    public Server getServer() { return server; }
    public Integer getUserId() { return userId; }
    public boolean isGuest() { return userId == null; }
    public int getElo() { return elo; }
    public void setElo(int elo) { this.elo = elo; }

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

        if (username == null) { // chưa đăng nhập: chỉ chấp nhận LOGIN / REGISTER
            switch (cmd) {
                case Protocol.LOGIN:
                    return login(line);
                case Protocol.REGISTER:
                    register(line);
                    return true;
                case Protocol.DISCONNECT:
                    return false;
                default:
                    send(Protocol.build(Protocol.ERROR, "Bạn cần đăng nhập trước"));
                    return true;
            }
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
            case Protocol.LEADERBOARD:
                sendLeaderboard();
                break;
            case Protocol.PROFILE:
                sendProfile();
                break;
            case Protocol.DISCONNECT:
                return false;
            default:
                send(Protocol.build(Protocol.ERROR, "Lệnh không hợp lệ: " + cmd));
        }
        return true;
    }

    // ===================== ĐĂNG KÝ / ĐĂNG NHẬP =====================

    /**
     * LOGIN|tên            -> đăng nhập "khách" (tương thích bản cũ, không lưu điểm)
     * LOGIN|tên|mật khẩu   -> đăng nhập tài khoản
     * Tách tối đa 3 phần để mật khẩu được phép chứa ký tự '|'.
     * Trả về false nếu cần ngắt kết nối (đăng nhập sai quá nhiều lần).
     */
    private boolean login(String line) {
        String[] q = line.split("\\|", 3);
        String name = q.length > 1 ? q[1].trim() : "";
        if (q.length < 3) {
            loginGuest(name);
            return true;
        }
        return loginAccount(name, q[2]);
    }

    private void loginGuest(String name) {
        if (!server.isGuestAllowed()) {
            send(Protocol.build(Protocol.LOGIN_FAIL, "Server yêu cầu đăng nhập bằng tài khoản"));
            return;
        }
        if (name.isEmpty() || name.length() > 20 || name.contains("|")) {
            send(Protocol.build(Protocol.LOGIN_FAIL, "Tên không hợp lệ (1-20 ký tự, không chứa '|')"));
            return;
        }
        try {
            if (server.getAuth().isRegistered(name)) { // chặn khách mạo danh tài khoản có thật
                send(Protocol.build(Protocol.LOGIN_FAIL, "Tên này đã được đăng ký, hãy đăng nhập bằng mật khẩu"));
                return;
            }
        } catch (SQLException e) {
            System.err.println("[Server] Lỗi CSDL: " + e.getMessage());
            send(Protocol.build(Protocol.LOGIN_FAIL, "Lỗi hệ thống, vui lòng thử lại"));
            return;
        }
        if (!server.register(name, this)) {
            send(Protocol.build(Protocol.LOGIN_FAIL, "Tên đã có người sử dụng"));
            return;
        }
        username = name;
        userId = null;
        send(Protocol.build(Protocol.LOGIN_OK, name));
        System.out.println("[Server] Đăng nhập (khách): " + name);
    }

    private boolean loginAccount(String name, String password) {
        AuthService.Result r = server.getAuth().login(name, password);
        if (!r.isOk()) {
            send(Protocol.build(Protocol.LOGIN_FAIL, r.getMessage()));
            if (++failedLogins >= MAX_FAILED_LOGINS) {
                send(Protocol.build(Protocol.ERROR, "Đăng nhập sai quá nhiều lần"));
                return false;
            }
            return true;
        }
        User u = r.getUser();
        // 1 tài khoản chỉ đăng nhập ở 1 nơi tại một thời điểm
        if (!server.register(u.getUsername(), this)) {
            send(Protocol.build(Protocol.LOGIN_FAIL, "Tài khoản đang đăng nhập ở nơi khác"));
            return true;
        }
        username = u.getUsername(); // dùng đúng cách viết hoa/thường đã lưu trong CSDL
        userId = u.getId();
        elo = u.getElo();
        send(Protocol.build(Protocol.LOGIN_OK, username, String.valueOf(elo)));
        System.out.println("[Server] Đăng nhập (tài khoản): " + username + " (Elo " + elo + ")");
        return true;
    }

    /** REGISTER|tên|mật khẩu */
    private void register(String line) {
        if (++registerCount > MAX_REGISTERS_PER_CONNECTION) {
            send(Protocol.build(Protocol.REGISTER_FAIL, "Đăng ký quá nhiều lần, hãy thử lại sau"));
            return;
        }
        String[] q = line.split("\\|", 3);
        if (q.length < 3) {
            send(Protocol.build(Protocol.REGISTER_FAIL, "Thiếu tên đăng nhập hoặc mật khẩu"));
            return;
        }
        AuthService.Result r = server.getAuth().register(q[1].trim(), q[2]);
        if (r.isOk()) send(Protocol.build(Protocol.REGISTER_OK, q[1].trim()));
        else send(Protocol.build(Protocol.REGISTER_FAIL, r.getMessage()));
    }

    // ===================== BẢNG XẾP HẠNG / THÀNH TÍCH =====================

    /** LEADERBOARD -> LEADERBOARD|tên:elo:thắng:thua:hòa|... (top 10) */
    private void sendLeaderboard() {
        try {
            StringBuilder sb = new StringBuilder(Protocol.LEADERBOARD);
            for (User u : server.getUserDao().topByElo(10)) {
                sb.append('|').append(u.getUsername()).append(':').append(u.getElo()).append(':')
                  .append(u.getWins()).append(':').append(u.getLosses()).append(':').append(u.getDraws());
            }
            send(sb.toString());
        } catch (SQLException e) {
            System.err.println("[Server] Lỗi CSDL: " + e.getMessage());
            send(Protocol.build(Protocol.ERROR, "Không lấy được bảng xếp hạng"));
        }
    }

    /** PROFILE -> PROFILE|tên|elo|thắng|thua|hòa (chỉ dành cho tài khoản, không dành cho khách) */
    private void sendProfile() {
        Integer id = userId;
        if (id == null) {
            send(Protocol.build(Protocol.ERROR, "Chỉ tài khoản đã đăng ký mới có thành tích"));
            return;
        }
        try {
            User u = server.getUserDao().findById(id);
            if (u == null) {
                send(Protocol.build(Protocol.ERROR, "Không tìm thấy tài khoản"));
                return;
            }
            send(Protocol.build(Protocol.PROFILE, u.getUsername(), String.valueOf(u.getElo()),
                    String.valueOf(u.getWins()), String.valueOf(u.getLosses()), String.valueOf(u.getDraws())));
        } catch (SQLException e) {
            System.err.println("[Server] Lỗi CSDL: " + e.getMessage());
            send(Protocol.build(Protocol.ERROR, "Không lấy được thành tích"));
        }
    }

    // ===================== NƯỚC ĐI / DỌN DẸP =====================

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
