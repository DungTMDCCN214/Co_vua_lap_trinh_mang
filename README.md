# Cờ Vua Online - Bài tập lớn Lập trình mạng

## Biên dịch
    mkdir out
    javac -encoding UTF-8 -d out src/protocol/*.java src/server/*.java src/client/*.java

## Chạy
    java -cp "out;src\lib\sqlite-jdbc-3.46.1.3.jar" server.Server 5555      # Server (cổng mặc định 5555)
    java -cp out client.MainFrame        # Client (chạy 2 lần để có 2 người chơi)

## Giao thức (dòng text, phân tách bằng '|')
Client -> Server: LOGIN|name, FIND_MATCH, CANCEL_FIND, MOVE|fc,fr|tc,tr, CHAT|msg, SURRENDER, DISCONNECT
Server -> Client: LOGIN_OK|name, LOGIN_FAIL|lydo, WAITING, MATCH_FOUND|opp|WHITE/BLACK,
                  MOVE|fc,fr|tc,tr (gửi cả 2 bên), INVALID_MOVE|lydo, CHAT|sender|msg, GAME_OVER|WIN/LOSE/DRAW|lydo
Tọa độ: cột 0-7 (a-h), hàng 0-7 (hàng 0 = phía quân Đen).
