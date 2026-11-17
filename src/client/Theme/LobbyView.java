package client.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Lobby presentation with a clean 200px sidebar and 4 dedicated screens:
 * 1. Sảnh chờ (Default match hub with 2 large game mode cards)
 * 2. Người online (Online user list with distinct Normal & Blitz invite actions)
 * 3. Bảng xếp hạng (Clean structured leaderboard table with W/D/L order and self-highlight)
 * 4. Thành tích (Player profile card, 80x80 square avatar, and W/D/L stats)
 */
public final class LobbyView extends JPanel {
    public interface Listener {
        void onFindMatch();
        void onFindBlitzMatch();
        void onCancelMatch();
        void onLeaderboardRequested();
        void onProfileRequested();
        void onOnlineListRequested();
        void onInvite(String targetName, boolean blitz);
        void onAcceptInvite(String inviterName);
        void onDeclineInvite(String inviterName);
        void onCancelInvite();
    }

    private static final String TAB_LOBBY = "Sảnh chờ";
    private static final String TAB_ONLINE = "Người online";
    private static final String TAB_LEADERBOARD = "Bảng xếp hạng";
    private static final String TAB_PROFILE = "Thành tích";

    // Sidebar navigation components
    private final JLabel userAvatar = createAvatarBadge("?", 38, 15);
    private final JLabel sidebarUsername = new JLabel("Chưa đăng nhập");
    private final JLabel sidebarElo = new JLabel("Elo -");
    private final JPanel navItemsPanel = new JPanel();
    private final List<NavItemPanel> navItems = new ArrayList<>();
    private String currentTab = TAB_LOBBY;

    // Right Content Area (CardLayout)
    private final CardLayout contentCards = new CardLayout();
    private final JPanel contentArea = new JPanel(contentCards);

    // --- Screen 1: Sảnh chờ ---
    private final JLabel lobbyWelcome = new JLabel("Xin chào, Người chơi");
    private final JButton findNormalBtn = new JButton("Tìm cờ thường");
    private final JButton findBlitzBtn = new JButton("Tìm cờ chớp");
    private final JLabel lobbyStatsLabel = new JLabel("Elo hiện tại: -   ·   Hạng: -");

    // --- Screen 2: Người online ---
    private final JPanel onlineRows = new JPanel();
    private final JScrollPane onlineScroll = new JScrollPane(onlineRows);
    private final List<OnlineUser> onlineUsers = new ArrayList<>();

    // --- Screen 3: Bảng xếp hạng ---
    private final JTextField searchField = new JTextField();
    private final JPanel leaderboardRows = new JPanel();
    private final JScrollPane leaderboardScroll = new JScrollPane(leaderboardRows);
    private final List<RankEntry> leaderboard = new ArrayList<>();

    // --- Screen 4: Thành tích ---
    private final JLabel profileAvatar = createAvatarBadge("?", 80, 32);
    private final JLabel profileNameLabel = new JLabel(" ");
    private final JLabel profileEloLabel = new JLabel("-");
    private final JLabel winCountLabel = new JLabel("0");
    private final JLabel drawCountLabel = new JLabel("0");
    private final JLabel lossCountLabel = new JLabel("0");
    private final JLabel totalGamesLabel = new JLabel("Tổng số ván đã đấu: 0");
    private final JLabel totalGamesValueLabel = new JLabel("0");

    // Invite dialogs & state
    private Listener listener;
    private String currentUsername = "";
    private Integer userElo;
    private int userRank = -1;
    private boolean isSearching;
    private int profileWins;
    private int profileDraws;
    private int profileLosses;

    private JDialog waitingDialog;
    private JDialog incomingDialog;
    private Timer incomingTimer;

    public LobbyView() {
        super(new BorderLayout(0, 0));
        setBackground(ThemeStyle.PAPER);

        // 1. Build Left Sidebar (200px)
        add(createSidebar(), BorderLayout.WEST);

        // 2. Build Right Content Area (Cards)
        contentArea.setBackground(ThemeStyle.PAPER);
        contentArea.add(createLobbyScreen(), TAB_LOBBY);
        contentArea.add(createOnlineScreen(), TAB_ONLINE);
        contentArea.add(createLeaderboardScreen(), TAB_LEADERBOARD);
        contentArea.add(createProfileScreen(), TAB_PROFILE);
        add(contentArea, BorderLayout.CENTER);

        // Default tab selection
        selectTab(TAB_LOBBY, false);
    }

    // =========================================================================
    // SIDEBAR CONSTRUCTION
    // =========================================================================

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 0));
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setMinimumSize(new Dimension(200, 0));
        sidebar.setBackground(Color.WHITE);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, ThemeStyle.LINE));

        // Brand header
        JPanel brandPanel = new JPanel(new BorderLayout(0, 2));
        brandPanel.setOpaque(false);
        brandPanel.setBorder(new EmptyBorder(22, 18, 12, 18));
        JLabel brand = new JLabel("KHAI CUỘC");
        brand.setFont(new Font("SansSerif", Font.BOLD, 17));
        brand.setForeground(ThemeStyle.INK);
        JLabel subtitle = new JLabel("Cờ vua trực tuyến");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 11));
        subtitle.setForeground(ThemeStyle.MUTED);
        brandPanel.add(brand, BorderLayout.NORTH);
        brandPanel.add(subtitle, BorderLayout.SOUTH);

        // User info block
        JPanel userCard = new JPanel(new BorderLayout(10, 0));
        userCard.setOpaque(false);
        userCard.setBorder(new EmptyBorder(8, 18, 18, 18));
        userCard.add(userAvatar, BorderLayout.WEST);

        JPanel userTexts = new JPanel();
        userTexts.setOpaque(false);
        userTexts.setLayout(new BoxLayout(userTexts, BoxLayout.Y_AXIS));
        sidebarUsername.setFont(new Font("SansSerif", Font.BOLD, 13));
        sidebarUsername.setForeground(ThemeStyle.INK);
        sidebarElo.setFont(new Font("SansSerif", Font.PLAIN, 11));
        sidebarElo.setForeground(ThemeStyle.MUTED);
        userTexts.add(sidebarUsername);
        userTexts.add(Box.createVerticalStrut(2));
        userTexts.add(sidebarElo);
        userCard.add(userTexts, BorderLayout.CENTER);

        JPanel topSection = new JPanel(new BorderLayout(0, 0));
        topSection.setOpaque(false);
        topSection.add(brandPanel, BorderLayout.NORTH);
        topSection.add(userCard, BorderLayout.CENTER);
        topSection.add(new JSeparator(JSeparator.HORIZONTAL), BorderLayout.SOUTH);

        // Navigation Items
        navItemsPanel.setOpaque(false);
        navItemsPanel.setLayout(new BoxLayout(navItemsPanel, BoxLayout.Y_AXIS));
        navItemsPanel.setBorder(new EmptyBorder(12, 0, 0, 0));

        addNavItem(TAB_LOBBY);
        addNavItem(TAB_ONLINE);
        addNavItem(TAB_LEADERBOARD);
        addNavItem(TAB_PROFILE);

        sidebar.add(topSection, BorderLayout.NORTH);
        sidebar.add(navItemsPanel, BorderLayout.CENTER);
        return sidebar;
    }

    private void addNavItem(String title) {
        NavItemPanel item = new NavItemPanel(title);
        navItems.add(item);
        navItemsPanel.add(item);
    }

    private void selectTab(String tabName, boolean notifyListener) {
        this.currentTab = tabName;
        for (NavItemPanel item : navItems) {
            item.setActive(item.title.equals(tabName));
        }
        contentCards.show(contentArea, tabName);

        if (notifyListener && listener != null) {
            switch (tabName) {
                case TAB_ONLINE:
                    listener.onOnlineListRequested();
                    break;
                case TAB_LEADERBOARD:
                    listener.onLeaderboardRequested();
                    break;
                case TAB_PROFILE:
                    listener.onProfileRequested();
                    break;
                default:
                    break;
            }
        }
    }

    private final class NavItemPanel extends JPanel {
        private final String title;
        private final JLabel label;
        private boolean active;

        private NavItemPanel(String title) {
            super(new BorderLayout());
            this.title = title;
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            setPreferredSize(new Dimension(200, 42));

            label = new JLabel(title);
            label.setFont(new Font("SansSerif", Font.PLAIN, 13));
            label.setForeground(new Color(75, 85, 80));
            setBorder(new EmptyBorder(10, 20, 10, 16));
            add(label, BorderLayout.CENTER);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!active) {
                        setOpaque(true);
                        setBackground(new Color(243, 245, 241));
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (!active) {
                        setOpaque(false);
                        repaint();
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    selectTab(title, true);
                }
            });
        }

        private void setActive(boolean active) {
            this.active = active;
            setOpaque(active);
            if (active) {
                setBackground(new Color(233, 242, 235));
                label.setForeground(ThemeStyle.GREEN_DARK);
                label.setFont(new Font("SansSerif", Font.BOLD, 13));
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 3, 0, 0, ThemeStyle.GREEN),
                        new EmptyBorder(10, 17, 10, 16)
                ));
            } else {
                setBackground(Color.WHITE);
                label.setForeground(new Color(75, 85, 80));
                label.setFont(new Font("SansSerif", Font.PLAIN, 13));
                setBorder(new EmptyBorder(10, 20, 10, 16));
            }
            repaint();
        }
    }

    // =========================================================================
    // MÀN 1: SẢNH CHỜ (Default match selection hub)
    // =========================================================================

    private JPanel createLobbyScreen() {
        JPanel panel = new JPanel(new BorderLayout(0, 24));
        panel.setBackground(ThemeStyle.PAPER);
        panel.setBorder(new EmptyBorder(28, 32, 28, 32));

        // Header greeting
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        lobbyWelcome.setFont(new Font("SansSerif", Font.BOLD, 22));
        lobbyWelcome.setForeground(ThemeStyle.INK);
        JLabel subtitle = new JLabel("Chọn thể thức để bắt đầu tìm đối thủ");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(ThemeStyle.MUTED);
        header.add(lobbyWelcome);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);

        // Center 2 large match mode cards
        JPanel cardsGrid = new JPanel(new GridLayout(1, 2, 22, 0));
        cardsGrid.setOpaque(false);

        // Card 1: Cờ thường
        ThemeStyle.button(findNormalBtn, true);
        findNormalBtn.setPreferredSize(new Dimension(0, 44));
        findNormalBtn.addActionListener(e -> {
            if (listener == null) return;
            if (isSearching) listener.onCancelMatch();
            else listener.onFindMatch();
        });
        JPanel normalCard = createGameModeCard(
                "CỜ THƯỜNG",
                "Ván đấu thông thường",
                "Không áp dụng đồng hồ đếm ngược. Thích hợp cho người mới bắt đầu và ván cờ chuẩn mực.",
                findNormalBtn,
                null,
                false
        );

        // Card 2: Cờ chớp
        ThemeStyle.button(findBlitzBtn, true);
        findBlitzBtn.setPreferredSize(new Dimension(0, 44));
        findBlitzBtn.addActionListener(e -> {
            if (listener == null) return;
            if (isSearching) listener.onCancelMatch();
            else listener.onFindBlitzMatch();
        });
        JPanel blitzCard = createGameModeCard(
                "CỜ CHỚP",
                "Ván đấu nhanh",
                "Mỗi bên có 5 phút suy nghĩ. Hết thời gian trước sẽ bị xử thua.",
                findBlitzBtn,
                "⚡",
                true
        );

        cardsGrid.add(normalCard);
        cardsGrid.add(blitzCard);

        // Bottom stats info
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        footer.setOpaque(false);
        lobbyStatsLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lobbyStatsLabel.setForeground(ThemeStyle.INK);
        footer.add(lobbyStatsLabel);

        panel.add(header, BorderLayout.NORTH);
        panel.add(cardsGrid, BorderLayout.CENTER);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createGameModeCard(String title, String tag, String description, JButton actionButton,
                                      String badge, boolean blitz) {
        JPanel card = ThemeStyle.cardPanel(new BorderLayout(0, 14), Color.WHITE);
        card.setBorder(ThemeStyle.cardBorder(24, 24, 24, 24));

        JPanel topBlock = new JPanel();
        topBlock.setOpaque(false);
        topBlock.setLayout(new BoxLayout(topBlock, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        titleRow.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 17));
        titleLabel.setForeground(ThemeStyle.INK);
        titleRow.add(titleLabel);
        if (badge != null) {
            JLabel badgeLabel = new JLabel(badge);
            badgeLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            badgeLabel.setForeground(ThemeStyle.GOLD);
            titleRow.add(badgeLabel);
        }

        JLabel tagLabel = new JLabel(tag);
        tagLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        tagLabel.setForeground(ThemeStyle.MUTED);

        JTextArea descArea = new JTextArea(description);
        descArea.setEditable(false);
        descArea.setFocusable(false);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setOpaque(false);
        descArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
        descArea.setForeground(new Color(110, 118, 112));
        descArea.setBorder(new EmptyBorder(8, 0, 0, 0));

        topBlock.add(titleRow);
        topBlock.add(Box.createVerticalStrut(4));
        topBlock.add(tagLabel);
        topBlock.add(descArea);

        card.add(topBlock, BorderLayout.NORTH);
        card.add(new ModeArtwork(blitz), BorderLayout.CENTER);
        card.add(actionButton, BorderLayout.SOUTH);
        return card;
    }

    private static final class ModeArtwork extends JPanel {
        private final boolean blitz;

        private ModeArtwork(boolean blitz) {
            this.blitz = blitz;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (blitz) drawClock(g);
            else drawKnights(g);
            g.dispose();
        }

        private void drawKnights(Graphics2D g) {
            int fontSize = Math.max(12, Math.min((int) (getHeight() * 0.9), (int) (getWidth() * 0.42)));
            Font font = new Font("Dialog", Font.PLAIN, fontSize);
            g.setFont(font);
            FontMetrics metrics = g.getFontMetrics();
            String whiteKnight = "\u2658";
            String blackKnight = "\u265E";
            int gap = Math.max(8, fontSize / 8);
            int totalWidth = metrics.stringWidth(whiteKnight) + gap + metrics.stringWidth(blackKnight);
            int x = (getWidth() - totalWidth) / 2;
            int baseline = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();

            g.setColor(Color.BLACK);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    if (dx != 0 || dy != 0) g.drawString(whiteKnight, x + dx, baseline + dy);
                }
            }
            g.setColor(Color.WHITE);
            g.drawString(whiteKnight, x, baseline);

            x += metrics.stringWidth(whiteKnight) + gap;
            g.setColor(new Color(35, 40, 37));
            g.drawString(blackKnight, x, baseline);
        }

        private void drawClock(Graphics2D g) {
            double size = Math.min(getWidth() * 0.88, getHeight() * 1.12) * 0.82;
            double scale = size / 512.0;
            g.translate((getWidth() - size) / 2.0, (getHeight() - size) / 2.0);
            g.scale(scale, scale);

            g.setColor(new Color(56, 70, 75));
            g.fillRoundRect(92, 430, 80, 48, 4, 4);
            g.fillRoundRect(340, 430, 80, 48, 4, 4);
            g.setColor(new Color(95, 125, 137));
            g.fillRect(92, 430, 52, 48);
            g.fillRect(340, 430, 52, 48);

            g.setColor(new Color(45, 50, 51));
            g.fillRoundRect(128, 94, 120, 26, 10, 10);
            g.setColor(new Color(190, 199, 202));
            g.fillRoundRect(264, 74, 120, 46, 10, 10);

            g.setColor(new Color(12, 14, 14));
            g.fillRoundRect(16, 122, 480, 292, 22, 22);
            g.setColor(new Color(241, 242, 240));
            g.fillRoundRect(24, 130, 464, 276, 18, 18);

            drawDial(g, 145, 268, true);
            drawDial(g, 367, 268, false);

            g.setColor(Color.BLACK);
            g.fillRoundRect(245, 156, 36, 20, 10, 10);
            g.fillRoundRect(245, 360, 36, 20, 10, 10);
        }

        private void drawDial(Graphics2D g, int cx, int cy, boolean first) {
            int radius = 101;
            g.setColor(Color.BLACK);
            g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
            g.setColor(new Color(33, 150, 243));
            g.fillOval(cx - radius + 14, cy - radius + 14, (radius - 14) * 2, (radius - 14) * 2);

            g.setStroke(new BasicStroke(18, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < 12; i++) {
                double angle = Math.toRadians(i * 30 - 90);
                int inner = radius - 25;
                int outer = radius - 16;
                g.drawLine(
                        cx + (int) (Math.cos(angle) * inner),
                        cy + (int) (Math.sin(angle) * inner),
                        cx + (int) (Math.cos(angle) * outer),
                        cy + (int) (Math.sin(angle) * outer)
                );
            }

            g.setStroke(new BasicStroke(18, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (first) {
                g.drawLine(cx, cy, cx, cy - 48);
                g.drawLine(cx, cy, cx + 52, cy + 52);
            } else {
                g.drawLine(cx, cy, cx - 40, cy - 40);
                g.drawLine(cx, cy, cx, cy + 48);
            }
        }
    }

    // =========================================================================
    // MÀN 2: NGƯỜI ONLINE (List with distinct Normal and Blitz invite buttons)
    // =========================================================================

    private JPanel createOnlineScreen() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(ThemeStyle.PAPER);
        panel.setBorder(new EmptyBorder(28, 32, 28, 32));

        // Header with title and refresh button
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Người online");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(ThemeStyle.INK);

        JButton refreshBtn = new JButton("Làm mới");
        ThemeStyle.outlineButton(refreshBtn);
        refreshBtn.setPreferredSize(new Dimension(100, 36));
        refreshBtn.addActionListener(e -> {
            if (listener != null) listener.onOnlineListRequested();
        });

        header.add(title, BorderLayout.WEST);
        header.add(refreshBtn, BorderLayout.EAST);

        // List viewport
        onlineRows.setLayout(new BoxLayout(onlineRows, BoxLayout.Y_AXIS));
        onlineRows.setBackground(ThemeStyle.PAPER);
        onlineScroll.setBorder(BorderFactory.createEmptyBorder());
        onlineScroll.getViewport().setBackground(ThemeStyle.PAPER);

        panel.add(header, BorderLayout.NORTH);
        panel.add(onlineScroll, BorderLayout.CENTER);
        return panel;
    }

    public void showOnlineList(String[] parts) {
        onlineUsers.clear();
        for (int i = 1; i < parts.length; i++) {
            String p = parts[i].trim();
            if (p.isEmpty()) continue;
            String[] fields = p.split(":", -1);
            String name = fields[0];
            String elo = fields.length > 1 ? fields[1] : "-";
            onlineUsers.add(new OnlineUser(name, elo));
        }
        renderOnlineList();
    }

    private void renderOnlineList() {
        onlineRows.removeAll();
        int count = 0;
        for (OnlineUser user : onlineUsers) {
            onlineRows.add(createOnlineUserRow(user));
            onlineRows.add(Box.createVerticalStrut(8));
            count++;
        }
        if (count == 0) {
            JLabel empty = new JLabel("Chưa có ai online. Hãy thử Tìm cờ ở Sảnh chờ.", SwingConstants.CENTER);
            empty.setFont(new Font("SansSerif", Font.PLAIN, 13));
            empty.setForeground(ThemeStyle.MUTED);
            empty.setBorder(new EmptyBorder(40, 0, 40, 0));
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            onlineRows.add(empty);
        }
        onlineRows.revalidate();
        onlineRows.repaint();
    }

    private JPanel createOnlineUserRow(OnlineUser user) {
        JPanel row = ThemeStyle.cardPanel(new BorderLayout(14, 0), Color.WHITE);
        row.setBorder(ThemeStyle.cardBorder(10, 16, 10, 16));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        JLabel avatar = createAvatarBadge(user.name, 40, 15);

        JPanel identity = new JPanel();
        identity.setOpaque(false);
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        JLabel nameLabel = new JLabel(user.name);
        nameLabel.setForeground(ThemeStyle.INK);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        String eloDesc = "-".equals(user.elo) ? "Khách" : "Elo " + user.elo;
        JLabel eloLabel = new JLabel(eloDesc);
        eloLabel.setForeground(ThemeStyle.MUTED);
        eloLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        identity.add(nameLabel);
        identity.add(Box.createVerticalStrut(2));
        identity.add(eloLabel);

        JPanel leftBlock = new JPanel(new BorderLayout(12, 0));
        leftBlock.setOpaque(false);
        leftBlock.add(avatar, BorderLayout.WEST);
        leftBlock.add(identity, BorderLayout.CENTER);

        row.add(leftBlock, BorderLayout.CENTER);

        boolean isSelf = (currentUsername != null && user.name.equalsIgnoreCase(currentUsername));
        if (isSelf) {
            JLabel selfLabel = new JLabel("(Bạn)  ");
            selfLabel.setForeground(ThemeStyle.MUTED);
            selfLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
            row.add(selfLabel, BorderLayout.EAST);
        } else {
            JButton normalBtn = new JButton("Mời cờ thường");
            JButton blitzBtn = new JButton("Mời cờ chớp");

            // Normal invite = Secondary (white background, green border/text)
            ThemeStyle.outlineButton(normalBtn);
            normalBtn.setPreferredSize(new Dimension(135, 36));
            normalBtn.setFont(new Font("SansSerif", Font.BOLD, 12));

            // Blitz invite = Primary (green background, white text)
            ThemeStyle.button(blitzBtn, true);
            blitzBtn.setPreferredSize(new Dimension(135, 36));
            blitzBtn.setFont(new Font("SansSerif", Font.BOLD, 12));

            normalBtn.addActionListener(e -> {
                if (listener != null) listener.onInvite(user.name, false);
            });
            blitzBtn.addActionListener(e -> {
                if (listener != null) listener.onInvite(user.name, true);
            });

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            actions.setOpaque(false);
            actions.add(normalBtn);
            actions.add(blitzBtn);
            row.add(actions, BorderLayout.EAST);
        }

        return row;
    }

    // =========================================================================
    // MÀN 3: BẢNG XẾP HẠNG (Table with headers, W/D/L order, self highlight)
    // =========================================================================

    private JPanel createLeaderboardScreen() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(ThemeStyle.PAPER);
        panel.setBorder(new EmptyBorder(28, 32, 28, 32));

        // Header with title and refresh button
        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setOpaque(false);
        JLabel title = new JLabel("Bảng xếp hạng");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(ThemeStyle.INK);

        JButton refreshBtn = new JButton("Làm mới");
        ThemeStyle.outlineButton(refreshBtn);
        refreshBtn.setPreferredSize(new Dimension(100, 36));
        refreshBtn.addActionListener(e -> {
            if (listener != null) listener.onLeaderboardRequested();
        });

        topHeader.add(title, BorderLayout.WEST);
        topHeader.add(refreshBtn, BorderLayout.EAST);

        // Search field with real-time filtering
        ThemeStyle.field(searchField);
        searchField.setPreferredSize(new Dimension(220, 36));
        searchField.putClientProperty("JTextField.placeholderText", "Tìm người chơi...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { renderLeaderboard(); }
            @Override public void removeUpdate(DocumentEvent e) { renderLeaderboard(); }
            @Override public void changedUpdate(DocumentEvent e) { renderLeaderboard(); }
        });

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        searchBar.setOpaque(false);
        searchBar.add(searchField);

        JPanel topArea = new JPanel(new BorderLayout(0, 10));
        topArea.setOpaque(false);
        topArea.add(topHeader, BorderLayout.NORTH);
        topArea.add(searchBar, BorderLayout.SOUTH);

        // Table column headers
        JPanel tableHeader = new JPanel(new BorderLayout(12, 0));
        tableHeader.setOpaque(false);
        tableHeader.setBorder(new EmptyBorder(4, 16, 4, 16));

        JLabel colRank = new JLabel("Hạng", SwingConstants.CENTER);
        colRank.setPreferredSize(new Dimension(50, 20));
        colRank.setFont(new Font("SansSerif", Font.BOLD, 12));
        colRank.setForeground(ThemeStyle.MUTED);

        JLabel colUser = new JLabel("Người chơi");
        colUser.setFont(new Font("SansSerif", Font.BOLD, 12));
        colUser.setForeground(ThemeStyle.MUTED);

        JLabel colRecord = new JLabel("Thắng / Hòa / Thua", SwingConstants.CENTER);
        colRecord.setPreferredSize(new Dimension(150, 20));
        colRecord.setFont(new Font("SansSerif", Font.BOLD, 12));
        colRecord.setForeground(ThemeStyle.MUTED);

        JLabel colElo = new JLabel("Elo", SwingConstants.RIGHT);
        colElo.setPreferredSize(new Dimension(80, 20));
        colElo.setFont(new Font("SansSerif", Font.BOLD, 12));
        colElo.setForeground(ThemeStyle.MUTED);

        JPanel headerRight = new JPanel(new BorderLayout(12, 0));
        headerRight.setOpaque(false);
        headerRight.add(colRecord, BorderLayout.WEST);
        headerRight.add(colElo, BorderLayout.EAST);

        tableHeader.add(colRank, BorderLayout.WEST);
        tableHeader.add(colUser, BorderLayout.CENTER);
        tableHeader.add(headerRight, BorderLayout.EAST);

        // List viewport
        leaderboardRows.setLayout(new BoxLayout(leaderboardRows, BoxLayout.Y_AXIS));
        leaderboardRows.setBackground(ThemeStyle.PAPER);
        leaderboardScroll.setBorder(BorderFactory.createEmptyBorder());
        leaderboardScroll.getViewport().setBackground(ThemeStyle.PAPER);

        JPanel tableWrapper = new JPanel(new BorderLayout(0, 4));
        tableWrapper.setOpaque(false);
        tableWrapper.add(tableHeader, BorderLayout.NORTH);
        tableWrapper.add(leaderboardScroll, BorderLayout.CENTER);

        panel.add(topArea, BorderLayout.NORTH);
        panel.add(tableWrapper, BorderLayout.CENTER);
        return panel;
    }

    public void showLeaderboard(String[] parts) {
        leaderboard.clear();
        userRank = -1;
        for (int i = 1; i < parts.length; i++) {
            String[] fields = parts[i].split(":", -1);
            if (fields.length < 5) continue;
            try {
                RankEntry entry = new RankEntry(i, fields[0], Integer.parseInt(fields[1]),
                        Integer.parseInt(fields[2]), Integer.parseInt(fields[3]),
                        Integer.parseInt(fields[4]));
                leaderboard.add(entry);
                if (currentUsername != null && currentUsername.equalsIgnoreCase(entry.name)) {
                    userRank = entry.rank;
                }
            } catch (NumberFormatException ignored) {}
        }
        updateLobbyStats();
        renderLeaderboard();
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
            leaderboardRows.add(Box.createVerticalStrut(6));
            visibleRows++;
        }
        if (visibleRows == 0) {
            JLabel empty = new JLabel(query.isEmpty()
                    ? "Chưa có người chơi trong bảng xếp hạng."
                    : "Không tìm thấy người chơi phù hợp.", SwingConstants.CENTER);
            empty.setFont(new Font("SansSerif", Font.PLAIN, 13));
            empty.setForeground(ThemeStyle.MUTED);
            empty.setBorder(new EmptyBorder(40, 0, 40, 0));
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            leaderboardRows.add(empty);
        }
        leaderboardRows.revalidate();
        leaderboardRows.repaint();
    }

    private JPanel createRankRow(RankEntry entry) {
        boolean isSelf = (currentUsername != null && currentUsername.equalsIgnoreCase(entry.name));

        JPanel row = ThemeStyle.cardPanel(new BorderLayout(12, 0),
                    isSelf ? new Color(233, 242, 235) : Color.WHITE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        row.setPreferredSize(new Dimension(0, 48));

        if (isSelf) {
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeStyle.GREEN, 1, true),
                    new EmptyBorder(6, 14, 6, 14)
            ));
        } else {
            row.setBorder(ThemeStyle.cardBorder(6, 14, 6, 14));
        }

        // Rank badge (Col 1)
        JLabel rankBadge = createRankBadge(entry.rank);

        // Player Name + Avatar (Col 2)
        JLabel avatar = createAvatarBadge(entry.name, 28, 11);
        JLabel nameLabel = new JLabel(isSelf ? entry.name + " (Bạn)" : entry.name);
        nameLabel.setFont(new Font("SansSerif", isSelf ? Font.BOLD : Font.PLAIN, 13));
        nameLabel.setForeground(isSelf ? ThemeStyle.GREEN_DARK : ThemeStyle.INK);

        JPanel playerCol = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        playerCol.setOpaque(false);
        playerCol.add(avatar);
        playerCol.add(nameLabel);

        // Record W / D / L (Col 3)
        JLabel recordLabel = new JLabel(entry.wins + " / " + entry.draws + " / " + entry.losses, SwingConstants.CENTER);
        recordLabel.setPreferredSize(new Dimension(150, 20));
        recordLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        recordLabel.setForeground(ThemeStyle.INK);

        // Elo (Col 4)
        JLabel eloLabel = new JLabel(String.valueOf(entry.elo), SwingConstants.RIGHT);
        eloLabel.setPreferredSize(new Dimension(80, 20));
        eloLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        eloLabel.setForeground(ThemeStyle.GREEN_DARK);

        JPanel rightCol = new JPanel(new BorderLayout(12, 0));
        rightCol.setOpaque(false);
        rightCol.add(recordLabel, BorderLayout.WEST);
        rightCol.add(eloLabel, BorderLayout.EAST);

        row.add(rankBadge, BorderLayout.WEST);
        row.add(playerCol, BorderLayout.CENTER);
        row.add(rightCol, BorderLayout.EAST);
        return row;
    }

    private JLabel createRankBadge(int rank) {
        JLabel badge = new JLabel(String.valueOf(rank), SwingConstants.CENTER);
        badge.setPreferredSize(new Dimension(32, 28));
        badge.setOpaque(true);
        badge.setFont(new Font("SansSerif", Font.BOLD, 12));

        if (rank == 1) { // Gold
            badge.setBackground(new Color(245, 230, 185));
            badge.setForeground(new Color(150, 110, 30));
        } else if (rank == 2) { // Silver
            badge.setBackground(new Color(230, 234, 236));
            badge.setForeground(new Color(100, 110, 115));
        } else if (rank == 3) { // Bronze
            badge.setBackground(new Color(240, 222, 208));
            badge.setForeground(new Color(145, 95, 60));
        } else { // 4+
            badge.setBackground(new Color(240, 239, 235));
            badge.setForeground(ThemeStyle.MUTED);
        }
        return badge;
    }

    // =========================================================================
    // MÀN 4: THÀNH TÍCH (Player profile, 80x80 square avatar, W/D/L order)
    // =========================================================================

    private JPanel createProfileScreen() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(ThemeStyle.PAPER);
        panel.setBorder(new EmptyBorder(28, 32, 28, 32));

        // Header with title and refresh button
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Thành tích");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(ThemeStyle.INK);

        JButton refreshBtn = new JButton("Làm mới");
        ThemeStyle.outlineButton(refreshBtn);
        refreshBtn.setPreferredSize(new Dimension(100, 36));
        refreshBtn.addActionListener(e -> {
            if (listener != null) listener.onProfileRequested();
        });

        header.add(title, BorderLayout.WEST);
        header.add(refreshBtn, BorderLayout.EAST);

        // User Identity Card (80x80 square avatar, name, subtitle, right-aligned Elo)
        JPanel userCard = ThemeStyle.cardPanel(new BorderLayout(16, 0), Color.WHITE);
        userCard.setBorder(ThemeStyle.cardBorder(20, 24, 20, 24));

        profileAvatar.setPreferredSize(new Dimension(80, 80));
        profileAvatar.setMinimumSize(new Dimension(80, 80));

        JPanel nameBlock = new JPanel();
        nameBlock.setOpaque(false);
        nameBlock.setLayout(new BoxLayout(nameBlock, BoxLayout.Y_AXIS));
        profileNameLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        profileNameLabel.setForeground(ThemeStyle.INK);
        JLabel roleLabel = new JLabel("Thành viên Chess Club");
        roleLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        roleLabel.setForeground(ThemeStyle.MUTED);
        nameBlock.add(Box.createVerticalGlue());
        nameBlock.add(profileNameLabel);
        nameBlock.add(Box.createVerticalStrut(4));
        nameBlock.add(roleLabel);
        nameBlock.add(Box.createVerticalGlue());

        JPanel eloBlock = new JPanel(new BorderLayout(0, 2));
        eloBlock.setOpaque(false);
        JLabel eloTag = new JLabel("ELO", SwingConstants.RIGHT);
        eloTag.setFont(new Font("SansSerif", Font.BOLD, 11));
        eloTag.setForeground(ThemeStyle.MUTED);
        profileEloLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        profileEloLabel.setForeground(ThemeStyle.GREEN_DARK);
        profileEloLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        eloBlock.add(eloTag, BorderLayout.NORTH);
        eloBlock.add(profileEloLabel, BorderLayout.CENTER);

        userCard.add(profileAvatar, BorderLayout.WEST);
        userCard.add(nameBlock, BorderLayout.CENTER);
        userCard.add(eloBlock, BorderLayout.EAST);

        // Stats Row: Tổng ván / THẮNG / HÒA / THUA — fixed height, top-aligned
        totalGamesLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        totalGamesLabel.setForeground(ThemeStyle.MUTED);

        JPanel statsRow = new JPanel(new GridLayout(1, 4, 16, 0));
        statsRow.setOpaque(false);
        statsRow.setPreferredSize(new Dimension(0, 120));
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        statsRow.add(createStatBox("TỔNG VÁN", totalGamesValueLabel,
                new Color(100, 100, 100), new Color(180, 180, 180)));
        statsRow.add(createStatBox("THẮNG", winCountLabel,
                new Color(51, 104, 73), new Color(51, 104, 73)));
        statsRow.add(createStatBox("HÒA", drawCountLabel,
                new Color(80, 110, 120), new Color(80, 110, 120)));
        statsRow.add(createStatBox("THUA", lossCountLabel,
                new Color(145, 71, 61), new Color(145, 71, 61)));

        // Wrap statsRow in a NORTH-anchored container so it doesn't stretch
        JPanel statsWrapper = new JPanel(new BorderLayout());
        statsWrapper.setOpaque(false);
        statsWrapper.add(statsRow, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.add(userCard, BorderLayout.NORTH);
        body.add(statsWrapper, BorderLayout.CENTER);

        panel.add(header, BorderLayout.NORTH);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStatBox(String title, JLabel valueLabel, Color valueColor, Color accentColor) {
        // Top color bar (4px)
        JPanel topBar = new JPanel();
        topBar.setBackground(accentColor);
        topBar.setPreferredSize(new Dimension(0, 4));
        topBar.setOpaque(true);

        // Label + number stacked top-left, no stretching
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        titleLabel.setForeground(new Color(90, 90, 100));

        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 40));
        valueLabel.setForeground(valueColor);

        JPanel textContent = new JPanel();
        textContent.setOpaque(false);
        textContent.setLayout(new BoxLayout(textContent, BoxLayout.Y_AXIS));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textContent.add(Box.createVerticalStrut(10));
        textContent.add(titleLabel);
        textContent.add(Box.createVerticalStrut(2));
        textContent.add(valueLabel);

        JPanel inner = new JPanel(new BorderLayout());
        inner.setOpaque(false);
        inner.setBorder(BorderFactory.createEmptyBorder(0, 14, 10, 14));
        inner.add(textContent, BorderLayout.NORTH);

        JPanel box = ThemeStyle.cardPanel(new BorderLayout(), Color.WHITE);
        box.setBorder(ThemeStyle.cardBorder(0, 0, 0, 0));
        box.add(topBar, BorderLayout.NORTH);
        box.add(inner, BorderLayout.CENTER);
        return box;
    }

    public void showProfile(String name, int elo, int wins, int losses, int draws) {
        profileWins = wins;
        profileDraws = draws;
        profileLosses = losses;

        profileNameLabel.setText(name);
        profileEloLabel.setText(String.valueOf(elo));
        updateAvatarLetter(profileAvatar, name);

        winCountLabel.setText(String.valueOf(wins));
        drawCountLabel.setText(String.valueOf(draws));
        lossCountLabel.setText(String.valueOf(losses));
        int total = wins + draws + losses;
        totalGamesValueLabel.setText(String.valueOf(total));
        totalGamesLabel.setText("Tổng số ván đã đấu: " + total);

        updateElo(elo);
    }

    // =========================================================================
    // GENERAL USER & MATCH STATE
    // =========================================================================

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setUser(String name, Integer elo, boolean account) {
        currentUsername = name;
        userElo = elo;
        userRank = -1;

        sidebarUsername.setText(name);
        sidebarElo.setText(elo == null ? "Khách" : "Elo " + elo);
        updateAvatarLetter(userAvatar, name);

        lobbyWelcome.setText("Xin chào, " + name);
        profileNameLabel.setText(name);
        updateAvatarLetter(profileAvatar, name);

        updateLobbyStats();
    }

    public void updateElo(int elo) {
        userElo = elo;
        sidebarElo.setText("Elo " + elo);
        profileEloLabel.setText(String.valueOf(elo));
        updateLobbyStats();
    }

    private void updateLobbyStats() {
        String eloStr = userElo == null ? "-" : String.valueOf(userElo);
        String rankStr = userRank > 0 ? String.valueOf(userRank) : "-";
        lobbyStatsLabel.setText("Elo hiện tại: " + eloStr + "   ·   Hạng: " + rankStr);
    }

    public void setSearching(boolean searching) {
        this.isSearching = searching;
        if (searching) {
            findNormalBtn.setText("Hủy tìm trận");
            findBlitzBtn.setText("Hủy tìm trận");
        } else {
            findNormalBtn.setText("Tìm cờ thường");
            findBlitzBtn.setText("Tìm cờ chớp");
        }
    }

    public void showError(String message) {
        ThemedNoticeDialog.showNotice(this, "Thông báo", "Thông báo lỗi", message, true);
    }

    // =========================================================================
    // INCOMING & WAITING INVITE DIALOGS
    // =========================================================================

    public void showWaitingInviteDialog(String targetUser) {
        dismissInviteDialogs();
        Window window = SwingUtilities.getWindowAncestor(this);
        waitingDialog = new JDialog(window, "Đang chờ phản hồi", Dialog.ModalityType.MODELESS);
        waitingDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        waitingDialog.setResizable(false);

        JPanel content = ThemeStyle.cardPanel(new BorderLayout(0, 16), ThemeStyle.PAPER);
        content.setBackground(ThemeStyle.PAPER);
        content.setBorder(ThemeStyle.cardBorder(20, 24, 20, 24));

        JLabel info = new JLabel("<html>Đang chờ <b>" + targetUser + "</b> phản hồi lời mời thách đấu...</html>");
        info.setFont(new Font("SansSerif", Font.PLAIN, 14));
        info.setForeground(ThemeStyle.INK);

        JButton cancelBtn = new JButton("Hủy lời mời");
        ThemeStyle.button(cancelBtn, false);
        cancelBtn.addActionListener(e -> {
            dismissInviteDialogs();
            if (listener != null) listener.onCancelInvite();
        });

        content.add(info, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(cancelBtn);
        content.add(actions, BorderLayout.SOUTH);

        waitingDialog.setContentPane(content);
        waitingDialog.pack();
        waitingDialog.setLocationRelativeTo(window);
        waitingDialog.setVisible(true);
    }

    public void showIncomingInviteDialog(String inviter, String elo, boolean blitz) {
        dismissInviteDialogs();
        Window window = SwingUtilities.getWindowAncestor(this);
        incomingDialog = new JDialog(window, "Lời mời thách đấu", Dialog.ModalityType.APPLICATION_MODAL);
        incomingDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        incomingDialog.setResizable(false);

        JPanel content = ThemeStyle.cardPanel(new BorderLayout(0, 16), ThemeStyle.PAPER);
        content.setBackground(ThemeStyle.PAPER);
        content.setBorder(ThemeStyle.cardBorder(20, 24, 20, 24));

        String modeName = blitz ? "Cờ chớp (5 phút)" : "Cờ thường";
        String eloText = "-".equals(elo) ? "Khách" : "Elo " + elo;
        JLabel headerLabel = new JLabel("LỜI MỜI THÁCH ĐẤU");
        headerLabel.setForeground(ThemeStyle.MUTED);
        headerLabel.setFont(new Font("SansSerif", Font.BOLD, 11));

        JLabel info = new JLabel("<html><b>" + inviter + "</b> (" + eloText + ") mời bạn tham gia ván đấu.<br>Chế độ: <b>" + modeName + "</b></html>");
        info.setFont(new Font("SansSerif", Font.PLAIN, 14));
        info.setForeground(ThemeStyle.INK);

        int[] remainingSeconds = {30};
        JLabel countdownLabel = new JLabel("Tự động từ chối sau: " + remainingSeconds[0] + "s");
        countdownLabel.setForeground(ThemeStyle.MUTED);
        countdownLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));

        JPanel centerBlock = new JPanel();
        centerBlock.setOpaque(false);
        centerBlock.setLayout(new BoxLayout(centerBlock, BoxLayout.Y_AXIS));
        centerBlock.add(headerLabel);
        centerBlock.add(Box.createVerticalStrut(6));
        centerBlock.add(info);
        centerBlock.add(Box.createVerticalStrut(8));
        centerBlock.add(countdownLabel);

        JButton acceptBtn = new JButton("Chấp nhận");
        JButton declineBtn = new JButton("Từ chối");
        ThemeStyle.button(acceptBtn, true);
        ThemeStyle.button(declineBtn, false);

        incomingTimer = new Timer(1000, e -> {
            remainingSeconds[0]--;
            if (remainingSeconds[0] <= 0) {
                dismissInviteDialogs();
                if (listener != null) listener.onDeclineInvite(inviter);
            } else {
                countdownLabel.setText("Tự động từ chối sau: " + remainingSeconds[0] + "s");
            }
        });
        incomingTimer.start();

        acceptBtn.addActionListener(e -> {
            dismissInviteDialogs();
            if (listener != null) listener.onAcceptInvite(inviter);
        });

        declineBtn.addActionListener(e -> {
            dismissInviteDialogs();
            if (listener != null) listener.onDeclineInvite(inviter);
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(declineBtn);
        actions.add(acceptBtn);

        content.add(centerBlock, BorderLayout.CENTER);
        content.add(actions, BorderLayout.SOUTH);

        incomingDialog.setContentPane(content);
        incomingDialog.pack();
        incomingDialog.setLocationRelativeTo(window);
        incomingDialog.setVisible(true);
    }

    public void dismissInviteDialogs() {
        if (incomingTimer != null && incomingTimer.isRunning()) {
            incomingTimer.stop();
            incomingTimer = null;
        }
        if (waitingDialog != null) {
            waitingDialog.dispose();
            waitingDialog = null;
        }
        if (incomingDialog != null) {
            incomingDialog.dispose();
            incomingDialog = null;
        }
    }

    public void onInviteDeclined(String name) {
        dismissInviteDialogs();
        ThemedNoticeDialog.showNotice(this, "Lời mời bị từ chối", "Lời mời bị từ chối",
                name + " đã từ chối lời mời hoặc đang bận.", false);
    }

    public void onInviteCancelled(String name) {
        dismissInviteDialogs();
        ThemedNoticeDialog.showNotice(this, "Lời mời đã hủy", "Lời mời đã hủy",
                name + " đã hủy lời mời thách đấu.", false);
    }

    public void onInviteExpired(String name) {
        dismissInviteDialogs();
        ThemedNoticeDialog.showNotice(this, "Lời mời hết hạn", "Lời mời hết hạn",
                "Lời mời đấu với " + name + " đã hết hạn (30 giây).", false);
    }

    // =========================================================================
    // HELPER UTILITIES
    // =========================================================================

    private static JLabel createAvatarBadge(String text, int size, int fontSize) {
        String letter = text == null || text.trim().isEmpty() ? "?" : text.trim().substring(0, 1).toUpperCase();
        JLabel badge = new JLabel(letter, SwingConstants.CENTER);
        badge.setOpaque(true);
        badge.setBackground(new Color(231, 237, 226));
        badge.setForeground(ThemeStyle.GREEN);
        badge.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        badge.setPreferredSize(new Dimension(size, size));
        return badge;
    }

    private static void updateAvatarLetter(JLabel badge, String name) {
        if (badge == null) return;
        String letter = name == null || name.trim().isEmpty() ? "?" : name.trim().substring(0, 1).toUpperCase();
        badge.setText(letter);
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

    private static final class OnlineUser {
        private final String name;
        private final String elo;

        private OnlineUser(String name, String elo) {
            this.name = name;
            this.elo = elo;
        }
    }
}