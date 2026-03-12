package com.droai.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Botón circular/redondeado con ícono (emoji) + label inferior.
 * Efecto hover glow y press oscurecido.
 */
public class RoundedButton extends JButton {

    private static final int ARC      = 16;
    private static final Color NORMAL = new Color(50, 55, 68);
    private static final Color HOVER  = new Color(65, 72, 88);
    private static final Color PRESS  = new Color(40, 44, 56);

    private Color currentBg = NORMAL;
    private final String icon;
    private final String label;

    public RoundedButton(String icon, String label) {
        this.icon  = icon;
        this.label = label;
        setPreferredSize(new Dimension(68, 68));
        setMinimumSize(new Dimension(68, 68));
        setMaximumSize(new Dimension(68, 68));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText(label);

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { currentBg = HOVER; repaint(); }
            @Override public void mouseExited(MouseEvent e)  { currentBg = NORMAL; repaint(); }
            @Override public void mousePressed(MouseEvent e)  { currentBg = PRESS; repaint(); }
            @Override public void mouseReleased(MouseEvent e) { currentBg = HOVER; repaint(); }
        });
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

        // Fondo
        g2.setColor(currentBg);
        g2.fill(new RoundRectangle2D.Float(0, 0, w - 2, h - 2, ARC, ARC));

        // Ícono
        g2.setColor(new Color(180, 200, 255));
        g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        FontMetrics fmIcon = g2.getFontMetrics();
        int iconX = (w - fmIcon.stringWidth(icon)) / 2;
        g2.drawString(icon, iconX, 30);

        // Label
        g2.setColor(new Color(160, 170, 190));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        FontMetrics fmLbl = g2.getFontMetrics();
        int lblX = (w - fmLbl.stringWidth(label)) / 2;
        g2.drawString(label, lblX, h - 8);

        g2.dispose();
    }
}
