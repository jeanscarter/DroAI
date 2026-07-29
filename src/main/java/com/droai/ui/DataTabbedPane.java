package com.droai.ui;

import com.droai.model.ArticuloRow;
import com.droai.model.DescuentoVolumenRow;
import com.droai.ui.table.CatalogoTableModel;
import com.droai.ui.table.DescuentoProductoTableModel;
import com.droai.ui.table.ResumenTableModel;
import com.formdev.flatlaf.FlatClientProperties;
import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.droai.ui.table.DescuentoVolumenTableModel;
import net.miginfocom.swing.MigLayout;

public class DataTabbedPane extends JTabbedPane {

    private final JTable tblCatalogo;
    private final JTable tblSimulador;
    private final JTable tblDctoVolumen;
    private final JTable tblDctoProducto;
    private final ImportarPanel importarPanel;
    private final CargaMasivaCostosPreciosPanel cargaMasivaPanel;

    private final CatalogoTableModel catalogoModel;
    private final ResumenTableModel simuladorModel;
    private final DescuentoVolumenTableModel dctoVolumenModel;
    private final DescuentoProductoTableModel dctoProductoModel;

    // Controles para panel de acciones masivas de Descuentos x Volumen
    private final JTextField txtDVPorcentaje;
    private final DatePicker dateDVFechaIni;
    private final DatePicker dateDVFechaFin;
    private final JButton btnDVAplicar;
    private final JButton btnDVSelectAll;
    private final JButton btnDVUnselectAll;
    private final JButton btnDVImportExcel;

    // Controles para panel de acciones masivas de Descuento x Producto
    private final JTextField txtDPDcto;
    private final JButton btnDPAplicar;
    private final JButton btnDPSelectAll;
    private final JButton btnDPUnselectAll;
    private final JButton btnDPImportExcel;

    public DataTabbedPane() {
        setFont(new Font("Segoe UI", Font.BOLD, 13));
        putClientProperty(FlatClientProperties.TABBED_PANE_TAB_TYPE, "underlined");

        catalogoModel = new CatalogoTableModel();
        tblCatalogo = createStyledTable(catalogoModel);
        addTab("  Catálogo de Productos  ", wrapTable(tblCatalogo));

        simuladorModel = new ResumenTableModel();
        tblSimulador = createStyledTable(simuladorModel);
        addTab("  Descuentos Adicional  ", wrapTable(tblSimulador));

        // Pestaña Descuentos x Volumen con panel interactivo
        dctoVolumenModel = new DescuentoVolumenTableModel();
        tblDctoVolumen = createStyledTable(dctoVolumenModel);
        tblDctoVolumen.setAutoCreateRowSorter(true);
        // Ajustar ancho de la columna de selección
        tblDctoVolumen.getColumnModel().getColumn(0).setMaxWidth(80);
        tblDctoVolumen.getColumnModel().getColumn(0).setMinWidth(80);
        tblDctoVolumen.getColumnModel().getColumn(0).setPreferredWidth(80);

        JPanel pnlDctoVolumen = new JPanel(new MigLayout("insets 0, fill, wrap", "[grow]", "[grow]0[]"));
        pnlDctoVolumen.add(wrapTable(tblDctoVolumen), "grow");

        // Panel de acciones inferior
        JPanel pnlDVActions = new JPanel(new MigLayout("insets 8 16 8 16, fillx, gap 8", "[]8[]8[]push[]8[]8[]8[]8[]8[]", "[]"));
        pnlDVActions.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Component.borderColor")));

        pnlDVActions.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Component.borderColor")));

        btnDVSelectAll = new JButton("Seleccionar Todos");
        btnDVSelectAll.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnDVSelectAll.addActionListener(e -> dctoVolumenModel.selectAll(true));
        pnlDVActions.add(btnDVSelectAll);

        btnDVUnselectAll = new JButton("Deseleccionar Todos");
        btnDVUnselectAll.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnDVUnselectAll.addActionListener(e -> dctoVolumenModel.selectAll(false));
        pnlDVActions.add(btnDVUnselectAll);

        btnDVImportExcel = new JButton("Cargar Excel DV");
        btnDVImportExcel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        pnlDVActions.add(btnDVImportExcel);

        JLabel lblDVInfo = new JLabel("Aplicar DV (%):");
        lblDVInfo.setFont(new Font("Segoe UI", Font.BOLD, 11));
        pnlDVActions.add(lblDVInfo);

        txtDVPorcentaje = new JTextField("0.00");
        txtDVPorcentaje.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtDVPorcentaje.setHorizontalAlignment(SwingConstants.RIGHT);
        pnlDVActions.add(txtDVPorcentaje, "w 60!");

        JLabel lblDVFechaIni = new JLabel("Desde:");
        lblDVFechaIni.setFont(new Font("Segoe UI", Font.BOLD, 11));
        pnlDVActions.add(lblDVFechaIni);

        dateDVFechaIni = createDatePicker();
        pnlDVActions.add(dateDVFechaIni);

        JLabel lblDVFechaFin = new JLabel("Hasta:");
        lblDVFechaFin.setFont(new Font("Segoe UI", Font.BOLD, 11));
        pnlDVActions.add(lblDVFechaFin);

        dateDVFechaFin = createDatePicker();
        pnlDVActions.add(dateDVFechaFin);

        btnDVAplicar = new JButton("Aplicar a Seleccionados");
        btnDVAplicar.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnDVAplicar.setBackground(UIManager.getColor("Component.accentColor"));
        btnDVAplicar.setForeground(Color.WHITE);
        btnDVAplicar.setFocusPainted(false);
        btnDVAplicar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pnlDVActions.add(btnDVAplicar);

        pnlDctoVolumen.add(pnlDVActions, "growx");

        addTab("  Descuentos x Volumen  ", pnlDctoVolumen);

        // Pestaña Descuento x Producto con panel interactivo
        dctoProductoModel = new DescuentoProductoTableModel();
        tblDctoProducto = createStyledTable(dctoProductoModel);
        tblDctoProducto.setAutoCreateRowSorter(true);
        tblDctoProducto.getColumnModel().getColumn(0).setMaxWidth(80);
        tblDctoProducto.getColumnModel().getColumn(0).setMinWidth(80);
        tblDctoProducto.getColumnModel().getColumn(0).setPreferredWidth(80);

        JPanel pnlDctoProducto = new JPanel(new MigLayout("insets 0, fill, wrap", "[grow]", "[grow]0[]"));
        pnlDctoProducto.add(wrapTable(tblDctoProducto), "grow");

        // Panel de acciones inferior
        JPanel pnlDPActions = new JPanel(new MigLayout("insets 8 16 8 16, fillx, gap 10", "[]10[]10[]push[]10[]10[]", "[]"));
        pnlDPActions.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Component.borderColor")));

        btnDPSelectAll = new JButton("Seleccionar Todos");
        btnDPSelectAll.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnDPSelectAll.addActionListener(e -> dctoProductoModel.selectAll(true));
        pnlDPActions.add(btnDPSelectAll);

        btnDPUnselectAll = new JButton("Deseleccionar Todos");
        btnDPUnselectAll.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnDPUnselectAll.addActionListener(e -> dctoProductoModel.selectAll(false));
        pnlDPActions.add(btnDPUnselectAll);

        btnDPImportExcel = new JButton("Cargar Excel DP");
        btnDPImportExcel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        pnlDPActions.add(btnDPImportExcel);

        JLabel lblDPDcto = new JLabel("% Dcto (DP):");
        lblDPDcto.setFont(new Font("Segoe UI", Font.BOLD, 11));
        pnlDPActions.add(lblDPDcto);

        txtDPDcto = new JTextField("0.00");
        txtDPDcto.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtDPDcto.setHorizontalAlignment(SwingConstants.RIGHT);
        pnlDPActions.add(txtDPDcto, "w 80!");

        btnDPAplicar = new JButton("Aplicar a Seleccionados");
        btnDPAplicar.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnDPAplicar.setBackground(UIManager.getColor("Component.accentColor"));
        btnDPAplicar.setForeground(Color.WHITE);
        btnDPAplicar.setFocusPainted(false);
        btnDPAplicar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pnlDPActions.add(btnDPAplicar);

        pnlDctoProducto.add(pnlDPActions, "growx");

        addTab("  Descuento x Producto  ", pnlDctoProducto);

        cargaMasivaPanel = new CargaMasivaCostosPreciosPanel();
        addTab("  Carga Masiva Costos/Precios  ", cargaMasivaPanel);

        importarPanel = new ImportarPanel();
        addTab("  Importar Datos  ", importarPanel);
    }

    public CargaMasivaCostosPreciosPanel getCargaMasivaPanel() {
        return cargaMasivaPanel;
    }

    public CatalogoTableModel getCatalogoModel() {
        return catalogoModel;
    }

    public JTable getCatalogoTable() {
        return tblCatalogo;
    }

    public ImportarPanel getImportarPanel() {
        return importarPanel;
    }

    public ResumenTableModel getSimuladorModel() {
        return simuladorModel;
    }

    public DescuentoVolumenTableModel getDctoVolumenModel() {
        return dctoVolumenModel;
    }

    public JTable getTblDctoVolumen() {
        return tblDctoVolumen;
    }

    public List<DescuentoVolumenRow> getDctoVolumenRowsVisibles() {
        List<DescuentoVolumenRow> visibleRows = new ArrayList<>();
        List<DescuentoVolumenRow> filteredData = dctoVolumenModel.getFilteredData();
        for (int i = 0; i < tblDctoVolumen.getRowCount(); i++) {
            int modelRow = tblDctoVolumen.convertRowIndexToModel(i);
            if (modelRow >= 0 && modelRow < filteredData.size()) {
                visibleRows.add(filteredData.get(modelRow));
            }
        }
        return visibleRows;
    }

    public List<ArticuloRow> getCatalogoRowsVisibles() {
        List<ArticuloRow> visibleRows = new ArrayList<>();
        List<ArticuloRow> filteredData = catalogoModel.getFilteredData();
        for (int i = 0; i < tblCatalogo.getRowCount(); i++) {
            int modelRow = tblCatalogo.convertRowIndexToModel(i);
            if (modelRow >= 0 && modelRow < filteredData.size()) {
                visibleRows.add(filteredData.get(modelRow));
            }
        }
        return visibleRows;
    }

    public JTextField getTxtDVPorcentaje() {
        return txtDVPorcentaje;
    }

    public DatePicker getDateDVFechaIni() {
        return dateDVFechaIni;
    }

    public DatePicker getDateDVFechaFin() {
        return dateDVFechaFin;
    }

    public void updateDatePickerThemes(boolean isDark) {
        applyThemeToDatePicker(dateDVFechaIni, isDark);
        applyThemeToDatePicker(dateDVFechaFin, isDark);
    }

    private DatePicker createDatePicker() {
        DatePickerSettings settings = new DatePickerSettings(Locale.of("es", "VE"));
        settings.setFormatForDatesCommonEra("yyyy-MM-dd");
        settings.setAllowEmptyDates(true);
        settings.setFirstDayOfWeek(DayOfWeek.MONDAY);

        settings.setFontValidDate(new Font("Segoe UI", Font.PLAIN, 11));

        DatePicker picker = new DatePicker(settings);
        picker.setPreferredSize(new Dimension(145, 28));
        applyThemeToDatePicker(picker, true); // por defecto tema oscuro
        return picker;
    }

    public static void applyThemeToDatePicker(DatePicker picker, boolean isDark) {
        if (picker == null) return;
        DatePickerSettings settings = picker.getSettings();

        Color bgPanel = isDark ? new Color(0x1E, 0x23, 0x2E) : Color.WHITE;
        Color fgText  = isDark ? new Color(0xF8, 0xFA, 0xFC) : new Color(0x0F, 0x17, 0x2A);
        Color inputBg = isDark ? new Color(0x1E, 0x23, 0x2E) : Color.WHITE;
        Color inputFg = isDark ? new Color(0xF8, 0xFA, 0xFC) : new Color(0x0F, 0x17, 0x2A);
        Color accent  = isDark ? new Color(0x2A, 0x6B, 0xFF) : new Color(0x1D, 0x4E, 0xD8);

        // 1. Campo de texto
        settings.setColor(DatePickerSettings.DateArea.TextFieldBackgroundValidDate, inputBg);
        settings.setColor(DatePickerSettings.DateArea.DatePickerTextValidDate, inputFg);

        // 2. Fondos del panel desplegable de calendario
        settings.setColor(DatePickerSettings.DateArea.BackgroundOverallCalendarPanel, bgPanel);
        settings.setColor(DatePickerSettings.DateArea.BackgroundMonthAndYearMenuLabels, bgPanel);
        settings.setColor(DatePickerSettings.DateArea.BackgroundTodayLabel, bgPanel);
        settings.setColor(DatePickerSettings.DateArea.BackgroundClearLabel, bgPanel);
        settings.setColor(DatePickerSettings.DateArea.BackgroundMonthAndYearNavigationButtons, bgPanel);

        // 3. Textos y días
        settings.setColor(DatePickerSettings.DateArea.CalendarBackgroundNormalDates, bgPanel);
        settings.setColor(DatePickerSettings.DateArea.CalendarTextNormalDates, fgText);
        settings.setColor(DatePickerSettings.DateArea.CalendarTextWeekdays, fgText);

        // 4. Fecha seleccionada
        settings.setColor(DatePickerSettings.DateArea.CalendarBackgroundSelectedDate, accent);
    }

    public JButton getBtnDVAplicar() {
        return btnDVAplicar;
    }

    public JButton getBtnDVImportExcel() {
        return btnDVImportExcel;
    }

    public DescuentoProductoTableModel getDctoProductoModel() {
        return dctoProductoModel;
    }

    public JTable getTblDctoProducto() {
        return tblDctoProducto;
    }

    public JTextField getTxtDPDcto() {
        return txtDPDcto;
    }

    public JButton getBtnDPImportExcel() {
        return btnDPImportExcel;
    }

    public JButton getBtnDPAplicar() {
        return btnDPAplicar;
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