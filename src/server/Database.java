package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Quản lý kết nối tới CSDL SQLite (một file, ví dụ chess.db) và tạo bảng ở lần chạy đầu.
 * Mỗi thao tác mở một Connection riêng rồi đóng lại (try-with-resources), nhờ đó nhiều luồng
 * ClientHandler dùng CSDL cùng lúc mà không dùng chung một Connection.
 * Lớp này chỉ dùng API chuẩn java.sql nên KHÔNG cần driver khi biên dịch, chỉ cần khi chạy.
 */
public class Database {
    private final String url;
    private final Properties props = new Properties();

    public Database(String filePath) throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC"); // nạp driver (có trong sqlite-jdbc-*.jar)
        } catch (ClassNotFoundException e) {
            throw new SQLException("Thiếu driver SQLite. Hãy thêm lib/sqlite-jdbc-*.jar vào classpath khi chạy server.", e);
        }
        this.url = "jdbc:sqlite:" + filePath;
        props.setProperty("busy_timeout", "5000");  // nếu file đang bị khóa, chờ tối đa 5 giây thay vì lỗi ngay
        props.setProperty("foreign_keys", "true");  // bật kiểm tra khóa ngoại
        props.setProperty("journal_mode", "WAL");   // cho phép đọc và ghi đồng thời tốt hơn
        initSchema();
    }

    /** Mở một kết nối mới. Người gọi phải đóng (dùng try-with-resources). */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, props);
    }

    private void initSchema() throws SQLException {
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS users ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "username TEXT NOT NULL UNIQUE COLLATE NOCASE,"   // không phân biệt hoa/thường (ASCII)
                    + "password_hash TEXT NOT NULL,"
                    + "elo INTEGER NOT NULL DEFAULT 1200,"
                    + "wins INTEGER NOT NULL DEFAULT 0,"
                    + "losses INTEGER NOT NULL DEFAULT 0,"
                    + "draws INTEGER NOT NULL DEFAULT 0,"
                    + "created_at TEXT NOT NULL DEFAULT (datetime('now')))");
            st.execute("CREATE TABLE IF NOT EXISTS games ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "white_id INTEGER NOT NULL REFERENCES users(id),"
                    + "black_id INTEGER NOT NULL REFERENCES users(id),"
                    + "result TEXT NOT NULL,"                 // WHITE_WIN / BLACK_WIN / DRAW
                    + "reason TEXT NOT NULL,"                 // CHECKMATE / STALEMATE / SURRENDER / DISCONNECT
                    + "white_elo_before INTEGER NOT NULL,"
                    + "black_elo_before INTEGER NOT NULL,"
                    + "white_elo_delta INTEGER NOT NULL,"     // điểm thay đổi của Trắng; Đen thay đổi ngược dấu
                    + "moves TEXT NOT NULL,"                  // "4,6-4,4;4,1-4,3;..." dùng để xem lại ván
                    + "started_at INTEGER NOT NULL,"          // thời điểm (epoch millis)
                    + "ended_at INTEGER NOT NULL)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_users_elo ON users(elo DESC)");
        }
    }
}
