package com.droai.ui;

import com.droai.model.SesionUsuario;
import com.droai.ui.components.RoundedPanel;
import com.droai.ui.components.Toast;
import com.droai.ui.util.IconHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Dashboard Administrativo Principal — punto de entrada tras la autenticación.
 *
 * <p>
 * Presenta tarjetas de navegación hacia los módulos del sistema:
 * <ul>
 * <li>Gestión de Precios y Descuentos ({@link MainFrame})</li>
 * <li>Monitor Situacional / Reportes de Ventas
 * ({@link MonitorSituacionalFrame})</li>
 * <li>Auditoría y Usuarios (placeholder para módulos futuros)</li>
 * </ul>
 *
 * <p>
 * Colores gestionados dinámicamente por {@link ThemeManager} para soportar
 * tema claro/oscuro.
 */
public class AdminDashboardFrame extends JFrame {

    private final ThemeManager tm = ThemeManager.get();

    // ── Listener para repintar al cambiar tema ──
    private final Runnable themeListener;

    public AdminDashboardFrame() {
        setTitle("DroAI — Menú Principal Administrativo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1220, 750);
        setMinimumSize(new Dimension(980, 640));
        setLocationRelativeTo(null);

        IconHelper.applyAppIcon(this);

        Toast.setParentFrame(this);

        buildUI();

        // Registrar listener de tema
        themeListener = this::rebuildUI;
        tm.addThemeChangeListener(themeListener);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                tm.removeThemeChangeListener(themeListener);
            }
        });
    }

    private void rebuildUI() {
        buildUI();
        revalidate();
        repaint();
    }

    private void buildUI() {
        // ── Root panel con gradiente ──
        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(tm.background());
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Gradiente superior sutil
                g2.setPaint(new GradientPaint(0, 0, tm.gradientTop(),
                        0, 220, tm.gradientBottom()));
                g2.fillRect(0, 0, getWidth(), 220);
                g2.dispose();
            }
        };
        root.setOpaque(false);

        // ═══════════════════════════════════════════════════════════
        // HEADER
        // ═══════════════════════════════════════════════════════════
        JPanel header = new JPanel(new MigLayout(
                "insets 20 32 12 32, fillx", "[grow]push[]", "[]4[]"));
        header.setOpaque(false);

        // Logo + Título
        JPanel titleGroup = new JPanel(new MigLayout("insets 0, gap 12", "[][]", "[]"));
        titleGroup.setOpaque(false);

        JLabel lblLogo = new JLabel();
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/images/logo.png"));
            Image scaled = icon.getImage().getScaledInstance(44, 44, Image.SCALE_SMOOTH);
            lblLogo.setIcon(new ImageIcon(scaled));
        } catch (Exception e) {
            lblLogo.setText("🧪");
            lblLogo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        }
        titleGroup.add(lblLogo);

        JPanel titleTexts = new JPanel(new MigLayout("insets 0, wrap, gap 0", "[]", "[]2[]"));
        titleTexts.setOpaque(false);
        JLabel lblTitle = new JLabel("Menú Principal Administrativo");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(tm.textPrimary());
        titleTexts.add(lblTitle);

        JLabel lblSubtitle = new JLabel("DroAI — Sistema de Gestión Integral");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtitle.setForeground(tm.textSecondary());
        titleTexts.add(lblSubtitle);

        titleGroup.add(titleTexts);
        header.add(titleGroup);

        // Info de sesión + Botón de tema
        JPanel rightPanel = new JPanel(new MigLayout("insets 0, wrap, gap 0, alignx right", "[right]", "[]8[]"));
        rightPanel.setOpaque(false);

        // Botón de cambio de tema
        JButton btnTema = new JButton(tm.isDark() ? "☀ Claro" : "🌙 Oscuro");
        btnTema.setFont(new Font("Segoe UI Emoji", Font.BOLD, 11));
        btnTema.setFocusPainted(false);
        btnTema.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnTema.setBackground(tm.cardBg());
        btnTema.setForeground(tm.textPrimary());
        btnTema.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(tm.border(), 1),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        btnTema.addActionListener(e -> tm.toggleTheme());
        rightPanel.add(btnTema);

        JPanel sessionInfo = new JPanel(new MigLayout("insets 0, wrap, gap 0, alignx right", "[right]", "[]2[]2[]"));
        sessionInfo.setOpaque(false);
        if (SesionUsuario.isAutenticado()) {
            SesionUsuario s = SesionUsuario.current();
            JLabel lblUser = new JLabel("👤 " + s.getNombreUsuario() + " (" + s.getCoUsuario() + ")");
            lblUser.setFont(new Font("Segoe UI Emoji", Font.BOLD, 12));
            lblUser.setForeground(tm.textPrimary());
            sessionInfo.add(lblUser);

            JLabel lblNivel = new JLabel(s.getNivelDescripcion());
            lblNivel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lblNivel.setForeground(tm.greenAccent());
            sessionInfo.add(lblNivel);

            JLabel lblMaquina = new JLabel("🖥 " + s.getMaquina());
            lblMaquina.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 10));
            lblMaquina.setForeground(tm.textSecondary());
            sessionInfo.add(lblMaquina);
        }
        rightPanel.add(sessionInfo);

        header.add(rightPanel);

        root.add(header, BorderLayout.NORTH);

        // ═══════════════════════════════════════════════════════════
        // CONTENT — Cards Grid
        // ═══════════════════════════════════════════════════════════
        JPanel content = new JPanel(new MigLayout(
                "insets 16 32 16 32, gap 16, center, wrap 4",
                "[grow, 240:280:][grow, 240:280:][grow, 240:280:][grow, 240:280:]",
                "[grow, 200:240:][grow, 200:240:]"));
        content.setOpaque(false);

        // ── Tarjeta 1: Gestión de Precios ──
        content.add(createModuleCard(
                "📊",
                "Gestión de Precios y Descuentos",
                "Listado de precios, Descuentos por Volumen (DV),\nDescuento por Producto (DP/DA), Importación masiva.",
                tm.accent(),
                this::abrirGestionPrecios), "grow");

        // ── Tarjeta 2: Gestión Comercial ──
        content.add(createModuleCard(
                "💼",
                "Gestión Comercial",
                "Análisis de ventas por valores y unidades,\nfiltros por Mes, Vendedor, Zona, Proveedor y Cliente.",
                tm.tealAccent(),
                this::abrirGestionComercial), "grow");

        // ── Tarjeta 3: Monitor Situacional ──
        content.add(createModuleCard(
                "📈",
                "Monitor Situacional",
                "Reportes de ventas, indicadores KPI,\nagrupaciones por alícuota y exportación.",
                tm.greenAccent(),
                this::abrirMonitorSituacional), "grow");

        // ── Tarjeta 4: Ajustes de Inventario ──
        content.add(createModuleCard(
                "📦",
                "Ajustes de Inventario",
                "Ajustes de Entrada y Salida, stock total por almacén,\ndesglose por lote y fecha de vencimiento.",
                tm.tealAccent(),
                this::abrirAjustesInventario), "grow");

        // ── Tarjeta 5: Cálculo de Comisiones ──
        content.add(createModuleCard(
                "💰",
                "Cálculo de Comisiones",
                "Relación quincenal por vendedor, reglas por días calle,\nbase sin IVA y exportación oficial Excel.",
                tm.purpleAccent(),
                this::abrirCalculoComisiones), "grow");

        // ── Tarjeta 6: Cuentas por Cobrar (CxC) ──
        content.add(createModuleCard(
                "📋",
                "Cuentas por Cobrar (CxC)",
                "Estado de cuentas y antigüedad de saldos,\nreporte multimoneda USD, analistas y exportación.",
                tm.orangeAccent(),
                this::abrirCuentasPorCobrar), "grow");

        // ── Tarjeta 7: Notas de Crédito / Doc. Venta ──
        content.add(createModuleCard(
                "📄",
                "Notas de Crédito (Doc. Venta)",
                "Documentos de venta, consulta y anulación,\nconversión multimoneda e impresión fiscal.",
                tm.accent(),
                this::abrirNotasCredito), "grow");

        root.add(content, BorderLayout.CENTER);

        // ═══════════════════════════════════════════════════════════
        // FOOTER
        // ═══════════════════════════════════════════════════════════
        JPanel footer = new JPanel(new MigLayout("insets 12 32 12 32, fillx", "[]push[]", "[]"));
        footer.setOpaque(false);

        JLabel lblVersion = new JLabel("DroAI v1.0 — Sistema Administrativo");
        lblVersion.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblVersion.setForeground(tm.textSecondary());
        footer.add(lblVersion);

        if (SesionUsuario.isAutenticado()) {
            JLabel lblFooterMachine = new JLabel("Estación: " + SesionUsuario.current().getMaquina());
            lblFooterMachine.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            lblFooterMachine.setForeground(tm.textSecondary());
            footer.add(lblFooterMachine);
        }

        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    // ═══════════════════════════════════════════════════════════════
    // Creación de tarjetas de módulo
    // ═══════════════════════════════════════════════════════════════

    /**
     * Crea una tarjeta de módulo con icono, título, descripción y acción de clic.
     */
    private JPanel createModuleCard(String icon, String title, String description,
            Color accentColor, Runnable onClickAction) {
        RoundedPanel card = new RoundedPanel(16, true);
        card.setBackground(tm.cardBg());
        card.setLayout(new MigLayout("insets 28 24 24 24, wrap, gap 8", "[grow, center]", ""));
        card.setCursor(onClickAction != null
                ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                : Cursor.getDefaultCursor());

        // Borde inferior de color acento
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 3, 0, accentColor),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));

        // Ícono grande
        JLabel lblIcon = new JLabel(icon);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(lblIcon, "center, gapbottom 8");

        // Título
        JLabel lblTitle = new JLabel("<html><center>" + title + "</center></html>");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(tm.textPrimary());
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(lblTitle, "center, gapbottom 4");

        // Descripción
        JLabel lblDesc = new JLabel("<html><center>" + description.replace("\n", "<br>") + "</center></html>");
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblDesc.setForeground(tm.textSecondary());
        lblDesc.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(lblDesc, "center, gapbottom 16");

        // Botón de acción
        if (onClickAction != null) {
            JButton btnAcceder = new JButton("Acceder →");
            btnAcceder.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btnAcceder.setBackground(accentColor);
            btnAcceder.setForeground(tm.btnForegroundFor(accentColor));
            btnAcceder.setFocusPainted(false);
            btnAcceder.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnAcceder.setBorder(BorderFactory.createEmptyBorder(10, 28, 10, 28));
            btnAcceder.addActionListener(e -> onClickAction.run());
            card.add(btnAcceder, "center");
        } else {
            JLabel lblProximamente = new JLabel("Próximamente");
            lblProximamente.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            lblProximamente.setForeground(tm.textLabel());
            card.add(lblProximamente, "center");
        }

        // Hover effect
        if (onClickAction != null) {
            card.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    card.setBackground(tm.cardHover());
                    card.repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    card.setBackground(tm.cardBg());
                    card.repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    onClickAction.run();
                }
            });
        }

        return card;
    }

    // ═══════════════════════════════════════════════════════════════
    // Acciones de navegación
    // ═══════════════════════════════════════════════════════════════

    /**
     * Abre el módulo de Gestión de Precios y Descuentos (MainFrame existente).
     */
    private void abrirGestionPrecios() {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }

    /**
     * Abre el Monitor Situacional / Reportes de Ventas.
     */
    private void abrirMonitorSituacional() {
        SwingUtilities.invokeLater(() -> {
            MonitorSituacionalFrame frame = new MonitorSituacionalFrame();
            frame.setVisible(true);
        });
    }

    /**
     * Abre el módulo de Cálculo de Comisiones con protección por clave.
     */
    private void abrirCalculoComisiones() {
        com.droai.ui.dialog.ComisionesPasswordDialog dialog = new com.droai.ui.dialog.ComisionesPasswordDialog(this);
        dialog.setVisible(true);
        if (dialog.isAutenticado()) {
            SwingUtilities.invokeLater(() -> {
                ComisionesFrame frame = new ComisionesFrame();
                frame.setVisible(true);
            });
        }
    }

    /**
     * Abre el módulo de Ajustes de Inventario (Entrada y Salida).
     */
    private void abrirAjustesInventario() {
        SwingUtilities.invokeLater(() -> {
            AjusteInventarioFrame frame = new AjusteInventarioFrame();
            frame.setVisible(true);
        });
    }

    /**
     * Abre el módulo de Gestión Comercial.
     */
    private void abrirGestionComercial() {
        SwingUtilities.invokeLater(() -> {
            GestionComercialFrame frame = new GestionComercialFrame();
            frame.setVisible(true);
        });
    }

    /**
     * Abre el módulo de Estado de Cuentas por Cobrar (CxC).
     */
    private void abrirCuentasPorCobrar() {
        SwingUtilities.invokeLater(() -> {
            CxCDocumentoFrame frame = new CxCDocumentoFrame();
            frame.setVisible(true);
        });
    }

    /**
     * Usuarios autorizados para acceder a Notas de Crédito / Documentos de Venta.
     */
    private static final java.util.Set<String> USUARIOS_AUTORIZADOS_NCR = java.util.Set.of("JG", "CN", "OP", "JR", "ND", "FC");

    /**
     * Abre el módulo de Notas de Crédito / Documento de Venta con validación de usuario.
     */
    private void abrirNotasCredito() {
        if (!SesionUsuario.isAutenticado()) {
            Toast.show("Debes iniciar sesión para acceder a este módulo", Toast.Type.WARNING);
            return;
        }

        String coUsuario = SesionUsuario.current().getCoUsuario().trim().toUpperCase();
        if (!USUARIOS_AUTORIZADOS_NCR.contains(coUsuario)) {
            JOptionPane.showMessageDialog(this,
                    "<html><body style='width: 320px; font-family: sans-serif;'>"
                    + "<h3 style='color: #ef4444; margin-bottom: 8px;'>🔒 Acceso Restringido</h3>"
                    + "El usuario <b>" + coUsuario + "</b> no tiene autorización para acceder al módulo de Notas de Crédito.<br><br>"
                    + "<span style='color: #64748b;'>Usuarios autorizados:</span> <b>JG, CN, OP, JR, ND, FC</b>.</body></html>",
                    "Seguridad — Control de Acceso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            DocumentoVentaFrame frame = new DocumentoVentaFrame();
            frame.setVisible(true);
        });
    }
}
