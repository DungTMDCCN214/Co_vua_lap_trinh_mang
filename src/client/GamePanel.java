package client;

import protocol.Protocol;

import javax.swing.*;
import java.awt.*;

/** Màn hình 3: bàn cờ + khung chat + nút đầu hàng. Quản lý trạng thái lượt đi và kết thúc trận. */
public class GamePanel extends JPanel {
    private final MainFrame frame;
    private final BoardPanel board = new BoardPanel();
    private final JLabel status = new JLabel(" ", SwingConstants.CENTER);
    private final JTextArea chatArea = new JTextArea();
    private final JTextField chatInput = new JTextField();
    private final JButton surrenderBtn = new JButton("Đầu hàng");
    private String opponent = "";
    private boolean over = false;

    public GamePanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        status.setFont(status.getFont().deriveFont(Font.BOLD, 16f));
        add(status, BorderLayout.NORTH);
        add(board, BorderLayout.CENTER);

        // Khung chat bên phải
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(chatArea);
        JButton sendBtn = new JButton("Gửi");
        JPanel inputRow = new JPanel(new BorderLayout(4, 0));
        inputRow.add(chatInput, BorderLayout.CENTER);
        inputRow.add(sendBtn, BorderLayout.EAST);
        JPanel chat = new JPanel(new BorderLayout(4, 4));
        chat.setPreferredSize(new Dimension(240, 0));
        chat.setBorder(BorderFactory.createTitledBorder("Chat"));
        chat.add(scroll, BorderLayout.CENTER);
        chat.add(inputRow, BorderLayout.SOUTH);
        add(chat, BorderLayout.EAST);
        add(surrenderBtn, BorderLayout.SOUTH);

        // Khi người chơi chọn nước đi hợp lệ -> gửi MOVE|fc,fr|tc,tr lên Server
        board.setMoveListener((fc, fr, tc, tr) ->
                frame.send(Protocol.build(Protocol.MOVE, fc + "," + fr, tc + "," + tr)));

        Runnable sendChat = () -> {
            String text = chatInput.getText().trim();
            if (!text.isEmpty() && !over) frame.send(Protocol.CHAT + Protocol.SEP + text);
            chatInput.setText("");
        };
        sendBtn.addActionListener(e -> sendChat.run());
        chatInput.addActionListener(e -> sendChat.run());

        surrenderBtn.addActionListener(e -> {
            if (!over && JOptionPane.showConfirmDialog(frame, "Bạn chắc chắn muốn đầu hàng?", "Đầu hàng",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                frame.send(Protocol.SURRENDER);
            }
        });
    }

    /** Bắt đầu trận mới sau khi nhận MATCH_FOUND. */
    public void startGame(String opponent, boolean white) {
        this.opponent = opponent;
        over = false;
        chatArea.setText("");
        board.start(white);
        surrenderBtn.setEnabled(true);
        addChat("Hệ thống", "Bạn cầm quân " + (white ? "TRẮNG" : "ĐEN") + ", đối thủ: " + opponent);
        updateStatus();
    }

    /** Nhận MOVE từ Server (cả nước đi của mình lẫn của đối thủ) và cập nhật bàn cờ. */
    public void onMove(String from, String to) {
        try {
            String[] f = from.split(","), t = to.split(",");
            board.applyMove(Integer.parseInt(f[0]), Integer.parseInt(f[1]),
                    Integer.parseInt(t[0]), Integer.parseInt(t[1]));
            updateStatus();
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
            // gói tin sai định dạng: bỏ qua
        }
    }

    public void onInvalidMove(String reason) {
        board.unlock();
        status.setText("Nước đi bị từ chối: " + reason);
    }

    public void addChat(String sender, String msg) {
        chatArea.append(sender + ": " + msg + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void updateStatus() {
        if (over) return;
        String s = (board.isMyTurn() ? "Đến lượt bạn" : "Đến lượt " + opponent);
        if (board.isInCheck()) s += " - CHIẾU!";
        status.setText(s);
    }

    /** Kết thúc trận: hiện kết quả rồi quay về Lobby. */
    public void onGameOver(String result, String reason) {
        over = true;
        board.setActive(false);
        surrenderBtn.setEnabled(false);
        String msg;
        switch (reason) {
            case "CHECKMATE": msg = result.equals("WIN") ? "Chiếu hết! Bạn thắng!" : "Bạn bị chiếu hết."; break;
            case "STALEMATE": msg = "Hòa cờ (hết nước đi hợp lệ)."; break;
            case "SURRENDER": msg = result.equals("WIN") ? "Đối thủ đã đầu hàng. Bạn thắng!" : "Bạn đã đầu hàng."; break;
            case "DISCONNECT": msg = result.equals("WIN") ? "Đối thủ mất kết nối. Bạn thắng!" : "Bạn đã mất kết nối."; break;
            default: msg = "Trận đấu kết thúc: " + result;
        }
        status.setText(msg);
        JOptionPane.showMessageDialog(frame, msg, "Kết thúc trận", JOptionPane.INFORMATION_MESSAGE);
        frame.showLobby();
    }
}
