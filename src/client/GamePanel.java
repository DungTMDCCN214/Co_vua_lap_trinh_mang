package client;

import client.Theme.GameView;
import protocol.Protocol;

/** Match state and protocol handling; the visual screen is implemented under Theme. */
public final class GamePanel implements GameView.Listener {
    private final MainFrame frame;
    private final GameView view = new GameView();
    private String opponent = "";
    private String opponentElo = "-";
    private boolean isBlitz;
    private boolean over;

    public GamePanel(MainFrame frame) {
        this.frame = frame;
        view.setListener(this);
    }

    public java.awt.Component view() {
        return view;
    }

    @Override
    public void onMove(int fromColumn, int fromRow, int toColumn, int toRow) {
        frame.send(Protocol.build(Protocol.MOVE,
                fromColumn + "," + fromRow, toColumn + "," + toRow));
    }

    @Override
    public void onChat(String message) {
        if (!over) frame.send(Protocol.build(Protocol.CHAT, message));
    }

    @Override
    public void onSurrender() {
        if (!over && frame.confirmSurrender()) {
            frame.send(Protocol.SURRENDER);
        }
    }

    @Override
    public void onDrawOffer() {
        if (!over && frame.confirmDrawOffer()) {
            frame.send(Protocol.DRAW_OFFER);
            view.addChat("Hệ thống", "Bạn đã gửi đề nghị hòa tới đối thủ.");
        }
    }

    public void onDrawOfferReceived() {
        if (over) return;
        view.addChat("Hệ thống", "Đối thủ gửi lời đề nghị hòa cờ.");
        boolean accepted = frame.confirmAcceptDraw();
        if (over) return;
        if (accepted) {
            frame.send(Protocol.DRAW_ACCEPT);
        } else {
            frame.send(Protocol.DRAW_DECLINE);
            view.addChat("Hệ thống", "Bạn đã từ chối lời đề nghị hòa.");
        }
    }

    public void onDrawDeclined() {
        view.addChat("Hệ thống", "Đối thủ đã từ chối lời đề nghị hòa (hoặc lời đề nghị đã tự hủy do đối thủ đi nước tiếp theo).");
    }

    public void onDrawFailed(String reason) {
        view.addChat("Hệ thống", "Không thể xin hòa: " + reason);
    }

    public void onClock(long msWhite, long msBlack, boolean isWhiteTurn, boolean running) {
        view.updateClock(msWhite, msBlack, isWhiteTurn, running);
    }

    public void startGame(String opponent, boolean white) {
        startGame(opponent, white, "-");
    }

    public void startGame(String opponent, boolean white, String opponentElo) {
        startGame(opponent, white, opponentElo, false, 0, 0);
    }

    public void startGame(String opponent, boolean white, String opponentElo,
                          boolean isBlitz, int blitzSeconds, int incrementSeconds) {
        this.opponent = opponent;
        this.opponentElo = opponentElo == null || opponentElo.isEmpty() ? "-" : opponentElo;
        this.isBlitz = isBlitz;
        this.over = false;

        view.setupMatch(white, opponent, this.opponentElo, isBlitz, blitzSeconds, incrementSeconds);
        view.startBoard(white);

        String modeText = isBlitz ? " [Cờ chớp " + (blitzSeconds / 60) + "+" + incrementSeconds + "]" : " [Cờ tiêu chuẩn]";
        view.addChat("Hệ thống", "Bạn cầm quân " + (white ? "TRẮNG" : "ĐEN")
                + ", đối thủ: " + opponent + " (Elo: " + this.opponentElo + ")" + modeText);
        updateStatus();
    }

    public void onMove(String from, String to) {
        try {
            String[] source = from.split(",");
            String[] target = to.split(",");
            view.applyMove(Integer.parseInt(source[0]), Integer.parseInt(source[1]),
                    Integer.parseInt(target[0]), Integer.parseInt(target[1]));
            updateStatus();
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
            // Ignore malformed move coordinates from the network.
        }
    }

    public void onInvalidMove(String reason) {
        view.unlockBoard();
        view.setStatus("Nước đi bị từ chối: " + reason);
    }

    public void addChat(String sender, String message) {
        view.addChat(sender, message);
    }

    private void updateStatus() {
        if (over) return;
        String text = view.isMyTurn() ? "Đến lượt bạn" : "Đến lượt " + opponent;
        if (view.isInCheck()) text += "  ·  CHIẾU";
        text += "  ·  Elo đối thủ " + opponentElo;
        if (isBlitz) text += "  ·  Cờ chớp";
        view.setStatus(text);
    }

    public void onGameOver(String result, String reason) {
        onGameOver(result, reason, null, null);
    }

    public void onGameOver(String result, String reason, String deltaText, String newEloText) {
        over = true;
        view.stopClock();
        view.setBoardActive(false);
        view.setSurrenderEnabled(false);

        String message = resultMessage(result, reason);
        String deltaLabel = formatDelta(deltaText);
        if (newEloText != null && !newEloText.isEmpty()) {
            message += "\nElo mới: " + newEloText + deltaLabel;
        } else if (!deltaLabel.isEmpty()) {
            message += deltaLabel;
        }

        frame.showGameOverMessage(result, reason, message);
        frame.showLobby();
    }

    private static String resultMessage(String result, String reason) {
        switch (reason) {
            case "CHECKMATE":
                return result.equals("WIN") ? "Chiếu hết! Bạn thắng." : "Bạn bị chiếu hết.";
            case "STALEMATE":
                return "Hòa cờ — bên tới lượt không còn nước đi hợp lệ.";
            case "SURRENDER":
                return result.equals("WIN") ? "Đối thủ đã đầu hàng. Bạn thắng." : "Bạn đã đầu hàng.";
            case "DISCONNECT":
                return result.equals("WIN") ? "Đối thủ mất kết nối. Bạn thắng." : "Bạn đã mất kết nối.";
            case "TIMEOUT":
                return result.equals("WIN") ? "Đối thủ đã hết thời gian! Bạn thắng." : "Bạn đã hết thời gian! Bạn thua.";
            case "AGREEMENT":
                return "Hai bên đã đồng ý hòa ván đấu.";
            default:
                return "Ván đấu kết thúc: " + result + ("".equals(reason) ? "" : " (" + reason + ")");
        }
    }

    private static String formatDelta(String deltaText) {
        if (deltaText == null || deltaText.isEmpty()) return "";
        try {
            int delta = Integer.parseInt(deltaText);
            return "  (" + (delta >= 0 ? "+" : "") + delta + " Elo)";
        } catch (NumberFormatException ignored) {
            return "";
        }
    }
}