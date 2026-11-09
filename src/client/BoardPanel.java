package client;

import protocol.ChessBoard;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Bàn cờ 8x8 vẽ bằng JPanel. Client giữ bản sao ChessBoard chỉ để tô sáng nước đi hợp lệ;
 * quân cờ chỉ được cập nhật khi Server gửi lại MOVE (Server là nguồn sự thật).
 * Người chơi Đen sẽ thấy bàn cờ xoay 180 độ.
 */
public class BoardPanel extends JPanel {
    public interface MoveListener { void onMove(int fc, int fr, int tc, int tr); }

    private static final String KEYS = "KQRBNP";
    private static final String GLYPHS = "\u265A\u265B\u265C\u265D\u265E\u265F";
    private static final Color LIGHT = new Color(240, 217, 181), DARK = new Color(181, 136, 99);

    private ChessBoard board = new ChessBoard();
    private boolean white = true, active = false, waiting = false;
    private int selC = -1, selR = -1;
    private List<int[]> targets = new ArrayList<>();
    private int[] last;
    private MoveListener listener;

    public BoardPanel() {
        setPreferredSize(new Dimension(560, 560));
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { click(e.getX(), e.getY()); }
        });
    }

    public void setMoveListener(MoveListener l) { listener = l; }

    public void start(boolean isWhite) {
        board = new ChessBoard();
        white = isWhite; active = true; waiting = false; last = null;
        clearSel();
        repaint();
    }

    public void setActive(boolean a) { active = a; clearSel(); repaint(); }
    public boolean isMyTurn() { return board.isWhiteTurn() == white; }
    public boolean isInCheck() { return board.inCheck(board.isWhiteTurn()); }
    public void unlock() { waiting = false; }

    /** Áp dụng nước đi do Server xác nhận (của mình hoặc của đối thủ). */
    public void applyMove(int fc, int fr, int tc, int tr) {
        board.move(fc, fr, tc, tr);
        last = new int[]{fc, fr, tc, tr};
        waiting = false;
        clearSel();
        repaint();
    }

    private void clearSel() { selC = selR = -1; targets = new ArrayList<>(); }

    private int cell() { return Math.min(getWidth(), getHeight()) / 8; }
    private int ox() { return (getWidth() - cell() * 8) / 2; }
    private int oy() { return (getHeight() - cell() * 8) / 2; }

    /** Xử lý click: chọn quân của mình -> hiện nước đi; click ô đích hợp lệ -> gửi lên Server. */
    private void click(int x, int y) {
        if (!active || waiting || !isMyTurn()) return;
        int s = cell();
        if (s == 0 || x < ox() || y < oy()) return;
        int dc = (x - ox()) / s, dr = (y - oy()) / s;
        if (dc > 7 || dr > 7) return;
        int c = white ? dc : 7 - dc, r = white ? dr : 7 - dr; // đổi tọa độ màn hình -> tọa độ bàn cờ
        char p = board.get(c, r);
        if (p != '.' && Character.isUpperCase(p) == white) {
            selC = c; selR = r;
            targets = board.legalTargets(c, r);
        } else if (selC >= 0) {
            for (int[] t : targets) {
                if (t[0] == c && t[1] == r) {
                    waiting = true; // chờ Server xác nhận, tránh gửi trùng
                    if (listener != null) listener.onMove(selC, selR, c, r);
                    break;
                }
            }
            clearSel();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int s = cell(), ox = ox(), oy = oy();
        Font pieceFont = new Font("Dialog", Font.PLAIN, (int) (s * 0.8));
        Font smallFont = new Font("SansSerif", Font.BOLD, Math.max(9, s / 6));

        for (int dr = 0; dr < 8; dr++) {
            for (int dc = 0; dc < 8; dc++) {
                int c = white ? dc : 7 - dc, r = white ? dr : 7 - dr;
                int x = ox + dc * s, y = oy + dr * s;
                g.setColor((c + r) % 2 == 0 ? LIGHT : DARK);
                g.fillRect(x, y, s, s);
                if (last != null && ((last[0] == c && last[1] == r) || (last[2] == c && last[3] == r))) {
                    g.setColor(new Color(255, 235, 59, 110)); g.fillRect(x, y, s, s);
                }
                if (c == selC && r == selR) {
                    g.setColor(new Color(76, 175, 80, 150)); g.fillRect(x, y, s, s);
                }
                // nhãn tọa độ
                g.setFont(smallFont);
                g.setColor((c + r) % 2 == 0 ? DARK : LIGHT);
                if (dc == 0) g.drawString(String.valueOf(8 - r), x + 3, y + smallFont.getSize());
                if (dr == 7) g.drawString(String.valueOf((char) ('a' + c)), x + s - smallFont.getSize(), y + s - 3);

                char p = board.get(c, r);
                if (p != '.') drawPiece(g, p, x, y, s, pieceFont);
                for (int[] t : targets) {
                    if (t[0] == c && t[1] == r) {
                        g.setColor(new Color(0, 0, 0, 70));
                        if (p == '.') g.fillOval(x + s * 3 / 8, y + s * 3 / 8, s / 4, s / 4);
                        else { g.setStroke(new BasicStroke(3)); g.drawOval(x + 4, y + 4, s - 8, s - 8); }
                    }
                }
            }
        }
    }

    private void drawPiece(Graphics2D g, char p, int x, int y, int s, Font f) {
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        String ch = String.valueOf(GLYPHS.charAt(KEYS.indexOf(Character.toUpperCase(p))));
        int tx = x + (s - fm.stringWidth(ch)) / 2;
        int ty = y + (s - fm.getHeight()) / 2 + fm.getAscent();
        if (Character.isUpperCase(p)) { // quân Trắng: chữ trắng viền đen
            g.setColor(Color.BLACK);
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) g.drawString(ch, tx + dx, ty + dy);
            g.setColor(Color.WHITE);
        } else {
            g.setColor(new Color(25, 25, 25));
        }
        g.drawString(ch, tx, ty);
    }
}
