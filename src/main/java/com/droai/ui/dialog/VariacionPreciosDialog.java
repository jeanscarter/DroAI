package com.droai.ui.dialog;

import com.droai.ui.components.RoundedPanel;
import com.droai.ui.components.Toast;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * Diálogo "Variación de Precios" — 3 columnas: Arancel, Utilidades, Precios.
 * Cada columna: campo %, radio buttons (Reemplazar/Aumentar/Disminuir).
 * Botón "Procesar" aplica cambios.
 */
public class VariacionPreciosDialog extends JDialog {

    private final JTextField txtArancel, txtUtilidades, txtPrecios;
    private final JRadioButton rbArancelReemplazar, rbArancelAumentar, rbArancelDisminuir;
    private final JRadioButton rbUtilidadesReemplazar, rbUtilidadesAumentar, rbUtilidadesDisminuir;
    private final JRadioButton rbPreciosAumentar, rbPreciosDisminuir;

    public VariacionPreciosDialog(Frame owner) {
        super(owner, "Variación de Precios", true);
        setSize(580, 260);
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel root = new JPanel(new MigLayout(
            "insets 20 24 20 24, gap 16",
            "[]16[]16[]push[]",
            "[top]"
        ));
        root.setBackground(new Color(30, 33, 42));

        // ========== Arancel ==========
        RoundedPanel pnlArancel = createColumn("Arancel:");
        txtArancel = styledField();
        rbArancelReemplazar = styledRadio("Reemplazar", true);
        rbArancelAumentar   = styledRadio("Aumentar", false);
        rbArancelDisminuir  = styledRadio("Disminuir", false);
        ButtonGroup bgA = new ButtonGroup();
        bgA.add(rbArancelReemplazar); bgA.add(rbArancelAumentar); bgA.add(rbArancelDisminuir);

        JPanel pctRowA = new JPanel(new MigLayout("insets 0, gap 4", "[][shrink 0]", ""));
        pctRowA.setOpaque(false);
        pctRowA.add(txtArancel, "w 70!");
        pctRowA.add(styledLabel("%"));
        pnlArancel.add(pctRowA, "wrap");
        pnlArancel.add(rbArancelReemplazar, "wrap");
        pnlArancel.add(rbArancelAumentar, "wrap");
        pnlArancel.add(rbArancelDisminuir);
        root.add(pnlArancel);

        // ========== Utilidades ==========
        RoundedPanel pnlUtil = createColumn("Utilidades:");
        txtUtilidades = styledField();
        rbUtilidadesReemplazar = styledRadio("Reemplazar", true);
        rbUtilidadesAumentar   = styledRadio("Aumentar", false);
        rbUtilidadesDisminuir  = styledRadio("Disminuir", false);
        ButtonGroup bgU = new ButtonGroup();
        bgU.add(rbUtilidadesReemplazar); bgU.add(rbUtilidadesAumentar); bgU.add(rbUtilidadesDisminuir);

        JPanel pctRowU = new JPanel(new MigLayout("insets 0, gap 4", "[][shrink 0]", ""));
        pctRowU.setOpaque(false);
        pctRowU.add(txtUtilidades, "w 70!");
        pctRowU.add(styledLabel("%"));
        pnlUtil.add(pctRowU, "wrap");
        pnlUtil.add(rbUtilidadesReemplazar, "wrap");
        pnlUtil.add(rbUtilidadesAumentar, "wrap");
        pnlUtil.add(rbUtilidadesDisminuir);
        root.add(pnlUtil);

        // ========== Precios ==========
        RoundedPanel pnlPrecios = createColumn("Precios");
        txtPrecios = styledField();
        rbPreciosAumentar  = styledRadio("Aumentar", true);
        rbPreciosDisminuir = styledRadio("Disminuir", false);
        ButtonGroup bgP = new ButtonGroup();
        bgP.add(rbPreciosAumentar); bgP.add(rbPreciosDisminuir);

        JPanel pctRowP = new JPanel(new MigLayout("insets 0, gap 4", "[][shrink 0]", ""));
        pctRowP.setOpaque(false);
        pctRowP.add(txtPrecios, "w 70!");
        pctRowP.add(styledLabel("%"));
        pnlPrecios.add(pctRowP, "wrap");
        pnlPrecios.add(rbPreciosAumentar, "wrap");
        pnlPrecios.add(rbPreciosDisminuir);
        root.add(pnlPrecios);

        // ========== Procesar ==========
        JButton btnProcesar = new JButton("⚙ Procesar");
        btnProcesar.setFont(new Font("Segoe UI Emoji", Font.BOLD, 13));
        btnProcesar.setBackground(new Color(50, 80, 160));
        btnProcesar.setForeground(Color.WHITE);
        btnProcesar.setFocusPainted(false);
        btnProcesar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnProcesar.setPreferredSize(new Dimension(110, 80));
        btnProcesar.addActionListener(e -> {
            Toast.show("Variación de precios aplicada", Toast.Type.SUCCESS);
            dispose();
        });
        root.add(btnProcesar, "aligny center");

        setContentPane(root);
    }

    private RoundedPanel createColumn(String title) {
        RoundedPanel p = new RoundedPanel(12, false);
        p.setBackground(new Color(38, 42, 54));
        p.setLayout(new MigLayout("insets 10 14 10 14, wrap, gap 4", "[]", ""));
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(180, 190, 215));
        p.add(lbl, "wrap, gapbottom 6");
        return p;
    }

    private JTextField styledField() {
        JTextField tf = new JTextField("0.00");
        tf.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tf.setHorizontalAlignment(SwingConstants.CENTER);
        tf.setBackground(new Color(45, 50, 62));
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(new Color(100, 160, 255));
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 80, 100)),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        return tf;
    }

    private JRadioButton styledRadio(String text, boolean sel) {
        JRadioButton rb = new JRadioButton(text, sel);
        rb.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        rb.setForeground(new Color(180, 190, 210));
        rb.setOpaque(false);
        rb.setFocusPainted(false);
        return rb;
    }

    private JLabel styledLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbl.setForeground(new Color(180, 190, 210));
        return lbl;
    }
}
