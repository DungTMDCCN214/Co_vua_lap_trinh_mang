package client.Theme;

import client.BoardPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Game screen layout: chess board, player/opponent clock displays, and in-game controls. */
public final class GameView extends JPanel {
    public interface Listener {
        void onMove(int fromColumn, int fromRow, int toColumn, int toRow);
        void onChat(String message);
        void onSurrender();
        void onDrawOffer();
    }

    private final BoardPanel board = new BoardPanel();
    private final JLabel status = new JLabel(" ");
    private final JTextArea chatArea = new JTextArea();
    private final JTextField chatInput = new JTextField();
    private final JButton sendButton = new JButton("Gửi");
    private final JButton drawButton = new JButton("Xin hòa");
    private final JButton surrenderButton = new JButton("Đầu hàng");

    // Chess clock components & player badges
    private final JLabel opponentInfoLabel = new JLabel("Đối thủ");
    private final JLabel playerInfoLabel = new JLabel("Bạn");
    private final JLabel opponentClockBadge = createClockBadge();
    private final JLabel playerClockBadge = createClockBadge();

    private boolean isBlitz;
    private boolean isWhitePlayer;
    private long playerMs;
    private long opponentMs;
    private boolean isWhiteTurn;
    private boolean clockRunning;
    private final Timer countdownTimer;

    private Listener listener;

    public GameView() {
        super(new BorderLayout(16, 16));
        setBackground(ThemeStyle.PAPER);
        setBorder(new EmptyBorder(18, 22, 18, 22));

        JPanel header = new JPanel(new BorderLayout(8, 4));
        header.setOpaque(false);
        JLabel kicker = new JLabel("VÁN ĐẤU  /  TRỰC TIẾP");
        kicker.setForeground(ThemeStyle.GOLD);
        kicker.setFont(new Font("SansSerif", Font.BOLD, 10));
        ThemeStyle.heading(status, 16);
        header.add(kicker, BorderLayout.NORTH);
        header.add(status, BorderLayout.CENTER);

        // Top bar for opponent info & clock
        JPanel opponentBar = new JPanel(new BorderLayout(8, 0));
        opponentBar.setOpaque(false);
        opponentInfoLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        opponentInfoLabel.setForeground(ThemeStyle.INK);
        opponentBar.add(opponentInfoLabel, BorderLayout.WEST);
        opponentBar.add(opponentClockBadge, BorderLayout.EAST);

        // Bottom bar for current player info & clock
        JPanel playerBar = new JPanel(new BorderLayout(8, 0));
        playerBar.setOpaque(false);
        playerInfoLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        playerInfoLabel.setForeground(ThemeStyle.INK);
        playerBar.add(playerInfoLabel, BorderLayout.WEST);
        playerBar.add(playerClockBadge, BorderLayout.EAST);

        JPanel boardFrame = ThemeStyle.cardPanel(new BorderLayout(0, 8), ThemeStyle.PANEL);
        boardFrame.setBorder(ThemeStyle.cardBorder(12, 12, 12, 12));
        boardFrame.add(opponentBar, BorderLayout.NORTH);
        boardFrame.add(board, BorderLayout.CENTER);
        boardFrame.add(playerBar, BorderLayout.SOUTH);
        boardFrame.setMinimumSize(new Dimension(300, 300));

        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        chatArea.setForeground(ThemeStyle.INK);
        chatArea.setBackground(ThemeStyle.PANEL);
        chatArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        ThemeStyle.field(chatInput);
        ThemeStyle.button(sendButton, true);
        ThemeStyle.button(drawButton, false);
        ThemeStyle.button(surrenderButton, false);

        JPanel chatInputRow = new JPanel(new BorderLayout(6, 0));
        chatInputRow.setOpaque(false);
        chatInputRow.add(chatInput, BorderLayout.CENTER);
        chatInputRow.add(sendButton, BorderLayout.EAST);
        JPanel chatCard = ThemeStyle.cardPanel(new BorderLayout(8, 8), ThemeStyle.PANEL);
        chatCard.setBorder(ThemeStyle.cardBorder(12, 12, 12, 12));
        JLabel chatTitle = new JLabel("TRÒ CHUYỆN");
        chatTitle.setForeground(ThemeStyle.MUTED);
        chatTitle.setFont(new Font("SansSerif", Font.BOLD, 10));
        chatCard.add(chatTitle, BorderLayout.NORTH);
        chatCard.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        chatCard.add(chatInputRow, BorderLayout.SOUTH);
        chatCard.setPreferredSize(new Dimension(245, 0));
        chatCard.setMinimumSize(new Dimension(170, 0));

        JSplitPane center = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, boardFrame, chatCard);
        center.setResizeWeight(0.74);
        center.setContinuousLayout(true);
        center.setOneTouchExpandable(true);
        center.setDividerSize(8);
        center.setBorder(null);
        center.setOpaque(false);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);
        footer.add(drawButton);
        footer.add(surrenderButton);

        add(header, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        board.setMoveListener((fc, fr, tc, tr) -> {
            if (listener != null) listener.onMove(fc, fr, tc, tr);
        });
        sendButton.addActionListener(event -> sendChat());
        chatInput.addActionListener(event -> sendChat());
        drawButton.addActionListener(event -> {
            if (listener != null) listener.onDrawOffer();
        });
        surrenderButton.addActionListener(event -> {
            if (listener != null) listener.onSurrender();
        });

        // 100ms local tick timer for smooth countdown display
        countdownTimer = new Timer(100, event -> {
            if (!isBlitz || !clockRunning) return;
            boolean myTurn = (isWhitePlayer == isWhiteTurn);
            if (myTurn) {
                playerMs = Math.max(0, playerMs - 100);
            } else {
                opponentMs = Math.max(0, opponentMs - 100);
            }
            updateClockLabels();
        });
    }

    private JLabel createClockBadge() {
        JLabel label = new JLabel("05:00", SwingConstants.CENTER);
        label.setFont(new Font("Monospaced", Font.BOLD, 17));
        label.setForeground(ThemeStyle.INK);
        label.setBackground(Color.WHITE);
        label.setOpaque(true);
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(215, 220, 212), 1),
                new EmptyBorder(4, 12, 4, 12)
        ));
        return label;
    }

    private void updateClockLabels() {
        playerClockBadge.setText(formatTime(playerMs));
        opponentClockBadge.setText(formatTime(opponentMs));

        boolean myTurn = (isWhitePlayer == isWhiteTurn);
        styleClockBadge(playerClockBadge, clockRunning && myTurn);
        styleClockBadge(opponentClockBadge, clockRunning && !myTurn);
    }

    private void styleClockBadge(JLabel badge, boolean active) {
        if (active) {
            badge.setBackground(new Color(230, 244, 234));
            badge.setForeground(ThemeStyle.GREEN_DARK);
            badge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeStyle.GREEN, 2),
                    new EmptyBorder(3, 11, 3, 11)
            ));
        } else {
            badge.setBackground(Color.WHITE);
            badge.setForeground(ThemeStyle.INK);
            badge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(215, 220, 212), 1),
                    new EmptyBorder(4, 12, 4, 12)
            ));
        }
    }

    private static String formatTime(long ms) {
        long totalSec = Math.max(0, (ms + 999) / 1000);
        long min = totalSec / 60;
        long sec = totalSec % 60;
        return String.format("%02d:%02d", min, sec);
    }

    public void setupMatch(boolean white, String opponent, String opponentElo,
                           boolean isBlitz, int blitzSeconds, int incrementSeconds) {
        this.isWhitePlayer = white;
        this.isBlitz = isBlitz;
        this.isWhiteTurn = true;
        this.clockRunning = isBlitz;
        if (countdownTimer.isRunning()) countdownTimer.stop();

        String myColor = white ? "TRẮNG" : "ĐEN";
        String oppColor = white ? "ĐEN" : "TRẮNG";
        playerInfoLabel.setText("Bạn · Quân " + myColor);
        opponentInfoLabel.setText("Đối thủ: " + opponent + " (Elo: " + opponentElo + ") · Quân " + oppColor);

        if (isBlitz) {
            long initialMs = (long) (blitzSeconds > 0 ? blitzSeconds : 300) * 1000;
            playerMs = initialMs;
            opponentMs = initialMs;
            opponentClockBadge.setVisible(true);
            playerClockBadge.setVisible(true);
            updateClockLabels();
            countdownTimer.start();
        } else {
            opponentClockBadge.setVisible(false);
            playerClockBadge.setVisible(false);
        }
    }

    public void updateClock(long msWhite, long msBlack, boolean whiteTurn, boolean running) {
        if (!isBlitz) return;
        this.isWhiteTurn = whiteTurn;
        this.clockRunning = running;
        if (isWhitePlayer) {
            playerMs = msWhite;
            opponentMs = msBlack;
        } else {
            playerMs = msBlack;
            opponentMs = msWhite;
        }
        updateClockLabels();
        if (this.clockRunning && !countdownTimer.isRunning()) {
            countdownTimer.start();
        } else if (!this.clockRunning && countdownTimer.isRunning()) {
            countdownTimer.stop();
        }
    }

    public void stopClock() {
        clockRunning = false;
        if (countdownTimer.isRunning()) countdownTimer.stop();
    }

    private void sendChat() {
        String message = chatInput.getText().trim();
        if (!message.isEmpty() && listener != null) listener.onChat(message);
        chatInput.setText("");
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void startBoard(boolean white) {
        board.start(white);
        surrenderButton.setEnabled(true);
        drawButton.setEnabled(true);
        chatArea.setText("");
    }

    public void applyMove(int fc, int fr, int tc, int tr) {
        board.applyMove(fc, fr, tc, tr);
        this.isWhiteTurn = isWhitePlayer ? board.isMyTurn() : !board.isMyTurn();
        updateClockLabels();
    }

    public void unlockBoard() {
        board.unlock();
    }

    public void setBoardActive(boolean active) {
        board.setActive(active);
    }

    public boolean isMyTurn() {
        return board.isMyTurn();
    }

    public boolean isInCheck() {
        return board.isInCheck();
    }

    public void addChat(String sender, String message) {
        chatArea.append(sender + ": " + message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    public void setStatus(String text) {
        status.setText(text);
    }

    public void setSurrenderEnabled(boolean enabled) {
        surrenderButton.setEnabled(enabled);
        drawButton.setEnabled(enabled);
    }

    public void setDrawOfferEnabled(boolean enabled) {
        drawButton.setEnabled(enabled);
    }
}