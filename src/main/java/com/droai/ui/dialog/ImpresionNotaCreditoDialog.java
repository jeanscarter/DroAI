package com.droai.ui.dialog;

import com.droai.dao.NotaCreditoDAO;
import com.droai.model.NotaCreditoModel;
import com.droai.model.SesionUsuario;
import com.droai.ui.ThemeManager;
import com.droai.ui.components.Toast;
import com.droai.ui.print.NotaCreditoPrintRenderer;
import com.droai.ui.util.IconHelper;
import net.miginfocom.swing.MigLayout;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.*;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterJob;

/**
 * Diálogo modal para vista previa e impresión física de la Nota de Crédito.
 */
public class ImpresionNotaCreditoDialog extends JDialog {

    private final NotaCreditoModel model;
    private final NotaCreditoDAO dao;
    private final ThemeManager tm = ThemeManager.get();
    private boolean enDolares = false;
    private final JPanel previewCanvas;
    private final JCheckBox chkEnDolares;
    private final Runnable onImpresoCallback;

    private double zoom = 1.0;
    private boolean isMaximized = true;
    private Rectangle normalBounds;
    private final JButton btnPantallaCompleta;
    private final JLabel lblZoom;
    private final JScrollPane scrollPane;

    public ImpresionNotaCreditoDialog(Window owner, NotaCreditoModel model, Runnable onImpresoCallback) {
        super(owner, "Impresión de Nota de Crédito — " + model.getNroDoc(), ModalityType.APPLICATION_MODAL);
        this.model = model;
        this.dao = new NotaCreditoDAO();
        this.onImpresoCallback = onImpresoCallback;

        IconHelper.applyAppIcon(this);
        setMinimumSize(new Dimension(880, 650));

        // Dimensiones iniciales maximizadas (pantalla completa dentro del área de trabajo)
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle maxBounds = env.getMaximumWindowBounds();
        normalBounds = new Rectangle(
                maxBounds.x + Math.max(0, (maxBounds.width - 980) / 2),
                maxBounds.y + Math.max(0, (maxBounds.height - 800) / 2),
                Math.min(980, maxBounds.width),
                Math.min(800, maxBounds.height)
        );
        setBounds(maxBounds);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(tm.background());

        // ═══════════════════════════════════════════════════════════
        // CANVAS DE VISTA PREVIA (HOJA BLANCA HORIZONTAL CON ZOOM)
        // ═══════════════════════════════════════════════════════════
        final int basePageWidth = 792;  // Ancho Carta Horizontal en puntos (11 pulgadas)
        final int basePageHeight = 612; // Alto Carta Horizontal en puntos (8.5 pulgadas)

        previewCanvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();

                int scaledW = (int) Math.round(basePageWidth * zoom);
                int scaledH = (int) Math.round(basePageHeight * zoom);
                int canvasW = getWidth();
                int canvasH = getHeight();

                int originX = Math.max(25, (canvasW - scaledW) / 2);
                int originY = Math.max(20, (canvasH - scaledH) / 2);

                // Sombra de la hoja de papel
                g2.setColor(new Color(0, 0, 0, 50));
                g2.fillRoundRect(originX + 4, originY + 4, scaledW, scaledH, 8, 8);

                // Hoja blanca
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(originX, originY, scaledW, scaledH, 6, 6);

                // Borde de la hoja
                g2.setColor(new Color(200, 200, 200));
                g2.drawRoundRect(originX, originY, scaledW, scaledH, 6, 6);

                // Renderizar con escala
                Graphics2D gDoc = (Graphics2D) g2.create(originX, originY, scaledW, scaledH);
                gDoc.scale(zoom, zoom);
                NotaCreditoPrintRenderer renderer = new NotaCreditoPrintRenderer(model, enDolares);
                renderer.paintDocument(gDoc, basePageWidth, basePageHeight);
                gDoc.dispose();

                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                int scaledW = (int) Math.round(basePageWidth * zoom) + 60;
                int scaledH = (int) Math.round(basePageHeight * zoom) + 50;
                return new Dimension(scaledW, scaledH);
            }
        };
        previewCanvas.setBackground(new Color(40, 44, 52));

        // ═══════════════════════════════════════════════════════════
        // TOP TOOLBAR
        // ═══════════════════════════════════════════════════════════
        JPanel toolbar = new JPanel(new MigLayout("insets 10 20 10 20, fillx", "[]16[]push[]8[]8[]12[]12[]12[]", "[]"));
        toolbar.setBackground(tm.cardBg());
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, tm.border()));

        JLabel lblTitle = new JLabel("🖨️ Vista Previa de Impresión — N/CR Nº " + model.getNumeroImpresion());
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(tm.textPrimary());
        toolbar.add(lblTitle);

        chkEnDolares = new JCheckBox("Imprimir en USD ($)");
        chkEnDolares.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chkEnDolares.setForeground(tm.textPrimary());
        chkEnDolares.setOpaque(false);
        chkEnDolares.addActionListener(e -> {
            enDolares = chkEnDolares.isSelected();
            previewCanvas.repaint();
        });
        toolbar.add(chkEnDolares);

        // Controles de Zoom
        JButton btnZoomOut = new JButton("−");
        btnZoomOut.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnZoomOut.setBackground(tm.cardBg());
        btnZoomOut.setForeground(tm.textPrimary());
        btnZoomOut.setToolTipText("Reducir zoom");
        btnZoomOut.setFocusPainted(false);
        btnZoomOut.addActionListener(e -> setZoom(zoom - 0.15));
        toolbar.add(btnZoomOut);

        lblZoom = new JLabel("100%");
        lblZoom.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblZoom.setForeground(tm.textPrimary());
        toolbar.add(lblZoom);

        JButton btnZoomIn = new JButton("+");
        btnZoomIn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnZoomIn.setBackground(tm.cardBg());
        btnZoomIn.setForeground(tm.textPrimary());
        btnZoomIn.setToolTipText("Aumentar zoom");
        btnZoomIn.setFocusPainted(false);
        btnZoomIn.addActionListener(e -> setZoom(zoom + 0.15));
        toolbar.add(btnZoomIn);

        JButton btnAjustar = new JButton("⤢ Ajustar");
        btnAjustar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnAjustar.setBackground(tm.cardBg());
        btnAjustar.setForeground(tm.textPrimary());
        btnAjustar.setToolTipText("Ajustar al alto de la ventana");
        btnAjustar.setFocusPainted(false);
        btnAjustar.addActionListener(e -> autoAjustarZoom());
        toolbar.add(btnAjustar);

        // Botón Pantalla Completa / Restaurar
        btnPantallaCompleta = new JButton("🗗 Restaurar");
        btnPantallaCompleta.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnPantallaCompleta.setBackground(tm.cardBg());
        btnPantallaCompleta.setForeground(tm.textPrimary());
        btnPantallaCompleta.setToolTipText("Alternar pantalla completa (F11)");
        btnPantallaCompleta.setFocusPainted(false);
        btnPantallaCompleta.addActionListener(e -> togglePantallaCompleta());
        toolbar.add(btnPantallaCompleta);

        JButton btnImprimir = new JButton("🖨️ Imprimir");
        btnImprimir.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnImprimir.setBackground(tm.accent());
        btnImprimir.setForeground(tm.btnForegroundFor(tm.accent()));
        btnImprimir.setFocusPainted(false);
        btnImprimir.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnImprimir.addActionListener(e -> ejecutarImpresion());
        toolbar.add(btnImprimir);

        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnCerrar.setBackground(tm.cardBg());
        btnCerrar.setForeground(tm.textPrimary());
        btnCerrar.addActionListener(e -> dispose());
        toolbar.add(btnCerrar);

        root.add(toolbar, BorderLayout.NORTH);

        scrollPane = new JScrollPane(previewCanvas);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(24);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(24);
        root.add(scrollPane, BorderLayout.CENTER);

        // Atajos de teclado: F11 (Pantalla completa) y ESC (Cerrar)
        root.registerKeyboardAction(e -> togglePantallaCompleta(),
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F11, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        root.registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        setContentPane(root);

        // Al abrirse, autoajustar zoom a la ventana
        SwingUtilities.invokeLater(this::autoAjustarZoom);
    }

    private void togglePantallaCompleta() {
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle maxBounds = env.getMaximumWindowBounds();
        if (isMaximized) {
            if (normalBounds != null) {
                setBounds(normalBounds);
            } else {
                setSize(980, 800);
                setLocationRelativeTo(getOwner());
            }
            isMaximized = false;
            btnPantallaCompleta.setText("🗖 Pantalla Completa");
        } else {
            normalBounds = getBounds();
            setBounds(maxBounds);
            isMaximized = true;
            btnPantallaCompleta.setText("🗗 Restaurar");
        }
        SwingUtilities.invokeLater(this::autoAjustarZoom);
    }

    private void setZoom(double newZoom) {
        this.zoom = Math.max(0.4, Math.min(2.5, newZoom));
        lblZoom.setText(Math.round(this.zoom * 100) + "%");
        previewCanvas.revalidate();
        previewCanvas.repaint();
    }

    private void autoAjustarZoom() {
        if (scrollPane != null && scrollPane.getViewport() != null) {
            int viewW = scrollPane.getViewport().getWidth();
            int viewH = scrollPane.getViewport().getHeight();
            if (viewW > 120 && viewH > 120) {
                double zoomW = (double) (viewW - 60) / 792.0;
                double zoomH = (double) (viewH - 50) / 612.0;
                double fitZoom = Math.min(zoomW, zoomH);
                setZoom(Math.max(0.6, fitZoom));
                return;
            }
        }
        setZoom(1.0);
    }

    private void ejecutarImpresion() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Nota_Credito_" + model.getNumeroImpresion());

        PageFormat pf = job.defaultPage();
        Paper paper = new Paper();
        // Carta Horizontal estándar (Landscape): 11 x 8.5 pulgadas = 792 x 612 puntos
        paper.setSize(792, 612);
        paper.setImageableArea(0, 0, 792, 612);
        pf.setPaper(paper);
        pf.setOrientation(PageFormat.LANDSCAPE);

        NotaCreditoPrintRenderer printable = new NotaCreditoPrintRenderer(model, enDolares);
        job.setPrintable(printable, pf);

        PrintRequestAttributeSet attrSet = new HashPrintRequestAttributeSet();
        attrSet.add(OrientationRequested.LANDSCAPE);
        attrSet.add(MediaSizeName.NA_LETTER);

        if (job.printDialog(attrSet)) {
            try {
                job.print(attrSet);
                Toast.show("✔ Documento enviado a la impresora", Toast.Type.SUCCESS);

                // Marcar como impresa en BD
                String usuarioActual = SesionUsuario.isAutenticado() ? SesionUsuario.current().getCoUsuario() : "DROAI";
                boolean ok = dao.marcarComoImpresa(model.getNroDoc(), model.getCoTipoDoc(), usuarioActual);
                if (ok) {
                    model.setImpresa(true);
                    if (onImpresoCallback != null) {
                        onImpresoCallback.run();
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error al imprimir: " + ex.getMessage(),
                        "Error de Impresión", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
