package com.droai.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;

/**
 * Botón circular/redondeado con ícono vectorial + label inferior.
 * Colores derivados dinámicamente de UIManager para soportar tema claro/oscuro.
 */
public class RoundedButton extends JButton {

    private static final int ARC = 16;

    private Color currentBg;
    private final String iconType;
    private final String label;

    public RoundedButton(String iconType, String label) {
        this.iconType = iconType != null ? iconType.toLowerCase() : "";
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

    @Override
    public void updateUI() {
        super.updateUI();
        currentBg = getBaseColor();
    }

    private Color getBaseColor() {
        Color bg = UIManager.getColor("Button.background");
        return bg != null ? bg : new Color(50, 55, 68);
    }

    private Color getHoverColor() {
        return brighter(getBaseColor(), 15);
    }

    private Color getPressColor() {
        return darker(getBaseColor(), 15);
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
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int w = getWidth(), h = getHeight();

        // Sombra sutil
        g2.setColor(new Color(0, 0, 0, 20));
        g2.fill(new RoundRectangle2D.Float(2, 3, w - 4, h - 4, ARC, ARC));

        // Fondo
        g2.setColor(currentBg);
        g2.fill(new RoundRectangle2D.Float(0, 0, w - 2, h - 2, ARC, ARC));

        // Color de texto/ícono
        Color fg = UIManager.getColor("Button.foreground");
        if (fg == null) fg = getForeground();

        // Dibujar ícono vectorial
        g2.setColor(fg);
        paintIconShape(g2, iconType, w / 2 - 1, 24);

        // Label
        Color secondary = UIManager.getColor("Label.disabledForeground");
        g2.setColor(secondary != null ? secondary : new Color(160, 170, 190));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        FontMetrics fmLbl = g2.getFontMetrics();
        int lblX = (w - fmLbl.stringWidth(label)) / 2;
        g2.drawString(label, lblX, h - 8);

        g2.dispose();
    }

    private void paintIconShape(Graphics2D g2, String type, int cx, int cy) {
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (type) {
            case "imprimir", "printer" -> {
                g2.drawRect(cx - 8, cy - 4, 16, 10);
                g2.drawRect(cx - 5, cy - 9, 10, 5);
                g2.drawRect(cx - 5, cy + 1, 10, 5);
                g2.fillRect(cx + 4, cy - 2, 2, 2);
            }
            case "importar", "import" -> {
                g2.draw(new Line2D.Float(cx, cy - 8, cx, cy + 2));
                g2.draw(new Line2D.Float(cx - 4, cy - 2, cx, cy + 2));
                g2.draw(new Line2D.Float(cx + 4, cy - 2, cx, cy + 2));
                g2.draw(new Line2D.Float(cx - 8, cy + 6, cx + 8, cy + 6));
            }
            case "subir", "export", "upload" -> {
                g2.draw(new Line2D.Float(cx, cy + 2, cx, cy - 8));
                g2.draw(new Line2D.Float(cx - 4, cy - 4, cx, cy - 8));
                g2.draw(new Line2D.Float(cx + 4, cy - 4, cx, cy - 8));
                g2.draw(new Line2D.Float(cx - 8, cy + 6, cx + 8, cy + 6));
            }
            case "guardar", "save" -> {
                g2.drawRect(cx - 7, cy - 7, 14, 14);
                g2.drawRect(cx - 4, cy - 7, 8, 5);
                g2.drawRect(cx - 4, cy + 1, 8, 6);
            }
            case "deshacer", "undo" -> {
                Path2D path = new Path2D.Float();
                path.moveTo(cx + 6, cy + 4);
                path.curveTo(cx + 6, cy - 6, cx - 2, cy - 6, cx - 6, cy - 2);
                g2.draw(path);
                g2.draw(new Line2D.Float(cx - 7, cy - 6, cx - 6, cy - 2));
                g2.draw(new Line2D.Float(cx - 2, cy - 6, cx - 6, cy - 2));
            }
            case "tema", "theme" -> {
                g2.drawOval(cx - 6, cy - 6, 12, 12);
                g2.fillArc(cx - 6, cy - 6, 12, 12, 90, 180);
            }
            case "filtrar", "filter" -> {
                Path2D p = new Path2D.Float();
                p.moveTo(cx - 7, cy - 6);
                p.lineTo(cx + 7, cy - 6);
                p.lineTo(cx + 2, cy);
                p.lineTo(cx + 2, cy + 6);
                p.lineTo(cx - 2, cy + 4);
                p.lineTo(cx - 2, cy);
                p.closePath();
                g2.draw(p);
            }
            case "buscar", "search" -> {
                g2.drawOval(cx - 6, cy - 7, 10, 10);
                g2.draw(new Line2D.Float(cx + 2, cy + 1, cx + 7, cy + 6));
            }
            default -> {
                g2.drawOval(cx - 5, cy - 5, 10, 10);
            }
        }
    }
}
