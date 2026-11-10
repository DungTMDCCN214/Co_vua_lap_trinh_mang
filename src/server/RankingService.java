package server;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Ghi nhận kết quả ván xếp hạng: tính Elo, cập nhật hai tài khoản và lưu ván vào bảng games
 * trong MỘT transaction (hoặc cùng thành công, hoặc cùng hoàn tác).
 */
public class RankingService {
    /** Ván quá ngắn (ví dụ đầu hàng ngay nước đầu) không tính điểm, tránh "cày điểm" giữa hai người quen. */
    public static final int MIN_PLIES_RANKED = 2;

    /** Kết quả cập nhật điểm. */
    public static final class Result {
        public final int whiteDelta, blackDelta, whiteElo, blackElo;
        Result(int whiteDelta, int blackDelta, int whiteElo, int blackElo) {
            this.whiteDelta = whiteDelta; this.blackDelta = blackDelta;
            this.whiteElo = whiteElo; this.blackElo = blackElo;
        }
    }

    private final Database db;
    private final UserDao users;
    private final GameDao games;

    public RankingService(Database db, UserDao users, GameDao games) {
        this.db = db; this.users = users; this.games = games;
    }

    /**
     * @param outcome "WHITE_WIN", "BLACK_WIN" hoặc "DRAW"
     * @param moves   danh sách nước đi dạng "fc,fr-tc,tr"
     * synchronized: tuần tự hóa việc ghi điểm trong server (SQLite chỉ có một người ghi tại một thời điểm).
     */
    public synchronized Result record(int whiteId, int blackId, String outcome, String reason,
                                      List<String> moves, long startedAt, long endedAt) throws SQLException {
        try (Connection c = db.getConnection()) {
            c.setAutoCommit(false); // bắt đầu transaction
            try {
                User w = users.findById(c, whiteId);
                User b = users.findById(c, blackId);
                if (w == null || b == null) throw new SQLException("Không tìm thấy tài khoản người chơi");

                double whiteScore = outcome.equals("WHITE_WIN") ? 1.0 : outcome.equals("DRAW") ? 0.5 : 0.0;
                int dw = EloCalculator.deltaForWhite(w.getElo(), b.getElo(), whiteScore);
                int blackDelta = -dw;

                boolean draw = outcome.equals("DRAW");
                users.applyResult(c, whiteId, dw, outcome.equals("WHITE_WIN") ? 1 : 0, outcome.equals("BLACK_WIN") ? 1 : 0, draw ? 1 : 0);
                users.applyResult(c, blackId, blackDelta, outcome.equals("BLACK_WIN") ? 1 : 0, outcome.equals("WHITE_WIN") ? 1 : 0, draw ? 1 : 0);
                games.insert(c, whiteId, blackId, outcome, reason, w.getElo(), b.getElo(), dw,
                        String.join(";", moves), startedAt, endedAt);
                c.commit();
                return new Result(dw, blackDelta, w.getElo() + dw, b.getElo() + blackDelta);
            } catch (SQLException | RuntimeException e) {
                c.rollback(); // lỗi giữa chừng: trả mọi thứ về như cũ
                throw e;
            }
        }
    }
}
