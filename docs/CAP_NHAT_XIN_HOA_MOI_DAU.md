# CẬP NHẬT SERVER: XIN HÒA VÀ MỜI ĐẤU

Gói này dùng đúng cấu trúc `src/...` của dự án `ChessNetwork` hiện tại (đã có thư mục `src/client/Theme`).
**Không có file mới. Chỉ ghi đè 6 file.** Thư mục `client/` và `lib/` giữ nguyên.
Client hiện tại vẫn chạy bình thường: server chỉ gửi các gói tin mới khi client gửi lệnh mới.

## 1. Danh sách file

| File (ghi đè) | Thay đổi |
|---|---|
| `src/protocol/Protocol.java` | Thêm hằng số: `DRAW_OFFER`, `DRAW_ACCEPT`, `DRAW_DECLINE`, `DRAW_FAIL`, `ONLINE_LIST`, `INVITE`, `INVITE_ACCEPT`, `INVITE_DECLINE`, `INVITE_CANCEL`, `INVITE_SENT`, `INVITE_FAIL`, `INVITE_DECLINED`, `INVITE_CANCELLED`, `INVITE_EXPIRED` |
| `src/server/Server.java` | Thêm `findOnline(tên)` và `onlineSnapshot()` để tra người đang online |
| `src/server/RoomManager.java` | **Viết lại lớn:** thêm mời đấu (bảng lời mời chờ, hạn 30 giây, trạng thái người chơi). `findMatch` và `cancel` giữ nguyên hành vi, thêm `onDisconnect` |
| `src/server/GameRoom.java` | Thêm xin hòa (`offerDraw`, `acceptDraw`, `declineDraw`), lời đề nghị tự hủy khi người nhận đi nước tiếp, hòa thỏa thuận chỉ tính điểm khi ván đủ dài |
| `src/server/ClientHandler.java` | Thêm các `case` lệnh mới, `handleDraw`; `cleanup` gọi `onDisconnect`; tên khách không được chứa `:` |
| `src/server/RankingService.java` | Thêm hằng `MIN_PLIES_DRAW_AGREED = 20` |

> Nếu bạn đã tự sửa các file này, hãy so sánh (diff) trước khi ghi đè. Trong dự án bạn gửi, 5 file server khớp với bản gốc của mình.

## 2. Biên dịch và chạy

Ở thư mục `ChessNetwork`:

```bash
rm -rf out && mkdir out
javac -encoding UTF-8 -d out src/protocol/*.java src/server/*.java src/client/*.java src/client/Theme/*.java
java --enable-native-access=ALL-UNNAMED -cp "out;src/lib/sqlite-jdbc-3.46.1.3.jar" server.Server
```

## 3. Gói tin mới

### Xin hòa
| Hướng | Gói tin | Ý nghĩa |
|---|---|---|
| C → S | `DRAW_OFFER` | Đề nghị hòa |
| S → C | `DRAW_OFFER` | Báo cho **đối thủ** có lời đề nghị |
| C → S | `DRAW_ACCEPT` | Chấp nhận. Cả hai nhận `GAME_OVER\|DRAW\|AGREEMENT` |
| C → S | `DRAW_DECLINE` | Từ chối |
| S → C | `DRAW_DECLINE` | Báo cho **người đề nghị**: bị từ chối, hoặc lời đề nghị tự hủy vì đối thủ đã đi nước tiếp |
| S → C | `DRAW_FAIL\|lý do` | Yêu cầu bị từ chối (đầu ván, đang có đề nghị chờ, đề nghị lại quá sớm, không ở trong ván...) |

### Mời đấu
| Hướng | Gói tin | Ý nghĩa |
|---|---|---|
| C → S | `ONLINE_LIST` | Xin danh sách người đang rảnh |
| S → C | `ONLINE_LIST\|tên:elo\|tên:elo...` | Elo là `-` nếu là khách. Danh sách có thể rỗng (chỉ có `ONLINE_LIST`) |
| C → S | `INVITE\|tên` | Mời một người |
| S → C | `INVITE_SENT\|tên` | Xác nhận đã gửi lời mời |
| S → C | `INVITE\|tên người mời\|elo` | Gửi cho **người được mời** |
| C → S | `INVITE_ACCEPT\|tên người mời` | Chấp nhận, hai bên nhận `MATCH_FOUND` như bình thường |
| C → S | `INVITE_DECLINE\|tên người mời` | Từ chối |
| C → S | `INVITE_CANCEL` | Người mời tự hủy |
| S → C | `INVITE_DECLINED\|tên` | Người mời nhận: bị từ chối, hoặc người kia bấm tìm trận hoặc rời đi |
| S → C | `INVITE_CANCELLED\|tên` | Người được mời nhận: người mời đã hủy, bấm tìm trận hoặc rời đi |
| S → C | `INVITE_EXPIRED\|tên` | Quá hạn 30 giây, gửi cả hai bên |
| S → C | `INVITE_FAIL\|lý do` | Mời không được (không tồn tại, bận, tự mời mình, lời mời hết hiệu lực...) |

## 4. Quy tắc nghiệp vụ

**Xin hòa**
- Chỉ xin được khi mỗi bên đã đi ít nhất một nước (từ nửa nước thứ 2 trở đi).
- Mỗi lúc chỉ có một lời đề nghị chờ. Người vừa đề nghị phải đợi thêm 6 nửa nước mới đề nghị lại được.
- Người **nhận** đề nghị mà đi nước tiếp = ngầm từ chối: lời đề nghị tự hủy và người đề nghị nhận `DRAW_DECLINE`.
- Hòa thỏa thuận (`AGREEMENT`) chỉ tính điểm Elo khi ván có từ **20 nửa nước** trở lên (mỗi bên 10 nước). Ván ngắn vẫn kết thúc hòa nhưng không đổi điểm. Đổi bằng `RankingService.MIN_PLIES_DRAW_AGREED`.

**Mời đấu**
- Mỗi người ở đúng một trạng thái: **rảnh / đang tìm trận / đang trong ván / đang có lời mời chờ**. Chỉ mời được người rảnh, và chỉ người rảnh mới mời được.
- Người đang có lời mời chờ mà bấm **Tìm trận** thì lời mời bị hủy rồi vào hàng đợi.
- Chấp nhận lời mời tạo phòng thẳng, **không qua hàng đợi**. Tìm trận ngẫu nhiên vẫn hoạt động song song.
- Ván mời đấu giữa hai tài khoản **vẫn tính Elo** như ván thường (cùng đi qua `GameRoom`). Ván có khách không tính điểm.
- Đổi hạn lời mời khi chạy thử: `-Dchess.inviteTtl=5` (giây).

## 5. Việc phía Client (cho bạn phụ trách client)

1. **Xin hòa:** nút "Xin hòa" trong màn hình Game (gửi `DRAW_OFFER`). Khi nhận `DRAW_OFFER` hiện hộp thoại Chấp nhận/Từ chối (gửi `DRAW_ACCEPT` hoặc `DRAW_DECLINE`). Nhận `DRAW_DECLINE` thì thông báo "đối thủ từ chối" và cho phép đề nghị lại sau. Nhận `DRAW_FAIL|lý do` thì hiện lý do.
2. **Lý do kết thúc mới:** `GamePanel` và `Theme/GameResultDialog` thêm `case "AGREEMENT"` (ví dụ "Hai bên đồng ý hòa").
3. **Mời đấu:** trong Lobby thêm danh sách người rảnh (gửi `ONLINE_LIST` khi mở màn hình và khi bấm "Làm mới"), nút Mời (gửi `INVITE|tên`). Nhận `INVITE_SENT` thì hiện trạng thái "đang chờ trả lời" kèm nút Hủy (gửi `INVITE_CANCEL`).
4. **Nhận lời mời:** nhận `INVITE|tên|elo` thì hiện hộp thoại có đếm ngược 30 giây (Chấp nhận gửi `INVITE_ACCEPT|tên`, Từ chối gửi `INVITE_DECLINE|tên`). Nhận `INVITE_CANCELLED` hoặc `INVITE_EXPIRED` thì đóng hộp thoại.
5. Xử lý `INVITE_DECLINED`, `INVITE_EXPIRED`, `INVITE_FAIL` bằng thông báo ngắn. `MainFrame.onMessage` đã bỏ qua gói tin lạ nên client cũ không bị hỏng.

## 6. Điểm kỹ thuật dùng khi vấn đáp

- **Trạng thái người chơi trong một khóa:** mọi thay đổi (ghép ngẫu nhiên, gửi lời mời, chấp nhận, hủy) nằm trong các phương thức `synchronized` của `RoomManager`, nên một người không thể bị ghép vào hai phòng cùng lúc.
- **Thứ tự khóa:** `RoomManager` → `GameRoom` → `ClientHandler`, không lấy ngược nên không deadlock.
- **Hẹn giờ lời mời:** `ScheduledExecutorService` một luồng daemon; hàm hết hạn kiểm tra lại lời mời còn hiệu lực trong khối khóa nên không hủy nhầm lời mời đã chấp nhận.
- **Hủy ngầm lời đề nghị hòa** khi người nhận đi nước tiếp, kiểm tra trong khối `synchronized` của `GameRoom`.
- **Chống lạm dụng:** giới hạn tần suất xin hòa; hòa thỏa thuận ngắn không tính điểm.
- Định dạng `ONLINE_LIST` dùng `:` và `|` làm dấu phân cách nên tên người chơi bị cấm chứa hai ký tự này.

## 7. Đã kiểm thử (script kết nối TCP thật, 36 kiểm tra, tất cả đạt)

Xin hòa: đầu ván, từ chối, đề nghị lại quá sớm, đề nghị khi đang có đề nghị chờ, tự chấp nhận, tự hủy khi người nhận đi tiếp, chấp nhận ván ngắn (không tính điểm), chấp nhận ván 20 nửa nước (có tính điểm).
Mời đấu: danh sách người rảnh, tự mời, mời người không tồn tại hoặc đang bận, từ chối, chấp nhận (khác màu), chiếu hết sau mời đấu (+16/−16), hủy, chấp nhận lời mời đã hủy, người mời hoặc người được mời bấm tìm trận, quá hạn, rớt mạng ở cả hai phía, tìm trận ngẫu nhiên vẫn chạy.
Tranh chấp: 20 vòng hai người mời cùng một người đồng thời, luôn đúng một lời mời hợp lệ. Đã biên dịch được với `--release 8`.

## 8. Gợi ý commit

```
feat(server): xin hòa (DRAW_OFFER/ACCEPT/DECLINE), hòa thỏa thuận chỉ tính điểm khi ván đủ dài
feat(server): mời đấu trực tiếp, trạng thái người chơi và hạn lời mời trong RoomManager
docs: cập nhật bảng gói tin cho xin hòa và mời đấu
```
