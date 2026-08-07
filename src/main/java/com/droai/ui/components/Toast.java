package com.droai.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Toast Raven-style: fade-in/out, stacking, auto-dismiss.
 */
public class Toast extends JPanel {

    public enum Type {
        SUCCESS(new Color(46, 125, 50),  "✓"),
        ERROR  (new Color(198, 40, 40),  "✗"),
        INFO   (new Color(21, 101, 192), "ℹ"),
        WARNING(new Color(245, 124, 0),  "⚠");

        final Color bg;
        final String icon;
        Type(Color bg, String icon) { this.bg = bg; this.icon = icon; }
    }

    private static final List<Toast> active = new ArrayList<>();
    private static JFrame parentFrame;

    private final String message;
    private final Type type;
    private float opacity = 0f;
    private Timer fadeInTimer, fadeOutTimer, dismissTimer;

    private Toast(String message, Type type) {
        this.message = message;
        this.type = type;
        setOpaque(false);
        setPreferredSize(new Dimension(420, 52));
        setSize(420, 52);
    }

    public static void setParentFrame(JFrame frame) { parentFrame = frame; }

    public static void showSuccess(String message) { show(message, Type.SUCCESS); }
    public static void showError(String message) { show(message, Type.ERROR); }
    public static void showInfo(String message) { show(message, Type.INFO); }
    public static void showWarning(String message) { show(message, Type.WARNING); }

    public static void show(String message, Type type) {
        if (parentFrame == null) return;
        SwingUtilities.invokeLater(() -> {
            Toast toast = new Toast(message, type);
            JLayeredPane lp = parentFrame.getLayeredPane();

            int y = 20;
            for (Toast t : active) y += t.getHeight() + 8;
            active.add(toast);

            toast.setLocation(lp.getWidth() - toast.getWidth() - 20, y);
            lp.add(toast, JLayeredPane.POPUP_LAYER);
            toast.fadeIn();
        });
    }

    private void fadeIn() {
        fadeInTimer = new Timer(16, e -> {
            opacity += 0.08f;
            if (opacity >= 1f) { opacity = 1f; fadeInTimer.stop(); scheduleDismiss(); }
            repaint();
        });
        fadeInTimer.start();
    }

    private void scheduleDismiss() {
        dismissTimer = new Timer(3500, e -> fadeOut());
        dismissTimer.setRepeats(false);
        dismissTimer.start();
    }

    private void fadeOut() {
        fadeOutTimer = new Timer(16, e -> {
            opacity -= 0.06f;
            if (opacity <= 0f) { opacity = 0f; fadeOutTimer.stop(); removeSelf(); }
            repaint();
        });
        fadeOutTimer.start();
    }

    private void removeSelf() {
        JLayeredPane lp = parentFrame.getLayeredPane();
        lp.remove(this);
        lp.repaint();
        active.remove(this);
        reposition();
    }

    private static void reposition() {
        if (parentFrame == null) return;
        JLayeredPane lp = parentFrame.getLayeredPane();
        int y = 20;
        for (Toast t : active) {
            t.setLocation(lp.getWidth() - t.getWidth() - 20, y);
            y += t.getHeight() + 8;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));

        // Shadow
        g2.setColor(new Color(0, 0, 0, 40));
        g2.fill(new RoundRectangle2D.Float(2, 3, getWidth(), getHeight(), 16, 16));
        // Background
        g2.setColor(type.bg);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
        // Icon
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        g2.drawString(type.icon, 16, 33);
        // Message
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.drawString(message, 44, 32);
        g2.dispose();
    }
}
