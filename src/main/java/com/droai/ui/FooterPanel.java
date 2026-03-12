package com.droai.ui;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * Panel inferior (Sur): Registros count, Ficha Producto, Descuentos,
 * Columna 3, Exportar Excel, Nivel de precio, Actualizar Todos,
 * Usar Costo de Fabrica, Ver existencia, Tasa.
 */
public class FooterPanel extends JPanel {

    private final JLabel lblRegistros;
    private final JCheckBox chkActualizar, chkCostoFabrica, chkVerExistencia;
    private final JComboBox<String> cmbColumna3, cmbNivelPrecio;
    private final JTextField txtTasa;
    private final JButton btnExportar, btnFichaProducto, btnDescuentos;

    private Runnable onExportar;

    public FooterPanel() {
        setLayout(new MigLayout(
            "insets 8 16 8 16, fillx, gap 10",
            "[]8[]8[]16[]8[]push[]8[]16[]8[]",
            "[]6[]"
        ));
        setBackground(new Color(30, 33, 42));

        // ============== FILA 1 ==============

        // Registros
        lblRegistros = new JLabel("Registros: 0");
        lblRegistros.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblRegistros.setForeground(new Color(100, 180, 255));
        add(lblRegistros);

        // Ficha Producto
        btnFichaProducto = accentButton("📋 Ficha Producto", new Color(45, 100, 180));
        add(btnFichaProducto);

        // Descuentos
        btnDescuentos = accentButton("% Descuentos", new Color(45, 100, 180));
        add(btnDescuentos);

        // Columna 3
        JLabel lblCol3 = styledLabel("Columna 3:");
        add(lblCol3);
        cmbColumna3 = styledCombo(new String[]{"", "Precio2", "Precio3", "Precio4"});
        add(cmbColumna3, "w 120!");

        // Exportar Excel
        btnExportar = accentButton("📊 Exportar Excel", new Color(46, 125, 50));
        btnExportar.addActionListener(e -> { if (onExportar != null) onExportar.run(); });
        add(btnExportar);

        // Nivel de precio
        JLabel lblNivel = styledLabel("Nivel de precio:");
        add(lblNivel);
        cmbNivelPrecio = styledCombo(new String[]{"", "Nivel 1", "Nivel 2", "Nivel 3"});
        add(cmbNivelPrecio, "w 120!, wrap");

        // ============== FILA 2 ==============

        chkActualizar    = styledCheck("Actualizar Todos", false);
        chkCostoFabrica  = styledCheck("Usar Costo de Fabrica", false);
        chkVerExistencia = styledCheck("Ver existencia", true);
        add(chkActualizar);
        add(chkCostoFabrica);
        add(chkVerExistencia);

        // spacer
        add(new JLabel(), "span 3, growx");

        // Tasa
        JLabel lblTasa = styledLabel("Tasa:");
        add(lblTasa);
        txtTasa = new JTextField("183.13");
        txtTasa.setFont(new Font("Segoe UI", Font.BOLD, 13));
        txtTasa.setHorizontalAlignment(SwingConstants.RIGHT);
        txtTasa.setBackground(new Color(42, 46, 58));
        txtTasa.setForeground(new Color(255, 215, 80));
        txtTasa.setCaretColor(Color.WHITE);
        txtTasa.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(55, 60, 75)),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        add(txtTasa, "w 100!, span, right");
    }

    // ========== Public API ==========

    public void setRegistroCount(int count) {
        lblRegistros.setText("Registros: " + count);
    }

    public void setOnExportar(Runnable cb) { this.onExportar = cb; }

    public double getTasa() {
        try { return Double.parseDouble(txtTasa.getText().trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    public boolean isActualizarTodos()    { return chkActualizar.isSelected(); }
    public boolean isUsarCostoFabrica()   { return chkCostoFabrica.isSelected(); }
    public boolean isVerExistencia()      { return chkVerExistencia.isSelected(); }

    // ========== Factory helpers ==========

    private JButton accentButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI Emoji", Font.BOLD, 11));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        return btn;
    }

    private JLabel styledLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(new Color(160, 170, 190));
        return lbl;
    }

    private JCheckBox styledCheck(String text, boolean selected) {
        JCheckBox chk = new JCheckBox(text, selected);
        chk.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        chk.setForeground(new Color(160, 170, 190));
        chk.setOpaque(false);
        chk.setFocusPainted(false);
        return chk;
    }

    private JComboBox<String> styledCombo(String[] items) {
        JComboBox<String> cmb = new JComboBox<>(items);
        cmb.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        cmb.setBackground(new Color(42, 46, 58));
        cmb.setForeground(Color.WHITE);
        return cmb;
    }
}
