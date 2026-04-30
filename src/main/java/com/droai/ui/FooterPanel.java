package com.droai.ui;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.function.Consumer;

/**
 * Panel inferior (Sur): Registros count, Ficha Producto, Descuentos,
 * Columna 3, Exportar Excel, Nivel de precio, Actualizar Todos,
 * Usar Costo de Fabrica, Ver existencia, Tasa.
 * Colores gestionados por FlatLaf — sin hardcoded.
 */
public class FooterPanel extends JPanel {

    private final JLabel lblRegistros;
    private final JCheckBox chkActualizar, chkCostoFabrica, chkVerExistencia;
    private final JComboBox<String> cmbColumna3, cmbNivelPrecio;
    private final JTextField txtTasa;
    private final JButton btnExportar, btnFichaProducto, btnDescuentos;

    private Runnable onExportar;
    private Runnable onFichaProducto;
    private Consumer<String> onColumna3Changed;

    public FooterPanel() {
        setLayout(new MigLayout(
            "insets 8 16 8 16, fillx, gap 10",
            "[]8[]8[]16[]8[]push[]8[]16[]8[]",
            "[]6[]"
        ));

        // ============== FILA 1 ==============

        // Registros
        lblRegistros = new JLabel("Registros: 0");
        lblRegistros.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblRegistros.setForeground(UIManager.getColor("Component.accentColor"));
        add(lblRegistros);

        // Ficha Producto
        btnFichaProducto = accentButton("📋 Ficha Producto");
        btnFichaProducto.addActionListener(e -> { if (onFichaProducto != null) onFichaProducto.run(); });
        add(btnFichaProducto);

        // Descuentos
        btnDescuentos = accentButton("% Descuentos");
        add(btnDescuentos);

        // Columna 3
        JLabel lblCol3 = styledLabel("Columna 3:");
        add(lblCol3);
        cmbColumna3 = new JComboBox<>(new String[]{"Marca", "Codigo de Barra", "Ubicacion", "Campo 1", "Campo 2"});
        cmbColumna3.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        cmbColumna3.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && onColumna3Changed != null) {
                onColumna3Changed.accept((String) e.getItem());
            }
        });
        add(cmbColumna3, "w 120!");

        // Exportar Excel
        btnExportar = new JButton("📊 Exportar Excel");
        btnExportar.setFont(new Font("Segoe UI Emoji", Font.BOLD, 11));
        btnExportar.setBackground(new Color(46, 125, 50));
        btnExportar.setForeground(Color.WHITE);
        btnExportar.setFocusPainted(false);
        btnExportar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnExportar.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        btnExportar.addActionListener(e -> { if (onExportar != null) onExportar.run(); });
        add(btnExportar);

        // Nivel de precio
        JLabel lblNivel = styledLabel("Nivel de precio:");
        add(lblNivel);
        cmbNivelPrecio = new JComboBox<>(new String[]{"", "Nivel 1", "Nivel 2", "Nivel 3"});
        cmbNivelPrecio.setFont(new Font("Segoe UI", Font.PLAIN, 11));
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
        add(txtTasa, "w 100!, span, right");
    }

    // ========== Public API ==========

    public void setRegistroCount(int count) {
        lblRegistros.setText("Registros: " + count);
    }

    public void setOnExportar(Runnable cb) { this.onExportar = cb; }
    public void setOnFichaProducto(Runnable cb) { this.onFichaProducto = cb; }

    public void setOnColumna3Changed(Consumer<String> cb) { this.onColumna3Changed = cb; }

    public double getTasa() {
        try { return Double.parseDouble(txtTasa.getText().trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    public boolean isActualizarTodos()    { return chkActualizar.isSelected(); }
    public boolean isUsarCostoFabrica()   { return chkCostoFabrica.isSelected(); }
    public boolean isVerExistencia()      { return chkVerExistencia.isSelected(); }

    // ========== Factory helpers ==========

    private JButton accentButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI Emoji", Font.BOLD, 11));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        return btn;
    }

    private JLabel styledLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        return lbl;
    }

    private JCheckBox styledCheck(String text, boolean selected) {
        JCheckBox chk = new JCheckBox(text, selected);
        chk.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        chk.setOpaque(false);
        chk.setFocusPainted(false);
        return chk;
    }
}
