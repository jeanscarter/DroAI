package com.droai.ui.dialog;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Diálogo modal para solicitar la contraseña extra de ingreso al módulo Cálculo de Comisiones.
 */
public class ComisionesPasswordDialog extends JDialog {

    private static final String CLAVE_CORRECTA = "22168162";
    private boolean autenticado = false;
    private final JPasswordField txtPassword;
    private final JLabel lblError;

    public ComisionesPasswordDialog(Window parent) {
        super(parent, "Seguridad — Cálculo de Comisiones", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(420, 240);
        setResizable(false);
        setLocationRelativeTo(parent);

        JPanel root = new JPanel(new MigLayout("insets 24, fillx, wrap", "[grow]", "[]12[]12[]16[]"));
        root.setBackground(new Color(30, 35, 46));

        // Header / Ícono
        JLabel lblHeader = new JLabel("🔒 Acceso Protegido");
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblHeader.setForeground(new Color(248, 250, 252));
        root.add(lblHeader);

        JLabel lblSub = new JLabel("Introduce la clave especial para ingresar a Cálculo de Comisiones:");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(148, 163, 184));
        root.add(lblSub);

        // Campo clave
        txtPassword = new JPasswordField();
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
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

        JButton btnIngresar = new JButton("Ingresar");
        btnIngresar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnIngresar.setBackground(new Color(42, 107, 255));
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
            lblError.setText("⚠️ Contraseña incorrecta. Inténtalo de nuevo.");
            txtPassword.selectAll();
            txtPassword.requestFocus();
        }
    }

    public boolean isAutenticado() {
        return autenticado;
    }
}
