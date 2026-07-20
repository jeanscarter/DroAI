package com.droai.ui;

import com.droai.model.SesionUsuario;
import com.droai.ui.components.RoundedPanel;
import com.droai.ui.components.Toast;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Dashboard Administrativo Principal — nuevo punto de entrada tras el login.
 *
 * <p>Presenta tarjetas de navegación hacia los módulos del sistema:
 * <ul>
 *   <li>Gestión de Precios y Descuentos ({@link MainFrame})</li>
 *   <li>Monitor Situacional / Reportes de Ventas ({@link MonitorSituacionalFrame})</li>
 *   <li>Auditoría y Usuarios (placeholder para módulos futuros)</li>
 * </ul>
 *
 * <p>Hereda la paleta de colores del sistema FlatLaf Dark configurada en {@code App.java}.
 */
public class AdminDashboardFrame extends JFrame {

    // ── Paleta heredada del sistema ──
    private static final Color BG_DARK       = new Color(17, 21, 28);
    private static final Color CARD_BG       = new Color(30, 35, 46);
    private static final Color CARD_HOVER    = new Color(38, 44, 58);
    private static final Color ACCENT        = new Color(42, 107, 255);
    private static final Color GREEN_ACCENT  = new Color(0, 210, 158);
    private static final Color ORANGE_ACCENT = new Color(245, 158, 11);
    private static final Color TEXT_PRIMARY  = new Color(248, 250, 252);
    private static final Color TEXT_SECONDARY = new Color(148, 163, 184);

    public AdminDashboardFrame() {
        setTitle("DroAI — Menú Principal Administrativo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/images/logo.png"));
            setIconImage(icon.getImage());
        } catch (Exception ignored) {}

        Toast.setParentFrame(this);

        // ── Root panel con gradiente ──
        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_DARK);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Gradiente superior sutil
                g2.setPaint(new GradientPaint(0, 0, new Color(42, 107, 255, 25),
                        0, 220, new Color(42, 107, 255, 0)));
                g2.fillRect(0, 0, getWidth(), 220);
                g2.dispose();
            }
        };
        root.setOpaque(false);

        // ═══════════════════════════════════════════════════════════
        //  HEADER
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
        lblTitle.setForeground(TEXT_PRIMARY);
        titleTexts.add(lblTitle);

        JLabel lblSubtitle = new JLabel("DroAI — Sistema de Gestión Integral");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtitle.setForeground(TEXT_SECONDARY);
        titleTexts.add(lblSubtitle);

        titleGroup.add(titleTexts);
        header.add(titleGroup);

        // Info de sesión
        JPanel sessionInfo = new JPanel(new MigLayout("insets 0, wrap, gap 0, alignx right", "[right]", "[]2[]2[]"));
        sessionInfo.setOpaque(false);
        if (SesionUsuario.isAutenticado()) {
            SesionUsuario s = SesionUsuario.current();
            JLabel lblUser = new JLabel("👤 " + s.getNombreUsuario() + " (" + s.getCoUsuario() + ")");
            lblUser.setFont(new Font("Segoe UI Emoji", Font.BOLD, 12));
            lblUser.setForeground(TEXT_PRIMARY);
            sessionInfo.add(lblUser);

            JLabel lblNivel = new JLabel(s.getNivelDescripcion());
            lblNivel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lblNivel.setForeground(GREEN_ACCENT);
            sessionInfo.add(lblNivel);

            JLabel lblMaquina = new JLabel("🖥 " + s.getMaquina());
            lblMaquina.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 10));
            lblMaquina.setForeground(TEXT_SECONDARY);
            sessionInfo.add(lblMaquina);
        }
        header.add(sessionInfo);

        root.add(header, BorderLayout.NORTH);

        // ═══════════════════════════════════════════════════════════
        //  CONTENT — Cards Grid
        // ═══════════════════════════════════════════════════════════
        JPanel content = new JPanel(new MigLayout(
                "insets 40 60 40 60, gap 28, center, wrap 3",
                "[grow, 280:320:]" .repeat(3).replaceFirst(",\\s*$", ""),
                "[grow, 280:320:]"));
        content.setOpaque(false);

        // ── Tarjeta 1: Gestión de Precios ──
        content.add(createModuleCard(
                "📊",
                "Gestión de Precios y Descuentos",
                "Listado de precios, Descuentos por Volumen (DV),\nDescuento por Producto (DP/DA), Importación masiva.",
                ACCENT,
                this::abrirGestionPrecios
        ), "grow");

        // ── Tarjeta 2: Monitor Situacional ──
        content.add(createModuleCard(
                "📈",
                "Monitor Situacional",
                "Reportes de ventas, indicadores KPI,\nagrupaciones por alícuota y exportación.",
                GREEN_ACCENT,
                this::abrirMonitorSituacional
        ), "grow");

        // ── Tarjeta 3: Auditoría y Usuarios ──
        content.add(createModuleCard(
                "👥",
                "Auditoría y Usuarios",
                "Gestión de usuarios, control de acceso,\nhistorial de operaciones. (Próximamente)",
                ORANGE_ACCENT,
                null // Placeholder — módulo futuro
        ), "grow");

        root.add(content, BorderLayout.CENTER);

        // ═══════════════════════════════════════════════════════════
        //  FOOTER
        // ═══════════════════════════════════════════════════════════
        JPanel footer = new JPanel(new MigLayout("insets 12 32 12 32, fillx", "[]push[]", "[]"));
        footer.setOpaque(false);

        JLabel lblVersion = new JLabel("DroAI v1.0 — Sistema Administrativo");
        lblVersion.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblVersion.setForeground(TEXT_SECONDARY);
        footer.add(lblVersion);

        if (SesionUsuario.isAutenticado()) {
            JLabel lblFooterMachine = new JLabel("Estación: " + SesionUsuario.current().getMaquina());
            lblFooterMachine.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            lblFooterMachine.setForeground(TEXT_SECONDARY);
            footer.add(lblFooterMachine);
        }

        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Creación de tarjetas de módulo
    // ═══════════════════════════════════════════════════════════════

    /**
     * Crea una tarjeta de módulo con icono, título, descripción y acción de clic.
     */
    private JPanel createModuleCard(String icon, String title, String description,
                                     Color accentColor, Runnable onClickAction) {
        RoundedPanel card = new RoundedPanel(16, true);
        card.setBackground(CARD_BG);
        card.setLayout(new MigLayout("insets 28 24 24 24, wrap, gap 8", "[grow, center]", ""));
        card.setCursor(onClickAction != null
                ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                : Cursor.getDefaultCursor());

        // Borde inferior de color acento
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 3, 0, accentColor),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        // Ícono grande
        JLabel lblIcon = new JLabel(icon);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(lblIcon, "center, gapbottom 8");

        // Título
        JLabel lblTitle = new JLabel("<html><center>" + title + "</center></html>");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(TEXT_PRIMARY);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(lblTitle, "center, gapbottom 4");

        // Descripción
        JLabel lblDesc = new JLabel("<html><center>" + description.replace("\n", "<br>") + "</center></html>");
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblDesc.setForeground(TEXT_SECONDARY);
        lblDesc.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(lblDesc, "center, gapbottom 16");

        // Botón de acción
        if (onClickAction != null) {
            JButton btnAcceder = new JButton("Acceder →");
            btnAcceder.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btnAcceder.setBackground(accentColor);
            btnAcceder.setForeground(accentColor.equals(GREEN_ACCENT) || accentColor.equals(ORANGE_ACCENT)
                    ? new Color(17, 21, 28) : Color.WHITE);
            btnAcceder.setFocusPainted(false);
            btnAcceder.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnAcceder.setBorder(BorderFactory.createEmptyBorder(10, 28, 10, 28));
            btnAcceder.addActionListener(e -> onClickAction.run());
            card.add(btnAcceder, "center");
        } else {
            JLabel lblProximamente = new JLabel("Próximamente");
            lblProximamente.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            lblProximamente.setForeground(new Color(100, 110, 130));
            card.add(lblProximamente, "center");
        }

        // Hover effect
        if (onClickAction != null) {
            card.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    card.setBackground(CARD_HOVER);
                    card.repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    card.setBackground(CARD_BG);
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
    //  Acciones de navegación
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
}
