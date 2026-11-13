package client.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Themed end-of-match notice for wins, losses, draws, resignations, and disconnects. */
final class GameResultDialog extends JDialog {
    private static final Color WIN = new Color(51, 104, 73);
    private static final Color LOSS = new Color(145, 71, 61);
    private static final Color DRAW = new Color(151, 111, 48);

    GameResultDialog(Window owner, String result, String reason, String details) {
        super(owner, "Kết quả ván đấu", ModalityType.APPLICATION_MODAL);
        Color accent = resultColor(result);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(ThemeStyle.PAPER);
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeStyle.LINE),
                new EmptyBorder(18, 18, 18, 18)));

        JPanel banner = new JPanel(new BorderLayout(14, 0));
        banner.setBackground(accent);
        banner.setBorder(new EmptyBorder(18, 20, 18, 20));
        JLabel emblem = new JLabel(result.equals("DRAW") ? "½" : result.equals("WIN") ? "★" : "×",
                SwingConstants.CENTER);
        emblem.setForeground(new Color(238, 221, 180));
        emblem.setFont(new Font("Serif", Font.BOLD, 42));
        emblem.setPreferredSize(new Dimension(58, 60));
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        JLabel overline = new JLabel("VÁN ĐẤU ĐÃ KẾT THÚC");
        overline.setForeground(new Color(196, 204, 188));
        overline.setFont(new Font("SansSerif", Font.BOLD, 10));
        JLabel title = new JLabel(resultTitle(result));
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        JLabel reasonLabel = new JLabel(reasonTitle(result, reason));
        reasonLabel.setForeground(new Color(238, 221, 180));
        reasonLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        titleBlock.add(overline);
        titleBlock.add(Box.createVerticalStrut(5));
        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(3));
        titleBlock.add(reasonLabel);
        banner.add(emblem, BorderLayout.WEST);
        banner.add(titleBlock, BorderLayout.CENTER);

        JPanel detailsCard = new JPanel(new BorderLayout(0, 8));
        detailsCard.setBackground(Color.WHITE);
        detailsCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                new EmptyBorder(14, 14, 14, 14)));
        JLabel label = new JLabel("KẾT QUẢ VÁN");
        label.setForeground(ThemeStyle.MUTED);
        label.setFont(new Font("SansSerif", Font.BOLD, 10));
        JTextArea detailText = new JTextArea(details);
        detailText.setEditable(false);
        detailText.setFocusable(false);
        detailText.setLineWrap(true);
        detailText.setWrapStyleWord(true);
        detailText.setOpaque(false);
        detailText.setBorder(new EmptyBorder(0, 0, 0, 0));
        detailText.setForeground(ThemeStyle.INK);
        detailText.setFont(new Font("SansSerif", Font.PLAIN, 14));
        detailsCard.add(label, BorderLayout.NORTH);
        detailsCard.add(detailText, BorderLayout.CENTER);

        JButton returnButton = new JButton("Về sảnh");
        ThemeStyle.button(returnButton, true);
        returnButton.addActionListener(event -> dispose());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(returnButton);

        content.add(banner, BorderLayout.NORTH);
        content.add(detailsCard, BorderLayout.CENTER);
        content.add(actions, BorderLayout.SOUTH);
        setContentPane(content);
        getRootPane().setDefaultButton(returnButton);
        setSize(470, 340);
        setLocationRelativeTo(owner);
    }

    private static Color resultColor(String result) {
        if ("WIN".equals(result)) return WIN;
        if ("DRAW".equals(result)) return DRAW;
        return LOSS;
    }

    private static String resultTitle(String result) {
        if ("WIN".equals(result)) return "CHIẾN THẮNG";
        if ("DRAW".equals(result)) return "HÒA CỜ";
        return "THẤT BẠI";
    }

    private static String reasonTitle(String result, String reason) {
        switch (reason) {
            case "CHECKMATE":
                return result.equals("WIN") ? "Chiếu hết đối thủ" : "Bạn bị chiếu hết";
            case "STALEMATE":
                return "Không còn nước đi hợp lệ";
            case "SURRENDER":
                return result.equals("WIN") ? "Đối thủ đã đầu hàng" : "Bạn đã đầu hàng";
            case "DISCONNECT":
                return result.equals("WIN") ? "Đối thủ mất kết nối" : "Bạn mất kết nối";
            default:
                return result.equals("DRAW") ? "Trận đấu kết thúc với kết quả hòa" : "Trận đấu đã kết thúc";
        }
    }
}
