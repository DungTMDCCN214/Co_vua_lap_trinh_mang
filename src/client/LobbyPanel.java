package client;

import protocol.Protocol;

import javax.swing.*;
import java.awt.*;

/** Màn hình 2: Lobby với nút "Tìm trận nhanh" và trạng thái tìm kiếm. */
public class LobbyPanel extends JPanel {
    private final JLabel welcome = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel status = new JLabel("Sẵn sàng", SwingConstants.CENTER);
    private final JButton findBtn = new JButton("Tìm trận nhanh");
    private boolean searching = false;

    public LobbyPanel(MainFrame frame) {
        setLayout(new GridBagLayout());
        setPreferredSize(new Dimension(820, 600));
        welcome.setFont(welcome.getFont().deriveFont(Font.BOLD, 20f));
        findBtn.setFont(findBtn.getFont().deriveFont(Font.BOLD, 18f));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.insets = new Insets(10, 10, 10, 10);
        gc.gridy = 0; add(welcome, gc);
        gc.gridy = 1; add(findBtn, gc);
        gc.gridy = 2; add(status, gc);

        findBtn.addActionListener(e -> {
            if (!searching) {
                frame.send(Protocol.FIND_MATCH);   // vào hàng đợi; Server sẽ trả WAITING hoặc MATCH_FOUND
                setSearching(true);
            } else {
                frame.send(Protocol.CANCEL_FIND);  // rời hàng đợi
                setSearching(false);
            }
        });
    }

    public void setUser(String name) { welcome.setText("Xin chào, " + name + "!"); }

    public void setSearching(boolean s) {
        searching = s;
        findBtn.setText(s ? "Hủy tìm trận" : "Tìm trận nhanh");
        status.setText(s ? "Đang tìm kiếm đối thủ..." : "Sẵn sàng");
    }
}
