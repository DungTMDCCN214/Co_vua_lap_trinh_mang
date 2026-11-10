package server;

/** Một dòng của bảng users (dữ liệu tài khoản + thành tích). */
public class User {
    private final int id;
    private final String username;
    private final String passwordHash;
    private final int elo;
    private final int wins, losses, draws;

    public User(int id, String username, String passwordHash, int elo, int wins, int losses, int draws) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.elo = elo;
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public int getElo() { return elo; }
    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public int getDraws() { return draws; }
}
