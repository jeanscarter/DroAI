package com.droai.ui;

import com.droai.ui.table.ListadoTableModel;
import com.droai.ui.table.ResumenTableModel;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * JTabbedPane estilizado con 4 pestañas:
 * 1. Listado de Productos (editable)
 * 2. Simulador
 * 3. Descuentos x Volumen (read-only)
 * 4. Descuento x Producto (read-only)
 */
public class DataTabbedPane extends JTabbedPane {

    private final JTable tblListado;
    private final JTable tblSimulador;
    private final JTable tblDctoVolumen;
    private final JTable tblDctoProducto;

    private final ListadoTableModel listadoModel;
    private final ResumenTableModel simuladorModel;
    private final ResumenTableModel dctoVolumenModel;
    private final ResumenTableModel dctoProductoModel;

    public DataTabbedPane() {
        setFont(new Font("Segoe UI", Font.BOLD, 13));
        setBackground(new Color(30, 33, 42));
        putClientProperty(FlatClientProperties.TABBED_PANE_TAB_TYPE, "underlined");

        // ----- Tab 1: Listado de Productos -----
        listadoModel = new ListadoTableModel();
        tblListado = createStyledTable(listadoModel);
        addTab("  Listado de Productos  ", wrapTable(tblListado));

        // ----- Tab 2: Simulador -----
        simuladorModel = new ResumenTableModel();
        tblSimulador = createStyledTable(simuladorModel);
        addTab("  Simulador  ", wrapTable(tblSimulador));

        // ----- Tab 3: Descuentos x Volumen -----
        dctoVolumenModel = new ResumenTableModel();
        tblDctoVolumen = createStyledTable(dctoVolumenModel);
        addTab("  Descuentos x Volumen  ", wrapTable(tblDctoVolumen));

        // ----- Tab 4: Descuento x Producto -----
        dctoProductoModel = new ResumenTableModel();
        tblDctoProducto = createStyledTable(dctoProductoModel);
        addTab("  Descuento x Producto  ", wrapTable(tblDctoProducto));
    }

    // ========== Public API ==========

    public ListadoTableModel getListadoModel()         { return listadoModel; }
    public ResumenTableModel getSimuladorModel()        { return simuladorModel; }
    public ResumenTableModel getDctoVolumenModel()      { return dctoVolumenModel; }
    public ResumenTableModel getDctoProductoModel()     { return dctoProductoModel; }

    // ========== Styling ==========

    private JTable createStyledTable(javax.swing.table.TableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setForeground(new Color(220, 225, 235));
        table.setBackground(new Color(30, 33, 42));
        table.setSelectionBackground(new Color(50, 80, 140));
        table.setSelectionForeground(Color.WHITE);
        table.setGridColor(new Color(45, 50, 62));
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Alternating row colors
        table.setDefaultRenderer(Object.class, new AlternatingRenderer());
        table.setDefaultRenderer(Double.class, new AlternatingDoubleRenderer());
        table.setDefaultRenderer(String.class, new AlternatingRenderer());

        // Header styling
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 11));
        header.setBackground(new Color(35, 38, 48));
        header.setForeground(new Color(170, 180, 200));
        header.setPreferredSize(new Dimension(0, 34));
        header.setReorderingAllowed(false);

        // Column widths for Listado
        if (model instanceof ListadoTableModel) {
            int[] widths = {80, 280, 100, 80, 50, 100, 80, 80, 70, 80, 60, 100};
            for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
                table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            }
        }

        return table;
    }

    private JScrollPane wrapTable(JTable table) {
        JScrollPane sp = new JScrollPane(table,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.getViewport().setBackground(new Color(30, 33, 42));
        sp.setBorder(BorderFactory.createEmptyBorder());
        return sp;
    }

    // ========== Custom Renderers ==========

    private static class AlternatingRenderer extends DefaultTableCellRenderer {
        private static final Color EVEN  = new Color(30, 33, 42);
        private static final Color ODD   = new Color(35, 38, 48);
        private static final Color SEL   = new Color(50, 80, 140);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int col) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, col);
            setBackground(selected ? SEL : (row % 2 == 0 ? EVEN : ODD));
            setForeground(selected ? Color.WHITE : new Color(220, 225, 235));
            setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
            return this;
        }
    }

    private static class AlternatingDoubleRenderer extends DefaultTableCellRenderer {
        private static final Color EVEN = new Color(30, 33, 42);
        private static final Color ODD  = new Color(35, 38, 48);
        private static final Color SEL  = new Color(50, 80, 140);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int col) {
            if (value instanceof Number n) {
                value = String.format("%.2f", n.doubleValue());
            }
            super.getTableCellRendererComponent(table, value, selected, focused, row, col);
            setHorizontalAlignment(SwingConstants.RIGHT);
            setBackground(selected ? SEL : (row % 2 == 0 ? EVEN : ODD));
            setForeground(selected ? Color.WHITE : new Color(220, 225, 235));
            setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
            return this;
        }
    }
}
