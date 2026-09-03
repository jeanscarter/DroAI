package com.droai.ui;

import com.droai.dao.NotaCreditoDAO;
import com.droai.model.NotaCreditoModel;
import com.droai.ui.components.Toast;
import com.droai.ui.dialog.ImpresionNotaCreditoDialog;
import com.droai.ui.util.IconHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Ventana de Documento de Venta / Nota de Crédito recreada fielmente a partir del módulo nativo de Profit Plus.
 */
public class DocumentoVentaFrame extends JFrame {

    private final NotaCreditoDAO dao;
    private final ThemeManager tm = ThemeManager.get();
    private NotaCreditoModel currentDoc;
    private boolean modoUSD = true; // Por defecto visualización en USD tal como en la captura

    private static final DecimalFormat DF;
    private static final DecimalFormat DFTasa;
    private static final DateTimeFormatter DTF_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");
    private static final DateTimeFormatter DTF_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.of("es", "VE"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        DF = new DecimalFormat("#,##0.00", symbols);
        DFTasa = new DecimalFormat("#,##0.00000000", symbols);
    }

    // ── Componentes Pestaña General ──
    private JTextField txtNumero;
    private JLabel lblEstatus;
    private JTextField txtFecha;
    private JTextField txtTipoMov;
    private JTextField txtTipoDoc;
    private JTextField txtTipoDocDesc;
    private JTextField txtCodCliente;
    private JTextField txtNomCliente;
    private JTextField txtCodVendedor;
    private JTextField txtNomVendedor;
    private JTextField txtVencimiento;
    private JTextField txtFechaReg;
    private JTextField txtDescripcion;
    private JTextField txtNControl;
    private JTextField txtDocAsoc;
    private JTextField txtNroRegistro;
    private JTextField txtFechaCheque;
    private JTextField txtBanco;
    private JTextField txtNroCheque;
    private JTextField txtMovBanco;
    private JTextField txtComprobIva;
    private JTextField txtMoneda;
    private JTextField txtMonedaDesc;
    private JTextField txtTasa;
    private JComboBox<String> cmbIva;
    private JButton btnConversion;

    // Montos
    private JTextField txtMontoBruto;
    private JTextField txtPorcDesc;
    private JTextField txtMontoDesc;
    private JTextField txtPorcReca;
    private JTextField txtMontoReca;
    private JTextField txtTotalSinImpuesto;
    private JTextField txtIva;
    private JTextField txtOtros;
    private JTextField txtMontoNeto;
    private JTextField txtSaldo;

    // Footer badges / botones
    private JLabel lblDocImpreso;
    private JButton btnDocOrigen;
    private JButton btnDetalle;
    private JButton btnImprimir;

    // ── Componentes Pestaña Adicionales ──
    private JTextField txtCampo1, txtCampo2, txtCampo3, txtCampo4;
    private JTextField txtCampo5, txtCampo6, txtCampo7, txtCampo8;
    private JTextField txtUsIn, txtFeIn, txtSucuIn;
    private JTextField txtUsMo, txtFeMo, txtSucuMo;

    public DocumentoVentaFrame() {
        this("0000007047", "N/CR");
    }

    public DocumentoVentaFrame(String nroDocInicial, String coTipoDocInicial) {
        this.dao = new NotaCreditoDAO();

        setTitle("Documento de Venta — DroAI");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(940, 720);
        setMinimumSize(new Dimension(860, 640));
        setLocationRelativeTo(null);

        IconHelper.applyAppIcon(this);
        Toast.setParentFrame(this);

        buildUI();

        // Cargar documento inicial
        cargarDocumento(nroDocInicial, coTipoDocInicial);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(tm.background());

        // ═══════════════════════════════════════════════════════════
        // 1. TOOLBAR SUPERIOR (Navegación + Acciones)
        // ═══════════════════════════════════════════════════════════
        JPanel toolbar = new JPanel(new MigLayout("insets 8 16 8 16, fillx", "[]4[]4[]4[]16[]8[]push[]8[]", "[]"));
        toolbar.setBackground(tm.cardBg());
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, tm.border()));

        JButton btnPrimero = createNavButton("|<", "Primer Documento", this::irAlPrimero);
        JButton btnAnterior = createNavButton("<", "Documento Anterior", this::irAlAnterior);
        JButton btnSiguiente = createNavButton(">", "Documento Siguiente", this::irAlSiguiente);
        JButton btnUltimo = createNavButton(">|", "Último Documento", this::irAlUltimo);

        toolbar.add(btnPrimero);
        toolbar.add(btnAnterior);
        toolbar.add(btnSiguiente);
        toolbar.add(btnUltimo);

        JButton btnBuscar = new JButton("🔍 Buscar");
        btnBuscar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnBuscar.setBackground(tm.cardBg());
        btnBuscar.setForeground(tm.textPrimary());
        btnBuscar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnBuscar.addActionListener(e -> abrirDialogoBusqueda());
        toolbar.add(btnBuscar);

        JButton btnRefrescar = new JButton("🔄 Refrescar");
        btnRefrescar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRefrescar.setBackground(tm.cardBg());
        btnRefrescar.setForeground(tm.textPrimary());
        btnRefrescar.addActionListener(e -> {
            if (currentDoc != null) cargarDocumento(currentDoc.getNroDoc(), currentDoc.getCoTipoDoc());
        });
        toolbar.add(btnRefrescar);

        btnImprimir = new JButton("🖨️ Imprimir Formato");
        btnImprimir.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnImprimir.setBackground(tm.accent());
        btnImprimir.setForeground(tm.btnForegroundFor(tm.accent()));
        btnImprimir.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnImprimir.addActionListener(e -> abrirImpresion());
        toolbar.add(btnImprimir);

        root.add(toolbar, BorderLayout.NORTH);

        // ═══════════════════════════════════════════════════════════
        // 2. TABS: GENERAL Y ADICIONALES
        // ═══════════════════════════════════════════════════════════
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 12));

        tabbedPane.addTab("General", buildGeneralTab());
        tabbedPane.addTab("Adicionales", buildAdicionalesTab());

        root.add(tabbedPane, BorderLayout.CENTER);

        // ═══════════════════════════════════════════════════════════
        // 3. FOOTER ACCIONES E INDICADORES
        // ═══════════════════════════════════════════════════════════
        JPanel footer = new JPanel(new MigLayout("insets 10 20 10 20, fillx", "[]8[]8[]push[]16[]", "[]"));
        footer.setBackground(tm.cardBg());
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, tm.border()));

        btnDetalle = new JButton("Detalle");
        btnDetalle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnDetalle.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Documento financiero sin desglose de artículos físicos.",
                    "Detalle de Renglones", JOptionPane.INFORMATION_MESSAGE);
        });
        footer.add(btnDetalle);

        JButton btnAnular = new JButton("Anular");
        btnAnular.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnAnular.setEnabled(false); // Solo lectura / auditoría
        footer.add(btnAnular);

        JButton btnImagen = new JButton("Imagen");
        btnImagen.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnImagen.addActionListener(e -> Toast.show("Sin imágenes adjuntas en este documento", Toast.Type.INFO));
        footer.add(btnImagen);

        lblDocImpreso = new JLabel("Doc. impreso");
        lblDocImpreso.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblDocImpreso.setForeground(Color.GRAY);
        footer.add(lblDocImpreso);

        btnDocOrigen = new JButton("Doc.origen");
        btnDocOrigen.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnDocOrigen.addActionListener(e -> mostrarInfoDocOrigen());
        footer.add(btnDocOrigen);

        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel buildGeneralTab() {
        JPanel panel = new JPanel(new MigLayout("insets 14 20 14 20, fillx, wrap 6",
                "[right, 90!]8[120!, grow]8[right, 80!]8[150!, grow]8[right, 80!]8[160!, grow]",
                "[]6[]6[]6[]6[]6[]6[]6[]10[grow]"));
        panel.setBackground(tm.background());

        // Fila 1: Número, Estatus, Fecha, Tipo Mov
        panel.add(new JLabel("Número:"));
        txtNumero = createStyledTextField(true);
        panel.add(txtNumero, "growx");

        panel.add(new JLabel("Estatus:"));
        lblEstatus = new JLabel("Pendiente");
        lblEstatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblEstatus.setForeground(new Color(234, 179, 8)); // Amarillo pendiente
        panel.add(lblEstatus);

        panel.add(new JLabel("Fecha:"));
        txtFecha = createStyledTextField(true);
        panel.add(txtFecha, "growx");

        // Fila 2: Documento, Tipo mov
        panel.add(new JLabel("Documento:"));
        JPanel docPanel = new JPanel(new MigLayout("insets 0, gap 4, fillx", "[60!][grow]", "[]"));
        docPanel.setOpaque(false);
        txtTipoDoc = createStyledTextField(true);
        txtTipoDocDesc = createStyledTextField(true);
        docPanel.add(txtTipoDoc);
        docPanel.add(txtTipoDocDesc, "growx");
        panel.add(docPanel, "span 3, growx");

        panel.add(new JLabel("Tipo mov.:"));
        txtTipoMov = createStyledTextField(true);
        panel.add(txtTipoMov, "growx");

        // Fila 3: Cliente, Vencimiento
        panel.add(new JLabel("Cliente:"));
        JPanel cliPanel = new JPanel(new MigLayout("insets 0, gap 4, fillx", "[90!][grow]", "[]"));
        cliPanel.setOpaque(false);
        txtCodCliente = createStyledTextField(true);
        txtNomCliente = createStyledTextField(true);
        cliPanel.add(txtCodCliente);
        cliPanel.add(txtNomCliente, "growx");
        panel.add(cliPanel, "span 3, growx");

        panel.add(new JLabel("Venc.:"));
        txtVencimiento = createStyledTextField(true);
        panel.add(txtVencimiento, "growx");

        // Fila 4: Vendedor, Fecha reg
        panel.add(new JLabel("Vendedor:"));
        JPanel venPanel = new JPanel(new MigLayout("insets 0, gap 4, fillx", "[50!][grow]", "[]"));
        venPanel.setOpaque(false);
        txtCodVendedor = createStyledTextField(true);
        txtNomVendedor = createStyledTextField(true);
        venPanel.add(txtCodVendedor);
        venPanel.add(txtNomVendedor, "growx");
        panel.add(venPanel, "span 3, growx");

        panel.add(new JLabel("Fecha reg.:"));
        txtFechaReg = createStyledTextField(true);
        panel.add(txtFechaReg, "growx");

        // Fila 5: Descripción, N° control
        panel.add(new JLabel("Descripción:"));
        txtDescripcion = createStyledTextField(true);
        panel.add(txtDescripcion, "span 3, growx");

        panel.add(new JLabel("N° control:"));
        txtNControl = createStyledTextField(true);
        panel.add(txtNControl, "growx");

        // Fila 6: Doc. asoc., N° Registro, Fecha cheque
        panel.add(new JLabel("Doc. asoc.:"));
        JPanel asocPanel = new JPanel(new MigLayout("insets 0, gap 4, fillx", "[60!]12[right, 70!]4[grow]", "[]"));
        asocPanel.setOpaque(false);
        txtDocAsoc = createStyledTextField(true);
        txtNroRegistro = createStyledTextField(true);
        asocPanel.add(txtDocAsoc);
        asocPanel.add(new JLabel("N° Registro:"));
        asocPanel.add(txtNroRegistro, "growx");
        panel.add(asocPanel, "span 3, growx");

        panel.add(new JLabel("Fecha cheque:"));
        txtFechaCheque = createStyledTextField(true);
        panel.add(txtFechaCheque, "growx");

        // Fila 7: Banco, N° Cheque, Mov. Banco, Comprob. I.V.A.
        panel.add(new JLabel("Banco:"));
        txtBanco = createStyledTextField(true);
        panel.add(txtBanco, "growx");

        panel.add(new JLabel("N° cheque:"));
        JPanel chkPanel = new JPanel(new MigLayout("insets 0, gap 4, fillx", "[80!]8[right, 70!]4[grow]", "[]"));
        chkPanel.setOpaque(false);
        txtNroCheque = createStyledTextField(true);
        txtMovBanco = createStyledTextField(true);
        chkPanel.add(txtNroCheque);
        chkPanel.add(new JLabel("Mov. Banco:"));
        chkPanel.add(txtMovBanco, "growx");
        panel.add(chkPanel, "span 1, growx");

        panel.add(new JLabel("Comprob. IVA:"));
        txtComprobIva = createStyledTextField(true);
        panel.add(txtComprobIva, "growx");

        // Fila 8: Moneda, Tasa, Botón Conversión, I.V.A.
        panel.add(new JLabel("Moneda:"));
        JPanel monePanel = new JPanel(new MigLayout("insets 0, gap 4, fillx", "[50!][grow]4[100!]4[]", "[]"));
        monePanel.setOpaque(false);
        txtMoneda = createStyledTextField(true);
        txtMonedaDesc = createStyledTextField(true);
        txtTasa = createStyledTextField(true);
        txtTasa.setBackground(new Color(180, 240, 255)); // Tasa resaltada
        txtTasa.setForeground(Color.BLACK);
        txtTasa.setFont(new Font("Segoe UI", Font.BOLD, 11));

        btnConversion = new JButton("Conversión");
        btnConversion.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnConversion.setBackground(new Color(59, 130, 246));
        btnConversion.setForeground(Color.WHITE);
        btnConversion.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnConversion.addActionListener(e -> toggleConversionMoneda());

        monePanel.add(txtMoneda);
        monePanel.add(txtMonedaDesc, "growx");
        monePanel.add(txtTasa);
        monePanel.add(btnConversion);
        panel.add(monePanel, "span 3, growx");

        panel.add(new JLabel("I.V.A.:"));
        cmbIva = new JComboBox<>(new String[]{"Exentos", "General 16%", "Reducida", "Adicional"});
        cmbIva.setEnabled(false);
        panel.add(cmbIva, "growx");

        // ═══════════════════════════════════════════════════════════
        // SECCIÓN MONTOS DEL DOCUMENTO
        // ═══════════════════════════════════════════════════════════
        JPanel pnlMontos = new JPanel(new MigLayout("insets 12 16 12 16, fillx, wrap 4",
                "[right, 130!]8[140!, grow]24[right, 110!]8[140!, grow]",
                "[]6[]6[]6[]"));
        pnlMontos.setBackground(tm.cardBg());
        pnlMontos.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(tm.border(), 1),
                "Montos del Documento" + (modoUSD ? " (USD $)" : " (Bs.)"),
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                tm.accent()));

        // Fila 1 Montos
        pnlMontos.add(new JLabel("Monto bruto:"));
        txtMontoBruto = createAmountField();
        pnlMontos.add(txtMontoBruto, "growx");

        pnlMontos.add(new JLabel("I.V.A.:"));
        txtIva = createAmountField();
        pnlMontos.add(txtIva, "growx");

        // Fila 2 Montos
        pnlMontos.add(new JLabel("% Descuento:"));
        JPanel descPanel = new JPanel(new MigLayout("insets 0, gap 4, fillx", "[45!][grow]", "[]"));
        descPanel.setOpaque(false);
        txtPorcDesc = createStyledTextField(true);
        txtMontoDesc = createAmountField();
        descPanel.add(txtPorcDesc);
        descPanel.add(txtMontoDesc, "growx");
        pnlMontos.add(descPanel, "growx");

        pnlMontos.add(new JLabel("Otros:"));
        txtOtros = createAmountField();
        pnlMontos.add(txtOtros, "growx");

        // Fila 3 Montos
        pnlMontos.add(new JLabel("% Recargo:"));
        JPanel recaPanel = new JPanel(new MigLayout("insets 0, gap 4, fillx", "[45!][grow]", "[]"));
        recaPanel.setOpaque(false);
        txtPorcReca = createStyledTextField(true);
        txtMontoReca = createAmountField();
        recaPanel.add(txtPorcReca);
        recaPanel.add(txtMontoReca, "growx");
        pnlMontos.add(recaPanel, "growx");

        pnlMontos.add(new JLabel("Monto neto:"));
        txtMontoNeto = createAmountField();
        txtMontoNeto.setFont(new Font("Segoe UI", Font.BOLD, 12));
        pnlMontos.add(txtMontoNeto, "growx");

        // Fila 4 Montos
        pnlMontos.add(new JLabel("Total sin impuesto:"));
        txtTotalSinImpuesto = createAmountField();
        pnlMontos.add(txtTotalSinImpuesto, "growx");

        pnlMontos.add(new JLabel("Saldo:"));
        txtSaldo = createAmountField();
        txtSaldo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        pnlMontos.add(txtSaldo, "growx");

        panel.add(pnlMontos, "span 6, growx, gaptop 10");

        return panel;
    }

    private JPanel buildAdicionalesTab() {
        JPanel panel = new JPanel(new MigLayout("insets 16 24 16 24, fillx, wrap 4",
                "[right, 110!]8[grow]24[right, 110!]8[grow]",
                "[]8[]8[]8[]16[]8[]8[]"));
        panel.setBackground(tm.background());

        // Campos libres
        panel.add(new JLabel("Campo 1:"));
        txtCampo1 = createStyledTextField(true);
        panel.add(txtCampo1, "growx");

        panel.add(new JLabel("Campo 2:"));
        txtCampo2 = createStyledTextField(true);
        panel.add(txtCampo2, "growx");

        panel.add(new JLabel("Campo 3:"));
        txtCampo3 = createStyledTextField(true);
        panel.add(txtCampo3, "growx");

        panel.add(new JLabel("Campo 4:"));
        txtCampo4 = createStyledTextField(true);
        panel.add(txtCampo4, "growx");

        panel.add(new JLabel("Campo 5 (N° N/CR):"));
        txtCampo5 = createStyledTextField(true);
        txtCampo5.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panel.add(txtCampo5, "growx");

        panel.add(new JLabel("Campo 6:"));
        txtCampo6 = createStyledTextField(true);
        panel.add(txtCampo6, "growx");

        panel.add(new JLabel("Campo 7:"));
        txtCampo7 = createStyledTextField(true);
        panel.add(txtCampo7, "growx");

        panel.add(new JLabel("Campo 8:"));
        txtCampo8 = createStyledTextField(true);
        panel.add(txtCampo8, "growx");

        // Auditoría
        JPanel pnlAudit = new JPanel(new MigLayout("insets 12 16 12 16, fillx, wrap 6",
                "[right, 100!]8[grow]12[right, 80!]8[grow]12[right, 60!]8[60!]", "[]6[]"));
        pnlAudit.setBackground(tm.cardBg());
        pnlAudit.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(tm.border(), 1),
                "Información de Auditoría Profit",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                tm.accent()));

        pnlAudit.add(new JLabel("Usuario Inclusión:"));
        txtUsIn = createStyledTextField(true);
        pnlAudit.add(txtUsIn, "growx");

        pnlAudit.add(new JLabel("Fecha Inclusión:"));
        txtFeIn = createStyledTextField(true);
        pnlAudit.add(txtFeIn, "growx");

        pnlAudit.add(new JLabel("Sucursal:"));
        txtSucuIn = createStyledTextField(true);
        pnlAudit.add(txtSucuIn, "growx");

        pnlAudit.add(new JLabel("Usuario Modif.:"));
        txtUsMo = createStyledTextField(true);
        pnlAudit.add(txtUsMo, "growx");

        pnlAudit.add(new JLabel("Fecha Modif.:"));
        txtFeMo = createStyledTextField(true);
        pnlAudit.add(txtFeMo, "growx");

        pnlAudit.add(new JLabel("Sucursal:"));
        txtSucuMo = createStyledTextField(true);
        pnlAudit.add(txtSucuMo, "growx");

        panel.add(pnlAudit, "span 4, growx, gaptop 16");

        return panel;
    }

    // ═══════════════════════════════════════════════════════════
    // LÓGICA DE CARGA Y ACTUALIZACIÓN
    // ═══════════════════════════════════════════════════════════

    public void cargarDocumento(String nroDoc, String coTipoDoc) {
        new SwingWorker<NotaCreditoModel, Void>() {
            @Override
            protected NotaCreditoModel doInBackground() {
                return dao.consultarDocumento(nroDoc, coTipoDoc);
            }

            @Override
            protected void done() {
                try {
                    NotaCreditoModel doc = get();
                    if (doc != null) {
                        currentDoc = doc;
                        mostrarDocumentoEnPantalla();
                    } else {
                        Toast.show("No se encontró el documento " + nroDoc, Toast.Type.WARNING);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.show("Error al cargar documento: " + e.getMessage(), Toast.Type.ERROR);
                }
            }
        }.execute();
    }

    private void mostrarDocumentoEnPantalla() {
        if (currentDoc == null) return;

        txtNumero.setText(currentDoc.getNroDoc());
        lblEstatus.setText(currentDoc.getEstatus());

        // Color de estatus
        if ("Anulado".equalsIgnoreCase(currentDoc.getEstatus())) {
            lblEstatus.setForeground(new Color(239, 68, 68));
        } else if ("Cancelado".equalsIgnoreCase(currentDoc.getEstatus())) {
            lblEstatus.setForeground(new Color(34, 197, 94));
        } else {
            lblEstatus.setForeground(new Color(234, 179, 8));
        }

        txtFecha.setText(currentDoc.getFecEmis() != null ? currentDoc.getFecEmis().format(DTF_FECHA_HORA) : "");
        txtTipoDoc.setText(currentDoc.getCoTipoDoc());
        txtTipoDocDesc.setText(currentDoc.getTipoDocDesc());
        txtTipoMov.setText("CR".equalsIgnoreCase(currentDoc.getTipoMov()) ? "Crédito" : "Débito");

        txtCodCliente.setText(currentDoc.getCoCli());
        txtNomCliente.setText(currentDoc.getCliDes());
        txtCodVendedor.setText(currentDoc.getCoVen());
        txtNomVendedor.setText(currentDoc.getVenDes());

        txtVencimiento.setText(currentDoc.getFecVenc() != null ? currentDoc.getFecVenc().format(DTF_FECHA) : "");
        txtFechaReg.setText(currentDoc.getFecReg() != null ? currentDoc.getFecReg().format(DTF_FECHA) : "");
        txtDescripcion.setText(currentDoc.getDescripcion());
        txtNControl.setText(currentDoc.getNControl());

        String docAsoc = (currentDoc.getDocOrigRaw() != null && !currentDoc.getDocOrigRaw().isBlank()) 
                ? currentDoc.getDocOrigRaw() : currentDoc.getDocOrig();
        String nroReg = (currentDoc.getNroOrigRaw() != null && !currentDoc.getNroOrigRaw().isBlank()) 
                ? currentDoc.getNroOrigRaw() : currentDoc.getNroOrig();
        txtDocAsoc.setText(docAsoc);
        txtNroRegistro.setText(nroReg);
        txtFechaCheque.setText(currentDoc.getFecCheque() != null ? currentDoc.getFecCheque().format(DTF_FECHA) : "");
        txtBanco.setText(currentDoc.getBanco());
        txtNroCheque.setText(currentDoc.getNroCheque());
        txtMovBanco.setText(currentDoc.getMovBanco());
        txtComprobIva.setText(currentDoc.getComprobIva());

        txtMoneda.setText(currentDoc.getCoMone());
        txtMonedaDesc.setText(currentDoc.getMoneDes());
        txtTasa.setText(DFTasa.format(currentDoc.getTasa()));

        cmbIva.setSelectedItem("7".equals(currentDoc.getTipoImp()) ? "Exentos" : "General 16%");

        // Indicador de impreso
        if (currentDoc.isImpresa()) {
            lblDocImpreso.setForeground(new Color(34, 197, 94));
            lblDocImpreso.setText("✔ Doc. impreso");
        } else {
            lblDocImpreso.setForeground(Color.GRAY);
            lblDocImpreso.setText("Doc. no impreso");
        }

        // Actualizar Montos según modo (USD / Bs.)
        actualizarMontos();

        // Pestaña Adicionales
        txtCampo1.setText(currentDoc.getCampo1());
        txtCampo2.setText(currentDoc.getCampo2());
        txtCampo3.setText(currentDoc.getCampo3());
        txtCampo4.setText(currentDoc.getCampo4());
        txtCampo5.setText(currentDoc.getCampo5());
        txtCampo6.setText(currentDoc.getCampo6());
        txtCampo7.setText(currentDoc.getCampo7());
        txtCampo8.setText(currentDoc.getCampo8());

        txtUsIn.setText(currentDoc.getCoUsIn());
        txtFeIn.setText(currentDoc.getFeUsIn() != null ? currentDoc.getFeUsIn().format(DTF_FECHA_HORA) : "");
        txtSucuIn.setText(currentDoc.getCoSucuIn());

        txtUsMo.setText(currentDoc.getCoUsMo());
        txtFeMo.setText(currentDoc.getFeUsMo() != null ? currentDoc.getFeUsMo().format(DTF_FECHA_HORA) : "");
        txtSucuMo.setText(currentDoc.getCoSucuMo());
    }

    private void actualizarMontos() {
        if (currentDoc == null) return;

        double bruto = modoUSD ? currentDoc.getMontoBrutoUsd() : currentDoc.getMontoBrutoBs();
        double desc = modoUSD ? currentDoc.getMontoDescUsd() : currentDoc.getMontoDescBs();
        double reca = modoUSD ? currentDoc.getMontoRecaUsd() : currentDoc.getMontoRecaBs();
        double totalSinImp = modoUSD ? currentDoc.getTotalSinImpuestoUsd() : currentDoc.getTotalSinImpuestoBs();
        double iva = modoUSD ? currentDoc.getIvaUsd() : currentDoc.getIvaBs();
        double otros = modoUSD ? currentDoc.getOtrosUsd() : currentDoc.getOtrosBs();
        double neto = modoUSD ? currentDoc.getMontoNetoUsd() : currentDoc.getMontoNetoBs();
        double saldo = modoUSD ? currentDoc.getSaldoUsd() : currentDoc.getSaldoBs();

        txtMontoBruto.setText(DF.format(bruto));
        txtPorcDesc.setText(currentDoc.getPorcDescGlob() > 0 ? String.format("%.2f", currentDoc.getPorcDescGlob()) : "");
        txtMontoDesc.setText(DF.format(desc));

        txtPorcReca.setText(currentDoc.getPorcReca() > 0 ? String.format("%.2f", currentDoc.getPorcReca()) : "");
        txtMontoReca.setText(DF.format(reca));

        txtTotalSinImpuesto.setText(DF.format(totalSinImp));
        txtIva.setText(DF.format(iva));
        txtOtros.setText(DF.format(otros));
        txtMontoNeto.setText(DF.format(neto));
        txtSaldo.setText(DF.format(saldo));
    }

    private void toggleConversionMoneda() {
        modoUSD = !modoUSD;
        btnConversion.setText(modoUSD ? "Ver en Bs." : "Ver en USD");
        Toast.show("Modo de visualización: " + (modoUSD ? "Dólares Americanos (USD)" : "Bolívares (Bs.)"), Toast.Type.INFO);
        actualizarMontos();
    }

    private void mostrarInfoDocOrigen() {
        if (currentDoc == null || currentDoc.getNroOrig().isBlank()) {
            Toast.show("Este documento no tiene documento origen asociado", Toast.Type.WARNING);
            return;
        }

        String msg = """
            <html>
            <h3>Información del Documento Afectado</h3>
            <b>Tipo Doc:</b> %s<br>
            <b>Número:</b> %s<br>
            <b>N° Control:</b> %s<br>
            <b>Fecha Emisión:</b> %s<br>
            <b>Subtotal:</b> %s Bs.<br>
            <b>I.V.A.:</b> %s Bs.<br>
            <b>Total:</b> %s Bs.
            </html>
            """.formatted(
                currentDoc.getDocOrig(),
                currentDoc.getNroOrig(),
                currentDoc.getNControlOrig(),
                currentDoc.getFecEmisOrig() != null ? currentDoc.getFecEmisOrig().format(DTF_FECHA) : "N/D",
                DF.format(currentDoc.getSubtotalOrigBs()),
                DF.format(currentDoc.getIvaOrigBs()),
                DF.format(currentDoc.getTotalOrigBs())
        );

        JOptionPane.showMessageDialog(this, msg, "Documento Origen: " + currentDoc.getDocOrig() + " " + currentDoc.getNroOrig(),
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void abrirImpresion() {
        if (currentDoc == null) {
            Toast.show("Selecciona o carga un documento primero", Toast.Type.WARNING);
            return;
        }
        ImpresionNotaCreditoDialog dialog = new ImpresionNotaCreditoDialog(this, currentDoc, () -> {
            cargarDocumento(currentDoc.getNroDoc(), currentDoc.getCoTipoDoc());
        });
        dialog.setVisible(true);
    }

    // ── Navegación ──

    private void irAlPrimero() {
        String coTipo = currentDoc != null ? currentDoc.getCoTipoDoc() : "N/CR";
        String primerDoc = dao.obtenerPrimerNumero(coTipo);
        if (primerDoc != null) cargarDocumento(primerDoc, coTipo);
    }

    private void irAlUltimo() {
        String coTipo = currentDoc != null ? currentDoc.getCoTipoDoc() : "N/CR";
        String ultimoDoc = dao.obtenerUltimoNumero(coTipo);
        if (ultimoDoc != null) cargarDocumento(ultimoDoc, coTipo);
    }

    private void irAlAnterior() {
        if (currentDoc == null) return;
        String anterior = dao.obtenerAnteriorNumero(currentDoc.getNroDoc(), currentDoc.getCoTipoDoc());
        if (anterior != null) {
            cargarDocumento(anterior, currentDoc.getCoTipoDoc());
        } else {
            Toast.show("Este es el primer documento registrado", Toast.Type.INFO);
        }
    }

    private void irAlSiguiente() {
        if (currentDoc == null) return;
        String siguiente = dao.obtenerSiguienteNumero(currentDoc.getNroDoc(), currentDoc.getCoTipoDoc());
        if (siguiente != null) {
            cargarDocumento(siguiente, currentDoc.getCoTipoDoc());
        } else {
            Toast.show("Este es el último documento registrado", Toast.Type.INFO);
        }
    }

    private void abrirDialogoBusqueda() {
        String query = JOptionPane.showInputDialog(this,
                "Ingresa el N° de Documento, N° Control, Factura o Nombre de Cliente:",
                "Buscar Nota de Crédito / Documento",
                JOptionPane.QUESTION_MESSAGE);

        if (query != null && !query.isBlank()) {
            List<NotaCreditoModel> resultados = dao.buscarNotasCredito(query, 30);
            if (resultados.isEmpty()) {
                Toast.show("No se encontraron documentos con el criterio: " + query, Toast.Type.WARNING);
                return;
            }

            if (resultados.size() == 1) {
                NotaCreditoModel unico = resultados.get(0);
                cargarDocumento(unico.getNroDoc(), unico.getCoTipoDoc());
            } else {
                // Diálogo para elegir entre múltiples resultados
                String[] opciones = resultados.stream()
                        .map(r -> r.getNroDoc() + " (" + r.getNumeroImpresion() + ") | " + r.getCliDes() + " | " + DF.format(r.getMontoNetoUsd()) + " USD")
                        .toArray(String[]::new);

                String seleccionado = (String) JOptionPane.showInputDialog(this,
                        "Selecciona el documento:",
                        "Resultados de Búsqueda (" + resultados.size() + ")",
                        JOptionPane.PLAIN_MESSAGE, null, opciones, opciones[0]);

                if (seleccionado != null) {
                    for (int i = 0; i < opciones.length; i++) {
                        if (opciones[i].equals(seleccionado)) {
                            NotaCreditoModel elegido = resultados.get(i);
                            cargarDocumento(elegido.getNroDoc(), elegido.getCoTipoDoc());
                            break;
                        }
                    }
                }
            }
        }
    }

    // ── Helpers de UI ──

    private JButton createNavButton(String text, String tooltip, Runnable action) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setToolTipText(tooltip);
        btn.setBackground(tm.cardBg());
        btn.setForeground(tm.textPrimary());
        btn.setFocusPainted(false);
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private JTextField createStyledTextField(boolean readOnly) {
        JTextField tf = new JTextField();
        tf.setEditable(!readOnly);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tf.setBackground(readOnly ? tm.cardBg() : tm.bgField());
        tf.setForeground(tm.textPrimary());
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(tm.border(), 1),
                BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        return tf;
    }

    private JTextField createAmountField() {
        JTextField tf = new JTextField();
        tf.setEditable(false);
        tf.setHorizontalAlignment(SwingConstants.RIGHT);
        tf.setFont(new Font("Segoe UI", Font.BOLD, 12));
        // Color celeste suave como en Profit Plus
        tf.setBackground(new Color(180, 240, 255));
        tf.setForeground(Color.BLACK);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 200, 240), 1),
                BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        return tf;
    }
}
