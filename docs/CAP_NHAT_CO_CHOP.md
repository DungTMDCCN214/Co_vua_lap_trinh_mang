# CẬP NHẬT SERVER: CỜ CHỚP (ĐỒNG HỒ CỜ)

Gói này dùng cấu trúc `src/...` của dự án `ChessNetwork`. **Không có file mới. Chỉ ghi đè 4 file.**
Thư mục `client/` và `lib/` giữ nguyên. **Client hiện tại vẫn chạy bình thường**: cờ chớp là một chế độ riêng, chỉ bật khi client gửi tham số `BLITZ`.

## 1. Danh sách file

| File (ghi đè) | Thay đổi |
|---|---|
| `src/protocol/Protocol.java` | Thêm hằng `BLITZ`, `CLOCK`; cập nhật chú thích `MATCH_FOUND`, `GAME_OVER` (lý do `TIMEOUT`), `INVITE` |
| `src/server/RoomManager.java` | Hàng đợi cờ chớp riêng; `findMatch(h, blitz)`; `invite(h, tên, blitz)`; lời mời nhớ chế độ |
| `src/server/GameRoom.java` | Đồng hồ hai bên, hẹn giờ hết giờ, gói `CLOCK`, thua do hết giờ; sửa thứ tự giải phóng phòng (xem mục 8) |
| `src/server/ClientHandler.java` | `FIND_MATCH\|BLITZ` và `INVITE\|tên\|BLITZ` |

Bốn file này **bao gồm cả phần Xin hòa và Mời đấu của lần cập nhật trước**. Nếu bạn chưa chép lần cập nhật đó thì phải chép cả `Server.java` và `RankingService.java` của gói trước. Hai file này không đổi ở lần này.

## 2. Luật cờ chớp (mặc định)

- **5 phút mỗi bên, không cộng thêm giờ** (5+0). Đổi được bằng tham số khi chạy server (mục 5).
- Đồng hồ do **server giữ và quyết định**. Client chỉ hiển thị.
- **Nước đi đầu tiên của mỗi bên không bị tính giờ**, để bù thời gian mở màn hình Game, nhưng phải đi trong vòng **30 giây**, nếu không bên đó thua (`TIMEOUT`, không tính điểm vì ván chưa có nước đi).
- Từ nước thứ hai, thời gian bị trừ theo thời gian suy nghĩ thực tế và cộng giờ thưởng (nếu có).
- Hết giờ thì **thua** (`TIMEOUT`), ván xếp hạng như bình thường (cùng một thang Elo với cờ thường).
- Cờ chớp và cờ thường có **hàng đợi ghép trận riêng**, người chơi cờ chớp chỉ gặp người cờ chớp. Mời đấu có thêm tham số chế độ.

## 3. Gói tin

| Hướng | Gói tin | Ý nghĩa |
|---|---|---|
| C → S | `FIND_MATCH` | Tìm trận **cờ thường** (như cũ) |
| C → S | `FIND_MATCH\|BLITZ` | Tìm trận **cờ chớp** |
| C → S | `INVITE\|tên\|BLITZ` | Mời đấu cờ chớp (`INVITE\|tên` vẫn là cờ thường) |
| S → C | `INVITE\|tên\|elo\|BLITZ` | Lời mời cờ chớp gửi cho người được mời (cờ thường: không có `BLITZ`) |
| S → C | `MATCH_FOUND\|đối thủ\|màu\|elo\|BLITZ\|300\|0` | Cờ chớp: thêm 3 trường cuối là chế độ, số giây mỗi bên, số giây cộng thêm mỗi nước. Cờ thường vẫn 3 tham số như cũ |
| S → C | `CLOCK\|msTrắng\|msĐen\|W/B\|1/0` | Gửi ngay sau `MATCH_FOUND` và sau **mỗi** `MOVE`. `W/B` là bên đang tới lượt; `1` = đồng hồ đang chạy, `0` = đang ở nước đầu chưa tính giờ |
| S → C | `GAME_OVER\|WIN/LOSE\|TIMEOUT[\|±điểm\|Elo mới]` | Hết giờ; hai trường cuối chỉ có ở ván xếp hạng |

Ví dụ một ván: `MATCH_FOUND|bob|WHITE|1200|BLITZ|300|0`, `CLOCK|300000|300000|W|0`, sau nước đầu `MOVE|6,7|5,5`, `CLOCK|300000|300000|B|0`, sau nước đầu của Đen `CLOCK|300000|300000|W|1`.

## 4. Cách hoạt động của đồng hồ

- **Server là nguồn thời gian duy nhất.** Mỗi `GameRoom` lưu thời gian còn lại của hai bên và thời điểm bắt đầu lượt (`System.nanoTime`, đồng hồ đơn điệu, không bị chỉnh giờ hệ thống làm nhảy).
- Một **tác vụ hẹn giờ** (`ScheduledExecutorService`) được đặt cho bên đang tới lượt đúng bằng thời gian còn lại của họ. Mỗi nước đi hợp lệ hủy tác vụ cũ và đặt tác vụ mới.
- Client không biết thời gian thật: nó nhận `CLOCK`, rồi **đếm ngược cục bộ** cho bên đang tới lượt (khi cờ `running = 1`) và **đồng bộ lại** mỗi khi nhận `CLOCK` mới. Độ lệch hiển thị tối đa là một lượt mạng.
- Thời gian trên đường truyền được tính vào bên đang đi. Khi chơi qua mạng yếu có thể thấy rõ, đây là hạn chế đã biết.

## 5. Cấu hình khi chạy server (tùy chọn)

| Tham số | Mặc định | Ý nghĩa |
|---|---|---|
| `-Dchess.blitzSeconds=300` | 300 | Số giây mỗi bên |
| `-Dchess.blitzIncrementSeconds=0` | 0 | Giây cộng thêm sau mỗi nước |
| `-Dchess.firstMoveSeconds=30` | 30 | Hạn đi nước đầu tiên của mỗi bên |

Ví dụ chạy thử nhanh (ván 20 giây, cộng 1 giây mỗi nước):

```bash
java --enable-native-access=ALL-UNNAMED -Dchess.blitzSeconds=20 -Dchess.blitzIncrementSeconds=1 -cp "out;src/lib/sqlite-jdbc-3.46.1.3.jar" server.Server
```

## 6. Việc phía Client (cho bạn phụ trách client)

1. **Lobby:** thêm nút "Cờ chớp" bên cạnh "Tìm trận" (gửi `FIND_MATCH|BLITZ`), và lựa chọn chế độ khi mời đấu (`INVITE|tên|BLITZ`).
2. **MATCH_FOUND:** nếu có trường thứ 5 là `BLITZ` thì bật chế độ có đồng hồ (số giây ở trường 6, giây cộng thêm ở trường 7).
3. **Màn hình Game:** hiển thị hai đồng hồ (mm:ss). Nhận `CLOCK` thì đặt lại giá trị hai bên; nếu `running = 1` thì đếm ngược cho bên đang tới lượt (ví dụ `javax.swing.Timer` 100 ms), nếu `0` thì đứng yên.
4. **Kết thúc:** thêm `case "TIMEOUT"` (ví dụ "Hết giờ") trong `GamePanel` và `Theme/GameResultDialog`, cùng với `AGREEMENT` đã nêu ở lần trước.
5. Hiển thị đồng hồ chỉ mang tính tham khảo. Server mới quyết định ai hết giờ, nên đừng tự kết thúc ván phía client.

## 7. Hạn chế đã biết (nên ghi vào báo cáo)

- **Hết giờ là thua**, kể cả khi đối phương không còn đủ quân để chiếu hết. Theo luật thật trường hợp đó là hòa; dự án chưa có luật thiếu quân.
- **Một thang Elo chung** cho cờ thường và cờ chớp, và bảng `games` chưa lưu chế độ. Tách Elo theo chế độ cần thêm cột vào CSDL (hướng phát triển).
- Độ trễ mạng ảnh hưởng độ công bằng của đồng hồ.
- Chỉ có một mức thời gian (5+0). Thêm nhiều mức cần thêm tham số chế độ và hàng đợi cho từng mức.

## 8. Một lỗi đã được sửa trong lần này

Trước đây server gửi `GAME_OVER` **rồi mới** giải phóng phòng. Client nhận kết quả xong bấm "Tìm trận" ngay có thể rơi vào khoảng hở vài micro giây, khi người chơi vẫn còn bị coi là "đang trong ván" và lệnh bị bỏ qua. Bộ kiểm thử cờ chớp đã bắt được lỗi này. Giờ `finishGame` giải phóng phòng **trước**, gửi kết quả **sau**. Lỗi nằm ở cả ván cờ thường, nên bản sửa có lợi cho mọi chế độ.

## 9. Điểm kỹ thuật dùng khi vấn đáp

- **Nguồn thời gian duy nhất ở server** và client chỉ đồng bộ lại: tránh gian lận và lệch đồng hồ giữa hai máy.
- **Kiểm tra hết giờ ngay trong khối `synchronized`** khi nhận nước đi: nước đi đến đúng lúc bộ hẹn giờ chưa kịp chạy vẫn bị tính thua đúng.
- **Số thứ tự hẹn giờ (`clockEpoch`)**: tác vụ hẹn giờ cũ chạy muộn thấy số thứ tự không khớp thì bỏ qua, không kết thúc nhầm ván.
- **Cờ `over`** bảo đảm chỉ kết thúc ván một lần dù có đồng thời nước đi, đầu hàng, rớt mạng và hết giờ.
- **Thứ tự khóa** vẫn là `RoomManager → GameRoom → ClientHandler`; luồng hẹn giờ chỉ lấy khóa của `GameRoom`, không lấy ngược, nên không deadlock.
- Dùng `ScheduledExecutorService` thay vì mỗi phòng một luồng ngủ chờ (`Thread.sleep`): một luồng daemon phục vụ mọi đồng hồ.

## 10. Đã kiểm thử (script kết nối TCP thật)

**24 kiểm tra cờ chớp, tất cả đạt:** cờ thường không đổi (không có `CLOCK`, `MATCH_FOUND` vẫn 3 tham số); hàng đợi cờ chớp và cờ thường tách riêng; đồng hồ khởi đầu, nước đầu không tính giờ, trừ đúng thời gian (còn khoảng 3000 ms sau 1 giây); hết giờ đúng thời điểm (~4 giây) và tính điểm (+16/−16); hết hạn nước đầu (3 giây) thua không tính điểm; đầu hàng và rớt mạng hủy đồng hồ (không có gói thừa sau 5 giây); mời đấu cờ chớp và cờ thường; cộng giờ thưởng (6 giây + 2 giây → còn khoảng 7000 ms); **15 vòng tranh chấp nước đi đúng lúc hết giờ**, hai bên luôn nhận kết quả giống nhau với đúng một `GAME_OVER`. Server không ghi lỗi hay ngoại lệ nào.
**Chạy lại 36 kiểm tra Xin hòa và Mời đấu của lần trước trên bản mới: tất cả đạt.** Đã biên dịch được với `--release 8`.

## 11. Gợi ý commit

```
feat(server): chế độ cờ chớp - hàng đợi riêng, đồng hồ do server giữ, thua khi hết giờ
fix(server): giải phóng phòng trước khi gửi GAME_OVER để client tìm trận lại ngay
docs: cập nhật bảng gói tin cho cờ chớp (BLITZ, CLOCK, TIMEOUT)
```
