package com.droai.ui.dialog;

import com.droai.export.ExcelExporter;
import com.droai.model.MatrizVentasRow;
import com.droai.service.MonitorService.MonitorResult;
import com.droai.ui.components.Toast;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

/**
 * Diálogo «Detalle» del Monitor Situacional.
 *
 * <p>Se abre al pulsar «Ver Más» en la tarjeta KPI de Monto Total.
 * Muestra las filas brutas (rawRows) organizadas en 6 pestañas con
 * tabla detallada, footer de totales y panel lateral de resumen.
 *
 * <p>Pestañas:
 * <ul>
 *   <li>Facturas — agrupación por documento</li>
 *   <li>Productos Facturados — detalle por renglón (pestaña principal)</li>
 *   <li>Devoluciones — placeholder</li>
 *   <li>Productos Devueltos — placeholder</li>
 *   <li>Remisiones — placeholder</li>
 *   <li>Productos Remitidos — placeholder</li>
 * </ul>
 */
public class DetalleMonitorDialog extends JDialog {

    // ── Paleta de colores DroAI ──
    private static final Color BG_DARK        = new Color(17, 21, 28);
    private static final Color BG_PANEL       = new Color(24, 28, 38);
    private static final Color BG_SECTION     = new Color(30, 35, 46);
    private static final Color BORDER         = new Color(55, 62, 80);
    private static final Color ACCENT         = new Color(42, 107, 255);
    private static final Color GREEN_ACCENT   = new Color(0, 210, 158);
    private static final Color TEXT_PRIMARY   = new Color(248, 250, 252);
    private static final Color TEXT_SECONDARY = new Color(148, 163, 184);
    private static final Color TABLE_BG       = new Color(24, 28, 38);
    private static final Color TABLE_ALT      = new Color(28, 33, 44);
    private static final Color TABLE_HEADER   = new Color(34, 40, 54);
    private static final Color BTN_GREEN_BG   = new Color(0, 180, 130);

    private final List<MatrizVentasRow> rawRows;
    private final boolean isBs;

    private JTabbedPane mainTabs;
    private final List<MatrizVentasRow> currentUnidadesValoresRows = new ArrayList<>();
    private JLabel lblUnidadesValoresSummary;

    // ── Labels del footer (se actualizan al cambiar pestaña) ──
    private JLabel lblRegistros;
    private JLabel lblTotalMonto;
    private JLabel lblTotalCosto;

    // ── Número formateador ──
    private static final NumberFormat NF;
    static {
        NF = NumberFormat.getNumberInstance(Locale.of("es", "VE"));
        NF.setMinimumFractionDigits(2);
        NF.setMaximumFractionDigits(2);
    }

    private static final NumberFormat NF_INT;
    static {
        NF_INT = NumberFormat.getNumberInstance(Locale.of("es", "VE"));
        NF_INT.setMaximumFractionDigits(0);
    }

    public DetalleMonitorDialog(Frame owner, MonitorResult result) {
        this(owner, result, true);
    }

    public DetalleMonitorDialog(Frame owner, MonitorResult result, boolean isBs) {
        super(owner, "Detalle", true);
        this.rawRows = result.rawRows();
        this.isBs = isBs;

        setSize(1200, 750);
        setMinimumSize(new Dimension(900, 550));
        setLocationRelativeTo(owner);

        JPanel root = new JPanel(new MigLayout(
                "insets 0, fill, wrap",
                "[grow]",
                "[]0[grow]0[]"
        )) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, BG_DARK, 0, getHeight(), BG_PANEL);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        root.setOpaque(false);

        root.add(buildTitleBar(), "growx, h 48!");
        root.add(buildCentralContent(), "grow");
        root.add(buildFooter(), "growx, h 52!");

        setContentPane(root);
    }

    // ═══════════════════════════════════════════════════════════════
    //  TITLE BAR
    // ═══════════════════════════════════════════════════════════════

    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new MigLayout("insets 10 20 10 20, fillx", "[]push[]", "[]"));
        bar.setBackground(ACCENT);

        JLabel icon = new JLabel("📋");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        bar.add(icon, "split 2, gapright 8");

        JLabel title = new JLabel("Detalle");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Color.WHITE);
        bar.add(title);

        JButton btnClose = new JButton("✕");
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnClose.setForeground(Color.WHITE);
        btnClose.setContentAreaFilled(false);
        btnClose.setBorderPainted(false);
        btnClose.setFocusPainted(false);
        btnClose.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> dispose());
        btnClose.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnClose.setForeground(new Color(255, 100, 100));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btnClose.setForeground(Color.WHITE);
            }
        });
        bar.add(btnClose);

        return bar;
    }

    // ═══════════════════════════════════════════════════════════════
    //  CENTRAL CONTENT: Tabs + Sidebar
    // ═══════════════════════════════════════════════════════════════

    private JPanel buildCentralContent() {
        JPanel central = new JPanel(new MigLayout(
                "insets 0, fill, gap 0",
                "[grow]0[140!]",
                "[grow]"
        ));
        central.setOpaque(false);

        // ── Left: Tabbed Pane ──
        JTabbedPane tabs = buildTabbedPane();
        central.add(tabs, "grow");

        // ── Right: Sidebar ──
        central.add(buildSidebar(), "grow");

        return central;
    }

    // ═══════════════════════════════════════════════════════════════
    //  TABBED PANE
    // ═══════════════════════════════════════════════════════════════

    private JTabbedPane buildTabbedPane() {
        mainTabs = new JTabbedPane(JTabbedPane.TOP);
        mainTabs.setFont(new Font("Segoe UI", Font.BOLD, 12));
        mainTabs.setOpaque(false);
        mainTabs.setBackground(BG_SECTION);
        mainTabs.setForeground(TEXT_PRIMARY);

        // Tab 1: Facturas
        mainTabs.addTab("Facturas", buildFacturasTab());

        // Tab 2: Productos Facturados (main)
        mainTabs.addTab("Productos Facturados", buildProductosFacturadosTab());

        // Tab 3: Unidades/Valores
        mainTabs.addTab("Unidades/Valores", buildUnidadesValoresTab());

        // Tab 4–7: Placeholders
        mainTabs.addTab("Devoluciones", buildPlaceholderTab("Devoluciones"));
        mainTabs.addTab("Productos Devueltos", buildPlaceholderTab("Productos Devueltos"));
        mainTabs.addTab("Remisiones", buildPlaceholderTab("Remisiones"));
        mainTabs.addTab("Productos Remitidos", buildPlaceholderTab("Productos Remitidos"));

        // Default: Productos Facturados
        mainTabs.setSelectedIndex(1);

        // Update footer counts on tab change
        mainTabs.addChangeListener(e -> updateFooterForTab(mainTabs.getSelectedIndex()));

        return mainTabs;
    }

    // ── Tab: Facturas ──

    private JScrollPane buildFacturasTab() {
        // Agrupa rawRows por número de factura
        Map<String, double[]> facturaMap = new LinkedHashMap<>();
        // value: [monto, costo, renglones]

        for (MatrizVentasRow row : rawRows) {
            String num = row.getNumero() != null ? row.getNumero().trim() : "—";
            double divFactor = isBs ? 1.0 : (row.getTasa() > 0 ? row.getTasa() : 1.0);
            facturaMap.computeIfAbsent(num, k -> new double[3]);
            double[] acum = facturaMap.get(num);
            acum[0] += row.getRenglonDg() / divFactor;   // monto neto
            acum[1] += (row.getCostoVenta() * row.getCantidad()) / divFactor; // costo
            acum[2] += 1;                                                    // renglones
        }

        String[] cols = {"Numero", "Renglones", "Monto Total", "Costo Total"};
        Object[][] data = new Object[facturaMap.size()][4];
        int i = 0;
        for (Map.Entry<String, double[]> entry : facturaMap.entrySet()) {
            data[i][0] = entry.getKey();
            data[i][1] = (int) entry.getValue()[2];
            data[i][2] = entry.getValue()[0];
            data[i][3] = entry.getValue()[1];
            i++;
        }

        DefaultTableModel model = new DefaultTableModel(data, cols) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
            @Override
            public Class<?> getColumnClass(int col) {
                return switch (col) {
                    case 1 -> Integer.class;
                    case 2, 3 -> Double.class;
                    default -> String.class;
                };
            }
        };

        JTable table = createStyledTable(model);
        table.getColumnModel().getColumn(0).setPreferredWidth(140);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(160);
        table.getColumnModel().getColumn(3).setPreferredWidth(160);

        return wrapTableInScroll(table);
    }

    // ── Tab: Productos Facturados ──

    private JScrollPane buildProductosFacturadosTab() {
        String[] cols = {
                "Numero", "Reng", "Codigo", "Descripcion", "Cantidad",
                "Precio", "Desc.%", "Total Reng.", "Costo", "Grupo", "Des.Grupo", "SubGr."
        };

        Object[][] data = new Object[rawRows.size()][12];
        for (int i = 0; i < rawRows.size(); i++) {
            MatrizVentasRow r = rawRows.get(i);
            double divFactor = isBs ? 1.0 : (r.getTasa() > 0 ? r.getTasa() : 1.0);
            data[i][0]  = r.getNumero() != null ? r.getNumero().trim() : "";
            data[i][1]  = i + 1;  // Renglón secuencial visible
            data[i][2]  = r.getCodigoArt() != null ? r.getCodigoArt().trim() : "";
            data[i][3]  = r.getDescripcion() != null ? r.getDescripcion().trim() : "";
            data[i][4]  = r.getCantidad();
            data[i][5]  = r.getPrecio() / divFactor;
            data[i][6]  = r.getDescPct();
            data[i][7]  = r.getRenglonDg() / divFactor;
            data[i][8]  = r.getCostoVenta() / divFactor;
            data[i][9]  = r.getCodLinea() != null ? r.getCodLinea().trim() : "";
            data[i][10] = r.getLinea() != null ? r.getLinea().trim() : "";
            data[i][11] = r.getCodSub() != null ? r.getCodSub().trim() : "";
        }

        DefaultTableModel model = new DefaultTableModel(data, cols) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
            @Override
            public Class<?> getColumnClass(int col) {
                return switch (col) {
                    case 1 -> Integer.class;
                    case 4, 5, 6, 7, 8 -> Double.class;
                    default -> String.class;
                };
            }
        };

        JTable table = createStyledTable(model);
        // Ajustar anchos de columna
        int[] widths = {100, 40, 60, 260, 55, 70, 55, 80, 65, 55, 130, 55};
        for (int c = 0; c < widths.length && c < table.getColumnCount(); c++) {
            table.getColumnModel().getColumn(c).setPreferredWidth(widths[c]);
        }

        return wrapTableInScroll(table);
    }

    // ── Tab: Unidades/Valores por Proveedor ──

    private JPanel buildUnidadesValoresTab() {
        JPanel panel = new JPanel(new MigLayout("insets 8, fill, wrap", "[grow]", "[]8[grow]"));
        panel.setBackground(BG_PANEL);

        // Panel de filtro superior
        JPanel filterPanel = new JPanel(new MigLayout("insets 4 8 4 8, fillx", "[]8[280!]16[]16[grow, right]", "[]"));
        filterPanel.setBackground(BG_SECTION);
        filterPanel.setBorder(BorderFactory.createLineBorder(BORDER, 1));

        JLabel lblProv = new JLabel("🏢 Proveedor:");
        lblProv.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblProv.setForeground(TEXT_PRIMARY);
        filterPanel.add(lblProv);

        // Obtener proveedores únicos de rawRows
        Map<String, String> provMap = new TreeMap<>(); // cod -> desc
        for (MatrizVentasRow r : rawRows) {
            String cod = r.getCodProveedor() != null ? r.getCodProveedor().trim() : "";
            String nom = r.getNombreProveedor() != null ? r.getNombreProveedor().trim() : "";
            if (!cod.isEmpty() || !nom.isEmpty()) {
                provMap.putIfAbsent(cod.isEmpty() ? nom : cod, nom.isEmpty() ? cod : nom);
            }
        }

        JComboBox<String> cmbProv = new JComboBox<>();
        cmbProv.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        cmbProv.addItem("— Todos los Proveedores (" + provMap.size() + ") —");
        for (Map.Entry<String, String> entry : provMap.entrySet()) {
            cmbProv.addItem("[" + entry.getKey() + "] " + entry.getValue());
        }
        filterPanel.add(cmbProv, "w 280!");

        JTextField txtSearch = new JTextField();
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        filterPanel.add(txtSearch, "w 220!");

        lblUnidadesValoresSummary = new JLabel("");
        lblUnidadesValoresSummary.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblUnidadesValoresSummary.setForeground(GREEN_ACCENT);
        filterPanel.add(lblUnidadesValoresSummary, "right");

        panel.add(filterPanel, "growx");

        // Columnas de la tabla de Unidades/Valores (14 columnas solicitadas)
        String[] cols = {
                "Numero", "Mes", "Fecha", "Proveedor", "Rif", "Razon Social",
                "Nombre Vendedor", "Zona", "Ciudad", "Cod Prov", "Cod Art",
                "Descripcion Art", "Cantidad", "Total Renglon"
        };

        DefaultTableModel model = new DefaultTableModel(new Object[0][14], cols) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
            @Override
            public Class<?> getColumnClass(int col) {
                return switch (col) {
                    case 12, 13 -> Double.class;
                    default -> String.class;
                };
            }
        };

        JTable table = createStyledTable(model);
        int[] widths = {85, 95, 80, 160, 95, 180, 140, 90, 90, 85, 75, 220, 65, 95};
        for (int c = 0; c < widths.length && c < table.getColumnCount(); c++) {
            table.getColumnModel().getColumn(c).setPreferredWidth(widths[c]);
        }

        JScrollPane scroll = wrapTableInScroll(table);
        panel.add(scroll, "grow");

        // Listener de filtrado
        Runnable applyFilter = () -> {
            String selProv = (String) cmbProv.getSelectedItem();
            String query = txtSearch.getText().trim().toLowerCase();

            String targetCod = null;
            if (selProv != null && selProv.startsWith("[")) {
                int endIdx = selProv.indexOf("]");
                if (endIdx > 1) {
                    targetCod = selProv.substring(1, endIdx).trim();
                }
            }

            currentUnidadesValoresRows.clear();
            double sumCant = 0;
            double sumVal = 0;
            double sumCosto = 0;

            for (MatrizVentasRow r : rawRows) {
                String rCodProv = r.getCodProveedor() != null ? r.getCodProveedor().trim() : "";
                String rNomProv = r.getNombreProveedor() != null ? r.getNombreProveedor().trim() : "";

                if (targetCod != null) {
                    boolean matchProv = rCodProv.equalsIgnoreCase(targetCod) || rNomProv.equalsIgnoreCase(targetCod);
                    if (!matchProv) continue;
                }

                if (!query.isEmpty()) {
                    String num = r.getNumero() != null ? r.getNumero().toLowerCase() : "";
                    String art = r.getCodigoArt() != null ? r.getCodigoArt().toLowerCase() : "";
                    String desc = r.getDescripcion() != null ? r.getDescripcion().toLowerCase() : "";
                    String prov = rNomProv.toLowerCase();
                    String client = r.getNombreRazonSocial() != null ? r.getNombreRazonSocial().toLowerCase() : "";
                    if (!num.contains(query) && !art.contains(query) && !desc.contains(query)
                            && !prov.contains(query) && !client.contains(query)) {
                        continue;
                    }
                }

                currentUnidadesValoresRows.add(r);
                double divFactor = isBs ? 1.0 : (r.getTasa() > 0 ? r.getTasa() : 1.0);
                sumCant += r.getCantidad();
                sumVal += r.getRenglonDg() / divFactor;
                sumCosto += (r.getCostoVenta() * r.getCantidad()) / divFactor;
            }

            // Ordenar los registros por Fecha ascendente y Numero ascendente
            currentUnidadesValoresRows.sort((a, b) -> {
                int c = Objects.toString(a.getFecha(), "").compareTo(Objects.toString(b.getFecha(), ""));
                if (c != 0) return c;
                return Objects.toString(a.getNumero(), "").compareTo(Objects.toString(b.getNumero(), ""));
            });

            // Actualizar modelo de tabla
            model.setRowCount(0);
            for (MatrizVentasRow r : currentUnidadesValoresRows) {
                double divFactor = isBs ? 1.0 : (r.getTasa() > 0 ? r.getTasa() : 1.0);
                model.addRow(new Object[]{
                        r.getNumero() != null ? r.getNumero().trim() : "",
                        r.getMes(),
                        r.getFecha() != null ? r.getFecha().trim() : "",
                        r.getNombreProveedor() != null ? r.getNombreProveedor().trim() : "",
                        r.getCiRif() != null ? r.getCiRif().trim() : "",
                        r.getNombreRazonSocial() != null ? r.getNombreRazonSocial().trim() : "",
                        r.getNombreVendedor() != null ? r.getNombreVendedor().trim() : "",
                        r.getZona() != null ? r.getZona().trim() : "",
                        r.getCiudad() != null ? r.getCiudad().trim() : "",
                        r.getCodProv() != null ? r.getCodProv().trim() : "",
                        r.getCodigoArt() != null ? r.getCodigoArt().trim() : "",
                        r.getDescripcion() != null ? r.getDescripcion().trim() : "",
                        r.getCantidad(),
                        r.getRenglonDg() / divFactor
                });
            }

            String prefix = isBs ? "Bs " : "$ ";
            lblUnidadesValoresSummary.setText(String.format("Unidades: %s  |  Valores: %s%s",
                    NF_INT.format(sumCant), prefix, NF.format(sumVal)));

            if (mainTabs != null && mainTabs.getSelectedIndex() == 2) {
                updateFooterForTab(2);
            }
        };

        cmbProv.addActionListener(e -> applyFilter.run());
        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilter.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilter.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter.run(); }
        });

        // Ejecución inicial
        applyFilter.run();

        return panel;
    }

    // ── Tab: Placeholder ──

    private JPanel buildPlaceholderTab(String name) {
        JPanel panel = new JPanel(new MigLayout("fill, center", "[center]", "[center]"));
        panel.setBackground(BG_PANEL);

        JLabel lbl = new JLabel("<html><center>"
                + "<span style='font-size:28px'>📋</span><br><br>"
                + "<span style='color:#F8FAFC; font-size:13px'>" + name + "</span><br>"
                + "<span style='color:#94A3B8; font-size:10px'>(Próxima Implementación)</span>"
                + "</center></html>");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(TEXT_SECONDARY);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lbl, "center");

        return panel;
    }

    // ═══════════════════════════════════════════════════════════════
    //  SIDEBAR (right panel)
    // ═══════════════════════════════════════════════════════════════

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new MigLayout(
                "insets 8, fill, wrap, gapy 4",
                "[grow]",
                "[]8[grow]"
        ));
        sidebar.setBackground(new Color(20, 24, 32));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER));

        // ── Botón Imprimir ──
        JButton btnPrint = createSidebarButton("🖨️", "Imprimir");
        btnPrint.addActionListener(e ->
                Toast.show("Funcionalidad de impresión próximamente", Toast.Type.INFO));
        sidebar.add(btnPrint, "growx");

        // ── Resumen por grupo ──
        JPanel resumenPanel = new JPanel(new MigLayout("insets 4, wrap, gap 0 2", "[grow]", ""));
        resumenPanel.setOpaque(false);

        // Agrupar por línea y obtener monto por grupo
        Map<String, Double> grupoMontos = new LinkedHashMap<>();
        for (MatrizVentasRow row : rawRows) {
            String grupo = row.getLinea() != null ? row.getLinea().trim() : "Sin Grupo";
            if (grupo.isEmpty()) grupo = "Sin Grupo";
            double divFactor = isBs ? 1.0 : (row.getTasa() > 0 ? row.getTasa() : 1.0);
            double val = row.getRenglonDg() / divFactor;
            grupoMontos.merge(grupo, val, Double::sum);
        }

        // Ordenar por monto descendente
        List<Map.Entry<String, Double>> sorted = new ArrayList<>(grupoMontos.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // Mostrar valores (limitados al espacio disponible)
        JScrollPane resumenScroll = new JScrollPane(resumenPanel);
        resumenScroll.setBorder(BorderFactory.createEmptyBorder());
        resumenScroll.setOpaque(false);
        resumenScroll.getViewport().setOpaque(false);
        resumenScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        resumenScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        for (Map.Entry<String, Double> entry : sorted) {
            JLabel lblVal = new JLabel(NF.format(entry.getValue()));
            lblVal.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            lblVal.setForeground(TEXT_PRIMARY);
            lblVal.setHorizontalAlignment(SwingConstants.RIGHT);
            lblVal.setToolTipText(entry.getKey() + ": " + (isBs ? "Bs " : "$ ") + NF.format(entry.getValue()));
            resumenPanel.add(lblVal, "growx, h 18!");
        }

        sidebar.add(resumenScroll, "grow");

        return sidebar;
    }

    private JButton createSidebarButton(String icon, String text) {
        JButton btn = new JButton() {
            private Color currentBg = BG_SECTION;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) {
                        currentBg = ACCENT;
                        repaint();
                    }
                    @Override public void mouseExited(MouseEvent e) {
                        currentBg = BG_SECTION;
                        repaint();
                    }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(currentBg);
                g2.fill(new RoundRectangle2D.Float(2, 2, getWidth() - 4, getHeight() - 4, 8, 8));

                // Icon
                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
                FontMetrics fmIcon = g2.getFontMetrics();
                int iconX = (getWidth() - fmIcon.stringWidth(icon)) / 2;
                g2.setColor(Color.WHITE);
                g2.drawString(icon, iconX, 30);

                // Text
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                FontMetrics fmText = g2.getFontMetrics();
                int textX = (getWidth() - fmText.stringWidth(text)) / 2;
                g2.drawString(text, textX, 48);

                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(120, 56));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ═══════════════════════════════════════════════════════════════
    //  FOOTER
    // ═══════════════════════════════════════════════════════════════

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new MigLayout(
                "insets 8 20 8 20, fillx",
                "[]24[]push[]16[]",
                "[]"
        ));
        footer.setBackground(new Color(20, 24, 32));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));

        // Conteo de registros
        lblRegistros = new JLabel(NF_INT.format(rawRows.size()) + " Registros");
        lblRegistros.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblRegistros.setForeground(TEXT_PRIMARY);
        lblRegistros.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(GREEN_ACCENT, 1, true),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)
        ));
        footer.add(lblRegistros);

        // Botón Exportar Excel
        JButton btnExport = new JButton() {
            private Color currentBg = BTN_GREEN_BG;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) {
                        currentBg = BTN_GREEN_BG.brighter();
                        repaint();
                    }
                    @Override public void mouseExited(MouseEvent e) {
                        currentBg = BTN_GREEN_BG;
                        repaint();
                    }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(currentBg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI Emoji", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                String txt = "📥 Exportar Excel";
                int x = (getWidth() - fm.stringWidth(txt)) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(txt, x, y);
                g2.dispose();
            }
        };
        btnExport.setPreferredSize(new Dimension(160, 32));
        btnExport.setContentAreaFilled(false);
        btnExport.setBorderPainted(false);
        btnExport.setFocusPainted(false);
        btnExport.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnExport.addActionListener(e -> exportarExcel());
        footer.add(btnExport);

        // Total Monto
        double totalMonto = 0;
        double totalCosto = 0;
        for (MatrizVentasRow row : rawRows) {
            double divFactor = isBs ? 1.0 : (row.getTasa() > 0 ? row.getTasa() : 1.0);
            totalMonto += row.getRenglonDg() / divFactor;
            totalCosto += (row.getCostoVenta() * row.getCantidad()) / divFactor;
        }

        String prefix = isBs ? "Bs " : "$ ";
        lblTotalMonto = new JLabel("Total Monto: " + prefix + NF.format(totalMonto));
        lblTotalMonto.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTotalMonto.setForeground(TEXT_PRIMARY);
        footer.add(lblTotalMonto);

        lblTotalCosto = new JLabel("Total Costo: " + prefix + NF.format(totalCosto));
        lblTotalCosto.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTotalCosto.setForeground(TEXT_SECONDARY);
        footer.add(lblTotalCosto);

        return footer;
    }

    // ═══════════════════════════════════════════════════════════════
    //  EXPORT
    // ═══════════════════════════════════════════════════════════════

    private void exportarExcel() {
        try {
            ExcelExporter exporter = new ExcelExporter();
            File file;
            if (mainTabs != null && mainTabs.getSelectedIndex() == 2) {
                file = exporter.exportUnidadesValores(currentUnidadesValoresRows, isBs);
            } else {
                file = exporter.exportMatrizVentas(rawRows, isBs);
            }
            Toast.show("Exportado: " + file.getName(), Toast.Type.SUCCESS);
        } catch (Exception ex) {
            Toast.show("Error al exportar: " + ex.getMessage(), Toast.Type.ERROR);
            ex.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════

    private void updateFooterForTab(int tabIndex) {
        String prefix = isBs ? "Bs " : "$ ";
        if (tabIndex == 0) {
            // Facturas tab — conteo de facturas únicas
            Set<String> facturas = new HashSet<>();
            double totalMonto = 0;
            double totalCosto = 0;
            for (MatrizVentasRow row : rawRows) {
                double divFactor = isBs ? 1.0 : (row.getTasa() > 0 ? row.getTasa() : 1.0);
                if (row.getNumero() != null) facturas.add(row.getNumero().trim());
                totalMonto += row.getRenglonDg() / divFactor;
                totalCosto += (row.getCostoVenta() * row.getCantidad()) / divFactor;
            }
            lblRegistros.setText(NF_INT.format(facturas.size()) + " Registros");
            lblTotalMonto.setText("Total Monto: " + prefix + NF.format(totalMonto));
            lblTotalCosto.setText("Total Costo: " + prefix + NF.format(totalCosto));
        } else if (tabIndex == 1) {
            // Productos Facturados
            double totalMonto = 0;
            double totalCosto = 0;
            for (MatrizVentasRow row : rawRows) {
                double divFactor = isBs ? 1.0 : (row.getTasa() > 0 ? row.getTasa() : 1.0);
                totalMonto += row.getRenglonDg() / divFactor;
                totalCosto += (row.getCostoVenta() * row.getCantidad()) / divFactor;
            }
            lblRegistros.setText(NF_INT.format(rawRows.size()) + " Registros");
            lblTotalMonto.setText("Total Monto: " + prefix + NF.format(totalMonto));
            lblTotalCosto.setText("Total Costo: " + prefix + NF.format(totalCosto));
        } else if (tabIndex == 2) {
            // Unidades/Valores tab
            double totalMonto = 0;
            double totalCosto = 0;
            for (MatrizVentasRow row : currentUnidadesValoresRows) {
                double divFactor = isBs ? 1.0 : (row.getTasa() > 0 ? row.getTasa() : 1.0);
                totalMonto += row.getRenglonDg() / divFactor;
                totalCosto += (row.getCostoVenta() * row.getCantidad()) / divFactor;
            }
            lblRegistros.setText(NF_INT.format(currentUnidadesValoresRows.size()) + " Registros");
            lblTotalMonto.setText("Total Monto: " + prefix + NF.format(totalMonto));
            lblTotalCosto.setText("Total Costo: " + prefix + NF.format(totalCosto));
        } else {
            lblRegistros.setText("0 Registros");
            lblTotalMonto.setText("Total Monto: " + prefix + "0,00");
            lblTotalCosto.setText("Total Costo: " + prefix + "0,00");
        }
    }

    /**
     * Crea una JTable estilizada para el tema oscuro del diálogo.
     */
    private JTable createStyledTable(TableModel model) {
        JTable t = new JTable(model);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        t.setRowHeight(28);
        t.setGridColor(BORDER);
        t.setBackground(TABLE_BG);
        t.setForeground(TEXT_PRIMARY);
        t.setSelectionBackground(ACCENT.darker());
        t.setSelectionForeground(Color.WHITE);
        t.setShowHorizontalLines(true);
        t.setShowVerticalLines(true);
        t.setIntercellSpacing(new Dimension(1, 1));
        t.setFillsViewportHeight(true);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        t.setAutoCreateRowSorter(true);

        // Header
        JTableHeader header = t.getTableHeader();
        header.setBackground(TABLE_HEADER);
        header.setForeground(TEXT_PRIMARY);
        header.setFont(new Font("Segoe UI", Font.BOLD, 11));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, ACCENT));
        header.setReorderingAllowed(false);

        // Renderer numérico alineado a la derecha
        DefaultTableCellRenderer numRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                if (value instanceof Number) {
                    value = NF.format(((Number) value).doubleValue());
                }
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.RIGHT);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? TABLE_BG : TABLE_ALT);
                    c.setForeground(TEXT_PRIMARY);
                }
                return c;
            }
        };

        // Renderer texto
        DefaultTableCellRenderer textRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? TABLE_BG : TABLE_ALT);
                    c.setForeground(TEXT_PRIMARY);
                }
                return c;
            }
        };

        // Aplicar renderers a todas las columnas
        for (int col = 0; col < t.getColumnCount(); col++) {
            Class<?> cls = t.getColumnClass(col);
            if (cls == Double.class || cls == Integer.class) {
                t.getColumnModel().getColumn(col).setCellRenderer(numRenderer);
            } else {
                t.getColumnModel().getColumn(col).setCellRenderer(textRenderer);
            }
        }

        return t;
    }

    private JScrollPane wrapTableInScroll(JTable table) {
        JScrollPane sp = new JScrollPane(table,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        sp.getViewport().setBackground(TABLE_BG);
        return sp;
    }
}
