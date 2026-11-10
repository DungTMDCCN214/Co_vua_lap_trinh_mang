package server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO (Data Access Object) cho bảng users: gom toàn bộ câu lệnh SQL liên quan tới người dùng.
 * Luôn dùng PreparedStatement với tham số '?' để chống SQL injection (tên đăng nhập là dữ liệu từ client gửi lên).
 */
public class UserDao {
    private final Database db;

    public UserDao(Database db) { this.db = db; }

    private static final String COLS = "id, username, password_hash, elo, wins, losses, draws";

    private static User map(ResultSet rs) throws SQLException {
        return new User(rs.getInt("id"), rs.getString("username"), rs.getString("password_hash"),
                rs.getInt("elo"), rs.getInt("wins"), rs.getInt("losses"), rs.getInt("draws"));
    }

    /** Thêm tài khoản mới. Ném SQLException nếu trùng tên (UNIQUE). */
    public void create(String username, String passwordHash) throws SQLException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO users(username, password_hash, elo) VALUES(?, ?, ?)")) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setInt(3, EloCalculator.DEFAULT_ELO);
            ps.executeUpdate();
        }
    }

    public User findByUsername(String username) throws SQLException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLS + " FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public User findById(int id) throws SQLException {
        try (Connection c = db.getConnection()) {
            return findById(c, id);
        }
    }

    /** Bản dùng chung Connection có sẵn, để nằm trong cùng một transaction với các thao tác khác. */
    public User findById(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT " + COLS + " FROM users WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public boolean exists(String username) throws SQLException {
        return findByUsername(username) != null;
    }

    /** Bảng xếp hạng: điểm Elo giảm dần, bằng điểm thì ai thắng nhiều hơn xếp trước. */
    public List<User> topByElo(int limit) throws SQLException {
        List<User> list = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT " + COLS + " FROM users ORDER BY elo DESC, wins DESC, username ASC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** Cộng/trừ điểm và tăng bộ đếm thắng/thua/hòa. Dùng câu UPDATE tương đối nên mỗi lệnh là nguyên tử. */
    public void applyResult(Connection c, int userId, int eloDelta, int win, int loss, int draw) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE users SET elo = elo + ?, wins = wins + ?, losses = losses + ?, draws = draws + ? WHERE id = ?")) {
            ps.setInt(1, eloDelta);
            ps.setInt(2, win);
            ps.setInt(3, loss);
            ps.setInt(4, draw);
            ps.setInt(5, userId);
            ps.executeUpdate();
        }
    }
}
