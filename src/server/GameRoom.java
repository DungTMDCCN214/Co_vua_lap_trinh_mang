package server;

import protocol.ChessBoard;
import protocol.Protocol;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Phòng đấu giữa 2 người chơi. Server giữ bàn cờ riêng và kiểm tra mọi nước đi (authoritative),
 * sau đó broadcast nước đi hợp lệ cho cả 2 bên để 2 Client luôn đồng bộ.
 * Khi ván kết thúc: nếu cả hai đều là tài khoản (không phải khách) và ván đủ dài thì tính Elo và lưu vào CSDL.
 * Chế độ cờ chớp (blitz): server giữ đồng hồ của cả hai bên và là nguồn thời gian duy nhất.
 * Client chỉ hiển thị và đồng bộ lại mỗi khi nhận gói CLOCK. Hết giờ thì bên đó thua (TIMEOUT).
 * Các phương thức synchronized vì 2 luồng ClientHandler (và luồng hẹn giờ) cùng truy cập 1 phòng.
 */
public class GameRoom {
    // ----- cấu hình cờ chớp (đổi khi chạy server bằng -D..., mặc định 5 phút mỗi bên, không cộng thêm giờ) -----
    static final long BLITZ_INITIAL_MS = Long.getLong("chess.blitzSeconds", 300L) * 1000;
    static final long BLITZ_INCREMENT_MS = Long.getLong("chess.blitzIncrementSeconds", 0L) * 1000;
    /** Nước đi đầu tiên của mỗi bên không bị tính giờ (bù thời gian mở màn hình), nhưng phải đi trong vòng chừng này. */
    static final long FIRST_MOVE_LIMIT_MS = Long.getLong("chess.firstMoveSeconds", 30L) * 1000;

    /** Luồng hẹn giờ dùng chung cho đồng hồ mọi phòng (daemon, tác vụ rất nhẹ). */
    private static final ScheduledExecutorService CLOCK_TIMER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "clock-timer");
        t.setDaemon(true);
        return t;
    });

    /** Sau một lời đề nghị hòa, người đó phải đợi thêm ít nhất chừng này nửa nước mới được đề nghị lại (chống spam). */
    private static final int DRAW_OFFER_COOLDOWN_PLIES = 6;
    /** Chỉ được xin hòa khi mỗi bên đã đi ít nhất một nước. */
    private static final int MIN_PLIES_TO_OFFER_DRAW = 2;

    private final ClientHandler white, black;
    private final RoomManager manager;
    private final ChessBoard board = new ChessBoard();
    private final List<String> moves = new ArrayList<>();   // "fc,fr-tc,tr" để lưu CSDL / xem lại
    private final long startedAt = System.currentTimeMillis();
    private boolean over = false;

    // ----- đồng hồ cờ chớp -----
    private final boolean blitz;
    private final long incrementMs;
    private long whiteMs, blackMs;          // thời gian còn lại tại thời điểm đồng hồ của bên đó dừng lần gần nhất
    private long turnStartNanos;            // lúc bắt đầu lượt hiện tại (để tính thời gian đã dùng)
    private ScheduledFuture<?> flagTask;    // tác vụ hẹn giờ "rơi cờ" của bên đang tới lượt
    private int clockEpoch = 0;             // đánh số lần hẹn giờ để bỏ qua tác vụ cũ chạy muộn

    // ----- trạng thái xin hòa -----
    private ClientHandler drawOfferedBy = null;   // null = không có lời đề nghị nào đang chờ
    private int whiteLastOfferPly = -1000, blackLastOfferPly = -1000;

    /** Phòng cờ thường (giữ nguyên chữ ký cũ để tương thích). */
    public GameRoom(ClientHandler a, ClientHandler b, RoomManager manager) { this(a, b, manager, false); }

    public GameRoom(ClientHandler a, ClientHandler b, RoomManager manager, boolean blitz) {
        boolean aIsWhite = new Random().nextBoolean(); // gán màu quân ngẫu nhiên
        this.white = aIsWhite ? a : b;
        this.black = aIsWhite ? b : a;
        this.manager = manager;
        this.blitz = blitz;
        this.incrementMs = BLITZ_INCREMENT_MS;
        this.whiteMs = BLITZ_INITIAL_MS;
        this.blackMs = BLITZ_INITIAL_MS;
    }

    /** Elo hiển thị cho đối thủ: số Elo nếu là tài khoản, "-" nếu là khách. */
    private static String eloText(ClientHandler h) {
        return h.isGuest() ? "-" : String.valueOf(h.getElo());
    }

    private ClientHandler opponentOf(ClientHandler h) { return h == white ? black : white; }

    /** Gán phòng cho 2 người chơi và gửi MATCH_FOUND (kèm Elo của đối thủ). */
    public synchronized void start() {
        white.setRoom(this);
        black.setRoom(this);
        white.send(matchFound(black, "WHITE"));
        black.send(matchFound(white, "BLACK"));
        if (blitz) {
            turnStartNanos = System.nanoTime();
            armFlag();   // Trắng phải đi nước đầu trong FIRST_MOVE_LIMIT_MS
            sendClock();
        }
    }

    /** MATCH_FOUND|đối thủ|màu|elo   (cờ chớp thêm |BLITZ|giây mỗi bên|giây cộng thêm mỗi nước). */
    private String matchFound(ClientHandler opponent, String color) {
        if (!blitz) return Protocol.build(Protocol.MATCH_FOUND, opponent.getUsername(), color, eloText(opponent));
        return Protocol.build(Protocol.MATCH_FOUND, opponent.getUsername(), color, eloText(opponent),
                Protocol.BLITZ, String.valueOf(BLITZ_INITIAL_MS / 1000), String.valueOf(incrementMs / 1000));
    }

    /** Nhận nước đi từ 1 người chơi: kiểm tra lượt + luật, hợp lệ thì phát cho cả 2 bên. */
    public synchronized void handleMove(ClientHandler from, int fc, int fr, int tc, int tr) {
        if (over) return;
        ClientHandler turn = board.isWhiteTurn() ? white : black;
        if (from != turn) {
            from.send(Protocol.build(Protocol.INVALID_MOVE, "Chưa đến lượt của bạn"));
            return;
        }
        // Cờ chớp: kiểm tra ngay trong khối khóa xem người đi còn giờ không.
        long elapsedMs = 0;
        if (blitz) {
            elapsedMs = (System.nanoTime() - turnStartNanos) / 1_000_000;
            long remaining = (from == white) ? whiteMs : blackMs;
            if (elapsedMs >= remaining) {
                endGame(from, "TIMEOUT");
                return;
            }
        }
        if (!board.move(fc, fr, tc, tr)) {
            from.send(Protocol.build(Protocol.INVALID_MOVE, "Nước đi không hợp lệ"));
            return;
        }
        moves.add(fc + "," + fr + "-" + tc + "," + tr);
        String msg = Protocol.build(Protocol.MOVE, fc + "," + fr, tc + "," + tr);
        white.send(msg);
        black.send(msg);

        if (blitz) {
            if (from == white) whiteMs = Math.max(0, whiteMs - elapsedMs + incrementMs);
            else blackMs = Math.max(0, blackMs - elapsedMs + incrementMs);
            turnStartNanos = System.nanoTime();
            armFlag();    // hẹn giờ cho bên vừa tới lượt
            sendClock();  // báo thời gian chuẩn của server cho cả hai client
        }

        // Người nhận lời đề nghị hòa mà đi tiếp = ngầm từ chối: tự hủy lời đề nghị và báo cho người đề nghị
        if (drawOfferedBy != null && drawOfferedBy != from) {
            drawOfferedBy.send(Protocol.DRAW_DECLINE);
            drawOfferedBy = null;
        }

        if (!board.hasAnyLegalMove()) { // bên kế tiếp hết nước đi
            if (board.inCheck(board.isWhiteTurn())) {
                endGame(board.isWhiteTurn() ? white : black, "CHECKMATE"); // bên bị chiếu hết thua
            } else {
                over = true;
                finishGame("DRAW", "STALEMATE");
            }
        }
    }

    // ===================== ĐỒNG HỒ CỜ CHỚP =====================

    /**
     * Hẹn giờ "rơi cờ" cho bên đang tới lượt: hết thời gian còn lại của họ (hoặc hết hạn nước đi đầu tiên).
     * Mỗi lần hẹn tăng clockEpoch để một tác vụ cũ chạy muộn không làm hỏng ván.
     */
    private void armFlag() {
        if (flagTask != null) flagTask.cancel(false);
        final int epoch = ++clockEpoch;
        long delay = board.isWhiteTurn() ? whiteMs : blackMs;
        flagTask = CLOCK_TIMER.schedule(() -> onFlag(epoch), Math.max(1, delay), TimeUnit.MILLISECONDS);
    }

    /** Chạy trên luồng hẹn giờ khi bên đang tới lượt hết giờ. Bỏ qua nếu ván đã xong hoặc đã đi nước khác. */
    private synchronized void onFlag(int epoch) {
        if (over || epoch != clockEpoch) return;
        endGame(board.isWhiteTurn() ? white : black, "TIMEOUT");
    }

    /** CLOCK|ms Trắng|ms Đen|W/B (bên đang tới lượt)|1 (đồng hồ cờ chớp đang chạy). */
    private void sendClock() {
        String msg = Protocol.build(Protocol.CLOCK, String.valueOf(whiteMs), String.valueOf(blackMs),
                board.isWhiteTurn() ? "W" : "B", "1");
        white.send(msg);
        black.send(msg);
    }

    // ===================== XIN HÒA =====================

    /** DRAW_OFFER: một bên đề nghị hòa; server chuyển lời đề nghị cho đối thủ. */
    public synchronized void offerDraw(ClientHandler from) {
        if (over) return;
        if (moves.size() < MIN_PLIES_TO_OFFER_DRAW) {
            from.send(Protocol.build(Protocol.DRAW_FAIL, "Chưa thể xin hòa ở đầu ván"));
            return;
        }
        if (drawOfferedBy != null) {
            from.send(Protocol.build(Protocol.DRAW_FAIL, "Đang có một lời đề nghị hòa chờ trả lời"));
            return;
        }
        int lastOffer = (from == white) ? whiteLastOfferPly : blackLastOfferPly;
        if (moves.size() - lastOffer < DRAW_OFFER_COOLDOWN_PLIES) {
            from.send(Protocol.build(Protocol.DRAW_FAIL, "Bạn vừa đề nghị hòa, hãy đi thêm vài nước rồi thử lại"));
            return;
        }
        if (from == white) whiteLastOfferPly = moves.size(); else blackLastOfferPly = moves.size();
        drawOfferedBy = from;
        opponentOf(from).send(Protocol.DRAW_OFFER);
    }

    /** DRAW_ACCEPT: người nhận chấp nhận -> ván hòa. */
    public synchronized void acceptDraw(ClientHandler from) {
        if (over) return;
        if (drawOfferedBy == null || drawOfferedBy == from) {
            from.send(Protocol.build(Protocol.DRAW_FAIL, "Không có lời đề nghị hòa nào để chấp nhận"));
            return;
        }
        over = true;
        drawOfferedBy = null;
        finishGame("DRAW", "AGREEMENT");
    }

    /** DRAW_DECLINE: người nhận từ chối; người đề nghị nhận DRAW_DECLINE. */
    public synchronized void declineDraw(ClientHandler from) {
        if (over) return;
        if (drawOfferedBy == null || drawOfferedBy == from) {
            from.send(Protocol.build(Protocol.DRAW_FAIL, "Không có lời đề nghị hòa nào để từ chối"));
            return;
        }
        drawOfferedBy.send(Protocol.DRAW_DECLINE);
        drawOfferedBy = null;
    }

    // ===================== CHAT / KẾT THÚC =====================

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
        // Giải phóng phòng TRƯỚC khi báo kết quả: client nhận GAME_OVER xong có thể bấm "Tìm trận" ngay lập tức,
        // lúc đó người chơi phải đã ở trạng thái rảnh (nếu đảo thứ tự, lệnh đó có thể bị bỏ qua vì phòng chưa được giải phóng).
        finish();
        white.send(gameOverMessage(resultFor(outcome, true), reason, r == null ? null : r.whiteDelta, r == null ? 0 : r.whiteElo));
        black.send(gameOverMessage(resultFor(outcome, false), reason, r == null ? null : r.blackDelta, r == null ? 0 : r.blackElo));
    }

    /** Ván xếp hạng: cả hai là tài khoản và đủ dài. Lỗi CSDL thì bỏ qua điểm, không làm sập ván. */
    private RankingService.Result recordIfRanked(String outcome, String reason) {
        if (white.isGuest() || black.isGuest() || moves.size() < RankingService.MIN_PLIES_RANKED) return null;
        // hòa do thỏa thuận chỉ tính điểm khi ván đủ dài (chống thỏa thuận hòa ngay để cày điểm)
        if (reason.equals("AGREEMENT") && moves.size() < RankingService.MIN_PLIES_DRAW_AGREED) return null;
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
        if (flagTask != null) flagTask.cancel(false); // ván đã kết thúc: hủy hẹn giờ
        white.setRoom(null);
        black.setRoom(null);
        manager.removeRoom(this);
    }
}
