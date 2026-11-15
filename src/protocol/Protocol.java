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
    public static final String LOGIN = "LOGIN";             // LOGIN|<username> (khách) hoặc LOGIN|<username>|<password> (tài khoản)
    public static final String FIND_MATCH = "FIND_MATCH";   // vào hàng đợi tìm trận
    public static final String CANCEL_FIND = "CANCEL_FIND"; // rời hàng đợi
    public static final String MOVE = "MOVE";               // MOVE|fc,fr|tc,tr
    public static final String CHAT = "CHAT";               // CHAT|<message>
    public static final String SURRENDER = "SURRENDER";     // đầu hàng
    public static final String DISCONNECT = "DISCONNECT";   // thoát
    public static final String REGISTER = "REGISTER";       // REGISTER|<username>|<password>
    public static final String LEADERBOARD = "LEADERBOARD"; // xin bảng xếp hạng top 10
    public static final String PROFILE = "PROFILE";         // xin thông tin / thành tích của chính mình
    // ----- Xin hòa (hai chiều: client gửi lên server, server chuyển tiếp cho đối thủ) -----
    public static final String DRAW_OFFER = "DRAW_OFFER";     // đề nghị hòa
    public static final String DRAW_ACCEPT = "DRAW_ACCEPT";   // chấp nhận lời đề nghị hòa
    public static final String DRAW_DECLINE = "DRAW_DECLINE"; // từ chối (server cũng gửi khi lời đề nghị tự hủy)
    // ----- Mời đấu -----
    public static final String ONLINE_LIST = "ONLINE_LIST";       // xin danh sách người đang rảnh
    public static final String INVITE = "INVITE";                 // INVITE|<tên người được mời>
    public static final String INVITE_ACCEPT = "INVITE_ACCEPT";   // INVITE_ACCEPT|<tên người mời>
    public static final String INVITE_DECLINE = "INVITE_DECLINE"; // INVITE_DECLINE|<tên người mời>
    public static final String INVITE_CANCEL = "INVITE_CANCEL";   // người mời tự hủy lời mời đang chờ
    // ----- Cờ chớp (có đồng hồ) -----
    /** Tham số chế độ: FIND_MATCH|BLITZ (tìm trận cờ chớp), INVITE|tên|BLITZ (mời đấu cờ chớp). Không có tham số = cờ thường. */
    public static final String BLITZ = "BLITZ";

    // ===== Server -> Client =====
    public static final String LOGIN_OK = "LOGIN_OK";         // LOGIN_OK|<username> (khách) hoặc LOGIN_OK|<username>|<elo>
    public static final String LOGIN_FAIL = "LOGIN_FAIL";     // LOGIN_FAIL|<lý do>
    public static final String WAITING = "WAITING";           // đang chờ đối thủ
    public static final String MATCH_FOUND = "MATCH_FOUND";   // MATCH_FOUND|<opponent>|WHITE/BLACK|<elo đối thủ, "-" nếu là khách>[|BLITZ|<giây mỗi bên>|<giây cộng thêm mỗi nước>]
    public static final String INVALID_MOVE = "INVALID_MOVE"; // INVALID_MOVE|<lý do>
    public static final String GAME_OVER = "GAME_OVER";       // GAME_OVER|WIN/LOSE/DRAW|<lý do>[|<điểm thay đổi>|<Elo mới>] (2 trường cuối chỉ có ở ván xếp hạng); lý do thêm: AGREEMENT (hòa thỏa thuận), TIMEOUT (hết giờ)
    public static final String ERROR = "ERROR";               // ERROR|<nội dung>
    /** CLOCK|<ms còn lại của Trắng>|<ms còn lại của Đen>|W/B (bên đang tới lượt)|1/0 (1 = đồng hồ đang chạy, 0 = chưa tính giờ) */
    public static final String CLOCK = "CLOCK";
    public static final String DRAW_FAIL = "DRAW_FAIL";         // DRAW_FAIL|<lý do>  (yêu cầu hòa bị từ chối)
    // ONLINE_LIST|<tên>:<elo>|...   (người đang rảnh; elo là "-" nếu là khách)
    // INVITE|<tên người mời>|<elo>[|BLITZ]  (gửi cho người được mời; có BLITZ nếu là lời mời cờ chớp)
    public static final String INVITE_SENT = "INVITE_SENT";           // INVITE_SENT|<tên>  (đã gửi lời mời)
    public static final String INVITE_FAIL = "INVITE_FAIL";           // INVITE_FAIL|<lý do>
    public static final String INVITE_DECLINED = "INVITE_DECLINED";   // INVITE_DECLINED|<tên>  (bị từ chối hoặc người kia rời đi)
    public static final String INVITE_CANCELLED = "INVITE_CANCELLED"; // INVITE_CANCELLED|<tên> (người mời đã hủy)
    public static final String INVITE_EXPIRED = "INVITE_EXPIRED";     // INVITE_EXPIRED|<tên>   (quá hạn, gửi cả hai bên)
    public static final String REGISTER_OK = "REGISTER_OK";     // REGISTER_OK|<username>
    public static final String REGISTER_FAIL = "REGISTER_FAIL"; // REGISTER_FAIL|<lý do>
    // LEADERBOARD|<tên>:<elo>:<thắng>:<thua>:<hòa>|...   (trả lời cho LEADERBOARD)
    // PROFILE|<tên>|<elo>|<thắng>|<thua>|<hòa>           (trả lời cho PROFILE)
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
