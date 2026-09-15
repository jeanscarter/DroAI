package com.droai.ui.dialog;

import com.droai.ui.util.IconHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Diálogo de seguridad para ingreso al módulo de Tesorería y Pagos.
 * Exclusivo para Carla Navarro (CN) y Jean Gutiérrez (JG).
 */
public class TesoreriaPasswordDialog extends JDialog {

    // Clave de seguridad autorizada para Tesorería
    private static final String CLAVE_CORRECTA = "889977";
    private boolean autenticado = false;
    private final JPasswordField txtPassword;
    private final JLabel lblError;

    public TesoreriaPasswordDialog(Window parent) {
        super(parent, "Seguridad - Módulo de Tesorería y Pagos", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        IconHelper.applyAppIcon(this);
        setSize(440, 250);
        setResizable(false);
        setLocationRelativeTo(parent);

        JPanel root = new JPanel(new MigLayout("insets 24, fillx, wrap", "[grow]", "[]10[]12[]14[]"));
        root.setBackground(new Color(24, 29, 41));

        // Header / Ícono
        JLabel lblHeader = new JLabel("🔒 Acceso Confidencial de Tesorería");
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblHeader.setForeground(new Color(248, 250, 252));
        root.add(lblHeader);

        JLabel lblSub = new JLabel("<html>Este módulo maneja flujo de caja bancario y pagos.<br>Introduce la clave autorizada para continuar:</html>");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(148, 163, 184));
        root.add(lblSub);

        // Campo clave
        txtPassword = new JPasswordField();
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtPassword.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    validar();
                }
            }
        });
        root.add(txtPassword, "growx");

        // Label Error
        lblError = new JLabel(" ");
        lblError.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblError.setForeground(new Color(239, 68, 68));
        root.add(lblError);

        // Botones
        JPanel btnPanel = new JPanel(new MigLayout("insets 0, fillx, gap 12", "[grow][grow]", "[]"));
        btnPanel.setOpaque(false);

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnCancelar.addActionListener(e -> dispose());
        btnPanel.add(btnCancelar, "grow");

        JButton btnIngresar = new JButton("Ingresar a Tesorería");
        btnIngresar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnIngresar.setBackground(new Color(217, 119, 6)); // Ámbar / Dorado bancario
        btnIngresar.setForeground(Color.WHITE);
        btnIngresar.addActionListener(e -> validar());
        btnPanel.add(btnIngresar, "grow");

        root.add(btnPanel, "growx");

        setContentPane(root);
    }

    private void validar() {
        String pass = new String(txtPassword.getPassword());
        if (CLAVE_CORRECTA.equals(pass)) {
            autenticado = true;
            dispose();
        } else {
            lblError.setText("⚠️ Clave de tesorería incorrecta.");
            txtPassword.selectAll();
            txtPassword.requestFocus();
        }
    }

    public boolean isAutenticado() {
        return autenticado;
    }
}
