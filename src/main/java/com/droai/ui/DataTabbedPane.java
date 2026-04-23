package com.droai.ui;

import com.droai.ui.table.CatalogoTableModel;
import com.droai.ui.table.ResumenTableModel;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

public class DataTabbedPane extends JTabbedPane {

    private final JTable tblCatalogo;
    private final JTable tblSimulador;
    private final JTable tblDctoVolumen;
    private final JTable tblDctoProducto;

    private final CatalogoTableModel catalogoModel;
    private final ResumenTableModel simuladorModel;
    private final ResumenTableModel dctoVolumenModel;
    private final ResumenTableModel dctoProductoModel;

    public DataTabbedPane() {
        setFont(new Font("Segoe UI", Font.BOLD, 13));
        putClientProperty(FlatClientProperties.TABBED_PANE_TAB_TYPE, "underlined");

        catalogoModel = new CatalogoTableModel();
        tblCatalogo = createStyledTable(catalogoModel);
        addTab("  Catálogo de Productos  ", wrapTable(tblCatalogo));

        simuladorModel = new ResumenTableModel();
        tblSimulador = createStyledTable(simuladorModel);
        addTab("  Simulador  ", wrapTable(tblSimulador));

        dctoVolumenModel = new ResumenTableModel();
        tblDctoVolumen = createStyledTable(dctoVolumenModel);
        addTab("  Descuentos x Volumen  ", wrapTable(tblDctoVolumen));

        dctoProductoModel = new ResumenTableModel();
        tblDctoProducto = createStyledTable(dctoProductoModel);
        addTab("  Descuento x Producto  ", wrapTable(tblDctoProducto));
    }

    public CatalogoTableModel getCatalogoModel() {
        return catalogoModel;
    }

    public ResumenTableModel getSimuladorModel() {
        return simuladorModel;
    }

    public ResumenTableModel getDctoVolumenModel() {
        return dctoVolumenModel;
    }

    public ResumenTableModel getDctoProductoModel() {
        return dctoProductoModel;
    }

    private JTable createStyledTable(javax.swing.table.TableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        // Columnas se expanden dinámicamente para llenar todo el ancho
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // Renderer para Double: solo formateo y alineación, sin colores hardcoded
        table.setDefaultRenderer(Double.class, new DoubleRenderer());

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 11));
        header.setPreferredSize(new Dimension(0, 34));
        header.setReorderingAllowed(false);

        return table;
    }

    private JScrollPane wrapTable(JTable table) {
        JScrollPane sp = new JScrollPane(table,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setBorder(BorderFactory.createEmptyBorder());
        return sp;
    }

    /**
     * Renderer que solo formatea los Double a 2 decimales y alinea a la derecha.
     * Los colores (fondo, texto, selección, filas alternadas) son gestionados por FlatLaf.
     */
    private static class DoubleRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int col) {
            if (value instanceof Number n) {
                value = String.format("%.2f", n.doubleValue());
            }
            super.getTableCellRendererComponent(table, value, selected, focused, row, col);
            setHorizontalAlignment(SwingConstants.RIGHT);
            return this;
        }
    }
}