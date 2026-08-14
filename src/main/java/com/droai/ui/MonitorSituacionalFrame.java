package com.droai.ui;

import com.droai.service.MonitorService;
import com.droai.service.MonitorService.MonitorResult;
import com.droai.ui.components.RoundedPanel;
import com.droai.ui.components.Toast;
import com.droai.ui.dialog.DetalleMonitorDialog;
import com.droai.ui.table.MonitorSituacionalTableModel;
import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Monitor Situacional / Reportes de Ventas.
 *
 * <p>Módulo de dashboard con indicadores KPI, filtros de fecha,
 * agrupación por alícuota y tabla de datos detallados.
 *
 * <p>Componentes:
 * <ul>
 *   <li>Panel Superior de Filtros (Fechas + Moneda + Procesar)</li>
 *   <li>Fila de tarjetas KPI (Monto Total, Documentos, Unidades)</li>
 *   <li>Área central dividida: gráficos (placeholder) + tabla de datos</li>
 * </ul>
 */
public class MonitorSituacionalFrame extends JFrame {

    // ── Colores dinámicos vía ThemeManager ──
    private final ThemeManager tm = ThemeManager.get();

    // ── Componentes ──
    private final DatePicker dateDesde;
    private final DatePicker dateHasta;
    private JToggleButton btnBs, btnUsd;
    private final JLabel lblMontoTotal, lblDocumentos, lblUnidades;
    private final MonitorSituacionalTableModel tableModel;
    private final JTable table;
    private final MonitorService monitorService;

    // ── Estado ──
    private boolean monedaBs = true;
    private MonitorResult lastResult;

    public MonitorSituacionalFrame() {
        setTitle("DroAI — Monitor Situacional");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1400, 860);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);

        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/images/logo.png"));
            setIconImage(icon.getImage());
        } catch (Exception ignored) {}

        Toast.setParentFrame(this);
        monitorService = new MonitorService();

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
        tableModel = new MonitorSituacionalTableModel();

        // ── Root con gradiente ──
        JPanel root = new JPanel(new MigLayout(
                "insets 0, fill, wrap",
                "[grow]",
                "[]0[]8[]0[grow]"
        )) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(tm.background());
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setPaint(new GradientPaint(0, 0, tm.gradientTop(),
                        0, 160, tm.gradientBottom()));
                g2.fillRect(0, 0, getWidth(), 160);
                g2.dispose();
            }
        };
        root.setOpaque(false);

        // ═══════════════════════════════════════════════════════════
        //  HEADER
        // ═══════════════════════════════════════════════════════════
        root.add(buildHeader(), "growx");

        // ═══════════════════════════════════════════════════════════
        //  FILTROS
        // ═══════════════════════════════════════════════════════════
        dateDesde = createStyledDatePicker();
        dateHasta = createStyledDatePicker();

        // Defaults: primer día del mes actual → hoy
        dateDesde.setDate(LocalDate.now().withDayOfMonth(1));
        dateHasta.setDate(LocalDate.now());

        root.add(buildFiltros(), "growx, gapx 24 24");

        // ═══════════════════════════════════════════════════════════
        //  KPI CARDS
        // ═══════════════════════════════════════════════════════════
        lblMontoTotal = new JLabel("Bs 0,00");
        lblDocumentos = new JLabel("0");
        lblUnidades = new JLabel("0");

        root.add(buildKpiCards(), "growx, gapx 24 24");

        // ═══════════════════════════════════════════════════════════
        //  AREA CENTRAL (Gráfico + Tabla)
        // ═══════════════════════════════════════════════════════════
        table = createStyledTable();
        root.add(buildCentralArea(), "grow, gapx 24 24, gapbottom 16");

        setContentPane(root);
    }

    // ═══════════════════════════════════════════════════════════════
    //  BUILD: Header
    // ═══════════════════════════════════════════════════════════════

    private JPanel buildHeader() {
        JPanel header = new JPanel(new MigLayout(
                "insets 16 24 8 24, fillx", "[]12[]push[]", "[]"));
        header.setOpaque(false);

        JLabel lblIcon = new JLabel("📈");
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        header.add(lblIcon);

        JPanel titleGroup = new JPanel(new MigLayout("insets 0, wrap, gap 0", "[]", "[]0[]"));
        titleGroup.setOpaque(false);
        JLabel lblTitle = new JLabel("Monitor Situacional");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(tm.textPrimary());
        titleGroup.add(lblTitle);
        JLabel lblSub = new JLabel("Reportes de Ventas — Indicadores y Análisis");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSub.setForeground(tm.textSecondary());
        titleGroup.add(lblSub);
        header.add(titleGroup);

        // Panel derecho: Botón Tema + Botón Volver
        JPanel rightPanel = new JPanel(new MigLayout("insets 0, gap 8", "[][]", "[]"));
        rightPanel.setOpaque(false);

        JButton btnTema = new JButton(tm.isDark() ? "☀" : "🌙");
        btnTema.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        btnTema.setFocusPainted(false);
        btnTema.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnTema.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        btnTema.setToolTipText("Cambiar tema claro/oscuro");
        btnTema.addActionListener(e -> tm.toggleTheme());
        rightPanel.add(btnTema);

        JButton btnVolver = new JButton("← Dashboard");
        btnVolver.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnVolver.setFocusPainted(false);
        btnVolver.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVolver.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        btnVolver.addActionListener(e -> dispose());
        rightPanel.add(btnVolver);

        header.add(rightPanel);

        return header;
    }

    // ═══════════════════════════════════════════════════════════════
    //  BUILD: Panel de Filtros
    // ═══════════════════════════════════════════════════════════════

    private JPanel buildFiltros() {
        RoundedPanel filtros = new RoundedPanel(12, true);
        filtros.setBackground(tm.cardBg());
        filtros.setLayout(new MigLayout(
                "insets 14 20 14 20, gap 12",
                "[]8[]12[]8[]24[]4[]push[]",
                "[]"));

        JLabel lblPeriodo = new JLabel("Periodo a Analizar");
        lblPeriodo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblPeriodo.setForeground(tm.textPrimary());
        filtros.add(lblPeriodo);

        JLabel lblDesde = new JLabel("Desde:");
        lblDesde.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblDesde.setForeground(tm.textSecondary());
        filtros.add(lblDesde);
        filtros.add(dateDesde);

        JLabel lblHasta = new JLabel("Hasta:");
        lblHasta.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblHasta.setForeground(tm.textSecondary());
        filtros.add(lblHasta);
        filtros.add(dateHasta);

        // Toggle moneda
        JPanel monedaPanel = new JPanel(new MigLayout("insets 0, gap 0", "[]0[]", "[]"));
        monedaPanel.setOpaque(false);

        btnBs = createMonedaToggle("Bs", true);
        btnUsd = createMonedaToggle("$", false);
        ButtonGroup bgMoneda = new ButtonGroup();
        bgMoneda.add(btnBs);
        bgMoneda.add(btnUsd);

        btnBs.addActionListener(e -> {
            monedaBs = true;
            actualizarKpis();
        });
        btnUsd.addActionListener(e -> {
            monedaBs = false;
            actualizarKpis();
        });

        monedaPanel.add(btnBs);
        monedaPanel.add(btnUsd);
        filtros.add(monedaPanel);

        // Botón Procesar
        JButton btnProcesar = new JButton("🔄  Procesar");
        btnProcesar.setFont(new Font("Segoe UI Emoji", Font.BOLD, 13));
        btnProcesar.setBackground(tm.accent());
        btnProcesar.setForeground(Color.WHITE);
        btnProcesar.setFocusPainted(false);
        btnProcesar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnProcesar.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
        btnProcesar.addActionListener(e -> procesarDatos());
        filtros.add(btnProcesar);

        return filtros;
    }

    // ═══════════════════════════════════════════════════════════════
    //  BUILD: KPI Cards
    // ═══════════════════════════════════════════════════════════════

    private JPanel buildKpiCards() {
        JPanel kpiRow = new JPanel(new MigLayout(
                "insets 0, gap 16, fillx",
                "[grow][grow][grow][]",
                "[]"));
        kpiRow.setOpaque(false);

        kpiRow.add(createKpiCard("💰", "Monto Total", lblMontoTotal, tm.accent()), "grow");
        kpiRow.add(createKpiCard("📄", "Documentos", lblDocumentos, tm.greenAccent()), "grow");
        kpiRow.add(createKpiCard("📦", "Unidades", lblUnidades, tm.orangeAccent()), "grow");

        // Botón Imprimir
        RoundedPanel printCard = new RoundedPanel(12, true);
        printCard.setBackground(tm.cardBg());
        printCard.setLayout(new MigLayout("insets 16, center, wrap", "[center]", "[]8[]"));

        JLabel lblPrint = new JLabel("🖨️");
        lblPrint.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        lblPrint.setHorizontalAlignment(SwingConstants.CENTER);
        printCard.add(lblPrint, "center");

        JButton btnPrint = new JButton("Imprimir");
        btnPrint.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnPrint.setFocusPainted(false);
        btnPrint.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPrint.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        btnPrint.addActionListener(e ->
                Toast.show("Funcionalidad de impresión próximamente", Toast.Type.INFO));
        printCard.add(btnPrint, "center");

        kpiRow.add(printCard, "w 100!, growy");

        return kpiRow;
    }

    /**
     * Crea una tarjeta KPI individual.
     */
    private RoundedPanel createKpiCard(String icon, String title, JLabel valueLabel,
                                        Color accentColor) {
        RoundedPanel card = new RoundedPanel(12, true);
        card.setBackground(tm.cardBg());
        card.setLayout(new MigLayout("insets 16 20 16 20, gap 8", "[]12[grow]push[]", "[]4[]"));

        // Ícono con fondo circular
        JLabel lblIcon = new JLabel(icon);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        card.add(lblIcon, "span 1 2");

        // Título
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblTitle.setForeground(tm.textSecondary());
        card.add(lblTitle, "wrap");

        // Valor
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(accentColor);
        card.add(valueLabel);

        // Botón Ver Más
        JButton btnVerMas = new JButton("Ver Más");
        btnVerMas.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        btnVerMas.setForeground(tm.textSecondary());
        btnVerMas.setContentAreaFilled(false);
        btnVerMas.setBorderPainted(false);
        btnVerMas.setFocusPainted(false);
        btnVerMas.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVerMas.addActionListener(e -> {
            if ("Monto Total".equals(title) && lastResult != null) {
                DetalleMonitorDialog dialog = new DetalleMonitorDialog(this, lastResult, monedaBs);
                dialog.setVisible(true);
            } else if (lastResult == null) {
                Toast.show("Procese los datos primero para ver el detalle", Toast.Type.WARNING);
            } else {
                Toast.show("Detalle de " + title + " próximamente", Toast.Type.INFO);
            }
        });
        card.add(btnVerMas);

        // Línea inferior de acento
        card.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, accentColor));

        return card;
    }

    // ═══════════════════════════════════════════════════════════════
    //  BUILD: Área Central (Gráfico + Tabla)
    // ═══════════════════════════════════════════════════════════════

    private JPanel buildCentralArea() {
        JPanel central = new JPanel(new MigLayout(
                "insets 0, gap 16, fill",
                "[40%][60%]",
                "[grow]"));
        central.setOpaque(false);

        // ── Panel Izquierdo: Gráficos (placeholder) ──
        RoundedPanel graphPanel = new RoundedPanel(12, true);
        graphPanel.setBackground(tm.cardBg());
        graphPanel.setLayout(new MigLayout("insets 16, fill, center", "[center]", "[]push[center]push[]"));

        JLabel lblGraphTitle = new JLabel("Distribución por Alícuota");
        lblGraphTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblGraphTitle.setForeground(tm.textPrimary());
        graphPanel.add(lblGraphTitle, "wrap");

        JLabel lblGraphPlaceholder = new JLabel("<html><center>" +
                "<span style='font-size:32px'>📊</span><br><br>" +
                "Área de Gráfico<br>" +
                "<span style='color:#94A3B8; font-size:9px'>(Próxima Implementación)</span>" +
                "</center></html>");
        lblGraphPlaceholder.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblGraphPlaceholder.setForeground(tm.textSecondary());
        lblGraphPlaceholder.setHorizontalAlignment(SwingConstants.CENTER);
        graphPanel.add(lblGraphPlaceholder, "center");

        JLabel lblGraphNote = new JLabel("Integración con JFreeChart disponible");
        lblGraphNote.setFont(new Font("Segoe UI", Font.ITALIC, 9));
        lblGraphNote.setForeground(tm.textLabel());
        graphPanel.add(lblGraphNote, "center");

        central.add(graphPanel, "grow");

        // ── Panel Derecho: Tabla de Datos ──
        RoundedPanel tablePanel = new RoundedPanel(12, true);
        tablePanel.setBackground(tm.cardBg());
        tablePanel.setLayout(new MigLayout("insets 12, fill", "[grow]", "[]8[grow]"));

        JLabel lblTableTitle = new JLabel("Agrupación por Tipo de Impuesto");
        lblTableTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTableTitle.setForeground(tm.textPrimary());
        tablePanel.add(lblTableTitle, "wrap");

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(tm.border(), 1));
        scrollPane.getViewport().setBackground(tm.tableBg());
        tablePanel.add(scrollPane, "grow");

        central.add(tablePanel, "grow");

        return central;
    }

    // ═══════════════════════════════════════════════════════════════
    //  Lógica de procesamiento
    // ═══════════════════════════════════════════════════════════════

    /**
     * Procesa los datos de ventas según los filtros seleccionados.
     * Ejecuta la consulta en un hilo separado (SwingWorker) para no bloquear el EDT.
     */
    private void procesarDatos() {
        LocalDate desde = dateDesde.getDate();
        LocalDate hasta = dateHasta.getDate();

        if (desde == null || hasta == null) {
            Toast.show("Seleccione un rango de fechas válido", Toast.Type.WARNING);
            return;
        }
        if (desde.isAfter(hasta)) {
            Toast.show("La fecha 'Desde' no puede ser posterior a 'Hasta'", Toast.Type.WARNING);
            return;
        }

        Toast.show("Procesando datos del período " + desde + " al " + hasta + "...", Toast.Type.INFO);

        new SwingWorker<MonitorResult, Void>() {
            @Override
            protected MonitorResult doInBackground() throws Exception {
                return monitorService.procesar(desde, hasta);
            }

            @Override
            protected void done() {
                try {
                    lastResult = get();
                    actualizarKpis();
                    tableModel.setData(lastResult.agrupaciones());
                    Toast.show("Datos actualizados correctamente — "
                            + lastResult.totalDocumentos() + " documentos procesados", Toast.Type.SUCCESS);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    Toast.show("Error al procesar: " + cause.getMessage(), Toast.Type.ERROR);
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    /**
     * Actualiza las tarjetas KPI con los datos del último resultado.
     */
    private void actualizarKpis() {
        if (lastResult == null) return;

        NumberFormat nf = NumberFormat.getNumberInstance(Locale.of("es", "VE"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);

        double monto = monedaBs ? lastResult.montoTotal() : lastResult.montoTotalUsd();

        String simbolo = monedaBs ? "Bs " : "$ ";
        lblMontoTotal.setText(simbolo + nf.format(monto));
        NumberFormat nfUnits = NumberFormat.getNumberInstance(Locale.of("es", "VE"));
        nfUnits.setMaximumFractionDigits(0);
        lblDocumentos.setText(nfUnits.format(lastResult.totalDocumentos()));
        lblUnidades.setText(nfUnits.format(lastResult.totalUnidades()));

        // Actualizar columna de moneda en la tabla
        tableModel.fireTableStructureChanged();
    }

    // ═══════════════════════════════════════════════════════════════
    //  Helpers UI
    // ═══════════════════════════════════════════════════════════════

    /**
     * Crea un DatePicker estilizado para el tema oscuro.
     */
    private DatePicker createStyledDatePicker() {
        DatePickerSettings settings = new DatePickerSettings(Locale.of("es", "VE"));
        settings.setFormatForDatesCommonEra("dd/MM/yyyy");
        settings.setAllowEmptyDates(false);
        settings.setFirstDayOfWeek(DayOfWeek.MONDAY);

        // Colores para integrarse con el tema oscuro
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

    /**
     * Crea un toggle de moneda estilizado.
     */
    private JToggleButton createMonedaToggle(String text, boolean selected) {
        JToggleButton btn = new JToggleButton(text, selected);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        btn.setPreferredSize(new Dimension(44, 30));
        return btn;
    }

    /**
     * Crea y configura la JTable con estilo moderno.
     */
    private JTable createStyledTable() {
        JTable t = new JTable(tableModel);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.setRowHeight(32);
        t.setGridColor(tm.border());
        t.setBackground(tm.tableBg());
        t.setForeground(tm.textPrimary());
        t.setSelectionBackground(tm.accent().darker());
        t.setSelectionForeground(Color.WHITE);
        t.setShowHorizontalLines(true);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setFillsViewportHeight(true);

        // Header personalizado
        JTableHeader header = t.getTableHeader();
        header.setBackground(tm.tableHeader());
        header.setForeground(tm.textPrimary());
        header.setFont(new Font("Segoe UI", Font.BOLD, 11));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, tm.accent()));
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer() {
            private final NumberFormat nf = NumberFormat.getNumberInstance(Locale.of("es", "VE"));
            {
                nf.setMinimumFractionDigits(2);
                nf.setMaximumFractionDigits(2);
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                if (value instanceof Number) {
                    value = nf.format(((Number) value).doubleValue());
                }
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.RIGHT);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? tm.tableBg() : tm.tableAlt());
                }
                return c;
            }
        };

        // Renderer para texto
        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? tm.tableBg() : tm.tableAlt());
                }
                return c;
            }
        };

        // Aplicar renderers
        t.getColumnModel().getColumn(0).setCellRenderer(leftRenderer);   // Grupo
        t.getColumnModel().getColumn(1).setCellRenderer(rightRenderer);  // Unidades
        t.getColumnModel().getColumn(2).setCellRenderer(leftRenderer);   // Descripción
        t.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);  // Monto
        t.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);  // %

        // Anchos de columna
        t.getColumnModel().getColumn(0).setPreferredWidth(160);
        t.getColumnModel().getColumn(1).setPreferredWidth(100);
        t.getColumnModel().getColumn(2).setPreferredWidth(280);
        t.getColumnModel().getColumn(3).setPreferredWidth(140);
        t.getColumnModel().getColumn(4).setPreferredWidth(90);

        return t;
    }
}
