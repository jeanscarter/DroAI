package com.droai.ui;

import com.droai.dao.ImportacionDAO.ValidationResult;
import com.droai.model.ArticuloImportRow;
import com.droai.model.ImportConfig;
import com.droai.model.SesionUsuario;
import com.droai.service.ImportadorService;
import com.droai.service.ImportadorService.PreviewResult;
import com.droai.ui.components.Toast;
import com.droai.ui.dialog.LoginAuditoriaDialog;
import com.formdev.flatlaf.FlatClientProperties;
import com.droai.model.ProductoReporteRow;
import com.droai.ui.table.ReporteTableModel;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel "Importar Datos": selección de Excel, previsualización,
 * validación previa y ejecución de importación UPSERT con auditoría.
 *
 * <p>Contiene dos pestañas:
 * 1. Importar Excel (lógica original).
 * 2. Reporte de Productos (visor de base de datos con búsqueda en tiempo real).
 */
public class ImportarPanel extends JPanel {

    // ── Paleta ──
    private static final Color ACCENT        = new Color(42, 107, 255);
    private static final Color SUCCESS_GREEN = new Color(46, 125, 50);
    private static final Color WARN_AMBER    = new Color(220, 160, 50);
    private static final Color BG_CARD       = new Color(30, 35, 46);
    private static final Color BORDER_COLOR  = new Color(55, 62, 80);
    private static final Color TEXT_SECONDARY = new Color(148, 163, 184);

    private final ImportadorService service;

    // ── UI Components Contenedor Principal ──
    private JTabbedPane innerTabs;

    // ── UI Components Importación ──
    private JLabel lblArchivoSeleccionado;
    private JLabel lblConfigInfo;
    private JLabel lblSesion;
    private JLabel lblValidacion;
    private JProgressBar progressBar;
    private JButton btnSeleccionar;
    private JButton btnLogin;
    private JButton btnValidar;
    private JButton btnImportar;
    private JTable tblPreview;
    private PreviewTableModel previewModel;

    // ── UI Components Reporte de Productos ──
    private JTable tblReporte;
    private ReporteTableModel reporteModel;
    private JTextField txtBusquedaReporte;
    private JLabel lblReporteStatus;
    private JButton btnCargarReporte;

    // ── Estado ──
    private File archivoSeleccionado;
    private ImportConfig configActual;
    private boolean validacionPasada = false;

    public ImportarPanel() {
        service = new ImportadorService();
        setLayout(new BorderLayout());
        setOpaque(false);

        // Inicializar componentes de Importación
        lblArchivoSeleccionado = new JLabel("Ningún archivo seleccionado");
        lblConfigInfo = new JLabel(" ");
        lblSesion = new JLabel();
        lblValidacion = new JLabel("Esperando archivo y autenticación...");
        progressBar = new JProgressBar(0, 100);
        
        btnSeleccionar = createStyledButton("📁  Seleccionar Archivo Excel", ACCENT);
        btnLogin = createStyledButton("🔐  Autenticar", ACCENT);
        btnValidar = createStyledButton("🔍  Validar Datos", WARN_AMBER);
        btnImportar = createStyledButton("⚡  Procesar e Importar a BD", SUCCESS_GREEN);
        
        previewModel = new PreviewTableModel();
        tblPreview = new JTable(previewModel);

        // Inicializar componentes de Reporte de Productos
        reporteModel = new ReporteTableModel();
        tblReporte = new JTable(reporteModel);
        txtBusquedaReporte = new JTextField();
        lblReporteStatus = new JLabel("0 de 0 productos");
        btnCargarReporte = createStyledButton("🔄 Cargar Reporte desde BD", ACCENT);

        // Construir JTabbedPane
        innerTabs = new JTabbedPane();
        innerTabs.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_TYPE, "underlined");
        innerTabs.setFont(new Font("Segoe UI", Font.BOLD, 13));

        innerTabs.addTab("📥 Importar Excel", buildImportPanel());
        innerTabs.addTab("📋 Reporte de Productos", buildReportePanel());

        add(innerTabs, BorderLayout.CENTER);

        // Lógica inicial
        actualizarSesionLabel();
        actualizarEstadoBotones();
    }

    // ═══════════════════════════════════════════════════════════════
    //  Construcción de Pestañas
    // ═══════════════════════════════════════════════════════════════

    private JPanel buildImportPanel() {
        JPanel panel = new JPanel(new MigLayout(
            "insets 20 24 20 24, fill, wrap, gapy 10",
            "[grow]",
            "[][][][][][grow][]"
        ));
        panel.setOpaque(false);

        // Título + Sesión
        JPanel titleRow = new JPanel(new MigLayout("insets 0, fillx", "[]push[]", "[]"));
        titleRow.setOpaque(false);

        JLabel lblTitle = new JLabel("📥  Importar Datos desde Excel");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(ACCENT);
        titleRow.add(lblTitle);

        btnLogin.addActionListener(e -> abrirLogin());
        titleRow.add(btnLogin);
        panel.add(titleRow, "growx");

        lblSesion.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        panel.add(lblSesion, "growx");

        JLabel lblSubtitle = new JLabel(
            "Seleccione un archivo .xlsx con la hoja 'Config'. Se requiere autenticación para escribir en la BD.");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtitle.setForeground(TEXT_SECONDARY);
        panel.add(lblSubtitle, "growx, gapbottom 4");

        // Selección de Archivo + Config
        JPanel cardArchivo = createCard();
        cardArchivo.setLayout(new MigLayout(
            "insets 14, fillx, gap 10",
            "[]10[grow]push[]",
            "[]6[]"
        ));

        btnSeleccionar.addActionListener(e -> seleccionarArchivo());
        cardArchivo.add(btnSeleccionar);

        lblArchivoSeleccionado.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblArchivoSeleccionado.setForeground(TEXT_SECONDARY);
        cardArchivo.add(lblArchivoSeleccionado, "growx, wrap");

        lblConfigInfo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblConfigInfo.setForeground(TEXT_SECONDARY);
        cardArchivo.add(lblConfigInfo, "span, growx");

        panel.add(cardArchivo, "growx");

        // Validación
        JPanel cardValidacion = createCard();
        cardValidacion.setLayout(new MigLayout(
            "insets 12 14 12 14, fillx, gap 10",
            "[]push[grow]",
            "[]"
        ));

        btnValidar.setEnabled(false);
        btnValidar.addActionListener(e -> ejecutarValidacion());
        cardValidacion.add(btnValidar);

        lblValidacion.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblValidacion.setForeground(TEXT_SECONDARY);
        cardValidacion.add(lblValidacion, "growx");

        panel.add(cardValidacion, "growx");

        // Tabla de Previsualización
        tblPreview.setRowHeight(28);
        tblPreview.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tblPreview.setShowHorizontalLines(true);
        tblPreview.setShowVerticalLines(false);
        tblPreview.setIntercellSpacing(new Dimension(0, 1));
        tblPreview.setFillsViewportHeight(true);
        tblPreview.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JTableHeader header = tblPreview.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 11));
        header.setPreferredSize(new Dimension(0, 32));
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        tblPreview.setDefaultRenderer(Object.class, leftRenderer);

        JScrollPane scrollPane = new JScrollPane(tblPreview,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));

        panel.add(scrollPane, "grow");

        // Progreso + Botón Importar
        JPanel cardAcciones = createCard();
        cardAcciones.setLayout(new MigLayout(
            "insets 12 14 12 14, fillx, gap 10",
            "[grow]14[]",
            "[]"
        ));

        progressBar.setStringPainted(true);
        progressBar.setString("Esperando archivo y autenticación...");
        progressBar.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        progressBar.setPreferredSize(new Dimension(0, 28));
        cardAcciones.add(progressBar, "growx");

        btnImportar.setEnabled(false);
        btnImportar.addActionListener(e -> ejecutarImportacion());
        cardAcciones.add(btnImportar);

        panel.add(cardAcciones, "growx");

        return panel;
    }

    private JPanel buildReportePanel() {
        JPanel panel = new JPanel(new MigLayout(
            "insets 20 24 20 24, fill, wrap, gapy 10",
            "[grow]",
            "[][][grow]"
        ));
        panel.setOpaque(false);

        // Encabezado
        JLabel lblReporteTitle = new JLabel("📋  Reporte de Productos");
        lblReporteTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblReporteTitle.setForeground(ACCENT);
        panel.add(lblReporteTitle, "growx");

        // Barra de Herramientas (Tarjeta de Acciones)
        JPanel cardAcciones = createCard();
        cardAcciones.setLayout(new MigLayout(
            "insets 10 14 10 14, fillx, gap 10",
            "[]10[]10[grow]10[]",
            "[]"
        ));

        btnCargarReporte.addActionListener(e -> cargarReporteDesdeBD());
        cardAcciones.add(btnCargarReporte);

        JLabel lblLupa = new JLabel("🔍");
        lblLupa.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cardAcciones.add(lblLupa);

        txtBusquedaReporte.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, 
            "Buscar por código, descripción, línea, marca, categoría, proveedor o principio activo...");
        txtBusquedaReporte.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        txtBusquedaReporte.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                filtrarReporte();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                filtrarReporte();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                filtrarReporte();
            }
        });
        cardAcciones.add(txtBusquedaReporte, "growx");

        lblReporteStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblReporteStatus.setForeground(TEXT_SECONDARY);
        cardAcciones.add(lblReporteStatus, "right");

        panel.add(cardAcciones, "growx");

        // Tabla de Reporte
        tblReporte.setRowHeight(28);
        tblReporte.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tblReporte.setShowHorizontalLines(true);
        tblReporte.setShowVerticalLines(false);
        tblReporte.setIntercellSpacing(new Dimension(0, 1));
        tblReporte.setFillsViewportHeight(true);
        tblReporte.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JTableHeader header = tblReporte.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 11));
        header.setPreferredSize(new Dimension(0, 32));
        header.setReorderingAllowed(false);

        // Renderizadores de celdas
        DefaultTableCellRenderer numericRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof Number n) {
                    setText(String.format("%.2f", n.doubleValue()));
                }
                setHorizontalAlignment(SwingConstants.RIGHT);
                return this;
            }
        };
        tblReporte.getColumnModel().getColumn(8).setCellRenderer(numericRenderer);
        tblReporte.getColumnModel().getColumn(9).setCellRenderer(numericRenderer);

        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        for (int i = 0; i < 8; i++) {
            tblReporte.getColumnModel().getColumn(i).setCellRenderer(leftRenderer);
        }

        // TableRowSorter
        TableRowSorter<ReporteTableModel> sorter = new TableRowSorter<>(reporteModel);
        tblReporte.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(tblReporte,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));

        panel.add(scrollPane, "grow");

        return panel;
    }

    // ═══════════════════════════════════════════════════════════════
    //  Lógica de Filtrado y Carga de Reporte
    // ═══════════════════════════════════════════════════════════════

    private void filtrarReporte() {
        String text = txtBusquedaReporte.getText().trim();
        @SuppressWarnings("unchecked")
        TableRowSorter<ReporteTableModel> sorter = (TableRowSorter<ReporteTableModel>) tblReporte.getRowSorter();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            String regex = "(?i)" + java.util.regex.Pattern.quote(text);
            // Columnas de texto: índices del 0 al 7
            sorter.setRowFilter(RowFilter.regexFilter(regex, 0, 1, 2, 3, 4, 5, 6, 7));
        }
        actualizarReporteStatusLabel();
    }

    private void actualizarReporteStatusLabel() {
        int visibles = tblReporte.getRowCount();
        int totales = reporteModel.getRowCount();
        lblReporteStatus.setText(visibles + " de " + totales + " productos");
    }

    private void cargarReporteDesdeBD() {
        btnCargarReporte.setEnabled(false);
        txtBusquedaReporte.setEnabled(false);
        lblReporteStatus.setText("Cargando...");
        
        new SwingWorker<List<ProductoReporteRow>, Void>() {
            @Override
            protected List<ProductoReporteRow> doInBackground() throws Exception {
                com.droai.dao.ArticuloDAO dao = new com.droai.dao.ArticuloDAO();
                return dao.fetchReporte();
            }

            @Override
            protected void done() {
                btnCargarReporte.setEnabled(true);
                txtBusquedaReporte.setEnabled(true);
                try {
                    List<ProductoReporteRow> list = get();
                    reporteModel.setData(list);
                    filtrarReporte();
                    Toast.show("✔ Reporte cargado exitosamente: " + list.size() + " productos", Toast.Type.SUCCESS);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.show("✘ Error al cargar reporte: " + e.getMessage(), Toast.Type.ERROR);
                    actualizarReporteStatusLabel();
                }
            }
        }.execute();
    }

    // ═══════════════════════════════════════════════════════════════
    //  API Pública
    // ═══════════════════════════════════════════════════════════════

    public boolean isShowingReporteTab() {
        return innerTabs != null && innerTabs.getSelectedIndex() == 1;
    }

    public List<ProductoReporteRow> getReporteRowsVisibles() {
        List<ProductoReporteRow> list = new ArrayList<>();
        if (tblReporte == null || reporteModel == null) {
            return list;
        }
        int count = tblReporte.getRowCount();
        for (int i = 0; i < count; i++) {
            int modelIdx = tblReporte.convertRowIndexToModel(i);
            ProductoReporteRow r = reporteModel.getRow(modelIdx);
            if (r != null) {
                list.add(r);
            }
        }
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  Lógica de Importación
    // ═══════════════════════════════════════════════════════════════

    private void abrirLogin() {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        LoginAuditoriaDialog dialog = new LoginAuditoriaDialog(owner);
        dialog.setVisible(true);

        if (dialog.isAutenticado()) {
            Toast.show("✔ Sesión iniciada: " + SesionUsuario.current().getCoUsuario(),
                    Toast.Type.SUCCESS);
            actualizarSesionLabel();
            actualizarEstadoBotones();
        }
    }

    private void actualizarSesionLabel() {
        if (SesionUsuario.isAutenticado()) {
            SesionUsuario s = SesionUsuario.current();
            lblSesion.setText("🟢 Sesión activa: %s (%s) — Las operaciones quedarán registradas a este usuario."
                    .formatted(s.getCoUsuario(), s.getNombreUsuario()));
            lblSesion.setForeground(new Color(0, 180, 130));
            btnLogin.setText("✔ " + s.getCoUsuario());
            btnLogin.setBackground(new Color(0, 180, 130));
        } else {
            lblSesion.setText("🔴 Sin sesión activa — Debe autenticarse antes de importar datos.");
            lblSesion.setForeground(new Color(220, 60, 60));
        }
    }

    private void seleccionarArchivo() {
        JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
        chooser.setDialogTitle("Seleccionar archivo Excel de importación");
        chooser.setFileFilter(new FileNameExtensionFilter("Archivos Excel (*.xlsx)", "xlsx"));
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            archivoSeleccionado = chooser.getSelectedFile();
            validacionPasada = false;
            lblArchivoSeleccionado.setText("📄 " + archivoSeleccionado.getName());
            lblArchivoSeleccionado.setForeground(UIManager.getColor("Label.foreground"));
            cargarConfigYPreview();
        }
    }

    private void cargarConfigYPreview() {
        btnSeleccionar.setEnabled(false);
        btnValidar.setEnabled(false);
        btnImportar.setEnabled(false);
        progressBar.setString("Leyendo configuración...");
        progressBar.setIndeterminate(true);

        new SwingWorker<PreviewResult, Void>() {
            @Override
            protected PreviewResult doInBackground() throws Exception {
                configActual = service.leerConfiguracion(archivoSeleccionado);
                return service.previsualizarDatos(archivoSeleccionado, configActual, 50);
            }

            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                btnSeleccionar.setEnabled(true);
                try {
                    PreviewResult result = get();

                    int totalFilas = configActual.getFilaFin() - configActual.getFilaInicio() + 1;
                    lblConfigInfo.setText(
                        "📋 Hoja: \"%s\"  |  Filas: %d → %d (%d registros)  |  Campos: %d"
                            .formatted(configActual.getHojaInput(),
                                configActual.getFilaInicio(), configActual.getFilaFin(),
                                totalFilas, configActual.getColumnMap().size()));
                    lblConfigInfo.setForeground(ACCENT);

                    previewModel.setData(result.headers(), result.rows());
                    for (int i = 0; i < tblPreview.getColumnCount(); i++) {
                        tblPreview.getColumnModel().getColumn(i).setPreferredWidth(120);
                    }

                    progressBar.setString("✔ Vista previa cargada (%d filas)."
                            .formatted(result.rows().size()));
                    progressBar.setValue(0);

                    lblValidacion.setText("Archivo cargado. Presione 'Validar Datos' para verificar.");
                    lblValidacion.setForeground(WARN_AMBER);
                    actualizarEstadoBotones();

                    Toast.show("Archivo leído: %d filas".formatted(result.rows().size()),
                            Toast.Type.SUCCESS);

                } catch (Exception ex) {
                    ex.printStackTrace();
                    progressBar.setString("✘ Error al leer el archivo.");
                    Toast.show("Error: " + ex.getMessage(), Toast.Type.ERROR);
                }
            }
        }.execute();
    }

    private void ejecutarValidacion() {
        btnValidar.setEnabled(false);
        btnImportar.setEnabled(false);
        progressBar.setString("Validando datos contra la base de datos...");
        progressBar.setIndeterminate(true);

        new SwingWorker<ValidationResult, Void>() {
            @Override
            protected ValidationResult doInBackground() throws Exception {
                return service.validarEnMemoria(archivoSeleccionado);
            }

            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                btnValidar.setEnabled(true);
                try {
                    ValidationResult result = get();

                    StringBuilder sb = new StringBuilder();
                    sb.append("Existentes: %d  |  Nuevos: %d"
                            .formatted(result.existentes(), result.nuevos()));
                    if (!result.errores().isEmpty()) {
                        sb.append("  |  ").append(String.join(" | ", result.errores()));
                    }

                    if (result.valid()) {
                        validacionPasada = true;
                        lblValidacion.setText("✔ " + sb);
                        lblValidacion.setForeground(new Color(0, 180, 130));
                        progressBar.setString("✔ Validación exitosa. Listo para importar.");
                        Toast.show("Validación exitosa", Toast.Type.SUCCESS);
                    } else {
                        validacionPasada = false;
                        lblValidacion.setText("✘ " + sb);
                        lblValidacion.setForeground(new Color(220, 60, 60));
                        progressBar.setString("✘ Validación fallida. Corrija los errores.");
                        Toast.show("Validación fallida: revise los errores", Toast.Type.ERROR);
                    }
                    actualizarEstadoBotones();

                } catch (Exception ex) {
                    ex.printStackTrace();
                    lblValidacion.setText("✘ Error en validación: " + ex.getMessage());
                    lblValidacion.setForeground(new Color(220, 60, 60));
                    progressBar.setString("✘ Error durante la validación.");
                    Toast.show("Error: " + ex.getMessage(), Toast.Type.ERROR);
                }
            }
        }.execute();
    }

    private void ejecutarImportacion() {
        if (!SesionUsuario.isAutenticado()) {
            Toast.show("Debe autenticarse primero", Toast.Type.WARNING);
            abrirLogin();
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "¿Confirma la importación a la base de datos?\n\n"
            + "• Los artículos existentes serán ACTUALIZADOS.\n"
            + "• Los artículos nuevos serán INSERTADOS.\n"
            + "• Usuario de auditoría: " + SesionUsuario.current().getCoUsuario() + "\n\n"
            + "Esta operación es transaccional y se puede revertir si falla.",
            "Confirmar Importación",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        btnImportar.setEnabled(false);
        btnSeleccionar.setEnabled(false);
        btnValidar.setEnabled(false);
        progressBar.setValue(0);

        new SwingWorker<Integer, int[]>() {
            @Override
            protected Integer doInBackground() throws Exception {
                return service.procesarImportacion(archivoSeleccionado, (current, total, message) -> {
                    publish(new int[]{current, total});
                    SwingUtilities.invokeLater(() -> {
                        if (total > 0) {
                            progressBar.setValue((int) ((double) current / total * 100));
                            progressBar.setString(message);
                        } else {
                            progressBar.setString(message);
                        }
                    });
                });
            }

            @Override
            protected void done() {
                btnSeleccionar.setEnabled(true);
                btnValidar.setEnabled(true);
                try {
                    int total = get();
                    progressBar.setValue(100);
                    progressBar.setString("✔ Importación completada: %d registros procesados."
                            .formatted(total));
                    Toast.show("Importación exitosa: %d registros".formatted(total),
                            Toast.Type.SUCCESS);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    progressBar.setString("✘ Error durante la importación (rollback ejecutado).");
                    
                    String mensajeError = ex.getMessage();
                    if (ex.getCause() != null) {
                        mensajeError += "\n\nCausa: " + ex.getCause().getMessage();
                    }
                    JOptionPane.showMessageDialog(ImportarPanel.this,
                            "No se pudo completar la importación. Se ejecutó un rollback de la transacción.\n\nDetalle del error:\n" + mensajeError,
                            "Error de Base de Datos",
                            JOptionPane.ERROR_MESSAGE);
                            
                    Toast.show("Error: " + ex.getMessage(), Toast.Type.ERROR);
                    btnImportar.setEnabled(true);
                }
            }
        }.execute();
    }

    private void actualizarEstadoBotones() {
        boolean tieneArchivo = archivoSeleccionado != null && configActual != null;
        boolean autenticado = SesionUsuario.isAutenticado();

        btnValidar.setEnabled(tieneArchivo && autenticado);
        btnImportar.setEnabled(tieneArchivo && autenticado && validacionPasada);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Helpers UI
    // ═══════════════════════════════════════════════════════════════

    private JPanel createCard() {
        JPanel card = new JPanel();
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(4, 4, 4, 4)
        ));
        return card;
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI Emoji", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        return btn;
    }

    // ═══════════════════════════════════════════════════════════════
    //  TableModel dinámico
    // ═══════════════════════════════════════════════════════════════

    private static class PreviewTableModel extends AbstractTableModel {
        private String[] headers = {};
        private List<ArticuloImportRow> rows = List.of();

        public void setData(String[] headers, List<ArticuloImportRow> rows) {
            this.headers = headers != null ? headers : new String[0];
            this.rows = rows != null ? rows : List.of();
            fireTableStructureChanged();
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return headers.length; }

        @Override
        public String getColumnName(int col) {
            return col < headers.length ? headers[col] : "Col " + (col + 1);
        }

        @Override
        public Object getValueAt(int row, int col) {
            String[] raw = rows.get(row).getRawValues();
            if (raw != null && col < raw.length) return raw[col];
            return "";
        }

        @Override public boolean isCellEditable(int row, int col) { return false; }
    }
}
