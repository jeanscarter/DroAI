package com.droai.ui.dialog;

import com.droai.ui.components.Toast;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Diálogo "Tasa de Cambio" — Moneda, Tasa Actual, Nueva Tasa,
 * checkboxes, Procesar.
 */
public class TasaCambioDialog extends JDialog {

    private final JTextField txtMoneda, txtTasaActual, txtNuevaTasa, txtFecha;
    private final JCheckBox chkSoloAfectar, chkAfectarCosto, chkGuardarTasa;
    private final JCheckBox chkActualizarFormas, chkIgualarRecepcion;

    private Runnable onTasaChanged;

    public TasaCambioDialog(Frame owner, double tasaActual) {
        super(owner, "Tasa de Cambio", true);
        setSize(520, 280);
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel root = new JPanel(new MigLayout(
            "insets 20 24 20 24, gap 10, wrap",
            "[]12[]16[]push[]",
            "[]8[]8[]16[]8[]"
        ));
        root.setBackground(new Color(30, 33, 42));

        // ========== Row 1: Moneda + Actualizado el ==========
        root.add(styledLabel("Moneda:"));
        txtMoneda = styledField("USD", 80);
        root.add(txtMoneda);

        JLabel lblActualizado = new JLabel("Actualizado el");
        lblActualizado.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblActualizado.setForeground(new Color(140, 150, 170));
        root.add(lblActualizado);

        // ========== Create fields first so lambda can reference them ==========

        txtTasaActual = styledField(String.format("%.2f", tasaActual), 100);
        txtTasaActual.setEditable(false);
        txtTasaActual.setForeground(new Color(150, 160, 180));

        txtFecha = styledField(
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a")),
            160
        );
        txtFecha.setEditable(false);
        txtFecha.setForeground(new Color(150, 160, 180));

        txtNuevaTasa = styledField("0.00", 100);
        txtNuevaTasa.setBackground(new Color(60, 40, 40));
        txtNuevaTasa.setForeground(new Color(255, 200, 100));
        txtNuevaTasa.setFont(new Font("Segoe UI", Font.BOLD, 16));

        // ========== Procesar button (spans rows, now safe to ref txtNuevaTasa) ==========
        JButton btnProcesar = new JButton("⚙ Procesar");
        btnProcesar.setFont(new Font("Segoe UI Emoji", Font.BOLD, 13));
        btnProcesar.setBackground(new Color(50, 80, 160));
        btnProcesar.setForeground(Color.WHITE);
        btnProcesar.setFocusPainted(false);
        btnProcesar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnProcesar.setPreferredSize(new Dimension(110, 70));
        btnProcesar.addActionListener(e -> {
            if (onTasaChanged != null) onTasaChanged.run();
            Toast.show("Tasa actualizada: " + txtNuevaTasa.getText(), Toast.Type.SUCCESS);
            dispose();
        });
        root.add(btnProcesar, "spany 3, aligny center, wrap");

        // ========== Row 2: Tasa Actual ==========
        root.add(styledLabel("Tasa de Actual:"));
        root.add(txtTasaActual);
        root.add(txtFecha, "wrap");

        // ========== Row 3: Nueva Tasa ==========
        root.add(styledLabel("Nueva Tasa:"));
        root.add(txtNuevaTasa, "span 2, wrap");

        // ========== Row 4: Solo afectar ==========
        chkSoloAfectar = styledCheck("Solo afectar productos asignados a la moneda", false);
        root.add(chkSoloAfectar, "span 4, wrap");

        // ========== Row 5: Checkboxes grid ==========
        JPanel chkPanel = new JPanel(new MigLayout("insets 0, gap 16", "[][]", "[][]"));
        chkPanel.setOpaque(false);
        chkAfectarCosto     = styledCheck("Afectar costo actual", true);
        chkGuardarTasa      = styledCheck("Guardar Tasa", true);
        chkActualizarFormas = styledCheck("Actualizar formas de pago", true);
        chkIgualarRecepcion = styledCheck("Igualar a Tasa de recepcion", true);
        chkPanel.add(chkAfectarCosto);
        chkPanel.add(chkGuardarTasa);
        chkPanel.add(chkActualizarFormas, "wrap");
        chkPanel.add(chkIgualarRecepcion);
        root.add(chkPanel, "span 4");

        setContentPane(root);
    }

    public void setOnTasaChanged(Runnable cb) { this.onTasaChanged = cb; }

    public double getNuevaTasa() {
        try { return Double.parseDouble(txtNuevaTasa.getText().trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private JLabel styledLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(180, 190, 210));
        return lbl;
    }

    private JTextField styledField(String text, int width) {
        JTextField tf = new JTextField(text);
        tf.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tf.setBackground(new Color(45, 50, 62));
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(new Color(100, 160, 255));
        tf.setPreferredSize(new Dimension(width, 30));
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(65, 72, 90)),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        return tf;
    }

    private JCheckBox styledCheck(String text, boolean sel) {
        JCheckBox cb = new JCheckBox(text, sel);
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        cb.setForeground(new Color(180, 190, 210));
        cb.setOpaque(false);
        cb.setFocusPainted(false);
        return cb;
    }
}
