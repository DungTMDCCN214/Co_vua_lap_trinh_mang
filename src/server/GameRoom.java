package server;

import protocol.ChessBoard;
import protocol.Protocol;

import java.util.Random;

/**
 * Phòng đấu giữa 2 người chơi. Server giữ bàn cờ riêng và kiểm tra mọi nước đi (authoritative),
 * sau đó broadcast nước đi hợp lệ cho cả 2 bên để 2 Client luôn đồng bộ.
 * Các phương thức synchronized vì 2 luồng ClientHandler cùng truy cập 1 phòng.
 */
public class GameRoom {
    private final ClientHandler white, black;
    private final RoomManager manager;
    private final ChessBoard board = new ChessBoard();
    private boolean over = false;

    public GameRoom(ClientHandler a, ClientHandler b, RoomManager manager) {
        boolean aIsWhite = new Random().nextBoolean(); // gán màu quân ngẫu nhiên
        this.white = aIsWhite ? a : b;
        this.black = aIsWhite ? b : a;
        this.manager = manager;
    }

    /** Gán phòng cho 2 người chơi và gửi MATCH_FOUND. */
    public synchronized void start() {
        white.setRoom(this);
        black.setRoom(this);
        white.send(Protocol.build(Protocol.MATCH_FOUND, black.getUsername(), "WHITE"));
        black.send(Protocol.build(Protocol.MATCH_FOUND, white.getUsername(), "BLACK"));
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
        String msg = Protocol.build(Protocol.MOVE, fc + "," + fr, tc + "," + tr);
        white.send(msg);
        black.send(msg);

        if (!board.hasAnyLegalMove()) { // bên kế tiếp hết nước đi
            if (board.inCheck(board.isWhiteTurn())) {
                endGame(board.isWhiteTurn() ? white : black, "CHECKMATE"); // bên bị chiếu hết thua
            } else {
                over = true;
                white.send(Protocol.build(Protocol.GAME_OVER, "DRAW", "STALEMATE"));
                black.send(Protocol.build(Protocol.GAME_OVER, "DRAW", "STALEMATE"));
                finish();
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
        ClientHandler winner = (loser == white) ? black : white;
        loser.send(Protocol.build(Protocol.GAME_OVER, "LOSE", reason));
        winner.send(Protocol.build(Protocol.GAME_OVER, "WIN", reason));
        finish();
    }

    private void finish() {
        white.setRoom(null);
        black.setRoom(null);
        manager.removeRoom(this);
    }
}
