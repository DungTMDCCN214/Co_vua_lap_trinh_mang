package client;

import client.Theme.LobbyView;
import protocol.Protocol;

/** Lobby behavior and protocol actions; all Swing construction lives under Theme. */
public final class LobbyPanel implements LobbyView.Listener {
    private final MainFrame frame;
    private final LobbyView view = new LobbyView();
    private String username;
    private Integer elo;
    private boolean accountMode;

    public LobbyPanel(MainFrame frame) {
        this.frame = frame;
        view.setListener(this);
    }

    public java.awt.Component view() {
        return view;
    }

    @Override
    public void onFindMatch() {
        frame.send(Protocol.FIND_MATCH);
        setSearching(true);
    }

    @Override
    public void onFindBlitzMatch() {
        frame.send(Protocol.build(Protocol.FIND_MATCH, Protocol.BLITZ));
        setSearching(true);
    }

    @Override
    public void onCancelMatch() {
        frame.send(Protocol.CANCEL_FIND);
        setSearching(false);
    }

    @Override
    public void onLeaderboardRequested() {
        frame.send(Protocol.LEADERBOARD);
    }

    @Override
    public void onProfileRequested() {
        frame.send(Protocol.PROFILE);
    }

    @Override
    public void onOnlineListRequested() {
        frame.send(Protocol.ONLINE_LIST);
    }

    @Override
    public void onInvite(String targetName, boolean blitz) {
        if (blitz) {
            frame.send(Protocol.build(Protocol.INVITE, targetName, Protocol.BLITZ));
        } else {
            frame.send(Protocol.build(Protocol.INVITE, targetName));
        }
    }

    @Override
    public void onAcceptInvite(String inviterName) {
        frame.send(Protocol.build(Protocol.INVITE_ACCEPT, inviterName));
    }

    @Override
    public void onDeclineInvite(String inviterName) {
        frame.send(Protocol.build(Protocol.INVITE_DECLINE, inviterName));
    }

    @Override
    public void onCancelInvite() {
        frame.send(Protocol.INVITE_CANCEL);
    }

    @Override
    public void onLogout() {
        frame.logout();
    }

    /** Xóa dữ liệu phiên hiện tại (đăng xuất / mất kết nối). */
    public void reset() {
        username = null;
        elo = null;
        accountMode = false;
        view.reset();
    }

    public void setUser(String name, Integer userElo) {
        username = name;
        elo = userElo;
        accountMode = userElo != null;
        view.setUser(username, elo, accountMode);
    }

    public void setUser(String name) {
        setUser(name, null);
    }

    public void setAccountMode(boolean isAccount) {
        accountMode = isAccount;
        view.setUser(username == null ? "" : username, elo, accountMode);
    }

    public boolean isAccountMode() {
        return accountMode;
    }

    public void updateElo(int latestElo) {
        if (!accountMode) return;
        elo = latestElo;
        view.setUser(username, elo, true);
    }

    public void showLeaderboard(String[] parts) {
        view.showLeaderboard(parts);
    }

    public void showProfile(String name, int profileElo, int wins, int losses, int draws) {
        username = name;
        elo = profileElo;
        view.showProfile(name, profileElo, wins, losses, draws);
    }

    public void showOnlineList(String[] parts) {
        view.showOnlineList(parts);
    }

    public void onInviteSent(String targetName) {
        view.showWaitingInviteDialog(targetName);
    }

    public void onInviteReceived(String inviter, String elo, boolean blitz) {
        view.showIncomingInviteDialog(inviter, elo, blitz);
    }

    public void onInviteDeclined(String name) {
        view.onInviteDeclined(name);
    }

    public void onInviteCancelled(String name) {
        view.onInviteCancelled(name);
    }

    public void onInviteExpired(String name) {
        view.onInviteExpired(name);
    }

    public void onInviteFailed(String reason) {
        view.dismissInviteDialogs();
        view.showError("Mời đấu thất bại: " + reason);
    }

    public void onMatchFound() {
        view.dismissInviteDialogs();
        setSearching(false);
    }

    public void showError(String message) {
        view.showError(message);
    }

    public void setSearching(boolean searching) {
        view.setSearching(searching);
    }
}