package com.droai.ui;

import com.droai.dao.ClienteDAO;
import com.droai.dao.MatrizVentasDAO;
import com.droai.export.ExcelExporter;
import com.droai.model.ClienteMaestroRow;
import com.droai.model.MatrizVentasRow;
import com.droai.ui.components.RoundedPanel;
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
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Módulo de Gestión Comercial — Análisis interactivo de Productos Facturados
 * y Maestro General de Clientes desde SQL Server.
 * Integra KPIs, filtros dinámicos por Mes, Vendedor, Zona, Proveedor y Cliente,
 * gráficos de tendencias y tabla detallada con exportación a Excel.
 */
public class GestionComercialFrame extends JFrame {

    // ── Colores dinámicos vía ThemeManager ──
    private final ThemeManager tm = ThemeManager.get();

    // ── Formatters ──
    private static final NumberFormat fmtCurrency = NumberFormat.getCurrencyInstance(Locale.of("es", "VE"));
    private static final NumberFormat fmtNumber = NumberFormat.getNumberInstance(Locale.of("es", "VE"));

    // ── Componentes Principales ──
    private final DatePicker dateDesde;
    private final DatePicker dateHasta;
    private final JButton btnCargarData;
    private final JButton btnExportarExcel;

    // Combos de Filtro Dinámico
    private final JComboBox<String> cmbFiltroMes;
    private final JComboBox<String> cmbFiltroVendedor;
    private final JComboBox<String> cmbFiltroZona;
    private final JComboBox<String> cmbFiltroProveedor;
    private final JComboBox<String> cmbFiltroCliente;
    private final JButton btnLimpiarFiltros;

    // 12 KPIs Labels
    private final JLabel lblKpiTotalVentas;
    private final JLabel lblKpiTotalUnidades;
    private final JLabel lblKpiCrecVentas;
    private final JLabel lblKpiCrecUnidades;
    private final JLabel lblKpiTicketPromedio;
    private final JLabel lblKpiDropSize;
    private final JLabel lblKpiPrecioPromedio;
    private final JLabel lblKpiMejorZonaNom;
    private final JLabel lblKpiMejorZonaVal;
    private final JLabel lblKpiMejorClienteNom;
    private final JLabel lblKpiMejorClienteVal;
    private final JLabel lblKpiMejorProveedorNom;
    private final JLabel lblKpiMejorProveedorVal;
    private final JLabel lblKpiMejorProdValNom;
    private final JLabel lblKpiMejorProdValVal;
    private final JLabel lblKpiMejorProdUndNom;
    private final JLabel lblKpiMejorProdUndVal;

    // Tabla de Detalle y Modelo
    private final DefaultTableModel tableModel;
    private final JTable tableDetalle;
    private final JTextField txtBusquedaTabla;
    private TableRowSorter<DefaultTableModel> sorter;

    // Data en Memoria
    private List<MatrizVentasRow> rawData = new ArrayList<>();
    private List<MatrizVentasRow> filteredData = new ArrayList<>();
    private final MatrizVentasDAO dao = new MatrizVentasDAO();

    // ── Pestaña Maestro de Clientes ──
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private List<ClienteMaestroRow> rawClientes = new ArrayList<>();
    private List<ClienteMaestroRow> filteredClientes = new ArrayList<>();

    private final DefaultTableModel tableModelClientes;
    private final JTable tableClientes;
    private final JTextField txtBusquedaClientes;
    private TableRowSorter<DefaultTableModel> sorterClientes;

    private final JComboBox<String> cmbCliVendedor;
    private final JComboBox<String> cmbCliZona;
    private final JComboBox<String> cmbCliTipo;
    private final JComboBox<String> cmbCliSegmento;
    private final JComboBox<String> cmbCliEstado;
    private final JButton btnLimpiarFiltrosClientes;
    private final JButton btnCargarClientes;
    private final JButton btnExportarClientes;

    private final JLabel lblCliTotal;
    private final JLabel lblCliActivos;
    private final JLabel lblCliInactivos;
    private final JLabel lblCliConCredito;
    private final JLabel lblCliLimiteTotal;

    public GestionComercialFrame() {
        setTitle("DroAI — Módulo de Gestión Comercial");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1520, 920);
        setMinimumSize(new Dimension(1200, 750));
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

        // Root panel con fondo y gradiente dinámico
        JPanel root = new JPanel(new MigLayout("insets 0, fill, wrap", "[grow]", "[]0[]0[grow]")) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(tm.background());
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setPaint(new GradientPaint(0, 0, tm.gradientTop(), 0, 180, tm.gradientBottom()));
                g2.fillRect(0, 0, getWidth(), 180);
                g2.dispose();
            }
        };
        root.setOpaque(false);

        // ═══════════════════════════════════════════════════════════
        // HEADER & CONTROLES DE FECHA
        // ═══════════════════════════════════════════════════════════
        dateDesde = createStyledDatePicker();
        dateHasta = createStyledDatePicker();
        dateDesde.setDate(LocalDate.now().withDayOfMonth(1));
        dateHasta.setDate(LocalDate.now());

        btnCargarData = new JButton("🔍 Consultar Data");
        btnCargarData.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCargarData.setBackground(tm.accent());
        btnCargarData.setForeground(Color.WHITE);
        btnCargarData.setFocusPainted(false);
        btnCargarData.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCargarData.addActionListener(e -> cargarDatosBD());

        btnExportarExcel = new JButton("📥 Exportar a Excel");
        btnExportarExcel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnExportarExcel.setBackground(tm.greenAccent());
        btnExportarExcel.setForeground(tm.btnForegroundFor(tm.greenAccent()));
        btnExportarExcel.setFocusPainted(false);
        btnExportarExcel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnExportarExcel.addActionListener(e -> exportarExcel());

        root.add(buildHeaderPanel(), "growx");

        // ═══════════════════════════════════════════════════════════
        // BARRA DE FILTROS DINÁMICOS
        // ═══════════════════════════════════════════════════════════
        cmbFiltroMes = createStyledCombo("Todos los Meses");
        cmbFiltroVendedor = createStyledCombo("Todos los Vendedores");
        cmbFiltroZona = createStyledCombo("Todas las Zonas");
        cmbFiltroProveedor = createStyledCombo("Todos los Proveedores");
        cmbFiltroCliente = createStyledCombo("Todos los Clientes");

        btnLimpiarFiltros = new JButton("🧹 Limpiar Filtros");
        btnLimpiarFiltros.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnLimpiarFiltros.setBackground(tm.btnNeutralBg());
        btnLimpiarFiltros.setForeground(tm.textPrimary());
        btnLimpiarFiltros.setFocusPainted(false);
        btnLimpiarFiltros.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLimpiarFiltros.addActionListener(e -> resetearFiltros());

        root.add(buildFiltrosPanel(), "growx, gapx 20 20");

        // ═══════════════════════════════════════════════════════════
        // TABBED PANE PRINCIPAL (DASHBOARD VISUAL vs TABLA DETALLE)
        // ═══════════════════════════════════════════════════════════
        JTabbedPane mainTabs = new JTabbedPane();
        mainTabs.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Inicializar Labels KPI
        lblKpiTotalVentas = createKpiValueLabel("$0,00", tm.greenAccent());
        lblKpiTotalUnidades = createKpiValueLabel("0", tm.accent());
        lblKpiCrecVentas = createKpiValueLabel("0,00%", tm.textPrimary());
        lblKpiCrecUnidades = createKpiValueLabel("0,00%", tm.textPrimary());
        lblKpiTicketPromedio = createKpiValueLabel("$0,00", tm.textPrimary());
        lblKpiDropSize = createKpiValueLabel("0", tm.textPrimary());
        lblKpiPrecioPromedio = createKpiValueLabel("$0,00", tm.textPrimary());
        lblKpiMejorZonaNom = createKpiSubLabel("-");
        lblKpiMejorZonaVal = createKpiValueLabel("$0,00", tm.purpleAccent());
        lblKpiMejorClienteNom = createKpiSubLabel("-");
        lblKpiMejorClienteVal = createKpiValueLabel("$0,00", tm.orangeAccent());
        lblKpiMejorProveedorNom = createKpiSubLabel("-");
        lblKpiMejorProveedorVal = createKpiValueLabel("$0,00", tm.orangeAccent());
        lblKpiMejorProdValNom = createKpiSubLabel("-");
        lblKpiMejorProdValVal = createKpiValueLabel("$0,00", tm.orangeAccent());
        lblKpiMejorProdUndNom = createKpiSubLabel("-");
        lblKpiMejorProdUndVal = createKpiValueLabel("0 Unds.", tm.greenAccent());

        // Tab 1: Dashboard Visual
        JPanel pnlDashboard = new JPanel(new MigLayout("insets 16, fill, wrap", "[grow]", "[]16[grow]"));
        pnlDashboard.setOpaque(false);
        pnlDashboard.add(buildKpiGrid(), "growx");

        JScrollPane scrollDashboard = new JScrollPane(pnlDashboard);
        scrollDashboard.setOpaque(false);
        scrollDashboard.getViewport().setOpaque(false);
        scrollDashboard.setBorder(BorderFactory.createEmptyBorder());
        mainTabs.addTab("📈 Dashboard Visual", scrollDashboard);

        // Tab 2: Tabla de Detalle Productos Facturados
        String[] columnNames = {
            "N° Factura", "Mes", "Fecha", "Cliente", "Vendedor", "Código",
            "Descripción Producto", "Cantidad", "Precio ($)", "Total ($)", "Proveedor", "Zona"
        };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                if (col == 7 || col == 8 || col == 9) return Double.class;
                return String.class;
            }
        };

        tableDetalle = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        tableDetalle.setRowSorter(sorter);
        styleTable(tableDetalle);

        txtBusquedaTabla = new JTextField();
        txtBusquedaTabla.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtBusquedaTabla.putClientProperty("JTextField.placeholderText", "🔍 Búsqueda rápida en tabla...");
        txtBusquedaTabla.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterTableText(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterTableText(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterTableText(); }
        });

        JPanel pnlTablaTab = new JPanel(new MigLayout("insets 12, fill, wrap", "[grow]", "[]8[grow]"));
        pnlTablaTab.setOpaque(false);

        JPanel pnlTopTabla = new JPanel(new MigLayout("insets 0, fillx", "[grow][]", "[]"));
        pnlTopTabla.setOpaque(false);
        pnlTopTabla.add(txtBusquedaTabla, "w 300!");
        pnlTopTabla.add(btnExportarExcel, "right");

        pnlTablaTab.add(pnlTopTabla, "growx");

        JScrollPane scrollTabla = new JScrollPane(tableDetalle);
        scrollTabla.setBorder(BorderFactory.createLineBorder(tm.cardBorder()));
        pnlTablaTab.add(scrollTabla, "grow");

        mainTabs.addTab("📋 Detalle Productos Facturados", pnlTablaTab);

        // ═══════════════════════════════════════════════════════════
        // Tab 3: Maestro de Clientes
        // ═══════════════════════════════════════════════════════════
        cmbCliVendedor = createStyledComboClientes("Todos los Vendedores");
        cmbCliZona = createStyledComboClientes("Todas las Zonas");
        cmbCliTipo = createStyledComboClientes("Todos los Tipos");
        cmbCliSegmento = createStyledComboClientes("Todos los Segmentos");
        cmbCliEstado = createStyledComboClientes("Todos los Estados");
        cmbCliEstado.addItem("✔ Solo Activos");
        cmbCliEstado.addItem("⛔ Solo Inactivos");
        cmbCliEstado.addItem("💳 Con Crédito");

        btnLimpiarFiltrosClientes = new JButton("🧹 Limpiar");
        btnLimpiarFiltrosClientes.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnLimpiarFiltrosClientes.setBackground(tm.btnNeutralBg());
        btnLimpiarFiltrosClientes.setForeground(tm.textPrimary());
        btnLimpiarFiltrosClientes.setFocusPainted(false);
        btnLimpiarFiltrosClientes.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLimpiarFiltrosClientes.addActionListener(e -> resetearFiltrosClientes());

        btnCargarClientes = new JButton("🔄 Actualizar BD");
        btnCargarClientes.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnCargarClientes.setBackground(tm.accent());
        btnCargarClientes.setForeground(Color.WHITE);
        btnCargarClientes.setFocusPainted(false);
        btnCargarClientes.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCargarClientes.addActionListener(e -> cargarClientesBD());

        btnExportarClientes = new JButton("📥 Exportar Excel");
        btnExportarClientes.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnExportarClientes.setBackground(tm.greenAccent());
        btnExportarClientes.setForeground(tm.btnForegroundFor(tm.greenAccent()));
        btnExportarClientes.setFocusPainted(false);
        btnExportarClientes.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnExportarClientes.addActionListener(e -> exportarClientesExcel());

        lblCliTotal = createKpiValueLabel("0", tm.accent());
        lblCliActivos = createKpiValueLabel("0", tm.greenAccent());
        lblCliInactivos = createKpiValueLabel("0", tm.redAccent());
        lblCliConCredito = createKpiValueLabel("0", tm.purpleAccent());
        lblCliLimiteTotal = createKpiValueLabel("$0,00", tm.orangeAccent());

        String[] colsCli = {
            "Código", "R.I.F", "Nombres / Razón Social", "NIT", "F. Registro",
            "Contrib.", "Tipo", "Zona", "Ciudad", "Segmento",
            "Inactivo", "Vendedor", "Cod. Postal", "Cond. Pago", "Email",
            "Crédito", "Teléfono", "Límite ($)", "Ruta", "Tipo Persona",
            "Contacto", "Dirección"
        };
        tableModelClientes = new DefaultTableModel(colsCli, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                if (col == 17) return Double.class;
                return String.class;
            }
        };
        tableClientes = new JTable(tableModelClientes);
        sorterClientes = new TableRowSorter<>(tableModelClientes);
        tableClientes.setRowSorter(sorterClientes);
        styleTableClientes(tableClientes);

        txtBusquedaClientes = new JTextField();
        txtBusquedaClientes.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtBusquedaClientes.putClientProperty("JTextField.placeholderText", "🔍 Buscar por código, RIF, nombre, teléfono, ciudad...");
        txtBusquedaClientes.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterTableClientesText(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterTableClientesText(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterTableClientesText(); }
        });

        mainTabs.addTab("👥 Maestro de Clientes", buildMaestroClientesTab());

        root.add(mainTabs, "grow, gapx 20 20, gapbottom 16");
        setContentPane(root);

        // Cargar data automáticamente al abrir la ventana
        cargarDatosBD();
        cargarClientesBD();
    }

    // ═══════════════════════════════════════════════════════════
    //  BUILDERS DE PANELES Y COMPONENTES
    // ═══════════════════════════════════════════════════════════

    private JPanel buildHeaderPanel() {
        JPanel header = new JPanel(new MigLayout("insets 16 24 12 24, fillx", "[grow][]", "[]"));
        header.setOpaque(false);

        JPanel titleGroup = new JPanel(new MigLayout("insets 0, gap 12", "[][]", "[]"));
        titleGroup.setOpaque(false);

        JLabel lblLogo = new JLabel("💼");
        lblLogo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        titleGroup.add(lblLogo);

        JPanel textGroup = new JPanel(new MigLayout("insets 0, wrap, gap 0", "[]", "[]2[]"));
        textGroup.setOpaque(false);

        JLabel lblTitle = new JLabel("Gestión Comercial — Análisis de Productos Facturados");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(tm.textPrimary());
        textGroup.add(lblTitle);

        JLabel lblSub = new JLabel("Monitoreo dinámico de ventas, tendencias, clientes y zonas desde base de datos SQL");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(tm.textSecondary());
        textGroup.add(lblSub);

        titleGroup.add(textGroup);
        header.add(titleGroup);

        // Panel de rango de fechas
        JPanel pnlFechas = new JPanel(new MigLayout("insets 6 12 6 12, gap 8", "[][][][][]", "[]"));
        pnlFechas.setBackground(tm.cardBg());
        pnlFechas.setBorder(BorderFactory.createLineBorder(tm.cardBorder()));

        JLabel lblDesde = new JLabel("Desde:");
        lblDesde.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblDesde.setForeground(tm.textSecondary());
        pnlFechas.add(lblDesde);
        pnlFechas.add(dateDesde);

        JLabel lblHasta = new JLabel("Hasta:");
        lblHasta.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblHasta.setForeground(tm.textSecondary());
        pnlFechas.add(lblHasta);
        pnlFechas.add(dateHasta);

        pnlFechas.add(btnCargarData);

        header.add(pnlFechas, "right");
        return header;
    }

    private JPanel buildFiltrosPanel() {
        RoundedPanel pnl = new RoundedPanel(12, true);
        pnl.setBackground(tm.cardBg());
        pnl.setLayout(new MigLayout("insets 12 16 12 16, gap 10, fillx", "[grow][grow][grow][grow][grow][]", "[]2[]"));
        pnl.setBorder(BorderFactory.createLineBorder(tm.cardBorder()));

        pnl.add(createFilterLabel("📅 MES:"));
        pnl.add(createFilterLabel("🧑‍💼 VENDEDOR:"));
        pnl.add(createFilterLabel("📍 ZONA:"));
        pnl.add(createFilterLabel("🏢 PROVEEDOR:"));
        pnl.add(createFilterLabel("🏥 CLIENTE:"));
        pnl.add(new JLabel(), "wrap");

        pnl.add(cmbFiltroMes, "growx");
        pnl.add(cmbFiltroVendedor, "growx");
        pnl.add(cmbFiltroZona, "growx");
        pnl.add(cmbFiltroProveedor, "growx");
        pnl.add(cmbFiltroCliente, "growx");
        pnl.add(btnLimpiarFiltros);

        return pnl;
    }

    private JPanel buildKpiGrid() {
        JPanel grid = new JPanel(new MigLayout("insets 0, gap 12, wrap 4", "[grow, fill][grow, fill][grow, fill][grow, fill]", "[]12[]12[]"));
        grid.setOpaque(false);

        grid.add(createKpiCard("💲 Total Ventas ($)", lblKpiTotalVentas, "Acumulado del período", tm.greenAccent()));
        grid.add(createKpiCard("💊 Total Unidades", lblKpiTotalUnidades, "Acumulado de piezas", tm.accent()));
        grid.add(createKpiCard("📈 Crecimiento Ventas", lblKpiCrecVentas, "Último vs Primer Mes", tm.purpleAccent()));
        grid.add(createKpiCard("📉 Crecimiento Unidades", lblKpiCrecUnidades, "Último vs Primer Mes", tm.orangeAccent()));

        grid.add(createKpiCard("🧾 Ticket Promedio", lblKpiTicketPromedio, "Total Ventas / N° Facturas", tm.accent()));
        grid.add(createKpiCard("📦 Drop Size", lblKpiDropSize, "Unidades / N° Facturas", tm.greenAccent()));
        grid.add(createKpiCard("🏷️ Precio Promedio", lblKpiPrecioPromedio, "Total Ventas / Total Unidades", tm.orangeAccent()));
        grid.add(createKpiCardGrouped("📍 Mejor Zona ($)", lblKpiMejorZonaNom, lblKpiMejorZonaVal, tm.purpleAccent()));

        grid.add(createKpiCardGrouped("🏆 Mejor Cliente ($)", lblKpiMejorClienteNom, lblKpiMejorClienteVal, tm.orangeAccent()));
        grid.add(createKpiCardGrouped("🏢 Mejor Proveedor ($)", lblKpiMejorProveedorNom, lblKpiMejorProveedorVal, tm.accent()));
        grid.add(createKpiCardGrouped("⭐ Mejor Producto ($)", lblKpiMejorProdValNom, lblKpiMejorProdValVal, tm.orangeAccent()));
        grid.add(createKpiCardGrouped("📦 Mejor Producto (Unds)", lblKpiMejorProdUndNom, lblKpiMejorProdUndVal, tm.greenAccent()));

        return grid;
    }

    private JPanel createKpiCard(String title, JLabel lblValue, String subtitle, Color accent) {
        RoundedPanel card = new RoundedPanel(12, true);
        card.setBackground(tm.cardBg());
        card.setLayout(new MigLayout("insets 14 16 14 16, wrap, gap 2", "[grow]", "[]4[]2[]"));
        card.setBorder(BorderFactory.createMatteBorder(0, 4, 0, 0, accent));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTitle.setForeground(tm.textSecondary());
        card.add(lblTitle);

        card.add(lblValue);

        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblSub.setForeground(tm.textLabel());
        card.add(lblSub);

        return card;
    }

    private JPanel createKpiCardGrouped(String title, JLabel lblSubNom, JLabel lblValueVal, Color accent) {
        RoundedPanel card = new RoundedPanel(12, true);
        card.setBackground(tm.cardBg());
        card.setLayout(new MigLayout("insets 14 16 14 16, wrap, gap 2", "[grow]", "[]2[]4[]"));
        card.setBorder(BorderFactory.createMatteBorder(0, 4, 0, 0, accent));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTitle.setForeground(tm.textSecondary());
        card.add(lblTitle);

        card.add(lblSubNom);
        card.add(lblValueVal);

        return card;
    }

    // ═══════════════════════════════════════════════════════════
    //  LÓGICA DE NEGOCIO Y CONSULTA BD
    // ═══════════════════════════════════════════════════════════

    private void cargarDatosBD() {
        LocalDate from = dateDesde.getDate();
        LocalDate to = dateHasta.getDate();
        if (from == null || to == null) {
            Toast.show("Seleccione las fechas de inicio y fin.", Toast.Type.WARNING);
            return;
        }
        if (from.isAfter(to)) {
            Toast.show("La fecha inicial no puede ser posterior a la fecha final.", Toast.Type.WARNING);
            return;
        }

        btnCargarData.setEnabled(false);
        btnCargarData.setText("⏳ Cargando...");

        new SwingWorker<List<MatrizVentasRow>, Void>() {
            @Override
            protected List<MatrizVentasRow> doInBackground() throws Exception {
                return dao.fetchMatrizVentas(from, to);
            }

            @Override
            protected void done() {
                try {
                    rawData = get();
                    poblarCombosFiltros();
                    aplicarFiltros();
                    Toast.show("✔ " + rawData.size() + " renglones facturados cargados.", Toast.Type.SUCCESS);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Toast.show("✘ Error al consultar la base de datos: " + ex.getMessage(), Toast.Type.ERROR);
                } finally {
                    btnCargarData.setEnabled(true);
                    btnCargarData.setText("🔍 Consultar Data");
                }
            }
        }.execute();
    }

    private void poblarCombosFiltros() {
        Set<String> meses = new TreeSet<>();
        Set<String> vendedores = new TreeSet<>();
        Set<String> zonas = new TreeSet<>();
        Set<String> proveedores = new TreeSet<>();
        Set<String> clientes = new TreeSet<>();

        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.of("es", "VE"));

        for (MatrizVentasRow r : rawData) {
            if (r.getFecha() != null && !r.getFecha().isBlank()) {
                try {
                    LocalDate d = LocalDate.parse(r.getFecha().substring(0, 10));
                    meses.add(d.format(monthFmt).toUpperCase());
                } catch (Exception ignored) {}
            }
            if (r.getNombreVendedor() != null && !r.getNombreVendedor().isBlank()) vendedores.add(r.getNombreVendedor().trim().toUpperCase());
            if (r.getZona() != null && !r.getZona().isBlank()) zonas.add(r.getZona().trim().toUpperCase());
            if (r.getNombreProveedor() != null && !r.getNombreProveedor().isBlank()) proveedores.add(r.getNombreProveedor().trim().toUpperCase());
            if (r.getNombreRazonSocial() != null && !r.getNombreRazonSocial().isBlank()) clientes.add(r.getNombreRazonSocial().trim().toUpperCase());
        }

        updateComboOptions(cmbFiltroMes, "Todos los Meses", meses);
        updateComboOptions(cmbFiltroVendedor, "Todos los Vendedores", vendedores);
        updateComboOptions(cmbFiltroZona, "Todas las Zonas", zonas);
        updateComboOptions(cmbFiltroProveedor, "Todos los Proveedores", proveedores);
        updateComboOptions(cmbFiltroCliente, "Todos los Clientes", clientes);
    }

    private void updateComboOptions(JComboBox<String> combo, String defaultText, Collection<String> items) {
        combo.removeAllItems();
        combo.addItem(defaultText);
        for (String item : items) {
            combo.addItem(item);
        }
    }

    private void aplicarFiltros() {
        String selMes = (String) cmbFiltroMes.getSelectedItem();
        String selVen = (String) cmbFiltroVendedor.getSelectedItem();
        String selZon = (String) cmbFiltroZona.getSelectedItem();
        String selPro = (String) cmbFiltroProveedor.getSelectedItem();
        String selCli = (String) cmbFiltroCliente.getSelectedItem();

        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.of("es", "VE"));

        filteredData = rawData.stream().filter(r -> {
            if (selMes != null && !selMes.startsWith("Todos")) {
                try {
                    LocalDate d = LocalDate.parse(r.getFecha().substring(0, 10));
                    String m = d.format(monthFmt).toUpperCase();
                    if (!m.equals(selMes)) return false;
                } catch (Exception e) { return false; }
            }
            if (selVen != null && !selVen.startsWith("Todos") && !r.getNombreVendedor().trim().equalsIgnoreCase(selVen)) return false;
            if (selZon != null && !selZon.startsWith("Todos") && !r.getZona().trim().equalsIgnoreCase(selZon)) return false;
            if (selPro != null && !selPro.startsWith("Todos") && !r.getNombreProveedor().trim().equalsIgnoreCase(selPro)) return false;
            if (selCli != null && !selCli.startsWith("Todos") && !r.getNombreRazonSocial().trim().equalsIgnoreCase(selCli)) return false;
            return true;
        }).collect(Collectors.toList());

        recalcularKpis();
        poblarTablaDetalle();
    }

    private void resetearFiltros() {
        cmbFiltroMes.setSelectedIndex(0);
        cmbFiltroVendedor.setSelectedIndex(0);
        cmbFiltroZona.setSelectedIndex(0);
        cmbFiltroProveedor.setSelectedIndex(0);
        cmbFiltroCliente.setSelectedIndex(0);
        aplicarFiltros();
    }

    private void recalcularKpis() {
        double totalVentas = 0;
        double totalUnidades = 0;
        Set<String> facturas = new HashSet<>();
        Map<String, Double> vZona = new HashMap<>();
        Map<String, Double> vCli = new HashMap<>();
        Map<String, Double> vProv = new HashMap<>();
        Map<String, Double> vProd = new HashMap<>();
        Map<String, Double> uProd = new HashMap<>();
        Map<String, Double> vMes = new TreeMap<>();
        Map<String, Double> uMes = new TreeMap<>();

        for (MatrizVentasRow r : filteredData) {
            double valor = r.getTotalRenglon();
            double unidad = r.getCantidad();
            totalVentas += valor;
            totalUnidades += unidad;
            if (r.getNumero() != null) facturas.add(r.getNumero());

            String zona = (r.getZona() == null || r.getZona().isBlank()) ? "SIN ZONA" : r.getZona().trim().toUpperCase();
            String cliente = (r.getNombreRazonSocial() == null || r.getNombreRazonSocial().isBlank()) ? "DESCONOCIDO" : r.getNombreRazonSocial().trim().toUpperCase();
            String proveedor = (r.getNombreProveedor() == null || r.getNombreProveedor().isBlank()) ? "DESCONOCIDO" : r.getNombreProveedor().trim().toUpperCase();
            String producto = (r.getDescripcion() == null || r.getDescripcion().isBlank()) ? "DESCONOCIDO" : r.getDescripcion().trim().toUpperCase();
            String fechaMes = (r.getFecha() != null && r.getFecha().length() >= 7) ? r.getFecha().substring(0, 7) : "OTRO";

            vZona.put(zona, vZona.getOrDefault(zona, 0.0) + valor);
            vCli.put(cliente, vCli.getOrDefault(cliente, 0.0) + valor);
            vProv.put(proveedor, vProv.getOrDefault(proveedor, 0.0) + valor);
            vProd.put(producto, vProd.getOrDefault(producto, 0.0) + valor);
            uProd.put(producto, uProd.getOrDefault(producto, 0.0) + unidad);

            vMes.put(fechaMes, vMes.getOrDefault(fechaMes, 0.0) + valor);
            uMes.put(fechaMes, uMes.getOrDefault(fechaMes, 0.0) + unidad);
        }

        int nFacturas = Math.max(1, facturas.size());

        lblKpiTotalVentas.setText(fmtCurrency.format(totalVentas));
        lblKpiTotalUnidades.setText(fmtNumber.format(totalUnidades));
        lblKpiTicketPromedio.setText(fmtCurrency.format(totalVentas / nFacturas));
        lblKpiDropSize.setText(fmtNumber.format(Math.round(totalUnidades / nFacturas)));
        lblKpiPrecioPromedio.setText(fmtCurrency.format(totalVentas / (totalUnidades > 0 ? totalUnidades : 1)));

        // Crecimiento Ventas y Unidades % (Último vs Primer mes)
        List<String> mesesOrdenados = new ArrayList<>(vMes.keySet());
        if (mesesOrdenados.size() >= 2) {
            double vPri = vMes.get(mesesOrdenados.get(0));
            double vUlt = vMes.get(mesesOrdenados.get(mesesOrdenados.size() - 1));
            double crecV = vPri > 0 ? ((vUlt - vPri) / vPri) * 100.0 : 0.0;
            lblKpiCrecVentas.setText(String.format("%+.2f%%", crecV));
            lblKpiCrecVentas.setForeground(crecV >= 0 ? tm.greenAccent() : tm.redAccent());

            double uPri = uMes.get(mesesOrdenados.get(0));
            double uUlt = uMes.get(mesesOrdenados.get(mesesOrdenados.size() - 1));
            double crecU = uPri > 0 ? ((uUlt - uPri) / uPri) * 100.0 : 0.0;
            lblKpiCrecUnidades.setText(String.format("%+.2f%%", crecU));
            lblKpiCrecUnidades.setForeground(crecU >= 0 ? tm.greenAccent() : tm.redAccent());
        } else {
            lblKpiCrecVentas.setText("N/A");
            lblKpiCrecUnidades.setText("N/A");
        }

        // Tops
        Map.Entry<String, Double> topZ = getTopEntry(vZona);
        lblKpiMejorZonaNom.setText(truncateText(topZ.getKey(), 24));
        lblKpiMejorZonaVal.setText(fmtCurrency.format(topZ.getValue()));

        Map.Entry<String, Double> topC = getTopEntry(vCli);
        lblKpiMejorClienteNom.setText(truncateText(topC.getKey(), 24));
        lblKpiMejorClienteVal.setText(fmtCurrency.format(topC.getValue()));

        Map.Entry<String, Double> topPr = getTopEntry(vProv);
        lblKpiMejorProveedorNom.setText(truncateText(topPr.getKey(), 24));
        lblKpiMejorProveedorVal.setText(fmtCurrency.format(topPr.getValue()));

        Map.Entry<String, Double> topPdV = getTopEntry(vProd);
        lblKpiMejorProdValNom.setText(truncateText(topPdV.getKey(), 24));
        lblKpiMejorProdValVal.setText(fmtCurrency.format(topPdV.getValue()));

        Map.Entry<String, Double> topPdU = getTopEntry(uProd);
        lblKpiMejorProdUndNom.setText(truncateText(topPdU.getKey(), 24));
        lblKpiMejorProdUndVal.setText(fmtNumber.format(topPdU.getValue()) + " Unds.");
    }

    private Map.Entry<String, Double> getTopEntry(Map<String, Double> map) {
        return map.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .orElse(new AbstractMap.SimpleEntry<>("-", 0.0));
    }

    private void poblarTablaDetalle() {
        tableModel.setRowCount(0);
        for (MatrizVentasRow r : filteredData) {
            tableModel.addRow(new Object[]{
                r.getNumero(),
                r.getMes(),
                r.getFecha(),
                r.getNombreRazonSocial(),
                r.getNombreVendedor(),
                r.getCodigoArt(),
                r.getDescripcion(),
                r.getCantidad(),
                r.getPrecio(),
                r.getTotalRenglon(),
                r.getNombreProveedor(),
                r.getZona()
            });
        }
    }

    private void filterTableText() {
        String text = txtBusquedaTabla.getText().trim();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
        }
    }

    private void exportarExcel() {
        if (filteredData.isEmpty()) {
            Toast.show("No hay datos para exportar.", Toast.Type.WARNING);
            return;
        }
        try {
            ExcelExporter exporter = new ExcelExporter();
            File saved = exporter.exportProductosFacturados(filteredData);
            Toast.show("✔ Archivo Excel exportado: " + saved.getName(), Toast.Type.SUCCESS);
            Desktop.getDesktop().open(saved);
        } catch (Exception ex) {
            ex.printStackTrace();
            Toast.show("✘ Error al exportar a Excel: " + ex.getMessage(), Toast.Type.ERROR);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  HELPERS DE UI Y ESTILADO
    // ═══════════════════════════════════════════════════════════

    private DatePicker createStyledDatePicker() {
        DatePickerSettings settings = new DatePickerSettings(Locale.of("es", "VE"));
        settings.setFormatForDatesCommonEra("dd/MM/yyyy");
        settings.setAllowEmptyDates(false);
        settings.setColor(DatePickerSettings.DateArea.TextFieldBackgroundValidDate, tm.bgField());
        settings.setColor(DatePickerSettings.DateArea.DatePickerTextValidDate, tm.textPrimary());
        settings.setColor(DatePickerSettings.DateArea.CalendarBackgroundNormalDates, tm.cardBg());
        settings.setColor(DatePickerSettings.DateArea.CalendarTextNormalDates, tm.textPrimary());
        settings.setColor(DatePickerSettings.DateArea.BackgroundOverallCalendarPanel, tm.cardBg());
        settings.setColor(DatePickerSettings.DateArea.CalendarBackgroundSelectedDate, tm.accent());
        settings.setFontValidDate(new Font("Segoe UI", Font.PLAIN, 12));
        DatePicker dp = new DatePicker(settings);
        dp.setPreferredSize(new Dimension(140, 28));
        return dp;
    }

    private JComboBox<String> createStyledCombo(String defaultText) {
        JComboBox<String> cb = new JComboBox<>();
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        cb.setBackground(tm.cardBg());
        cb.setForeground(tm.textPrimary());
        cb.setPreferredSize(new Dimension(160, 28));
        cb.addItem(defaultText);
        cb.addActionListener(e -> {
            if (cb.isFocusOwner()) aplicarFiltros();
        });
        return cb;
    }

    private JLabel createFilterLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setForeground(tm.textSecondary());
        return lbl;
    }

    private JLabel createKpiValueLabel(String text, Color fg) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lbl.setForeground(fg);
        return lbl;
    }

    private JLabel createKpiSubLabel(String text) {
        JLabel lbl = new JLabel(truncateText(text, 24));
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(tm.textPrimary());
        return lbl;
    }

    private String truncateText(String str, int maxLen) {
        if (str == null) return "-";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 2) + "..";
    }

    private void styleTable(JTable t) {
        t.setBackground(tm.tableBg());
        t.setForeground(tm.textPrimary());
        t.setGridColor(tm.cardBorder());
        t.setRowHeight(26);
        t.getTableHeader().setBackground(tm.tableHeader());
        t.getTableHeader().setForeground(tm.textPrimary());
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);

        DefaultTableCellRenderer currencyRenderer = new DefaultTableCellRenderer() {
            @Override
            public void setValue(Object value) {
                if (value instanceof Number n) {
                    setText(fmtCurrency.format(n.doubleValue()));
                } else {
                    super.setValue(value);
                }
            }
        };
        currencyRenderer.setHorizontalAlignment(SwingConstants.RIGHT);

        if (t.getColumnCount() >= 9) {
            t.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);
            t.getColumnModel().getColumn(7).setCellRenderer(currencyRenderer);
            t.getColumnModel().getColumn(8).setCellRenderer(currencyRenderer);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  PESTAÑA 3: MAESTRO DE CLIENTES — MÉTODOS Y CONTROLADORES
    // ═══════════════════════════════════════════════════════════

    private JPanel buildMaestroClientesTab() {
        JPanel tab = new JPanel(new MigLayout("insets 12, fill, wrap", "[grow]", "[]8[]8[grow]"));
        tab.setOpaque(false);

        // 1. Barra de Filtros y Búsqueda
        RoundedPanel pnlTop = new RoundedPanel(12, true);
        pnlTop.setBackground(tm.cardBg());
        pnlTop.setLayout(new MigLayout("insets 10 14 10 14, fillx, gap 8", "[200:260,grow][grow][grow][grow][grow][grow][][][]", "[]2[]"));
        pnlTop.setBorder(BorderFactory.createLineBorder(tm.cardBorder()));

        pnlTop.add(createFilterLabel("🔍 BÚSQUEDA RÁPIDA:"));
        pnlTop.add(createFilterLabel("🧑‍💼 VENDEDOR:"));
        pnlTop.add(createFilterLabel("📍 ZONA:"));
        pnlTop.add(createFilterLabel("🏢 TIPO:"));
        pnlTop.add(createFilterLabel("🏷️ SEGMENTO:"));
        pnlTop.add(createFilterLabel("📌 ESTADO:"));
        pnlTop.add(new JLabel(), "span 3, wrap");

        pnlTop.add(txtBusquedaClientes, "growx");
        pnlTop.add(cmbCliVendedor, "growx");
        pnlTop.add(cmbCliZona, "growx");
        pnlTop.add(cmbCliTipo, "growx");
        pnlTop.add(cmbCliSegmento, "growx");
        pnlTop.add(cmbCliEstado, "growx");
        pnlTop.add(btnLimpiarFiltrosClientes);
        pnlTop.add(btnCargarClientes);
        pnlTop.add(btnExportarClientes);

        tab.add(pnlTop, "growx");

        // 2. Tira de KPIs de Resumen
        JPanel pnlKpis = new JPanel(new MigLayout("insets 0, fillx, gap 10", "[grow, fill][grow, fill][grow, fill][grow, fill][grow, fill]", "[]"));
        pnlKpis.setOpaque(false);

        pnlKpis.add(createMiniKpiCard("👥 Total Clientes", lblCliTotal, tm.accent()));
        pnlKpis.add(createMiniKpiCard("✔ Clientes Activos", lblCliActivos, tm.greenAccent()));
        pnlKpis.add(createMiniKpiCard("⛔ Clientes Inactivos", lblCliInactivos, tm.redAccent()));
        pnlKpis.add(createMiniKpiCard("💳 Con Crédito", lblCliConCredito, tm.purpleAccent()));
        pnlKpis.add(createMiniKpiCard("💰 Límite Crédito Total", lblCliLimiteTotal, tm.orangeAccent()));

        tab.add(pnlKpis, "growx");

        // 3. Tabla de Clientes con Scroll
        JScrollPane scroll = new JScrollPane(tableClientes);
        scroll.setBorder(BorderFactory.createLineBorder(tm.cardBorder()));
        tab.add(scroll, "grow");

        return tab;
    }

    private JPanel createMiniKpiCard(String title, JLabel lblValue, Color accent) {
        RoundedPanel card = new RoundedPanel(10, true);
        card.setBackground(tm.cardBg());
        card.setLayout(new MigLayout("insets 8 14 8 14, wrap, gap 1", "[grow]", "[]2[]"));
        card.setBorder(BorderFactory.createMatteBorder(0, 3, 0, 0, accent));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblTitle.setForeground(tm.textSecondary());
        card.add(lblTitle);

        card.add(lblValue);
        return card;
    }

    private JComboBox<String> createStyledComboClientes(String defaultText) {
        JComboBox<String> cb = new JComboBox<>();
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        cb.setBackground(tm.cardBg());
        cb.setForeground(tm.textPrimary());
        cb.setPreferredSize(new Dimension(150, 28));
        cb.addItem(defaultText);
        cb.addActionListener(e -> {
            if (cb.isFocusOwner()) aplicarFiltrosClientes();
        });
        return cb;
    }

    private void styleTableClientes(JTable t) {
        t.setBackground(tm.tableBg());
        t.setForeground(tm.textPrimary());
        t.setGridColor(tm.cardBorder());
        t.setRowHeight(26);
        t.getTableHeader().setBackground(tm.tableHeader());
        t.getTableHeader().setForeground(tm.textPrimary());
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer currencyRenderer = new DefaultTableCellRenderer() {
            @Override
            public void setValue(Object value) {
                if (value instanceof Number n) {
                    setText(fmtCurrency.format(n.doubleValue()));
                } else {
                    super.setValue(value);
                }
            }
        };
        currencyRenderer.setHorizontalAlignment(SwingConstants.RIGHT);

        DefaultTableCellRenderer inactivoRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                String val = (value != null) ? value.toString().trim() : "";
                if ("SI".equalsIgnoreCase(val)) {
                    setForeground(tm.redAccent());
                    setText("⛔ Inactivo");
                } else {
                    setForeground(tm.greenAccent());
                    setText("✔ Activo");
                }
                if (isSelected) {
                    setBackground(table.getSelectionBackground());
                } else {
                    setBackground(row % 2 == 0 ? tm.tableBg() : tm.tableAlt());
                }
                return c;
            }
        };

        DefaultTableCellRenderer creditoRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                String val = (value != null) ? value.toString().trim() : "";
                if ("SI".equalsIgnoreCase(val)) {
                    setForeground(tm.accent());
                    setText("💳 SI");
                } else {
                    setForeground(tm.textSecondary());
                    setText("NO");
                }
                if (isSelected) {
                    setBackground(table.getSelectionBackground());
                } else {
                    setBackground(row % 2 == 0 ? tm.tableBg() : tm.tableAlt());
                }
                return c;
            }
        };

        if (t.getColumnCount() >= 22) {
            t.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);  // Código
            t.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);  // RIF
            t.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);  // NIT
            t.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);  // F. Registro
            t.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);  // Contrib.
            t.getColumnModel().getColumn(10).setCellRenderer(inactivoRenderer); // Inactivo
            t.getColumnModel().getColumn(15).setCellRenderer(creditoRenderer);  // Crédito
            t.getColumnModel().getColumn(17).setCellRenderer(currencyRenderer); // Límite ($)
        }
    }

    private void cargarClientesBD() {
        btnCargarClientes.setEnabled(false);
        btnCargarClientes.setText("⏳ Cargando...");

        new SwingWorker<List<ClienteMaestroRow>, Void>() {
            @Override
            protected List<ClienteMaestroRow> doInBackground() throws Exception {
                return clienteDAO.obtenerMaestroClientes();
            }

            @Override
            protected void done() {
                btnCargarClientes.setEnabled(true);
                btnCargarClientes.setText("🔄 Actualizar BD");
                try {
                    rawClientes = get();
                    poblarCombosClientes();
                    aplicarFiltrosClientes();
                    Toast.show("✔ Maestro de Clientes cargado: " + rawClientes.size() + " clientes.", Toast.Type.SUCCESS);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Toast.show("✘ Error al cargar clientes: " + ex.getMessage(), Toast.Type.ERROR);
                }
            }
        }.execute();
    }

    private void poblarCombosClientes() {
        Set<String> vendedores = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> zonas = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> tipos = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> segmentos = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (ClienteMaestroRow r : rawClientes) {
            if (!r.getVendedor().isEmpty()) vendedores.add(r.getVendedor());
            if (!r.getZona().isEmpty()) zonas.add(r.getZona());
            if (!r.getTipoCliente().isEmpty()) tipos.add(r.getTipoCliente());
            if (!r.getSegmento().isEmpty()) segmentos.add(r.getSegmento());
        }

        updateComboOptions(cmbCliVendedor, "Todos los Vendedores", vendedores);
        updateComboOptions(cmbCliZona, "Todas las Zonas", zonas);
        updateComboOptions(cmbCliTipo, "Todos los Tipos", tipos);
        updateComboOptions(cmbCliSegmento, "Todos los Segmentos", segmentos);
    }

    private void aplicarFiltrosClientes() {
        String selVen = (String) cmbCliVendedor.getSelectedItem();
        String selZon = (String) cmbCliZona.getSelectedItem();
        String selTip = (String) cmbCliTipo.getSelectedItem();
        String selSeg = (String) cmbCliSegmento.getSelectedItem();
        String selEst = (String) cmbCliEstado.getSelectedItem();

        filteredClientes = rawClientes.stream().filter(r -> {
            if (selVen != null && !selVen.startsWith("Todos") && !r.getVendedor().equalsIgnoreCase(selVen)) return false;
            if (selZon != null && !selZon.startsWith("Todas") && !r.getZona().equalsIgnoreCase(selZon)) return false;
            if (selTip != null && !selTip.startsWith("Todos") && !r.getTipoCliente().equalsIgnoreCase(selTip)) return false;
            if (selSeg != null && !selSeg.startsWith("Todos") && !r.getSegmento().equalsIgnoreCase(selSeg)) return false;
            if (selEst != null) {
                if (selEst.contains("Solo Activos") && !r.isActivo()) return false;
                if (selEst.contains("Solo Inactivos") && r.isActivo()) return false;
                if (selEst.contains("Con Crédito") && !"SI".equalsIgnoreCase(r.getCredito())) return false;
            }
            return true;
        }).collect(Collectors.toList());

        poblarTablaClientes();
        calcularKpisClientes();
        filterTableClientesText();
    }

    private void poblarTablaClientes() {
        tableModelClientes.setRowCount(0);
        for (ClienteMaestroRow r : filteredClientes) {
            tableModelClientes.addRow(new Object[]{
                r.getCodigo(),
                r.getRif(),
                r.getNombre(),
                r.getNit(),
                r.getFechaRegistro(),
                r.getContribuyente(),
                r.getTipoCliente(),
                r.getZona(),
                r.getCiudad(),
                r.getSegmento(),
                r.getInactivo(),
                r.getVendedor(),
                r.getCodPostal(),
                r.getCondPago(),
                r.getEmail(),
                r.getCredito(),
                r.getTelefono(),
                r.getLimiteCredito(),
                r.getRuta(),
                r.getTipoPersona(),
                r.getContacto(),
                r.getDireccion()
            });
        }
    }

    private void calcularKpisClientes() {
        int total = filteredClientes.size();
        long activos = filteredClientes.stream().filter(ClienteMaestroRow::isActivo).count();
        long inactivos = total - activos;
        long conCredito = filteredClientes.stream().filter(r -> "SI".equalsIgnoreCase(r.getCredito())).count();
        double limiteTotal = filteredClientes.stream().mapToDouble(ClienteMaestroRow::getLimiteCredito).sum();

        lblCliTotal.setText(fmtNumber.format(total));
        lblCliActivos.setText(fmtNumber.format(activos));
        lblCliInactivos.setText(fmtNumber.format(inactivos));
        lblCliConCredito.setText(fmtNumber.format(conCredito));
        lblCliLimiteTotal.setText(fmtCurrency.format(limiteTotal));
    }

    private void filterTableClientesText() {
        String text = txtBusquedaClientes.getText().trim();
        if (text.isEmpty()) {
            sorterClientes.setRowFilter(null);
        } else {
            sorterClientes.setRowFilter(RowFilter.regexFilter("(?i)" + text));
        }
    }

    private void resetearFiltrosClientes() {
        txtBusquedaClientes.setText("");
        if (cmbCliVendedor.getItemCount() > 0) cmbCliVendedor.setSelectedIndex(0);
        if (cmbCliZona.getItemCount() > 0) cmbCliZona.setSelectedIndex(0);
        if (cmbCliTipo.getItemCount() > 0) cmbCliTipo.setSelectedIndex(0);
        if (cmbCliSegmento.getItemCount() > 0) cmbCliSegmento.setSelectedIndex(0);
        if (cmbCliEstado.getItemCount() > 0) cmbCliEstado.setSelectedIndex(0);
        aplicarFiltrosClientes();
    }

    private void exportarClientesExcel() {
        if (filteredClientes.isEmpty()) {
            Toast.show("No hay datos de clientes para exportar.", Toast.Type.WARNING);
            return;
        }
        try {
            ExcelExporter exporter = new ExcelExporter();
            File saved = exporter.exportMaestroClientes(filteredClientes);
            Toast.show("✔ Archivo Excel exportado: " + saved.getName(), Toast.Type.SUCCESS);
            Desktop.getDesktop().open(saved);
        } catch (Exception ex) {
            ex.printStackTrace();
            Toast.show("✘ Error al exportar a Excel: " + ex.getMessage(), Toast.Type.ERROR);
        }
    }
}
