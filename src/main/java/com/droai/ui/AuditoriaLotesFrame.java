package com.droai.ui;

import com.droai.dao.AuditoriaLotesDAO;
import com.droai.model.DiscrepanciaLoteDTO;
import com.droai.model.KardexMovimientoDTO;
import com.droai.model.LoteDetalleDTO;
import com.droai.model.SesionUsuario;
import com.droai.ui.components.RoundedPanel;
import com.droai.ui.components.Toast;
import com.droai.ui.util.IconHelper;
import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Módulo de Auditoría y Control de Discrepancias entre Stock Almacén y Lotes.
 * Acceso restringido exclusivamente a DV y JG.
 */
public class AuditoriaLotesFrame extends JFrame {

    private static final DecimalFormat NF = new DecimalFormat("#,##0.00");
    public static final Set<String> USUARIOS_AUTORIZADOS = Set.of("DV", "JG");

    private final AuditoriaLotesDAO dao = new AuditoriaLotesDAO();
    private final ThemeManager tm = ThemeManager.get();

    // Modelos de tabla
    private DefaultTableModel modelDiscrepancias;
    private JTable tblDiscrepancias;

    private DefaultTableModel modelLotes;
    private JTable tblLotes;

    private DefaultTableModel modelKardex;
    private JTable tblKardex;

    // Filtros y KPIs
    private JTextField txtFiltro;
    private JCheckBox chkExcluirMermas;
    private JLabel lblKpiTotal;
    private JLabel lblKpiFaltante;
    private JLabel lblKpiExceso;

    // Banner de producto seleccionado
    private JLabel lblBannerCodigo;
    private JLabel lblBannerDescripcion;
    private JLabel lblBannerAlmacen;
    private JLabel lblBannerStockAlm;
    private JLabel lblBannerStockLot;
    private JLabel lblBannerDiferencia;

    // Datos del artículo seleccionado actualmente
    private List<LoteDetalleDTO> lotesActuales = null;

    public AuditoriaLotesFrame() {
        setTitle("DroAI — Auditoría y Control de Lotes vs. Almacén");
        setSize(1320, 840);
        setMinimumSize(new Dimension(1080, 700));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        IconHelper.applyAppIcon(this);

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
        cargarDiscrepancias();
    }

    /**
     * Valida si el usuario en sesión tiene autorización para abrir esta ventana.
     */
    public static boolean verificarAutorizacion(Component parent) {
        if (!SesionUsuario.isAutenticado()) {
            JOptionPane.showMessageDialog(parent,
                    "Debes iniciar sesión para acceder a este módulo.",
                    "Seguridad — Control de Acceso",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }

        String coUsuario = SesionUsuario.current().getCoUsuario().trim().toUpperCase();
        int nivel = SesionUsuario.current().getNivel();

        if (!USUARIOS_AUTORIZADOS.contains(coUsuario) && nivel > 0) {
            JOptionPane.showMessageDialog(parent,
                    "<html><body style='width: 320px; font-family: sans-serif;'>"
                    + "<h3 style='color: #ef4444; margin-bottom: 8px;'>🔒 Acceso Restringido</h3>"
                    + "El usuario <b>" + coUsuario + "</b> no tiene autorización para acceder al módulo de Auditoría de Lotes.<br><br>"
                    + "<span style='color: #64748b;'>Usuarios autorizados:</span> <b>DV, JG</b>.</body></html>",
                    "Seguridad — Control de Acceso",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private void initUI() {
        JPanel root = new JPanel(new MigLayout("insets 16, fill, wrap 1", "[grow]", "[]12[]12[grow]"));
        root.setBackground(tm.background());

        // 1. Barra de Encabezado
        root.add(buildHeaderBar(), "growx");

        // 2. Barra de Herramientas y KPIs
        root.add(buildToolAndKpiBar(), "growx");

        // 3. Panel Principal Dividido (Maestro - Detalle)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildMasterPanel(), buildDetailPanel());
        splitPane.setResizeWeight(0.50);
        splitPane.setDividerLocation(660);
        splitPane.setBorder(null);
        splitPane.setBackground(tm.background());
        root.add(splitPane, "grow");

        setContentPane(root);
    }

    private JPanel buildHeaderBar() {
        JPanel bar = new JPanel(new MigLayout("insets 10 16, fillx", "[grow][]", "[]"));
        bar.setBackground(tm.bgPanel());
        bar.putClientProperty(FlatClientProperties.STYLE, "arc: 12");

        JPanel titleGroup = new JPanel(new MigLayout("insets 0, gap 4", "[]", "[]2[]"));
        titleGroup.setOpaque(false);

        JLabel title = new JLabel("🔍 Auditoría y Control de Lotes vs. Existencia en Almacén");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(tm.textPrimary());
        titleGroup.add(title, "wrap");

        JLabel subtitle = new JLabel("Detección de descuadres, historial de movimientos Kardex y nivelación automática en 1-Clic");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(tm.textSecondary());
        titleGroup.add(subtitle);

        bar.add(titleGroup);

        JPanel actionsRight = new JPanel(new MigLayout("insets 0, gap 8", "[][]", "[]"));
        actionsRight.setOpaque(false);

        JButton btnVolver = new JButton("← Menú Principal");
        btnVolver.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnVolver.setBackground(tm.bgField());
        btnVolver.setForeground(tm.textPrimary());
        btnVolver.setFocusable(false);
        btnVolver.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVolver.addActionListener(e -> {
            new AdminDashboardFrame().setVisible(true);
            dispose();
        });
        actionsRight.add(btnVolver);

        JButton btnTema = new JButton(tm.isDark() ? "☀" : "🌙");
        btnTema.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        btnTema.setFocusPainted(false);
        btnTema.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnTema.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        btnTema.setToolTipText("Cambiar tema claro/oscuro");
        btnTema.addActionListener(e -> tm.toggleTheme());
        actionsRight.add(btnTema);

        bar.add(actionsRight, "right");
        return bar;
    }

    private JPanel buildToolAndKpiBar() {
        JPanel bar = new JPanel(new MigLayout("insets 12 16, fillx", "[grow]16[]", "[]"));
        bar.setBackground(tm.cardBg());
        bar.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
        bar.setBorder(BorderFactory.createLineBorder(tm.border(), 1));

        // Izquierda: Filtro de búsqueda
        JPanel searchBox = new JPanel(new MigLayout("insets 0, gap 8", "[grow][][][]", "[]"));
        searchBox.setOpaque(false);

        txtFiltro = new JTextField();
        txtFiltro.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Filtrar por código o nombre del producto...");
        txtFiltro.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtFiltro.addActionListener(e -> cargarDiscrepancias());
        searchBox.add(txtFiltro, "width 280!");

        JButton btnBuscar = new JButton("Filtrar");
        btnBuscar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnBuscar.setBackground(tm.accent());
        btnBuscar.setForeground(Color.WHITE);
        btnBuscar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnBuscar.addActionListener(e -> cargarDiscrepancias());
        searchBox.add(btnBuscar);

        JButton btnLimpiar = new JButton("Limpiar");
        btnLimpiar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnLimpiar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLimpiar.addActionListener(e -> {
            txtFiltro.setText("");
            cargarDiscrepancias();
        });
        searchBox.add(btnLimpiar);

        chkExcluirMermas = new JCheckBox("Excluir Averías / Venc. (VC, AV, VP)", true);
        chkExcluirMermas.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chkExcluirMermas.setForeground(tm.textPrimary());
        chkExcluirMermas.setOpaque(false);
        chkExcluirMermas.setFocusable(false);
        chkExcluirMermas.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chkExcluirMermas.setToolTipText("Excluye almacenes de avería y vencimientos (VC, AV, VP)");
        chkExcluirMermas.addActionListener(e -> cargarDiscrepancias());
        searchBox.add(chkExcluirMermas, "gapleft 6");

        bar.add(searchBox);

        // Derecha: KPIs y Botón de Nivelación Automática
        JPanel rightGroup = new JPanel(new MigLayout("insets 0, gap 12", "[][][][]", "[]"));
        rightGroup.setOpaque(false);

        lblKpiTotal = new JLabel("Total Discrepancias: 0");
        lblKpiTotal.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblKpiTotal.setForeground(tm.textPrimary());
        rightGroup.add(lblKpiTotal);

        lblKpiFaltante = new JLabel("Faltan Lote: 0");
        lblKpiFaltante.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblKpiFaltante.setForeground(new Color(239, 68, 68)); // Rojo
        rightGroup.add(lblKpiFaltante);

        lblKpiExceso = new JLabel("Exceso Lote: 0");
        lblKpiExceso.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblKpiExceso.setForeground(new Color(245, 158, 11)); // Naranja
        rightGroup.add(lblKpiExceso);

        // BOTON DE NIVELACION AUTOMATICA EN 1-CLIC
        JButton btnAutoAjustar = new JButton("⚡ Auto-Ajustar y Nivelar Todo");
        btnAutoAjustar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnAutoAjustar.setBackground(new Color(16, 185, 129)); // Verde Esmeralda
        btnAutoAjustar.setForeground(Color.WHITE);
        btnAutoAjustar.setFocusPainted(false);
        btnAutoAjustar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnAutoAjustar.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        btnAutoAjustar.setToolTipText("Nivela automáticamente todos los productos con descuadre en la base de datos");
        btnAutoAjustar.addActionListener(e -> ejecutarAutoAjuste());
        rightGroup.add(btnAutoAjustar);

        bar.add(rightGroup, "right");
        return bar;
    }

    private JPanel buildMasterPanel() {
        JPanel panel = new JPanel(new MigLayout("insets 0, fill, wrap 1", "[grow]", "[][grow]"));
        panel.setOpaque(false);

        JLabel lblSecTitle = new JLabel("Productos con Discrepancia (Almacén vs. Lotes)");
        lblSecTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblSecTitle.setForeground(tm.textPrimary());
        panel.add(lblSecTitle, "gapbottom 4");

        String[] cols = {"Código", "Descripción", "Alm.", "Stock Alm.", "Stock Lotes", "Diferencia", "Estado"};
        modelDiscrepancias = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tblDiscrepancias = new JTable(modelDiscrepancias);
        tblDiscrepancias.setRowHeight(28);
        tblDiscrepancias.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblDiscrepancias.getTableHeader().setReorderingAllowed(false);

        // Anchos de columna
        tblDiscrepancias.getColumnModel().getColumn(0).setPreferredWidth(75);
        tblDiscrepancias.getColumnModel().getColumn(1).setPreferredWidth(230);
        tblDiscrepancias.getColumnModel().getColumn(2).setPreferredWidth(45);
        tblDiscrepancias.getColumnModel().getColumn(3).setPreferredWidth(80);
        tblDiscrepancias.getColumnModel().getColumn(4).setPreferredWidth(80);
        tblDiscrepancias.getColumnModel().getColumn(5).setPreferredWidth(80);
        tblDiscrepancias.getColumnModel().getColumn(6).setPreferredWidth(120);

        // Renderer personalizado con colores
        tblDiscrepancias.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? tm.cardBg() : tm.bgPanel());
                    c.setForeground(tm.textPrimary());

                    // Colorear columna de Diferencia y Estado
                    if (col == 5 || col == 6) {
                        try {
                            double diff = Double.parseDouble(table.getValueAt(row, 5).toString().replace(",", "."));
                            if (diff > 0.001) {
                                c.setForeground(new Color(239, 68, 68)); // Rojo: falta lote
                            } else if (diff < -0.001) {
                                c.setForeground(new Color(245, 158, 11)); // Naranja: exceso en lote
                            } else {
                                c.setForeground(new Color(16, 185, 129)); // Verde
                            }
                        } catch (Exception ignored) {}
                    }
                }
                if (col == 3 || col == 4 || col == 5) {
                    setHorizontalAlignment(SwingConstants.RIGHT);
                } else if (col == 0 || col == 2) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }
                return c;
            }
        });

        tblDiscrepancias.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tblDiscrepancias.getSelectedRow();
                if (row >= 0) {
                    String coArt = tblDiscrepancias.getValueAt(row, 0).toString();
                    String coAlma = tblDiscrepancias.getValueAt(row, 2).toString();
                    cargarDetalleProducto(coArt, coAlma);
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tblDiscrepancias);
        scroll.setBorder(BorderFactory.createLineBorder(tm.border(), 1));
        panel.add(scroll, "grow");

        return panel;
    }

    private JPanel buildDetailPanel() {
        JPanel panel = new JPanel(new MigLayout("insets 0 12 0 0, fill, wrap 1", "[grow]", "[][grow]"));
        panel.setOpaque(false);

        // Banner informativo superior
        RoundedPanel banner = new RoundedPanel(12, true);
        banner.setBackground(tm.cardBg());
        banner.setLayout(new MigLayout("insets 12 16, fillx, wrap 2", "[grow][right]", "[]4[]"));
        banner.setBorder(BorderFactory.createLineBorder(tm.border(), 1));

        lblBannerCodigo = new JLabel("Seleccione un producto de la tabla izquierda");
        lblBannerCodigo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblBannerCodigo.setForeground(tm.textPrimary());
        banner.add(lblBannerCodigo);

        lblBannerAlmacen = new JLabel("");
        lblBannerAlmacen.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblBannerAlmacen.setForeground(tm.accent());
        banner.add(lblBannerAlmacen, "right");

        lblBannerDescripcion = new JLabel("");
        lblBannerDescripcion.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblBannerDescripcion.setForeground(tm.textSecondary());
        banner.add(lblBannerDescripcion, "span 2");

        JPanel statsRow = new JPanel(new MigLayout("insets 4 0 0 0, gap 16", "[][][]", "[]"));
        statsRow.setOpaque(false);

        lblBannerStockAlm = new JLabel("Stock Almacén: -");
        lblBannerStockAlm.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statsRow.add(lblBannerStockAlm);

        lblBannerStockLot = new JLabel("Stock Lotes: -");
        lblBannerStockLot.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statsRow.add(lblBannerStockLot);

        lblBannerDiferencia = new JLabel("Descuadre: -");
        lblBannerDiferencia.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statsRow.add(lblBannerDiferencia);

        banner.add(statsRow, "span 2, gaptop 4");
        panel.add(banner, "growx, gapbottom 8");

        // Tabbed Pane con Lotes y Kardex
        JTabbedPane tabbed = new JTabbedPane();
        tabbed.setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Tab 1: Lotes Actuales
        JPanel tabLotes = new JPanel(new MigLayout("insets 8, fill, wrap 1", "[grow]", "[][grow]"));
        tabLotes.setBackground(tm.background());

        JPanel lotesActionBar = new JPanel(new MigLayout("insets 0, fillx", "[grow][]", "[]"));
        lotesActionBar.setOpaque(false);
        JLabel lblLotesTitle = new JLabel("Lotes registrados en saLoteEntrada:");
        lblLotesTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblLotesTitle.setForeground(tm.textPrimary());
        lotesActionBar.add(lblLotesTitle);

        JPanel lotesActionsGroup = new JPanel(new MigLayout("insets 0, gap 8", "[][]", "[]"));
        lotesActionsGroup.setOpaque(false);

        JButton btnCrearLote = new JButton("➕ Asignar / Crear Lote");
        btnCrearLote.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCrearLote.setBackground(new Color(16, 185, 129)); // Verde Esmeralda
        btnCrearLote.setForeground(Color.WHITE);
        btnCrearLote.setFocusPainted(false);
        btnCrearLote.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCrearLote.setToolTipText("Crea o asigna un lote y vencimiento real a este artículo para nivelar el descuadre");
        btnCrearLote.addActionListener(e -> abrirModalCrearLote());
        lotesActionsGroup.add(btnCrearLote);

        JButton btnModificarLote = new JButton("✏️ Modificar Lote / Vencimiento");
        btnModificarLote.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnModificarLote.setBackground(tm.accent());
        btnModificarLote.setForeground(Color.WHITE);
        btnModificarLote.setFocusPainted(false);
        btnModificarLote.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnModificarLote.addActionListener(e -> abrirModalModificarLote());
        lotesActionsGroup.add(btnModificarLote);

        lotesActionBar.add(lotesActionsGroup, "right");
        tabLotes.add(lotesActionBar, "growx");

        String[] colsLotes = {"Lote", "Vencimiento", "Inicial", "Disponible", "Tipo Doc.", "Fecha Ingreso"};
        modelLotes = new DefaultTableModel(colsLotes, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblLotes = new JTable(modelLotes);
        tblLotes.setRowHeight(26);
        tblLotes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabLotes.add(new JScrollPane(tblLotes), "grow");
        tabbed.addTab("📦 Lotes en Existencia", tabLotes);

        // Tab 2: Historial Kardex
        JPanel tabKardex = new JPanel(new MigLayout("insets 8, fill, wrap 1", "[grow]", "[grow]"));
        tabKardex.setBackground(tm.background());

        String[] colsKardex = {"Tipo", "Documento", "Fecha", "Cant.", "Lote Asig.", "Detalle / Tercero"};
        modelKardex = new DefaultTableModel(colsKardex, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblKardex = new JTable(modelKardex);
        tblKardex.setRowHeight(26);
        tabKardex.add(new JScrollPane(tblKardex), "grow");
        tabbed.addTab("📜 Historial de Movimientos (Kardex)", tabKardex);

        panel.add(tabbed, "grow");
        return panel;
    }

    private void cargarDiscrepancias() {
        String filtro = txtFiltro.getText();
        boolean excluirMermas = chkExcluirMermas != null && chkExcluirMermas.isSelected();
        SwingWorker<List<DiscrepanciaLoteDTO>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<DiscrepanciaLoteDTO> doInBackground() {
                return dao.listarDiscrepancias(filtro, excluirMermas);
            }

            @Override
            protected void done() {
                try {
                    List<DiscrepanciaLoteDTO> lista = get();
                    modelDiscrepancias.setRowCount(0);

                    int total = lista.size();
                    int faltantes = 0;
                    int excesos = 0;

                    for (DiscrepanciaLoteDTO d : lista) {
                        if (d.getDiferencia() > 0.001) faltantes++;
                        else if (d.getDiferencia() < -0.001) excesos++;

                        modelDiscrepancias.addRow(new Object[]{
                                d.getCoArt(),
                                d.getArtDes(),
                                d.getCoAlma(),
                                NF.format(d.getStockAlmacen()),
                                NF.format(d.getStockLotes()),
                                NF.format(d.getDiferencia()),
                                d.getEstado()
                        });
                    }

                    lblKpiTotal.setText("Total Discrepancias: " + total);
                    lblKpiFaltante.setText("Faltan Lote: " + faltantes);
                    lblKpiExceso.setText("Exceso Lote: " + excesos);

                    if (total > 0 && tblDiscrepancias.getSelectedRow() < 0) {
                        tblDiscrepancias.setRowSelectionInterval(0, 0);
                    }
                } catch (Exception ex) {
                    Toast.show("Error al cargar discrepancias: " + ex.getMessage(), Toast.Type.ERROR);
                }
            }
        };
        worker.execute();
    }

    private void cargarDetalleProducto(String coArt, String coAlma) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private List<LoteDetalleDTO> lotes;
            private List<KardexMovimientoDTO> kardex;

            @Override
            protected Void doInBackground() {
                lotes = dao.listarLotesArticulo(coArt, coAlma);
                kardex = dao.listarHistorialKardex(coArt);
                return null;
            }

            @Override
            protected void done() {
                lotesActuales = lotes;

                // Actualizar Banner
                int row = tblDiscrepancias.getSelectedRow();
                if (row >= 0) {
                    lblBannerCodigo.setText("Artículo: " + tblDiscrepancias.getValueAt(row, 0));
                    lblBannerDescripcion.setText(tblDiscrepancias.getValueAt(row, 1).toString());
                    lblBannerAlmacen.setText("Almacén: " + tblDiscrepancias.getValueAt(row, 2));
                    lblBannerStockAlm.setText("Stock Almacén: " + tblDiscrepancias.getValueAt(row, 3));
                    lblBannerStockLot.setText("Stock Lotes: " + tblDiscrepancias.getValueAt(row, 4));
                    lblBannerDiferencia.setText("Descuadre: " + tblDiscrepancias.getValueAt(row, 5));
                }

                // Cargar tabla de lotes
                modelLotes.setRowCount(0);
                for (LoteDetalleDTO l : lotes) {
                    modelLotes.addRow(new Object[]{
                            l.getNumeroLote(),
                            l.getFechaExpiracion(),
                            NF.format(l.getCantidadInicial()),
                            NF.format(l.getStockActual()),
                            l.getTipoDoc(),
                            l.getFechaIngreso()
                    });
                }

                // Cargar tabla de kardex
                modelKardex.setRowCount(0);
                for (KardexMovimientoDTO k : kardex) {
                    modelKardex.addRow(new Object[]{
                            k.getTipoDoc(),
                            k.getDocNum(),
                            k.getFecha(),
                            NF.format(k.getCantidad()),
                            k.isLoteAsignado() ? "SÍ" : "NO",
                            k.getOrigenODestino()
                    });
                }
            }
        };
        worker.execute();
    }

    private void abrirModalCrearLote() {
        int selRow = tblDiscrepancias.getSelectedRow();
        if (selRow < 0) {
            Toast.show("Selecciona un producto de la tabla izquierda para asignarle lote.", Toast.Type.WARNING);
            return;
        }

        String coArt = tblDiscrepancias.getValueAt(selRow, 0).toString().trim();
        String artDes = tblDiscrepancias.getValueAt(selRow, 1).toString().trim();
        String coAlma = tblDiscrepancias.getValueAt(selRow, 2).toString().trim();
        String strDiff = tblDiscrepancias.getValueAt(selRow, 5).toString().replace(".", "").replace(",", ".");
        double diff = 0.0;
        try {
            diff = Double.parseDouble(strDiff);
        } catch (Exception ignored) {}

        double cantSugerida = diff > 0.0001 ? diff : 1.0;
        String fechaSugerida = LocalDate.now().plusYears(3).toString();

        JTextField txtNumeroLote = new JTextField();
        txtNumeroLote.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ej: LOTE-1234 o N° del empaque");

        JTextField txtFechaVenc = new JTextField(fechaSugerida);
        txtFechaVenc.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "YYYY-MM-DD");

        JTextField txtCantidad = new JTextField(NF.format(cantSugerida));

        JPanel form = new JPanel(new MigLayout("insets 16, wrap 2, fillx", "[label]12[grow, fill]", "[]6[]12[]8[]8[]"));

        JLabel lblInfoArt = new JLabel("<html><b>Artículo:</b> " + coArt + " — " + artDes + "</html>");
        lblInfoArt.setForeground(tm.textPrimary());
        form.add(lblInfoArt, "span 2");

        JLabel lblInfoAlma = new JLabel("<html><b>Almacén:</b> " + coAlma + " &nbsp;|&nbsp; <b>Descuadre actual:</b> <span style='color: " + (diff > 0 ? "#ef4444" : "#10b981") + ";'>" + NF.format(diff) + "</span></html>");
        lblInfoAlma.setForeground(tm.textSecondary());
        form.add(lblInfoAlma, "span 2");

        form.add(new JLabel("Número de Lote:"));
        form.add(txtNumeroLote);

        form.add(new JLabel("Vencimiento (YYYY-MM-DD):"));
        form.add(txtFechaVenc);

        form.add(new JLabel("Cantidad a ingresar:"));
        form.add(txtCantidad);

        int res = JOptionPane.showConfirmDialog(this, form,
                "➕ Asignar / Crear Lote — " + coArt + " (" + coAlma + ")",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (res == JOptionPane.OK_OPTION) {
            String nuevoLote = txtNumeroLote.getText().trim();
            String nuevaFecha = txtFechaVenc.getText().trim();
            String cantRaw = txtCantidad.getText().trim();

            if (nuevoLote.isBlank()) {
                Toast.show("El número de lote es obligatorio.", Toast.Type.WARNING);
                return;
            }

            if (!nuevaFecha.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                Toast.show("La fecha debe tener formato válido YYYY-MM-DD (ej: 2027-12-31).", Toast.Type.WARNING);
                return;
            }

            double cantidad;
            try {
                if (cantRaw.contains(",") && cantRaw.contains(".")) {
                    cantRaw = cantRaw.replace(".", "").replace(",", ".");
                } else if (cantRaw.contains(",")) {
                    cantRaw = cantRaw.replace(",", ".");
                }
                cantidad = Double.parseDouble(cantRaw);
                if (cantidad <= 0.0001) {
                    Toast.show("La cantidad debe ser mayor a 0.", Toast.Type.WARNING);
                    return;
                }
            } catch (NumberFormatException nfe) {
                Toast.show("La cantidad ingresada no es un número válido.", Toast.Type.WARNING);
                return;
            }

            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            String coUsuario = SesionUsuario.isAutenticado() ? SesionUsuario.current().getCoUsuario() : "DROAI";

            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    return dao.registrarNuevoLote(coArt, coAlma, nuevoLote, nuevaFecha, cantidad, coUsuario);
                }

                @Override
                protected void done() {
                    setCursor(Cursor.getDefaultCursor());
                    try {
                        boolean ok = get();
                        if (ok) {
                            Toast.show("¡Lote '" + nuevoLote + "' registrado exitosamente (" + NF.format(cantidad) + " unids)!", Toast.Type.SUCCESS);
                            cargarDiscrepancias();
                            cargarDetalleProducto(coArt, coAlma);
                        } else {
                            Toast.show("No se pudo registrar el lote.", Toast.Type.ERROR);
                        }
                    } catch (Exception ex) {
                        Toast.show("Error al registrar lote: " + ex.getMessage(), Toast.Type.ERROR);
                    }
                }
            };
            worker.execute();
        }
    }

    private void abrirModalModificarLote() {
        int row = tblLotes.getSelectedRow();
        if (row < 0 || lotesActuales == null || row >= lotesActuales.size()) {
            Toast.show("Selecciona un lote de la tabla para modificarlo.", Toast.Type.WARNING);
            return;
        }

        LoteDetalleDTO lote = lotesActuales.get(row);

        JTextField txtNuevoLote = new JTextField(lote.getNumeroLote());
        JTextField txtNuevaFecha = new JTextField(lote.getFechaExpiracion());

        JPanel form = new JPanel(new MigLayout("insets 16, wrap 2, fillx", "[label]12[grow, fill]", "[]8[]"));
        form.add(new JLabel("Número de Lote:"));
        form.add(txtNuevoLote);
        form.add(new JLabel("Vencimiento (YYYY-MM-DD):"));
        form.add(txtNuevaFecha);

        int res = JOptionPane.showConfirmDialog(this, form,
                "Modificar Lote: " + lote.getNumeroLote() + " (Art: " + lote.getCoArt() + ")",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (res == JOptionPane.OK_OPTION) {
            String nuevoLote = txtNuevoLote.getText().trim();
            String nuevaFecha = txtNuevaFecha.getText().trim();

            if (nuevoLote.isBlank() || nuevaFecha.isBlank()) {
                Toast.show("El número de lote y la fecha no pueden estar vacíos.", Toast.Type.WARNING);
                return;
            }

            String coUsuario = SesionUsuario.isAutenticado() ? SesionUsuario.current().getCoUsuario() : "DROAI";
            boolean ok = dao.actualizarLoteYVencimiento(lote.getRowguid(), nuevoLote, nuevaFecha, coUsuario);

            if (ok) {
                Toast.show("Lote y fecha actualizados exitosamente.", Toast.Type.SUCCESS);
                cargarDetalleProducto(lote.getCoArt(), lote.getCoAlma());
            } else {
                Toast.show("No se pudo actualizar el lote.", Toast.Type.ERROR);
            }
        }
    }

    private void ejecutarAutoAjuste() {
        boolean excluirMermas = chkExcluirMermas != null && chkExcluirMermas.isSelected();
        String avisoFiltro = excluirMermas
                ? "<li><b>Filtro activo:</b> Se excluirán los almacenes de mermas y vencimientos (<b>VC, AV, VP</b>).</li>"
                : "";

        int confirm = JOptionPane.showConfirmDialog(this,
                "<html><body style='width: 380px; font-family: sans-serif;'>"
                + "<h3 style='color: #10b981; margin-bottom: 8px;'>⚡ Nivelación Automática de Lotes</h3>"
                + "Se procederá a nivelar automáticamente los artículos con descuadre:<br>"
                + "<ul>"
                + avisoFiltro
                + "<li>Los artículos sin lote o con lote insuficiente recibirán su respaldo en <b>saLoteEntrada</b>.</li>"
                + "<li>Los lotes en exceso serán sincronizados con la existencia física real.</li>"
                + "<li>Las recepciones pendientes serán marcadas como asignadas.</li>"
                + "</ul>"
                + "¿Deseas continuar?</body></html>",
                "Confirmar Auto-Ajuste Global",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        String coUsuario = SesionUsuario.isAutenticado() ? SesionUsuario.current().getCoUsuario() : "DROAI";

        SwingWorker<Integer, Void> worker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() throws Exception {
                return dao.autoAjustarTodo(coUsuario, excluirMermas);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    int ajustados = get();
                    Toast.show("¡Nivelación exitosa! Se ajustaron " + ajustados + " artículos.", Toast.Type.SUCCESS);
                    cargarDiscrepancias();
                } catch (Exception ex) {
                    Toast.show("Error en auto-ajuste: " + ex.getMessage(), Toast.Type.ERROR);
                }
            }
        };
        worker.execute();
    }
}
