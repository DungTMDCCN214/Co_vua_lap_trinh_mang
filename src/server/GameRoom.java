package server;

import protocol.ChessBoard;
import protocol.Protocol;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Phòng đấu giữa 2 người chơi. Server giữ bàn cờ riêng và kiểm tra mọi nước đi (authoritative),
 * sau đó broadcast nước đi hợp lệ cho cả 2 bên để 2 Client luôn đồng bộ.
 * Khi ván kết thúc: nếu cả hai đều là tài khoản (không phải khách) và ván đủ dài thì tính Elo và lưu vào CSDL.
 * Các phương thức synchronized vì 2 luồng ClientHandler cùng truy cập 1 phòng.
 */
public class GameRoom {
    private final ClientHandler white, black;
    private final RoomManager manager;
    private final ChessBoard board = new ChessBoard();
    private final List<String> moves = new ArrayList<>();   // "fc,fr-tc,tr" để lưu CSDL / xem lại
    private final long startedAt = System.currentTimeMillis();
    private boolean over = false;

    public GameRoom(ClientHandler a, ClientHandler b, RoomManager manager) {
        boolean aIsWhite = new Random().nextBoolean(); // gán màu quân ngẫu nhiên
        this.white = aIsWhite ? a : b;
        this.black = aIsWhite ? b : a;
        this.manager = manager;
    }

    /** Elo hiển thị cho đối thủ: số Elo nếu là tài khoản, "-" nếu là khách. */
    private static String eloText(ClientHandler h) {
        return h.isGuest() ? "-" : String.valueOf(h.getElo());
    }

    /** Gán phòng cho 2 người chơi và gửi MATCH_FOUND (kèm Elo của đối thủ). */
    public synchronized void start() {
        white.setRoom(this);
        black.setRoom(this);
        white.send(Protocol.build(Protocol.MATCH_FOUND, black.getUsername(), "WHITE", eloText(black)));
        black.send(Protocol.build(Protocol.MATCH_FOUND, white.getUsername(), "BLACK", eloText(white)));
    }

    /** Nhận nước đi từ 1 người chơi: kiểm tra lượt + luật, hợp lệ thì phát cho cả 2 bên. */
    public synchronized void handleMove(ClientHandler from, int fc, int fr, int tc, int tr) {
        if (over) return;
        ClientHandler turn = board.isWhiteTurn() ? white : black;
        if (from != turn) {
            from.send(Protocol.build(Protocol.INVALID_MOVE, "Chưa đến lượt của bạn"));
            return;
        }
        if (!board.move(fc, fr, tc, tr)) {
            from.send(Protocol.build(Protocol.INVALID_MOVE, "Nước đi không hợp lệ"));
            return;
        }
        moves.add(fc + "," + fr + "-" + tc + "," + tr);
        String msg = Protocol.build(Protocol.MOVE, fc + "," + fr, tc + "," + tr);
        white.send(msg);
        black.send(msg);

        if (!board.hasAnyLegalMove()) { // bên kế tiếp hết nước đi
            if (board.inCheck(board.isWhiteTurn())) {
                endGame(board.isWhiteTurn() ? white : black, "CHECKMATE"); // bên bị chiếu hết thua
            } else {
                over = true;
                finishGame("DRAW", "STALEMATE");
            }
        }
    }

    /** Chuyển tiếp tin nhắn chat cho cả 2 người trong phòng. */
    public synchronized void chat(ClientHandler from, String message) {
        if (over || message.trim().isEmpty()) return;
        String msg = Protocol.build(Protocol.CHAT, from.getUsername(), message);
        white.send(msg);
        black.send(msg);
    }

    /** Kết thúc trận: 'loser' thua (đầu hàng / rớt mạng / bị chiếu hết), đối thủ thắng. */
    public synchronized void endGame(ClientHandler loser, String reason) {
        if (over) return;
        over = true;
        finishGame(loser == white ? "BLACK_WIN" : "WHITE_WIN", reason);
    }

    /**
     * Chốt kết quả: ghi điểm (nếu là ván xếp hạng), gửi GAME_OVER cho cả hai rồi giải phóng phòng.
     * Phải được gọi khi đã giữ khóa của phòng và đã đặt over = true.
     */
    private void finishGame(String outcome, String reason) {
        RankingService.Result r = recordIfRanked(outcome, reason);
        white.send(gameOverMessage(resultFor(outcome, true), reason, r == null ? null : r.whiteDelta, r == null ? 0 : r.whiteElo));
        black.send(gameOverMessage(resultFor(outcome, false), reason, r == null ? null : r.blackDelta, r == null ? 0 : r.blackElo));
        finish();
    }

    /** Ván xếp hạng: cả hai là tài khoản và có ít nhất MIN_PLIES_RANKED nước đi. Lỗi CSDL thì bỏ qua điểm, không làm sập ván. */
    private RankingService.Result recordIfRanked(String outcome, String reason) {
        if (white.isGuest() || black.isGuest() || moves.size() < RankingService.MIN_PLIES_RANKED) return null;
        try {
            RankingService.Result r = white.getServer().getRanking().record(
                    white.getUserId(), black.getUserId(), outcome, reason, moves, startedAt, System.currentTimeMillis());
            white.setElo(r.whiteElo); // cập nhật Elo trong bộ nhớ cho lần ghép cặp sau
            black.setElo(r.blackElo);
            return r;
        } catch (SQLException e) {
            System.err.println("[Server] Không lưu được kết quả ván đấu: " + e.getMessage());
            return null;
        }
    }

    private static String resultFor(String outcome, boolean forWhite) {
        if (outcome.equals("DRAW")) return "DRAW";
        boolean whiteWon = outcome.equals("WHITE_WIN");
        return (whiteWon == forWhite) ? "WIN" : "LOSE";
    }

    /** GAME_OVER|kết quả|lý do  hoặc  GAME_OVER|kết quả|lý do|điểm thay đổi|Elo mới (ván xếp hạng). */
    private static String gameOverMessage(String result, String reason, Integer delta, int newElo) {
        if (delta == null) return Protocol.build(Protocol.GAME_OVER, result, reason);
        String d = (delta >= 0 ? "+" : "") + delta;
        return Protocol.build(Protocol.GAME_OVER, result, reason, d, String.valueOf(newElo));
    }

    private void finish() {
        white.setRoom(null);
        black.setRoom(null);
        manager.removeRoom(this);
    }
}
