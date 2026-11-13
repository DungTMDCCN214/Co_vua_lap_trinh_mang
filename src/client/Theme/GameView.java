package client.Theme;

import client.BoardPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Game screen layout. The existing chess board component is embedded without modification. */
public final class GameView extends JPanel {
    public interface Listener {
        void onMove(int fromColumn, int fromRow, int toColumn, int toRow);
        void onChat(String message);
        void onSurrender();
    }

    private final BoardPanel board = new BoardPanel();
    private final JLabel status = new JLabel(" ");
    private final JTextArea chatArea = new JTextArea();
    private final JTextField chatInput = new JTextField();
    private final JButton sendButton = new JButton("Gửi");
    private final JButton surrenderButton = new JButton("Đầu hàng");
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

        JPanel boardFrame = new JPanel(new BorderLayout());
        boardFrame.setBackground(ThemeStyle.PANEL);
        boardFrame.setBorder(ThemeStyle.cardBorder(12, 12, 12, 12));
        boardFrame.add(board, BorderLayout.CENTER);
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
        ThemeStyle.button(surrenderButton, false);

        JPanel chatInputRow = new JPanel(new BorderLayout(6, 0));
        chatInputRow.setOpaque(false);
        chatInputRow.add(chatInput, BorderLayout.CENTER);
        chatInputRow.add(sendButton, BorderLayout.EAST);
        JPanel chatCard = new JPanel(new BorderLayout(8, 8));
        chatCard.setBackground(ThemeStyle.PANEL);
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

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        footer.add(surrenderButton);

        add(header, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        board.setMoveListener((fc, fr, tc, tr) -> {
            if (listener != null) listener.onMove(fc, fr, tc, tr);
        });
        sendButton.addActionListener(event -> sendChat());
        chatInput.addActionListener(event -> sendChat());
        surrenderButton.addActionListener(event -> {
            if (listener != null) listener.onSurrender();
        });
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
        chatArea.setText("");
    }

    public void applyMove(int fc, int fr, int tc, int tr) {
        board.applyMove(fc, fr, tc, tr);
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
    }
}
