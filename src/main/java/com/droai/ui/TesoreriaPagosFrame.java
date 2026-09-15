package com.droai.ui;

import com.droai.dao.TesoreriaDAO;
import com.droai.dao.TesoreriaDAO.ConceptoGastoInfo;
import com.droai.dao.TesoreriaDAO.CuentaBancariaInfo;
import com.droai.dao.TesoreriaDAO.TransaccionEjecutivaInfo;
import com.droai.model.TesoreriaPagoItem;
import com.droai.model.TesoreriaPagoItem.DestinoProfit;
import com.droai.model.TesoreriaPagoItem.EstadoValidacion;
import com.droai.service.TesoreriaService;
import com.droai.service.TesoreriaService.ArqueoDiaResult;
import com.droai.service.TesoreriaService.ReporteEjecutivoData;
import com.droai.ui.components.RoundedPanel;
import com.droai.ui.components.TesoreriaChartsPanel;
import com.droai.ui.components.Toast;
import com.droai.ui.util.IconHelper;
import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.io.File;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Ventana Principal de Gestión de Tesorería y Control de Pagos.
 * Exclusivo para Carla Navarro (CN) y Jean Gutiérrez (JG).
 *
 * Estructurada en 3 vistas principales:
 * 1. REGISTRO MANUAL DIRECTO (Predeterminada): Formulario ágil para registrar pagos individuales durante el día.
 * 2. CARGA MASIVA (Excel Arqueo Diario): Ingesta masiva del libro diario multioja y conciliación en lote.
 * 3. REPORTE EJECUTIVO & ANALÍTICA: Métricas consolidadas de Profit Plus por rango de fecha, gráficas interactivas y exportación Excel.
 */
public class TesoreriaPagosFrame extends JFrame {

    private final ThemeManager tm = ThemeManager.get();
    private final TesoreriaService service;
    private final TesoreriaDAO dao;

    // Catálogos
    private List<CuentaBancariaInfo> cuentasBancarias = new ArrayList<>();
    private List<ConceptoGastoInfo> conceptosGasto = new ArrayList<>();
    private double tasaUSDActual = 1.0;

    // Pestañas
    private JTabbedPane mainTabs;

    // =========================================================================
    // 1. ESTADO TAB REGISTRO MANUAL
    // =========================================================================
    private JComboBox<CuentaBancariaInfo> cmbManualCuenta;
    private JComboBox<DestinoProfit> cmbManualDestino;
    private JTextField txtManualFecha;
    private JTextField txtManualReferencia;
    private JLabel lblManualStatusRef;
    private JTextField txtManualBeneficiario;
    private JComboBox<ConceptoGastoInfo> cmbManualConcepto;
    private JTextField txtManualMontoBs;
    private JTextField txtManualTasa;
    private JLabel lblManualMontoUSD;
    private JTextField txtManualDescrip;
    private JButton btnManualRegistrar;
    private JButton btnManualLimpiar;
    private JTable tableManualSession;
    private ManualSessionTableModel modelManualSession;
    private final List<ManualSessionRow> sessionManualItems = new ArrayList<>();
    private JLabel lblManualSessionResumen;

    public record ManualSessionRow(
            String hora,
            String banco,
            String referencia,
            DestinoProfit destino,
            String beneficiario,
            String concepto,
            double montoBs,
            double montoUSD,
            String estatus
    ) {}

    // =========================================================================
    // 2. ESTADO TAB CARGA MASIVA EXCEL
    // =========================================================================
    private File currentFile;
    private final List<TesoreriaPagoItem> allItems = new ArrayList<>();
    private final List<TesoreriaPagoItem> displayedItems = new ArrayList<>();
    private JLabel lblArchivoInfo;
    private JComboBox<String> cmbHojas;
    private JButton btnCargarHoja;
    private JButton btnSeleccionarArchivo;
    private JLabel lblKpiFecha;
    private JLabel lblKpiTasa;
    private JLabel lblKpiApertura;
    private JLabel lblKpiCierre;
    private JLabel lblKpiTotalPagos;
    private JTable tablePagos;
    private TesoreriaTableModel tableModel;
    private JTextField txtFiltro;
    private JLabel lblResumenSeleccion;
    private JButton btnValidarProfit;
    private JButton btnProcesarProfit;
    private JButton btnSeleccionarTodo;
    private JButton btnDeseleccionarTodo;

    // =========================================================================
    // 3. ESTADO TAB REPORTE EJECUTIVO & ANALÍTICA
    // =========================================================================
    private JTextField txtRepDesde;
    private JTextField txtRepHasta;
    private JComboBox<String> cmbRepCuenta;
    private JComboBox<String> cmbRepTipo;
    private JButton btnConsultarReporte;
    private JButton btnExportarReporteExcel;
    private JLabel lblKpiRepTotalBs;
    private JLabel lblKpiRepTotalUSD;
    private JLabel lblKpiRepCount;
    private JLabel lblKpiRepMB;
    private JLabel lblKpiRepOP;
    private JLabel lblKpiRepTasa;
    private TesoreriaChartsPanel chartsPanel;
    private JTable tableReporte;
    private ReporteTableModel modelReporte;
    private final List<TransaccionEjecutivaInfo> currentTransacciones = new ArrayList<>();

    // Formateadores
    private static final DecimalFormat CURRENCY_BS = new DecimalFormat("Bs #,##0.00");
    private static final DecimalFormat CURRENCY_USD = new DecimalFormat("$ #,##0.00");
    private static final DecimalFormat TASA_FMT = new DecimalFormat("#,##0.0000 Bs/$");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    public TesoreriaPagosFrame() {
        this.service = new TesoreriaService();
        this.dao = service.getDao();

        setTitle("DroAI - Tesorería y Control de Pagos (Carla Navarro & JG)");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1380, 840);
        setMinimumSize(new Dimension(1160, 680));
        setLocationRelativeTo(null);

        IconHelper.applyAppIcon(this);
        Toast.setParentFrame(this);

        cargarCatalogosProfit();
        buildUI();

        // Carga inicial del reporte ejecutivo para el mes actual en segundo plano
        SwingUtilities.invokeLater(this::ejecutarConsultaReporte);
    }

    private void cargarCatalogosProfit() {
        try {
            cuentasBancarias = dao.listarCuentasBancarias();
            conceptosGasto = dao.listarConceptosGasto();
            tasaUSDActual = dao.obtenerTasaUSD();
        } catch (Exception e) {
            System.err.println("[TesoreriaPagosFrame] Error cargando catálogos Profit: " + e.getMessage());
        }
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(tm.background());

        // =========================================================================
        // HEADER SUPERIOR GENERAL
        // =========================================================================
        JPanel header = new JPanel(new MigLayout("insets 14 24 12 24, fillx", "[grow]push[]", "[]"));
        header.setBackground(tm.cardBg());
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, tm.border()));

        JPanel titlePanel = new JPanel(new MigLayout("insets 0, gap 12", "[][]", "[]"));
        titlePanel.setOpaque(false);

        JLabel lblIcon = new JLabel("🏦");
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 30));
        titlePanel.add(lblIcon);

        JPanel titleTexts = new JPanel(new MigLayout("insets 0, wrap, gap 0", "[]", "[]2[]"));
        titleTexts.setOpaque(false);

        JLabel lblTitle = new JLabel("Tesorería y Control de Pagos");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 19));
        lblTitle.setForeground(tm.textPrimary());
        titleTexts.add(lblTitle);

        JLabel lblSub = new JLabel("Registro Manual Inmediato, Conciliación de Arqueo Diario Excel y Reportes Analíticos Profit Plus");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(tm.textSecondary());
        titleTexts.add(lblSub);

        titlePanel.add(titleTexts);
        header.add(titlePanel);

        // Indicador de tasa vigente en header
        JLabel lblTasaHeader = new JLabel(String.format("Tasa Profit: %.4f Bs/$", tasaUSDActual));
        lblTasaHeader.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTasaHeader.setForeground(new Color(16, 185, 129));
        lblTasaHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(16, 185, 129), 1, true),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        header.add(lblTasaHeader);

        root.add(header, BorderLayout.NORTH);

        // =========================================================================
        // PESTAÑAS PRINCIPALES (JTabbedPane)
        // =========================================================================
        mainTabs = new JTabbedPane();
        mainTabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        mainTabs.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSABLE, false);

        // Pestaña 1: REGISTRO MANUAL DIRECTO (Por defecto)
        mainTabs.addTab("  Registro Manual Directo  ", buildTabRegistroManual());

        // Pestaña 2: CARGA MASIVA EXCEL
        mainTabs.addTab("  Carga Masiva (Arqueo Diario Excel)  ", buildTabCargaMasiva());

        // Pestaña 3: REPORTE EJECUTIVO & ANALÍTICA
        mainTabs.addTab("  Reporte Ejecutivo & Analítica  ", buildTabReporteEjecutivo());

        root.add(mainTabs, BorderLayout.CENTER);
        setContentPane(root);
    }

    // =========================================================================
    // VISTA 1: REGISTRO MANUAL DIRECTO
    // =========================================================================
    private JPanel buildTabRegistroManual() {
        JPanel panel = new JPanel(new MigLayout("insets 16 24 16 24, fill, wrap", "[grow]", "[]14[grow]"));
        panel.setOpaque(false);

        // Formulario en tarjeta redondeada
        RoundedPanel formCard = new RoundedPanel(14, true);
        formCard.setBackground(tm.cardBg());
        formCard.setLayout(new MigLayout("insets 18 20 18 20, fillx, gap 12", "[170, fill][190, fill][140, fill][grow, fill]", "[]8[]8[]8[]8[]"));
        formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 3, 0, tm.greenAccent()),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        JLabel lblFormTitle = new JLabel("Ingreso Rápido de Pago Individual");
        lblFormTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblFormTitle.setForeground(tm.textPrimary());
        formCard.add(lblFormTitle, "span 4, wrap");

        // Fila 1: Cuenta, Destino, Fecha, Beneficiario
        formCard.add(createFieldLabel("1. Cuenta Bancaria Origen:"));
        formCard.add(createFieldLabel("2. Destino en Profit Plus:"));
        formCard.add(createFieldLabel("3. Fecha de Pago:"));
        formCard.add(createFieldLabel("4. Beneficiario / Proveedor:"), "wrap");

        cmbManualCuenta = new JComboBox<>();
        if (!cuentasBancarias.isEmpty()) {
            for (CuentaBancariaInfo cb : cuentasBancarias) cmbManualCuenta.addItem(cb);
        }
        formCard.add(cmbManualCuenta);

        cmbManualDestino = new JComboBox<>(DestinoProfit.values());
        formCard.add(cmbManualDestino);

        txtManualFecha = new JTextField(LocalDate.now().format(DATE_FMT));
        txtManualFecha.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        formCard.add(txtManualFecha);

        txtManualBeneficiario = new JTextField();
        txtManualBeneficiario.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ej. DROGUERIA NENA, GASTOS VARIOS, etc.");
        formCard.add(txtManualBeneficiario, "wrap");

        // Fila 2: Referencia + botón verificar + status, Concepto de Gasto
        formCard.add(createFieldLabel("5. Referencia Bancaria:"));
        formCard.add(createFieldLabel("Validación en Profit:"));
        formCard.add(createFieldLabel("6. Concepto de Gasto (saCuentaIngEgr):"), "span 2, wrap");

        JPanel refBox = new JPanel(new MigLayout("insets 0, fillx", "[grow][]", "[]"));
        refBox.setOpaque(false);
        txtManualReferencia = new JTextField();
        txtManualReferencia.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "N° Referencia / Transferencia");
        refBox.add(txtManualReferencia, "grow");

        JButton btnVerificarRef = new JButton("Verificar");
        btnVerificarRef.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnVerificarRef.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVerificarRef.addActionListener(e -> verificarReferenciaManual());
        refBox.add(btnVerificarRef);
        formCard.add(refBox);

        lblManualStatusRef = new JLabel("Pendiente de verificar");
        lblManualStatusRef.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblManualStatusRef.setForeground(tm.textSecondary());
        formCard.add(lblManualStatusRef);

        cmbManualConcepto = new JComboBox<>();
        if (!conceptosGasto.isEmpty()) {
            for (ConceptoGastoInfo cgi : conceptosGasto) cmbManualConcepto.addItem(cgi);
        }
        formCard.add(cmbManualConcepto, "span 2, wrap");

        // Fila 3: Monto Bs, Tasa, Monto USD, Descripción
        formCard.add(createFieldLabel("7. Monto en Bolívares (Bs.):"));
        formCard.add(createFieldLabel("8. Tasa de Cambio (Bs./$):"));
        formCard.add(createFieldLabel("Monto Equivalente ($):"));
        formCard.add(createFieldLabel("9. Descripción / Observación / Glosa:"), "wrap");

        txtManualMontoBs = new JTextField();
        txtManualMontoBs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        txtManualMontoBs.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "0,00");
        txtManualMontoBs.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                recalcularMontoUSDManual();
            }
        });
        formCard.add(txtManualMontoBs);

        txtManualTasa = new JTextField(String.format(Locale.US, "%.4f", tasaUSDActual));
        txtManualTasa.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtManualTasa.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                recalcularMontoUSDManual();
            }
        });
        formCard.add(txtManualTasa);

        lblManualMontoUSD = new JLabel("$ 0.00");
        lblManualMontoUSD.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblManualMontoUSD.setForeground(new Color(16, 185, 129));
        formCard.add(lblManualMontoUSD);

        txtManualDescrip = new JTextField();
        txtManualDescrip.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Comentario para el registro contable en Profit...");
        formCard.add(txtManualDescrip, "wrap");

        // Fila 4: Botones de Acción
        JPanel actionBox = new JPanel(new MigLayout("insets 8 0 0 0, fillx", "[]12[]push", "[]"));
        actionBox.setOpaque(false);

        btnManualRegistrar = new JButton("1. Registrar Pago en Profit Plus");
        btnManualRegistrar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnManualRegistrar.setBackground(new Color(16, 185, 129)); // Verde
        btnManualRegistrar.setForeground(Color.WHITE);
        btnManualRegistrar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnManualRegistrar.addActionListener(e -> registrarPagoManual());
        actionBox.add(btnManualRegistrar);

        btnManualLimpiar = new JButton("Limpiar Formulario");
        btnManualLimpiar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnManualLimpiar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnManualLimpiar.addActionListener(e -> limpiarFormularioManual());
        actionBox.add(btnManualLimpiar);

        formCard.add(actionBox, "span 4, growx");
        panel.add(formCard, "growx");

        // Tabla de Sesión (Historial del día)
        RoundedPanel sessionCard = new RoundedPanel(14, true);
        sessionCard.setBackground(tm.cardBg());
        sessionCard.setLayout(new MigLayout("insets 14 18 14 18, fill, wrap", "[grow]", "[]8[grow]8[]"));

        JLabel lblHistTitle = new JLabel("Pagos Registrados en la Sesión de Hoy");
        lblHistTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblHistTitle.setForeground(tm.textPrimary());
        sessionCard.add(lblHistTitle);

        modelManualSession = new ManualSessionTableModel();
        tableManualSession = new JTable(modelManualSession);
        tableManualSession.setRowHeight(28);
        tableManualSession.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableManualSession.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        JScrollPane spSession = new JScrollPane(tableManualSession);
        spSession.setBorder(BorderFactory.createLineBorder(tm.border(), 1));
        sessionCard.add(spSession, "grow");

        lblManualSessionResumen = new JLabel("0 pagos registrados en esta sesión | Total: Bs 0,00 ($ 0.00)");
        lblManualSessionResumen.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblManualSessionResumen.setForeground(tm.textSecondary());
        sessionCard.add(lblManualSessionResumen);

        panel.add(sessionCard, "grow");
        return panel;
    }

    private JLabel createFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(tm.textSecondary());
        return lbl;
    }

    private void recalcularMontoUSDManual() {
        try {
            String sBs = txtManualMontoBs.getText().trim().replace(".", "").replace(",", ".");
            String sTasa = txtManualTasa.getText().trim().replace(",", ".");
            if (!sBs.isEmpty() && !sTasa.isEmpty()) {
                double bs = Double.parseDouble(sBs);
                double tasa = Double.parseDouble(sTasa);
                if (tasa > 0) {
                    double usd = bs / tasa;
                    lblManualMontoUSD.setText(CURRENCY_USD.format(usd));
                    return;
                }
            }
        } catch (Exception ignored) {}
        lblManualMontoUSD.setText("$ 0.00");
    }

    private void verificarReferenciaManual() {
        String ref = txtManualReferencia.getText().trim();
        if (ref.isEmpty()) {
            lblManualStatusRef.setText("Ingresa una referencia primero");
            lblManualStatusRef.setForeground(new Color(239, 68, 68));
            return;
        }

        CuentaBancariaInfo cta = (CuentaBancariaInfo) cmbManualCuenta.getSelectedItem();
        String codCta = cta != null ? cta.codCta() : "";

        Optional<String> existe = dao.verificarReferenciaIndividual(codCta, ref);
        if (existe.isPresent()) {
            lblManualStatusRef.setText("Ya existe: " + existe.get());
            lblManualStatusRef.setForeground(new Color(239, 68, 68));
            Toast.show("¡Alerta! La referencia ya está registrada en Profit", Toast.Type.WARNING);
        } else {
            lblManualStatusRef.setText("Referencia libre (No registrada en Profit)");
            lblManualStatusRef.setForeground(new Color(16, 185, 129));
            Toast.show("Referencia disponible para registro", Toast.Type.SUCCESS);
        }
    }

    private void registrarPagoManual() {
        String ref = txtManualReferencia.getText().trim();
        if (ref.isEmpty()) {
            Toast.show("Debe indicar el número de referencia bancaria", Toast.Type.WARNING);
            txtManualReferencia.requestFocus();
            return;
        }

        double montoBs;
        try {
            String sBs = txtManualMontoBs.getText().trim().replace(".", "").replace(",", ".");
            montoBs = Double.parseDouble(sBs);
            if (montoBs <= 0) throw new IllegalArgumentException();
        } catch (Exception ex) {
            Toast.show("Indique un monto en Bolívares válido mayor a cero", Toast.Type.WARNING);
            txtManualMontoBs.requestFocus();
            return;
        }

        double tasa;
        try {
            String sTasa = txtManualTasa.getText().trim().replace(",", ".");
            tasa = Double.parseDouble(sTasa);
            if (tasa <= 0) tasa = tasaUSDActual;
        } catch (Exception ex) {
            tasa = tasaUSDActual;
        }

        double montoUSD = montoBs / tasa;
        CuentaBancariaInfo cta = (CuentaBancariaInfo) cmbManualCuenta.getSelectedItem();
        String banco = cta != null ? cta.descBanco() : "BANCO";
        DestinoProfit destino = (DestinoProfit) cmbManualDestino.getSelectedItem();
        ConceptoGastoInfo cgi = (ConceptoGastoInfo) cmbManualConcepto.getSelectedItem();
        String concepto = cgi != null ? cgi.descripcion() : "GASTOS VARIOS";
        String beneficiario = txtManualBeneficiario.getText().trim();
        if (beneficiario.isEmpty()) beneficiario = "BENEFICIARIO GENERAL";

        // Comprobación de duplicado
        Optional<String> existe = dao.verificarReferenciaIndividual(cta != null ? cta.codCta() : "", ref);
        if (existe.isPresent()) {
            int conf = JOptionPane.showConfirmDialog(this,
                    "<html><b>Atención:</b> La referencia <b>" + ref + "</b> ya existe en Profit Plus:<br><br>"
                            + "<span style='color:red;'>" + existe.get() + "</span><br><br>"
                            + "¿Desea registrarla de todas formas?",
                    "Posible Pago Duplicado",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (conf != JOptionPane.YES_OPTION) return;
        }

        // Agregar a la bitácora de sesión
        sessionManualItems.add(new ManualSessionRow(
                LocalTime.now().format(TIME_FMT),
                banco,
                ref,
                destino,
                beneficiario,
                concepto,
                montoBs,
                montoUSD,
                "Registrado OK"
        ));
        modelManualSession.fireTableDataChanged();

        // Actualizar resumen de sesión
        double totBs = sessionManualItems.stream().mapToDouble(ManualSessionRow::montoBs).sum();
        double totUSD = sessionManualItems.stream().mapToDouble(ManualSessionRow::montoUSD).sum();
        lblManualSessionResumen.setText(String.format("%d pagos registrados en esta sesión | Total: %s (%s)",
                sessionManualItems.size(), CURRENCY_BS.format(totBs), CURRENCY_USD.format(totUSD)));

        Toast.show("Pago " + ref + " registrado exitosamente en la sesión", Toast.Type.SUCCESS);
        limpiarFormularioManual();
    }

    private void limpiarFormularioManual() {
        txtManualReferencia.setText("");
        txtManualBeneficiario.setText("");
        txtManualMontoBs.setText("");
        txtManualDescrip.setText("");
        lblManualMontoUSD.setText("$ 0.00");
        lblManualStatusRef.setText("Pendiente de verificar");
        lblManualStatusRef.setForeground(tm.textSecondary());
        txtManualFecha.setText(LocalDate.now().format(DATE_FMT));
        txtManualReferencia.requestFocus();
    }

    // =========================================================================
    // VISTA 2: CARGA MASIVA (ARQUEO DIARIO EXCEL)
    // =========================================================================
    private JPanel buildTabCargaMasiva() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        // Barra de archivo y selección de hoja
        JPanel fileBar = new JPanel(new MigLayout("insets 12 24 10 24, fillx, gap 10", "[][][grow][]", "[]"));
        fileBar.setBackground(tm.cardBg());
        fileBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, tm.border()));

        btnSeleccionarArchivo = new JButton("Seleccionar Excel (.xlsx)");
        btnSeleccionarArchivo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSeleccionarArchivo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSeleccionarArchivo.addActionListener(e -> seleccionarArchivoExcel());
        fileBar.add(btnSeleccionarArchivo);

        lblArchivoInfo = new JLabel("No se ha cargado ningún archivo");
        lblArchivoInfo.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblArchivoInfo.setForeground(tm.textSecondary());
        fileBar.add(lblArchivoInfo);

        JLabel lblHoja = new JLabel("Día / Hoja:");
        lblHoja.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblHoja.setForeground(tm.textPrimary());
        fileBar.add(lblHoja);

        cmbHojas = new JComboBox<>();
        cmbHojas.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cmbHojas.setPreferredSize(new Dimension(140, 30));
        fileBar.add(cmbHojas);

        btnCargarHoja = new JButton("Cargar Arqueo");
        btnCargarHoja.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCargarHoja.setBackground(new Color(217, 119, 6)); // Dorado ámbar
        btnCargarHoja.setForeground(Color.WHITE);
        btnCargarHoja.setEnabled(false);
        btnCargarHoja.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCargarHoja.addActionListener(e -> cargarHojaSeleccionada());
        fileBar.add(btnCargarHoja);

        panel.add(fileBar, BorderLayout.NORTH);

        // Centro: KPI Cards y Tabla
        JPanel centerPanel = new JPanel(new MigLayout("insets 14 24 14 24, fill, wrap", "[grow]", "[]12[grow]"));
        centerPanel.setOpaque(false);

        // KPI CARDS
        JPanel kpiPanel = new JPanel(new MigLayout("insets 0, fillx, gap 12", "[grow][grow][grow][grow][grow]", "[]"));
        kpiPanel.setOpaque(false);

        lblKpiFecha = new JLabel("---");
        kpiPanel.add(createKpiCard("Fecha de Operación", lblKpiFecha, new Color(59, 130, 246)), "grow");

        lblKpiTasa = new JLabel("---");
        kpiPanel.add(createKpiCard("Tasa BCV Oficial", lblKpiTasa, new Color(16, 185, 129)), "grow");

        lblKpiApertura = new JLabel("---");
        kpiPanel.add(createKpiCard("Saldo Apertura", lblKpiApertura, new Color(139, 92, 246)), "grow");

        lblKpiCierre = new JLabel("---");
        kpiPanel.add(createKpiCard("Saldo Cierre", lblKpiCierre, new Color(245, 158, 11)), "grow");

        lblKpiTotalPagos = new JLabel("---");
        kpiPanel.add(createKpiCard("Total Pagos del Día", lblKpiTotalPagos, new Color(239, 68, 68)), "grow");

        centerPanel.add(kpiPanel, "growx");

        // TABLA Y FILTRO
        JPanel tableContainer = new JPanel(new MigLayout("insets 0, fill, wrap", "[grow]", "[]8[grow]"));
        tableContainer.setOpaque(false);

        JPanel filterBar = new JPanel(new MigLayout("insets 0, fillx", "[]8[]push[right]", "[]"));
        filterBar.setOpaque(false);

        btnSeleccionarTodo = new JButton("Seleccionar Todo");
        btnSeleccionarTodo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnSeleccionarTodo.addActionListener(e -> alternarSeleccion(true));
        filterBar.add(btnSeleccionarTodo);

        btnDeseleccionarTodo = new JButton("Deseleccionar Todo");
        btnDeseleccionarTodo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnDeseleccionarTodo.addActionListener(e -> alternarSeleccion(false));
        filterBar.add(btnDeseleccionarTodo);

        txtFiltro = new JTextField();
        txtFiltro.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar por referencia, beneficiario o concepto...");
        txtFiltro.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtFiltro.setPreferredSize(new Dimension(320, 28));
        txtFiltro.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                aplicarFiltro();
            }
        });
        filterBar.add(txtFiltro);

        tableContainer.add(filterBar, "growx");

        tableModel = new TesoreriaTableModel();
        tablePagos = new JTable(tableModel);
        tablePagos.setRowHeight(32);
        tablePagos.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tablePagos.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tablePagos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablePagos.getTableHeader().setReorderingAllowed(false);

        configurarColumnasTabla();

        JScrollPane scrollPane = new JScrollPane(tablePagos);
        scrollPane.setBorder(BorderFactory.createLineBorder(tm.border(), 1));
        tableContainer.add(scrollPane, "grow");

        centerPanel.add(tableContainer, "grow");
        panel.add(centerPanel, BorderLayout.CENTER);

        // FOOTER: Resumen y Botones
        JPanel footer = new JPanel(new MigLayout("insets 12 24 14 24, fillx", "[grow]push[][]", "[]"));
        footer.setBackground(tm.cardBg());
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, tm.border()));

        lblResumenSeleccion = new JLabel("0 pagos seleccionados | Total: Bs 0,00 ($ 0.00)");
        lblResumenSeleccion.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblResumenSeleccion.setForeground(tm.textPrimary());
        footer.add(lblResumenSeleccion);

        btnValidarProfit = new JButton("1. Validar contra Profit Plus (Solo Lectura)");
        btnValidarProfit.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnValidarProfit.setBackground(new Color(37, 99, 235)); // Azul
        btnValidarProfit.setForeground(Color.WHITE);
        btnValidarProfit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnValidarProfit.addActionListener(e -> ejecutarValidacionProfit());
        footer.add(btnValidarProfit);

        btnProcesarProfit = new JButton("2. Procesar Selección en Profit Plus");
        btnProcesarProfit.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnProcesarProfit.setBackground(new Color(16, 185, 129)); // Verde
        btnProcesarProfit.setForeground(Color.WHITE);
        btnProcesarProfit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnProcesarProfit.addActionListener(e -> confirmarYProcesarProfit());
        footer.add(btnProcesarProfit);

        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createKpiCard(String title, JLabel valueLabel, Color accent) {
        RoundedPanel card = new RoundedPanel(12, true);
        card.setBackground(tm.cardBg());
        card.setLayout(new MigLayout("insets 10 14 10 14, wrap, fillx", "[grow]", "[]4[]"));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 3, 0, accent),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        JLabel lblT = new JLabel(title);
        lblT.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblT.setForeground(tm.textSecondary());
        card.add(lblT);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        valueLabel.setForeground(tm.textPrimary());
        card.add(valueLabel);

        return card;
    }

    private void configurarColumnasTabla() {
        TableColumn colCheck = tablePagos.getColumnModel().getColumn(0);
        colCheck.setPreferredWidth(35);
        colCheck.setMaxWidth(45);

        // Columna 1: Banco Pagador
        TableColumn colBanco = tablePagos.getColumnModel().getColumn(1);
        colBanco.setPreferredWidth(140);
        JComboBox<String> cmbBancos = new JComboBox<>();
        if (!cuentasBancarias.isEmpty()) {
            for (CuentaBancariaInfo cb : cuentasBancarias) cmbBancos.addItem(cb.codCta() + " - " + cb.descBanco());
        } else {
            cmbBancos.addItem("0108 - PROVINCIAL");
            cmbBancos.addItem("0134 - BANESCO");
            cmbBancos.addItem("0191 - BNC");
            cmbBancos.addItem("0102 - VENEZUELA");
        }
        colBanco.setCellEditor(new DefaultCellEditor(cmbBancos));

        tablePagos.getColumnModel().getColumn(2).setPreferredWidth(100);
        tablePagos.getColumnModel().getColumn(3).setPreferredWidth(260);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tablePagos.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);
        tablePagos.getColumnModel().getColumn(4).setPreferredWidth(110);
        tablePagos.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);
        tablePagos.getColumnModel().getColumn(5).setPreferredWidth(95);

        // Columna 6: DESTINO EN PROFIT
        TableColumn colDestino = tablePagos.getColumnModel().getColumn(6);
        colDestino.setPreferredWidth(190);
        JComboBox<DestinoProfit> cmbDestinos = new JComboBox<>(DestinoProfit.values());
        colDestino.setCellEditor(new DefaultCellEditor(cmbDestinos));
        colDestino.setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof DestinoProfit dp) {
                    setText(dp.getLabel());
                    if (!isSelected) {
                        switch (dp) {
                            case MOVIMIENTO_BANCO -> {
                                setForeground(new Color(37, 99, 235));
                                setFont(getFont().deriveFont(Font.BOLD));
                            }
                            case ORDEN_PAGO -> {
                                setForeground(new Color(16, 185, 129));
                                setFont(getFont().deriveFont(Font.BOLD));
                            }
                            case TRASPASO -> {
                                setForeground(new Color(245, 158, 11));
                                setFont(getFont().deriveFont(Font.BOLD));
                            }
                            case OMITIR -> {
                                setForeground(Color.GRAY);
                                setFont(getFont().deriveFont(Font.ITALIC));
                            }
                        }
                    }
                }
                return c;
            }
        });

        // Columna 7: Concepto de Gasto
        TableColumn colConcepto = tablePagos.getColumnModel().getColumn(7);
        colConcepto.setPreferredWidth(200);
        if (!conceptosGasto.isEmpty()) {
            JComboBox<String> cmbConceptos = new JComboBox<>();
            for (ConceptoGastoInfo cgi : conceptosGasto) cmbConceptos.addItem(cgi.toString());
            colConcepto.setCellEditor(new DefaultCellEditor(cmbConceptos));
        }

        // Columna 8: Estatus en Profit
        TableColumn colEstado = tablePagos.getColumnModel().getColumn(8);
        colEstado.setPreferredWidth(150);
        colEstado.setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof EstadoValidacion ev) {
                    setText(ev.getLabel());
                    if (!isSelected) {
                        switch (ev) {
                            case NUEVO -> setForeground(new Color(16, 185, 129));
                            case YA_EXISTE -> setForeground(new Color(239, 68, 68));
                            case PROCESADO -> setForeground(new Color(37, 99, 235));
                            case ERROR -> setForeground(new Color(220, 38, 38));
                            default -> setForeground(tm.textSecondary());
                        }
                    }
                }
                return c;
            }
        });
    }

    private void seleccionarArchivoExcel() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccionar Libro de Control Bancario y Pagos (.xlsx)");
        chooser.setFileFilter(new FileNameExtensionFilter("Libros de Excel (*.xlsx)", "xlsx"));
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            currentFile = chooser.getSelectedFile();
            lblArchivoInfo.setText(currentFile.getName());
            lblArchivoInfo.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblArchivoInfo.setForeground(tm.textPrimary());

            cmbHojas.removeAllItems();
            btnCargarHoja.setEnabled(false);

            SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
                @Override
                protected List<String> doInBackground() throws Exception {
                    return service.obtenerHojasDisponibles(currentFile);
                }

                @Override
                protected void done() {
                    try {
                        List<String> hojas = get();
                        for (String h : hojas) cmbHojas.addItem(h);
                        if (!hojas.isEmpty()) {
                            cmbHojas.setSelectedIndex(hojas.size() - 1);
                            btnCargarHoja.setEnabled(true);
                            Toast.show("Libro cargado: " + hojas.size() + " días operativos detectados.", Toast.Type.SUCCESS);
                        } else {
                            Toast.show("No se encontraron hojas con formato DD-MM en el archivo.", Toast.Type.WARNING);
                        }
                    } catch (Exception ex) {
                        Toast.show("Error al inspeccionar el libro: " + ex.getMessage(), Toast.Type.ERROR);
                    }
                }
            };
            worker.execute();
        }
    }

    private void cargarHojaSeleccionada() {
        String hoja = (String) cmbHojas.getSelectedItem();
        if (currentFile == null || hoja == null) return;

        btnCargarHoja.setEnabled(false);
        Toast.show("Procesando arqueo del día " + hoja + "...", Toast.Type.INFO);

        SwingWorker<ArqueoDiaResult, Void> worker = new SwingWorker<>() {
            @Override
            protected ArqueoDiaResult doInBackground() throws Exception {
                return service.procesarArqueoDia(currentFile, hoja);
            }

            @Override
            protected void done() {
                btnCargarHoja.setEnabled(true);
                try {
                    ArqueoDiaResult res = get();
                    lblKpiFecha.setText(res.fecha() != null ? res.fecha().format(DATE_FMT) : hoja);
                    lblKpiTasa.setText(res.tasaDia() > 0 ? TASA_FMT.format(res.tasaDia()) : "---");
                    lblKpiApertura.setText(CURRENCY_BS.format(res.totalAperturaBs()));
                    lblKpiCierre.setText(CURRENCY_BS.format(res.totalCierreBs()));
                    lblKpiTotalPagos.setText(String.valueOf(res.pagos().size()));

                    allItems.clear();
                    allItems.addAll(res.pagos());
                    aplicarFiltro();

                    Toast.show(String.format("Arqueo cargado: %d transacciones analizadas para el día %s.", res.pagos().size(), hoja), Toast.Type.SUCCESS);
                } catch (Exception ex) {
                    Toast.show("Error procesando arqueo: " + ex.getMessage(), Toast.Type.ERROR);
                }
            }
        };
        worker.execute();
    }

    private void aplicarFiltro() {
        String query = txtFiltro.getText().trim().toLowerCase();
        displayedItems.clear();
        for (TesoreriaPagoItem it : allItems) {
            if (query.isEmpty() ||
                    it.getReferencia().toLowerCase().contains(query) ||
                    it.getDescripcion().toLowerCase().contains(query) ||
                    it.getDescConcepto().toLowerCase().contains(query) ||
                    it.getCodCta().toLowerCase().contains(query)) {
                displayedItems.add(it);
            }
        }
        tableModel.fireTableDataChanged();
        actualizarResumenSeleccion();
    }

    private void alternarSeleccion(boolean sel) {
        for (TesoreriaPagoItem it : displayedItems) it.setSeleccionado(sel);
        tableModel.fireTableDataChanged();
        actualizarResumenSeleccion();
    }

    private void actualizarResumenSeleccion() {
        int count = 0;
        double sumBs = 0;
        double sumUSD = 0;
        for (TesoreriaPagoItem it : displayedItems) {
            if (it.isSeleccionado()) {
                count++;
                sumBs += it.getMontoBs();
                sumUSD += it.getMontoUSD();
            }
        }
        lblResumenSeleccion.setText(String.format("%d pagos seleccionados | Total: %s (%s)",
                count, CURRENCY_BS.format(sumBs), CURRENCY_USD.format(sumUSD)));
    }

    private void ejecutarValidacionProfit() {
        if (displayedItems.isEmpty()) {
            Toast.show("No hay transacciones para validar.", Toast.Type.WARNING);
            return;
        }

        btnValidarProfit.setEnabled(false);
        Toast.show("Consultando saMovimientoBanco y saOrdenPago en Profit Plus...", Toast.Type.INFO);

        SwingWorker<Integer, Void> worker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() {
                service.validarReferenciasContraProfit(displayedItems);
                return displayedItems.size();
            }

            @Override
            protected void done() {
                btnValidarProfit.setEnabled(true);
                tableModel.fireTableDataChanged();

                long nuevos = displayedItems.stream().filter(it -> it.getEstadoValidacion() == EstadoValidacion.NUEVO).count();
                long existen = displayedItems.stream().filter(it -> it.getEstadoValidacion() == EstadoValidacion.YA_EXISTE).count();

                String msg = String.format("Validación completada: %d listos para registrar, %d ya existen en Profit.", nuevos, existen);
                Toast.show(msg, existen > 0 ? Toast.Type.WARNING : Toast.Type.SUCCESS);
                JOptionPane.showMessageDialog(TesoreriaPagosFrame.this, msg, "Resultado Validación Profit Plus", JOptionPane.INFORMATION_MESSAGE);
            }
        };
        worker.execute();
    }

    private void confirmarYProcesarProfit() {
        List<TesoreriaPagoItem> seleccionados = displayedItems.stream()
                .filter(TesoreriaPagoItem::isSeleccionado)
                .filter(it -> it.getDestinoProfit() != DestinoProfit.OMITIR)
                .toList();

        if (seleccionados.isEmpty()) {
            Toast.show("No hay ningún pago seleccionado para registrar en Profit.", Toast.Type.WARNING);
            return;
        }

        double totalBs = seleccionados.stream().mapToDouble(TesoreriaPagoItem::getMontoBs).sum();
        double totalUSD = seleccionados.stream().mapToDouble(TesoreriaPagoItem::getMontoUSD).sum();

        long cantMB = seleccionados.stream().filter(it -> it.getDestinoProfit() == DestinoProfit.MOVIMIENTO_BANCO).count();
        long cantOP = seleccionados.stream().filter(it -> it.getDestinoProfit() == DestinoProfit.ORDEN_PAGO).count();

        String resumenHtml = "<html><body style='width: 380px; font-family: sans-serif;'>"
                + "<h3>Confirmación de Registro en Profit Plus</h3>"
                + "Se procesarán <b>" + seleccionados.size() + " transacciones</b>:<br>"
                + "<ul>"
                + "<li><b>Movimientos de Banco (Gastos):</b> " + cantMB + "</li>"
                + "<li><b>Órdenes de Pago (Proveedores):</b> " + cantOP + "</li>"
                + "<li><b>Total en Bolívares:</b> " + CURRENCY_BS.format(totalBs) + "</li>"
                + "<li><b>Total en Dólares:</b> " + CURRENCY_USD.format(totalUSD) + "</li>"
                + "</ul>"
                + "<p style='color: #2563eb;'><i>Modo Seguro:</i> Las transacciones con estatus 'Ya existe' serán omitidas para evitar duplicados.</p>"
                + "¿Desea proceder con el registro?</body></html>";

        int res = JOptionPane.showConfirmDialog(this, resumenHtml, "Confirmar Registro de Pagos", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (res == JOptionPane.YES_OPTION) {
            for (TesoreriaPagoItem it : seleccionados) {
                if (it.getEstadoValidacion() != EstadoValidacion.YA_EXISTE) {
                    it.setEstadoValidacion(EstadoValidacion.PROCESADO);
                    it.setMensajeValidacion("Simulación OK: Listo para inserción final");
                }
            }
            tableModel.fireTableDataChanged();
            Toast.show("Lote de pagos procesado en modo seguro.", Toast.Type.SUCCESS);
        }
    }

    // =========================================================================
    // VISTA 3: REPORTE EJECUTIVO & ANALÍTICA
    // =========================================================================
    private JPanel buildTabReporteEjecutivo() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        // Barra de Filtros
        JPanel filterBar = new JPanel(new MigLayout("insets 12 24 10 24, fillx, gap 10", "[][110, fill][][110, fill][][180, fill][][180, fill]push[][]", "[]"));
        filterBar.setBackground(tm.cardBg());
        filterBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, tm.border()));

        JLabel lblD = new JLabel("Desde:");
        lblD.setFont(new Font("Segoe UI", Font.BOLD, 12));
        filterBar.add(lblD);

        txtRepDesde = new JTextField(LocalDate.now().withDayOfMonth(1).format(DATE_FMT));
        txtRepDesde.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filterBar.add(txtRepDesde);

        JLabel lblH = new JLabel("Hasta:");
        lblH.setFont(new Font("Segoe UI", Font.BOLD, 12));
        filterBar.add(lblH);

        txtRepHasta = new JTextField(LocalDate.now().format(DATE_FMT));
        txtRepHasta.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filterBar.add(txtRepHasta);

        JLabel lblCta = new JLabel("Banco / Cuenta:");
        lblCta.setFont(new Font("Segoe UI", Font.BOLD, 12));
        filterBar.add(lblCta);

        cmbRepCuenta = new JComboBox<>();
        cmbRepCuenta.addItem("TODAS LAS CUENTAS");
        for (CuentaBancariaInfo cb : cuentasBancarias) cmbRepCuenta.addItem(cb.codCta() + " - " + cb.descBanco());
        filterBar.add(cmbRepCuenta);

        JLabel lblTipo = new JLabel("Tipo Operación:");
        lblTipo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        filterBar.add(lblTipo);

        cmbRepTipo = new JComboBox<>(new String[]{"TODOS", "MOVIMIENTO DE BANCO (GASTOS)", "ORDEN DE PAGO (PROVEEDORES)"});
        filterBar.add(cmbRepTipo);

        btnConsultarReporte = new JButton("Consultar Profit");
        btnConsultarReporte.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnConsultarReporte.setBackground(new Color(37, 99, 235)); // Azul
        btnConsultarReporte.setForeground(Color.WHITE);
        btnConsultarReporte.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnConsultarReporte.addActionListener(e -> ejecutarConsultaReporte());
        filterBar.add(btnConsultarReporte);

        btnExportarReporteExcel = new JButton("Exportar Excel (.xlsx)");
        btnExportarReporteExcel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnExportarReporteExcel.setBackground(new Color(16, 185, 129)); // Verde
        btnExportarReporteExcel.setForeground(Color.WHITE);
        btnExportarReporteExcel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnExportarReporteExcel.addActionListener(e -> exportarReporteExcel());
        filterBar.add(btnExportarReporteExcel);

        panel.add(filterBar, BorderLayout.NORTH);

        // Contenedor Central con KPIs, Gráficas y Tabla Detalle
        JPanel content = new JPanel(new MigLayout("insets 14 24 14 24, fill, wrap", "[grow]", "[]12[270, fill]12[grow, fill]"));
        content.setOpaque(false);

        // Tarjetas KPI (6 columnas)
        JPanel kpiRepPanel = new JPanel(new MigLayout("insets 0, fillx, gap 10", "[grow][grow][grow][grow][grow][grow]", "[]"));
        kpiRepPanel.setOpaque(false);

        lblKpiRepTotalBs = new JLabel("Bs 0,00");
        kpiRepPanel.add(createKpiCard("Total Egresado (Bs.)", lblKpiRepTotalBs, new Color(59, 130, 246)), "grow");

        lblKpiRepTotalUSD = new JLabel("$ 0.00");
        kpiRepPanel.add(createKpiCard("Total Egresado ($ USD)", lblKpiRepTotalUSD, new Color(16, 185, 129)), "grow");

        lblKpiRepCount = new JLabel("0");
        kpiRepPanel.add(createKpiCard("N° Transacciones", lblKpiRepCount, new Color(139, 92, 246)), "grow");

        lblKpiRepMB = new JLabel("0");
        kpiRepPanel.add(createKpiCard("Movimientos de Banco", lblKpiRepMB, new Color(37, 99, 235)), "grow");

        lblKpiRepOP = new JLabel("0");
        kpiRepPanel.add(createKpiCard("Órdenes de Pago", lblKpiRepOP, new Color(245, 158, 11)), "grow");

        lblKpiRepTasa = new JLabel("---");
        kpiRepPanel.add(createKpiCard("Tasa Promedio Ponderada", lblKpiRepTasa, new Color(236, 72, 153)), "grow");

        content.add(kpiRepPanel, "growx");

        // Panel de Gráficas
        chartsPanel = new TesoreriaChartsPanel();
        content.add(chartsPanel, "growx");

        // Tabla Detalle de Transacciones
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setOpaque(false);

        JLabel lblDetTitle = new JLabel("Detalle Consolidado de Egresos Registrados en Profit Plus");
        lblDetTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblDetTitle.setForeground(tm.textPrimary());
        lblDetTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        tablePanel.add(lblDetTitle, BorderLayout.NORTH);

        modelReporte = new ReporteTableModel();
        tableReporte = new JTable(modelReporte);
        tableReporte.setRowHeight(28);
        tableReporte.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableReporte.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tableReporte.getColumnModel().getColumn(7).setCellRenderer(rightRenderer);
        tableReporte.getColumnModel().getColumn(8).setCellRenderer(rightRenderer);
        tableReporte.getColumnModel().getColumn(9).setCellRenderer(rightRenderer);

        JScrollPane spRep = new JScrollPane(tableReporte);
        spRep.setBorder(BorderFactory.createLineBorder(tm.border(), 1));
        tablePanel.add(spRep, BorderLayout.CENTER);

        content.add(tablePanel, "grow");
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private void ejecutarConsultaReporte() {
        LocalDate dDesde;
        LocalDate dHasta;

        try {
            dDesde = parseDateText(txtRepDesde.getText().trim());
            dHasta = parseDateText(txtRepHasta.getText().trim());
        } catch (Exception ex) {
            Toast.show("Formato de fecha inválido. Utilice dd/MM/yyyy", Toast.Type.WARNING);
            return;
        }

        String ctaSel = (String) cmbRepCuenta.getSelectedItem();
        String codCta = null;
        if (ctaSel != null && !ctaSel.startsWith("TODAS")) {
            codCta = ctaSel.split(" - ")[0].trim();
        }

        String tipoSel = (String) cmbRepTipo.getSelectedItem();

        btnConsultarReporte.setEnabled(false);
        Toast.show("Consultando egresos y pagos en Profit Plus...", Toast.Type.INFO);

        final String finalCodCta = codCta;
        SwingWorker<ReporteEjecutivoData, Void> worker = new SwingWorker<>() {
            @Override
            protected ReporteEjecutivoData doInBackground() {
                return service.generarReporteEjecutivo(dDesde, dHasta, finalCodCta, tipoSel);
            }

            @Override
            protected void done() {
                btnConsultarReporte.setEnabled(true);
                try {
                    ReporteEjecutivoData data = get();
                    lblKpiRepTotalBs.setText(CURRENCY_BS.format(data.totalBs()));
                    lblKpiRepTotalUSD.setText(CURRENCY_USD.format(data.totalUSD()));
                    lblKpiRepCount.setText(String.valueOf(data.totalTransacciones()));
                    lblKpiRepMB.setText(String.valueOf(data.countMovimientosBanco()));
                    lblKpiRepOP.setText(String.valueOf(data.countOrdenesPago()));
                    lblKpiRepTasa.setText(TASA_FMT.format(data.tasaPonderada()));

                    chartsPanel.updateData(data);

                    currentTransacciones.clear();
                    currentTransacciones.addAll(data.transacciones());
                    modelReporte.fireTableDataChanged();

                    Toast.show(String.format("Reporte actualizado: %d transacciones encontradas.", data.totalTransacciones()), Toast.Type.SUCCESS);
                } catch (Exception ex) {
                    Toast.show("Error consultando reporte en Profit: " + ex.getMessage(), Toast.Type.ERROR);
                }
            }
        };
        worker.execute();
    }

    private void exportarReporteExcel() {
        if (currentTransacciones.isEmpty()) {
            Toast.show("No hay transacciones para exportar.", Toast.Type.WARNING);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar Reporte Ejecutivo de Tesorería (.xlsx)");
        chooser.setSelectedFile(new File("Reporte_Tesoreria_" + LocalDate.now().toString() + ".xlsx"));
        chooser.setFileFilter(new FileNameExtensionFilter("Libros de Excel (*.xlsx)", "xlsx"));

        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            if (!target.getName().toLowerCase().endsWith(".xlsx")) {
                target = new File(target.getParentFile(), target.getName() + ".xlsx");
            }

            File finalFile = target;
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    LocalDate dDesde = parseDateText(txtRepDesde.getText().trim());
                    LocalDate dHasta = parseDateText(txtRepHasta.getText().trim());
                    ReporteEjecutivoData data = service.generarReporteEjecutivo(dDesde, dHasta, null, "TODOS");
                    service.exportarReporteEjecutivoExcel(finalFile, data, dDesde, dHasta);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        Toast.show("Reporte exportado exitosamente a: " + finalFile.getName(), Toast.Type.SUCCESS);
                        int opt = JOptionPane.showConfirmDialog(TesoreriaPagosFrame.this,
                                "El reporte se exportó correctamente en:\n" + finalFile.getAbsolutePath() + "\n\n¿Desea abrirlo ahora?",
                                "Exportación Exitosa", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                        if (opt == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().open(finalFile);
                        }
                    } catch (Exception ex) {
                        Toast.show("Error al exportar reporte Excel: " + ex.getMessage(), Toast.Type.ERROR);
                    }
                }
            };
            worker.execute();
        }
    }

    private LocalDate parseDateText(String txt) {
        if (txt == null || txt.isBlank()) return LocalDate.now();
        String[] p = txt.split("/");
        if (p.length == 3) {
            int d = Integer.parseInt(p[0].trim());
            int m = Integer.parseInt(p[1].trim());
            int y = Integer.parseInt(p[2].trim());
            if (y < 100) y += 2000;
            return LocalDate.of(y, m, d);
        }
        return LocalDate.parse(txt);
    }

    // =========================================================================
    // TABLE MODELS
    // =========================================================================

    // Model Tab 1: Historial de Sesión
    private class ManualSessionTableModel extends AbstractTableModel {
        private final String[] cols = {"Hora", "Banco", "Referencia", "Destino Profit", "Beneficiario", "Concepto de Gasto", "Monto (Bs)", "Monto ($)", "Estatus"};

        @Override public int getRowCount() { return sessionManualItems.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override
        public Object getValueAt(int row, int col) {
            ManualSessionRow r = sessionManualItems.get(row);
            return switch (col) {
                case 0 -> r.hora();
                case 1 -> r.banco();
                case 2 -> r.referencia();
                case 3 -> r.destino().getLabel();
                case 4 -> r.beneficiario();
                case 5 -> r.concepto();
                case 6 -> CURRENCY_BS.format(r.montoBs());
                case 7 -> CURRENCY_USD.format(r.montoUSD());
                case 8 -> r.estatus();
                default -> "";
            };
        }
    }

    // Model Tab 2: Arqueo Masivo
    private class TesoreriaTableModel extends AbstractTableModel {
        private final String[] columns = {
                "Sel", "Banco Origen", "Referencia", "Descripción / Beneficiario",
                "Monto (Bs)", "Monto ($)", "Destino en Profit", "Concepto de Gasto", "Estatus Profit"
        };

        @Override public int getRowCount() { return displayedItems.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int col) { return columns[col]; }

        @Override
        public Class<?> getColumnClass(int col) {
            return switch (col) {
                case 0 -> Boolean.class;
                case 4, 5 -> Double.class;
                case 6 -> DestinoProfit.class;
                case 8 -> EstadoValidacion.class;
                default -> String.class;
            };
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col == 0 || col == 1 || col == 6 || col == 7;
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (row >= displayedItems.size()) return null;
            TesoreriaPagoItem it = displayedItems.get(row);
            return switch (col) {
                case 0 -> it.isSeleccionado();
                case 1 -> resolverNombreBanco(it.getCodCta());
                case 2 -> it.getReferencia();
                case 3 -> it.getDescripcion();
                case 4 -> it.getMontoBs();
                case 5 -> it.getMontoUSD();
                case 6 -> it.getDestinoProfit();
                case 7 -> it.getCodConcepto() + " - " + it.getDescConcepto();
                case 8 -> it.getEstadoValidacion();
                default -> "";
            };
        }

        @Override
        public void setValueAt(Object val, int row, int col) {
            if (row >= displayedItems.size()) return;
            TesoreriaPagoItem it = displayedItems.get(row);
            switch (col) {
                case 0 -> {
                    it.setSeleccionado(Boolean.TRUE.equals(val));
                    actualizarResumenSeleccion();
                }
                case 1 -> {
                    if (val != null) {
                        String s = val.toString();
                        if (s.startsWith("0108")) it.setCodCta("0108");
                        else if (s.startsWith("0134")) it.setCodCta("0134");
                        else if (s.startsWith("0191")) it.setCodCta("0191");
                        else if (s.startsWith("0102")) it.setCodCta("0102");
                    }
                }
                case 6 -> {
                    if (val instanceof DestinoProfit dp) it.setDestinoProfit(dp);
                }
                case 7 -> {
                    if (val != null) {
                        String str = val.toString();
                        String[] p = str.split(" - ", 2);
                        it.setCodConcepto(p[0].trim());
                        if (p.length > 1) it.setDescConcepto(p[1].trim());
                    }
                }
            }
            fireTableCellUpdated(row, col);
        }

        private String resolverNombreBanco(String codCta) {
            return switch (codCta != null ? codCta.trim() : "") {
                case "0108" -> "0108 - PROVINCIAL";
                case "0134" -> "0134 - BANESCO";
                case "0191" -> "0191 - BNC";
                case "0102" -> "0102 - VENEZUELA";
                default -> codCta != null ? codCta : "0134 - BANESCO";
            };
        }
    }

    // Model Tab 3: Reporte Ejecutivo
    private class ReporteTableModel extends AbstractTableModel {
        private final String[] cols = {
                "N° Doc", "Origen", "Banco / Cuenta", "Fecha", "Referencia",
                "Beneficiario / Proveedor", "Concepto de Gasto", "Monto (Bs)", "Tasa", "Monto ($)"
        };

        @Override public int getRowCount() { return currentTransacciones.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override
        public Object getValueAt(int row, int col) {
            TransaccionEjecutivaInfo t = currentTransacciones.get(row);
            return switch (col) {
                case 0 -> t.idDoc();
                case 1 -> t.tipoOrigen();
                case 2 -> t.bancoDesc();
                case 3 -> t.fecha() != null ? t.fecha().format(DATE_FMT) : "";
                case 4 -> t.referencia();
                case 5 -> t.beneficiario();
                case 6 -> t.codConcepto() + " - " + t.conceptoDesc();
                case 7 -> CURRENCY_BS.format(t.montoBs());
                case 8 -> String.format(Locale.US, "%.4f", t.tasa());
                case 9 -> CURRENCY_USD.format(t.montoUSD());
                default -> "";
            };
        }
    }
}
