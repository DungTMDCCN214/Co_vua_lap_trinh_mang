package server;

import protocol.Protocol;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Quản lý hai cách vào ván, mỗi cách có hai chế độ (cờ thường / cờ chớp):
 *   1) Tìm trận ngẫu nhiên: mỗi chế độ có một hàng đợi riêng (Matchmaking Queue), người chơi cờ chớp chỉ ghép với người cờ chớp.
 *   2) Mời đấu: người này mời đích danh người kia (kèm chế độ), đối phương chấp nhận thì tạo phòng thẳng.
 * Mỗi người chơi luôn ở đúng một trạng thái: rảnh / đang tìm trận / đang trong ván / đang có lời mời chờ.
 * Mọi thay đổi trạng thái nằm trong các phương thức synchronized của lớp này (cùng một khóa) nên không thể
 * xảy ra chuyện một người vừa được ghép ngẫu nhiên vừa nhận lời mời, dẫn tới hai phòng cho một người.
 * Thứ tự khóa trong toàn server: RoomManager -> GameRoom -> ClientHandler (không bao giờ lấy ngược lại).
 */
public class RoomManager {
    /** Thời hạn của một lời mời (giây). Đổi khi chạy server bằng -Dchess.inviteTtl=30 (dùng cho kiểm thử). */
    private static final long INVITE_TTL_SECONDS = Long.getLong("chess.inviteTtl", 30L);

    /** Luồng hẹn giờ dùng chung để hủy các lời mời quá hạn (daemon: không giữ cho server khỏi tắt). */
    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "invite-timer");
        t.setDaemon(true);
        return t;
    });

    /** Một lời mời đang chờ. */
    private static final class Invite {
        final ClientHandler inviter, invitee;
        final boolean blitz;
        ScheduledFuture<?> timeout;
        Invite(ClientHandler inviter, ClientHandler invitee, boolean blitz) {
            this.inviter = inviter; this.invitee = invitee; this.blitz = blitz;
        }
    }

    private final Queue<ClientHandler> queue = new LinkedList<>();       // hàng đợi cờ thường
    private final Queue<ClientHandler> blitzQueue = new LinkedList<>();  // hàng đợi cờ chớp
    private final Set<GameRoom> rooms = ConcurrentHashMap.newKeySet();
    /** Mỗi lời mời được ghi dưới cả hai khóa (người mời và người được mời): ai có mặt trong map này là đang "bận". */
    private final Map<ClientHandler, Invite> pending = new HashMap<>();

    // ===================== TÌM TRẬN NGẪU NHIÊN =====================

    /** Tìm trận cờ thường (giữ nguyên chữ ký cũ để tương thích). */
    public void findMatch(ClientHandler h) { findMatch(h, false); }

    /** Có người bấm "Tìm trận": nếu hàng đợi của chế độ đó đã có 1 người thì ghép cặp, ngược lại đưa vào chờ. */
    public synchronized void findMatch(ClientHandler h, boolean blitz) {
        if (h.getRoom() != null || queue.contains(h) || blitzQueue.contains(h)) return;
        dropInvites(h); // đang có lời mời chờ mà bấm tìm trận thì hủy lời mời đó
        Queue<ClientHandler> q = blitz ? blitzQueue : queue;
        ClientHandler opponent = q.poll();
        if (opponent == null) {
            q.add(h);
            h.send(Protocol.WAITING);
        } else {
            startRoom(opponent, h, blitz);
        }
    }

    /** Người chơi hủy tìm trận: gỡ khỏi cả hai hàng đợi (người đó chỉ có thể nằm trong một hàng). */
    public synchronized void cancel(ClientHandler h) {
        queue.remove(h);
        blitzQueue.remove(h);
    }

    /** Người chơi ngắt kết nối: gỡ khỏi hàng đợi và hủy mọi lời mời liên quan. */
    public synchronized void onDisconnect(ClientHandler h) {
        cancel(h);
        dropInvites(h);
    }

    public void removeRoom(GameRoom room) { rooms.remove(room); }

    private void startRoom(ClientHandler a, ClientHandler b, boolean blitz) {
        GameRoom room = new GameRoom(a, b, this, blitz);
        rooms.add(room);
        room.start(); // gán phòng + gửi MATCH_FOUND (+ đồng hồ nếu là cờ chớp)
    }

    // ===================== MỜI ĐẤU =====================

    /** Người chơi rảnh = không trong ván, không trong hàng đợi nào và không có lời mời chờ. */
    private boolean isFree(ClientHandler h) {
        return h.getRoom() == null && !queue.contains(h) && !blitzQueue.contains(h) && !pending.containsKey(h);
    }

    private static String eloText(ClientHandler h) {
        return h.isGuest() ? "-" : String.valueOf(h.getElo());
    }

    /** ONLINE_LIST -> ONLINE_LIST|tên:elo|tên:elo... (chỉ những người đang rảnh, không gồm chính mình). */
    public synchronized void sendOnlineList(ClientHandler me) {
        StringBuilder sb = new StringBuilder(Protocol.ONLINE_LIST);
        for (Map.Entry<String, ClientHandler> e : me.getServer().onlineSnapshot().entrySet()) {
            ClientHandler h = e.getValue();
            if (h != me && isFree(h)) {
                sb.append('|').append(e.getKey()).append(':').append(eloText(h));
            }
        }
        me.send(sb.toString());
    }

    /** Mời cờ thường (giữ nguyên chữ ký cũ để tương thích). */
    public void invite(ClientHandler from, String targetName) { invite(from, targetName, false); }

    /** INVITE|tên[|BLITZ]: gửi lời mời cho một người đang rảnh. */
    public synchronized void invite(ClientHandler from, String targetName, boolean blitz) {
        ClientHandler to = from.getServer().findOnline(targetName);
        if (to == null) { from.send(Protocol.build(Protocol.INVITE_FAIL, "Không tìm thấy người chơi này")); return; }
        if (to == from) { from.send(Protocol.build(Protocol.INVITE_FAIL, "Không thể tự mời chính mình")); return; }
        if (!isFree(from)) {
            from.send(Protocol.build(Protocol.INVITE_FAIL, "Bạn đang bận (đang tìm trận, trong ván hoặc có lời mời chờ)"));
            return;
        }
        if (!isFree(to)) { from.send(Protocol.build(Protocol.INVITE_FAIL, targetName + " đang bận")); return; }

        Invite inv = new Invite(from, to, blitz);
        pending.put(from, inv);
        pending.put(to, inv);
        inv.timeout = TIMER.schedule(() -> expire(inv), INVITE_TTL_SECONDS, TimeUnit.SECONDS);
        from.send(Protocol.build(Protocol.INVITE_SENT, targetName));
        if (blitz) to.send(Protocol.build(Protocol.INVITE, from.getUsername(), eloText(from), Protocol.BLITZ));
        else to.send(Protocol.build(Protocol.INVITE, from.getUsername(), eloText(from)));
    }

    /** INVITE_ACCEPT|tên / INVITE_DECLINE|tên: người được mời trả lời. */
    public synchronized void respondInvite(ClientHandler me, String inviterName, boolean accept) {
        Invite inv = pending.get(me);
        if (inv == null || inv.invitee != me || !inviterName.equals(inv.inviter.getUsername())) {
            me.send(Protocol.build(Protocol.INVITE_FAIL, "Lời mời không còn hiệu lực"));
            return;
        }
        removeInvite(inv);
        if (!accept) {
            inv.inviter.send(Protocol.build(Protocol.INVITE_DECLINED, me.getUsername()));
            return;
        }
        // kiểm tra phòng thủ: cả hai phải chưa ở trong ván nào
        if (inv.inviter.getRoom() != null || me.getRoom() != null) {
            me.send(Protocol.build(Protocol.INVITE_FAIL, "Lời mời không còn hiệu lực"));
            return;
        }
        startRoom(inv.inviter, me, inv.blitz); // vào ván thẳng, không qua hàng đợi, đúng chế độ của lời mời
    }

    /** INVITE_CANCEL: người mời tự hủy lời mời đang chờ. */
    public synchronized void cancelInvite(ClientHandler me) {
        Invite inv = pending.get(me);
        if (inv == null || inv.inviter != me) {
            me.send(Protocol.build(Protocol.INVITE_FAIL, "Bạn không có lời mời nào đang chờ"));
            return;
        }
        removeInvite(inv);
        inv.invitee.send(Protocol.build(Protocol.INVITE_CANCELLED, me.getUsername()));
    }

    /** Hủy lời mời liên quan tới h (khi h tìm trận hoặc rời đi) và báo cho người còn lại. */
    private void dropInvites(ClientHandler h) {
        Invite inv = pending.get(h);
        if (inv == null) return;
        removeInvite(inv);
        if (inv.inviter == h) inv.invitee.send(Protocol.build(Protocol.INVITE_CANCELLED, h.getUsername()));
        else inv.inviter.send(Protocol.build(Protocol.INVITE_DECLINED, h.getUsername()));
    }

    /** Hết hạn: chạy trên luồng hẹn giờ. Nếu lời mời đã được xử lý trước đó thì bỏ qua. */
    private synchronized void expire(Invite inv) {
        if (pending.get(inv.inviter) != inv) return;
        removeInvite(inv);
        inv.inviter.send(Protocol.build(Protocol.INVITE_EXPIRED, inv.invitee.getUsername()));
        inv.invitee.send(Protocol.build(Protocol.INVITE_EXPIRED, inv.inviter.getUsername()));
    }

    private void removeInvite(Invite inv) {
        pending.remove(inv.inviter);
        pending.remove(inv.invitee);
        if (inv.timeout != null) inv.timeout.cancel(false);
    }
}
