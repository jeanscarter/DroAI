package com.droai.ui.dialog;

import com.droai.model.CapaCostoDTO;
import com.droai.model.InventarioValoradoRow;
import com.droai.ui.ThemeManager;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Diálogo modal para visualizar el desglose detallado de las capas FIFO (PEPS)
 * que conforman la existencia de un producto al corte.
 */
public class CapasCostoDialog extends JDialog {

    private final ThemeManager tm = ThemeManager.get();
    private static final DecimalFormat NF_DEC = new DecimalFormat("#,##0.00");
    private static final DecimalFormat NF_USD = new DecimalFormat("$#,##0.00");
    private static final DecimalFormat NF_BS = new DecimalFormat("'Bs.' #,##0.00");

    public CapasCostoDialog(Window parent, InventarioValoradoRow row, String fechaCorte) {
        super(parent, "Desglose de Capas FIFO — " + row.getCoArt(), ModalityType.APPLICATION_MODAL);
        setSize(980, 520);
        setMinimumSize(new Dimension(800, 400));
        setLocationRelativeTo(parent);

        buildUI(row, fechaCorte);
    }

    private void buildUI(InventarioValoradoRow row, String fechaCorte) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(tm.background());

        // ── Header ──
        JPanel header = new JPanel(new MigLayout("insets 16 20 12 20, fillx", "[grow][]", "[]4[]"));
        header.setBackground(tm.cardBg());
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, tm.border()));

        JLabel lblTitle = new JLabel("📦 Capas de Compra FIFO — " + row.getArtDes());
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(tm.textPrimary());
        header.add(lblTitle, "wrap");

        String info = String.format("<html><b>Código:</b> %s &nbsp;|&nbsp; <b>Almacén:</b> %s (%s) &nbsp;|&nbsp; <b>Existencia al corte (%s):</b> <span style='color:%s;'>%s</span> &nbsp;|&nbsp; <b>Tipo:</b> %s</html>",
                row.getCoArt(), row.getCoAlma(), row.getDesAlma(), fechaCorte,
                tm.isDark() ? "#38bdf8" : "#0284c7",
                NF_DEC.format(row.getExistencia()), row.getTipoMercancia());
        JLabel lblInfo = new JLabel(info);
        lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblInfo.setForeground(tm.textSecondary());
        header.add(lblInfo, "span 2");

        root.add(header, BorderLayout.NORTH);

        // ── Tabla de Capas ──
        String[] cols = {
                "Tipo Doc", "Nro. Documento", "Fecha Compra", "Cant. Comprada",
                "Cant. en Capa (Saldo)", "Costo Unit. ($)", "Costo Unit. (Bs.)",
                "Tasa Compra", "Subtotal ($)", "Subtotal (Bs.)", "Proveedor"
        };

        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        List<CapaCostoDTO> capas = row.getCapas();
        double sumCapasUnd = 0;
        double sumCapasUSD = 0;
        double sumCapasBS = 0;

        if (capas != null && !capas.isEmpty()) {
            for (CapaCostoDTO c : capas) {
                model.addRow(new Object[]{
                        c.getTipoDoc(),
                        c.getDocNum(),
                        c.getFecha(),
                        NF_DEC.format(c.getCantidadTotal()),
                        NF_DEC.format(c.getCantidadEnCapa()),
                        NF_USD.format(c.getCostUnitUSD()),
                        NF_BS.format(c.getCostUnitBS()),
                        NF_DEC.format(c.getTasaCambio()),
                        NF_USD.format(c.getSubtotalUSD()),
                        NF_BS.format(c.getSubtotalBS()),
                        c.getNomProv() != null ? c.getNomProv() : c.getCoProv()
                });
                sumCapasUnd += c.getCantidadEnCapa();
                sumCapasUSD += c.getSubtotalUSD();
                sumCapasBS += c.getSubtotalBS();
            }
        } else {
            // Caso consignación o sin capas registradas
            model.addRow(new Object[]{
                    "ÚLTIMO COSTO", "-", fechaCorte, NF_DEC.format(row.getExistencia()),
                    NF_DEC.format(row.getExistencia()), NF_USD.format(row.getCostoPromedioUSD()),
                    NF_BS.format(row.getCostoPromedioBS()), "-",
                    NF_USD.format(row.getTotalValorizadoUSD()), NF_BS.format(row.getTotalValorizadoBS()),
                    row.getNomProv()
            });
            sumCapasUnd = row.getExistencia();
            sumCapasUSD = row.getTotalValorizadoUSD();
            sumCapasBS = row.getTotalValorizadoBS();
        }

        JTable table = new JTable(model);
        table.setRowHeight(26);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setBackground(tm.tableBg());
        table.setForeground(tm.textPrimary());
        table.setSelectionBackground(tm.accent());
        table.setSelectionForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(tm.tableHeader());
        table.getTableHeader().setForeground(tm.textPrimary());

        DefaultTableCellRenderer centerRend = new DefaultTableCellRenderer();
        centerRend.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRend);
        table.getColumnModel().getColumn(1).setCellRenderer(centerRend);
        table.getColumnModel().getColumn(2).setCellRenderer(centerRend);

        DefaultTableCellRenderer rightRend = new DefaultTableCellRenderer();
        rightRend.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i = 3; i <= 9; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(rightRend);
        }

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        sp.getViewport().setBackground(tm.background());
        root.add(sp, BorderLayout.CENTER);

        // ── Footer con Totales ──
        JPanel footer = new JPanel(new MigLayout("insets 12 20 12 20, fillx", "[grow][]", "[]"));
        footer.setBackground(tm.cardBg());
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, tm.border()));

        double costoPromUSD = sumCapasUnd > 0 ? (sumCapasUSD / sumCapasUnd) : 0;
        double costoPromBS = sumCapasUnd > 0 ? (sumCapasBS / sumCapasUnd) : 0;

        String totHtml = String.format("<html><b>Unidades en Capas:</b> %s &nbsp;&nbsp;|&nbsp;&nbsp; <b>Costo Promedio Capas ($):</b> <span style='color:%s;'>%s</span> &nbsp;&nbsp;|&nbsp;&nbsp; <b>Costo Promedio (Bs.):</b> %s &nbsp;&nbsp;|&nbsp;&nbsp; <b>Total Valorado ($):</b> <span style='color:%s;'>%s</span></html>",
                NF_DEC.format(sumCapasUnd),
                tm.isDark() ? "#34d399" : "#059669", NF_USD.format(costoPromUSD),
                NF_BS.format(costoPromBS),
                tm.isDark() ? "#34d399" : "#059669", NF_USD.format(sumCapasUSD));
        JLabel lblTot = new JLabel(totHtml);
        lblTot.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblTot.setForeground(tm.textPrimary());
        footer.add(lblTot, "growx");

        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCerrar.setBackground(tm.btnNeutralBg());
        btnCerrar.setForeground(tm.textPrimary());
        btnCerrar.setFocusPainted(false);
        btnCerrar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCerrar.addActionListener(e -> dispose());
        footer.add(btnCerrar, "right");

        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }
}
