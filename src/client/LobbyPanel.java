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

    public void showError(String message) {
        view.showError(message);
    }

    public void setSearching(boolean searching) {
        view.setSearching(searching);
    }
}
