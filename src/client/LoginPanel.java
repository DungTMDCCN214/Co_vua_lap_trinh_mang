package client;

import protocol.Protocol;

import javax.swing.*;
import java.awt.*;

/** Màn hình 1: nhập IP Server, Port và tên người chơi. */
public class LoginPanel extends JPanel {
    private final JTextField hostField = new JTextField("localhost", 15);
    private final JTextField portField = new JTextField(String.valueOf(Protocol.DEFAULT_PORT), 15);
    private final JTextField nameField = new JTextField(15);
    private final JButton connectBtn = new JButton("Kết nối");
    private final JLabel errorLabel = new JLabel(" ");

    public LoginPanel(MainFrame frame) {
        setLayout(new GridBagLayout());
        setPreferredSize(new Dimension(820, 600));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("CỜ VUA ONLINE", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2;
        add(title, gc);

        gc.gridwidth = 1;
        addRow(gc, 1, "IP Server:", hostField);
        addRow(gc, 2, "Port:", portField);
        addRow(gc, 3, "Tên người chơi:", nameField);

        gc.gridx = 0; gc.gridy = 4; gc.gridwidth = 2;
        add(connectBtn, gc);
        errorLabel.setForeground(Color.RED);
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gc.gridy = 5;
        add(errorLabel, gc);

        connectBtn.addActionListener(e -> {
            try {
                int port = Integer.parseInt(portField.getText().trim());
                String name = nameField.getText().trim();
                if (name.isEmpty()) { showError("Vui lòng nhập tên người chơi"); return; }
                errorLabel.setText(" ");
                frame.connect(hostField.getText().trim(), port, name);
            } catch (NumberFormatException ex) {
                showError("Port phải là số");
            }
        });
        getRootPane(); // không bắt buộc, giữ để rõ ràng
    }

    private void addRow(GridBagConstraints gc, int row, String label, JComponent field) {
        gc.gridx = 0; gc.gridy = row; add(new JLabel(label), gc);
        gc.gridx = 1; add(field, gc);
    }

    public void setBusy(boolean busy) {
        connectBtn.setEnabled(!busy);
        connectBtn.setText(busy ? "Đang kết nối..." : "Kết nối");
    }

    public void showError(String msg) { errorLabel.setText(msg); }
}
