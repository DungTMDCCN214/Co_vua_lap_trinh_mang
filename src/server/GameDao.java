package server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** DAO cho bảng games: lưu kết quả từng ván đấu (kèm danh sách nước đi để sau này xem lại). */
public class GameDao {

    /** Ghi một ván đã kết thúc. Nhận Connection từ ngoài để nằm chung transaction với việc cập nhật Elo. */
    public void insert(Connection c, int whiteId, int blackId, String result, String reason,
                       int whiteEloBefore, int blackEloBefore, int whiteEloDelta,
                       String moves, long startedAt, long endedAt) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO games(white_id, black_id, result, reason, white_elo_before, black_elo_before,"
                        + " white_elo_delta, moves, started_at, ended_at) VALUES(?,?,?,?,?,?,?,?,?,?)")) {
            ps.setInt(1, whiteId);
            ps.setInt(2, blackId);
            ps.setString(3, result);
            ps.setString(4, reason);
            ps.setInt(5, whiteEloBefore);
            ps.setInt(6, blackEloBefore);
            ps.setInt(7, whiteEloDelta);
            ps.setString(8, moves);
            ps.setLong(9, startedAt);
            ps.setLong(10, endedAt);
            ps.executeUpdate();
        }
    }
}
