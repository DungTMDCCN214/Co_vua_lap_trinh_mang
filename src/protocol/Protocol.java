package protocol;

/**
 * Định nghĩa giao thức gói tin: mỗi gói là 1 dòng text, các trường cách nhau bằng dấu '|'.
 * Ví dụ: "MOVE|4,6|4,4"  (cột,hàng nguồn | cột,hàng đích; gốc (0,0) là góc trên-trái, hàng 0 = phía quân Đen)
 */
public final class Protocol {
    private Protocol() {}

    public static final String SEP = "|";
    public static final int DEFAULT_PORT = 5555;

    // ===== Client -> Server =====
    public static final String LOGIN = "LOGIN";             // LOGIN|<username>
    public static final String FIND_MATCH = "FIND_MATCH";   // vào hàng đợi tìm trận
    public static final String CANCEL_FIND = "CANCEL_FIND"; // rời hàng đợi
    public static final String MOVE = "MOVE";               // MOVE|fc,fr|tc,tr
    public static final String CHAT = "CHAT";               // CHAT|<message>
    public static final String SURRENDER = "SURRENDER";     // đầu hàng
    public static final String DISCONNECT = "DISCONNECT";   // thoát

    // ===== Server -> Client =====
    public static final String LOGIN_OK = "LOGIN_OK";         // LOGIN_OK|<username>
    public static final String LOGIN_FAIL = "LOGIN_FAIL";     // LOGIN_FAIL|<lý do>
    public static final String WAITING = "WAITING";           // đang chờ đối thủ
    public static final String MATCH_FOUND = "MATCH_FOUND";   // MATCH_FOUND|<opponent>|WHITE/BLACK
    public static final String INVALID_MOVE = "INVALID_MOVE"; // INVALID_MOVE|<lý do>
    public static final String GAME_OVER = "GAME_OVER";       // GAME_OVER|WIN/LOSE/DRAW|CHECKMATE/STALEMATE/SURRENDER/DISCONNECT
    public static final String ERROR = "ERROR";               // ERROR|<nội dung>
    // Server broadcast MOVE|..|.. và CHAT|<sender>|<message> cho cả 2 người trong phòng.

    /** Ghép các trường thành 1 dòng gói tin. */
    public static String build(String... parts) {
        return String.join(SEP, parts);
    }

    /** Tách 1 dòng gói tin thành mảng các trường (giữ cả trường rỗng). */
    public static String[] parse(String line) {
        return line.split("\\|", -1);
    }
}
