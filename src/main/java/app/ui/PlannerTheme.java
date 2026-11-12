package app.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;

public final class PlannerTheme {
    static Color background;
    static Color surface;
    static Color text;
    static Color muted;
    static Color accent;
    static Color mint;
    static Color line;
    private static boolean dark;

    private PlannerTheme() { }

    public static void install(boolean useDark) {
        dark = useDark;
        background = color(0x101520, 0xEEF2F7);
        surface = color(0x192333, 0xFFFFFF);
        text = color(0xF3F6FC, 0x162236);
        muted = color(0xB7C5DB, 0x495B72);
        accent = color(0xF4BA79, 0x6E3E10);
        mint = color(0x6EE6C1, 0x13725D);
        line = color(0x354359, 0xD9E3EF);
        MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme() {
            protected ColorUIResource getPrimary1() { return new ColorUIResource(line); }
            protected ColorUIResource getPrimary2() { return new ColorUIResource(line); }
            protected ColorUIResource getPrimary3() { return new ColorUIResource(line); }
            protected ColorUIResource getSecondary1() { return new ColorUIResource(line); }
            protected ColorUIResource getSecondary2() { return new ColorUIResource(line); }
            protected ColorUIResource getSecondary3() { return new ColorUIResource(surface); }
            public ColorUIResource getBlack() { return new ColorUIResource(text); }
            public ColorUIResource getWhite() { return new ColorUIResource(background); }
        });
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (UnsupportedLookAndFeelException ex) {
            throw new IllegalStateException(ex);
        }
        for (String component : new String[]{"Panel", "Viewport", "Table", "List", "TextArea", "TextField",
                "ComboBox", "TableHeader", "OptionPane", "Tree", "Spinner"}) {
            UIManager.put(component + ".background", new ColorUIResource(surface));
            UIManager.put(component + ".foreground", new ColorUIResource(text));
        }
        UIManager.put("Button.disabledText", new ColorUIResource(dark ? muted : new Color(0x515674)));
        UIManager.put("TabbedPane.selected", new ColorUIResource(line));
        UIManager.put("TextField.caretForeground", new ColorUIResource(text));
        UIManager.put("ProgressBar.foreground", new ColorUIResource(mint));
        UIManager.put("ProgressBar.background", new ColorUIResource(line));
    }

    private static Color color(int darkValue, int lightValue) { return new Color(dark ? darkValue : lightValue); }
    static boolean dark() { return dark; }

    static JLabel label(String name, int size, boolean bold) {
        JLabel label = new JLabel(name);
        label.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size));
        return label;
    }

    static JPanel card(LayoutManager layout) {
        JPanel panel = new JPanel(layout) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(surface);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g.setColor(line);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(17, 20, 17, 20));
        return panel;
    }

    static JButton button(String name, Runnable action) {
        JButton button = new JButton(name) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean primary = Boolean.TRUE.equals(getClientProperty("primary"));
                boolean nav = Boolean.TRUE.equals(getClientProperty("navigation"));
                g.setColor(isEnabled() && (getModel().isRollover() || primary) ? accent : nav ? surface : line);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                setForeground(isEnabled() && (getModel().isRollover() || primary)
                    ? (dark ? background : Color.WHITE) : (isEnabled() ? text : muted));
                if (hasFocus()) {
                    g.setColor(mint);
                    g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 13, 13);
                }
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setBorder(BorderFactory.createEmptyBorder(8, 13, 8, 13));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> action.run());
        return button;
    }
}
