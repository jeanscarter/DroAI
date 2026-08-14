package com.droai.ui;

import com.droai.dao.ComisionesDAO.VendedorOption;
import com.droai.export.ExcelExporter;
import com.droai.model.ComisionRow;
import com.droai.service.ComisionesService;
import com.droai.ui.components.Toast;
import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ventana Principal del Módulo de Cálculo de Comisiones.
 */
public class ComisionesFrame extends JFrame {

    private final ThemeManager tm = ThemeManager.get();

    private final ComisionesService service;
    private final ExcelExporter excelExporter;

    private DatePicker dpFechaDesde;
    private DatePicker dpFechaHasta;
    private JComboBox<Object> cmbVendedor;
    private JTable table;
    private DefaultTableModel tableModel;

    private JLabel lblStatDoc;
    private JLabel lblStatCobrado;
    private JLabel lblStatBase;
    private JLabel lblStatComision;
    private JLabel lblStatRegistros;

    private List<ComisionRow> currentData = new ArrayList<>();
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat CURRENCY_FMT = new DecimalFormat("#,##0.00");

    private JLabel lblSub; // Subtítulo dinámico (muestra BD activa)

    public ComisionesFrame() {
        this.service = new ComisionesService();
        this.excelExporter = new ExcelExporter();

        setTitle("DroAI — Cálculo de Comisiones");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1300, 780);
        setMinimumSize(new Dimension(1050, 650));
        setLocationRelativeTo(null);

        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/images/logo.png"));
            setIconImage(icon.getImage());
        } catch (Exception ignored) {}

        Toast.setParentFrame(this);

        // ── Listener de tema ──
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
        cargarVendedores();
        setDefaultDates();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(tm.background());

        // ═══════════════════════════════════════════════════════════
        //  HEADER & FILTERS
        // ═══════════════════════════════════════════════════════════
        JPanel headerPanel = new JPanel(new MigLayout("insets 16 24 12 24, fillx, wrap 2", "[grow][]", "[]8[]"));
        headerPanel.setBackground(tm.cardBg());
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, tm.border()));

        // Título + Subtítulo
        JPanel titleBox = new JPanel(new MigLayout("insets 0, wrap", "[]", "[]2[]"));
        titleBox.setOpaque(false);

        JLabel lblTitle = new JLabel("💰 Cálculo y Relación de Comisiones");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(tm.textPrimary());
        titleBox.add(lblTitle);

        lblSub = new JLabel("Base de Datos: (seleccione fechas) | Reglas por Días Calle y Base sin IVA");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(tm.textSecondary());
        titleBox.add(lblSub);

        headerPanel.add(titleBox, "growx");

        // Botones de exportación
        JPanel exportBox = new JPanel(new MigLayout("insets 0, gap 8", "[][]", "[]"));
        exportBox.setOpaque(false);

        JButton btnExportSel = new JButton("📥 Exportar Vendedor Seleccionado");
        btnExportSel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnExportSel.setBackground(tm.greenAccent());
        btnExportSel.setForeground(tm.btnForegroundFor(tm.greenAccent()));
        btnExportSel.setFocusPainted(false);
        btnExportSel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnExportSel.addActionListener(e -> exportarSeleccionado());
        exportBox.add(btnExportSel);

        JButton btnExportAll = new JButton("📦 Exportar Todos los Vendedores");
        btnExportAll.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnExportAll.setBackground(tm.accent());
        btnExportAll.setForeground(Color.WHITE);
        btnExportAll.setFocusPainted(false);
        btnExportAll.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnExportAll.addActionListener(e -> exportarTodos());
        exportBox.add(btnExportAll);

        headerPanel.add(exportBox, "alignx right");

        // Botón de tema
        JButton btnTema = new JButton(tm.isDark() ? "☀" : "🌙");
        btnTema.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        btnTema.setFocusPainted(false);
        btnTema.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnTema.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        btnTema.setToolTipText("Cambiar tema claro/oscuro");
        btnTema.addActionListener(e -> tm.toggleTheme());
        headerPanel.add(btnTema, "alignx right, wrap");

        // Barra de filtros (Fechas + Vendedor)
        JPanel filterBar = new JPanel(new MigLayout("insets 12 24 12 24, fillx, gap 12", "[][][][][][][grow][]", "[]"));
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

        JButton btn1QCNA = new JButton("1ra Quincena");
        btn1QCNA.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btn1QCNA.addActionListener(e -> setQuincena(1));
        filterBar.add(btn1QCNA);

        JButton btn2QCNA = new JButton("2da Quincena");
        btn2QCNA.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btn2QCNA.addActionListener(e -> setQuincena(2));
        filterBar.add(btn2QCNA);

        JLabel lblVen = new JLabel("Vendedor:");
        lblVen.setForeground(tm.textPrimary());
        filterBar.add(lblVen);

        cmbVendedor = new JComboBox<>();
        cmbVendedor.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        filterBar.add(cmbVendedor, "growx");

        JButton btnProcesar = new JButton("🔍 Procesar");
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
        //  TABLE
        // ═══════════════════════════════════════════════════════════
        String[] columnNames = {
                "#", "TIPO DOC.", "NUMERO DOCUMENTO", "CLASE", "FECHA DE EMISION",
                "FECHA DE VENCIMIENTO", "FECHA DE COBRO", "NUMERO COBRO", "DIAS CALLE",
                "CODIGO CLIENTE", "NOMBRE CLIENTE", "MONTO DOCUMENTO", "% DESC",
                "MONTO COBRADO", "BASE COMISION", "% COMISION", "MONTO COMISION", "VENDEDOR", "STATUS", "PSICO"
        };

        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Renderers numéricos y centrados
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);

        for (int i = 0; i < columnNames.length; i++) {
            if (i == 10 || i == 17) {
                // Nombre Cliente y Vendedor alineado a la izquierda
            } else if (i == 11 || i == 12 || i == 13 || i == 14 || i == 15 || i == 16) {
                table.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
            } else {
                table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }
        }

        // Renderer especial para la columna STATUS con colores
        final int statusColIdx = 18; // índice de la columna STATUS
        table.getColumnModel().getColumn(statusColIdx).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                if (value != null) {
                    String val = value.toString();
                    if (val.contains("Cerrada")) {
                        if (!isSelected) c.setForeground(tm.greenAccent());
                    } else {
                        if (!isSelected) c.setForeground(tm.orangeAccent());
                    }
                }
                return c;
            }
        });

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        root.add(scrollPane, BorderLayout.CENTER);

        // ═══════════════════════════════════════════════════════════
        //  FOOTER / SUMMARY KPIs
        // ═══════════════════════════════════════════════════════════
        JPanel kpiPanel = new JPanel(new MigLayout("insets 12 24 12 24, fillx, gap 24", "[grow][grow][grow][grow][grow]", "[]"));
        kpiPanel.setBackground(tm.cardBg());
        kpiPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, tm.border()));

        lblStatRegistros = createKpiCard(kpiPanel, "Registros", "0", tm.textSecondary());
        lblStatDoc = createKpiCard(kpiPanel, "Monto Documentos", "0,00 Bs.", tm.textPrimary());
        lblStatCobrado = createKpiCard(kpiPanel, "Monto Cobrado", "0,00 Bs.", tm.textPrimary());
        lblStatBase = createKpiCard(kpiPanel, "Base Comisión", "0,00 Bs.", tm.orangeAccent());
        lblStatComision = createKpiCard(kpiPanel, "Comisión Ganada", "0,00 Bs.", tm.greenAccent());

        root.add(kpiPanel, BorderLayout.SOUTH);

        setContentPane(root);
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
        picker.setPreferredSize(new Dimension(150, 30));
        return picker;
    }

    private JLabel createKpiCard(JPanel parent, String title, String initialVal, Color valColor) {
        JPanel card = new JPanel(new MigLayout("insets 8 12 8 12, wrap", "[]", "[]2[]"));
        card.setBackground(tm.bgPanel());
        card.setBorder(BorderFactory.createLineBorder(tm.border(), 1));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblTitle.setForeground(tm.textSecondary());
        card.add(lblTitle);

        JLabel lblValue = new JLabel(initialVal);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblValue.setForeground(valColor);
        card.add(lblValue);

        parent.add(card, "grow");
        return lblValue;
    }

    private void setDefaultDates() {
        dpFechaDesde.setDate(LocalDate.of(2026, 7, 1));
        dpFechaHasta.setDate(LocalDate.of(2026, 7, 15));
    }

    private void setQuincena(int nroQuincena) {
        try {
            LocalDate ref = dpFechaDesde.getDate();
            if (ref == null) ref = LocalDate.now();
            YearMonth ym = YearMonth.from(ref);
            if (nroQuincena == 1) {
                dpFechaDesde.setDate(ym.atDay(1));
                dpFechaHasta.setDate(ym.atDay(15));
            } else {
                dpFechaDesde.setDate(ym.atDay(16));
                dpFechaHasta.setDate(ym.atEndOfMonth());
            }
        } catch (Exception e) {
            dpFechaDesde.setDate(LocalDate.of(2026, 7, 1));
            dpFechaHasta.setDate(LocalDate.of(2026, 7, 15));
        }
    }

    private void cargarVendedores() {
        cmbVendedor.removeAllItems();
        cmbVendedor.addItem("— Todos los Vendedores —");

        SwingWorker<List<VendedorOption>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<VendedorOption> doInBackground() {
                return service.obtenerVendedores();
            }

            @Override
            protected void done() {
                try {
                    List<VendedorOption> vens = get();
                    for (VendedorOption v : vens) {
                        cmbVendedor.addItem(v);
                    }
                } catch (Exception e) {
                    Toast.showError("Error al cargar vendedores: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void procesarConsulta() {
        LocalDate desde = dpFechaDesde.getDate();
        LocalDate hasta = dpFechaHasta.getDate();

        if (desde == null || hasta == null) {
            Toast.showError("Por favor selecciona ambas fechas (Desde y Hasta).");
            return;
        }

        String coVenFiltro = null;
        Object selected = cmbVendedor.getSelectedItem();
        if (selected instanceof VendedorOption) {
            coVenFiltro = ((VendedorOption) selected).getCodigo();
        }

        final String finalCoVen = coVenFiltro;

        // Actualizar subtítulo y título con la BD activa
        String dbLabel = service.getDbLabel(desde, hasta);
        lblSub.setText("Base de Datos: " + dbLabel + " | Reglas por Días Calle y Base sin IVA");
        setTitle("DroAI — Cálculo de Comisiones (" + dbLabel + ")");

        Toast.showInfo("Consultando base de datos " + dbLabel + "...");

        SwingWorker<List<ComisionRow>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ComisionRow> doInBackground() {
                return service.consultarComisiones(desde, hasta, finalCoVen);
            }

            @Override
            protected void done() {
                try {
                    currentData = get();
                    actualizarComboVendedoresDesdeResultados(currentData);
                    actualizarTabla(currentData);
                    ComisionesService.TotalesComisiones tot = service.calcularTotales(currentData);

                    lblStatRegistros.setText(String.valueOf(tot.getTotalRegistros()));
                    lblStatDoc.setText(CURRENCY_FMT.format(tot.getTotalMontoDoc()) + " Bs.");
                    lblStatCobrado.setText(CURRENCY_FMT.format(tot.getTotalMontoCobrado()) + " Bs.");
                    lblStatBase.setText(CURRENCY_FMT.format(tot.getTotalBaseComision()) + " Bs.");
                    lblStatComision.setText(CURRENCY_FMT.format(tot.getTotalMontoComision()) + " Bs.");

                    // Verificar facturas abiertas y mostrar advertencia
                    long facturasAbiertas = service.contarFacturasAbiertas(currentData);
                    if (facturasAbiertas > 0) {
                        Toast.showWarning("⚠️ Hay " + facturasAbiertas + " registro(s) con facturas ABIERTAS (no cerradas).");
                    } else {
                        Toast.showSuccess("✅ Todas las facturas están CERRADAS. " + currentData.size() + " registros encontrados.");
                    }
                } catch (Exception e) {
                    Toast.showError("Error al consultar comisiones: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void actualizarComboVendedoresDesdeResultados(List<ComisionRow> rows) {
        Object currentlySelected = cmbVendedor.getSelectedItem();
        cmbVendedor.removeAllItems();
        cmbVendedor.addItem("— Todos los Vendedores —");

        java.util.Map<String, String> uniqueSellers = new java.util.LinkedHashMap<>();
        if (rows != null) {
            for (ComisionRow r : rows) {
                String cod = r.getCodigoVendedor();
                String nom = r.getNombreVendedor();
                if (cod != null && !cod.isBlank()) {
                    uniqueSellers.putIfAbsent(cod.trim(), nom != null && !nom.isBlank() ? nom.trim() : cod.trim());
                }
            }
        }

        VendedorOption toReselect = null;
        for (java.util.Map.Entry<String, String> entry : uniqueSellers.entrySet()) {
            VendedorOption vo = new VendedorOption(entry.getKey(), entry.getValue());
            cmbVendedor.addItem(vo);
            if (currentlySelected instanceof VendedorOption && ((VendedorOption) currentlySelected).getCodigo().equalsIgnoreCase(entry.getKey())) {
                toReselect = vo;
            }
        }

        if (toReselect != null) {
            cmbVendedor.setSelectedItem(toReselect);
        }
    }

    private void actualizarTabla(List<ComisionRow> rows) {
        tableModel.setRowCount(0);
        for (ComisionRow r : rows) {
            Object[] rowData = {
                    r.getNumero(),
                    r.getTipoDoc(),
                    r.getNumeroDocumento(),
                    r.getClase(),
                    r.getFechaEmision() != null ? r.getFechaEmision().format(DISPLAY_FMT) : "",
                    r.getFechaVencimiento() != null ? r.getFechaVencimiento().format(DISPLAY_FMT) : "",
                    r.getFechaCobro() != null ? r.getFechaCobro().format(DISPLAY_FMT) : "",
                    r.getNumeroCobro(),
                    r.getDiasCalle(),
                    r.getCodigoCliente(),
                    r.getNombreCliente(),
                    CURRENCY_FMT.format(r.getMontoDocumento()),
                    CURRENCY_FMT.format(r.getPorcDesc()),
                    CURRENCY_FMT.format(r.getMontoCobrado()),
                    CURRENCY_FMT.format(r.getBaseComision()),
                    r.getPorcComision() > 0 ? CURRENCY_FMT.format(r.getPorcComision()) : "-",
                    CURRENCY_FMT.format(r.getMontoComision()),
                    r.getNombreVendedor(),
                    r.isFacturaCerrada() ? "✅ Cerrada" : "⚠️ Abierta",
                    r.getPsico()
            };
            tableModel.addRow(rowData);
        }
    }

    private File getDefaultExportDir() {
        File dDir = new File("D:\\comisiones OSKA");
        if (dDir.exists() && dDir.isDirectory()) {
            return dDir;
        }

        File userHome = new File(System.getProperty("user.home"));
        File desktop = new File(userHome, "Desktop");
        if (desktop.exists() && desktop.isDirectory()) {
            return desktop;
        }

        File home = javax.swing.filechooser.FileSystemView.getFileSystemView().getHomeDirectory();
        if (home != null && home.exists()) {
            return home;
        }

        return userHome;
    }

    private void exportarSeleccionado() {
        if (currentData.isEmpty()) {
            Toast.showWarning("Primero debes procesar una consulta para exportar.");
            return;
        }

        Object selected = cmbVendedor.getSelectedItem();
        if (!(selected instanceof VendedorOption)) {
            // Si está en "— Todos los Vendedores —", llamar al exportador maestro
            exportarTodos();
            return;
        }

        VendedorOption vo = (VendedorOption) selected;
        String coVen = vo.getCodigo();
        String nomVen = vo.getNombre();

        File defaultDir = getDefaultExportDir();
        JFileChooser fileChooser = new JFileChooser(defaultDir);
        fileChooser.setDialogTitle("Guardar Relación de Comisiones (" + coVen + ")");
        fileChooser.setSelectedFile(new File(defaultDir, "COMISIONES 1RA QCNA JULIO 2026 " + coVen + ".xlsx"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".xlsx")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".xlsx");
            }

            try {
                List<ComisionRow> vendorData = new ArrayList<>();
                for (ComisionRow r : currentData) {
                    if (coVen.equalsIgnoreCase(r.getCodigoVendedor())) {
                        vendorData.add(r);
                    }
                }
                File exported = excelExporter.exportRelacionComisiones(vendorData, "00000160", LocalDate.now(), coVen, nomVen, fileToSave);
                Toast.showSuccess("Archivo exportado exitosamente.");
                JOptionPane.showMessageDialog(this,
                        "El archivo Excel ha sido exportado exitosamente en:\n\n" + exported.getAbsolutePath(),
                        "Exportación Completada", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                Toast.showError("Error al exportar Excel: " + e.getMessage());
            }
        }
    }

    private void exportarTodos() {
        if (currentData.isEmpty()) {
            Toast.showWarning("Primero debes procesar una consulta para exportar.");
            return;
        }

        File defaultDir = getDefaultExportDir();
        JFileChooser fileChooser = new JFileChooser(defaultDir);
        fileChooser.setDialogTitle("Guardar Relación General Única de Comisiones (Todos los Vendedores)");
        fileChooser.setSelectedFile(new File(defaultDir, "RELACION DE COMISIONES 1RA QCNA JULIO 2026 GENERAL.xlsx"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".xlsx")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".xlsx");
            }

            try {
                // Exporta un UNICO archivo maestro con hoja GENERAL + pestañas por vendedor
                File exported = excelExporter.exportRelacionComisionesMaestro(currentData, "00000160", LocalDate.now(), fileToSave);
                Toast.showSuccess("Archivo maestro consolidado exportado exitosamente.");
                JOptionPane.showMessageDialog(this,
                        "Se ha generado el ARCHIVO ÚNICO CONSOLIDADO de comisiones con la hoja GENERAL y pestañas por vendedor en:\n\n" + exported.getAbsolutePath(),
                        "Exportación Maestro Completada", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                Toast.showError("Error durante la exportación del archivo maestro: " + e.getMessage());
            }
        }
    }
}
