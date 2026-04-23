package com.droai.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Botón circular/redondeado con ícono (emoji) + label inferior.
 * Colores derivados dinámicamente de UIManager para soportar tema claro/oscuro.
 */
public class RoundedButton extends JButton {

    private static final int ARC = 16;

    private Color currentBg;
    private final String icon;
    private final String label;

    public RoundedButton(String icon, String label) {
        this.icon  = icon;
        this.label = label;
        this.currentBg = getBaseColor();
        setPreferredSize(new Dimension(68, 68));
        setMinimumSize(new Dimension(68, 68));
        setMaximumSize(new Dimension(68, 68));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText(label);

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { currentBg = getHoverColor(); repaint(); }
            @Override public void mouseExited(MouseEvent e)  { currentBg = getBaseColor(); repaint(); }
            @Override public void mousePressed(MouseEvent e)  { currentBg = getPressColor(); repaint(); }
            @Override public void mouseReleased(MouseEvent e) { currentBg = getHoverColor(); repaint(); }
        });
    }

    private Color getBaseColor() {
        Color bg = UIManager.getColor("Button.background");
        return bg != null ? bg : new Color(50, 55, 68);
    }

    private Color getHoverColor() {
        Color base = getBaseColor();
        return brighter(base, 15);
    }

    private Color getPressColor() {
        Color base = getBaseColor();
        return darker(base, 15);
    }

    private Color brighter(Color c, int amount) {
        return new Color(
                Math.min(255, c.getRed() + amount),
                Math.min(255, c.getGreen() + amount),
                Math.min(255, c.getBlue() + amount));
    }

    private Color darker(Color c, int amount) {
        return new Color(
                Math.max(0, c.getRed() - amount),
                Math.max(0, c.getGreen() - amount),
                Math.max(0, c.getBlue() - amount));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth(), h = getHeight();

        // Sombra sutil
        g2.setColor(new Color(0, 0, 0, 20));
        g2.fill(new RoundRectangle2D.Float(2, 3, w - 4, h - 4, ARC, ARC));

        // Fondo — derivado dinámicamente
        g2.setColor(currentBg);
        g2.fill(new RoundRectangle2D.Float(0, 0, w - 2, h - 2, ARC, ARC));

        // Color de texto dinámico del tema
        Color fg = UIManager.getColor("Button.foreground");
        if (fg == null) fg = getForeground();

        // Ícono
        g2.setColor(fg);
        g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        FontMetrics fmIcon = g2.getFontMetrics();
        int iconX = (w - fmIcon.stringWidth(icon)) / 2;
        g2.drawString(icon, iconX, 30);

        // Label
        Color secondary = UIManager.getColor("Label.disabledForeground");
        g2.setColor(secondary != null ? secondary : new Color(160, 170, 190));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        FontMetrics fmLbl = g2.getFontMetrics();
        int lblX = (w - fmLbl.stringWidth(label)) / 2;
        g2.drawString(label, lblX, h - 8);

        g2.dispose();
    }
}
