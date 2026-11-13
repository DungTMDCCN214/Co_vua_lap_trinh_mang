package client.Theme;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

final class ThemeStyle {
    static final Color PAPER = new Color(246, 245, 239);
    static final Color PANEL = new Color(255, 254, 250);
    static final Color INK = new Color(47, 57, 51);
    static final Color MUTED = new Color(119, 126, 118);
    static final Color GREEN = new Color(79, 116, 94);
    static final Color GREEN_DARK = new Color(53, 78, 64);
    static final Color DANGER = new Color(166, 84, 71);
    static final Color DANGER_DARK = new Color(137, 67, 57);
    static final Color LINE = new Color(226, 225, 216);
    static final Color GOLD = new Color(177, 145, 91);

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
        button(button, base, primary ? Color.WHITE : INK, outline);
    }

    static void dangerButton(JButton button) {
        button(button, DANGER, Color.WHITE, DANGER_DARK);
    }

    private static void button(JButton button, Color base, Color foreground, Color outline) {
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setBackground(base);
        button.setForeground(foreground);
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
                        0.5f, 0.5f, component.getWidth() - 1f, component.getHeight() - 1f, 18f, 18f);
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
        field.setBorder(new RoundedBorder(LINE, 12, new Insets(7, 11, 7, 11)));
        field.setBackground(PANEL);
        field.setForeground(INK);
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(240, 38));
    }

    static Border cardBorder(int top, int left, int bottom, int right) {
        return new RoundedBorder(LINE, 18, new Insets(top, left, bottom, right));
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

    private static final class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int radius;
        private final Insets insets;

        private RoundedBorder(Color color, int radius, Insets insets) {
            this.color = color;
            this.radius = radius;
            this.insets = insets;
        }

        @Override
        public void paintBorder(Component component, Graphics graphics,
                                int x, int y, int width, int height) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color);
            g.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g.dispose();
        }

        @Override
        public Insets getBorderInsets(Component component) {
            return new Insets(insets.top, insets.left, insets.bottom, insets.right);
        }
    }
}
