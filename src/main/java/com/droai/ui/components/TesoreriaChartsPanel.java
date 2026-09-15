package com.droai.ui.components;

import com.droai.service.TesoreriaService.ReporteEjecutivoData;
import com.droai.ui.ThemeManager;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Panel de visualización ejecutiva con gráficas interactivas nativas (Java2D):
 * 1. Donut Chart de distribución de egresos por Banco.
 * 2. Barras horizontales estilizadas para el Top de Conceptos de Gasto.
 * 3. Gráfico de barras de evolución temporal diaria de egresos en el período.
 */
public class TesoreriaChartsPanel extends JPanel {

    private final ThemeManager tm = ThemeManager.get();
    private final DonutChartPanel donutPanel = new DonutChartPanel();
    private final ConceptosBarPanel conceptosPanel = new ConceptosBarPanel();
    private final TimelineBarPanel timelinePanel = new TimelineBarPanel();

    private static final DecimalFormat CURRENCY_USD = new DecimalFormat("$ #,##0.00");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM");

    private static final Color[] PALETTE = {
            new Color(37, 99, 235),   // Azul (Provincial)
            new Color(16, 185, 129),  // Verde (Banesco)
            new Color(245, 158, 11),  // Ámbar (BNC)
            new Color(139, 92, 246),  // Púrpura (Venezuela)
            new Color(236, 72, 153),  // Rosa
            new Color(20, 184, 166),  // Teal
            new Color(249, 115, 22)   // Naranja
    };

    public TesoreriaChartsPanel() {
        setOpaque(false);
        setLayout(new MigLayout("insets 0, fill, gap 16", "[grow 45, fill][grow 55, fill]", "[grow 55, fill][grow 45, fill]"));

        // Card 1: Distribución por Banco (Izquierda ocupa 2 filas)
        RoundedPanel cardDonut = new RoundedPanel(14, true);
        cardDonut.setBackground(tm.cardBg());
        cardDonut.setLayout(new BorderLayout());
        cardDonut.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 3, 0, tm.accent()),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        JLabel lblDonutTitle = new JLabel("Distribución de Egresos por Banco");
        lblDonutTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblDonutTitle.setForeground(tm.textPrimary());
        cardDonut.add(lblDonutTitle, BorderLayout.NORTH);
        cardDonut.add(donutPanel, BorderLayout.CENTER);
        add(cardDonut, "cell 0 0 1 2, grow");

        // Card 2: Top Conceptos de Gasto (Derecha Superior)
        RoundedPanel cardConceptos = new RoundedPanel(14, true);
        cardConceptos.setBackground(tm.cardBg());
        cardConceptos.setLayout(new BorderLayout());
        cardConceptos.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 3, 0, tm.greenAccent()),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        JLabel lblConceptosTitle = new JLabel("Top Conceptos de Gasto (Egresos)");
        lblConceptosTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblConceptosTitle.setForeground(tm.textPrimary());
        cardConceptos.add(lblConceptosTitle, BorderLayout.NORTH);
        cardConceptos.add(conceptosPanel, BorderLayout.CENTER);
        add(cardConceptos, "cell 1 0, grow");

        // Card 3: Evolución Temporal Diaria (Derecha Inferior)
        RoundedPanel cardTimeline = new RoundedPanel(14, true);
        cardTimeline.setBackground(tm.cardBg());
        cardTimeline.setLayout(new BorderLayout());
        cardTimeline.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 3, 0, tm.orangeAccent()),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        JLabel lblTimelineTitle = new JLabel("Evolución Diaria de Pagos (Período)");
        lblTimelineTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTimelineTitle.setForeground(tm.textPrimary());
        cardTimeline.add(lblTimelineTitle, BorderLayout.NORTH);
        cardTimeline.add(timelinePanel, BorderLayout.CENTER);
        add(cardTimeline, "cell 1 1, grow");
    }

    public void updateData(ReporteEjecutivoData data) {
        donutPanel.setData(data);
        conceptosPanel.setData(data);
        timelinePanel.setData(data);
        revalidate();
        repaint();
    }

    // =========================================================================
    // 1. DONUT CHART (POR BANCO)
    // =========================================================================
    private class DonutChartPanel extends JPanel {
        private ReporteEjecutivoData data;

        public DonutChartPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(280, 240));
        }

        public void setData(ReporteEjecutivoData data) {
            this.data = data;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            if (data == null || data.porBancoBs().isEmpty() || data.totalBs() <= 0) {
                g2.setColor(tm.textSecondary());
                g2.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                String msg = "Sin transacciones en el período seleccionado";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
                g2.dispose();
                return;
            }

            int chartSize = Math.min(w, h - 80) - 20;
            if (chartSize < 80) chartSize = 80;
            int x = (w - chartSize) / 2;
            int y = 10;

            double startAngle = 90;
            int colorIdx = 0;
            Map<String, Double> bancos = data.porBancoBs();

            // Dibujar segmentos
            for (Map.Entry<String, Double> entry : bancos.entrySet()) {
                double sliceAngle = (entry.getValue() / data.totalBs()) * 360.0;
                Color col = PALETTE[colorIdx % PALETTE.length];
                g2.setColor(col);
                g2.fill(new Arc2D.Double(x, y, chartSize, chartSize, startAngle, sliceAngle, Arc2D.PIE));
                startAngle += sliceAngle;
                colorIdx++;
            }

            // Agujero central (Donut)
            int holeSize = (int) (chartSize * 0.58);
            int hx = x + (chartSize - holeSize) / 2;
            int hy = y + (chartSize - holeSize) / 2;
            g2.setColor(tm.cardBg());
            g2.fillOval(hx, hy, holeSize, holeSize);

            // Texto central en el agujero
            g2.setColor(tm.textPrimary());
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            String lblTotal = CURRENCY_USD.format(data.totalUSD());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(lblTotal, hx + (holeSize - fm.stringWidth(lblTotal)) / 2, hy + holeSize / 2 - 2);

            g2.setColor(tm.textSecondary());
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            String sub = "Total Egresos";
            fm = g2.getFontMetrics();
            g2.drawString(sub, hx + (holeSize - fm.stringWidth(sub)) / 2, hy + holeSize / 2 + 14);

            // Leyenda inferior
            int legendY = y + chartSize + 18;
            colorIdx = 0;
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            for (Map.Entry<String, Double> entry : bancos.entrySet()) {
                if (legendY + 14 > h) break;
                Color col = PALETTE[colorIdx % PALETTE.length];
                g2.setColor(col);
                g2.fillOval(20, legendY - 9, 10, 10);

                g2.setColor(tm.textPrimary());
                double pct = (entry.getValue() / data.totalBs()) * 100.0;
                String leg = entry.getKey() + " (" + String.format(Locale.US, "%.1f%%", pct) + ") - " + CURRENCY_USD.format(data.porBancoUSD().getOrDefault(entry.getKey(), 0.0));
                g2.drawString(leg, 36, legendY);
                legendY += 18;
                colorIdx++;
            }

            g2.dispose();
        }
    }

    // =========================================================================
    // 2. HORIZONTAL BARS (TOP CONCEPTOS DE GASTO)
    // =========================================================================
    private class ConceptosBarPanel extends JPanel {
        private ReporteEjecutivoData data;

        public ConceptosBarPanel() {
            setOpaque(false);
        }

        public void setData(ReporteEjecutivoData data) {
            this.data = data;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            if (data == null || data.porConceptoBs().isEmpty() || data.totalBs() <= 0) {
                g2.setColor(tm.textSecondary());
                g2.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                String msg = "Sin conceptos registrados";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
                g2.dispose();
                return;
            }

            // Tomar Top 5 conceptos ordenados por monto descendente
            List<Map.Entry<String, Double>> top = new ArrayList<>(data.porConceptoBs().entrySet());
            top.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
            int maxItems = Math.min(top.size(), 5);

            double maxVal = top.get(0).getValue();
            if (maxVal <= 0) maxVal = 1.0;

            int startY = 16;
            int rowHeight = (h - 24) / Math.max(maxItems, 1);
            if (rowHeight > 36) rowHeight = 36;

            for (int i = 0; i < maxItems; i++) {
                Map.Entry<String, Double> e = top.get(i);
                int y = startY + i * rowHeight;

                // Título concepto y monto
                g2.setColor(tm.textPrimary());
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                String nombre = e.getKey().length() > 32 ? e.getKey().substring(0, 30) + "..." : e.getKey();
                g2.drawString(nombre, 10, y + 10);

                double valUSD = data.porConceptoUSD().getOrDefault(e.getKey(), 0.0);
                String valStr = CURRENCY_USD.format(valUSD);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(valStr, w - fm.stringWidth(valStr) - 10, y + 10);

                // Barra de fondo y barra activa
                int barY = y + 15;
                int barH = 7;
                int barMaxW = w - 20;

                g2.setColor(tm.isDark() ? new Color(45, 52, 68) : new Color(226, 232, 240));
                g2.fill(new RoundRectangle2D.Double(10, barY, barMaxW, barH, barH, barH));

                int barFillW = (int) ((e.getValue() / maxVal) * barMaxW);
                if (barFillW < 6) barFillW = 6;
                Color barColor = PALETTE[i % PALETTE.length];
                g2.setColor(barColor);
                g2.fill(new RoundRectangle2D.Double(10, barY, barFillW, barH, barH, barH));
            }

            g2.dispose();
        }
    }

    // =========================================================================
    // 3. TIMELINE BAR PANEL (EVOLUCIÓN DIARIA)
    // =========================================================================
    private class TimelineBarPanel extends JPanel {
        private ReporteEjecutivoData data;

        public TimelineBarPanel() {
            setOpaque(false);
        }

        public void setData(ReporteEjecutivoData data) {
            this.data = data;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            if (data == null || data.porDiaBs().isEmpty()) {
                g2.setColor(tm.textSecondary());
                g2.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                String msg = "Sin datos diarios en este período";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
                g2.dispose();
                return;
            }

            Map<LocalDate, Double> dias = data.porDiaUSD();
            int n = dias.size();
            double maxUSD = dias.values().stream().max(Double::compare).orElse(1.0);
            if (maxUSD <= 0) maxUSD = 1.0;

            int paddingLeft = 14;
            int paddingRight = 14;
            int paddingBottom = 22;
            int paddingTop = 12;

            int usableW = w - paddingLeft - paddingRight;
            int usableH = h - paddingTop - paddingBottom;

            int barWidth = Math.max(8, (usableW / Math.max(n, 1)) - 6);
            if (barWidth > 32) barWidth = 32;

            int idx = 0;
            for (Map.Entry<LocalDate, Double> entry : dias.entrySet()) {
                int cx = paddingLeft + (int) ((idx + 0.5) * (usableW / (double) n));
                int bx = cx - barWidth / 2;

                int barH = (int) ((entry.getValue() / maxUSD) * usableH);
                if (barH < 4 && entry.getValue() > 0) barH = 4;
                int by = paddingTop + usableH - barH;

                // Barra
                g2.setColor(new Color(16, 185, 129)); // Verde esmeralda
                g2.fill(new RoundRectangle2D.Double(bx, by, barWidth, barH, 6, 6));

                // Etiqueta de fecha (cada N barras según densidad)
                int step = n > 15 ? 3 : (n > 8 ? 2 : 1);
                if (idx % step == 0 || idx == n - 1) {
                    g2.setColor(tm.textSecondary());
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                    String lblDate = entry.getKey().format(DATE_FMT);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(lblDate, cx - fm.stringWidth(lblDate) / 2, h - 5);
                }

                idx++;
            }

            g2.dispose();
        }
    }
}
