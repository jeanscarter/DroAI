package com.droai.ui;

import com.droai.dao.AuditoriaPedidosFacturacionDAO;
import com.droai.export.AuditoriaPedidosExporter;
import com.droai.model.AuditoriaPedidoRenglonRow;
import com.droai.model.AuditoriaPedidoRow;
import com.droai.ui.components.RoundedPanel;
import com.droai.ui.components.Toast;
import com.droai.ui.util.IconHelper;
import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ventana interactiva para auditar pedidos web enviados al SIAD vs.
 * facturación en Profit Plus (detección de casos tipo 101469, facturación parcial y faltantes).
 */
public class AuditoriaPedidosFacturacionFrame extends JFrame {

    private static final DecimalFormat DF_MONTO;
    private static final DecimalFormat DF_CANT;
    private static final SimpleDateFormat SDF_FECHA = new SimpleDateFormat("dd/MM/yyyy hh:mm a");

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.of("es", "VE"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        DF_MONTO = new DecimalFormat("#,##0.00", symbols);
        DF_CANT = new DecimalFormat("#,##0", symbols);
    }

    private final ThemeManager tm = ThemeManager.get();
    private final AuditoriaPedidosFacturacionDAO dao = new AuditoriaPedidosFacturacionDAO();

    // Filtros
    private DatePicker dateDesde;
    private DatePicker dateHasta;
    private JComboBox<String> cmbFiltroEstado;
    private JTextField txtBuscar;
    private JButton btnConsultar;
    private JButton btnExportar;

    // KPI Labels
    private JLabel lblKpiTotalPedidos;
    private JLabel lblKpiCriticos;
    private JLabel lblKpiParciales;
    private JLabel lblKpiPendientes;
    private JLabel lblKpiCompletos;
    private JLabel lblKpiMonto;

    // Tablas
    private DefaultTableModel modelMaster;
    private JTable tblMaster;

    private DefaultTableModel modelDetail;
    private JTable tblDetail;

    // Banner Detalle
    private JLabel lblBannerPedido;
    private JLabel lblBannerCliente;
    private JLabel lblBannerDiagnostico;

    // Datos en memoria
    private List<AuditoriaPedidoRow> listaTodos = new ArrayList<>();
    private List<AuditoriaPedidoRow> listaFiltrada = new ArrayList<>();

    public AuditoriaPedidosFacturacionFrame() {
        setTitle("DroAI — Auditoría de Pedidos Web vs. Facturación SIAD");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1400, 880);
        setMinimumSize(new Dimension(1100, 720));
        setLocationRelativeTo(null);

        IconHelper.applyAppIcon(this);
        Toast.setParentFrame(this);

        // Listener para cambio de tema
        Runnable themeListener = () -> {
            SwingUtilities.updateComponentTreeUI(this);
            repaint();
        };
        tm.addThemeChangeListener(themeListener);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                tm.removeThemeChangeListener(themeListener);
            }
        });

        initUI();
        consultarEnSegundoPlano();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(tm.background());
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setPaint(new GradientPaint(0, 0, tm.gradientTop(), 0, 180, tm.gradientBottom()));
                g2.fillRect(0, 0, getWidth(), 180);
                g2.dispose();
            }
        };
        root.setOpaque(false);

        // ═══════════════════════════════════════════════════════════
        // HEADER + FILTROS + KPIS
        // ═══════════════════════════════════════════════════════════
        JPanel topContainer = new JPanel(new MigLayout("insets 16 24 10 24, fillx, wrap", "[grow]", "[]8[]12[]"));
        topContainer.setOpaque(false);

        topContainer.add(buildHeader(), "growx");
        topContainer.add(buildFiltrosBar(), "growx");
        topContainer.add(buildKpiBar(), "growx");

        root.add(topContainer, BorderLayout.NORTH);

        // ═══════════════════════════════════════════════════════════
        // MASTER-DETAIL SPLIT PANE
        // ═══════════════════════════════════════════════════════════
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, buildMasterPanel(), buildDetailPanel());
        splitPane.setOpaque(false);
        splitPane.setBorder(new EmptyBorder(0, 24, 16, 24));
        splitPane.setResizeWeight(0.55);
        splitPane.setDividerSize(8);

        root.add(splitPane, BorderLayout.CENTER);

        setContentPane(root);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new MigLayout("insets 0, fillx", "[]12[]push[]", "[]"));
        p.setOpaque(false);

        JLabel lblIcon = new JLabel("📑");
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        p.add(lblIcon);

        JPanel texts = new JPanel(new MigLayout("insets 0, wrap, gap 0", "[]", "[]2[]"));
        texts.setOpaque(false);

        JLabel lblTitle = new JLabel("Auditoría de Pedidos Web vs. Facturación SIAD");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(tm.textPrimary());
        texts.add(lblTitle);

        JLabel lblSub = new JLabel("Detección de pedidos con renglones faltantes (tipo 101469), facturación parcial y notas 'B'");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(tm.textSecondary());
        texts.add(lblSub);

        p.add(texts);

        // Botón cambio de tema
        JButton btnTheme = new JButton(tm.isDark() ? "☀️ Modo Claro" : "🌙 Modo Oscuro");
        btnTheme.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnTheme.setFocusPainted(false);
        btnTheme.addActionListener(e -> {
            tm.toggleTheme();
            btnTheme.setText(tm.isDark() ? "☀️ Modo Claro" : "🌙 Modo Oscuro");
        });
        p.add(btnTheme);

        return p;
    }

    private JPanel buildFiltrosBar() {
        RoundedPanel p = new RoundedPanel(12, true);
        p.setBackground(tm.cardBg());
        p.setLayout(new MigLayout("insets 10 16 10 16, fillx", "[]8[]16[]8[]20[]8[grow]16[]8[]", "[]"));

        JLabel lblD = new JLabel("Desde:");
        lblD.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblD.setForeground(tm.textPrimary());
        p.add(lblD);

        dateDesde = createDatePicker();
        dateDesde.setDate(LocalDate.of(2026, 10, 1)); // Por defecto desde 01/10/2026 según solicitud
        p.add(dateDesde);

        JLabel lblH = new JLabel("Hasta:");
        lblH.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblH.setForeground(tm.textPrimary());
        p.add(lblH);

        dateHasta = createDatePicker();
        dateHasta.setDate(LocalDate.now());
        p.add(dateHasta);

        JLabel lblFiltro = new JLabel("Estado:");
        lblFiltro.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblFiltro.setForeground(tm.textPrimary());
        p.add(lblFiltro);

        cmbFiltroEstado = new JComboBox<>(new String[]{
                "Todos los Pedidos",
                "⚠️ Solo con Problemas / Alertas",
                "🔴 Faltan Renglones en Nota (Tipo 101469)",
                "🟠 Parcialmente Facturados",
                "🟡 Pendientes por Facturar",
                "🟢 Facturados Completos"
        });
        cmbFiltroEstado.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cmbFiltroEstado.addActionListener(e -> aplicarFiltrosLocales());
        p.add(cmbFiltroEstado);

        txtBuscar = new JTextField();
        txtBuscar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtBuscar.putClientProperty("JTextField.placeholderText", "🔍 Buscar por Pedido, Nota, Factura, Cliente o Vendedor...");
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { aplicarFiltrosLocales(); }
            @Override public void removeUpdate(DocumentEvent e) { aplicarFiltrosLocales(); }
            @Override public void changedUpdate(DocumentEvent e) { aplicarFiltrosLocales(); }
        });
        p.add(txtBuscar, "growx");

        btnConsultar = new JButton("🔄 Consultar");
        btnConsultar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnConsultar.setBackground(tm.accent());
        btnConsultar.setForeground(tm.btnForegroundFor(tm.accent()));
        btnConsultar.setFocusPainted(false);
        btnConsultar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnConsultar.addActionListener(e -> consultarEnSegundoPlano());
        p.add(btnConsultar);

        btnExportar = new JButton("📊 Exportar Excel");
        btnExportar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnExportar.setBackground(tm.greenAccent());
        btnExportar.setForeground(tm.btnForegroundFor(tm.greenAccent()));
        btnExportar.setFocusPainted(false);
        btnExportar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnExportar.addActionListener(e -> exportarExcel());
        p.add(btnExportar);

        return p;
    }

    private JPanel buildKpiBar() {
        JPanel bar = new JPanel(new MigLayout("insets 0, fillx, gap 12", "[grow][grow][grow][grow][grow][grow]", "[]"));
        bar.setOpaque(false);

        lblKpiTotalPedidos = new JLabel("0");
        lblKpiCriticos = new JLabel("0");
        lblKpiParciales = new JLabel("0");
        lblKpiPendientes = new JLabel("0");
        lblKpiCompletos = new JLabel("0");
        lblKpiMonto = new JLabel("$ 0,00");

        bar.add(createKpiCard("TOTAL PEDIDOS", lblKpiTotalPedidos, tm.textPrimary(), "📋"), "grow");
        bar.add(createKpiCard("FALTAN RENGLONES (101469)", lblKpiCriticos, tm.redAccent(), "🔴"), "grow");
        bar.add(createKpiCard("PARCIAL FACTURADOS", lblKpiParciales, tm.orangeAccent(), "🟠"), "grow");
        bar.add(createKpiCard("PENDIENTES FACTURA", lblKpiPendientes, new Color(234, 179, 8), "🟡"), "grow");
        bar.add(createKpiCard("FACTURADOS OK", lblKpiCompletos, tm.greenAccent(), "🟢"), "grow");
        bar.add(createKpiCard("MONTO FACTURADO", lblKpiMonto, tm.tealAccent(), "💵"), "grow");

        return bar;
    }

    private JPanel createKpiCard(String titulo, JLabel lblValor, Color colorAcento, String icon) {
        RoundedPanel card = new RoundedPanel(12, true);
        card.setBackground(tm.cardBg());
        card.setLayout(new MigLayout("insets 10 14 10 14, wrap", "[]", "[]2[]"));

        JLabel lblTop = new JLabel(icon + " " + titulo);
        lblTop.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblTop.setForeground(tm.textSecondary());
        card.add(lblTop);

        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblValor.setForeground(colorAcento);
        card.add(lblValor);

        return card;
    }

    private JPanel buildMasterPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JLabel lblSec = new JLabel("  Pedidos Web vs. Notas de Entrega Profit Plus");
        lblSec.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblSec.setForeground(tm.textPrimary());
        lblSec.setBorder(new EmptyBorder(0, 0, 6, 0));
        p.add(lblSec, BorderLayout.NORTH);

        String[] cols = {
                "Estado", "Pedido Web", "Nota Entrega", "Factura(s)", "Fecha Emisión",
                "Cliente", "Vendedor", "Reng Web", "Reng Nota", "Reng Fact", "Faltantes",
                "Total ($)", "Facturado ($)", "Pendiente ($)", "Diagnóstico"
        };

        modelMaster = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tblMaster = new JTable(modelMaster);
        tblMaster.setRowHeight(28);
        tblMaster.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tblMaster.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblMaster.setShowGrid(true);
        tblMaster.setGridColor(tm.border());

        JTableHeader th = tblMaster.getTableHeader();
        th.setFont(new Font("Segoe UI", Font.BOLD, 12));
        th.setBackground(tm.cardBg());
        th.setForeground(tm.textPrimary());

        // Renderers
        tblMaster.getColumnModel().getColumn(0).setCellRenderer(new EstadoBadgeRenderer());
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i : new int[]{7, 8, 9, 10, 11, 12, 13}) {
            tblMaster.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
        }

        tblMaster.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tblMaster.getSelectedRow();
                if (row >= 0 && row < listaFiltrada.size()) {
                    mostrarDetallePedido(listaFiltrada.get(row));
                }
            }
        });

        JScrollPane sp = new JScrollPane(tblMaster);
        sp.setBorder(BorderFactory.createLineBorder(tm.border()));
        sp.getViewport().setBackground(tm.cardBg());
        p.add(sp, BorderLayout.CENTER);

        return p;
    }

    private JPanel buildDetailPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        // Banner informativo del pedido seleccionado
        RoundedPanel banner = new RoundedPanel(10, false);
        banner.setBackground(tm.cardHover());
        banner.setLayout(new MigLayout("insets 8 14 8 14, fillx", "[]16[grow]push[]", "[]"));

        lblBannerPedido = new JLabel("Seleccione un pedido para inspeccionar sus renglones");
        lblBannerPedido.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblBannerPedido.setForeground(tm.textPrimary());
        banner.add(lblBannerPedido);

        lblBannerCliente = new JLabel("");
        lblBannerCliente.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblBannerCliente.setForeground(tm.textSecondary());
        banner.add(lblBannerCliente);

        lblBannerDiagnostico = new JLabel("");
        lblBannerDiagnostico.setFont(new Font("Segoe UI", Font.BOLD, 12));
        banner.add(lblBannerDiagnostico);

        p.add(banner, BorderLayout.NORTH);

        String[] colsDetalle = {
                "Estado Línea", "Reng#", "Código", "Descripción", "Cant Web",
                "Cant Nota", "Cant Fact", "Faltante", "Precio ($)", "% Desc",
                "Subtotal ($)", "Nro Lote", "Factura"
        };

        modelDetail = new DefaultTableModel(colsDetalle, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tblDetail = new JTable(modelDetail);
        tblDetail.setRowHeight(26);
        tblDetail.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        tblDetail.setShowGrid(true);
        tblDetail.setGridColor(tm.border());

        JTableHeader th = tblDetail.getTableHeader();
        th.setFont(new Font("Segoe UI", Font.BOLD, 11));
        th.setBackground(tm.cardBg());
        th.setForeground(tm.textPrimary());

        tblDetail.getColumnModel().getColumn(0).setCellRenderer(new DetalleEstadoBadgeRenderer());
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i : new int[]{1, 4, 5, 6, 7, 8, 10}) {
            tblDetail.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
        }

        JScrollPane sp = new JScrollPane(tblDetail);
        sp.setBorder(BorderFactory.createLineBorder(tm.border()));
        sp.getViewport().setBackground(tm.cardBg());
        p.add(sp, BorderLayout.CENTER);

        return p;
    }

    private DatePicker createDatePicker() {
        DatePickerSettings s = new DatePickerSettings();
        s.setFormatForDatesCommonEra("dd/MM/yyyy");
        s.setAllowEmptyDates(false);
        s.setColor(DatePickerSettings.DateArea.BackgroundOverallCalendarPanel, tm.cardBg());
        DatePicker dp = new DatePicker(s);
        dp.setPreferredSize(new Dimension(135, 30));
        return dp;
    }

    private void consultarEnSegundoPlano() {
        btnConsultar.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        LocalDate d = dateDesde.getDate();
        LocalDate h = dateHasta.getDate();

        SwingWorker<List<AuditoriaPedidoRow>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<AuditoriaPedidoRow> doInBackground() {
                return dao.auditarPedidos(d, h);
            }

            @Override
            protected void done() {
                try {
                    listaTodos = get();
                    aplicarFiltrosLocales();
                    Toast.show("Auditoría completada: " + listaTodos.size() + " pedidos analizados", Toast.Type.SUCCESS);
                } catch (Exception ex) {
                    Toast.show("Error al consultar datos: " + ex.getMessage(), Toast.Type.ERROR);
                } finally {
                    btnConsultar.setEnabled(true);
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };
        worker.execute();
    }

    private void aplicarFiltrosLocales() {
        String texto = txtBuscar.getText().trim().toLowerCase();
        int filtroIdx = cmbFiltroEstado.getSelectedIndex();

        listaFiltrada = new ArrayList<>();
        int criticos = 0;
        int parciales = 0;
        int pendientes = 0;
        int completos = 0;
        double montoFactTotal = 0.0;

        for (AuditoriaPedidoRow p : listaTodos) {
            // Contadores globales
            if ("CRITICO_FALTAN_RENGLONES".equals(p.getEstado())) criticos++;
            else if ("PARCIALMENTE_FACTURADO".equals(p.getEstado())) parciales++;
            else if ("PENDIENTE_FACTURAR".equals(p.getEstado())) pendientes++;
            else if ("FACTURADO_TOTAL".equals(p.getEstado())) completos++;
            montoFactTotal += p.getTotalMontoFact();

            // Filtro por Estado
            boolean matchEstado = switch (filtroIdx) {
                case 1 -> "CRITICO_FALTAN_RENGLONES".equals(p.getEstado()) || "PARCIALMENTE_FACTURADO".equals(p.getEstado());
                case 2 -> "CRITICO_FALTAN_RENGLONES".equals(p.getEstado());
                case 3 -> "PARCIALMENTE_FACTURADO".equals(p.getEstado());
                case 4 -> "PENDIENTE_FACTURAR".equals(p.getEstado());
                case 5 -> "FACTURADO_TOTAL".equals(p.getEstado());
                default -> true;
            };
            if (!matchEstado) continue;

            // Filtro por Texto
            if (!texto.isEmpty()) {
                boolean matchText = (p.getPedidoWeb() != null && p.getPedidoWeb().toLowerCase().contains(texto)) ||
                        (p.getNotaEntrega() != null && p.getNotaEntrega().toLowerCase().contains(texto)) ||
                        (p.getFacturas() != null && p.getFacturas().toLowerCase().contains(texto)) ||
                        (p.getCoCli() != null && p.getCoCli().toLowerCase().contains(texto)) ||
                        (p.getCliDes() != null && p.getCliDes().toLowerCase().contains(texto)) ||
                        (p.getVenDes() != null && p.getVenDes().toLowerCase().contains(texto));
                if (!matchText) continue;
            }

            listaFiltrada.add(p);
        }

        // Actualizar KPIs
        lblKpiTotalPedidos.setText(DF_CANT.format(listaTodos.size()));
        lblKpiCriticos.setText(DF_CANT.format(criticos));
        lblKpiParciales.setText(DF_CANT.format(parciales));
        lblKpiPendientes.setText(DF_CANT.format(pendientes));
        lblKpiCompletos.setText(DF_CANT.format(completos));
        lblKpiMonto.setText("$ " + DF_MONTO.format(montoFactTotal));

        // Refrescar Tabla Master
        modelMaster.setRowCount(0);
        for (AuditoriaPedidoRow p : listaFiltrada) {
            modelMaster.addRow(new Object[]{
                    p.getEstado(),
                    p.getPedidoWeb(),
                    p.getNotaEntrega(),
                    p.getFacturas() != null && !p.getFacturas().isEmpty() ? p.getFacturas() : "—",
                    p.getFecEmis() != null ? SDF_FECHA.format(p.getFecEmis()) : "",
                    p.getCliDes(),
                    p.getVenDes(),
                    p.getTotalRengWeb(),
                    p.getTotalRengNota(),
                    p.getTotalRengFact(),
                    p.getTotalRengFaltantes() > 0 ? "⚠️ " + p.getTotalRengFaltantes() : "0",
                    "$ " + DF_MONTO.format(p.getTotalMontoNota()),
                    "$ " + DF_MONTO.format(p.getTotalMontoFact()),
                    "$ " + DF_MONTO.format(p.getTotalMontoPend()),
                    p.getDiagnostico()
            });
        }

        if (!listaFiltrada.isEmpty()) {
            tblMaster.setRowSelectionInterval(0, 0);
            mostrarDetallePedido(listaFiltrada.get(0));
        } else {
            modelDetail.setRowCount(0);
            lblBannerPedido.setText("No hay pedidos para mostrar con el filtro actual");
            lblBannerCliente.setText("");
            lblBannerDiagnostico.setText("");
        }
    }

    private void mostrarDetallePedido(AuditoriaPedidoRow p) {
        lblBannerPedido.setText("Pedido Web: " + p.getPedidoWeb() + "  |  Nota: " + p.getNotaEntrega() + (p.getFacturas() != null && !p.getFacturas().isEmpty() ? "  |  Factura: " + p.getFacturas() : ""));
        lblBannerCliente.setText(p.getCliDes() + " (" + p.getCoCli() + ")");
        lblBannerDiagnostico.setText(p.getDiagnostico());

        if ("CRITICO_FALTAN_RENGLONES".equals(p.getEstado())) {
            lblBannerDiagnostico.setForeground(tm.redAccent());
        } else if ("PARCIALMENTE_FACTURADO".equals(p.getEstado())) {
            lblBannerDiagnostico.setForeground(tm.orangeAccent());
        } else {
            lblBannerDiagnostico.setForeground(tm.greenAccent());
        }

        modelDetail.setRowCount(0);
        if (p.getRenglones() != null) {
            for (AuditoriaPedidoRenglonRow r : p.getRenglones()) {
                modelDetail.addRow(new Object[]{
                        r.getEstado(),
                        r.getRengNum(),
                        r.getCoArt(),
                        r.getArtDes(),
                        DF_CANT.format(r.getCantWeb()),
                        DF_CANT.format(r.getCantNota()),
                        DF_CANT.format(r.getCantFact()),
                        r.getCantFaltante() > 0.001 ? "⚠️ " + DF_CANT.format(r.getCantFaltante()) : "0",
                        "$ " + DF_MONTO.format(r.getPrecVta()),
                        r.getPorcDesc() != null ? r.getPorcDesc() : "0",
                        "$ " + DF_MONTO.format(r.getRengNeto()),
                        r.getNroLote() != null && !r.getNroLote().isEmpty() ? r.getNroLote() : "—",
                        r.getFacturaDoc() != null && !r.getFacturaDoc().isEmpty() ? r.getFacturaDoc() : "—"
                });
            }
        }
    }

    private void exportarExcel() {
        if (listaFiltrada.isEmpty()) {
            Toast.show("No hay datos para exportar", Toast.Type.WARNING);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar Reporte de Auditoría de Pedidos");
        fc.setSelectedFile(new File("Auditoria_Pedidos_SIAD_" + LocalDate.now() + ".xlsx"));

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File dest = fc.getSelectedFile();
            if (!dest.getName().toLowerCase().endsWith(".xlsx")) {
                dest = new File(dest.getAbsolutePath() + ".xlsx");
            }
            try {
                AuditoriaPedidosExporter.exportar(listaFiltrada, dest);
                Toast.show("Reporte exportado exitosamente a " + dest.getName(), Toast.Type.SUCCESS);
            } catch (Exception ex) {
                Toast.show("Error al exportar: " + ex.getMessage(), Toast.Type.ERROR);
            }
        }
    }

    // ── Renderers ──

    private static class EstadoBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String st = String.valueOf(value);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));

            if ("CRITICO_FALTAN_RENGLONES".equals(st)) {
                lbl.setText("🔴 FALTAN RENGLONES");
                lbl.setForeground(new Color(239, 68, 68));
            } else if ("PARCIALMENTE_FACTURADO".equals(st)) {
                lbl.setText("🟠 PARCIAL FACTURADO");
                lbl.setForeground(new Color(249, 115, 22));
            } else if ("PENDIENTE_FACTURAR".equals(st)) {
                lbl.setText("🟡 PENDIENTE FACTURA");
                lbl.setForeground(new Color(234, 179, 8));
            } else if ("FACTURADO_TOTAL".equals(st)) {
                lbl.setText("🟢 FACTURADO OK");
                lbl.setForeground(new Color(34, 197, 94));
            } else if ("ANULADO".equals(st)) {
                lbl.setText("⚪ ANULADO");
                lbl.setForeground(Color.GRAY);
            } else {
                lbl.setText(st);
            }
            return lbl;
        }
    }

    private static class DetalleEstadoBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String st = String.valueOf(value);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));

            if ("FACTURADO".equals(st)) {
                lbl.setText("🟢 FACTURADO");
                lbl.setForeground(new Color(34, 197, 94));
            } else if ("NO_VIAJO_A_NOTA".equals(st)) {
                lbl.setText("🔴 NO VIAJÓ A NOTA");
                lbl.setForeground(new Color(239, 68, 68));
            } else if ("PENDIENTE_FACTURA".equals(st)) {
                lbl.setText("🟠 PENDIENTE FACTURA");
                lbl.setForeground(new Color(249, 115, 22));
            } else {
                lbl.setText(st);
            }
            return lbl;
        }
    }
}
