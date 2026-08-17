package com.droai.ui;

import com.droai.dao.AjusteInventarioDAO;
import com.droai.model.AjusteProductoDTO;
import com.droai.model.StockAlmacenRow;
import com.droai.model.StockLoteRow;
import com.droai.model.SesionUsuario;
import com.droai.ui.components.Toast;
import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Ventana para realizar Ajustes de Inventario (Entrada y Salida).
 * Muestra el stock total en todos los almacenes, precio, costo y desglose
 * detallado por almacén, por lote y por fecha de vencimiento.
 */
public class AjusteInventarioFrame extends JFrame {

    private static final DecimalFormat NF = new DecimalFormat("#,##0.00");
    private static final DecimalFormat NF_INT = new DecimalFormat("#,##0");

    private final AjusteInventarioDAO dao = new AjusteInventarioDAO();
    private AjusteProductoDTO productoSeleccionado = null;

    // Componentes Búsqueda
    private JTextField txtBusqueda;
    private JTable tblBusqueda;
    private DefaultTableModel modelBusqueda;

    // Ficha Resumen Producto
    private JLabel lblHeaderCodigo;
    private JLabel lblHeaderDescripcion;
    private JLabel lblHeaderMarca;
    private JLabel lblValStockTotal;
    private JLabel lblValPrecio;
    private JLabel lblValCosto;
    private JLabel lblInfoPrecioVta;

    // Desglose TabbedPane
    private JTabbedPane tabbedPaneDesglose;
    private JTable tblStockAlmacen;
    private DefaultTableModel modelStockAlmacen;
    private JTable tblStockLote;
    private DefaultTableModel modelStockLote;

    // Formulario de Ajuste
    private JRadioButton rbEntrada;
    private JRadioButton rbSalida;
    private JComboBox<StockAlmacenRow> cmbAlmacen;
    private JTextField txtLote;
    private JTextField txtFechaVencimiento;
    private JTextField txtCantidad;
    private JTextField txtCosto;
    private JTextField txtMotivo;
    private JButton btnProcesar;

    // ── Colores dinámicos vía ThemeManager ──
    private final ThemeManager tm = ThemeManager.get();
    // Acento cyan específico de este módulo
    private Color accentCyan() { return tm.isDark() ? new Color(56, 189, 248) : new Color(14, 165, 233); }

    private static final java.util.Set<String> USUARIOS_AJUSTE_PERMITIDOS = java.util.Set.of("JG", "OP", "JR", "ND");

    public AjusteInventarioFrame() {
        setTitle("DroAI — Ajustes de Inventario (Entrada y Salida)");
        setSize(1280, 820);
        setMinimumSize(new Dimension(1024, 720));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

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
        cargarAlmacenes();
    }

    private void initUI() {
        JPanel root = new JPanel(new MigLayout("insets 16, fill, wrap 1", "[grow]", "[]12[]12[grow]"));
        root.setBackground(tm.background());

        // 1. Header Bar
        root.add(buildHeaderBar(), "growx");

        // 2. Buscador de Productos
        root.add(buildSearchPanel(), "growx");

        // 3. Main Content (Izquierda: Detalle & Desglose | Derecha: Formulario Ajuste)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildLeftPanel(), buildRightPanel());
        splitPane.setResizeWeight(0.65);
        splitPane.setDividerLocation(750);
        splitPane.setBorder(null);
        splitPane.setBackground(tm.background());
        root.add(splitPane, "grow");

        setContentPane(root);
    }

    private JPanel buildHeaderBar() {
        JPanel bar = new JPanel(new MigLayout("insets 10 16, fillx", "[grow][]", "[]"));
        bar.setBackground(tm.bgPanel());
        bar.putClientProperty(FlatClientProperties.STYLE, "arc: 12");

        JLabel title = new JLabel("Ajustes de Inventario (Entrada / Salida)");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(tm.textPrimary());

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

        bar.add(title);

        // Botón de tema
        JButton btnTema = new JButton(tm.isDark() ? "☀" : "🌙");
        btnTema.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        btnTema.setFocusPainted(false);
        btnTema.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnTema.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        btnTema.setToolTipText("Cambiar tema claro/oscuro");
        btnTema.addActionListener(e -> tm.toggleTheme());
        bar.add(btnTema);

        bar.add(btnVolver);
        return bar;
    }

    private JPanel buildSearchPanel() {
        JPanel panel = new JPanel(new MigLayout("insets 14, fillx, wrap 2", "[grow][]", "[]8[]"));
        panel.setBackground(tm.bgSection());
        panel.putClientProperty(FlatClientProperties.STYLE, "arc: 12");

        txtBusqueda = new JTextField();
        txtBusqueda.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "🔍 Buscar por Código, Descripción, Marca o Código de Barras...");
        txtBusqueda.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtBusqueda.setBackground(tm.bgField());
        txtBusqueda.setForeground(tm.textPrimary());
        txtBusqueda.setCaretColor(tm.textPrimary());
        txtBusqueda.addActionListener(e -> buscarProductos());

        JButton btnBuscar = new JButton("Buscar");
        btnBuscar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnBuscar.setBackground(tm.accent());
        btnBuscar.setForeground(Color.WHITE);
        btnBuscar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnBuscar.addActionListener(e -> buscarProductos());

        panel.add(txtBusqueda, "growx");
        panel.add(btnBuscar, "w 110!, h 36!");

        // Tabla de resultados de búsqueda rápida (incluye Costo, Precio S/IVA y Precio C/IVA)
        String[] cols = {"Código", "Descripción", "Marca", "Código Barra", "Stock Total", "Costo ($)", "Precio S/IVA ($)", "Precio C/IVA ($)"};
        modelBusqueda = new DefaultTableModel(new Object[0][8], cols) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        tblBusqueda = createStyledTable(modelBusqueda);
        tblBusqueda.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tblBusqueda.getSelectedRow();
                if (row >= 0) {
                    String codigo = (String) modelBusqueda.getValueAt(row, 0);
                    cargarProductoSeleccionado(codigo);
                }
            }
        });

        JScrollPane scrollBusqueda = new JScrollPane(tblBusqueda);
        scrollBusqueda.setPreferredSize(new Dimension(0, 110));
        scrollBusqueda.getViewport().setBackground(tm.bgPanel());
        scrollBusqueda.setBorder(BorderFactory.createLineBorder(tm.border(), 1));
        panel.add(scrollBusqueda, "span 2, growx");

        return panel;
    }

    private JPanel buildLeftPanel() {
        JPanel left = new JPanel(new MigLayout("insets 0, fill, wrap 1", "[grow]", "[]12[grow]"));
        left.setOpaque(false);

        // Ficha Resumen (Stock Total, Costo, Precio)
        left.add(buildSummaryCards(), "growx");

        // Pestañas Desglose (Almacén vs Lote)
        tabbedPaneDesglose = new JTabbedPane();
        tabbedPaneDesglose.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPaneDesglose.setBackground(tm.bgSection());
        tabbedPaneDesglose.setForeground(tm.textPrimary());

        // Tab 1: Stock por Almacén
        String[] colsAlma = {"Cód. Almacén", "Nombre Almacén", "Stock Actual"};
        modelStockAlmacen = new DefaultTableModel(new Object[0][3], colsAlma) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tblStockAlmacen = createStyledTable(modelStockAlmacen);
        tblStockAlmacen.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tblStockAlmacen.getSelectedRow();
                if (row >= 0) {
                    String coAlma = (String) modelStockAlmacen.getValueAt(row, 0);
                    seleccionarAlmacenEnCombo(coAlma);
                }
            }
        });
        JScrollPane scrollAlma = new JScrollPane(tblStockAlmacen);
        scrollAlma.getViewport().setBackground(tm.bgPanel());
        scrollAlma.setBorder(BorderFactory.createLineBorder(tm.border(), 1));
        tabbedPaneDesglose.addTab("  Stock por Almacén  ", scrollAlma);

        // Tab 2: Stock por Lote y Vencimiento
        String[] colsLote = {"Almacén", "Lote", "Fecha Vencimiento", "Stock Lote"};
        modelStockLote = new DefaultTableModel(new Object[0][4], colsLote) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tblStockLote = createStyledTable(modelStockLote);
        tblStockLote.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tblStockLote.getSelectedRow();
                if (row >= 0 && productoSeleccionado != null) {
                    int modelRow = tblStockLote.convertRowIndexToModel(row);
                    if (modelRow >= 0 && modelRow < productoSeleccionado.getStockPorLote().size()) {
                        StockLoteRow loteRow = productoSeleccionado.getStockPorLote().get(modelRow);
                        seleccionarAlmacenEnCombo(loteRow.getCoAlma());
                        if (txtLote != null) {
                            txtLote.setText(loteRow.getNumeroLote() != null ? loteRow.getNumeroLote() : "");
                        }
                        if (txtFechaVencimiento != null) {
                            txtFechaVencimiento.setText(loteRow.getFechaExpiracion() != null ? loteRow.getFechaExpiracion() : "");
                        }
                    }
                }
            }
        });
        JScrollPane scrollLote = new JScrollPane(tblStockLote);
        scrollLote.getViewport().setBackground(tm.bgPanel());
        scrollLote.setBorder(BorderFactory.createLineBorder(tm.border(), 1));
        tabbedPaneDesglose.addTab("  Stock por Lote y Vencimiento  ", scrollLote);

        left.add(tabbedPaneDesglose, "grow");
        return left;
    }

    private JPanel buildSummaryCards() {
        JPanel panel = new JPanel(new MigLayout("insets 14, fillx", "[grow 1.5][grow 1][grow 1][grow 1.2]", "[]"));
        panel.setBackground(tm.bgSection());
        panel.putClientProperty(FlatClientProperties.STYLE, "arc: 12");

        // Info Producto
        JPanel pnlProd = new JPanel(new MigLayout("insets 6, wrap 1", "[grow]", "[]2[]2[]"));
        pnlProd.setOpaque(false);
        lblHeaderCodigo = new JLabel("SELECCIONE UN PRODUCTO");
        lblHeaderCodigo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblHeaderCodigo.setForeground(accentCyan());
        lblHeaderDescripcion = new JLabel("Utilice el buscador para cargar existencias");
        lblHeaderDescripcion.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblHeaderDescripcion.setForeground(tm.textPrimary());
        lblHeaderMarca = new JLabel("");
        lblHeaderMarca.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblHeaderMarca.setForeground(tm.textSecondary());
        pnlProd.add(lblHeaderCodigo);
        pnlProd.add(lblHeaderDescripcion);
        pnlProd.add(lblHeaderMarca);

        // Card Stock Total
        JPanel cardStock = createMetricCard("Stock Total", "0", tm.greenAccent());
        lblValStockTotal = (JLabel) cardStock.getComponent(1);

        // Card Costo Actual
        JPanel cardCosto = createMetricCard("Costo Actual ($)", "$ 0.00", tm.textPrimary());
        lblValCosto = (JLabel) cardCosto.getComponent(1);

        // Card Precio Venta
        JPanel cardPrecio = createMetricCard("Precio Venta (S/IVA | C/IVA)", "$ 0.00 | $ 0.00", accentCyan());
        lblValPrecio = (JLabel) cardPrecio.getComponent(1);
        lblValPrecio.setFont(new Font("Segoe UI", Font.BOLD, 13));

        panel.add(pnlProd, "growx");
        panel.add(cardStock, "growx");
        panel.add(cardCosto, "growx");
        panel.add(cardPrecio, "growx");

        return panel;
    }

    private JPanel createMetricCard(String title, String val, Color color) {
        JPanel card = new JPanel(new MigLayout("insets 10, wrap 1", "[grow]", "[]4[]"));
        card.setBackground(tm.bgField());
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 8");

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTitle.setForeground(tm.textSecondary());

        JLabel lblVal = new JLabel(val);
        lblVal.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblVal.setForeground(color);

        card.add(lblTitle);
        card.add(lblVal);
        return card;
    }

    private JPanel buildRightPanel() {
        JPanel panel = new JPanel(new MigLayout("insets 18, fillx, wrap 2", "[130!][grow]", "[]14[]14[]14[]14[]14[]22[]"));
        panel.setBackground(tm.bgSection());
        panel.putClientProperty(FlatClientProperties.STYLE, "arc: 12");

        JLabel title = new JLabel("Procesar Ajuste");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(tm.textPrimary());
        panel.add(title, "span 2, wrap");

        // Tipo de Movimiento
        panel.add(createFormLabel("Tipo de Ajuste:"));
        rbEntrada = new JRadioButton("Entrada por Ajuste (EA)", true);
        rbSalida = new JRadioButton("Salida por Ajuste (SA)", false);
        rbEntrada.setOpaque(false);
        rbSalida.setOpaque(false);
        rbEntrada.setForeground(tm.textPrimary());
        rbSalida.setForeground(tm.textPrimary());
        rbEntrada.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        rbSalida.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        ButtonGroup bgTipo = new ButtonGroup();
        bgTipo.add(rbEntrada);
        bgTipo.add(rbSalida);

        JPanel pnlRadio = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlRadio.setOpaque(false);
        pnlRadio.add(rbEntrada);
        pnlRadio.add(rbSalida);
        panel.add(pnlRadio, "growx");

        // Almacén
        panel.add(createFormLabel("Almacén:"));
        cmbAlmacen = new JComboBox<>();
        cmbAlmacen.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbAlmacen.setBackground(tm.bgField());
        cmbAlmacen.setForeground(tm.textPrimary());
        cmbAlmacen.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof StockAlmacenRow row) {
                    setText(row.getCoAlma() + " - " + row.getDesAlma());
                }
                setBackground(isSelected ? tm.accent() : tm.bgField());
                setForeground(isSelected ? Color.WHITE : tm.textPrimary());
                return this;
            }
        });
        panel.add(cmbAlmacen, "growx, h 34!");

        // Lote (N° de Lote)
        panel.add(createFormLabel("Lote (N° Lote):"));
        txtLote = new JTextField();
        txtLote.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Seleccione en la tabla o escriba el lote...");
        txtLote.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtLote.setBackground(tm.bgField());
        txtLote.setForeground(tm.textPrimary());
        txtLote.setCaretColor(tm.textPrimary());
        panel.add(txtLote, "growx, h 34!");

        // F. Vencimiento
        panel.add(createFormLabel("F. Vencimiento:"));
        txtFechaVencimiento = new JTextField();
        txtFechaVencimiento.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "dd/mm/yyyy (ej: 31/12/2026)");
        txtFechaVencimiento.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtFechaVencimiento.setBackground(tm.bgField());
        txtFechaVencimiento.setForeground(tm.textPrimary());
        txtFechaVencimiento.setCaretColor(tm.textPrimary());
        panel.add(txtFechaVencimiento, "growx, h 34!");

        // Cantidad
        panel.add(createFormLabel("Cantidad:"));
        txtCantidad = new JTextField("1.0");
        txtCantidad.setFont(new Font("Segoe UI", Font.BOLD, 14));
        txtCantidad.setBackground(tm.bgField());
        txtCantidad.setForeground(tm.textPrimary());
        txtCantidad.setCaretColor(tm.textPrimary());
        panel.add(txtCantidad, "growx, h 34!");

        // Costo Unitario
        panel.add(createFormLabel("Costo Unitario:"));
        JPanel pnlCosto = new JPanel(new MigLayout("insets 0, fillx, wrap 1", "[grow]", "[]2[]"));
        pnlCosto.setOpaque(false);
        txtCosto = new JTextField("0.00");
        txtCosto.setFont(new Font("Segoe UI", Font.BOLD, 13));
        txtCosto.setBackground(tm.bgField());
        txtCosto.setForeground(tm.textPrimary());
        txtCosto.setCaretColor(tm.textPrimary());

        lblInfoPrecioVta = new JLabel("Ref. Precio S/IVA: $ 0.00 | C/IVA: $ 0.00");
        lblInfoPrecioVta.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblInfoPrecioVta.setForeground(accentCyan());

        pnlCosto.add(txtCosto, "growx, h 34!");
        pnlCosto.add(lblInfoPrecioVta, "growx");
        panel.add(pnlCosto, "growx");

        // Motivo
        panel.add(createFormLabel("Motivo / Obs:"));
        txtMotivo = new JTextField();
        txtMotivo.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Justificación del ajuste...");
        txtMotivo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtMotivo.setBackground(tm.bgField());
        txtMotivo.setForeground(tm.textPrimary());
        txtMotivo.setCaretColor(tm.textPrimary());
        panel.add(txtMotivo, "growx, h 34!");

        // Botón Procesar
        btnProcesar = new JButton("✔ Procesar Ajuste");
        btnProcesar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnProcesar.setBackground(tm.greenAccent());
        btnProcesar.setForeground(tm.btnForegroundFor(tm.greenAccent()));
        btnProcesar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnProcesar.addActionListener(e -> procesarAjuste());

        panel.add(btnProcesar, "span 2, growx, h 44!");

        return panel;
    }

    private JLabel createFormLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(tm.textSecondary());
        return lbl;
    }

    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setBackground(tm.bgPanel());
        table.setForeground(tm.textPrimary());
        table.setSelectionBackground(tm.accent());
        table.setSelectionForeground(Color.WHITE);
        table.setGridColor(tm.border());

        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(tm.bgField());
        table.getTableHeader().setForeground(tm.textPrimary());
        table.setAutoCreateRowSorter(true);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);

        for (int i = 0; i < table.getColumnCount(); i++) {
            Class<?> cls = table.getColumnClass(i);
            if (Number.class.isAssignableFrom(cls) || table.getColumnName(i).toLowerCase().contains("stock")) {
                table.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
            }
        }
        return table;
    }

    private void cargarAlmacenes() {
        new SwingWorker<List<StockAlmacenRow>, Void>() {
            @Override
            protected List<StockAlmacenRow> doInBackground() throws Exception {
                return dao.obtenerAlmacenes();
            }

            @Override
            protected void done() {
                try {
                    List<StockAlmacenRow> almacenes = get();
                    cmbAlmacen.removeAllItems();
                    for (StockAlmacenRow row : almacenes) {
                        cmbAlmacen.addItem(row);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private void buscarProductos() {
        String query = txtBusqueda.getText().trim();
        new SwingWorker<List<AjusteProductoDTO>, Void>() {
            @Override
            protected List<AjusteProductoDTO> doInBackground() throws Exception {
                return dao.buscarProductos(query);
            }

            @Override
            protected void done() {
                try {
                    List<AjusteProductoDTO> list = get();
                    modelBusqueda.setRowCount(0);
                    for (AjusteProductoDTO dto : list) {
                        modelBusqueda.addRow(new Object[]{
                                dto.getCodigo(),
                                dto.getDescripcion(),
                                dto.getMarca(),
                                dto.getCodigoBarra(),
                                NF_INT.format(dto.getStockTotal()),
                                "$ " + NF.format(dto.getCostoActual()),
                                "$ " + NF.format(dto.getPrecio1()),
                                "$ " + NF.format(dto.getPrecioCiva())
                        });
                    }
                    if (!list.isEmpty()) {
                        tblBusqueda.setRowSelectionInterval(0, 0);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Toast.show("Error al buscar productos", Toast.Type.ERROR);
                }
            }
        }.execute();
    }

    private void cargarProductoSeleccionado(String codigo) {
        new SwingWorker<AjusteProductoDTO, Void>() {
            @Override
            protected AjusteProductoDTO doInBackground() throws Exception {
                return dao.obtenerDetalleProducto(codigo);
            }

            @Override
            protected void done() {
                try {
                    AjusteProductoDTO dto = get();
                    if (dto != null) {
                        productoSeleccionado = dto;
                        lblHeaderCodigo.setText("[" + dto.getCodigo() + "] " + dto.getDescripcion());
                        lblHeaderDescripcion.setText("UDM: " + (dto.getUdm().isBlank() ? "UND" : dto.getUdm()) + " | Cód. Barra: " + (dto.getCodigoBarra().isBlank() ? "N/A" : dto.getCodigoBarra()));
                        lblHeaderMarca.setText("Marca / Proveedor: " + (dto.getMarca().isBlank() ? "N/A" : dto.getMarca()));

                        lblValStockTotal.setText(NF_INT.format(dto.getStockTotal()));
                        lblValCosto.setText("$ " + NF.format(dto.getCostoActual()));
                        lblValPrecio.setText("$ " + NF.format(dto.getPrecio1()) + "  (C/IVA: $ " + NF.format(dto.getPrecioCiva()) + ")");

                        double costoUsar = dto.getCostoActual() > 0 ? dto.getCostoActual() : dto.getPrecio1();
                        txtCosto.setText(String.format("%.2f", costoUsar).replace(",", "."));
                        lblInfoPrecioVta.setText("Ref. Precio S/IVA: $ " + NF.format(dto.getPrecio1()) + " | C/IVA: $ " + NF.format(dto.getPrecioCiva()));

                        // Poblar Tabla Stock por Almacén
                        modelStockAlmacen.setRowCount(0);
                        for (StockAlmacenRow r : dto.getStockPorAlmacen()) {
                            modelStockAlmacen.addRow(new Object[]{
                                    r.getCoAlma(), r.getDesAlma(), NF_INT.format(r.getStock())
                            });
                        }

                        // Poblar Tabla Stock por Lote
                        modelStockLote.setRowCount(0);
                        for (StockLoteRow r : dto.getStockPorLote()) {
                            modelStockLote.addRow(new Object[]{
                                    r.getDesAlma(), r.getNumeroLote(), r.getFechaExpiracion(), NF_INT.format(r.getStockActual())
                            });
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Toast.show("Error al cargar detalle del producto", Toast.Type.ERROR);
                }
            }
        }.execute();
    }

    private void seleccionarAlmacenEnCombo(String coAlma) {
        if (coAlma == null || cmbAlmacen == null) return;
        for (int i = 0; i < cmbAlmacen.getItemCount(); i++) {
            StockAlmacenRow item = cmbAlmacen.getItemAt(i);
            if (item != null && item.getCoAlma().equalsIgnoreCase(coAlma.trim())) {
                cmbAlmacen.setSelectedIndex(i);
                break;
            }
        }
    }

    private boolean tienePermisoParaAjustar() {
        if (!SesionUsuario.isAutenticado()) {
            return false;
        }
        String u = SesionUsuario.current().getCoUsuario();
        return u != null && USUARIOS_AJUSTE_PERMITIDOS.contains(u.trim().toUpperCase());
    }

    private void procesarAjuste() {
        if (!tienePermisoParaAjustar()) {
            Toast.show("No posee permisos para realizar ajustes de inventario.", Toast.Type.WARNING);
            return;
        }

        if (productoSeleccionado == null) {
            Toast.show("Seleccione primero un producto para realizar el ajuste", Toast.Type.WARNING);
            return;
        }

        StockAlmacenRow almaSel = (StockAlmacenRow) cmbAlmacen.getSelectedItem();
        if (almaSel == null) {
            Toast.show("Seleccione un almacén válido", Toast.Type.WARNING);
            return;
        }

        double cant = 0;
        try {
            cant = Double.parseDouble(txtCantidad.getText().replace(",", ".").trim());
            if (cant <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            Toast.show("Ingrese una cantidad válida mayor a 0", Toast.Type.WARNING);
            return;
        }

        double costo = 0;
        try {
            costo = Double.parseDouble(txtCosto.getText().replace(",", ".").trim());
            if (costo < 0) costo = 0;
        } catch (NumberFormatException ignored) {}

        String tipoTrans = rbEntrada.isSelected() ? "EA" : "SA";
        String motivo = txtMotivo.getText().trim();
        String coArt = productoSeleccionado.getCodigo();
        String coAlma = almaSel.getCoAlma();
        String lote = (txtLote != null) ? txtLote.getText().trim() : "";
        String fVencStr = (txtFechaVencimiento != null) ? txtFechaVencimiento.getText().trim() : "";
        java.util.Date fVencDate = parseFecha(fVencStr);

        if (!fVencStr.isEmpty() && fVencDate == null) {
            Toast.show("Formato de fecha inválido. Use dd/mm/yyyy (ej: 31/12/2026)", Toast.Type.WARNING);
            return;
        }

        final double finalCant = cant;
        final double finalCosto = costo;
        final String finalLote = lote;
        final java.util.Date finalFVenc = fVencDate;

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return dao.procesarAjuste(coArt, coAlma, tipoTrans, finalCant, finalCosto, motivo, finalLote, finalFVenc);
            }

            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        String tipoNombre = tipoTrans.equals("EA") ? "Entrada" : "Salida";
                        String msgLote = finalLote.isEmpty() ? "" : " (Lote: " + finalLote + ")";
                        Toast.show("✔ Ajuste de " + tipoNombre + " registrado con éxito (" + finalCant + " un.)" + msgLote, Toast.Type.SUCCESS);
                        if (txtLote != null) txtLote.setText("");
                        if (txtFechaVencimiento != null) txtFechaVencimiento.setText("");
                        cargarProductoSeleccionado(coArt);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Toast.show("✘ Error al procesar el ajuste: " + ex.getMessage(), Toast.Type.ERROR);
                }
            }
        }.execute();
    }

    private java.util.Date parseFecha(String str) {
        if (str == null || str.isBlank()) return null;
        String clean = str.trim();
        String[] formats = {"dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd", "dd/MM/yy"};
        for (String fmt : formats) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(fmt);
                sdf.setLenient(false);
                return sdf.parse(clean);
            } catch (Exception ignored) {}
        }
        return null;
    }
}
