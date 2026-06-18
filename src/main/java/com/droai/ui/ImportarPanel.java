package com.droai.ui;

import com.droai.dao.ImportacionDAO.ValidationResult;
import com.droai.dao.ReporteProductoDAO;
import com.droai.model.ArticuloImportRow;
import com.droai.model.ImportConfig;
import com.droai.model.ProductoReporteRow;
import com.droai.model.SesionUsuario;
import com.droai.service.ImportadorService;
import com.droai.service.ImportadorService.PreviewResult;
import com.droai.ui.components.Toast;
import com.droai.ui.dialog.LoginAuditoriaDialog;
import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
 * <p>Requiere autenticación contra Profit Plus antes de permitir
 * la escritura en la base de datos.
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
    private final ReporteProductoDAO reporteDAO;

    // ── UI Components: Importación ──
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

    // ── UI Components: Reporte ──
    private JButton btnCargarReporte;
    private JLabel lblReporteStatus;
    private JTable tblReporte;
    private ReporteTableModel reporteModel;
    private JTextField txtBuscarReporte;
    private TableRowSorter<ReporteTableModel> reporteSorter;
    private JTabbedPane innerTabs;

    // ── Estado ──
    private File archivoSeleccionado;
    private ImportConfig configActual;
    private boolean validacionPasada = false;

    public ImportarPanel() {
        service = new ImportadorService();
        reporteDAO = new ReporteProductoDAO();
        setLayout(new MigLayout("insets 0, fill", "[grow]", "[grow]"));
        setOpaque(false);

        // ── Sub-pestañas internas ──
        innerTabs = new JTabbedPane();
        innerTabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        innerTabs.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_TYPE, "underlined");

        innerTabs.addTab("  📥 Importar Excel  ", buildImportPanel());
        innerTabs.addTab("  📋 Reporte de Productos  ", buildReportePanel());

        add(innerTabs, "grow");
    }

    // ═══════════════════════════════════════════════════════════════
    //  Construcción del Panel de Importación (contenido original)
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildImportPanel() {
        JPanel panel = new JPanel(new MigLayout(
            "insets 20 24 20 24, fill, wrap, gapy 10",
            "[grow]",
            "[][][][][][grow][]"
        ));
        panel.setOpaque(false);

        // ═══════════════════════════════════════════════════════════
        //  Sección 1: Título + Sesión
        // ═══════════════════════════════════════════════════════════
        JPanel titleRow = new JPanel(new MigLayout("insets 0, fillx", "[]push[]", "[]"));
        titleRow.setOpaque(false);

        JLabel lblTitle = new JLabel("📥  Importar Datos desde Excel");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(ACCENT);
        titleRow.add(lblTitle);

        btnLogin = createStyledButton("🔐  Autenticar", ACCENT);
        btnLogin.addActionListener(e -> abrirLogin());
        titleRow.add(btnLogin);
        panel.add(titleRow, "growx");

        // ── Sesión info ──
        lblSesion = new JLabel();
        lblSesion.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        actualizarSesionLabel();
        panel.add(lblSesion, "growx");

        // ── Subtítulo ──
        JLabel lblSubtitle = new JLabel(
            "Seleccione un archivo .xlsx con la hoja 'Config'. Se requiere autenticación para escribir en la BD.");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtitle.setForeground(TEXT_SECONDARY);
        panel.add(lblSubtitle, "growx, gapbottom 4");

        // ═══════════════════════════════════════════════════════════
        //  Sección 2: Selección de Archivo + Config
        // ═══════════════════════════════════════════════════════════
        JPanel cardArchivo = createCard();
        cardArchivo.setLayout(new MigLayout(
            "insets 14, fillx, gap 10",
            "[]10[grow]push[]",
            "[]6[]"
        ));

        btnSeleccionar = createStyledButton("📁  Seleccionar Archivo Excel", ACCENT);
        btnSeleccionar.addActionListener(e -> seleccionarArchivo());
        cardArchivo.add(btnSeleccionar);

        lblArchivoSeleccionado = new JLabel("Ningún archivo seleccionado");
        lblArchivoSeleccionado.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblArchivoSeleccionado.setForeground(TEXT_SECONDARY);
        cardArchivo.add(lblArchivoSeleccionado, "growx, wrap");

        lblConfigInfo = new JLabel(" ");
        lblConfigInfo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblConfigInfo.setForeground(TEXT_SECONDARY);
        cardArchivo.add(lblConfigInfo, "span, growx");

        panel.add(cardArchivo, "growx");

        // ═══════════════════════════════════════════════════════════
        //  Sección 3: Validación
        // ═══════════════════════════════════════════════════════════
        JPanel cardValidacion = createCard();
        cardValidacion.setLayout(new MigLayout(
            "insets 12 14 12 14, fillx, gap 10",
            "[]push[grow]",
            "[]"
        ));

        btnValidar = createStyledButton("🔍  Validar Datos", WARN_AMBER);
        btnValidar.setEnabled(false);
        btnValidar.addActionListener(e -> ejecutarValidacion());
        cardValidacion.add(btnValidar);

        lblValidacion = new JLabel("Esperando archivo y autenticación...");
        lblValidacion.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblValidacion.setForeground(TEXT_SECONDARY);
        cardValidacion.add(lblValidacion, "growx");

        panel.add(cardValidacion, "growx");

        // ═══════════════════════════════════════════════════════════
        //  Sección 4: Tabla de Previsualización
        // ═══════════════════════════════════════════════════════════
        previewModel = new PreviewTableModel();
        tblPreview = new JTable(previewModel);
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

        // ═══════════════════════════════════════════════════════════
        //  Sección 5: Progreso + Botón Importar
        // ═══════════════════════════════════════════════════════════
        JPanel cardAcciones = createCard();
        cardAcciones.setLayout(new MigLayout(
            "insets 12 14 12 14, fillx, gap 10",
            "[grow]14[]",
            "[]"
        ));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Esperando archivo y autenticación...");
        progressBar.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        progressBar.setPreferredSize(new Dimension(0, 28));
        cardAcciones.add(progressBar, "growx");

        btnImportar = createStyledButton("⚡  Procesar e Importar a BD", SUCCESS_GREEN);
        btnImportar.setEnabled(false);
        btnImportar.addActionListener(e -> ejecutarImportacion());
        cardAcciones.add(btnImportar);

        panel.add(cardAcciones, "growx");

        return panel;
    }

    // ═══════════════════════════════════════════════════════════════
    //  Construcción del Panel de Reporte de Productos
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildReportePanel() {
        JPanel panel = new JPanel(new MigLayout(
            "insets 20 24 20 24, fill, wrap, gapy 10",
            "[grow]",
            "[][][grow]"
        ));
        panel.setOpaque(false);

        // ── Título ──
        JLabel lblReporteTitle = new JLabel("📋  Reporte de Productos — Línea / Principio Activo / Categoría / Proveedor");
        lblReporteTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblReporteTitle.setForeground(ACCENT);
        panel.add(lblReporteTitle, "growx");

        // ── Barra de acciones: Botón + Búsqueda + Status ──
        JPanel cardAccionesReporte = createCard();
        cardAccionesReporte.setLayout(new MigLayout(
            "insets 12 14 12 14, fillx, gap 10",
            "[]16[][grow]push[]",
            "[]"
        ));

        btnCargarReporte = createStyledButton("🔄  Cargar Reporte desde BD", ACCENT);
        btnCargarReporte.addActionListener(e -> cargarReporteProductos());
        cardAccionesReporte.add(btnCargarReporte);

        JLabel lblBuscar = new JLabel("🔍");
        lblBuscar.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        cardAccionesReporte.add(lblBuscar);

        txtBuscarReporte = new JTextField();
        txtBuscarReporte.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtBuscarReporte.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                "Buscar por código, descripción, línea, principio activo, categoría o proveedor...");
        txtBuscarReporte.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filtrarReporte(); }
            @Override public void removeUpdate(DocumentEvent e) { filtrarReporte(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrarReporte(); }
        });
        cardAccionesReporte.add(txtBuscarReporte, "growx");

        lblReporteStatus = new JLabel("Sin datos cargados.");
        lblReporteStatus.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblReporteStatus.setForeground(TEXT_SECONDARY);
        cardAccionesReporte.add(lblReporteStatus);

        panel.add(cardAccionesReporte, "growx");

        // ── Tabla de Reporte ──
        reporteModel = new ReporteTableModel();
        tblReporte = new JTable(reporteModel);
        tblReporte.setRowHeight(28);
        tblReporte.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tblReporte.setShowHorizontalLines(true);
        tblReporte.setShowVerticalLines(false);
        tblReporte.setIntercellSpacing(new Dimension(0, 1));
        tblReporte.setFillsViewportHeight(true);
        tblReporte.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        reporteSorter = new TableRowSorter<>(reporteModel);
        tblReporte.setRowSorter(reporteSorter);

        JTableHeader reporteHeader = tblReporte.getTableHeader();
        reporteHeader.setFont(new Font("Segoe UI", Font.BOLD, 11));
        reporteHeader.setPreferredSize(new Dimension(0, 34));
        reporteHeader.setReorderingAllowed(false);

        // Renderer numérico alineado a la derecha para la columna Existencia
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tblReporte.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);

        JScrollPane reporteScroll = new JScrollPane(tblReporte,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        reporteScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));

        panel.add(reporteScroll, "grow");

        return panel;
    }

    // ═══════════════════════════════════════════════════════════════
    //  Lógica: Autenticación
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

    // ═══════════════════════════════════════════════════════════════
    //  Lógica: Seleccionar Archivo
    // ═══════════════════════════════════════════════════════════════
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

    // ═══════════════════════════════════════════════════════════════
    //  Lógica: Validación
    // ═══════════════════════════════════════════════════════════════
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

    // ═══════════════════════════════════════════════════════════════
    //  Lógica: Importación
    // ═══════════════════════════════════════════════════════════════
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
                    
                    // Mostrar error detallado en un diálogo para evitar que el Toast lo corte
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

    // ═══════════════════════════════════════════════════════════════
    //  Estado de botones
    // ═══════════════════════════════════════════════════════════════
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
    //  Lógica: Reporte de Productos
    // ═══════════════════════════════════════════════════════════════
    private void cargarReporteProductos() {
        btnCargarReporte.setEnabled(false);
        lblReporteStatus.setText("⏳ Consultando la base de datos...");
        lblReporteStatus.setForeground(WARN_AMBER);

        new SwingWorker<List<ProductoReporteRow>, Void>() {
            @Override
            protected List<ProductoReporteRow> doInBackground() throws Exception {
                return reporteDAO.fetchReporteProductos();
            }

            @Override
            protected void done() {
                btnCargarReporte.setEnabled(true);
                try {
                    List<ProductoReporteRow> datos = get();
                    reporteModel.setData(datos);

                    // Actualizar sorter con el nuevo modelo
                    reporteSorter = new TableRowSorter<>(reporteModel);
                    tblReporte.setRowSorter(reporteSorter);
                    filtrarReporte(); // Re-aplicar filtro si hay texto

                    lblReporteStatus.setText("✔ %d productos cargados".formatted(datos.size()));
                    lblReporteStatus.setForeground(new Color(0, 180, 130));
                    Toast.show("Reporte cargado: %d productos".formatted(datos.size()),
                            Toast.Type.SUCCESS);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    lblReporteStatus.setText("✘ Error: " + ex.getMessage());
                    lblReporteStatus.setForeground(new Color(220, 60, 60));
                    Toast.show("Error al cargar reporte: " + ex.getMessage(), Toast.Type.ERROR);
                }
            }
        }.execute();
    }

    /**
     * Filtra las filas del reporte según el texto ingresado en el campo de búsqueda.
     * Busca en todas las columnas de texto (código, descripción, línea, principio activo,
     * categoría, proveedor).
     */
    private void filtrarReporte() {
        String texto = txtBuscarReporte.getText().trim();
        if (texto.isEmpty()) {
            reporteSorter.setRowFilter(null);
        } else {
            // Filtrar en columnas de texto (0-5): Código, Descripción, Línea, P. Activo, Categoría, Proveedor
            reporteSorter.setRowFilter(
                    RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(texto), 0, 1, 2, 3, 4, 5)
            );
        }
        // Actualizar conteo visible
        int visible = tblReporte.getRowCount();
        int total = reporteModel.getRowCount();
        if (!texto.isEmpty()) {
            lblReporteStatus.setText("🔍 %d de %d productos".formatted(visible, total));
        }
    }

    public boolean isShowingReporteTab() {
        return innerTabs != null && innerTabs.getSelectedIndex() == 1;
    }

    public List<ProductoReporteRow> getReporteRowsVisibles() {
        List<ProductoReporteRow> list = new ArrayList<>();
        if (tblReporte == null || reporteModel == null) return list;
        int rowCount = tblReporte.getRowCount();
        List<ProductoReporteRow> modelData = reporteModel.getData();
        for (int i = 0; i < rowCount; i++) {
            int modelIdx = tblReporte.convertRowIndexToModel(i);
            if (modelIdx >= 0 && modelIdx < modelData.size()) {
                list.add(modelData.get(modelIdx));
            }
        }
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  TableModel: Preview Importación (dinámico)
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

    // ═══════════════════════════════════════════════════════════════
    //  TableModel: Reporte de Productos
    // ═══════════════════════════════════════════════════════════════
    private static class ReporteTableModel extends AbstractTableModel {

        private static final String[] COLUMNS = {
            "Código", "Descripción", "Línea", "Principio Activo",
            "Categoría", "Proveedor", "Existencia"
        };

        private List<ProductoReporteRow> data = new ArrayList<>();

        public List<ProductoReporteRow> getData() {
            return data;
        }

        public void setData(List<ProductoReporteRow> data) {
            this.data = data != null ? data : new ArrayList<>();
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int col) { return COLUMNS[col]; }

        @Override
        public Class<?> getColumnClass(int col) {
            return col == 6 ? Double.class : String.class;
        }

        @Override
        public Object getValueAt(int row, int col) {
            ProductoReporteRow r = data.get(row);
            return switch (col) {
                case 0 -> r.getCodigo();
                case 1 -> r.getDescripcion();
                case 2 -> r.getLinea();
                case 3 -> r.getPrincipioActivo();
                case 4 -> r.getCategoria();
                case 5 -> r.getProveedor();
                case 6 -> r.getExistencia();
                default -> "";
            };
        }

        @Override public boolean isCellEditable(int row, int col) { return false; }
    }
}
