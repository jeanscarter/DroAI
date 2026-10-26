package com.droai.ui;

import com.droai.dao.SiadPedidosDAO;
import com.droai.model.AuditoriaPedidoRow;
import com.droai.model.RastreoPedidoDTO;
import com.droai.model.SiadDiagnosticoDTO;
import com.droai.model.SiadDiagnosticoDTO.LoteFEFODTO;
import com.droai.model.SiadDiagnosticoDTO.RenglonDiagnosticoDTO;
import com.droai.ui.components.RoundedPanel;
import com.droai.ui.components.Toast;
import com.droai.ui.util.IconHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Ventana independiente ("sesión aparte") dedicada al monitoreo, rastreo puntual y
 * recuperación en 1-Clic de pedidos web de SIAD.
 * 
 * Diseñada para máxima agilidad en conexiones VPN:
 * - No recorre carpetas remotas masivas.
 * - Monitorea en vivo la bandeja de error (instantánea, 0 a 3 archivos).
 * - Búsqueda atómica directa por N° de pedido (ej: 105863) en milisegundos.
 */
public class MonitorPedidosSiadFrame extends JFrame {

    private static final DecimalFormat DF_MONTO;
    private static final DecimalFormat DF_CANT;
    private static final SimpleDateFormat SDF_FECHA = new SimpleDateFormat("dd/MM/yyyy hh:mm a");

    static {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.of("es", "VE"));
        sym.setDecimalSeparator(',');
        sym.setGroupingSeparator('.');
        DF_MONTO = new DecimalFormat("#,##0.00", sym);
        DF_CANT = new DecimalFormat("#,##0", sym);
    }

    private final ThemeManager tm = ThemeManager.get();
    private final SiadPedidosDAO siadDAO = new SiadPedidosDAO();

    // Componentes de Búsqueda
    private JTextField txtBuscarPedido;
    private JButton btnRastrear;
    private JButton btnRefrescarBandeja;
    private JLabel lblBadgeRebotadosCount;

    // Componentes del Panel de Diagnóstico de Pedido
    private JPanel panelDiagnosticoContainer;
    private JLabel lblPedIdTitulo;
    private JLabel lblEstadoBadge;
    private JLabel lblClienteInfo;
    private JLabel lblFechaInfo;
    private JLabel lblTotalesInfo;
    private JLabel lblErrorBanner;
    private RoundedPanel cardErrorBanner;
    private JLabel lblProfitInfo;

    // Pipeline Steps
    private JLabel lblStep1Web;
    private JLabel lblStep2Siad;
    private JLabel lblStep3Nota;
    private JLabel lblStep4Fact;

    // Acciones del Pedido Consultado
    private JButton btnResolverAsistente;
    private JButton btnReprocesarDirecto;

    // Tabla de Renglones del Pedido
    private DefaultTableModel modelRenglones;
    private JTable tblRenglones;

    // Tabla de Bandeja de Rebotados en Vivo
    private DefaultTableModel modelRebotados;
    private JTable tblRebotados;

    // Pedido actualmente cargado
    private RastreoPedidoDTO rastreoActual;

    public MonitorPedidosSiadFrame() {
        this(null);
    }

    public MonitorPedidosSiadFrame(String preloadedPedId) {
        setTitle("DroAI — Monitor y Recuperación de Pedidos SIAD");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1320, 840);
        setMinimumSize(new Dimension(1080, 680));
        setLocationRelativeTo(null);

        IconHelper.applyAppIcon(this);
        Toast.setParentFrame(this);

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
        cargarBandejaRebotadosAsync();

        if (preloadedPedId != null && !preloadedPedId.trim().isEmpty()) {
            txtBuscarPedido.setText(preloadedPedId.trim());
            ejecutarRastreoAsync(preloadedPedId.trim());
        }
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(tm.background());
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setPaint(new GradientPaint(0, 0, tm.gradientTop(), 0, 160, tm.gradientBottom()));
                g2.fillRect(0, 0, getWidth(), 160);
                g2.dispose();
            }
        };
        root.setOpaque(false);

        // ═══════════════════════════════════════════════════════════
        // HEADER + HERO SEARCH BAR
        // ═══════════════════════════════════════════════════════════
        JPanel topContainer = new JPanel(new MigLayout("insets 16 24 10 24, fillx, wrap", "[grow]", "[]10[]"));
        topContainer.setOpaque(false);

        topContainer.add(buildHeader(), "growx");
        topContainer.add(buildHeroSearchBar(), "growx");

        root.add(topContainer, BorderLayout.NORTH);

        // ═══════════════════════════════════════════════════════════
        // MAIN BODY: SPLIT VIEW (DIAGNÓSTICO ARRIBA, BANDEJA ABAJO)
        // ═══════════════════════════════════════════════════════════
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, buildPanelDiagnostico(), buildPanelBandeja());
        splitPane.setOpaque(false);
        splitPane.setBorder(new EmptyBorder(0, 24, 16, 24));
        splitPane.setResizeWeight(0.60);
        splitPane.setDividerSize(8);

        root.add(splitPane, BorderLayout.CENTER);

        setContentPane(root);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new MigLayout("insets 0, fillx", "[]12[]push[]", "[]"));
        p.setOpaque(false);

        JLabel lblIcon = new JLabel("⚡");
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        p.add(lblIcon);

        JPanel texts = new JPanel(new MigLayout("insets 0, wrap, gap 0", "[]", "[]2[]"));
        texts.setOpaque(false);

        JLabel lblTitle = new JLabel("Monitor y Recuperación de Pedidos SIAD");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(tm.textPrimary());
        texts.add(lblTitle);

        JLabel lblSub = new JLabel("Diagnóstico puntual de pedidos web, inspección de lotes/stock en Profit Plus y reenvío a SIAD");
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

    private JPanel buildHeroSearchBar() {
        RoundedPanel p = new RoundedPanel(12, true);
        p.setBackground(tm.cardBg());
        p.setLayout(new MigLayout("insets 12 18 12 18, fillx", "[]10[grow]12[]16[push]10[]", "[]"));

        JLabel lblPrompt = new JLabel("🛒 N° Pedido Web:");
        lblPrompt.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblPrompt.setForeground(tm.textPrimary());
        p.add(lblPrompt);

        txtBuscarPedido = new JTextField();
        txtBuscarPedido.setFont(new Font("Segoe UI", Font.BOLD, 14));
        txtBuscarPedido.putClientProperty("JTextField.placeholderText", "Ingresa N° de pedido (ej. 105863) y presiona Enter o clic en Rastrear...");
        txtBuscarPedido.addActionListener(e -> onBuscarClicked());
        p.add(txtBuscarPedido, "growx");

        btnRastrear = new JButton("🔍 Rastrear y Diagnosticar");
        btnRastrear.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRastrear.setBackground(tm.accent());
        btnRastrear.setForeground(tm.btnForegroundFor(tm.accent()));
        btnRastrear.setFocusPainted(false);
        btnRastrear.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRastrear.addActionListener(e -> onBuscarClicked());
        p.add(btnRastrear);

        lblBadgeRebotadosCount = new JLabel("Buscando...");
        lblBadgeRebotadosCount.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblBadgeRebotadosCount.setForeground(tm.textSecondary());
        p.add(lblBadgeRebotadosCount);

        btnRefrescarBandeja = new JButton("🔄 Actualizar Bandeja");
        btnRefrescarBandeja.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnRefrescarBandeja.setBackground(tm.cardBg());
        btnRefrescarBandeja.setForeground(tm.textPrimary());
        btnRefrescarBandeja.setFocusPainted(false);
        btnRefrescarBandeja.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRefrescarBandeja.addActionListener(e -> cargarBandejaRebotadosAsync());
        p.add(btnRefrescarBandeja);

        return p;
    }

    private JPanel buildPanelDiagnostico() {
        panelDiagnosticoContainer = new JPanel(new BorderLayout(0, 8));
        panelDiagnosticoContainer.setOpaque(false);

        RoundedPanel cardInfo = new RoundedPanel(12, true);
        cardInfo.setBackground(tm.cardBg());
        cardInfo.setLayout(new MigLayout("insets 14 18 12 18, fillx, wrap", "[grow]", "[]6[]8[]8[]"));

        // Línea 1: Título de Pedido + Estado Badge + Acciones
        JPanel rowHeader = new JPanel(new MigLayout("insets 0, fillx", "[]12[]push[]8[]", "[]"));
        rowHeader.setOpaque(false);

        lblPedIdTitulo = new JLabel("Seleccione o busque un pedido web para diagnosticar");
        lblPedIdTitulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblPedIdTitulo.setForeground(tm.textPrimary());
        rowHeader.add(lblPedIdTitulo);

        lblEstadoBadge = new JLabel(" SIN SELECCIÓN ");
        lblEstadoBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblEstadoBadge.setOpaque(true);
        lblEstadoBadge.setBackground(new Color(100, 116, 139));
        lblEstadoBadge.setForeground(Color.WHITE);
        lblEstadoBadge.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        rowHeader.add(lblEstadoBadge);

        btnResolverAsistente = new JButton("🔧 Solucionar con Asistente (FEFO / Nivelación)");
        btnResolverAsistente.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnResolverAsistente.setBackground(new Color(220, 38, 38));
        btnResolverAsistente.setForeground(Color.WHITE);
        btnResolverAsistente.setFocusPainted(false);
        btnResolverAsistente.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnResolverAsistente.setEnabled(false);
        btnResolverAsistente.addActionListener(e -> abrirAsistenteSolucion());
        rowHeader.add(btnResolverAsistente);

        btnReprocesarDirecto = new JButton("🚀 Enviar Directo a Cola SIAD");
        btnReprocesarDirecto.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnReprocesarDirecto.setBackground(tm.greenAccent());
        btnReprocesarDirecto.setForeground(tm.btnForegroundFor(tm.greenAccent()));
        btnReprocesarDirecto.setFocusPainted(false);
        btnReprocesarDirecto.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnReprocesarDirecto.setEnabled(false);
        btnReprocesarDirecto.addActionListener(e -> ejecutarReprocesamientoDirecto());
        rowHeader.add(btnReprocesarDirecto);

        cardInfo.add(rowHeader, "growx");

        // Línea 2: Pipeline de Estados
        JPanel rowPipeline = new JPanel(new MigLayout("insets 4 0 4 0, fillx, center", "[]16[]16[]16[]", "[]"));
        rowPipeline.setOpaque(false);

        lblStep1Web = createStepLabel("1. Archivo Web TXT", "⚪");
        lblStep2Siad = createStepLabel("2. Procesamiento SIAD", "⚪");
        lblStep3Nota = createStepLabel("3. Nota de Entrega Profit", "⚪");
        lblStep4Fact = createStepLabel("4. Facturación", "⚪");

        rowPipeline.add(lblStep1Web);
        rowPipeline.add(lblStep2Siad);
        rowPipeline.add(lblStep3Nota);
        rowPipeline.add(lblStep4Fact);

        cardInfo.add(rowPipeline, "growx");

        // Línea 3: Metadatos del Pedido
        JPanel rowMeta = new JPanel(new MigLayout("insets 0, fillx", "[]24[]24[]push[]", "[]"));
        rowMeta.setOpaque(false);

        lblClienteInfo = new JLabel("Cliente: —");
        lblClienteInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblClienteInfo.setForeground(tm.textPrimary());
        rowMeta.add(lblClienteInfo);

        lblFechaInfo = new JLabel("Fecha: —");
        lblFechaInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblFechaInfo.setForeground(tm.textSecondary());
        rowMeta.add(lblFechaInfo);

        lblTotalesInfo = new JLabel("Renglones: — | Unidades: —");
        lblTotalesInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblTotalesInfo.setForeground(tm.textSecondary());
        rowMeta.add(lblTotalesInfo);

        lblProfitInfo = new JLabel("Doc Profit: —");
        lblProfitInfo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblProfitInfo.setForeground(tm.tealAccent());
        rowMeta.add(lblProfitInfo);

        cardInfo.add(rowMeta, "growx");

        // Línea 4: Banner del Error Original (Visible cuando rebotó)
        cardErrorBanner = new RoundedPanel(8, false);
        cardErrorBanner.setBackground(tm.isDark() ? new Color(69, 10, 10) : new Color(254, 242, 242));
        cardErrorBanner.setLayout(new MigLayout("insets 8 12 8 12, fillx", "[]10[grow]", "[]"));
        cardErrorBanner.setVisible(false);

        JLabel lblIconErr = new JLabel("⚠️");
        lblIconErr.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        cardErrorBanner.add(lblIconErr);

        lblErrorBanner = new JLabel("Error de validación");
        lblErrorBanner.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblErrorBanner.setForeground(new Color(220, 38, 38));
        cardErrorBanner.add(lblErrorBanner, "growx");

        cardInfo.add(cardErrorBanner, "growx");

        panelDiagnosticoContainer.add(cardInfo, BorderLayout.NORTH);

        // Tabla de Renglones del Pedido
        panelDiagnosticoContainer.add(buildTablaRenglonesPanel(), BorderLayout.CENTER);

        return panelDiagnosticoContainer;
    }

    private JLabel createStepLabel(String text, String stateIcon) {
        JLabel l = new JLabel(stateIcon + " " + text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(tm.textSecondary());
        return l;
    }

    private JPanel buildTablaRenglonesPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        String[] cols = {
                "Reng#", "Código", "Descripción", "Cant Solicitada",
                "Lote Solicitado", "Stock Almacén 01", "Stock Lote",
                "Lote FEFO Sugerido", "Estado / Diagnóstico"
        };
        modelRenglones = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        tblRenglones = new JTable(modelRenglones);
        tblRenglones.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tblRenglones.setRowHeight(26);
        tblRenglones.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblRenglones.setShowGrid(false);
        tblRenglones.setIntercellSpacing(new Dimension(0, 0));

        JTableHeader th = tblRenglones.getTableHeader();
        th.setFont(new Font("Segoe UI", Font.BOLD, 11));
        th.setBackground(tm.cardBg());
        th.setForeground(tm.textPrimary());

        // Alineación derecha para números
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i : new int[]{0, 3, 5, 6}) {
            tblRenglones.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
        }

        tblRenglones.getColumnModel().getColumn(8).setCellRenderer(new RenglonEstadoRenderer());

        JScrollPane sp = new JScrollPane(tblRenglones);
        sp.setBorder(BorderFactory.createLineBorder(tm.border()));
        sp.getViewport().setBackground(tm.cardBg());
        p.add(sp, BorderLayout.CENTER);

        return p;
    }

    private JPanel buildPanelBandeja() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);

        JPanel headerBandeja = new JPanel(new MigLayout("insets 0 0 4 0, fillx", "[]push[]", "[]"));
        headerBandeja.setOpaque(false);

        JLabel lblSec = new JLabel("🚨 Bandeja de Pedidos Rebotados en SIAD (Tiempo Real)");
        lblSec.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblSec.setForeground(tm.textPrimary());
        headerBandeja.add(lblSec);

        JLabel lblTip = new JLabel("💡 Haz doble clic sobre un pedido para diagnosticarlo y resolverlo");
        lblTip.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblTip.setForeground(tm.textSecondary());
        headerBandeja.add(lblTip);

        p.add(headerBandeja, BorderLayout.NORTH);

        String[] cols = {
                "Pedido Web", "Fecha Emisión", "Cliente", "Renglones", "Monto ($)", "Causa del Rechazo SIAD"
        };
        modelRebotados = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        tblRebotados = new JTable(modelRebotados);
        tblRebotados.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tblRebotados.setRowHeight(26);
        tblRebotados.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblRebotados.setShowGrid(false);
        tblRebotados.setIntercellSpacing(new Dimension(0, 0));

        JTableHeader th = tblRebotados.getTableHeader();
        th.setFont(new Font("Segoe UI", Font.BOLD, 11));
        th.setBackground(tm.cardBg());
        th.setForeground(tm.textPrimary());

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tblRebotados.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);
        tblRebotados.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);

        // Doble clic para inspeccionar
        tblRebotados.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = tblRebotados.getSelectedRow();
                if (row >= 0) {
                    String pedId = (String) tblRebotados.getValueAt(row, 0);
                    txtBuscarPedido.setText(pedId);
                    if (e.getClickCount() == 2) {
                        ejecutarRastreoAsync(pedId);
                    } else {
                        ejecutarRastreoAsync(pedId);
                    }
                }
            }
        });

        JScrollPane sp = new JScrollPane(tblRebotados);
        sp.setBorder(BorderFactory.createLineBorder(tm.border()));
        sp.getViewport().setBackground(tm.cardBg());
        p.add(sp, BorderLayout.CENTER);

        return p;
    }

    private void onBuscarClicked() {
        String id = txtBuscarPedido.getText().trim();
        if (id.isEmpty()) {
            Toast.show("Por favor ingrese el número del pedido web", Toast.Type.WARNING);
            return;
        }
        ejecutarRastreoAsync(id);
    }

    /**
     * Rastrea un pedido específico en segundo plano con respuesta instantánea.
     */
    private void ejecutarRastreoAsync(String pedId) {
        btnRastrear.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SwingWorker<RastreoPedidoDTO, Void> worker = new SwingWorker<>() {
            @Override
            protected RastreoPedidoDTO doInBackground() {
                return siadDAO.rastrearPedidoCompleto(pedId);
            }

            @Override
            protected void done() {
                try {
                    rastreoActual = get();
                    mostrarDiagnostico(rastreoActual);
                } catch (Exception ex) {
                    Toast.show("Error rastreando pedido: " + ex.getMessage(), Toast.Type.ERROR);
                } finally {
                    btnRastrear.setEnabled(true);
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };
        worker.execute();
    }

    /**
     * Actualiza los componentes visuales con los resultados del rastreo.
     */
    private void mostrarDiagnostico(RastreoPedidoDTO dto) {
        if (dto == null) {
            lblPedIdTitulo.setText("Pedido no localizado");
            lblEstadoBadge.setText(" NO ENCONTRADO ");
            lblEstadoBadge.setBackground(new Color(100, 116, 139));
            btnResolverAsistente.setEnabled(false);
            btnReprocesarDirecto.setEnabled(false);
            cardErrorBanner.setVisible(false);
            modelRenglones.setRowCount(0);
            return;
        }

        lblPedIdTitulo.setText("Pedido Web #" + dto.getPedidoId());

        // Configurar Badge y Pasos
        switch (dto.getEstadoGeneral()) {
            case "REBOTADO_SIAD" -> {
                lblEstadoBadge.setText(" 🚨 REBOTADO POR SIAD ");
                lblEstadoBadge.setBackground(new Color(220, 38, 38));
                lblStep1Web.setText("🟢 1. Archivo Web TXT");
                lblStep2Siad.setText("❌ 2. Falló en SIAD");
                lblStep3Nota.setText("⚪ 3. Sin Nota");
                lblStep4Fact.setText("⚪ 4. Sin Factura");
                btnResolverAsistente.setEnabled(dto.getArchivoTxt() != null);
                btnReprocesarDirecto.setEnabled(dto.getArchivoTxt() != null);

                cardErrorBanner.setVisible(true);
                lblErrorBanner.setText(dto.getMensajeErrorSiad() != null ? dto.getMensajeErrorSiad() : "Fallo de stock/lotes en SIAD");
            }
            case "EN_COLA_SIAD" -> {
                lblEstadoBadge.setText(" ⏳ EN COLA ACTIVA SIAD ");
                lblEstadoBadge.setBackground(new Color(234, 179, 8));
                lblStep1Web.setText("🟢 1. Archivo Web TXT");
                lblStep2Siad.setText("⏳ 2. En Cola de Importación");
                lblStep3Nota.setText("⚪ 3. Pendiente");
                lblStep4Fact.setText("⚪ 4. Pendiente");
                btnResolverAsistente.setEnabled(false);
                btnReprocesarDirecto.setEnabled(false);
                cardErrorBanner.setVisible(false);
            }
            case "PROCESADO_PROFIT" -> {
                lblEstadoBadge.setText(" 🟢 PROCESADO EN PROFIT ");
                lblEstadoBadge.setBackground(new Color(16, 185, 129));
                lblStep1Web.setText("🟢 1. Archivo Web TXT");
                lblStep2Siad.setText("🟢 2. Procesado SIAD");
                lblStep3Nota.setText("🟢 3. Nota: " + dto.getDocNumNota());
                lblStep4Fact.setText(dto.getFacturasProfit() != null && !dto.getFacturasProfit().isEmpty()
                        ? "🟢 4. Factura(s): " + dto.getFacturasProfit()
                        : "🟡 4. Pendiente Facturar");
                btnResolverAsistente.setEnabled(false);
                btnReprocesarDirecto.setEnabled(false);
                cardErrorBanner.setVisible(false);
            }
            case "PROCESADO_SIAD" -> {
                lblEstadoBadge.setText(" 🟢 PROCESADO POR SIAD ");
                lblEstadoBadge.setBackground(new Color(16, 185, 129));
                lblStep1Web.setText("🟢 1. Archivo Web TXT");
                lblStep2Siad.setText("🟢 2. Procesado SIAD");
                lblStep3Nota.setText("🟡 3. Nota Generada");
                lblStep4Fact.setText("⚪ 4. Facturación");
                btnResolverAsistente.setEnabled(false);
                btnReprocesarDirecto.setEnabled(false);
                cardErrorBanner.setVisible(false);
            }
            default -> {
                lblEstadoBadge.setText(" ❓ NO ENCONTRADO ");
                lblEstadoBadge.setBackground(new Color(100, 116, 139));
                lblStep1Web.setText("⚪ 1. Archivo Web");
                lblStep2Siad.setText("⚪ 2. SIAD");
                lblStep3Nota.setText("⚪ 3. Nota");
                lblStep4Fact.setText("⚪ 4. Factura");
                btnResolverAsistente.setEnabled(false);
                btnReprocesarDirecto.setEnabled(false);
                cardErrorBanner.setVisible(false);
            }
        }

        // Metadatos
        lblClienteInfo.setText("Cliente: " + (dto.getCoCli() != null ? dto.getCoCli() + " - " + (dto.getCliDes() != null ? dto.getCliDes() : "") : "—"));
        lblFechaInfo.setText("Fecha: " + (dto.getFechaEmisionTxt() != null ? dto.getFechaEmisionTxt() : (dto.getFecEmisNota() != null ? SDF_FECHA.format(dto.getFecEmisNota()) : "—")));
        lblTotalesInfo.setText("Renglones: " + dto.getTotalRenglonesTxt() + " | Unidades: " + DF_CANT.format(dto.getTotalUnidadesTxt()));

        if (dto.isExisteEnProfit()) {
            String profitTxt = "Nota: " + dto.getDocNumNota();
            if (dto.getFacturasProfit() != null && !dto.getFacturasProfit().isEmpty()) {
                profitTxt += " | Facturas: " + dto.getFacturasProfit();
            }
            lblProfitInfo.setText("Doc Profit: " + profitTxt);
        } else {
            lblProfitInfo.setText("Doc Profit: No registrado aún");
        }

        // Renglones
        modelRenglones.setRowCount(0);
        if (dto.getDiagnostico() != null && dto.getDiagnostico().getRenglones() != null) {
            for (RenglonDiagnosticoDTO r : dto.getDiagnostico().getRenglones()) {
                LoteFEFODTO fefo = r.getMejorLoteFEFO();
                String fefoSugerido = (fefo != null) ? fefo.getNumeroLote() + " (Disp: " + DF_CANT.format(fefo.getCantDisponible()) + ")" : "Ninguno";
                String estadoStr;
                if (!r.isTieneError()) {
                    estadoStr = "🟢 OK (Disponible)";
                } else if ("LOTE_AGOTADO_HAY_FEFO".equals(r.getTipoError())) {
                    estadoStr = "🟠 Lote Agotado (Hay FEFO)";
                } else {
                    estadoStr = "🔴 " + r.getMensajeError();
                }

                modelRenglones.addRow(new Object[]{
                        r.getRengNum(),
                        r.getCoArt(),
                        r.getArtDes(),
                        DF_CANT.format(r.getCantSolicitada()),
                        r.getLoteSolicitado(),
                        DF_CANT.format(r.getStockAlmacen()),
                        DF_CANT.format(r.getStockLoteSolicitado()),
                        fefoSugerido,
                        estadoStr
                });
            }
        }
    }

    /**
     * Carga la bandeja de pedidos rebotados de SIAD en segundo plano (sólo carpeta error).
     */
    private void cargarBandejaRebotadosAsync() {
        lblBadgeRebotadosCount.setText("Consultando SIAD...");

        SwingWorker<List<AuditoriaPedidoRow>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<AuditoriaPedidoRow> doInBackground() {
                return siadDAO.listarPedidosEnError(null, null);
            }

            @Override
            protected void done() {
                try {
                    List<AuditoriaPedidoRow> lista = get();
                    modelRebotados.setRowCount(0);
                    for (AuditoriaPedidoRow r : lista) {
                        modelRebotados.addRow(new Object[]{
                                r.getPedidoWeb(),
                                r.getFecEmis() != null ? SDF_FECHA.format(r.getFecEmis()) : "—",
                                (r.getCoCli() != null ? r.getCoCli() : "") + " " + (r.getCliDes() != null ? r.getCliDes() : ""),
                                r.getTotalRengWeb(),
                                "$ " + DF_MONTO.format(r.getTotalMontoNota()),
                                r.getDiagnostico()
                        });
                    }

                    if (lista.isEmpty()) {
                        lblBadgeRebotadosCount.setText("🟢 0 Pedidos Rebotados en SIAD");
                        lblBadgeRebotadosCount.setForeground(new Color(16, 185, 129));
                    } else {
                        lblBadgeRebotadosCount.setText("🚨 " + lista.size() + " Pedido(s) Rebotado(s) en SIAD");
                        lblBadgeRebotadosCount.setForeground(new Color(220, 38, 38));
                    }
                } catch (Exception e) {
                    lblBadgeRebotadosCount.setText("Error leyendo bandeja");
                }
            }
        };
        worker.execute();
    }

    /**
     * Abre el asistente interactivo de solución para el pedido actual.
     */
    private void abrirAsistenteSolucion() {
        if (rastreoActual == null || rastreoActual.getArchivoTxt() == null) {
            Toast.show("No hay archivo de pedido rebotado seleccionado", Toast.Type.WARNING);
            return;
        }

        ResolverPedidoSiadDialog dlg = new ResolverPedidoSiadDialog(this, rastreoActual.getArchivoTxt(), () -> {
            Toast.show("¡Pedido reprocesado exitosamente!", Toast.Type.SUCCESS);
            cargarBandejaRebotadosAsync();
            ejecutarRastreoAsync(rastreoActual.getPedidoId());
        });
        dlg.setVisible(true);
    }

    /**
     * Reprocesa directamente el pedido a la cola activa.
     */
    private void ejecutarReprocesamientoDirecto() {
        if (rastreoActual == null || rastreoActual.getArchivoTxt() == null) return;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "¿Desea enviar el archivo directamente a la cola de importación de SIAD?",
                "Confirmar Reprocesamiento",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        boolean ok = siadDAO.reprocesarPedidoSiad(rastreoActual.getArchivoTxt());
        setCursor(Cursor.getDefaultCursor());

        if (ok) {
            Toast.show("Pedido movido a la cola activa de SIAD", Toast.Type.SUCCESS);
            cargarBandejaRebotadosAsync();
            ejecutarRastreoAsync(rastreoActual.getPedidoId());
        } else {
            Toast.show("No se pudo mover el archivo a la cola", Toast.Type.ERROR);
        }
    }

    /**
     * Renderer semáforo para la columna de estado del renglón.
     */
    private static class RenglonEstadoRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            String valStr = value != null ? value.toString() : "";

            if (!isSelected) {
                if (valStr.startsWith("🟢")) {
                    c.setForeground(new Color(16, 185, 129));
                } else if (valStr.startsWith("🟠")) {
                    c.setForeground(new Color(234, 88, 12));
                } else if (valStr.startsWith("🔴")) {
                    c.setForeground(new Color(220, 38, 38));
                }
            }
            return c;
        }
    }
}
