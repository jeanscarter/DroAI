package com.droai.ui;

import com.droai.ui.components.RoundedButton;
import com.droai.ui.components.RoundedPanel;
import com.droai.ui.dialog.TasaCambioDialog;
import com.droai.ui.dialog.VariacionPreciosDialog;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Panel superior (Norte): Logo, Filtros, Ordenar, Filtrar/Buscar,
 * Utilidad, Variaciones (botones), Toolbar acciones, Búsqueda.
 * Colores gestionados por FlatLaf — sin hardcoded.
 */
public class HeaderPanel extends JPanel {

    private final JTextField txtFiltro;
    private final JTextField txtBuscar;
    private final JRadioButton rbCodigo, rbDescripcion, rbReferencia;
    private final JRadioButton rbMatematica, rbFinanciera;
    private final JCheckBox chkCalcular, chkUtilid;
    private final RoundedButton btnFiltrar, btnBuscar;
    private final RoundedButton btnImprimir, btnImportar, btnSubir, btnGuardar, btnDeshacer, btnTema;
    private final JButton btnPreciosPct, btnTasaCambio;

    private Consumer<String> onSearch;
    private Runnable onFiltrar;
    private Runnable onGuardar;
    private Runnable onDeshacer;
    private Runnable onCambiarTema;

    public HeaderPanel() {
        setLayout(new MigLayout(
            "insets 6 12 4 12, fillx, gap 6",
            "[]6[]10[]6[]push[]4[]4[]4[]4[]",
            "[]4[]"
        ));

        // ============== ROW 1 ==============

        // -- Logo area --
        JLabel lblLogo = new JLabel();
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/images/logo.png"));
            Image scaled = icon.getImage().getScaledInstance(36, 36, Image.SCALE_SMOOTH);
            lblLogo.setIcon(new ImageIcon(scaled));
        } catch (Exception e) {
            lblLogo.setText("🧪");
            lblLogo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        }
        add(lblLogo);

        // -- Filtros Aplicados --
        JLabel lblFiltros = styledLabel("Filtros Aplicados:");
        add(lblFiltros);

        txtFiltro = new JTextField();
        txtFiltro.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        add(txtFiltro, "w 160!");

        // -- Ordenado por --
        rbCodigo      = styledRadio("Codigo", true);
        rbDescripcion = styledRadio("Descripcion", false);
        rbReferencia  = styledRadio("Referencia", false);
        ButtonGroup bgOrden = new ButtonGroup();
        bgOrden.add(rbCodigo); bgOrden.add(rbDescripcion); bgOrden.add(rbReferencia);

        JPanel pnlOrden = new JPanel(new MigLayout("insets 0, gap 2", "[]2[]2[]2[]", ""));
        pnlOrden.setOpaque(false);
        JLabel lblOrd = styledLabel("Ordenado por:");
        pnlOrden.add(lblOrd);
        pnlOrden.add(rbCodigo);
        pnlOrden.add(rbDescripcion);
        pnlOrden.add(rbReferencia);
        add(pnlOrden);

        // -- Filtrar / Buscar botones --
        btnFiltrar = new RoundedButton("🔽", "Filtrar");
        btnFiltrar.addActionListener(e -> { if (onFiltrar != null) onFiltrar.run(); });
        add(btnFiltrar);

        btnBuscar = new RoundedButton("🔍", "Buscar");
        add(btnBuscar);

        // -- Utilidad card (compact) --
        RoundedPanel pnlUtilidad = new RoundedPanel(10, false);
        pnlUtilidad.setBackground(UIManager.getColor("Panel.background"));
        pnlUtilidad.setLayout(new MigLayout("insets 4 8 4 8, gap 2, wrap", "[]", ""));
        JLabel lblUtil = styledLabel("Utilidad");
        lblUtil.setFont(lblUtil.getFont().deriveFont(Font.BOLD, 11f));
        pnlUtilidad.add(lblUtil);
        rbMatematica = styledRadio("Matematica", false);
        rbFinanciera = styledRadio("Financiera", true);
        ButtonGroup bgUtil = new ButtonGroup();
        bgUtil.add(rbMatematica); bgUtil.add(rbFinanciera);
        JPanel utilRadios = new JPanel(new MigLayout("insets 0, gap 0", "[][]", ""));
        utilRadios.setOpaque(false);
        utilRadios.add(rbMatematica);
        utilRadios.add(rbFinanciera);
        pnlUtilidad.add(utilRadios);
        chkCalcular = styledCheck("Calcular", true);
        chkUtilid   = styledCheck("Utilid", false);
        JPanel utilChks = new JPanel(new MigLayout("insets 0, gap 4", "[][]", ""));
        utilChks.setOpaque(false);
        utilChks.add(chkCalcular); utilChks.add(chkUtilid);
        pnlUtilidad.add(utilChks);
        add(pnlUtilidad);

        // -- Variaciones card --
        RoundedPanel pnlVar = new RoundedPanel(10, false);
        pnlVar.setBackground(UIManager.getColor("Panel.background"));
        pnlVar.setLayout(new MigLayout("insets 4 8 4 8, gap 4, wrap", "[]", ""));
        JLabel lblVar = styledLabel("Variaciones");
        lblVar.setFont(lblVar.getFont().deriveFont(Font.BOLD, 11f));
        pnlVar.add(lblVar);

        btnPreciosPct = accentButton("📊 Precios %");
        btnPreciosPct.addActionListener(e -> openVariacionPrecios());
        pnlVar.add(btnPreciosPct, "growx");

        btnTasaCambio = accentButton("💲 Tasa de Cambio");
        btnTasaCambio.addActionListener(e -> openTasaCambio());
        pnlVar.add(btnTasaCambio, "growx");
        add(pnlVar);

        // -- Toolbar actions (top-right) --
        btnImprimir = new RoundedButton("🖨️", "Imprimir");
        btnImportar = new RoundedButton("📥", "Importar");
        btnSubir    = new RoundedButton("📤", "Subir");
        btnGuardar  = new RoundedButton("💾", "Guardar");
        btnDeshacer = new RoundedButton("↩️", "Deshacer");
        btnTema     = new RoundedButton("🌓", "Tema");

        btnGuardar.addActionListener(e -> { if (onGuardar != null) onGuardar.run(); });
        btnDeshacer.addActionListener(e -> { if (onDeshacer != null) onDeshacer.run(); });
        btnTema.addActionListener(e -> { if (onCambiarTema != null) onCambiarTema.run(); });

        add(btnImprimir);
        add(btnImportar);
        add(btnSubir);
        add(btnGuardar);
        add(btnDeshacer);
        add(btnTema, "wrap");

        // ============== ROW 2: compact search bar ==============
        JLabel lblSearch = styledLabel("🔍");
        lblSearch.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        add(lblSearch);

        txtBuscar = new JTextField();
        txtBuscar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtBuscar.putClientProperty("JTextField.placeholderText", "Buscar en catálogo...");
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { fireSearch(); }
            public void removeUpdate(DocumentEvent e)  { fireSearch(); }
            public void changedUpdate(DocumentEvent e)  { fireSearch(); }
        });
        add(txtBuscar, "span, growx");
    }

    // ========== Dialog openers ==========

    private void openVariacionPrecios() {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        VariacionPreciosDialog dlg = new VariacionPreciosDialog(owner);
        dlg.setVisible(true);
    }

    private void openTasaCambio() {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        TasaCambioDialog dlg = new TasaCambioDialog(owner, 183.13);
        dlg.setVisible(true);
    }

    private void fireSearch() {
        if (onSearch != null) onSearch.accept(txtBuscar.getText());
    }

    // ========== Callbacks ==========

    public void setOnSearch(Consumer<String> cb)  { this.onSearch  = cb; }
    public void setOnFiltrar(Runnable cb)         { this.onFiltrar = cb; }
    public void setOnGuardar(Runnable cb)         { this.onGuardar = cb; }
    public void setOnDeshacer(Runnable cb)        { this.onDeshacer = cb; }
    public void setOnCambiarTema(Runnable cb)     { this.onCambiarTema = cb; }

    // ========== Factory helpers ==========

    private JLabel styledLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        return lbl;
    }

    private JRadioButton styledRadio(String text, boolean selected) {
        JRadioButton rb = new JRadioButton(text, selected);
        rb.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        rb.setOpaque(false);
        rb.setFocusPainted(false);
        return rb;
    }

    private JCheckBox styledCheck(String text, boolean selected) {
        JCheckBox cb = new JCheckBox(text, selected);
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        cb.setOpaque(false);
        cb.setFocusPainted(false);
        return cb;
    }

    private JButton accentButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI Emoji", Font.BOLD, 10));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        return btn;
    }
}
