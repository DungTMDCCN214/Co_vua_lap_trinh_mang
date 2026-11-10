package server;

import java.sql.SQLException;
import java.util.regex.Pattern;

/** Nghiệp vụ đăng ký / đăng nhập: kiểm tra dữ liệu, băm và so khớp mật khẩu, gọi UserDao. */
public class AuthService {
    /** Tên tài khoản: chữ cái (có dấu tiếng Việt), chữ số, gạch dưới; 3-20 ký tự. Không có '|' và ':' nên an toàn với giao thức. */
    private static final Pattern USERNAME = Pattern.compile("^[\\p{L}\\p{N}_]{3,20}$");
    private static final int MIN_PASSWORD = 6, MAX_PASSWORD = 64;
    /** Băm giả để đăng nhập tên không tồn tại cũng tốn thời gian như tên có thật (chống dò tên tài khoản qua thời gian phản hồi). */
    private static final String DUMMY_HASH = PasswordHasher.hash("dummy-password");

    /** Kết quả trả về cho ClientHandler. */
    public static final class Result {
        private final boolean ok;
        private final String message;
        private final User user;

        private Result(boolean ok, String message, User user) {
            this.ok = ok; this.message = message; this.user = user;
        }
        static Result ok(User user) { return new Result(true, "", user); }
        static Result ok(String message) { return new Result(true, message, null); }
        static Result fail(String message) { return new Result(false, message, null); }

        public boolean isOk() { return ok; }
        public String getMessage() { return message; }
        public User getUser() { return user; }
    }

    private final UserDao users;

    public AuthService(UserDao users) { this.users = users; }

    public Result register(String username, String password) {
        if (!USERNAME.matcher(username).matches())
            return Result.fail("Tên đăng nhập 3-20 ký tự, chỉ gồm chữ, số và dấu gạch dưới");
        if (password.length() < MIN_PASSWORD || password.length() > MAX_PASSWORD)
            return Result.fail("Mật khẩu phải từ " + MIN_PASSWORD + " đến " + MAX_PASSWORD + " ký tự");
        try {
            if (users.exists(username)) return Result.fail("Tên đăng nhập đã tồn tại");
            try {
                users.create(username, PasswordHasher.hash(password));
            } catch (SQLException e) {
                // Hai người đăng ký cùng tên cùng lúc: một người thua ở ràng buộc UNIQUE
                if (users.exists(username)) return Result.fail("Tên đăng nhập đã tồn tại");
                throw e;
            }
            return Result.ok(username);
        } catch (SQLException e) {
            System.err.println("[Server] Lỗi CSDL khi đăng ký: " + e.getMessage());
            return Result.fail("Lỗi hệ thống, vui lòng thử lại");
        }
    }

    /** Đăng nhập: sai tên hay sai mật khẩu đều trả cùng một thông báo. */
    public Result login(String username, String password) {
        try {
            User u = users.findByUsername(username);
            boolean ok = PasswordHasher.verify(password, u != null ? u.getPasswordHash() : DUMMY_HASH);
            if (u == null || !ok) return Result.fail("Sai tên đăng nhập hoặc mật khẩu");
            return Result.ok(u);
        } catch (SQLException e) {
            System.err.println("[Server] Lỗi CSDL khi đăng nhập: " + e.getMessage());
            return Result.fail("Lỗi hệ thống, vui lòng thử lại");
        }
    }

    /** Tên này đã có tài khoản chưa? (dùng để chặn người chơi "khách" mạo danh) */
    public boolean isRegistered(String username) throws SQLException {
        return users.exists(username);
    }
}
