package server;

import protocol.Protocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Multi-threaded Server: vòng lặp accept() nhận kết nối, mỗi Client giao cho 1 luồng trong thread pool. */
public class Server {
    private final Map<String, ClientHandler> online = new ConcurrentHashMap<>(); // danh sách Client online
    private final RoomManager roomManager = new RoomManager();
    private final ExecutorService pool = Executors.newCachedThreadPool();

    // ----- tầng dữ liệu / nghiệp vụ (dùng chung cho mọi ClientHandler) -----
    private final UserDao userDao;
    private final AuthService auth;
    private final RankingService ranking;
    private final boolean guestAllowed;

    public Server(Database db, boolean guestAllowed) {
        this.userDao = new UserDao(db);
        this.auth = new AuthService(userDao);
        this.ranking = new RankingService(db, userDao, new GameDao());
        this.guestAllowed = guestAllowed;
    }

    public RoomManager getRoomManager() { return roomManager; }
    public UserDao getUserDao() { return userDao; }
    public AuthService getAuth() { return auth; }
    public RankingService getRanking() { return ranking; }
    public boolean isGuestAllowed() { return guestAllowed; }

    /** Mở ServerSocket và chờ Client. accept() chặn cho đến khi có kết nối mới. */
    public void start(int port) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[Server] Đang lắng nghe tại cổng " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("[Server] Kết nối mới: " + socket.getRemoteSocketAddress());
                pool.execute(new ClientHandler(socket, this)); // 1 luồng / 1 client
            }
        }
    }

    /** Đăng ký tên; trả về false nếu tên đã có người dùng. */
    public boolean register(String name, ClientHandler h) { return online.putIfAbsent(name, h) == null; }

    public void unregister(String name, ClientHandler h) { if (name != null) online.remove(name, h); }

    /**
     * Chạy: java -cp "out;lib/sqlite-jdbc-3.46.1.3.jar" server.Server [port]   (Linux/macOS dùng ':' thay cho ';')
     * Tùy chọn: -Dchess.db=chess.db (file CSDL)   -Dchess.allowGuest=true|false (cho phép đăng nhập không mật khẩu)
     */
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Protocol.DEFAULT_PORT;
        String dbPath = System.getProperty("chess.db", "chess.db");
        boolean guest = Boolean.parseBoolean(System.getProperty("chess.allowGuest", "true"));
        Database db;
        try {
            db = new Database(dbPath);
        } catch (SQLException e) {
            System.err.println("[Server] Không khởi tạo được CSDL: " + e.getMessage());
            return;
        }
        System.out.println("[Server] CSDL: " + dbPath + " | cho phép khách: " + guest);
        new Server(db, guest).start(port);
    }
}
