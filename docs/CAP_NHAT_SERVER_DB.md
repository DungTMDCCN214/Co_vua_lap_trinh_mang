# CẬP NHẬT SERVER: TÀI KHOẢN + ĐIỂM ELO + CƠ SỞ DỮ LIỆU (SQLite)

Thư mục `source/` trong gói này có cấu trúc giống hệt `source/` của repo: chép đè vào là xong.
**Client hiện tại không cần sửa**: server vẫn nhận `LOGIN|tên` như cũ (người chơi "khách", không lưu điểm).

## 1. Danh sách file

### File MỚI (thêm vào `source/server/`)
| File | Vai trò |
|------|---------|
| `Database.java` | Mở kết nối SQLite, tạo bảng `users`, `games` ở lần chạy đầu |
| `User.java` | Đối tượng một dòng của bảng `users` |
| `UserDao.java` | Mọi câu SQL liên quan người dùng (thêm, tìm, bảng xếp hạng, cộng điểm) |
| `GameDao.java` | Lưu một ván đã kết thúc vào bảng `games` |
| `PasswordHasher.java` | Băm mật khẩu PBKDF2-HMAC-SHA256 + salt ngẫu nhiên |
| `EloCalculator.java` | Công thức Elo (K = 32, điểm khởi đầu 1200) |
| `AuthService.java` | Nghiệp vụ đăng ký / đăng nhập |
| `RankingService.java` | Tính Elo và lưu ván trong một transaction |

### File MỚI khác
| File | Chép vào |
|------|----------|
| `lib/sqlite-jdbc-3.46.1.3.jar` | `source/lib/` (driver SQLite, giấy phép Apache 2.0) |

### File THAY THẾ (ghi đè bản cũ)
| File | Thay đổi chính |
|------|----------------|
| `common/protocol/Protocol.java` | Thêm hằng `REGISTER`, `REGISTER_OK`, `REGISTER_FAIL`, `LEADERBOARD`, `PROFILE` và chú thích định dạng mới |
| `server/Server.java` | Giữ `UserDao`, `AuthService`, `RankingService`; `main` mở CSDL; đọc tùy chọn `-Dchess.db`, `-Dchess.allowGuest` |
| `server/ClientHandler.java` | Thêm `REGISTER`, `LOGIN` có mật khẩu, `LEADERBOARD`, `PROFILE`; lưu `userId`, `elo`; chặn dò mật khẩu |
| `server/GameRoom.java` | Ghi lại danh sách nước đi; khi hết ván thì tính Elo, lưu CSDL, gửi `GAME_OVER` kèm điểm |
| `.gitignore` (trong `source/`) | Cho phép commit `lib/*.jar`; bỏ qua file `*.db` |

### File GIỮ NGUYÊN
`RoomManager.java`, `ChessBoard.java` và toàn bộ thư mục `client/`.

> Nếu bạn đã tự sửa 4 file thay thế, hãy đối chiếu bằng công cụ so sánh (diff) trước khi ghi đè.

## 2. Biên dịch và chạy

Chạy từ thư mục `source/`.

```bash
# Biên dịch (không cần driver ở bước này)
mkdir out
javac -encoding UTF-8 -d out common/protocol/*.java server/*.java client/*.java

# Chạy server (Windows dùng dấu ';' giữa các đường dẫn)
java -cp "out;lib/sqlite-jdbc-3.46.1.3.jar" server.Server

# Linux / macOS dùng dấu ':'
java -cp "out:lib/sqlite-jdbc-3.46.1.3.jar" server.Server
```

Client vẫn chạy như cũ: `java -cp out client.MainFrame`.

Tùy chọn của server:
- `-Dchess.db=duong/dan/chess.db`: file CSDL (mặc định `chess.db` ở thư mục đang chạy)
- `-Dchess.allowGuest=false`: bắt buộc đăng nhập bằng tài khoản (mặc định `true`)
- Ví dụ: `java -Dchess.allowGuest=false -cp "out;lib/sqlite-jdbc-3.46.1.3.jar" server.Server 5555`

Lần chạy đầu server tự tạo file `chess.db`. Muốn xóa dữ liệu thì tắt server rồi xóa `chess.db*`.

## 3. Giao thức: phần thêm vào (tương thích bản cũ)

### Client → Server
| Lệnh | Định dạng | Ghi chú |
|------|-----------|---------|
| `REGISTER` | `REGISTER\|<tên>\|<mật khẩu>` | Chỉ dùng **trước khi đăng nhập**. Tối đa 3 lần mỗi kết nối |
| `LOGIN` | `LOGIN\|<tên>\|<mật khẩu>` | Đăng nhập tài khoản. Mật khẩu được phép chứa `\|` |
| `LOGIN` | `LOGIN\|<tên>` | Đăng nhập khách (như bản cũ) |
| `LEADERBOARD` | `LEADERBOARD` | Xin top 10 |
| `PROFILE` | `PROFILE` | Xin thành tích của chính mình (chỉ tài khoản) |

### Server → Client
| Lệnh | Định dạng | Ghi chú |
|------|-----------|---------|
| `REGISTER_OK` / `REGISTER_FAIL` | `REGISTER_OK\|<tên>` / `REGISTER_FAIL\|<lý do>` | Đăng ký **không** tự đăng nhập; client gửi `LOGIN` sau đó |
| `LOGIN_OK` | `LOGIN_OK\|<tên>\|<elo>` | Với khách chỉ có `LOGIN_OK\|<tên>` |
| `MATCH_FOUND` | `MATCH_FOUND\|<đối thủ>\|<màu>\|<elo đối thủ>` | Elo là `-` nếu đối thủ là khách |
| `GAME_OVER` | `GAME_OVER\|<kết quả>\|<lý do>\|<±điểm>\|<elo mới>` | Hai trường cuối chỉ có ở ván xếp hạng, ví dụ `GAME_OVER\|WIN\|CHECKMATE\|+16\|1216` |
| `LEADERBOARD` | `LEADERBOARD\|tên:elo:thắng:thua:hòa\|...` | Tối đa 10 mục |
| `PROFILE` | `PROFILE\|tên\|elo\|thắng\|thua\|hòa` | |

Client cũ chỉ đọc các trường đầu nên vẫn chạy bình thường.

## 4. Quy tắc nghiệp vụ

- **Tên đăng ký**: 3–20 ký tự gồm chữ (có dấu), số, gạch dưới. Không phân biệt hoa/thường với chữ ASCII (`An` và `an` là một).
- **Mật khẩu**: 6–64 ký tự; lưu dạng `pbkdf2$120000$<salt>$<hash>`, không lưu mật khẩu thô.
- **Ván xếp hạng**: cả hai là tài khoản và ván có ít nhất 2 nước đi (đổi bằng `RankingService.MIN_PLIES_RANKED`). Ván có khách hoặc quá ngắn không tính điểm.
- **Kết quả tính điểm**: chiếu hết, đầu hàng, rớt mạng (người rớt thua), hòa do hết nước đi.
- **Elo**: `E = 1/(1+10^((Rb-Ra)/400))`, `R' = R + 32·(S−E)`; tổng điểm hai người được bảo toàn.
- **Khách** không được dùng tên đã có tài khoản; mỗi tài khoản chỉ đăng nhập ở một nơi tại một thời điểm.
- **Chống dò mật khẩu**: đăng nhập sai 5 lần thì server ngắt kết nối; sai tên và sai mật khẩu cùng một thông báo.

## 5. Lược đồ CSDL

```sql
users(id, username UNIQUE NOCASE, password_hash, elo DEFAULT 1200, wins, losses, draws, created_at)
games(id, white_id→users, black_id→users, result, reason,
      white_elo_before, black_elo_before, white_elo_delta, moves, started_at, ended_at)
-- moves dạng "5,6-5,5;4,1-4,3;..." dùng để xem lại ván
```

## 6. Điểm kỹ thuật dùng khi vấn đáp

- **DAO**: `UserDao`, `GameDao` gom toàn bộ SQL; tầng nghiệp vụ (`AuthService`, `RankingService`) không viết SQL.
- **PreparedStatement** cho mọi truy vấn có dữ liệu từ client: chống SQL injection.
- **Mỗi thao tác một Connection** (try-with-resources): nhiều luồng `ClientHandler` không dùng chung kết nối.
- **Transaction** trong `RankingService.record`: cập nhật hai tài khoản và ghi ván cùng thành công hoặc cùng hoàn tác (`commit`/`rollback`).
- **Đồng bộ hóa**: `record` là `synchronized` (SQLite chỉ có một người ghi cùng lúc); khóa luôn lấy theo thứ tự `RoomManager → GameRoom → RankingService`, không tạo vòng chờ.
- **Lỗi CSDL không làm sập ván**: `recordIfRanked` bắt `SQLException`, ván kết thúc bình thường nhưng không tính điểm.
- **Băm mật khẩu**: PBKDF2 có salt riêng và số vòng lặp lưu cùng chuỗi; so sánh bằng `MessageDigest.isEqual` (thời gian không đổi); băm giả khi tên không tồn tại để không lộ tên nào có thật qua thời gian phản hồi.
- Mật khẩu vẫn đi qua mạng dạng văn bản thuần: nâng cấp bằng TLS (`SSLSocket`) là hướng phát triển.

## 7. Đã kiểm thử (script kết nối TCP thật)

Đăng ký (trùng tên, sai định dạng, mật khẩu ngắn, giới hạn số lần), đăng nhập (sai mật khẩu, tên không tồn tại, mật khẩu chứa `|`, đăng nhập hai nơi, khách mạo danh), khóa sau 5 lần sai, ván chiếu hết (+16/−16: 1200 → 1216/1184), ván đầu hàng ngay (không tính điểm), khách đấu tài khoản (không tính điểm), bảng xếp hạng, thành tích, **khởi động lại server mà điểm vẫn còn**. Đã biên dịch được với `--release 8`.

## 8. Việc phía Client (để người phụ trách client làm tiếp)

1. `LoginPanel`: thêm ô mật khẩu, nút Đăng ký (gửi `REGISTER`, nhận `REGISTER_OK` rồi gửi `LOGIN|tên|mật khẩu`).
2. `MainFrame`: xử lý `LOGIN_OK` có trường elo, `REGISTER_OK/FAIL`, `LEADERBOARD`, `PROFILE`.
3. `GamePanel`: hiển thị Elo đối thủ (từ `MATCH_FOUND`) và điểm thay đổi (từ `GAME_OVER`).
4. `LobbyPanel`: bảng xếp hạng và thành tích cá nhân.

## 9. Gợi ý commit

```
feat(server): thêm tầng CSDL SQLite (Database, UserDao, GameDao)
feat(server): đăng ký, đăng nhập có băm mật khẩu PBKDF2 (AuthService)
feat(server): tính Elo và lưu ván trong transaction (RankingService, EloCalculator)
feat(protocol): thêm REGISTER, LEADERBOARD, PROFILE; GAME_OVER kèm điểm
docs: cập nhật README server và mục công nghệ (sqlite-jdbc)
```
