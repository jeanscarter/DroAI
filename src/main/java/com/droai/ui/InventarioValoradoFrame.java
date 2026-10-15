package com.droai.ui;

import com.droai.dao.InventarioValoracionDAO;
import com.droai.export.InventarioValoracionExcelExporter;
import com.droai.model.CapaCostoDTO;
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
 * Módulo de Valuación de Inventario y Kárdex Multimoneda.
 *
 * <p>Permite:
 * <ul>
 *   <li>Reconstruir la existencia y valuación al corte por Capas FIFO (PEPS) para Venta Firme.</li>
 *   <li>Valuar la mercancía en consignación en base al último costo en USD reportado por proveedores.</li>
 *   <li>Consultar el Kárdex completo de movimiento de un producto con saldos y costos en USD y Bs.</li>
 *   <li>Exportar a Excel oficial con resumen ejecutivo, detalle y desglose de capas.</li>
 * </ul>
 */
public class InventarioValoradoFrame extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(InventarioValoradoFrame.class);
    private final ThemeManager tm = ThemeManager.get();
    private final InventarioValoracionDAO dao = new InventarioValoracionDAO();

    private static final DecimalFormat NF_DEC = new DecimalFormat("#,##0.00");
    private static final DecimalFormat NF_USD = new DecimalFormat("$#,##0.00");
    private static final DecimalFormat NF_BS = new DecimalFormat("'Bs.' #,##0.00");

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
    private List<InventarioValoradoRow> listaActual = new ArrayList<>();
    private double tasaCorteActual = 1.0;

    // Tarjetas KPI
    private JLabel lblKpiItems;
    private JLabel lblKpiUnds;
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
    private JLabel lblKardexResumen;

    // Pestañas
    private JTabbedPane tabbedPane;

    public InventarioValoradoFrame() {
        setTitle("DroAI — Valuación de Inventario y Kárdex Multimoneda");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1320, 840);
        setMinimumSize(new Dimension(1080, 680));
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

        JLabel lblTitle = new JLabel("Valuación de Inventario y Kárdex Multimoneda");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(tm.textPrimary());
        header.add(lblTitle, "wrap");

        JLabel lblSub = new JLabel("Existencia a fecha determinada • Capas FIFO de Compra • Mercancía en Consignación • Kárdex en Bs. y USD");
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
        tabbedPane.addTab("📑 Kárdex de Movimiento de Producto", buildTabKardex());

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
        filterBar.setLayout(new MigLayout("insets 12 16 12 16, fillx, gap 8", "[][][][][][][][grow][]", "[]"));
        filterBar.setBorder(BorderFactory.createLineBorder(tm.border(), 1));

        // Fecha de corte
        JLabel lblFc = new JLabel("Fecha Corte:");
        lblFc.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblFc.setForeground(tm.textPrimary());
        filterBar.add(lblFc);

        dpFechaCorte = createStyledDatePicker();
        // Por defecto, fin del mes anterior (estándar contable de inventario)
        LocalDate finMesAnterior = YearMonth.now().minusMonths(1).atEndOfMonth();
        dpFechaCorte.setDate(finMesAnterior);
        filterBar.add(dpFechaCorte, "w 160!, h 32!");

        JButton btnFinMesAnt = new JButton("Fin Mes Ant.");
        btnFinMesAnt.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnFinMesAnt.setBackground(tm.btnNeutralBg());
        btnFinMesAnt.setForeground(tm.textPrimary());
        btnFinMesAnt.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnFinMesAnt.addActionListener(e -> dpFechaCorte.setDate(YearMonth.now().minusMonths(1).atEndOfMonth()));
        filterBar.add(btnFinMesAnt, "h 30!");

        JButton btnHoy = new JButton("Hoy");
        btnHoy.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnHoy.setBackground(tm.btnNeutralBg());
        btnHoy.setForeground(tm.textPrimary());
        btnHoy.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnHoy.addActionListener(e -> dpFechaCorte.setDate(LocalDate.now()));
        filterBar.add(btnHoy, "h 30!");

        // Badge indicador de base de datos dinámica según fecha de corte
        lblDbBadge = new JLabel();
        lblDbBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblDbBadge.setOpaque(true);
        lblDbBadge.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        actualizarBadgeDb(dpFechaCorte.getDate());
        filterBar.add(lblDbBadge);

        dpFechaCorte.addDateChangeListener(event -> {
            actualizarBadgeDb(event.getNewDate());
        });

        // Tipo
        JLabel lblTipo = new JLabel("Tipo:");
        lblTipo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTipo.setForeground(tm.textPrimary());
        filterBar.add(lblTipo);

        cbTipo = new JComboBox<>(new String[]{"TODOS", "VENTA FIRME (Propia)", "CONSIGNACION"});
        cbTipo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filterBar.add(cbTipo, "w 170!");

        // Almacén
        JLabel lblAlm = new JLabel("Almacén:");
        lblAlm.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblAlm.setForeground(tm.textPrimary());
        filterBar.add(lblAlm);

        cbAlmacen = new JComboBox<>(new String[]{
                "TODOS", "01 - PRINCIPAL", "02 - CONSIGNACION", "B2 - PULMON BO",
                "PM - PULMON MED", "PV - PULMON VARIOS", "C1 - ESPECIALES",
                "E1 - PSICOTROPICOS", "VC - VENC. CONSIGNACION", "VP - VENC. PROPIO", "AV - AVERIA"
        });
        cbAlmacen.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filterBar.add(cbAlmacen, "w 180!");

        // Búsqueda
        txtBuscar = new JTextField();
        txtBuscar.putClientProperty("JTextField.placeholderText", "Buscar producto / proveedor...");
        txtBuscar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtBuscar.addActionListener(e -> filtrarEnTabla());
        filterBar.add(txtBuscar, "growx");

        // Botón Calcular
        btnCalcular = new JButton("⚡ Calcular y Valuar");
        btnCalcular.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCalcular.setBackground(tm.accent());
        btnCalcular.setForeground(Color.WHITE);
        btnCalcular.setFocusPainted(false);
        btnCalcular.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCalcular.addActionListener(e -> ejecutarCalculoValoracion());
        filterBar.add(btnCalcular);

        // Botón Exportar
        btnExportar = new JButton("📊 Exportar Excel");
        btnExportar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnExportar.setBackground(tm.greenAccent());
        btnExportar.setForeground(Color.WHITE);
        btnExportar.setFocusPainted(false);
        btnExportar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnExportar.addActionListener(e -> exportarExcel());
        filterBar.add(btnExportar);

        // ── Tarjetas KPI de Resumen ──
        JPanel kpiContainer = new JPanel(new GridLayout(1, 6, 12, 0));
        kpiContainer.setOpaque(false);

        kpiContainer.add(createKpiCard("📦 Total Artículos", lblKpiItems = new JLabel("0"), null));
        kpiContainer.add(createKpiCard("🔢 Total Unidades", lblKpiUnds = new JLabel("0.00"), null));
        kpiContainer.add(createKpiCard("🏢 Venta Firme", lblKpiFirmeUSD = new JLabel("$0.00"), lblKpiFirmeBS = new JLabel("Bs. 0.00")));
        kpiContainer.add(createKpiCard("🤝 Consignación", lblKpiConsigUSD = new JLabel("$0.00"), lblKpiConsigBS = new JLabel("Bs. 0.00")));
        kpiContainer.add(createKpiCard("💰 Total Global", lblKpiGlobalUSD = new JLabel("$0.00"), lblKpiGlobalBS = new JLabel("Bs. 0.00")));
        kpiContainer.add(createKpiCard("💵 Tasa BCV al Corte", lblKpiTasa = new JLabel("Bs. 0.00"), null));

        JPanel topGroup = new JPanel(new BorderLayout(0, 10));
        topGroup.setOpaque(false);
        topGroup.add(filterBar, BorderLayout.NORTH);
        topGroup.add(kpiContainer, BorderLayout.SOUTH);
        panel.add(topGroup, BorderLayout.NORTH);

        // ── Tabla de Resultados ──
        String[] columnas = {
                "Código", "Descripción", "Almacén", "Tipo", "Existencia",
                "Costo Promedio ($)", "Costo Promedio (Bs.)",
                "Último Costo ($)", "Último Costo (Bs.)",
                "Costo Prov. ($)",
                "Total Valorado ($)", "Total Valorado (Bs.)",
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

        // Alineaciones
        DefaultTableCellRenderer centerRend = new DefaultTableCellRenderer();
        centerRend.setHorizontalAlignment(SwingConstants.CENTER);
        tablaInventario.getColumnModel().getColumn(0).setCellRenderer(centerRend);
        tablaInventario.getColumnModel().getColumn(2).setCellRenderer(centerRend);
        tablaInventario.getColumnModel().getColumn(3).setCellRenderer(centerRend);
        tablaInventario.getColumnModel().getColumn(12).setCellRenderer(centerRend);

        DefaultTableCellRenderer rightRend = new DefaultTableCellRenderer();
        rightRend.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i = 4; i <= 11; i++) {
            tablaInventario.getColumnModel().getColumn(i).setCellRenderer(rightRend);
        }

        // Doble clic para ver capas FIFO del artículo
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

        JButton btnIrKardex = new JButton("📑 Ver Kárdex de este Producto →");
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

        // ── Filtros Kárdex ──
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
        txtKardexArt.putClientProperty("JTextField.placeholderText", "Ingrese código exacto (ej. 005741)");
        txtKardexArt.addActionListener(e -> consultarKardex());
        filterKardex.add(txtKardexArt, "w 220!");

        JLabel lblD = new JLabel("Desde:");
        lblD.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblD.setForeground(tm.textPrimary());
        filterKardex.add(lblD);

        dpKardexDesde = createStyledDatePicker();
        dpKardexDesde.setDate(YearMonth.now().atDay(1)); // 1er día de este mes
        filterKardex.add(dpKardexDesde, "w 160!, h 32!");

        JLabel lblH = new JLabel("Hasta:");
        lblH.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblH.setForeground(tm.textPrimary());
        filterKardex.add(lblH);

        dpKardexHasta = createStyledDatePicker();
        dpKardexHasta.setDate(LocalDate.now());
        filterKardex.add(dpKardexHasta, "w 160!, h 32!");

        // Badge indicador de base de datos para Kárdex
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

        btnConsultarKardex = new JButton("🔍 Consultar Kárdex");
        btnConsultarKardex.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnConsultarKardex.setBackground(tm.accent());
        btnConsultarKardex.setForeground(Color.WHITE);
        btnConsultarKardex.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnConsultarKardex.addActionListener(e -> consultarKardex());
        filterKardex.add(btnConsultarKardex, "h 32!");

        panel.add(filterKardex, BorderLayout.NORTH);

        // ── Tabla Kárdex ──
        String[] columnasK = {
                "Fecha", "Tipo Doc", "Nro. Doc", "Almacén", "Concepto / Origen / Destino",
                "Lote", "Entradas", "Salidas", "Saldo Existencia",
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
        for (int i = 6; i <= 12; i++) {
            tablaKardex.getColumnModel().getColumn(i).setCellRenderer(rightRend);
        }

        JScrollPane scrollKardex = new JScrollPane(tablaKardex);
        scrollKardex.setBorder(BorderFactory.createLineBorder(tm.border(), 1));
        panel.add(scrollKardex, BorderLayout.CENTER);

        // ── Barra Resumen Kárdex ──
        JPanel bottomKardex = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        bottomKardex.setBackground(tm.cardBg());
        bottomKardex.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, tm.border()));

        lblKardexResumen = new JLabel("Seleccione un código de producto y presione 'Consultar Kárdex'.");
        lblKardexResumen.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblKardexResumen.setForeground(tm.textSecondary());
        bottomKardex.add(lblKardexResumen);

        panel.add(bottomKardex, BorderLayout.SOUTH);

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
                    r.getCantCapas() > 0 ? (r.getCantCapas() + " Capas") : "-",
                    r.getNomProv() != null ? r.getNomProv() : r.getCoProv()
            });
        }
    }

    private void actualizarKpis(List<InventarioValoradoRow> rows, double tasa) {
        int itemsFirme = 0;
        double undsFirme = 0;
        double totalUsdFirme = 0;
        double totalBsFirme = 0;

        int itemsConsig = 0;
        double undsConsig = 0;
        double totalUsdConsig = 0;
        double totalBsConsig = 0;

        for (InventarioValoradoRow r : rows) {
            if ("CONSIGNACION".equalsIgnoreCase(r.getTipoMercancia())) {
                itemsConsig++;
                undsConsig += r.getExistencia();
                totalUsdConsig += r.getTotalValorizadoUSD();
                totalBsConsig += r.getTotalValorizadoBS();
            } else {
                itemsFirme++;
                undsFirme += r.getExistencia();
                totalUsdFirme += r.getTotalValorizadoUSD();
                totalBsFirme += r.getTotalValorizadoBS();
            }
        }

        lblKpiItems.setText(String.format("%,d", itemsFirme + itemsConsig));
        lblKpiUnds.setText(NF_DEC.format(undsFirme + undsConsig));

        lblKpiFirmeUSD.setText(NF_USD.format(totalUsdFirme));
        if (lblKpiFirmeBS != null) lblKpiFirmeBS.setText(NF_BS.format(totalBsFirme));

        lblKpiConsigUSD.setText(NF_USD.format(totalUsdConsig));
        if (lblKpiConsigBS != null) lblKpiConsigBS.setText(NF_BS.format(totalBsConsig));

        lblKpiGlobalUSD.setText(NF_USD.format(totalUsdFirme + totalUsdConsig));
        if (lblKpiGlobalBS != null) lblKpiGlobalBS.setText(NF_BS.format(totalBsFirme + totalBsConsig));

        lblKpiTasa.setText(String.format("Bs. %,.2f", tasa));
    }

    private void filtrarEnTabla() {
        String texto = txtBuscar.getText().trim();
        if (texto.isEmpty()) {
            sorterInventario.setRowFilter(null);
        } else {
            sorterInventario.setRowFilter(RowFilter.regexFilter("(?i)" + texto));
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
            tabbedPane.setSelectedIndex(1); // Cambiar a pestaña Kárdex
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

        btnConsultarKardex.setEnabled(false);
        btnConsultarKardex.setText("⏳ Consultando...");

        SwingWorker<List<KardexMovimientoValoradoRow>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<KardexMovimientoValoradoRow> doInBackground() {
                return dao.consultarKardexProducto(coArt, desde, hasta, filtroAlm);
            }

            @Override
            protected void done() {
                try {
                    List<KardexMovimientoValoradoRow> movs = get();
                    modelKardex.setRowCount(0);

                    double totEnt = 0;
                    double totSal = 0;
                    double saldoFinal = 0;

                    for (KardexMovimientoValoradoRow m : movs) {
                        modelKardex.addRow(new Object[]{
                                m.getFecha(),
                                m.getTipoDoc(),
                                m.getDocNum(),
                                m.getAlmacen(),
                                m.getOrigenDestino(),
                                m.getLote(),
                                m.getCantidadEntrada() > 0 ? NF_DEC.format(m.getCantidadEntrada()) : "-",
                                m.getCantidadSalida() > 0 ? NF_DEC.format(m.getCantidadSalida()) : "-",
                                NF_DEC.format(m.getSaldo()),
                                m.getCostoUnitUSD() > 0 ? NF_USD.format(m.getCostoUnitUSD()) : "-",
                                m.getCostoUnitBS() > 0 ? NF_BS.format(m.getCostoUnitBS()) : "-",
                                m.getTotalUSD() > 0 ? NF_USD.format(m.getTotalUSD()) : "-",
                                m.getTotalBS() > 0 ? NF_BS.format(m.getTotalBS()) : "-"
                        });

                        if (!"SALDO-INICIAL".equals(m.getTipoDoc())) {
                            totEnt += m.getCantidadEntrada();
                            totSal += m.getCantidadSalida();
                        }
                        saldoFinal = m.getSaldo();
                    }

                    String res = String.format("<html><b>Producto:</b> %s &nbsp;&nbsp;|&nbsp;&nbsp; <b>Total Entradas:</b> %s &nbsp;&nbsp;|&nbsp;&nbsp; <b>Total Salidas:</b> %s &nbsp;&nbsp;|&nbsp;&nbsp; <b>Saldo al Cierre:</b> <span style='color:%s;'>%s</span></html>",
                            coArt, NF_DEC.format(totEnt), NF_DEC.format(totSal),
                            tm.isDark() ? "#38bdf8" : "#0284c7", NF_DEC.format(saldoFinal));
                    lblKardexResumen.setText(res);
                } catch (Exception ex) {
                    logger.error("Error al consultar Kárdex: {}", ex.getMessage(), ex);
                    Toast.show("Error al consultar Kárdex: " + ex.getMessage(), Toast.Type.ERROR);
                } finally {
                    btnConsultarKardex.setEnabled(true);
                    btnConsultarKardex.setText("🔍 Consultar Kárdex");
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
}
