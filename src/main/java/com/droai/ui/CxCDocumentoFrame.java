package com.droai.ui;

import com.droai.export.ExcelExporter;
import com.droai.model.CxCDocumentoRow;
import com.droai.service.CxCDocumentoService;
import com.droai.service.CxCDocumentoService.ResumenAgrupado;
import com.droai.service.CxCDocumentoService.TotalesCxC;
import com.droai.ui.components.Toast;
import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Ventana Principal del Módulo de Estado de Cuentas por Cobrar (CxC) Multimoneda (USD).
 * Recrea el reporte de la pestaña '14-08-26' del Maestro de Cobranzas.
 */
public class CxCDocumentoFrame extends JFrame {

    private final ThemeManager tm = ThemeManager.get();
    private final CxCDocumentoService service;
    private final ExcelExporter excelExporter;

    private DatePicker dpFechaDesde;
    private DatePicker dpFechaHasta;
    private DatePicker dpFechaCorte;
    private JTextField txtBuscar;

    private JTabbedPane tabbedPane;

    // Tab 1: Detalle
    private JTable tableDetalle;
    private DefaultTableModel modelDetalle;
    private TableRowSorter<DefaultTableModel> sorterDetalle;

    // Tab 2: Vendedores
    private JTable tableVendedores;
    private DefaultTableModel modelVendedores;

    // Tab 3: Analistas
    private JTable tableAnalistas;
    private DefaultTableModel modelAnalistas;

    // Moneda Toggle ($ vs Bs)
    private boolean isBs = false;
    private JToggleButton btnUsd, btnBs;

    // KPI Labels & Titles
    private JLabel lblStatRegistros;
    private JLabel lblStatSaldo;
    private JLabel lblStatAlt;
    private JLabel lblStatPorVencer;
    private JLabel lblStatVencido;
    private JLabel lblStatPorcVencido;

    private JLabel lblTitleCardSaldo;
    private JLabel lblTitleCardAlt;
    private JLabel lblTitleCardPorVencer;
    private JLabel lblTitleCardVencido;

    private JLabel lblSub;

    private List<CxCDocumentoRow> currentData = new ArrayList<>();
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat CURRENCY_FMT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat TASA_FMT = new DecimalFormat("#,##0.0000");

    public CxCDocumentoFrame() {
        this.service = new CxCDocumentoService();
        this.excelExporter = new ExcelExporter();

        setTitle("DroAI — Estado de Cuentas por Cobrar (CxC Multimoneda)");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1450, 850);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);

        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/images/logo.png"));
            setIconImage(icon.getImage());
        } catch (Exception ignored) {}

        Toast.setParentFrame(this);

        Runnable themeListener = () -> {
            SwingUtilities.updateComponentTreeUI(this);
            repaint();
        };
        tm.addThemeChangeListener(themeListener);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) {
                tm.removeThemeChangeListener(themeListener);
            }
        });

        initUI();
        setDefaultDates();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(tm.background());

        // ═══════════════════════════════════════════════════════════
        //  HEADER
        // ═══════════════════════════════════════════════════════════
        JPanel headerPanel = new JPanel(new MigLayout("insets 16 24 12 24, fillx, wrap 2", "[grow][]", "[]8[]"));
        headerPanel.setBackground(tm.cardBg());
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, tm.border()));

        JPanel titleBox = new JPanel(new MigLayout("insets 0, wrap", "[]", "[]2[]"));
        titleBox.setOpaque(false);

        JLabel lblTitle = new JLabel("📋 Estado de Cuentas por Cobrar (CxC)");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(tm.textPrimary());
        titleBox.add(lblTitle);

        lblSub = new JLabel("Reporte Multimoneda ($ USD) — Condición: Sin Cancelar | Base de Datos: DROA_A");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(tm.textSecondary());
        titleBox.add(lblSub);

        headerPanel.add(titleBox, "growx");

        JPanel exportBox = new JPanel(new MigLayout("insets 0, gap 8", "[][]", "[]"));
        exportBox.setOpaque(false);

        JButton btnExportExcel = new JButton("📥 Exportar a Excel (EDC Maestro)");
        btnExportExcel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnExportExcel.setBackground(tm.greenAccent());
        btnExportExcel.setForeground(tm.btnForegroundFor(tm.greenAccent()));
        btnExportExcel.setFocusPainted(false);
        btnExportExcel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnExportExcel.addActionListener(e -> exportarExcel());
        exportBox.add(btnExportExcel);

        JButton btnTema = new JButton(tm.isDark() ? "☀" : "🌙");
        btnTema.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        btnTema.setFocusPainted(false);
        btnTema.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnTema.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        btnTema.setToolTipText("Cambiar tema claro/oscuro");
        btnTema.addActionListener(e -> tm.toggleTheme());
        exportBox.add(btnTema);

        headerPanel.add(exportBox, "alignx right");

        // ═══════════════════════════════════════════════════════════
        //  FILTERS BAR
        // ═══════════════════════════════════════════════════════════
        JPanel filterBar = new JPanel(new MigLayout("insets 12 24 12 24, fillx, gap 10", "[][][][][][][][grow][]", "[]"));
        filterBar.setBackground(tm.background());
        filterBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, tm.border()));

        JLabel lblDesde = new JLabel("Desde:");
        lblDesde.setForeground(tm.textPrimary());
        filterBar.add(lblDesde);

        dpFechaDesde = createStyledDatePicker();
        filterBar.add(dpFechaDesde);

        JLabel lblHasta = new JLabel("Hasta:");
        lblHasta.setForeground(tm.textPrimary());
        filterBar.add(lblHasta);

        dpFechaHasta = createStyledDatePicker();
        filterBar.add(dpFechaHasta);

        JLabel lblCorte = new JLabel("Corte Venc.:");
        lblCorte.setForeground(tm.textPrimary());
        filterBar.add(lblCorte);

        dpFechaCorte = createStyledDatePicker();
        filterBar.add(dpFechaCorte);

        // Toggle Moneda ($ / Bs)
        JPanel monedaPanel = new JPanel(new MigLayout("insets 0, gap 0", "[]0[]", "[]"));
        monedaPanel.setOpaque(false);

        btnUsd = createMonedaToggle("$", true);
        btnBs = createMonedaToggle("Bs", false);
        ButtonGroup bgMoneda = new ButtonGroup();
        bgMoneda.add(btnUsd);
        bgMoneda.add(btnBs);

        btnUsd.addActionListener(e -> {
            if (isBs) {
                isBs = false;
                actualizarVistasMoneda();
            }
        });
        btnBs.addActionListener(e -> {
            if (!isBs) {
                isBs = true;
                actualizarVistasMoneda();
            }
        });

        monedaPanel.add(btnUsd);
        monedaPanel.add(btnBs);
        filterBar.add(monedaPanel);

        // Barra de búsqueda rápida
        txtBuscar = new JTextField();
        txtBuscar.putClientProperty("JTextField.placeholderText", "🔍 Filtrar por Cliente, Vendedor, Factura o Analista...");
        txtBuscar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { aplicarFiltroRapido(); }
            @Override public void removeUpdate(DocumentEvent e) { aplicarFiltroRapido(); }
            @Override public void changedUpdate(DocumentEvent e) { aplicarFiltroRapido(); }
        });
        filterBar.add(txtBuscar, "growx");

        JButton btnProcesar = new JButton("🔍 Consultar");
        btnProcesar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnProcesar.setBackground(tm.accent());
        btnProcesar.setForeground(Color.WHITE);
        btnProcesar.setFocusPainted(false);
        btnProcesar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnProcesar.addActionListener(e -> procesarConsulta());
        filterBar.add(btnProcesar);

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(headerPanel, BorderLayout.NORTH);
        topContainer.add(filterBar, BorderLayout.SOUTH);
        root.add(topContainer, BorderLayout.NORTH);

        // ═══════════════════════════════════════════════════════════
        //  TABS & TABLES
        // ═══════════════════════════════════════════════════════════
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Tab 1: Detalle de Documentos
        JPanel panelDetalle = createDetalleTab();
        tabbedPane.addTab("📄 Detalle de Documentos", panelDetalle);

        // Tab 2: Resumen por Vendedor
        JPanel panelVendedores = createVendedoresTab();
        tabbedPane.addTab("👤 Resumen por Vendedor", panelVendedores);

        // Tab 3: Resumen por Analista
        JPanel panelAnalistas = createAnalistasTab();
        tabbedPane.addTab("📊 Resumen por Analista (D-H, F-E, J-S)", panelAnalistas);

        root.add(tabbedPane, BorderLayout.CENTER);

        // ═══════════════════════════════════════════════════════════
        //  FOOTER / KPI CARDS
        // ═══════════════════════════════════════════════════════════
        JPanel kpiPanel = new JPanel(new MigLayout("insets 12 24 12 24, fillx, gap 20", "[grow][grow][grow][grow][grow][grow]", "[]"));
        kpiPanel.setBackground(tm.cardBg());
        kpiPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, tm.border()));

        lblStatRegistros = createKpiCard(kpiPanel, "Total Documentos", "0", tm.textSecondary(), null);
        lblStatSaldo = createKpiCard(kpiPanel, "Saldo Total USD", "$0.00", tm.textPrimary(), lbl -> lblTitleCardSaldo = lbl);
        lblStatAlt = createKpiCard(kpiPanel, "Total en Bs.", "0,00 Bs.", tm.orangeAccent(), lbl -> lblTitleCardAlt = lbl);
        lblStatPorVencer = createKpiCard(kpiPanel, "Por Vencer USD", "$0.00", tm.greenAccent(), lbl -> lblTitleCardPorVencer = lbl);
        lblStatVencido = createKpiCard(kpiPanel, "Vencido Total USD", "$0.00", tm.redAccent(), lbl -> lblTitleCardVencido = lbl);
        lblStatPorcVencido = createKpiCard(kpiPanel, "% Morosidad / Vencido", "0.0%", tm.purpleAccent(), null);

        root.add(kpiPanel, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel createDetalleTab() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] columnNames = {
                "#", "CODIGO CLIENTE", "RIF", "GRUPO CLIENTE", "CLIENTE", "FACT", "TIPO", "F-I", "EMISIÓN",
                "VENCIM.", "DIAS DE VENC", "NETO ($)", "IVA ($)", "SALDO ($)", "TASA", "TOTAL Bs.",
                "POR VENCER", "1-30", "31-60", "61-90", ">=91", "COD. VND", "VEND.", "ANALISTA", "PEDIDO"
        };

        modelDetalle = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        tableDetalle = new JTable(modelDetalle);
        tableDetalle.setRowHeight(24);
        tableDetalle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableDetalle.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tableDetalle.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);

        for (int i = 0; i < columnNames.length; i++) {
            if (i == 3 || i == 4 || i == 22 || i == 24) {
                // Grupo Cliente, Cliente, Vendedor, Pedido alineado a la izquierda
            } else if (i >= 11 && i <= 20) {
                tableDetalle.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
            } else {
                tableDetalle.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }
        }

        // Renderer especial para F-I (columna 7)
        tableDetalle.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                if (value != null && "F-I".equalsIgnoreCase(value.toString().trim())) {
                    if (!isSelected) {
                        c.setForeground(tm.redAccent());
                        setFont(getFont().deriveFont(Font.BOLD));
                    }
                }
                return c;
            }
        });

        // Renderer especial para Días de Vencimiento (columna 10)
        tableDetalle.getColumnModel().getColumn(10).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                if (value != null && !value.toString().isBlank()) {
                    try {
                        int dias = Integer.parseInt(value.toString().trim());
                        if (!isSelected) {
                            if (dias <= 0) c.setForeground(tm.greenAccent());
                            else if (dias <= 30) c.setForeground(tm.orangeAccent());
                            else c.setForeground(tm.redAccent());
                        }
                    } catch (Exception ignored) {}
                }
                return c;
            }
        });

        sorterDetalle = new TableRowSorter<>(modelDetalle);
        tableDetalle.setRowSorter(sorterDetalle);

        // Comparador para columna # (0)
        sorterDetalle.setComparator(0, (o1, o2) -> {
            int i1 = (o1 instanceof Number) ? ((Number) o1).intValue() : Integer.parseInt(o1.toString().trim());
            int i2 = (o2 instanceof Number) ? ((Number) o2).intValue() : Integer.parseInt(o2.toString().trim());
            return Integer.compare(i1, i2);
        });

        // Comparador para fechas: Emisión (7) y Vencim (8)
        Comparator<Object> dateComparator = (o1, o2) -> {
            if (o1 == null && o2 == null) return 0;
            if (o1 == null || o1.toString().isBlank()) return 1;
            if (o2 == null || o2.toString().isBlank()) return -1;
            try {
                LocalDate d1 = LocalDate.parse(o1.toString().trim(), DISPLAY_FMT);
                LocalDate d2 = LocalDate.parse(o2.toString().trim(), DISPLAY_FMT);
                return d1.compareTo(d2);
            } catch (Exception e) {
                return o1.toString().compareToIgnoreCase(o2.toString());
            }
        };
        sorterDetalle.setComparator(7, dateComparator);
        sorterDetalle.setComparator(8, dateComparator);

        // Comparador para días de vencimiento (9)
        sorterDetalle.setComparator(9, (o1, o2) -> {
            int i1 = (o1 != null && !o1.toString().isBlank()) ? Integer.parseInt(o1.toString().trim()) : 0;
            int i2 = (o2 != null && !o2.toString().isBlank()) ? Integer.parseInt(o2.toString().trim()) : 0;
            return Integer.compare(i1, i2);
        });

        // Comparador para montos numéricos (10 al 19)
        Comparator<Object> currencyComparator = (o1, o2) -> {
            double v1 = parseCurrency(o1);
            double v2 = parseCurrency(o2);
            return Double.compare(v1, v2);
        };
        for (int c = 10; c <= 19; c++) {
            sorterDetalle.setComparator(c, currencyComparator);
        }

        JScrollPane sp = new JScrollPane(tableDetalle);
        sp.setBorder(BorderFactory.createEmptyBorder());
        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createVendedoresTab() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] cols = {
                "VENDEDOR", "POR VENCER ($)", "1-30 ($)", "31-60 ($)", "61-90 ($)", ">=91 ($)", "SALDO TOTAL ($)", "VENCIDO ($)", "% VENCIDO"
        };
        modelVendedores = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableVendedores = new JTable(modelVendedores);
        tableVendedores.setRowHeight(26);
        tableVendedores.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableVendedores.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i = 1; i < cols.length; i++) {
            tableVendedores.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
        }

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modelVendedores);
        tableVendedores.setRowSorter(sorter);

        JScrollPane sp = new JScrollPane(tableVendedores);
        sp.setBorder(BorderFactory.createEmptyBorder());
        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createAnalistasTab() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] cols = {
                "ANALISTA", "POR VENCER ($)", "1-30 ($)", "31-60 ($)", "61-90 ($)", ">=91 ($)", "SALDO TOTAL ($)", "VENCIDO ($)", "% VENCIDO"
        };
        modelAnalistas = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableAnalistas = new JTable(modelAnalistas);
        tableAnalistas.setRowHeight(26);
        tableAnalistas.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableAnalistas.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i = 1; i < cols.length; i++) {
            tableAnalistas.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
        }

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modelAnalistas);
        tableAnalistas.setRowSorter(sorter);

        JScrollPane sp = new JScrollPane(tableAnalistas);
        sp.setBorder(BorderFactory.createEmptyBorder());
        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }

    private JLabel createKpiCard(JPanel parent, String title, String initialVal, Color valColor, java.util.function.Consumer<JLabel> titleConsumer) {
        JPanel card = new JPanel(new MigLayout("insets 8 12 8 12, wrap", "[]", "[]2[]"));
        card.setBackground(tm.bgPanel());
        card.setBorder(BorderFactory.createLineBorder(tm.border(), 1));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblTitle.setForeground(tm.textSecondary());
        card.add(lblTitle);
        if (titleConsumer != null) {
            titleConsumer.accept(lblTitle);
        }

        JLabel lblValue = new JLabel(initialVal);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblValue.setForeground(valColor);
        card.add(lblValue);

        parent.add(card, "grow");
        return lblValue;
    }

    private JToggleButton createMonedaToggle(String text, boolean selected) {
        JToggleButton btn = new JToggleButton(text, selected);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(5, 14, 5, 14));
        btn.setPreferredSize(new Dimension(48, 30));
        return btn;
    }

    private DatePicker createStyledDatePicker() {
        DatePickerSettings settings = new DatePickerSettings(Locale.of("es", "VE"));
        settings.setFormatForDatesCommonEra("dd/MM/yyyy");
        settings.setAllowEmptyDates(false);
        settings.setFirstDayOfWeek(DayOfWeek.MONDAY);

        settings.setColor(DatePickerSettings.DateArea.TextFieldBackgroundValidDate, tm.bgField());
        settings.setColor(DatePickerSettings.DateArea.DatePickerTextValidDate, tm.textPrimary());
        settings.setColor(DatePickerSettings.DateArea.CalendarBackgroundNormalDates, tm.cardBg());
        settings.setColor(DatePickerSettings.DateArea.CalendarTextNormalDates, tm.textPrimary());
        settings.setColor(DatePickerSettings.DateArea.BackgroundOverallCalendarPanel, tm.cardBg());
        settings.setColor(DatePickerSettings.DateArea.BackgroundMonthAndYearMenuLabels, tm.cardBg());
        settings.setColor(DatePickerSettings.DateArea.BackgroundTodayLabel, tm.cardBg());
        settings.setColor(DatePickerSettings.DateArea.BackgroundClearLabel, tm.cardBg());
        settings.setColor(DatePickerSettings.DateArea.CalendarBackgroundSelectedDate, tm.accent());

        settings.setFontValidDate(new Font("Segoe UI", Font.PLAIN, 12));

        DatePicker picker = new DatePicker(settings);
        picker.setPreferredSize(new Dimension(140, 30));
        return picker;
    }

    private void setDefaultDates() {
        dpFechaDesde.setDate(LocalDate.of(2020, 1, 1));
        dpFechaHasta.setDate(LocalDate.now());
        dpFechaCorte.setDate(LocalDate.now());
    }

    private void procesarConsulta() {
        LocalDate desde = dpFechaDesde.getDate();
        LocalDate hasta = dpFechaHasta.getDate();
        LocalDate corte = dpFechaCorte.getDate();

        if (desde == null || hasta == null) {
            Toast.showError("Por favor selecciona ambas fechas (Desde y Hasta).");
            return;
        }

        String dbLabel = service.getDbLabel(desde, hasta);
        lblSub.setText("Reporte Multimoneda (" + (isBs ? "Bs" : "USD $") + ") — Condición: Sin Cancelar | Base de Datos: " + dbLabel + " | Corte: " + corte.format(DISPLAY_FMT));
        setTitle("DroAI — Estado de Cuentas por Cobrar (" + dbLabel + ")");

        Toast.showInfo("Consultando cuentas por cobrar en " + dbLabel + "...");

        SwingWorker<List<CxCDocumentoRow>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<CxCDocumentoRow> doInBackground() {
                return service.consultarDocumentos(desde, hasta, corte);
            }

            @Override
            protected void done() {
                try {
                    currentData = get();
                    actualizarTablas(currentData);
                    actualizarKpis();

                    Toast.showSuccess("✅ " + currentData.size() + " documentos de CxC cargados exitosamente.");
                } catch (Exception e) {
                    Toast.showError("Error al consultar CxC: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void actualizarVistasMoneda() {
        actualizarCabecerasColumnas();

        LocalDate corte = dpFechaCorte.getDate() != null ? dpFechaCorte.getDate() : LocalDate.now();
        LocalDate desde = dpFechaDesde.getDate() != null ? dpFechaDesde.getDate() : LocalDate.of(2020, 1, 1);
        LocalDate hasta = dpFechaHasta.getDate() != null ? dpFechaHasta.getDate() : LocalDate.now();
        String dbLabel = service.getDbLabel(desde, hasta);
        lblSub.setText("Reporte Multimoneda (" + (isBs ? "Bs" : "USD $") + ") — Condición: Sin Cancelar | Base de Datos: " + dbLabel + " | Corte: " + corte.format(DISPLAY_FMT));

        actualizarTablas(currentData);
        actualizarKpis();
    }

    private void actualizarCabecerasColumnas() {
        if (tableDetalle != null && tableDetalle.getColumnModel().getColumnCount() >= 20) {
            tableDetalle.getColumnModel().getColumn(10).setHeaderValue(isBs ? "NETO (Bs)" : "NETO ($)");
            tableDetalle.getColumnModel().getColumn(11).setHeaderValue(isBs ? "IVA (Bs)" : "IVA ($)");
            tableDetalle.getColumnModel().getColumn(12).setHeaderValue(isBs ? "SALDO (Bs)" : "SALDO ($)");
            tableDetalle.getColumnModel().getColumn(14).setHeaderValue(isBs ? "TOTAL ($)" : "TOTAL Bs.");
            tableDetalle.getTableHeader().repaint();
        }

        if (tableVendedores != null && tableVendedores.getColumnModel().getColumnCount() >= 9) {
            String monedaLabel = isBs ? "(Bs)" : "($)";
            tableVendedores.getColumnModel().getColumn(1).setHeaderValue("POR VENCER " + monedaLabel);
            tableVendedores.getColumnModel().getColumn(2).setHeaderValue("1-30 " + monedaLabel);
            tableVendedores.getColumnModel().getColumn(3).setHeaderValue("31-60 " + monedaLabel);
            tableVendedores.getColumnModel().getColumn(4).setHeaderValue("61-90 " + monedaLabel);
            tableVendedores.getColumnModel().getColumn(5).setHeaderValue(">=91 " + monedaLabel);
            tableVendedores.getColumnModel().getColumn(6).setHeaderValue("SALDO TOTAL " + monedaLabel);
            tableVendedores.getColumnModel().getColumn(7).setHeaderValue("VENCIDO " + monedaLabel);
            tableVendedores.getTableHeader().repaint();
        }

        if (tableAnalistas != null && tableAnalistas.getColumnModel().getColumnCount() >= 9) {
            String monedaLabel = isBs ? "(Bs)" : "($)";
            tableAnalistas.getColumnModel().getColumn(1).setHeaderValue("POR VENCER " + monedaLabel);
            tableAnalistas.getColumnModel().getColumn(2).setHeaderValue("1-30 " + monedaLabel);
            tableAnalistas.getColumnModel().getColumn(3).setHeaderValue("31-60 " + monedaLabel);
            tableAnalistas.getColumnModel().getColumn(4).setHeaderValue("61-90 " + monedaLabel);
            tableAnalistas.getColumnModel().getColumn(5).setHeaderValue(">=91 " + monedaLabel);
            tableAnalistas.getColumnModel().getColumn(6).setHeaderValue("SALDO TOTAL " + monedaLabel);
            tableAnalistas.getColumnModel().getColumn(7).setHeaderValue("VENCIDO " + monedaLabel);
            tableAnalistas.getTableHeader().repaint();
        }
    }

    private void actualizarKpis() {
        TotalesCxC tot = service.calcularTotales(currentData);

        lblStatRegistros.setText(String.valueOf(tot.getTotalRegistros()));
        if (!isBs) {
            if (lblTitleCardSaldo != null) lblTitleCardSaldo.setText("Saldo Total USD");
            lblStatSaldo.setText("$" + CURRENCY_FMT.format(tot.getTotalSaldo()));

            if (lblTitleCardAlt != null) lblTitleCardAlt.setText("Total en Bs.");
            lblStatAlt.setText(CURRENCY_FMT.format(tot.getTotalBs()) + " Bs.");

            if (lblTitleCardPorVencer != null) lblTitleCardPorVencer.setText("Por Vencer USD");
            lblStatPorVencer.setText("$" + CURRENCY_FMT.format(tot.getTotalPorVencer()));

            if (lblTitleCardVencido != null) lblTitleCardVencido.setText("Vencido Total USD");
            lblStatVencido.setText("$" + CURRENCY_FMT.format(tot.getTotalVencido()));
        } else {
            if (lblTitleCardSaldo != null) lblTitleCardSaldo.setText("Saldo Total Bs.");
            lblStatSaldo.setText(CURRENCY_FMT.format(tot.getTotalBs()) + " Bs.");

            if (lblTitleCardAlt != null) lblTitleCardAlt.setText("Total en USD");
            lblStatAlt.setText("$" + CURRENCY_FMT.format(tot.getTotalSaldo()));

            if (lblTitleCardPorVencer != null) lblTitleCardPorVencer.setText("Por Vencer Bs.");
            lblStatPorVencer.setText(CURRENCY_FMT.format(tot.getTotalPorVencerBs()) + " Bs.");

            if (lblTitleCardVencido != null) lblTitleCardVencido.setText("Vencido Total Bs.");
            lblStatVencido.setText(CURRENCY_FMT.format(tot.getTotalVencidoBs()) + " Bs.");
        }
        lblStatPorcVencido.setText(String.format("%.1f%%", tot.getPorcVencido()));
    }

    private void actualizarTablas(List<CxCDocumentoRow> rows) {
        if (rows == null) return;

        // Orden por defecto: 1° Vencimiento (antiguo a nuevo), 2° Factura (A-Z)
        rows.sort(Comparator
                .comparing(CxCDocumentoRow::getFechaVencimiento, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(r -> r.getFactura() != null ? r.getFactura() : "", String.CASE_INSENSITIVE_ORDER)
        );

        if (sorterDetalle != null) {
            sorterDetalle.setSortKeys(null);
        }

        // 1. Detalle
        modelDetalle.setRowCount(0);
        int sec = 1;
        for (CxCDocumentoRow r : rows) {
            double tasa = r.getTasa() > 0 ? r.getTasa() : 1.0;
            double netoVal = isBs ? (r.getNeto() * tasa) : r.getNeto();
            double ivaVal = isBs ? (r.getIva() * tasa) : r.getIva();
            double saldoVal = isBs ? r.getTotalBs() : r.getSaldo();
            double altVal = isBs ? r.getSaldo() : r.getTotalBs();
            double factor = isBs ? tasa : 1.0;

            Object[] rowData = {
                    sec++,
                    r.getCodigoCliente(),
                    r.getRifCliente(),
                    r.getGrupoCliente() != null ? r.getGrupoCliente().trim() : "",
                    r.getCliente(),
                    r.getFactura(),
                    r.getTipoDoc(),
                    r.getFacturaImpaga(),
                    r.getFechaEmision() != null ? r.getFechaEmision().format(DISPLAY_FMT) : "",
                    r.getFechaVencimiento() != null ? r.getFechaVencimiento().format(DISPLAY_FMT) : "",
                    r.getDiasVencimiento(),
                    CURRENCY_FMT.format(netoVal),
                    Math.abs(ivaVal) > 0.001 ? CURRENCY_FMT.format(ivaVal) : "-",
                    CURRENCY_FMT.format(saldoVal),
                    TASA_FMT.format(r.getTasa()),
                    CURRENCY_FMT.format(altVal),
                    Math.abs(r.getPorVencer()) > 0.001 ? CURRENCY_FMT.format(r.getPorVencer() * factor) : "-",
                    Math.abs(r.getVencido1a30()) > 0.001 ? CURRENCY_FMT.format(r.getVencido1a30() * factor) : "-",
                    Math.abs(r.getVencido31a60()) > 0.001 ? CURRENCY_FMT.format(r.getVencido31a60() * factor) : "-",
                    Math.abs(r.getVencido61a90()) > 0.001 ? CURRENCY_FMT.format(r.getVencido61a90() * factor) : "-",
                    Math.abs(r.getVencidoMas91()) > 0.001 ? CURRENCY_FMT.format(r.getVencidoMas91() * factor) : "-",
                    r.getCodVendedor(),
                    r.getNombreVendedor(),
                    r.getAnalista(),
                    r.getPedido()
            };
            modelDetalle.addRow(rowData);
        }

        // 2. Resumen Vendedores
        modelVendedores.setRowCount(0);
        List<ResumenAgrupado> listVend = service.agruparPorVendedor(rows, isBs);
        for (ResumenAgrupado ra : listVend) {
            Object[] r = {
                    ra.getGrupo(),
                    CURRENCY_FMT.format(ra.getPorVencer()),
                    CURRENCY_FMT.format(ra.getVencido1a30()),
                    CURRENCY_FMT.format(ra.getVencido31a60()),
                    CURRENCY_FMT.format(ra.getVencido61a90()),
                    CURRENCY_FMT.format(ra.getVencidoMas91()),
                    CURRENCY_FMT.format(ra.getSaldoTotal()),
                    CURRENCY_FMT.format(ra.getVencidoTotal()),
                    String.format("%.1f%%", ra.getPorcVencido())
            };
            modelVendedores.addRow(r);
        }

        // 3. Resumen Analistas
        modelAnalistas.setRowCount(0);
        List<ResumenAgrupado> listAna = service.agruparPorAnalista(rows, isBs);
        for (ResumenAgrupado ra : listAna) {
            Object[] r = {
                    ra.getGrupo(),
                    CURRENCY_FMT.format(ra.getPorVencer()),
                    CURRENCY_FMT.format(ra.getVencido1a30()),
                    CURRENCY_FMT.format(ra.getVencido31a60()),
                    CURRENCY_FMT.format(ra.getVencido61a90()),
                    CURRENCY_FMT.format(ra.getVencidoMas91()),
                    CURRENCY_FMT.format(ra.getSaldoTotal()),
                    CURRENCY_FMT.format(ra.getVencidoTotal()),
                    String.format("%.1f%%", ra.getPorcVencido())
            };
            modelAnalistas.addRow(r);
        }
    }

    private void aplicarFiltroRapido() {
        if (sorterDetalle == null) return;
        String query = txtBuscar.getText().trim();
        if (query.isEmpty()) {
            sorterDetalle.setRowFilter(null);
        } else {
            sorterDetalle.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(query)));
        }
    }

    private void exportarExcel() {
        if (currentData.isEmpty()) {
            Toast.showWarning("Primero debes procesar una consulta para exportar.");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Estado de Cuentas por Cobrar (EDC)");
        fileChooser.setSelectedFile(new File("EDC " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yy")) + " MAESTRO.xlsx"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".xlsx")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".xlsx");
            }

            try {
                File exported = excelExporter.exportCxCDocumentos(currentData, fileToSave, isBs);
                Toast.showSuccess("Archivo exportado exitosamente.");
                JOptionPane.showMessageDialog(this,
                        "El archivo Excel ha sido exportado exitosamente en:\n\n" + exported.getAbsolutePath(),
                        "Exportación Completada", JOptionPane.INFORMATION_MESSAGE);
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(exported);
                }
            } catch (Exception e) {
                Toast.showError("Error al exportar Excel: " + e.getMessage());
            }
        }
    }

    private double parseCurrency(Object obj) {
        if (obj == null) return 0.0;
        String s = obj.toString().trim();
        if (s.isEmpty() || s.equals("-")) return 0.0;
        try {
            return CURRENCY_FMT.parse(s).doubleValue();
        } catch (Exception e) {
            try {
                return Double.parseDouble(s.replace(".", "").replace(",", "."));
            } catch (Exception ignored) {
                return 0.0;
            }
        }
    }
}
