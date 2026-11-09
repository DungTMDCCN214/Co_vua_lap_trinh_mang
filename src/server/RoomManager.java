package server;

import protocol.Protocol;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Quản lý hàng đợi tìm trận (Matchmaking Queue) và các phòng đang chơi. */
public class RoomManager {
    private final Queue<ClientHandler> queue = new LinkedList<>();
    private final Set<GameRoom> rooms = ConcurrentHashMap.newKeySet();

    /** Có người bấm "Tìm trận": nếu hàng đợi đã có 1 người thì ghép cặp, ngược lại đưa vào chờ. */
    public synchronized void findMatch(ClientHandler h) {
        if (h.getRoom() != null || queue.contains(h)) return;
        ClientHandler opponent = queue.poll();
        if (opponent == null) {
            queue.add(h);
            h.send(Protocol.WAITING);
        } else {
            GameRoom room = new GameRoom(opponent, h, this);
            rooms.add(room);
            room.start();
        }
    }

    /** Người chơi hủy tìm trận hoặc ngắt kết nối: gỡ khỏi hàng đợi. */
    public synchronized void cancel(ClientHandler h) { queue.remove(h); }

    public void removeRoom(GameRoom room) { rooms.remove(room); }
}
