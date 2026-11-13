package client.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Lobby presentation: match controls, game-like ranking, and player profile card. */
public final class LobbyView extends JPanel {
    public interface Listener {
        void onFindMatch();
        void onCancelMatch();
        void onLeaderboardRequested();
        void onProfileRequested();
    }

    private static final Color[] RANK_COLORS = {
            new Color(184, 143, 69), new Color(125, 139, 143), new Color(157, 111, 76)
    };

    private final JLabel welcome = new JLabel(" ");
    private final JButton findButton = new JButton("Tìm trận");
    private final JButton leaderboardButton = new JButton("Bảng xếp hạng");
    private final JButton profileButton = new JButton("Thành tích");

    private final JPanel dataPanel = new JPanel(new BorderLayout(8, 8));
    private final JLabel dataTitle = new JLabel(" ");
    private final JButton closeButton = new JButton("Đóng");
    private final JPanel searchPanel = new JPanel(new BorderLayout(8, 0));
    private final JTextField searchField = new JTextField();
    private final JButton searchButton = new JButton("Tìm");
    private final JPanel leaderboardRows = new JPanel();
    private final JScrollPane leaderboardScroll = new JScrollPane(leaderboardRows);
    private final List<RankEntry> leaderboard = new ArrayList<>();
    private Listener listener;
    private String profileName = "";
    private int profileElo;
    private int profileWins;
    private int profileLosses;
    private int profileDraws;

    public LobbyView() {
        super(new BorderLayout(18, 18));
        setBackground(ThemeStyle.PAPER);
        setBorder(new EmptyBorder(26, 34, 30, 34));

        JPanel top = new JPanel(new BorderLayout(0, 20));
        top.setOpaque(false);
        JLabel kicker = new JLabel("PHÒNG CHỜ  /  CHESS CLUB");
        kicker.setForeground(ThemeStyle.GOLD);
        kicker.setFont(new Font("SansSerif", Font.BOLD, 11));
        ThemeStyle.heading(welcome, 26);
        top.add(kicker, BorderLayout.NORTH);
        top.add(welcome, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1, 3, 10, 0));
        actions.setOpaque(false);
        ThemeStyle.button(findButton, true);
        ThemeStyle.button(leaderboardButton, false);
        ThemeStyle.button(profileButton, false);
        actions.add(findButton);
        actions.add(leaderboardButton);
        actions.add(profileButton);
        top.add(actions, BorderLayout.SOUTH);

        leaderboardRows.setLayout(new BoxLayout(leaderboardRows, BoxLayout.Y_AXIS));
        leaderboardRows.setBackground(ThemeStyle.PANEL);
        leaderboardScroll.setBorder(BorderFactory.createEmptyBorder());
        leaderboardScroll.getViewport().setBackground(ThemeStyle.PANEL);

        ThemeStyle.field(searchField);
        searchField.setPreferredSize(new Dimension(180, 36));
        ThemeStyle.button(searchButton, true);
        ThemeStyle.button(closeButton, false);
        dataTitle.setForeground(ThemeStyle.INK);
        dataTitle.setFont(new Font("SansSerif", Font.BOLD, 17));

        JPanel dataHeader = new JPanel(new BorderLayout());
        dataHeader.setOpaque(false);
        dataHeader.add(dataTitle, BorderLayout.WEST);
        dataHeader.add(closeButton, BorderLayout.EAST);

        searchPanel.setOpaque(false);
        JLabel searchLabel = new JLabel("Tìm người chơi");
        searchLabel.setForeground(ThemeStyle.MUTED);
        searchPanel.add(searchLabel, BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        JPanel dataTop = new JPanel(new BorderLayout(0, 12));
        dataTop.setOpaque(false);
        dataTop.add(dataHeader, BorderLayout.NORTH);
        dataTop.add(searchPanel, BorderLayout.SOUTH);
        dataPanel.setBackground(ThemeStyle.PANEL);
        dataPanel.setBorder(ThemeStyle.cardBorder(16, 18, 16, 18));
        dataPanel.add(dataTop, BorderLayout.NORTH);
        dataPanel.add(leaderboardScroll, BorderLayout.CENTER);
        dataPanel.setVisible(false);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(dataPanel, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        findButton.addActionListener(event -> {
            if (listener == null) return;
            if (findButton.getText().startsWith("Hủy")) listener.onCancelMatch();
            else listener.onFindMatch();
        });
        leaderboardButton.addActionListener(event -> {
            showLeaderboardPanel();
            if (listener != null) listener.onLeaderboardRequested();
        });
        profileButton.addActionListener(event -> {
            showProfilePanel();
            if (listener != null) listener.onProfileRequested();
        });
        closeButton.addActionListener(event -> dataPanel.setVisible(false));
        searchButton.addActionListener(event -> renderLeaderboard());
        searchField.addActionListener(event -> renderLeaderboard());
    }

    private void showLeaderboardPanel() {
        dataTitle.setText("Bảng xếp hạng");
        searchPanel.setVisible(true);
        replaceContent(leaderboardScroll);
        dataPanel.setVisible(true);
        renderLeaderboard();
        revalidate();
        repaint();
    }

    private void showProfilePanel() {
        dataTitle.setText("Hồ sơ người chơi");
        searchPanel.setVisible(false);
        replaceContent(createProfileCard());
        dataPanel.setVisible(true);
        revalidate();
        repaint();
    }

    private void replaceContent(Component component) {
        Component center = ((BorderLayout) dataPanel.getLayout())
                .getLayoutComponent(BorderLayout.CENTER);
        if (center != null) dataPanel.remove(center);
        dataPanel.add(component, BorderLayout.CENTER);
    }

    private void renderLeaderboard() {
        leaderboardRows.removeAll();
        String query = searchField.getText().trim();
        int visibleRows = 0;
        for (RankEntry entry : leaderboard) {
            if (!query.isEmpty() && !Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE)
                    .matcher(entry.name).find()) {
                continue;
            }
            leaderboardRows.add(createRankRow(entry));
            leaderboardRows.add(Box.createVerticalStrut(7));
            visibleRows++;
        }
        if (visibleRows == 0) {
            JLabel empty = new JLabel(query.isEmpty()
                    ? "Chưa có người chơi trong bảng xếp hạng."
                    : "Không tìm thấy người chơi phù hợp.");
            empty.setForeground(ThemeStyle.MUTED);
            empty.setBorder(new EmptyBorder(20, 8, 20, 8));
            leaderboardRows.add(empty);
        }
        leaderboardRows.revalidate();
        leaderboardRows.repaint();
    }

    private JPanel createRankRow(RankEntry entry) {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setBackground(Color.WHITE);
        row.setBorder(ThemeStyle.cardBorder(10, 12, 10, 14));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));

        Color rankColor = entry.rank <= RANK_COLORS.length
                ? RANK_COLORS[entry.rank - 1] : ThemeStyle.MUTED;
        JLabel rank = new JLabel(String.format("%02d", entry.rank), SwingConstants.CENTER);
        rank.setOpaque(true);
        rank.setBackground(new Color(rankColor.getRed(), rankColor.getGreen(), rankColor.getBlue(), 28));
        rank.setForeground(rankColor);
        rank.setFont(new Font("SansSerif", Font.BOLD, 17));
        rank.setPreferredSize(new Dimension(48, 42));

        String initials = entry.name.isEmpty() ? "?" :
                entry.name.substring(0, 1).toUpperCase();
        JLabel avatar = new JLabel(initials, SwingConstants.CENTER);
        avatar.setOpaque(true);
        avatar.setBackground(new Color(231, 237, 226));
        avatar.setForeground(ThemeStyle.GREEN);
        avatar.setFont(new Font("SansSerif", Font.BOLD, 16));
        avatar.setPreferredSize(new Dimension(42, 42));

        JPanel identity = new JPanel(new BorderLayout(10, 0));
        identity.setOpaque(false);
        identity.add(avatar, BorderLayout.WEST);
        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(entry.name);
        name.setForeground(ThemeStyle.INK);
        name.setFont(new Font("SansSerif", Font.BOLD, 14));
        JLabel record = new JLabel(entry.wins + " thắng   ·   " + entry.draws
                + " hòa   ·   " + entry.losses + " thua");
        record.setForeground(ThemeStyle.MUTED);
        record.setFont(new Font("SansSerif", Font.PLAIN, 11));
        labels.add(name);
        labels.add(Box.createVerticalStrut(4));
        labels.add(record);
        identity.add(labels, BorderLayout.CENTER);

        JPanel rating = new JPanel();
        rating.setOpaque(false);
        rating.setLayout(new BoxLayout(rating, BoxLayout.Y_AXIS));
        JLabel eloLabel = new JLabel("ELO");
        eloLabel.setForeground(ThemeStyle.MUTED);
        eloLabel.setFont(new Font("SansSerif", Font.BOLD, 9));
        JLabel elo = new JLabel(String.valueOf(entry.elo));
        elo.setForeground(ThemeStyle.GREEN_DARK);
        elo.setFont(new Font("SansSerif", Font.BOLD, 18));
        rating.add(eloLabel);
        rating.add(elo);

        row.add(rank, BorderLayout.WEST);
        row.add(identity, BorderLayout.CENTER);
        row.add(rating, BorderLayout.EAST);
        return row;
    }

    private JPanel createProfileCard() {
        JPanel card = new JPanel(new BorderLayout(18, 22));
        card.setBackground(Color.WHITE);
        card.setBorder(ThemeStyle.cardBorder(24, 24, 24, 24));

        String initials = profileName.isEmpty() ? "?" :
                profileName.substring(0, 1).toUpperCase();
        JLabel avatar = new JLabel(initials, SwingConstants.CENTER);
        avatar.setOpaque(true);
        avatar.setBackground(new Color(231, 237, 226));
        avatar.setForeground(ThemeStyle.GREEN);
        avatar.setFont(new Font("Serif", Font.BOLD, 32));
        avatar.setPreferredSize(new Dimension(82, 82));

        JPanel identity = new JPanel(new BorderLayout(18, 0));
        identity.setOpaque(false);
        identity.add(avatar, BorderLayout.WEST);
        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        JLabel eyebrow = new JLabel("HỒ SƠ NGƯỜI CHƠI");
        eyebrow.setForeground(ThemeStyle.GOLD);
        eyebrow.setFont(new Font("SansSerif", Font.BOLD, 10));
        JLabel name = new JLabel(profileName);
        ThemeStyle.heading(name, 25);
        JLabel rank = new JLabel("Thành viên Chess Club");
        rank.setForeground(ThemeStyle.MUTED);
        rank.setFont(new Font("SansSerif", Font.PLAIN, 12));
        labels.add(eyebrow);
        labels.add(Box.createVerticalStrut(5));
        labels.add(name);
        labels.add(Box.createVerticalStrut(4));
        labels.add(rank);
        identity.add(labels, BorderLayout.CENTER);

        JPanel rating = new JPanel(new BorderLayout(4, 4));
        rating.setOpaque(false);
        JLabel ratingLabel = new JLabel("ĐIỂM XẾP HẠNG");
        ratingLabel.setForeground(ThemeStyle.MUTED);
        ratingLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        JLabel ratingValue = new JLabel(String.valueOf(profileElo));
        ratingValue.setForeground(ThemeStyle.GREEN_DARK);
        ratingValue.setFont(new Font("Serif", Font.BOLD, 34));
        rating.add(ratingLabel, BorderLayout.NORTH);
        rating.add(ratingValue, BorderLayout.CENTER);

        JPanel summary = new JPanel(new GridLayout(1, 3, 10, 0));
        summary.setOpaque(false);
        summary.add(statCard("THẮNG", profileWins, new Color(57, 110, 75)));
        summary.add(statCard("THUA", profileLosses, new Color(160, 83, 68)));
        summary.add(statCard("HÒA", profileDraws, new Color(110, 117, 120)));

        card.add(identity, BorderLayout.CENTER);
        card.add(rating, BorderLayout.EAST);
        card.add(summary, BorderLayout.SOUTH);
        return card;
    }

    private JPanel statCard(String title, int value, Color accent) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBackground(ThemeStyle.PAPER);
        card.setBorder(new EmptyBorder(12, 14, 12, 14));
        JLabel label = new JLabel(title);
        label.setForeground(ThemeStyle.MUTED);
        label.setFont(new Font("SansSerif", Font.BOLD, 10));
        JLabel number = new JLabel(String.valueOf(value));
        number.setForeground(accent);
        number.setFont(new Font("Serif", Font.BOLD, 25));
        card.add(label, BorderLayout.NORTH);
        card.add(number, BorderLayout.CENTER);
        return card;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setUser(String name, Integer elo, boolean account) {
        welcome.setText(elo == null
                ? "Xin chào, " + name
                : "Xin chào, " + name + "  ·  Elo " + elo);
        profileButton.setEnabled(account);
    }

    public void updateElo(int elo) {
        String text = welcome.getText();
        int separator = text.indexOf("  ·  Elo ");
        String name = separator >= 0 ? text.substring(0, separator) : text;
        welcome.setText(name + "  ·  Elo " + elo);
    }

    public void setSearching(boolean searching) {
        findButton.setText(searching ? "Hủy tìm trận" : "Tìm trận");
    }

    public void showLeaderboard(String[] parts) {
        leaderboard.clear();
        for (int i = 1; i < parts.length; i++) {
            String[] fields = parts[i].split(":", -1);
            if (fields.length < 5) continue;
            try {
                leaderboard.add(new RankEntry(i, fields[0], Integer.parseInt(fields[1]),
                        Integer.parseInt(fields[2]), Integer.parseInt(fields[3]),
                        Integer.parseInt(fields[4])));
            } catch (NumberFormatException ignored) {
                // Skip malformed leaderboard entries without discarding valid rows.
            }
        }
        renderLeaderboard();
    }

    public void showProfile(String name, int elo, int wins, int losses, int draws) {
        profileName = name;
        profileElo = elo;
        profileWins = wins;
        profileLosses = losses;
        profileDraws = draws;
        if (dataPanel.isVisible() && "Hồ sơ người chơi".equals(dataTitle.getText())) {
            showProfilePanel();
        }
        updateElo(elo);
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Không thể tải dữ liệu", JOptionPane.ERROR_MESSAGE);
    }

    private static final class RankEntry {
        private final int rank;
        private final String name;
        private final int elo;
        private final int wins;
        private final int losses;
        private final int draws;

        private RankEntry(int rank, String name, int elo, int wins, int losses, int draws) {
            this.rank = rank;
            this.name = name;
            this.elo = elo;
            this.wins = wins;
            this.losses = losses;
            this.draws = draws;
        }
    }
}
