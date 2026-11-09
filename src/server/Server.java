package server;

import protocol.Protocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Multi-threaded Server: vòng lặp accept() nhận kết nối, mỗi Client giao cho 1 luồng trong thread pool. */
public class Server {
    private final Map<String, ClientHandler> online = new ConcurrentHashMap<>(); // danh sách Client online
    private final RoomManager roomManager = new RoomManager();
    private final ExecutorService pool = Executors.newCachedThreadPool();

    public RoomManager getRoomManager() { return roomManager; }

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

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Protocol.DEFAULT_PORT;
        new Server().start(port);
    }
}
