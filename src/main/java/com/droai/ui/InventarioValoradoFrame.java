package com.droai.ui;

import com.droai.dao.InventarioValoracionDAO;
import com.droai.export.InventarioValoracionExcelExporter;
import com.droai.model.CostoVentaRow;
import com.droai.model.CostoVentasPeriodoResult;
import com.droai.model.InventarioValoradoRow;
import com.droai.model.KardexMovimientoValoradoRow;
import com.droai.ui.components.RoundedPanel;
import com.droai.ui.components.Toast;
import com.droai.ui.dialog.CapasCostoDialog;
import com.droai.ui.util.IconHelper;
import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
 * Módulo de Valuación de Inventario, Kárdex y Costo de Ventas Multimoneda.
 *
 * <p>Permite:
 * <ul>
 *   <li>Reconstruir la existencia y valuación al corte por Capas FIFO (PEPS) para Venta Firme con enfoque dual (Histórico vs. Reposición).</li>
 *   <li>Valuar la mercancía en consignación al último costo reportado por proveedores.</li>
 *   <li>Consultar el Kárdex de compras de un producto con saldos y existencias al corte.</li>
 *   <li>Determinar el Costo de Ventas y Margen de Utilidad del período discriminando Venta Firme y Consignación.</li>
 *   <li>Exportar a Excel oficial con formatos ejecutivos de auditoría.</li>
 * </ul>
 */
public class InventarioValoradoFrame extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(InventarioValoradoFrame.class);
    private final ThemeManager tm = ThemeManager.get();
    private final InventarioValoracionDAO dao = new InventarioValoracionDAO();

    private static final DecimalFormat NF_DEC = new DecimalFormat("#,##0.00");
    private static final DecimalFormat NF_USD = new DecimalFormat("$#,##0.00");
    private static final DecimalFormat NF_BS = new DecimalFormat("'Bs.' #,##0.00");
    private static final DecimalFormat NF_PCT = new DecimalFormat("#,##0.00'%'");

    // ── Componentes Tab 1: Valoración General ──
    private DatePicker dpFechaCorte;
    private JLabel lblDbBadge;
    private JComboBox<String> cbTipo;
    private JComboBox<String> cbAlmacen;
    private JTextField txtBuscar;
    private JButton btnCalcular;
    private JButton btnExportar;
    private JTable tablaInventario;
    private DefaultTableModel modelInventario;
    private TableRowSorter<DefaultTableModel> sorterInventario;
    private JLabel lblCoincidencias;
    private List<InventarioValoradoRow> listaActual = new ArrayList<>();
    private double tasaCorteActual = 1.0;

    // Tarjetas KPI Tab 1
    private JLabel lblKpiItems;
    private JLabel lblKpiUnds;
    private JLabel lblKpiUndsDetalle;
    private JLabel lblKpiFirmeUSD;
    private JLabel lblKpiFirmeBS;
    private JLabel lblKpiConsigUSD;
    private JLabel lblKpiConsigBS;
    private JLabel lblKpiGlobalUSD;
    private JLabel lblKpiGlobalBS;
    private JLabel lblKpiTasa;

    // ── Componentes Tab 2: Kárdex ──
    private JTextField txtKardexArt;
    private DatePicker dpKardexDesde;
    private DatePicker dpKardexHasta;
    private JLabel lblKardexDbBadge;
    private JComboBox<String> cbKardexAlma;
    private JButton btnConsultarKardex;
    private JTable tablaKardex;
    private DefaultTableModel modelKardex;
    private JLabel lblKardexProdInfo;
    private JLabel lblKardexExistencia;
    private JLabel lblKardexCostoPromUSD;
    private JLabel lblKardexCostoPromBS;
    private JLabel lblKardexTotalUSD;
    private JLabel lblKardexTotalBS;

    // ── Componentes Tab 3: Costo de Ventas y Margen del Período ──
    private DatePicker dpCostoDesde;
    private DatePicker dpCostoHasta;
    private JLabel lblCostoDbBadge;
    private JComboBox<String> cbCostoTipo;
    private JComboBox<String> cbCostoAlmacen;
    private JTextField txtCostoBuscar;
    private JButton btnCalcularCosto;
    private JButton btnExportarCosto;
    private JTable tablaCostoVentas;
    private DefaultTableModel modelCostoVentas;
    private TableRowSorter<DefaultTableModel> sorterCostoVentas;
    private CostoVentasPeriodoResult costoVentasActual = new CostoVentasPeriodoResult();

    // Tarjetas KPI Tab 3
    private JLabel lblCostoKpiVentasUSD;
    private JLabel lblCostoKpiVentasBS;
    private JLabel lblCostoKpiCostoUSD;
    private JLabel lblCostoKpiCostoDetalle;
    private JLabel lblCostoKpiUtilUSD;
    private JLabel lblCostoKpiUtilBS;
    private JLabel lblCostoKpiMargen;
    private JLabel lblCostoKpiUnds;

    // Pestañas
    private JTabbedPane tabbedPane;

    public InventarioValoradoFrame() {
        setTitle("DroAI — Valuación de Inventario, Kárdex y Costo de Ventas");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1360, 860);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);
        IconHelper.applyAppIcon(this);

        buildUI();
        tm.addThemeChangeListener(this::repaint);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(tm.background());

        // ═══════════════════════════════════════════════════════════
        // HEADER
        // ═══════════════════════════════════════════════════════════
        JPanel header = new JPanel(new MigLayout("insets 16 28 12 28, fillx", "[grow][]", "[]2[]"));
        header.setBackground(tm.cardBg());
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, tm.border()));

        JLabel lblTitle = new JLabel("Valuación de Inventario, Kárdex y Costo de Ventas");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(tm.textPrimary());
        header.add(lblTitle, "wrap");

        JLabel lblSub = new JLabel("Existencia al Corte • Capas FIFO • Mercancía en Consignación • Costo de Ventas del Período • Enfoque Multimoneda ($ / Bs.)");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(tm.textSecondary());
        header.add(lblSub, "growx");

        JButton btnCerrar = new JButton("← Volver al Menú");
        btnCerrar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCerrar.setBackground(tm.btnNeutralBg());
        btnCerrar.setForeground(tm.textPrimary());
        btnCerrar.setFocusPainted(false);
        btnCerrar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCerrar.addActionListener(e -> dispose());
        header.add(btnCerrar, "right");

        root.add(header, BorderLayout.NORTH);

        // ═══════════════════════════════════════════════════════════
        // TABS PRINCIPALES
        // ═══════════════════════════════════════════════════════════
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.addTab("📦 Valoración Global de Inventario", buildTabValoracion());
        tabbedPane.addTab("📋 Capas FIFO del Producto", buildTabKardex());
        tabbedPane.addTab("📈 Costo de Ventas del Período", buildTabCostoVentas());

        root.add(tabbedPane, BorderLayout.CENTER);

        setContentPane(root);
    }

    // ═══════════════════════════════════════════════════════════════
    // TAB 1: VALORACIÓN GLOBAL DE INVENTARIO
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildTabValoracion() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(tm.background());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 20, 16, 20));

        // ── Barra Superior de Filtros y Acciones ──
        JPanel filterBar = new RoundedPanel(12, true);
        filterBar.setBackground(tm.cardBg());
        filterBar.setLayout(new MigLayout("insets 12 16 12 16, fillx, wrap", "[grow]", "[]8[]"));
        filterBar.setBorder(BorderFactory.createLineBorder(tm.border(), 1));

        // Fila 1: Filtros de fecha, tipo, almacén y acciones principales
        JPanel row1 = new JPanel(new MigLayout("insets 0, fillx, gap 8", "[][][][][][][][][grow][]", "[]"));
        row1.setOpaque(false);

        JLabel lblFc = new JLabel("Fecha Corte:");
        lblFc.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblFc.setForeground(tm.textPrimary());
        row1.add(lblFc);

        dpFechaCorte = createStyledDatePicker();
        LocalDate finMesAnterior = YearMonth.now().minusMonths(1).atEndOfMonth();
        dpFechaCorte.setDate(finMesAnterior);
        row1.add(dpFechaCorte, "w 150!, h 32!");

        JButton btnFinMesAnt = new JButton("Fin Mes Ant.");
        btnFinMesAnt.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnFinMesAnt.setBackground(tm.btnNeutralBg());
        btnFinMesAnt.setForeground(tm.textPrimary());
        btnFinMesAnt.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnFinMesAnt.addActionListener(e -> dpFechaCorte.setDate(YearMonth.now().minusMonths(1).atEndOfMonth()));
        row1.add(btnFinMesAnt, "h 30!");

        JButton btnHoy = new JButton("Hoy");
        btnHoy.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnHoy.setBackground(tm.btnNeutralBg());
        btnHoy.setForeground(tm.textPrimary());
        btnHoy.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnHoy.addActionListener(e -> dpFechaCorte.setDate(LocalDate.now()));
        row1.add(btnHoy, "h 30!");

        lblDbBadge = new JLabel();
        lblDbBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblDbBadge.setOpaque(true);
        lblDbBadge.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        actualizarBadgeDb(dpFechaCorte.getDate());
        row1.add(lblDbBadge);

        dpFechaCorte.addDateChangeListener(event -> {
            actualizarBadgeDb(event.getNewDate());
            if (dpKardexHasta != null && event.getNewDate() != null) {
                dpKardexHasta.setDate(event.getNewDate());
                dpKardexDesde.setDate(LocalDate.of(event.getNewDate().getYear(), 1, 1));
            }
        });

        JLabel lblTipo = new JLabel("Tipo:");
        lblTipo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTipo.setForeground(tm.textPrimary());
        row1.add(lblTipo);

        cbTipo = new JComboBox<>(new String[]{"TODOS", "VENTA FIRME (Propia)", "CONSIGNACION"});
        cbTipo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        row1.add(cbTipo, "w 160!");

        JLabel lblAlm = new JLabel("Almacén:");
        lblAlm.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblAlm.setForeground(tm.textPrimary());
        row1.add(lblAlm);

        cbAlmacen = new JComboBox<>(new String[]{
                "TODOS", "01 - PRINCIPAL", "02 - CONSIGNACION", "B2 - PULMON BO",
                "PM - PULMON MED", "PV - PULMON VARIOS", "C1 - ESPECIALES",
                "E1 - PSICOTROPICOS", "VC - VENC. CONSIGNACION", "VP - VENC. PROPIO", "AV - AVERIA"
        });
        cbAlmacen.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        row1.add(cbAlmacen, "w 170!");

        btnCalcular = new JButton("⚡ Calcular y Valuar");
        btnCalcular.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCalcular.setBackground(tm.accent());
        btnCalcular.setForeground(Color.WHITE);
        btnCalcular.setFocusPainted(false);
        btnCalcular.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCalcular.addActionListener(e -> ejecutarCalculoValoracion());
        row1.add(btnCalcular, "h 32!");

        btnExportar = new JButton("📊 Exportar Excel");
        btnExportar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnExportar.setBackground(tm.greenAccent());
        btnExportar.setForeground(Color.WHITE);
        btnExportar.setFocusPainted(false);
        btnExportar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnExportar.addActionListener(e -> exportarExcel());
        row1.add(btnExportar, "h 32!");

        filterBar.add(row1, "growx");

        // Fila 2: Búsqueda rápida de producto en tiempo real
        JPanel rowSearch = new JPanel(new MigLayout("insets 0, fillx, gap 8", "[][grow][][]", "[]"));
        rowSearch.setOpaque(false);

        JLabel lblBuscar = new JLabel("🔍 Buscar en Valoración:");
        lblBuscar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblBuscar.setForeground(tm.accent());
        rowSearch.add(lblBuscar);

        txtBuscar = new JTextField();
        txtBuscar.putClientProperty("JTextField.placeholderText", "Escriba código (ej. 004930), descripción o proveedor para encontrar el producto al instante...");
        txtBuscar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filtrarEnTabla(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filtrarEnTabla(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filtrarEnTabla(); }
        });
        txtBuscar.addActionListener(e -> filtrarEnTabla());
        rowSearch.add(txtBuscar, "growx, h 30!");

        JButton btnLimpiar = new JButton("✖ Limpiar");
        btnLimpiar.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnLimpiar.setBackground(tm.btnNeutralBg());
        btnLimpiar.setForeground(tm.textPrimary());
        btnLimpiar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLimpiar.addActionListener(e -> {
            txtBuscar.setText("");
            filtrarEnTabla();
            txtBuscar.requestFocusInWindow();
        });
        rowSearch.add(btnLimpiar, "h 30!");

        lblCoincidencias = new JLabel("0 productos");
        lblCoincidencias.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblCoincidencias.setForeground(tm.textSecondary());
        rowSearch.add(lblCoincidencias);

        filterBar.add(rowSearch, "growx");

JPanel kpiContainer = new JPanel(new GridLayout(1, 6, 12, 0));
        kpiContainer.setOpaque(false);

        kpiContainer.add(createKpiCard("📦 Total Artículos", lblKpiItems = new JLabel("0"), null));
        kpiContainer.add(createKpiCard("🔢 Total Unidades", lblKpiUnds = new JLabel("0.00"), lblKpiUndsDetalle = new JLabel("Firme: 0,00  |  Consig: 0,00")));
        kpiContainer.add(createKpiCard("🏢 Venta Firme", lblKpiFirmeUSD = new JLabel("$0.00"), lblKpiFirmeBS = new JLabel("Hist: Bs. 0.00")));
        kpiContainer.add(createKpiCard("🤝 Consignación", lblKpiConsigUSD = new JLabel("$0.00"), lblKpiConsigBS = new JLabel("Bs. 0.00")));
        kpiContainer.add(createKpiCard("💰 Total Global", lblKpiGlobalUSD = new JLabel("$0.00"), lblKpiGlobalBS = new JLabel("Bs. 0.00")));
        kpiContainer.add(createKpiCard("💵 Tasa BCV al Corte", lblKpiTasa = new JLabel("Bs. 0.00"), null));

        JPanel topGroup = new JPanel(new BorderLayout(0, 10));
        topGroup.setOpaque(false);
        topGroup.add(filterBar, BorderLayout.NORTH);
        topGroup.add(kpiContainer, BorderLayout.SOUTH);
        panel.add(topGroup, BorderLayout.NORTH);

        // ── Tabla de Resultados (Vista Dual: Histórico vs Reposición) ──
        String[] columnas = {
                "Código", "Descripción", "Almacén", "Tipo", "Existencia",
                "Costo Prom. ($)", "Costo Prom. (Bs.)",
                "Último Costo ($)", "Último Costo (Bs.)",
                "Costo Prov. ($)",
                "Total Valorado ($)",
                "Costo Histórico (Bs.)", "Valor Reposición (Bs.)", "Dif. Cambiario (Bs.)",
                "Capas FIFO", "Proveedor"
        };

        modelInventario = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaInventario = new JTable(modelInventario);
        tablaInventario.setRowHeight(28);
        tablaInventario.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tablaInventario.setBackground(tm.tableBg());
        tablaInventario.setForeground(tm.textPrimary());
        tablaInventario.setSelectionBackground(tm.accent());
        tablaInventario.setSelectionForeground(Color.WHITE);
        tablaInventario.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tablaInventario.getTableHeader().setBackground(tm.tableHeader());
        tablaInventario.getTableHeader().setForeground(tm.textPrimary());

        sorterInventario = new TableRowSorter<>(modelInventario);
        tablaInventario.setRowSorter(sorterInventario);

        DefaultTableCellRenderer centerRend = new DefaultTableCellRenderer();
        centerRend.setHorizontalAlignment(SwingConstants.CENTER);
        tablaInventario.getColumnModel().getColumn(0).setCellRenderer(centerRend);
        tablaInventario.getColumnModel().getColumn(2).setCellRenderer(centerRend);
        tablaInventario.getColumnModel().getColumn(3).setCellRenderer(centerRend);
        tablaInventario.getColumnModel().getColumn(14).setCellRenderer(centerRend);

        DefaultTableCellRenderer rightRend = new DefaultTableCellRenderer();
        rightRend.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i = 4; i <= 13; i++) {
            tablaInventario.getColumnModel().getColumn(i).setCellRenderer(rightRend);
        }

        tablaInventario.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    abrirCapasProductoSeleccionado();
                }
            }
        });

        JScrollPane scrollTable = new JScrollPane(tablaInventario);
        scrollTable.setBorder(BorderFactory.createLineBorder(tm.border(), 1));
        panel.add(scrollTable, BorderLayout.CENTER);

        // ── Barra Inferior de Acciones ──
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        bottomBar.setOpaque(false);

        JButton btnVerCapas = new JButton("🔍 Ver Capas FIFO de este Producto");
        btnVerCapas.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnVerCapas.setBackground(tm.btnNeutralBg());
        btnVerCapas.setForeground(tm.textPrimary());
        btnVerCapas.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVerCapas.addActionListener(e -> abrirCapasProductoSeleccionado());
        bottomBar.add(btnVerCapas);

        JButton btnIrKardex = new JButton("📋 Ver Capas FIFO de este Producto →");
        btnIrKardex.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnIrKardex.setBackground(tm.accent());
        btnIrKardex.setForeground(Color.WHITE);
        btnIrKardex.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnIrKardex.addActionListener(e -> irAKardexDesdeSeleccion());
        bottomBar.add(btnIrKardex);

        panel.add(bottomBar, BorderLayout.SOUTH);

        return panel;
    }

    // ═══════════════════════════════════════════════════════════
    // TAB 2: KÁRDEX DE MOVIMIENTO DE PRODUCTO
    // ═══════════════════════════════════════════════════════════
    private JPanel buildTabKardex() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(tm.background());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 20, 16, 20));

        JPanel filterKardex = new RoundedPanel(12, true);
        filterKardex.setBackground(tm.cardBg());
        filterKardex.setLayout(new MigLayout("insets 12 16 12 16, fillx, gap 10", "[][grow][][][][][][]", "[]"));
        filterKardex.setBorder(BorderFactory.createLineBorder(tm.border(), 1));

        JLabel lblCod = new JLabel("Código / Producto:");
        lblCod.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblCod.setForeground(tm.textPrimary());
        filterKardex.add(lblCod);

        txtKardexArt = new JTextField();
        txtKardexArt.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtKardexArt.putClientProperty("JTextField.placeholderText", "Ingrese código exacto (ej. 004930)");
        txtKardexArt.addActionListener(e -> consultarKardex());
        filterKardex.add(txtKardexArt, "w 220!");

        JLabel lblD = new JLabel("Desde:");
        lblD.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblD.setForeground(tm.textPrimary());
        filterKardex.add(lblD);

        dpKardexDesde = createStyledDatePicker();
        LocalDate fechaInicioK = dpFechaCorte != null ? LocalDate.of(dpFechaCorte.getDate().getYear(), 1, 1) : LocalDate.now().withDayOfYear(1);
        dpKardexDesde.setDate(fechaInicioK);
        filterKardex.add(dpKardexDesde, "w 150!, h 32!");

        JLabel lblH = new JLabel("Hasta (Fecha Corte):");
        lblH.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblH.setForeground(tm.textPrimary());
        filterKardex.add(lblH);

        dpKardexHasta = createStyledDatePicker();
        LocalDate fechaFinK = dpFechaCorte != null ? dpFechaCorte.getDate() : YearMonth.now().minusMonths(1).atEndOfMonth();
        dpKardexHasta.setDate(fechaFinK);
        filterKardex.add(dpKardexHasta, "w 150!, h 32!");

lblKardexDbBadge = new JLabel();
        lblKardexDbBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblKardexDbBadge.setOpaque(true);
        lblKardexDbBadge.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        actualizarKardexBadgeDb(dpKardexDesde.getDate(), dpKardexHasta.getDate());
        filterKardex.add(lblKardexDbBadge);

        dpKardexDesde.addDateChangeListener(e -> actualizarKardexBadgeDb(e.getNewDate(), dpKardexHasta.getDate()));
        dpKardexHasta.addDateChangeListener(e -> actualizarKardexBadgeDb(dpKardexDesde.getDate(), e.getNewDate()));

        cbKardexAlma = new JComboBox<>(new String[]{"TODOS", "01", "02", "B2", "PM", "PV", "C1", "E1", "VC", "VP", "AV"});
        cbKardexAlma.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filterKardex.add(cbKardexAlma, "w 90!");

        btnConsultarKardex = new JButton("🔍 Consultar Capas FIFO");
        btnConsultarKardex.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnConsultarKardex.setBackground(tm.accent());
        btnConsultarKardex.setForeground(Color.WHITE);
        btnConsultarKardex.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnConsultarKardex.addActionListener(e -> consultarKardex());
        filterKardex.add(btnConsultarKardex, "h 32!");

        panel.add(filterKardex, BorderLayout.NORTH);

        String[] columnasK = {
                "Fecha", "Tipo Doc", "Nro. Doc", "Almacén", "Concepto / Proveedor de Origen",
                "Lote", "Entradas (Compras)", "Salidas",
                "Costo Unit. ($)", "Costo Unit. (Bs.)", "Total Monto ($)", "Total Monto (Bs.)"
        };

modelKardex = new DefaultTableModel(columnasK, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaKardex = new JTable(modelKardex);
        tablaKardex.setRowHeight(26);
        tablaKardex.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tablaKardex.setBackground(tm.tableBg());
        tablaKardex.setForeground(tm.textPrimary());
        tablaKardex.setSelectionBackground(tm.accent());
        tablaKardex.setSelectionForeground(Color.WHITE);
        tablaKardex.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tablaKardex.getTableHeader().setBackground(tm.tableHeader());
        tablaKardex.getTableHeader().setForeground(tm.textPrimary());

        DefaultTableCellRenderer centerRend = new DefaultTableCellRenderer();
        centerRend.setHorizontalAlignment(SwingConstants.CENTER);
        tablaKardex.getColumnModel().getColumn(0).setCellRenderer(centerRend);
        tablaKardex.getColumnModel().getColumn(1).setCellRenderer(centerRend);
        tablaKardex.getColumnModel().getColumn(2).setCellRenderer(centerRend);
        tablaKardex.getColumnModel().getColumn(3).setCellRenderer(centerRend);
        tablaKardex.getColumnModel().getColumn(5).setCellRenderer(centerRend);

        DefaultTableCellRenderer rightRend = new DefaultTableCellRenderer();
        rightRend.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i = 6; i < tablaKardex.getColumnCount(); i++) {
            tablaKardex.getColumnModel().getColumn(i).setCellRenderer(rightRend);
        }

        JScrollPane scrollKardex = new JScrollPane(tablaKardex);
        scrollKardex.setBorder(BorderFactory.createLineBorder(tm.border(), 1));
        panel.add(scrollKardex, BorderLayout.CENTER);

        RoundedPanel bottomKardex = new RoundedPanel(10, true);
        bottomKardex.setBackground(tm.cardBg());
        bottomKardex.setBorder(BorderFactory.createLineBorder(tm.border(), 1));
        bottomKardex.setLayout(new MigLayout("insets 8 16 8 16, fillx, gap 12", "[grow 2][grow 1][grow 1][grow 1]", "[]"));

        JPanel pProd = new JPanel(new MigLayout("insets 0, fillx, wrap", "[grow]", "[]2[]"));
        pProd.setOpaque(false);
        lblKardexProdInfo = new JLabel("Seleccione un código de producto y presione 'Consultar Capas FIFO'.");
        lblKardexProdInfo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblKardexProdInfo.setForeground(tm.textPrimary());
        pProd.add(lblKardexProdInfo);
        bottomKardex.add(pProd, "growx");

        JPanel pExist = new JPanel(new MigLayout("insets 2 10 2 10, fillx, wrap", "[grow]", "[]0[]"));
        pExist.setOpaque(false);
        pExist.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, tm.border()));
        JLabel lblT1 = new JLabel("EXISTENCIA A LA FECHA");
        lblT1.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblT1.setForeground(tm.textSecondary());
        pExist.add(lblT1);
        lblKardexExistencia = new JLabel("-");
        lblKardexExistencia.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblKardexExistencia.setForeground(new Color(56, 189, 248));
        pExist.add(lblKardexExistencia);
        bottomKardex.add(pExist, "growx");

        JPanel pProm = new JPanel(new MigLayout("insets 2 10 2 10, fillx, wrap", "[grow]", "[]0[]0[]"));
        pProm.setOpaque(false);
        pProm.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, tm.border()));
        JLabel lblT2 = new JLabel("COSTO PROMEDIO FIFO");
        lblT2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblT2.setForeground(tm.textSecondary());
        pProm.add(lblT2);
        lblKardexCostoPromUSD = new JLabel("-");
        lblKardexCostoPromUSD.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblKardexCostoPromUSD.setForeground(new Color(245, 158, 11));
        pProm.add(lblKardexCostoPromUSD);
        lblKardexCostoPromBS = new JLabel("");
        lblKardexCostoPromBS.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblKardexCostoPromBS.setForeground(tm.textSecondary());
        pProm.add(lblKardexCostoPromBS);
        bottomKardex.add(pProm, "growx");

        JPanel pTot = new JPanel(new MigLayout("insets 2 10 2 10, fillx, wrap", "[grow]", "[]0[]0[]"));
        pTot.setOpaque(false);
        pTot.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, tm.border()));
        JLabel lblT3 = new JLabel("COSTO TOTAL EXISTENCIA");
        lblT3.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblT3.setForeground(tm.textSecondary());
        pTot.add(lblT3);
        lblKardexTotalUSD = new JLabel("-");
        lblKardexTotalUSD.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblKardexTotalUSD.setForeground(new Color(16, 185, 129));
        pTot.add(lblKardexTotalUSD);
        lblKardexTotalBS = new JLabel("");
        lblKardexTotalBS.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblKardexTotalBS.setForeground(tm.textSecondary());
        pTot.add(lblKardexTotalBS);
        bottomKardex.add(pTot, "growx");

        panel.add(bottomKardex, BorderLayout.SOUTH);

        return panel;
    }

    // ═══════════════════════════════════════════════════════════
    // TAB 3: COSTO DE VENTAS Y MARGEN DEL PERÍODO
    // ═══════════════════════════════════════════════════════════
    private JPanel buildTabCostoVentas() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(tm.background());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 20, 16, 20));

        // ── Barra Superior de Filtros y Acciones ──
        JPanel filterBar = new RoundedPanel(12, true);
        filterBar.setBackground(tm.cardBg());
        filterBar.setLayout(new MigLayout("insets 12 16 12 16, fillx, gap 8", "[][][][][][][][][][grow][]", "[]"));
        filterBar.setBorder(BorderFactory.createLineBorder(tm.border(), 1));

        JLabel lblD = new JLabel("Desde:");
        lblD.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblD.setForeground(tm.textPrimary());
        filterBar.add(lblD);

        dpCostoDesde = createStyledDatePicker();
        dpCostoDesde.setDate(LocalDate.now().withDayOfMonth(1));
        filterBar.add(dpCostoDesde, "w 150!, h 32!");

        JLabel lblH = new JLabel("Hasta:");
        lblH.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblH.setForeground(tm.textPrimary());
        filterBar.add(lblH);

        dpCostoHasta = createStyledDatePicker();
        dpCostoHasta.setDate(LocalDate.now());
        filterBar.add(dpCostoHasta, "w 150!, h 32!");

        JButton btnEsteMes = new JButton("Este Mes");
        btnEsteMes.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnEsteMes.setBackground(tm.btnNeutralBg());
        btnEsteMes.setForeground(tm.textPrimary());
        btnEsteMes.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEsteMes.addActionListener(e -> {
            dpCostoDesde.setDate(LocalDate.now().withDayOfMonth(1));
            dpCostoHasta.setDate(LocalDate.now());
        });
        filterBar.add(btnEsteMes, "h 30!");

        JButton btnMesAnt = new JButton("Mes Ant.");
        btnMesAnt.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnMesAnt.setBackground(tm.btnNeutralBg());
        btnMesAnt.setForeground(tm.textPrimary());
        btnMesAnt.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnMesAnt.addActionListener(e -> {
            YearMonth ymAnt = YearMonth.now().minusMonths(1);
            dpCostoDesde.setDate(ymAnt.atDay(1));
            dpCostoHasta.setDate(ymAnt.atEndOfMonth());
        });
        filterBar.add(btnMesAnt, "h 30!");

        lblCostoDbBadge = new JLabel();
        lblCostoDbBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblCostoDbBadge.setOpaque(true);
        lblCostoDbBadge.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        actualizarCostoBadgeDb(dpCostoDesde.getDate(), dpCostoHasta.getDate());
        filterBar.add(lblCostoDbBadge);

        dpCostoDesde.addDateChangeListener(e -> actualizarCostoBadgeDb(e.getNewDate(), dpCostoHasta.getDate()));
        dpCostoHasta.addDateChangeListener(e -> actualizarCostoBadgeDb(dpCostoDesde.getDate(), e.getNewDate()));

        JLabel lblTipo = new JLabel("Tipo:");
        lblTipo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTipo.setForeground(tm.textPrimary());
        filterBar.add(lblTipo);

        cbCostoTipo = new JComboBox<>(new String[]{"TODOS", "VENTA FIRME (Propia)", "CONSIGNACION"});
        cbCostoTipo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filterBar.add(cbCostoTipo, "w 160!");

        JLabel lblAlm = new JLabel("Almacén:");
        lblAlm.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblAlm.setForeground(tm.textPrimary());
        filterBar.add(lblAlm);

        cbCostoAlmacen = new JComboBox<>(new String[]{
                "TODOS", "01 - PRINCIPAL", "02 - CONSIGNACION", "B2 - PULMON BO",
                "PM - PULMON MED", "PV - PULMON VARIOS", "C1 - ESPECIALES",
                "E1 - PSICOTROPICOS", "VC - VENC. CONSIGNACION", "VP - VENC. PROPIO", "AV - AVERIA"
        });
        cbCostoAlmacen.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filterBar.add(cbCostoAlmacen, "w 160!");

        txtCostoBuscar = new JTextField();
        txtCostoBuscar.putClientProperty("JTextField.placeholderText", "Buscar cliente / producto / doc...");
        txtCostoBuscar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtCostoBuscar.addActionListener(e -> filtrarEnTablaCostoVentas());
        filterBar.add(txtCostoBuscar, "growx");

        btnCalcularCosto = new JButton("⚡ Calcular Costo de Ventas");
        btnCalcularCosto.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCalcularCosto.setBackground(tm.accent());
        btnCalcularCosto.setForeground(Color.WHITE);
        btnCalcularCosto.setFocusPainted(false);
        btnCalcularCosto.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCalcularCosto.addActionListener(e -> ejecutarCalculoCostoVentas());
        filterBar.add(btnCalcularCosto);

        btnExportarCosto = new JButton("📊 Exportar Excel");
        btnExportarCosto.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnExportarCosto.setBackground(tm.greenAccent());
        btnExportarCosto.setForeground(Color.WHITE);
        btnExportarCosto.setFocusPainted(false);
        btnExportarCosto.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnExportarCosto.addActionListener(e -> exportarExcelCostoVentas());
        filterBar.add(btnExportarCosto);

        // ── Tarjetas KPI Tab 3 ──
        JPanel kpiContainer = new JPanel(new GridLayout(1, 5, 12, 0));
        kpiContainer.setOpaque(false);

        kpiContainer.add(createKpiCard("💰 Ventas Netas Totales", lblCostoKpiVentasUSD = new JLabel("$0.00"), lblCostoKpiVentasBS = new JLabel("Bs. 0.00")));
        kpiContainer.add(createKpiCard("🏷️ Costo de Ventas Total", lblCostoKpiCostoUSD = new JLabel("$0.00"), lblCostoKpiCostoDetalle = new JLabel("Firme: $0.00  |  Consig: $0.00")));
        kpiContainer.add(createKpiCard("📈 Utilidad Bruta en Ventas", lblCostoKpiUtilUSD = new JLabel("$0.00"), lblCostoKpiUtilBS = new JLabel("Bs. 0.00")));
        kpiContainer.add(createKpiCard("📊 Margen Bruto General", lblCostoKpiMargen = new JLabel("0.00%"), null));
        kpiContainer.add(createKpiCard("📦 Total Unidades Vendidas", lblCostoKpiUnds = new JLabel("0.00"), null));

        JPanel topGroup = new JPanel(new BorderLayout(0, 10));
        topGroup.setOpaque(false);
        topGroup.add(filterBar, BorderLayout.NORTH);
        topGroup.add(kpiContainer, BorderLayout.SOUTH);
        panel.add(topGroup, BorderLayout.NORTH);

        // ── Tabla de Costo de Ventas ──
        String[] columnasCV = {
                "Fecha", "Tipo Doc", "Nro. Doc", "Cliente", "Código Art.", "Descripción",
                "Almacén", "Tipo", "Cant. Vendida",
                "Precio Vta ($)", "Total Venta ($)", "Total Venta (Bs.)",
                "Costo Unit ($)", "Total Costo ($)", "Total Costo (Bs.)",
                "Utilidad ($)", "Margen %"
        };

        modelCostoVentas = new DefaultTableModel(columnasCV, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaCostoVentas = new JTable(modelCostoVentas);
        tablaCostoVentas.setRowHeight(26);
        tablaCostoVentas.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tablaCostoVentas.setBackground(tm.tableBg());
        tablaCostoVentas.setForeground(tm.textPrimary());
        tablaCostoVentas.setSelectionBackground(tm.accent());
        tablaCostoVentas.setSelectionForeground(Color.WHITE);
        tablaCostoVentas.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tablaCostoVentas.getTableHeader().setBackground(tm.tableHeader());
        tablaCostoVentas.getTableHeader().setForeground(tm.textPrimary());

        sorterCostoVentas = new TableRowSorter<>(modelCostoVentas);
        tablaCostoVentas.setRowSorter(sorterCostoVentas);

        DefaultTableCellRenderer centerRend = new DefaultTableCellRenderer();
        centerRend.setHorizontalAlignment(SwingConstants.CENTER);
        tablaCostoVentas.getColumnModel().getColumn(0).setCellRenderer(centerRend);
        tablaCostoVentas.getColumnModel().getColumn(1).setCellRenderer(centerRend);
        tablaCostoVentas.getColumnModel().getColumn(2).setCellRenderer(centerRend);
        tablaCostoVentas.getColumnModel().getColumn(4).setCellRenderer(centerRend);
        tablaCostoVentas.getColumnModel().getColumn(6).setCellRenderer(centerRend);
        tablaCostoVentas.getColumnModel().getColumn(7).setCellRenderer(centerRend);

        DefaultTableCellRenderer rightRend = new DefaultTableCellRenderer();
        rightRend.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i = 8; i <= 16; i++) {
            tablaCostoVentas.getColumnModel().getColumn(i).setCellRenderer(rightRend);
        }

        JScrollPane scrollCosto = new JScrollPane(tablaCostoVentas);
        scrollCosto.setBorder(BorderFactory.createLineBorder(tm.border(), 1));
        panel.add(scrollCosto, BorderLayout.CENTER);

        return panel;
    }

    // ═══════════════════════════════════════════════════════════
    // LÓGICA DE NEGOCIO Y CÁLCULOS
    // ═══════════════════════════════════════════════════════════

    private void ejecutarCalculoValoracion() {
        LocalDate fecha = dpFechaCorte.getDate();
        if (fecha == null) {
            Toast.show("Seleccione una fecha de corte válida", Toast.Type.WARNING);
            return;
        }

        String tipoSel = (String) cbTipo.getSelectedItem();
        String filtroTipo = "TODOS";
        if (tipoSel != null && tipoSel.contains("VENTA FIRME")) filtroTipo = "VENTA_FIRME";
        else if (tipoSel != null && tipoSel.contains("CONSIGNACION")) filtroTipo = "CONSIGNACION";

        String almSel = (String) cbAlmacen.getSelectedItem();
        String filtroAlm = (almSel != null && !almSel.startsWith("TODOS")) ? almSel.substring(0, 2).trim() : null;
        String busq = txtBuscar.getText().trim();

        btnCalcular.setEnabled(false);
        btnCalcular.setText("⏳ Calculando...");

        final String finalFiltroTipo = filtroTipo;
        final String finalFiltroAlm = filtroAlm;

        SwingWorker<List<InventarioValoradoRow>, Void> worker = new SwingWorker<>() {
            private double tasaUsd;

            @Override
            protected List<InventarioValoradoRow> doInBackground() {
                tasaUsd = dao.obtenerTasaUSD(fecha);
                return dao.calcularInventarioValorado(fecha, finalFiltroTipo, finalFiltroAlm, busq);
            }

            @Override
            protected void done() {
                try {
                    listaActual = get();
                    tasaCorteActual = tasaUsd;
                    actualizarTablaInventario(listaActual);
                    actualizarKpis(listaActual, tasaUsd);
                    Toast.show(String.format("✔ Valoración completada: %,d productos valorados", listaActual.size()), Toast.Type.SUCCESS);
                } catch (Exception ex) {
                    logger.error("Error al calcular valuación: {}", ex.getMessage(), ex);
                    Toast.show("Error al calcular valuación: " + ex.getMessage(), Toast.Type.ERROR);
                } finally {
                    btnCalcular.setEnabled(true);
                    btnCalcular.setText("⚡ Calcular y Valuar");
                }
            }
        };

        worker.execute();
    }

    private void actualizarTablaInventario(List<InventarioValoradoRow> rows) {
        modelInventario.setRowCount(0);
        for (InventarioValoradoRow r : rows) {
            modelInventario.addRow(new Object[]{
                    r.getCoArt(),
                    r.getArtDes(),
                    r.getCoAlma(),
                    r.getTipoMercancia(),
                    NF_DEC.format(r.getExistencia()),
                    NF_USD.format(r.getCostoPromedioUSD()),
                    NF_BS.format(r.getCostoPromedioBS()),
                    NF_USD.format(r.getUltimoCostoUSD()),
                    NF_BS.format(r.getUltimoCostoBS()),
                    NF_USD.format(r.getCostoProveedorUSD()),
                    NF_USD.format(r.getTotalValorizadoUSD()),
                    NF_BS.format(r.getTotalValorizadoBS()),
                    NF_BS.format(r.getTotalReposicionBS()),
                    NF_BS.format(r.getDiferencialCambiarioBS()),
                    r.getCantCapas() > 0 ? (r.getCantCapas() + " Capas") : "-",
                    r.getNomProv() != null ? r.getNomProv() : r.getCoProv()
            });
        }
        filtrarEnTabla();
    }

    private void actualizarKpis(List<InventarioValoradoRow> rows, double tasa) {
        int itemsFirme = 0;
        double undsFirme = 0;
        double totalUsdFirme = 0;
        double totalBsHistFirme = 0;
        double totalBsReposFirme = 0;

        int itemsConsig = 0;
        double undsConsig = 0;
        double totalUsdConsig = 0;
        double totalBsHistConsig = 0;
        double totalBsReposConsig = 0;

        for (InventarioValoradoRow r : rows) {
            if ("CONSIGNACION".equalsIgnoreCase(r.getTipoMercancia())) {
                itemsConsig++;
                undsConsig += r.getExistencia();
                totalUsdConsig += r.getTotalValorizadoUSD();
                totalBsHistConsig += r.getTotalValorizadoBS();
                totalBsReposConsig += r.getTotalReposicionBS();
            } else {
                itemsFirme++;
                undsFirme += r.getExistencia();
                totalUsdFirme += r.getTotalValorizadoUSD();
                totalBsHistFirme += r.getTotalValorizadoBS();
                totalBsReposFirme += r.getTotalReposicionBS();
            }
        }

        lblKpiItems.setText(String.format("%,d", itemsFirme + itemsConsig));
        lblKpiUnds.setText(NF_DEC.format(undsFirme + undsConsig));
        if (lblKpiUndsDetalle != null) {
            lblKpiUndsDetalle.setText(String.format("Firme: %s  |  Consig: %s", NF_DEC.format(undsFirme), NF_DEC.format(undsConsig)));
        }

        lblKpiFirmeUSD.setText(NF_USD.format(totalUsdFirme));
        if (lblKpiFirmeBS != null) lblKpiFirmeBS.setText("Hist: " + NF_BS.format(totalBsHistFirme));

        lblKpiConsigUSD.setText(NF_USD.format(totalUsdConsig));
        if (lblKpiConsigBS != null) lblKpiConsigBS.setText(NF_BS.format(totalBsHistConsig));

        double totalUsdGlobal = totalUsdFirme + totalUsdConsig;
        double totalBsHistGlobal = totalBsHistFirme + totalBsHistConsig;
        double totalBsReposGlobal = totalBsReposFirme + totalBsReposConsig;

        lblKpiGlobalUSD.setText(NF_USD.format(totalUsdGlobal));
        if (lblKpiGlobalBS != null) {
            lblKpiGlobalBS.setText(String.format("Hist: %s | Repos: %s", NF_BS.format(totalBsHistGlobal), NF_BS.format(totalBsReposGlobal)));
        }

        lblKpiTasa.setText(String.format("Bs. %,.2f", tasa));
    }

    private void filtrarEnTabla() {
        if (sorterInventario == null) return;
        String texto = txtBuscar != null ? txtBuscar.getText().trim() : "";
        if (texto.isEmpty()) {
            sorterInventario.setRowFilter(null);
            if (lblCoincidencias != null) {
                int total = listaActual != null ? listaActual.size() : 0;
                lblCoincidencias.setText(String.format("Mostrando %,d productos", total));
            }
        } else {
            try {
                sorterInventario.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(texto)));
                if (lblCoincidencias != null) {
                    int visibles = tablaInventario.getRowCount();
                    int total = listaActual != null ? listaActual.size() : 0;
                    lblCoincidencias.setText(String.format("Mostrando %,d de %,d productos", visibles, total));
                }
            } catch (Exception ex) {
                sorterInventario.setRowFilter(null);
            }
        }
    }

private void abrirCapasProductoSeleccionado() {
        int selRow = tablaInventario.getSelectedRow();
        if (selRow < 0) {
            Toast.show("Seleccione un producto en la tabla", Toast.Type.INFO);
            return;
        }

        int modelIdx = tablaInventario.convertRowIndexToModel(selRow);
        if (modelIdx >= 0 && modelIdx < listaActual.size()) {
            InventarioValoradoRow row = listaActual.get(modelIdx);
            CapasCostoDialog dialog = new CapasCostoDialog(this, row, dpFechaCorte.getDate().toString());
            dialog.setVisible(true);
        }
    }

    private void irAKardexDesdeSeleccion() {
        int selRow = tablaInventario.getSelectedRow();
        if (selRow < 0) {
            Toast.show("Seleccione un producto en la tabla primero", Toast.Type.INFO);
            return;
        }

        int modelIdx = tablaInventario.convertRowIndexToModel(selRow);
        if (modelIdx >= 0 && modelIdx < listaActual.size()) {
            InventarioValoradoRow row = listaActual.get(modelIdx);
            txtKardexArt.setText(row.getCoArt());
            if (dpFechaCorte != null && dpFechaCorte.getDate() != null) {
                dpKardexHasta.setDate(dpFechaCorte.getDate());
                dpKardexDesde.setDate(LocalDate.of(dpFechaCorte.getDate().getYear(), 1, 1));
            }
            if (row.getCoAlma() != null && !row.getCoAlma().isBlank()) {
                cbKardexAlma.setSelectedItem(row.getCoAlma().trim());
            } else if (cbAlmacen.getSelectedItem() != null) {
                cbKardexAlma.setSelectedItem(cbAlmacen.getSelectedItem());
            }
            tabbedPane.setSelectedIndex(1);
            consultarKardex();
        }
    }

    private void consultarKardex() {
        String coArt = txtKardexArt.getText().trim();
        if (coArt.isEmpty()) {
            Toast.show("Ingrese un código de producto para consultar", Toast.Type.WARNING);
            return;
        }

        LocalDate desde = dpKardexDesde.getDate();
        LocalDate hasta = dpKardexHasta.getDate();
        if (desde == null || hasta == null) {
            Toast.show("Seleccione un rango de fechas válido", Toast.Type.WARNING);
            return;
        }

        String alm = (String) cbKardexAlma.getSelectedItem();
        String filtroAlm = (alm != null && !alm.equals("TODOS")) ? alm : null;

        // Verificar si este producto ya fue calculado en la pestaña de Valoración Global para la misma fecha
        InventarioValoradoRow matchVal = null;
        if (dpFechaCorte != null && hasta.equals(dpFechaCorte.getDate()) && listaActual != null && !listaActual.isEmpty()) {
            for (InventarioValoradoRow r : listaActual) {
                if (r.getCoArt() != null && r.getCoArt().trim().equalsIgnoreCase(coArt.trim())) {
                    if (filtroAlm == null || r.getCoAlma() == null || r.getCoAlma().trim().equalsIgnoreCase(filtroAlm.trim())) {
                        matchVal = r;
                        break;
                    }
                }
            }
        }
        final InventarioValoradoRow finalMatchVal = matchVal;

        btnConsultarKardex.setEnabled(false);
        btnConsultarKardex.setText("⏳ Consultando...");

        SwingWorker<InventarioValoracionDAO.KardexConsultaResult, Void> worker = new SwingWorker<>() {
            @Override
            protected InventarioValoracionDAO.KardexConsultaResult doInBackground() {
                return dao.consultarKardexProducto(coArt, desde, hasta, filtroAlm);
            }

            @Override
            protected void done() {
                try {
                    InventarioValoracionDAO.KardexConsultaResult kRes = get();
                    modelKardex.setRowCount(0);

                    for (KardexMovimientoValoradoRow m : kRes.getMovimientos()) {
                        modelKardex.addRow(new Object[]{
                                m.getFecha(),
                                m.getTipoDoc(),
                                m.getDocNum(),
                                m.getAlmacen(),
                                m.getOrigenDestino(),
                                m.getLote(),
                                m.getCantidadEntrada() > 0 ? NF_DEC.format(m.getCantidadEntrada()) : "-",
                                "-",
                                m.getCostoUnitUSD() > 0 ? NF_USD.format(m.getCostoUnitUSD()) : "-",
                                m.getCostoUnitBS() > 0 ? NF_BS.format(m.getCostoUnitBS()) : "-",
                                m.getTotalUSD() > 0 ? NF_USD.format(m.getTotalUSD()) : "-",
                                m.getTotalBS() > 0 ? NF_BS.format(m.getTotalBS()) : "-"
                        });
                    }

                    String artNombre = (finalMatchVal != null && finalMatchVal.getArtDes() != null)
                            ? finalMatchVal.getArtDes()
                            : ((kRes.getArtDes() != null && !kRes.getArtDes().isBlank()) ? kRes.getArtDes() : coArt);

                    lblKardexProdInfo.setText(String.format("<html><b>Producto:</b> %s — %s<br/><span style='color:%s;'>Total Compras (Periodo): <b>%s uds</b> (%,d recepciones)</span></html>",
                            coArt, artNombre,
                            tm.isDark() ? "#94a3b8" : "#64748b",
                            NF_DEC.format(kRes.getTotalCompradoPeriodo()),
                            kRes.getMovimientos().size()));

                    if (finalMatchVal != null) {
                        lblKardexExistencia.setText(NF_DEC.format(finalMatchVal.getExistencia()) + " uds");
                        lblKardexCostoPromUSD.setText(NF_USD.format(finalMatchVal.getCostoPromedioUSD()));
                        lblKardexCostoPromBS.setText(NF_BS.format(finalMatchVal.getCostoPromedioBS()));
                        lblKardexTotalUSD.setText(NF_USD.format(finalMatchVal.getTotalValorizadoUSD()));
                        lblKardexTotalBS.setText(NF_BS.format(finalMatchVal.getTotalValorizadoBS()));
                    } else {
                        lblKardexExistencia.setText(NF_DEC.format(kRes.getExistenciaFecha()) + " uds");
                        lblKardexCostoPromUSD.setText(NF_USD.format(kRes.getCostoPromedioUSD()));
                        lblKardexCostoPromBS.setText(NF_BS.format(kRes.getCostoPromedioBS()));
                        lblKardexTotalUSD.setText(NF_USD.format(kRes.getTotalValorizadoUSD()));
                        lblKardexTotalBS.setText(NF_BS.format(kRes.getTotalValorizadoBS()));
                    }

                } catch (Exception ex) {
                    logger.error("Error al consultar Capas FIFO: {}", ex.getMessage(), ex);
                    Toast.show("Error al consultar Capas FIFO: " + ex.getMessage(), Toast.Type.ERROR);
                } finally {
                    btnConsultarKardex.setEnabled(true);
                    btnConsultarKardex.setText("🔍 Consultar Capas FIFO");
                }
            }
        };

        worker.execute();
    }

private void exportarExcel() {
        if (listaActual.isEmpty()) {
            Toast.show("No hay datos calculados para exportar", Toast.Type.WARNING);
            return;
        }

        LocalDate corte = dpFechaCorte.getDate();
        String defaultName = "Inventario_Valorado_" + corte.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(defaultName));
        int res = fc.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File dest = fc.getSelectedFile();
            try {
                InventarioValoracionExcelExporter.exportar(listaActual, corte, tasaCorteActual, dest);
                Toast.show("✔ Archivo Excel exportado con éxito", Toast.Type.SUCCESS);
                int opt = JOptionPane.showConfirmDialog(this, "¿Desea abrir el archivo Excel generado?", "Exportación Exitosa", JOptionPane.YES_NO_OPTION);
                if (opt == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(dest);
                }
            } catch (Exception ex) {
                logger.error("Error al exportar Excel: {}", ex.getMessage(), ex);
                Toast.show("Error al exportar: " + ex.getMessage(), Toast.Type.ERROR);
            }
        }
    }

    // ── Lógica de Tab 3: Costo de Ventas del Período ──

    private void ejecutarCalculoCostoVentas() {
        LocalDate desde = dpCostoDesde.getDate();
        LocalDate hasta = dpCostoHasta.getDate();
        if (desde == null || hasta == null) {
            Toast.show("Seleccione un rango de fechas válido", Toast.Type.WARNING);
            return;
        }
        if (desde.isAfter(hasta)) {
            Toast.show("La fecha 'Desde' no puede ser posterior a 'Hasta'", Toast.Type.WARNING);
            return;
        }

        String tipoSel = (String) cbCostoTipo.getSelectedItem();
        String filtroTipo = "TODOS";
        if (tipoSel != null && tipoSel.contains("VENTA FIRME")) filtroTipo = "VENTA_FIRME";
        else if (tipoSel != null && tipoSel.contains("CONSIGNACION")) filtroTipo = "CONSIGNACION";

        String almSel = (String) cbCostoAlmacen.getSelectedItem();
        String filtroAlm = (almSel != null && !almSel.startsWith("TODOS")) ? almSel.substring(0, 2).trim() : null;
        String busq = txtCostoBuscar.getText().trim();

        btnCalcularCosto.setEnabled(false);
        btnCalcularCosto.setText("⏳ Calculando...");

        final String finalFiltroTipo = filtroTipo;
        final String finalFiltroAlm = filtroAlm;

        SwingWorker<CostoVentasPeriodoResult, Void> worker = new SwingWorker<>() {
            @Override
            protected CostoVentasPeriodoResult doInBackground() {
                String targetDb = InventarioValoracionDAO.resolverBaseDatos(hasta);
                return dao.consultarCostoVentasPeriodo(targetDb, desde, hasta, finalFiltroAlm, finalFiltroTipo, busq);
            }

            @Override
            protected void done() {
                try {
                    costoVentasActual = get();
                    actualizarTablaCostoVentas(costoVentasActual.getItems());
                    actualizarKpisCostoVentas(costoVentasActual);
                    Toast.show(String.format("✔ Costo de Ventas calculado: %,d renglones vendidos", costoVentasActual.getItems().size()), Toast.Type.SUCCESS);
                } catch (Exception ex) {
                    logger.error("Error al calcular Costo de Ventas: {}", ex.getMessage(), ex);
                    Toast.show("Error al calcular Costo de Ventas: " + ex.getMessage(), Toast.Type.ERROR);
                } finally {
                    btnCalcularCosto.setEnabled(true);
                    btnCalcularCosto.setText("⚡ Calcular Costo de Ventas");
                }
            }
        };

        worker.execute();
    }

    private void actualizarTablaCostoVentas(List<CostoVentaRow> items) {
        modelCostoVentas.setRowCount(0);
        for (CostoVentaRow r : items) {
            modelCostoVentas.addRow(new Object[]{
                    r.getFecha(),
                    r.getTipoDoc(),
                    r.getDocNum(),
                    r.getCliDes() != null && !r.getCliDes().isBlank() ? r.getCliDes() : r.getCoCli(),
                    r.getCoArt(),
                    r.getArtDes(),
                    r.getCoAlma(),
                    r.getTipoMercancia(),
                    NF_DEC.format(r.getCantidad()),
                    NF_USD.format(r.getPrecioVentaUSD()),
                    NF_USD.format(r.getTotalVentaUSD()),
                    NF_BS.format(r.getTotalVentaBS()),
                    NF_USD.format(r.getCostoUnitUSD()),
                    NF_USD.format(r.getTotalCostoUSD()),
                    NF_BS.format(r.getTotalCostoBS()),
                    NF_USD.format(r.getUtilidadUSD()),
                    NF_PCT.format(r.getMargenPct())
            });
        }
    }

    private void actualizarKpisCostoVentas(CostoVentasPeriodoResult res) {
        lblCostoKpiVentasUSD.setText(NF_USD.format(res.getTotalVentaUSD()));
        lblCostoKpiVentasBS.setText(NF_BS.format(res.getTotalVentaBS()));

        lblCostoKpiCostoUSD.setText(NF_USD.format(res.getTotalCostoUSD()));
        lblCostoKpiCostoDetalle.setText(String.format("Firme: %s  |  Consig: %s",
                NF_USD.format(res.getTotalCostoFirmeUSD()),
                NF_USD.format(res.getTotalCostoConsigUSD())));

        lblCostoKpiUtilUSD.setText(NF_USD.format(res.getUtilidadUSD()));
        lblCostoKpiUtilBS.setText(NF_BS.format(res.getUtilidadBS()));

        lblCostoKpiMargen.setText(NF_PCT.format(res.getMargenPct()));
        if (res.getMargenPct() < 0) {
            lblCostoKpiMargen.setForeground(new Color(239, 68, 68)); // Rojo
        } else if (res.getMargenPct() >= 20) {
            lblCostoKpiMargen.setForeground(new Color(16, 185, 129)); // Verde
        } else {
            lblCostoKpiMargen.setForeground(new Color(245, 158, 11)); // Ámbar
        }

        lblCostoKpiUnds.setText(NF_DEC.format(res.getTotalUnidades()));
    }

    private void filtrarEnTablaCostoVentas() {
        String texto = txtCostoBuscar.getText().trim();
        if (texto.isEmpty()) {
            sorterCostoVentas.setRowFilter(null);
        } else {
            sorterCostoVentas.setRowFilter(RowFilter.regexFilter("(?i)" + texto));
        }
    }

    private void exportarExcelCostoVentas() {
        if (costoVentasActual == null || costoVentasActual.getItems() == null || costoVentasActual.getItems().isEmpty()) {
            Toast.show("No hay datos calculados de Costo de Ventas para exportar", Toast.Type.WARNING);
            return;
        }

        LocalDate desde = dpCostoDesde.getDate();
        LocalDate hasta = dpCostoHasta.getDate();
        String defaultName = "Costo_Ventas_" + desde.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" + hasta.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(defaultName));
        int res = fc.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File dest = fc.getSelectedFile();
            try {
                InventarioValoracionExcelExporter.exportarCostoVentas(costoVentasActual, desde, hasta, dest);
                Toast.show("✔ Reporte Excel de Costo de Ventas exportado con éxito", Toast.Type.SUCCESS);
                int opt = JOptionPane.showConfirmDialog(this, "¿Desea abrir el archivo Excel generado?", "Exportación Exitosa", JOptionPane.YES_NO_OPTION);
                if (opt == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(dest);
                }
            } catch (Exception ex) {
                logger.error("Error al exportar Costo de Ventas a Excel: {}", ex.getMessage(), ex);
                Toast.show("Error al exportar: " + ex.getMessage(), Toast.Type.ERROR);
            }
        }
    }

    // ── Helper para crear tarjetas KPI ──
    private JPanel createKpiCard(String titulo, JLabel lblPrincipal, JLabel lblSecundario) {
        RoundedPanel card = new RoundedPanel(10, true);
        card.setBackground(tm.cardBg());
        card.setLayout(new MigLayout("insets 8 12 8 12, fillx, wrap", "[grow]", "[]2[]" + (lblSecundario != null ? "2[]" : "")));
        card.setBorder(BorderFactory.createLineBorder(tm.border(), 1));

        JLabel lblT = new JLabel(titulo);
        lblT.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblT.setForeground(tm.textSecondary());
        card.add(lblT);

        lblPrincipal.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblPrincipal.setForeground(tm.accent());
        card.add(lblPrincipal);

        if (lblSecundario != null) {
            lblSecundario.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lblSecundario.setForeground(tm.textSecondary());
            card.add(lblSecundario);
        }

        return card;
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
        settings.setColor(DatePickerSettings.DateArea.CalendarBorderSelectedDate, tm.accent());
        settings.setFontValidDate(new Font("Segoe UI", Font.BOLD, 12));

        DatePicker picker = new DatePicker(settings);
        picker.setPreferredSize(new Dimension(160, 32));

        JButton btnCal = picker.getComponentToggleCalendarButton();
        btnCal.setText("📅");
        btnCal.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        btnCal.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCal.setFocusable(false);
        btnCal.setMargin(new Insets(0, 4, 0, 4));
        btnCal.setToolTipText("Abrir selector de calendario");

        JTextField tf = picker.getComponentDateTextField();
        tf.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tf.setToolTipText("Haga clic para abrir el calendario selector de fechas");
        tf.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (picker.isEnabled()) {
                    picker.openPopup();
                }
            }
        });

        return picker;
    }

    private void actualizarBadgeDb(LocalDate d) {
        if (d == null || lblDbBadge == null) return;
        String db = InventarioValoracionDAO.resolverBaseDatos(d);
        if (InventarioValoracionDAO.DB_HISTORICA.equalsIgnoreCase(db)) {
            lblDbBadge.setText("BD: " + db + " (Histórico ≤ 27/07/2026)");
            lblDbBadge.setBackground(new Color(245, 158, 11, 35));
            lblDbBadge.setForeground(new Color(245, 158, 11));
            lblDbBadge.setToolTipText("Consultando base de datos histórica previa a la migración (" + db + ")");
        } else {
            lblDbBadge.setText("BD: " + db + " (Actual)");
            lblDbBadge.setBackground(new Color(16, 185, 129, 35));
            lblDbBadge.setForeground(new Color(16, 185, 129));
            lblDbBadge.setToolTipText("Consultando base de datos actual de producción (" + db + ")");
        }
    }

    private void actualizarKardexBadgeDb(LocalDate d1, LocalDate d2) {
        if (d1 == null || d2 == null || lblKardexDbBadge == null) return;
        boolean d1Hist = !d1.isAfter(InventarioValoracionDAO.FECHA_CORTE_MIGRACION);
        boolean d2Hist = !d2.isAfter(InventarioValoracionDAO.FECHA_CORTE_MIGRACION);

        if (d1Hist && d2Hist) {
            lblKardexDbBadge.setText("BD: DROA2_A (Histórico ≤ 27/07/2026)");
            lblKardexDbBadge.setBackground(new Color(245, 158, 11, 35));
            lblKardexDbBadge.setForeground(new Color(245, 158, 11));
            lblKardexDbBadge.setToolTipText("Consultando base de datos histórica (DROA2_A)");
        } else if (!d1Hist && !d2Hist) {
            lblKardexDbBadge.setText("BD: DROA_A (Actual)");
            lblKardexDbBadge.setBackground(new Color(16, 185, 129, 35));
            lblKardexDbBadge.setForeground(new Color(16, 185, 129));
            lblKardexDbBadge.setToolTipText("Consultando base de datos actual de producción (DROA_A)");
        } else {
            lblKardexDbBadge.setText("BD: DROA2_A + DROA_A (Migración combinada)");
            lblKardexDbBadge.setBackground(new Color(59, 130, 246, 35));
            lblKardexDbBadge.setForeground(new Color(59, 130, 246));
            lblKardexDbBadge.setToolTipText("Rango que abarca antes y después del 27/07/2026: combina DROA2_A y DROA_A");
        }
    }

    private void actualizarCostoBadgeDb(LocalDate d1, LocalDate d2) {
        if (d1 == null || d2 == null || lblCostoDbBadge == null) return;
        boolean d1Hist = !d1.isAfter(InventarioValoracionDAO.FECHA_CORTE_MIGRACION);
        boolean d2Hist = !d2.isAfter(InventarioValoracionDAO.FECHA_CORTE_MIGRACION);

        if (d1Hist && d2Hist) {
            lblCostoDbBadge.setText("BD: DROA2_A (Histórico ≤ 27/07/2026)");
            lblCostoDbBadge.setBackground(new Color(245, 158, 11, 35));
            lblCostoDbBadge.setForeground(new Color(245, 158, 11));
            lblCostoDbBadge.setToolTipText("Consultando base de datos histórica (DROA2_A)");
        } else if (!d1Hist && !d2Hist) {
            lblCostoDbBadge.setText("BD: DROA_A (Actual)");
            lblCostoDbBadge.setBackground(new Color(16, 185, 129, 35));
            lblCostoDbBadge.setForeground(new Color(16, 185, 129));
            lblCostoDbBadge.setToolTipText("Consultando base de datos actual de producción (DROA_A)");
        } else {
            lblCostoDbBadge.setText("BD: DROA2_A + DROA_A (Migración combinada)");
            lblCostoDbBadge.setBackground(new Color(59, 130, 246, 35));
            lblCostoDbBadge.setForeground(new Color(59, 130, 246));
            lblCostoDbBadge.setToolTipText("Rango que abarca antes y después del 27/07/2026: combina DROA2_A y DROA_A");
        }
    }
}
