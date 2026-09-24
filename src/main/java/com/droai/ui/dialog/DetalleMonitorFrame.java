package com.droai.ui.dialog;

import com.droai.export.ExcelExporter;
import com.droai.model.FacturaResumenRow;
import com.droai.model.MatrizVentasRow;
import com.droai.service.MonitorService;
import com.droai.ui.components.Toast;
import com.droai.ui.table.FacturasTableModel;
import com.droai.ui.table.ProductosFacturadosTableModel;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.File;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Ventana independiente (JFrame) de Detalle del Monitor Situacional.
 *
 * <p>Permite expandir y maximizar la ventana para explorar la data con mayor comodidad.
 *
 * <p>Pestañas incluidas:
 * <ul>
 *   <li><b>Facturas</b>: Resumen por documento con totales acumulados</li>
 *   <li><b>Productos Facturados</b>: Detalle por renglón con las 40 columnas del Excel y soporte Bs/$</li>
 *   <li><b>Devoluciones / Remisiones</b>: Secciones de soporte</li>
 * </ul>
 */
public class DetalleMonitorFrame extends JFrame {

    // ── Paleta heredada del sistema ──
    private static final Color BG_DARK       = new Color(17, 21, 28);
    private static final Color CARD_BG       = new Color(30, 35, 46);
    private static final Color ACCENT        = new Color(42, 107, 255);
    private static final Color GREEN_ACCENT  = new Color(0, 210, 158);
    private static final Color TEXT_PRIMARY  = new Color(248, 250, 252);
    private static final Color TEXT_SECONDARY = new Color(148, 163, 184);
    private static final Color BORDER        = new Color(55, 62, 80);
    private static final Color TABLE_BG      = new Color(24, 28, 38);
    private static final Color TABLE_ALT     = new Color(28, 33, 44);
    private static final Color TABLE_HEADER  = new Color(34, 40, 54);

    private final FacturasTableModel facturasModel;
    private final ProductosFacturadosTableModel productosModel;

    private boolean esDolar;
    private JTextField txtTotalCantidadProductos;
    private JTextField txtTotalMontoProductos;

    private JTextField txtFacturasMontoBruto;
    private JTextField txtFacturasDescuento;
    private JTextField txtFacturasIva;
    private JTextField txtFacturasMontoNeto;
    private JTextField txtFacturasTotalGeneral;

    public DetalleMonitorFrame(List<MatrizVentasRow> rawRows) {
        this(rawRows, false);
    }

    public DetalleMonitorFrame(List<MatrizVentasRow> rawRows, boolean esDolar) {
        this.esDolar = esDolar;
        setTitle("Detalle — Monitor Situacional");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1350, 750);
        setMinimumSize(new Dimension(950, 550));
        setLocationRelativeTo(null);

        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/images/logo.png"));
            setIconImage(icon.getImage());
        } catch (Exception ignored) {}

        Toast.setParentFrame(this);

        facturasModel = new FacturasTableModel();
        productosModel = new ProductosFacturadosTableModel();

        facturasModel.setEsDolar(esDolar);
        productosModel.setEsDolar(esDolar);

        // Preparar datos
        MonitorService monitorService = new MonitorService();
        List<FacturaResumenRow> facturas = monitorService.agruparFacturas(rawRows);
        facturasModel.setData(facturas);
        productosModel.setData(rawRows);

        // ── Root ──
        JPanel root = new JPanel(new MigLayout("insets 0, fill", "[grow]", "[grow]"));
        root.setBackground(BG_DARK);

        // ── TabbedPane ──
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.setBackground(CARD_BG);
        tabs.setForeground(TEXT_PRIMARY);

        tabs.addTab("  Facturas  ", buildFacturasPanel());
        tabs.addTab("  Productos Facturados  ", buildProductosPanel());
        tabs.addTab("  Devoluciones  ", buildPlaceholderPanel("Devoluciones"));
        tabs.addTab("  Productos Devueltos  ", buildPlaceholderPanel("Productos Devueltos"));
        tabs.addTab("  Remisiones  ", buildPlaceholderPanel("Remisiones"));
        tabs.addTab("  Productos Remitidos  ", buildPlaceholderPanel("Productos Remitidos"));

        root.add(tabs, "grow");
        setContentPane(root);

        actualizarTotalesUI();
    }

    private JToggleButton createMonedaToggle(String text, boolean selected) {
        JToggleButton btn = new JToggleButton(text, selected);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(45, 28));
        btn.setBackground(CARD_BG);
        btn.setForeground(TEXT_SECONDARY);
        return btn;
    }

    private JPanel createMonedaToggleGroup() {
        JPanel monedaPanel = new JPanel(new MigLayout("insets 0, gap 0", "[]0[]", "[]"));
        monedaPanel.setOpaque(false);

        JToggleButton btnBs = createMonedaToggle("Bs", !esDolar);
        JToggleButton btnUsd = createMonedaToggle("$", esDolar);

        ButtonGroup bg = new ButtonGroup();
        bg.add(btnBs);
        bg.add(btnUsd);

        btnBs.addActionListener(e -> setMoneda(false));
        btnUsd.addActionListener(e -> setMoneda(true));

        monedaPanel.add(btnBs);
        monedaPanel.add(btnUsd);
        return monedaPanel;
    }

    private void setMoneda(boolean esDolar) {
        this.esDolar = esDolar;
        facturasModel.setEsDolar(esDolar);
        productosModel.setEsDolar(esDolar);
        actualizarTotalesUI();
    }

    private void actualizarTotalesUI() {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.of("es", "VE"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);

        if (txtTotalCantidadProductos != null) {
            txtTotalCantidadProductos.setText(nf.format(productosModel.getTotalCantidad()));
        }
        if (txtTotalMontoProductos != null) {
            txtTotalMontoProductos.setText(nf.format(productosModel.getTotalMonto()));
        }

        if (txtFacturasMontoBruto != null) {
            txtFacturasMontoBruto.setText(nf.format(facturasModel.getTotalMontoBruto()));
        }
        if (txtFacturasDescuento != null) {
            txtFacturasDescuento.setText(nf.format(facturasModel.getTotalDescuento()));
        }
        if (txtFacturasIva != null) {
            txtFacturasIva.setText(nf.format(facturasModel.getTotalIva()));
        }
        if (txtFacturasMontoNeto != null) {
            txtFacturasMontoNeto.setText(nf.format(facturasModel.getTotalMontoBruto() - facturasModel.getTotalDescuento()));
        }
        if (txtFacturasTotalGeneral != null) {
            txtFacturasTotalGeneral.setText(nf.format(facturasModel.getTotalGeneral()));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  PESTAÑA 1: Facturas
    // ═══════════════════════════════════════════════════════════════

    private JPanel buildFacturasPanel() {
        JPanel panel = new JPanel(new MigLayout("insets 8, fill", "[grow]", "[grow]0[]"));
        panel.setBackground(BG_DARK);

        JTable table = createStyledTable(facturasModel);
        configurarColumnasFacturas(table);
        JScrollPane scroll = wrapTable(table);
        panel.add(scroll, "grow, wrap");

        // ── Barra inferior con totales + toggle de moneda ──
        JPanel footer = new JPanel(new MigLayout(
                "insets 8 16 8 16, fillx, gap 12",
                "[]12[]push[]12[]12[]12[]12[]",
                "[]"));
        footer.setBackground(CARD_BG);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));

        JLabel lblRegistros = new JLabel(facturasModel.getRowCount() + " Registros");
        lblRegistros.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblRegistros.setForeground(TEXT_PRIMARY);
        footer.add(lblRegistros);

        footer.add(createMonedaToggleGroup());

        txtFacturasMontoBruto = createTotalBox("");
        txtFacturasDescuento = createTotalBox("");
        txtFacturasIva = createTotalBox("");
        txtFacturasMontoNeto = createTotalBox("");
        txtFacturasTotalGeneral = createTotalBox("");

        footer.add(txtFacturasMontoBruto);
        footer.add(txtFacturasDescuento);
        footer.add(txtFacturasIva);
        footer.add(txtFacturasMontoNeto);
        footer.add(txtFacturasTotalGeneral);

        panel.add(footer, "growx");
        return panel;
    }

    // ═══════════════════════════════════════════════════════════════
    //  PESTAÑA 2: Productos Facturados
    // ═══════════════════════════════════════════════════════════════

    private JPanel buildProductosPanel() {
        JPanel panel = new JPanel(new MigLayout("insets 8, fill", "[grow]", "[grow]0[]"));
        panel.setBackground(BG_DARK);

        JTable table = createStyledTable(productosModel);
        configurarColumnasProductos(table);
        JScrollPane scroll = wrapTable(table);
        panel.add(scroll, "grow, wrap");

        // ── Barra inferior con totales + Toggle Moneda + Exportar Excel ──
        JPanel footer = new JPanel(new MigLayout(
                "insets 8 16 8 16, fillx, gap 12",
                "[]12[]12[]push[]12[]",
                "[]"));
        footer.setBackground(CARD_BG);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));

        JLabel lblRegistros = new JLabel(productosModel.getRowCount() + " Registros");
        lblRegistros.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblRegistros.setForeground(TEXT_PRIMARY);
        footer.add(lblRegistros);

        footer.add(createMonedaToggleGroup());

        // Botón Exportar Excel
        JButton btnExportar = new JButton("📊  Exportar Excel");
        btnExportar.setFont(new Font("Segoe UI Emoji", Font.BOLD, 12));
        btnExportar.setBackground(GREEN_ACCENT);
        btnExportar.setForeground(new Color(17, 21, 28));
        btnExportar.setFocusPainted(false);
        btnExportar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnExportar.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        btnExportar.addActionListener(e -> exportarProductosExcel());
        footer.add(btnExportar);

        txtTotalCantidadProductos = createTotalBox("");
        txtTotalMontoProductos = createTotalBox("");

        footer.add(txtTotalCantidadProductos);
        footer.add(txtTotalMontoProductos);

        panel.add(footer, "growx");
        return panel;
    }

    // ═══════════════════════════════════════════════════════════════
    //  Placeholder tabs
    // ═══════════════════════════════════════════════════════════════

    private JPanel buildPlaceholderPanel(String nombre) {
        JPanel panel = new JPanel(new MigLayout("insets 0, fill, center", "[center]", "[center]"));
        panel.setBackground(BG_DARK);

        JLabel lbl = new JLabel("<html><center>" +
                "<span style='font-size:32px'>🔜</span><br><br>" +
                "<span style='font-size:14px; color:#F8FAFC'>" + nombre + "</span><br>" +
                "<span style='font-size:10px; color:#94A3B8'>Próxima Implementación</span>" +
                "</center></html>");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lbl.setForeground(TEXT_SECONDARY);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lbl, "center");

        return panel;
    }

    // ═══════════════════════════════════════════════════════════════
    //  Exportar a Excel
    // ═══════════════════════════════════════════════════════════════

    private void exportarProductosExcel() {
        List<MatrizVentasRow> rows = productosModel.getData();
        if (rows.isEmpty()) {
            Toast.show("No hay datos para exportar", Toast.Type.WARNING);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                ExcelExporter exporter = new ExcelExporter();
                File file = exporter.exportProductosFacturados(rows, esDolar);
                String monedaText = esDolar ? "$" : "Bs";
                Toast.show("Excel exportado en (" + monedaText + ") con 40 columnas: " + file.getName(), Toast.Type.SUCCESS);

                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file);
                }
            } catch (Exception ex) {
                Toast.show("Error al exportar: " + ex.getMessage(), Toast.Type.ERROR);
                ex.printStackTrace();
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════
    //  Helpers UI
    // ═══════════════════════════════════════════════════════════════

    private JTextField createTotalBox(String value) {
        JTextField txt = new JTextField(value);
        txt.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txt.setHorizontalAlignment(SwingConstants.RIGHT);
        txt.setEditable(false);
        txt.setBackground(TABLE_HEADER);
        txt.setForeground(TEXT_PRIMARY);
        txt.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        txt.setPreferredSize(new Dimension(130, 28));
        return txt;
    }

    private JTable createStyledTable(javax.swing.table.TableModel model) {
        JTable t = new JTable(model);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        t.setRowHeight(28);
        t.setGridColor(BORDER);
        t.setBackground(TABLE_BG);
        t.setForeground(TEXT_PRIMARY);
        t.setSelectionBackground(ACCENT.darker());
        t.setSelectionForeground(Color.WHITE);
        t.setShowHorizontalLines(true);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setFillsViewportHeight(true);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Header
        JTableHeader header = t.getTableHeader();
        header.setBackground(TABLE_HEADER);
        header.setForeground(TEXT_PRIMARY);
        header.setFont(new Font("Segoe UI", Font.BOLD, 11));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, ACCENT));
        header.setReorderingAllowed(false);

        // Renderers
        t.setDefaultRenderer(Double.class, new NumericCellRenderer());
        t.setDefaultRenderer(String.class, new StripedCellRenderer(SwingConstants.LEFT));
        t.setDefaultRenderer(Integer.class, new StripedCellRenderer(SwingConstants.CENTER));

        return t;
    }

    private void configurarColumnasFacturas(JTable t) {
        int[] widths = {110, 90, 100, 220, 100, 60, 80, 100, 80, 110, 60, 140};
        for (int i = 0; i < widths.length && i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    private void configurarColumnasProductos(JTable t) {
        int[] widths = {
            100, // 0: Numero
            90,  // 1: Fecha
            90,  // 2: CI / RIF
            200, // 3: Nombre o Razon Social
            70,  // 4: Cod.Ven.
            150, // 5: Nombre Vendedor
            70,  // 6: Tasa
            80,  // 7: Codigo Art.
            220, // 8: Descripcion
            70,  // 9: Cantidad
            90,  // 10: Precio
            60,  // 11: DP
            60,  // 12: DA
            60,  // 13: DCT
            60,  // 14: DC
            60,  // 15: DV
            100, // 16: Total Renglon
            80,  // 17: % Desc. Glob.
            100, // 18: Renglon DG
            90,  // 19: Monto I.V.A.
            70,  // 20: % I.V.A.
            100, // 21: Tot. Renglon IVA
            90,  // 22: Costo Venta
            100, // 23: Total Costo Venta
            90,  // 24: Tot. CV DP
            90,  // 25: Monto Utilidad
            70,  // 26: % Util.
            90,  // 27: Costo Actual
            80,  // 28: Stock Actual
            70,  // 29: Cod. Linea
            140, // 30: Linea
            70,  // 31: Cod. Sub.
            140, // 32: Sub-Linea
            70,  // 33: Cod. Prov.
            160, // 34: Nombre Proveedor
            80,  // 35: Zona
            70,  // 36: Almacen
            90,  // 37: Pedido Web
            80,  // 38: Origen
            100  // 39: Usuario Web
        };
        for (int i = 0; i < widths.length && i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    private JScrollPane wrapTable(JTable table) {
        JScrollPane sp = new JScrollPane(table,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        sp.getViewport().setBackground(TABLE_BG);
        return sp;
    }

    // ── Renderers ──

    private class NumericCellRenderer extends DefaultTableCellRenderer {
        private final NumberFormat nf = NumberFormat.getNumberInstance(Locale.of("es", "VE"));
        {
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof Number) {
                value = nf.format(((Number) value).doubleValue());
            }
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.RIGHT);
            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? TABLE_BG : TABLE_ALT);
                c.setForeground(TEXT_PRIMARY);
            }
            return c;
        }
    }

    private class StripedCellRenderer extends DefaultTableCellRenderer {
        private final int alignment;

        StripedCellRenderer(int alignment) {
            this.alignment = alignment;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(alignment);
            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? TABLE_BG : TABLE_ALT);
                c.setForeground(TEXT_PRIMARY);
            }
            return c;
        }
    }
}
