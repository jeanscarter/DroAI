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

    public ImpresionNotaCreditoDialog(Window owner, NotaCreditoModel model, Runnable onImpresoCallback) {
        super(owner, "Impresión de Nota de Crédito — " + model.getNroDoc(), ModalityType.APPLICATION_MODAL);
        this.model = model;
        this.dao = new NotaCreditoDAO();
        this.onImpresoCallback = onImpresoCallback;

        IconHelper.applyAppIcon(this);
        setSize(960, 780);
        setMinimumSize(new Dimension(880, 650));
        setLocationRelativeTo(owner);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(tm.background());

        // ═══════════════════════════════════════════════════════════
        // CANVAS DE VISTA PREVIA (HOJA BLANCA HORIZONTAL)
        // ═══════════════════════════════════════════════════════════
        previewCanvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();

                // Centrar la hoja simulada en el viewport (Horizontal / Landscape: 792 x 612 pt)
                int pageWidth = 792;  // Ancho Letter en puntos
                int pageHeight = 612; // Alto Letter en puntos
                int canvasW = getWidth();

                int originX = Math.max(20, (canvasW - pageWidth) / 2);
                int originY = 20;

                // Sombra de la hoja de papel
                g2.setColor(new Color(0, 0, 0, 45));
                g2.fillRoundRect(originX + 4, originY + 4, pageWidth, pageHeight, 8, 8);

                // Hoja blanca
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(originX, originY, pageWidth, pageHeight, 6, 6);

                // Borde de la hoja
                g2.setColor(new Color(200, 200, 200));
                g2.drawRoundRect(originX, originY, pageWidth, pageHeight, 6, 6);

                // Renderizar el contenido oficial
                Graphics2D gDoc = (Graphics2D) g2.create(originX, originY, pageWidth, pageHeight);
                NotaCreditoPrintRenderer renderer = new NotaCreditoPrintRenderer(model, enDolares);
                renderer.paintDocument(gDoc, pageWidth, pageHeight);
                gDoc.dispose();

                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(860, 670);
            }
        };
        previewCanvas.setBackground(new Color(45, 50, 60));

        // ═══════════════════════════════════════════════════════════
        // TOP TOOLBAR
        // ═══════════════════════════════════════════════════════════
        JPanel toolbar = new JPanel(new MigLayout("insets 12 20 12 20, fillx", "[]16[]push[]12[]", "[]"));
        toolbar.setBackground(tm.cardBg());
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, tm.border()));

        JLabel lblTitle = new JLabel("🖨️ Vista Previa de Impresión (Horizontal) — N/CR Nº " + model.getNumeroImpresion());
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

        JButton btnImprimir = new JButton("Imprimir Documento");
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

        JScrollPane scrollPane = new JScrollPane(previewCanvas);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        root.add(scrollPane, BorderLayout.CENTER);

        setContentPane(root);
    }

    private void ejecutarImpresion() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Nota_Credito_" + model.getNumeroImpresion());

        PageFormat pf = job.defaultPage();
        Paper paper = new Paper();
        // Carta / Letter estándar: 8.5 x 11 pulgadas = 612 x 792 puntos
        paper.setSize(612, 792);
        paper.setImageableArea(0, 0, 612, 792);
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
