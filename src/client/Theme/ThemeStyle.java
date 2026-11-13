package client.Theme;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

final class ThemeStyle {
    static final Color PAPER = new Color(245, 240, 228);
    static final Color PANEL = new Color(255, 253, 247);
    static final Color INK = new Color(43, 53, 48);
    static final Color MUTED = new Color(113, 120, 110);
    static final Color GREEN = new Color(57, 91, 73);
    static final Color GREEN_DARK = new Color(40, 67, 54);
    static final Color LINE = new Color(222, 216, 201);
    static final Color GOLD = new Color(185, 143, 74);

    private ThemeStyle() {}

    static void panel(JPanel panel, Color color) {
        panel.setBackground(color);
    }

    static void heading(JLabel label, int size) {
        label.setForeground(INK);
        label.setFont(new Font("SansSerif", Font.BOLD, size));
    }

    static void body(JLabel label) {
        label.setForeground(MUTED);
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));
    }

    static void button(JButton button, boolean primary) {
        Color base = primary ? GREEN : PANEL;
        Color outline = primary ? GREEN_DARK : LINE;
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setBackground(base);
        button.setForeground(primary ? Color.WHITE : INK);
        button.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setUI(new BasicButtonUI() {
            @Override
            public void update(Graphics graphics, JComponent component) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                AbstractButton current = (AbstractButton) component;
                ButtonModel model = current.getModel();
                Color fill = base;
                if (!current.isEnabled()) {
                    fill = new Color(220, 218, 210);
                } else if (model.isPressed()) {
                    fill = base.darker();
                } else if (model.isRollover()) {
                    fill = base.brighter();
                }

                RoundRectangle2D shape = new RoundRectangle2D.Float(
                        0.5f, 0.5f, component.getWidth() - 1f, component.getHeight() - 1f, 14f, 14f);
                g.setColor(fill);
                g.fill(shape);
                g.setColor(outline);
                g.draw(shape);
                g.dispose();
                super.paint(graphics, component);
            }
        });
    }

    static void field(JComponent field) {
        Border border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(8, 10, 8, 10));
        field.setBorder(border);
        field.setBackground(Color.WHITE);
        field.setForeground(INK);
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(240, 38));
    }

    static void table(JTable table) {
        table.setFillsViewportHeight(true);
        table.setRowHeight(32);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(226, 236, 226));
        table.setSelectionForeground(INK);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(236, 231, 218));
        table.getTableHeader().setForeground(INK);
        table.setBackground(PANEL);
        table.setForeground(INK);
    }
}
