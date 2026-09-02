package com.droai.ui.dialog;

import com.droai.model.SesionUsuario;
import com.droai.service.UsuarioService;
import com.droai.service.UsuarioService.AutenticacionException;
import com.droai.ui.util.IconHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Diálogo modal de autenticación contra Profit Plus (tabla {@code tusers}).
 *
 * <p>Campos reales de tusers:
 * <ul>
 *   <li>{@code codusu} → Código de usuario</li>
 *   <li>{@code clave}  → Contraseña</li>
 *   <li>{@code nombre} → Nombre completo</li>
 *   <li>{@code nivel}  → 0=AdminSys, 1=Supervisor, 2=Admin, 3=Operador, 4=Cajero</li>
 *   <li>{@code estatus} → '1' = activo</li>
 * </ul>
 *
 * <p>Diseño Raven-style con gradiente, bordes redondeados y feedback visual.
 */
public class LoginAuditoriaDialog extends JDialog {

    private static final Logger logger = LoggerFactory.getLogger(LoginAuditoriaDialog.class);

    // ── Paleta ──
    private static final Color BG_DARK        = new Color(17, 21, 28);
    private static final Color BG_FIELD       = new Color(38, 44, 58);
    private static final Color BORDER         = new Color(55, 62, 80);
    private static final Color ACCENT         = new Color(42, 107, 255);
    private static final Color TEXT_PRIMARY   = new Color(248, 250, 252);
    private static final Color TEXT_SECONDARY = new Color(148, 163, 184);
    private static final Color ERROR_RED      = new Color(220, 60, 60);
    private static final Color SUCCESS_GREEN  = new Color(0, 180, 130);

    private final JTextField txtUsuario;
    private final JPasswordField txtPassword;
    private final JButton btnLogin;
    private final JLabel lblEstado;
    private final JLabel lblSesionActual;
    private final UsuarioService usuarioService;

    private boolean autenticado = false;

    public LoginAuditoriaDialog(Frame owner) {
        super(owner, "Autenticación — Profit Plus", true);
        this.usuarioService = new UsuarioService();

        IconHelper.applyAppIcon(this);

        setSize(480, 460);
        setLocationRelativeTo(owner);
        setResizable(false);
        setUndecorated(true);

        // ── Root panel con gradiente ──
        JPanel root = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_DARK);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
                g2.setPaint(new GradientPaint(0, 0, new Color(42, 107, 255, 35),
                        0, 140, new Color(42, 107, 255, 0)));
                g2.fillRect(0, 0, getWidth(), 140);
                g2.dispose();
            }
        };
        root.setLayout(new MigLayout(
            "insets 28 36 24 36, fill, wrap",
            "[grow]",
            "[]12[]4[]20[]4[]12[]4[]20[]16[]push[]12[]"
        ));
        root.setOpaque(false);
        root.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));

        // ── Ícono + Título ──
        JLabel lblIcon = new JLabel();
        ImageIcon appLogo = IconHelper.getAppImageIcon(52, 52);
        if (appLogo != null) {
            lblIcon.setIcon(appLogo);
        } else {
            lblIcon.setText("🔐");
            lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
        }
        lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
        root.add(lblIcon, "center");

        JLabel lblTitle = new JLabel("Autenticación de Profit Plus");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 19));
        lblTitle.setForeground(TEXT_PRIMARY);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        root.add(lblTitle, "center");

        JLabel lblSub = new JLabel("Ingrese sus credenciales del sistema Profit Plus");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSub.setForeground(TEXT_SECONDARY);
        lblSub.setHorizontalAlignment(SwingConstants.CENTER);
        root.add(lblSub, "center, gapbottom 8");

        // ── Campo: Código de Usuario ──
        root.add(styledLabel("Código de Usuario"), "growx");

        txtUsuario = createStyledField();
        txtUsuario.putClientProperty("JTextField.placeholderText", "Ej: JG");
        txtUsuario.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) txtPassword.requestFocusInWindow();
            }
        });
        root.add(txtUsuario, "growx, h 40!");

        // ── Campo: Contraseña ──
        root.add(styledLabel("Contraseña"), "growx");

        txtPassword = new JPasswordField();
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtPassword.setBackground(BG_FIELD);
        txtPassword.setForeground(TEXT_PRIMARY);
        txtPassword.setCaretColor(ACCENT);
        txtPassword.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        txtPassword.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) intentarLogin();
            }
        });
        root.add(txtPassword, "growx, h 40!");

        // ── Botón Login ──
        btnLogin = new JButton("⚡  Iniciar Sesión");
        btnLogin.setFont(new Font("Segoe UI Emoji", Font.BOLD, 14));
        btnLogin.setBackground(ACCENT);
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogin.setBorder(BorderFactory.createEmptyBorder(12, 28, 12, 28));
        btnLogin.addActionListener(e -> intentarLogin());
        root.add(btnLogin, "growx, h 44!, gaptop 4");

        // ── Estado ──
        lblEstado = new JLabel(" ");
        lblEstado.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblEstado.setForeground(TEXT_SECONDARY);
        lblEstado.setHorizontalAlignment(SwingConstants.CENTER);
        root.add(lblEstado, "center");

        // ── Info de sesión previa ──
        lblSesionActual = new JLabel();
        lblSesionActual.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblSesionActual.setHorizontalAlignment(SwingConstants.CENTER);
        actualizarSesionLabel();
        root.add(lblSesionActual, "center");

        // ── Botón Salir ──
        JButton btnSalir = new JButton("✕  Cancelar y Salir");
        btnSalir.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 11));
        btnSalir.setBackground(new Color(55, 62, 80));
        btnSalir.setForeground(TEXT_SECONDARY);
        btnSalir.setFocusPainted(false);
        btnSalir.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSalir.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        btnSalir.addActionListener(e -> dispose());
        root.add(btnSalir, "center");

        setContentPane(root);
        SwingUtilities.invokeLater(() -> txtUsuario.requestFocusInWindow());
    }

    // ═══════════════════════════════════════════════════════════════
    //  Lógica de Login
    // ═══════════════════════════════════════════════════════════════

    private void intentarLogin() {
        String usuario = txtUsuario.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (usuario.isBlank()) {
            mostrarError("Ingrese el código de usuario.");
            txtUsuario.requestFocusInWindow();
            return;
        }
        if (password.isBlank()) {
            mostrarError("Ingrese la contraseña.");
            txtPassword.requestFocusInWindow();
            return;
        }

        btnLogin.setEnabled(false);
        lblEstado.setText("Validando credenciales...");
        lblEstado.setForeground(ACCENT);

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                logger.info("[Login] Intentando autenticar al usuario: {}", usuario);
                return usuarioService.autenticar(usuario, password);
            }

            @Override
            protected void done() {
                btnLogin.setEnabled(true);
                try {
                    boolean ok = get();
                    if (ok) {
                        autenticado = true;
                        SesionUsuario s = SesionUsuario.current();
                        String msgExito = "Bienvenido, %s — %s (Nivel: %s)".formatted(s.getNombreUsuario(), s.getCoUsuario(), s.getNivelDescripcion());
                        logger.info("[Login] ✔ Éxito: {}", msgExito);
                        lblEstado.setText("✔ " + msgExito);
                        lblEstado.setForeground(SUCCESS_GREEN);

                        Timer closeTimer = new Timer(1200, ev -> dispose());
                        closeTimer.setRepeats(false);
                        closeTimer.start();
                    }
                    } catch (Exception ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof AutenticacionException) {
                        logger.warn("[Login] ✘ Fallo de autenticación para '{}': {}", usuario, cause.getMessage());
                        mostrarError(cause.getMessage());
                    } else {
                        String errMsg = "Error de conexión: " + (cause != null ? cause.getMessage() : ex.getMessage());
                        logger.error("[Login] ✘ Error técnico al autenticar '{}': {}", usuario, errMsg, ex);
                        mostrarError(errMsg);
                    }
                    txtPassword.setText("");
                    txtPassword.requestFocusInWindow();
                }
            }
        }.execute();
    }

    private void mostrarError(String msg) {
        lblEstado.setText("✘ " + msg);
        lblEstado.setForeground(ERROR_RED);
    }

    private void actualizarSesionLabel() {
        if (SesionUsuario.isAutenticado()) {
            SesionUsuario s = SesionUsuario.current();
            lblSesionActual.setText("Sesión activa: %s (%s) — %s"
                    .formatted(s.getCoUsuario(), s.getNombreUsuario(), s.getNivelDescripcion()));
            lblSesionActual.setForeground(SUCCESS_GREEN);
        } else {
            lblSesionActual.setText("DroAI — Sistema de Gestión de Catálogo");
            lblSesionActual.setForeground(TEXT_SECONDARY);
        }
    }

    /** @return true si el usuario se autenticó exitosamente en este diálogo. */
    public boolean isAutenticado() {
        return autenticado;
    }

    // ═══════════════════════════════════════════════════════════════
    //  Helpers UI
    // ═══════════════════════════════════════════════════════════════

    private JLabel styledLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(TEXT_SECONDARY);
        return lbl;
    }

    private JTextField createStyledField() {
        JTextField tf = new JTextField();
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tf.setBackground(BG_FIELD);
        tf.setForeground(TEXT_PRIMARY);
        tf.setCaretColor(ACCENT);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        return tf;
    }
}
