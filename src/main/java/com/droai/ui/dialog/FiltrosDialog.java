package com.droai.ui.dialog;

import com.droai.model.FiltrosCriteria;
import com.droai.model.FiltrosCriteria.FiltroCosto;
import com.droai.model.FiltrosCriteria.FiltroPrecio;
import com.droai.model.FiltrosCriteria.FiltroStock;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * Diálogo modal de "Filtros" que replica la ventana de Profit Plus.
 *
 * <p>Layout basado en la imagen de referencia: campos de texto, combos,
 * checkboxes, radio buttons agrupados por Costo/Precio/Stock,
 * campos de fecha, y botones "Aplicar Filtros" / "Quitar Filtros" a la derecha.
 */
public class FiltrosDialog extends JDialog {

    // ── Colores del tema oscuro premium ──
    private static final Color BG_DIALOG   = new Color(30, 33, 42);
    private static final Color BG_FIELD    = new Color(45, 50, 62);
    private static final Color BG_SECTION  = new Color(38, 42, 54);
    private static final Color BORDER      = new Color(65, 72, 90);
    private static final Color TEXT_LABEL  = new Color(180, 190, 210);
    private static final Color TEXT_VALUE  = Color.WHITE;
    private static final Color ACCENT_BLUE = new Color(42, 107, 255);
    private static final Color ACCENT_RED  = new Color(200, 60, 60);
    private static final Color CARET       = new Color(100, 160, 255);

    // ── Campos de texto ──
    private final JTextField txtCodigo, txtDescripcion, txtReferencia, txtCodBarra;
    private final JTextField txtMarca, txtModelo, txtUbicacion, txtCampo1, txtCampo2;
    private final JTextField txtProveedor;

    // ── Combos ──
    private final JComboBox<String> cmbCodigoOp, cmbMoneda, cmbGrupo, cmbSubGrupo;
    private final JComboBox<String> cmbProveedorOp, cmbAlmacen;

    // ── Campos de fecha ──
    @SuppressWarnings("unused")
    private final JTextField txtFechaCreacionDia, txtFechaCreacionMes, txtFechaCreacionAnio;
    @SuppressWarnings("unused")
    private final JTextField txtFechaActDia, txtFechaActMes, txtFechaActAnio;
    @SuppressWarnings("unused")
    private final JSpinner spnFechaCreacionHora, spnFechaActHora;

    // ── Checkboxes ──
    private final JCheckBox chkMostrarInactivos, chkCualquierPosicion;
    private final JCheckBox chkSoloPrecioMenorCosto, chkDiferente;

    // ── Radio buttons: Costo ──
    private final JRadioButton rbCostoTodos, rbCostoSinCosto, rbCostoConCosto;

    // ── Radio buttons: Precio ──
    private final JRadioButton rbPrecioTodos, rbPrecioSinPrecio, rbPrecioConPrecio;

    // ── Radio buttons: Stock ──
    private final JRadioButton rbStockTodos, rbStockConStock, rbStockSinStock;

    // ── Resultado ──
    private FiltrosCriteria resultado = null;
    private boolean filtrosEliminados = false;

    public FiltrosDialog(Frame owner) {
        this(owner, null);
    }

    public FiltrosDialog(Frame owner, FiltrosCriteria criteriaAnterior) {
        super(owner, "Filtros", true);
        setSize(780, 560);
        setLocationRelativeTo(owner);
        setResizable(false);

        // ── Root panel ──
        JPanel root = new JPanel(new MigLayout(
            "insets 16 20 16 20, gap 6",
            "[grow]10[right]",
            "[]"
        ));
        root.setBackground(BG_DIALOG);

        // ── Panel principal de campos (izquierda) ──
        JPanel formPanel = new JPanel(new MigLayout(
            "insets 10 14 10 14, gap 6, wrap",
            "[]8[]16[]8[]16[]8[]",
            "[]6[]6[]6[]6[]6[]6[]6[]6[]6[]6[]6[]6[]6[]6[]"
        ));
        formPanel.setBackground(BG_SECTION);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));

        // ══════════════ Fila 1: Código + Moneda + Mostrar Inactivos ══════════════
        formPanel.add(styledLabel("Código:"));
        txtCodigo = styledField(90);
        formPanel.add(txtCodigo);
        cmbCodigoOp = styledCombo(new String[]{""}, 50);
        formPanel.add(cmbCodigoOp);

        formPanel.add(styledLabel("Moneda:"));
        cmbMoneda = styledCombo(new String[]{"", "USD", "BS", "EUR"}, 100);
        formPanel.add(cmbMoneda);

        chkMostrarInactivos = styledCheck("Mostrar Inactivos", false);
        formPanel.add(chkMostrarInactivos, "wrap");

        // ══════════════ Fila 2: Descripción + Cualquier posición ══════════════
        formPanel.add(styledLabel("Descripción:"));
        txtDescripcion = styledField(200);
        formPanel.add(txtDescripcion, "span 3, growx");

        chkCualquierPosicion = styledCheck("Cualquier posicion", true);
        formPanel.add(chkCualquierPosicion, "span 2, wrap");

        // ══════════════ Fila 3: Referencia ══════════════
        formPanel.add(styledLabel("Referencia:"));
        txtReferencia = styledField(200);
        formPanel.add(txtReferencia, "span 3, growx");

        // Etiqueta "Desde la fecha de Creacion"
        JLabel lblFechaCreacion = styledLabel("Desde la fecha de Creacion");
        lblFechaCreacion.setFont(new Font("Segoe UI", Font.BOLD, 10));
        formPanel.add(lblFechaCreacion, "span 2, wrap");

        // ══════════════ Fila 4: Cod.Barra + Fecha de Creación ══════════════
        formPanel.add(styledLabel("Cod.Barra:"));
        txtCodBarra = styledField(200);
        formPanel.add(txtCodBarra, "span 3, growx");

        // Campos de fecha de creación: / /
        JPanel pnlFechaCreacion = buildFechaPanel();
        txtFechaCreacionDia = (JTextField) pnlFechaCreacion.getClientProperty("dia");
        txtFechaCreacionMes = (JTextField) pnlFechaCreacion.getClientProperty("mes");
        txtFechaCreacionAnio = (JTextField) pnlFechaCreacion.getClientProperty("anio");
        spnFechaCreacionHora = (JSpinner) pnlFechaCreacion.getClientProperty("spinner");
        formPanel.add(pnlFechaCreacion, "span 2, wrap");

        // ══════════════ Fila 5: Marca + Modelo + Fecha de Actualización ══════════════
        formPanel.add(styledLabel("Marca:"));
        txtMarca = styledField(100);
        formPanel.add(txtMarca);

        formPanel.add(styledLabel("Modelo:"));
        txtModelo = styledField(90);
        formPanel.add(txtModelo);

        // Etiqueta "Desde la fecha de Actualización" ya se pone arriba del campo
        JLabel lblFechaAct = styledLabel("Desde la fecha de Actualizacion");
        lblFechaAct.setFont(new Font("Segoe UI", Font.BOLD, 10));
        formPanel.add(lblFechaAct, "span 2, wrap");

        // ══════════════ Fila 6: Grupo + Fecha de Actualización ══════════════
        formPanel.add(styledLabel("Grupo:"));
        cmbGrupo = styledCombo(new String[]{""}, 120);
        formPanel.add(cmbGrupo, "span 3");

        JPanel pnlFechaAct = buildFechaPanel();
        txtFechaActDia = (JTextField) pnlFechaAct.getClientProperty("dia");
        txtFechaActMes = (JTextField) pnlFechaAct.getClientProperty("mes");
        txtFechaActAnio = (JTextField) pnlFechaAct.getClientProperty("anio");
        spnFechaActHora = (JSpinner) pnlFechaAct.getClientProperty("spinner");
        formPanel.add(pnlFechaAct, "span 2, wrap");

        // ══════════════ Fila 7: SubGrupo ══════════════
        formPanel.add(styledLabel("SubGrupo:"));
        cmbSubGrupo = styledCombo(new String[]{""}, 120);
        formPanel.add(cmbSubGrupo, "span 5, wrap");

        // ══════════════ Fila 8: Proveedor (dentro de borde titulado) ══════════════
        JPanel pnlProveedor = new JPanel(new MigLayout("insets 6 8 6 8, gap 4", "[]4[]4[grow]4[]", "[]"));
        pnlProveedor.setBackground(BG_SECTION);
        pnlProveedor.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER),
            "Proveedor:",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 11),
            TEXT_LABEL
        ));

        cmbProveedorOp = styledCombo(new String[]{""}, 50);
        pnlProveedor.add(cmbProveedorOp);
        txtProveedor = styledField(200);
        pnlProveedor.add(txtProveedor, "growx");

        JButton btnBuscarProv = new JButton("🔍");
        btnBuscarProv.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        btnBuscarProv.setBackground(ACCENT_BLUE);
        btnBuscarProv.setForeground(TEXT_VALUE);
        btnBuscarProv.setFocusPainted(false);
        btnBuscarProv.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnBuscarProv.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        pnlProveedor.add(btnBuscarProv);

        formPanel.add(pnlProveedor, "span 6, growx, wrap");

        // ══════════════ Fila 9: Costo (radios) + Campo 1 ══════════════
        formPanel.add(styledLabel("Costo:"));

        rbCostoTodos = styledRadio("Todos", true);
        rbCostoSinCosto = styledRadio("Solo sin costo", false);
        rbCostoConCosto = styledRadio("Solo con costo", false);
        ButtonGroup bgCosto = new ButtonGroup();
        bgCosto.add(rbCostoTodos);
        bgCosto.add(rbCostoSinCosto);
        bgCosto.add(rbCostoConCosto);

        JPanel pnlCostoRadios = new JPanel(new MigLayout("insets 0, gap 4", "[][][]", "[]"));
        pnlCostoRadios.setOpaque(false);
        pnlCostoRadios.add(rbCostoTodos);
        pnlCostoRadios.add(rbCostoSinCosto);
        pnlCostoRadios.add(rbCostoConCosto);
        formPanel.add(pnlCostoRadios, "span 3");

        formPanel.add(styledLabel("Campo 1:"));
        txtCampo1 = styledField(100);
        formPanel.add(txtCampo1, "wrap");

        // ══════════════ Fila 10: Solo Precio <= Costo + Campo 2 ══════════════
        formPanel.add(new JLabel(), "skip 1"); // spacer
        chkSoloPrecioMenorCosto = styledCheck("Solo Precio <= Costo", false);
        formPanel.add(chkSoloPrecioMenorCosto, "span 2");

        formPanel.add(styledLabel("Campo 2:"));
        txtCampo2 = styledField(100);
        formPanel.add(txtCampo2, "wrap");

        // ══════════════ Fila 11: Precio (radios) ══════════════
        formPanel.add(styledLabel("Precio:"));

        rbPrecioTodos = styledRadio("Todos", true);
        rbPrecioSinPrecio = styledRadio("Solo sin precio", false);
        rbPrecioConPrecio = styledRadio("Solo con precio", false);
        ButtonGroup bgPrecio = new ButtonGroup();
        bgPrecio.add(rbPrecioTodos);
        bgPrecio.add(rbPrecioSinPrecio);
        bgPrecio.add(rbPrecioConPrecio);

        JPanel pnlPrecioRadios = new JPanel(new MigLayout("insets 0, gap 4", "[][][]", "[]"));
        pnlPrecioRadios.setOpaque(false);
        pnlPrecioRadios.add(rbPrecioTodos);
        pnlPrecioRadios.add(rbPrecioSinPrecio);
        pnlPrecioRadios.add(rbPrecioConPrecio);
        formPanel.add(pnlPrecioRadios, "span 5, wrap");

        // ══════════════ Fila 12: Stock (radios) ══════════════
        formPanel.add(styledLabel("Stock:"));

        rbStockTodos = styledRadio("Todos", true);
        rbStockConStock = styledRadio("Solo con stock", false);
        rbStockSinStock = styledRadio("Solo sin stock", false);
        ButtonGroup bgStock = new ButtonGroup();
        bgStock.add(rbStockTodos);
        bgStock.add(rbStockConStock);
        bgStock.add(rbStockSinStock);

        JPanel pnlStockRadios = new JPanel(new MigLayout("insets 0, gap 4", "[][][]", "[]"));
        pnlStockRadios.setOpaque(false);
        pnlStockRadios.add(rbStockTodos);
        pnlStockRadios.add(rbStockConStock);
        pnlStockRadios.add(rbStockSinStock);
        formPanel.add(pnlStockRadios, "span 5, wrap");

        // ══════════════ Fila 13: Almacén (dentro de borde titulado) ══════════════
        JPanel pnlAlmacen = new JPanel(new MigLayout("insets 6 8 6 8, gap 4", "[]4[grow]", "[]"));
        pnlAlmacen.setBackground(BG_SECTION);
        pnlAlmacen.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER),
            "Almacén:",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 11),
            TEXT_LABEL
        ));
        cmbAlmacen = styledCombo(new String[]{""}, 120);
        pnlAlmacen.add(cmbAlmacen, "growx");
        formPanel.add(pnlAlmacen, "span 6, growx, wrap");

        // ══════════════ Fila 14: Ubicación + Diferente ══════════════
        formPanel.add(styledLabel("Ubicacion:"));
        txtUbicacion = styledField(160);
        formPanel.add(txtUbicacion, "span 2");

        chkDiferente = styledCheck("Diferente", false);
        formPanel.add(chkDiferente, "span 3, wrap");

        // ── Panel de botones laterales (derecha) ──
        JPanel buttonsPanel = new JPanel(new MigLayout("insets 0, wrap, gap 12", "[]", "[]12[]push"));
        buttonsPanel.setOpaque(false);

        JButton btnAplicar = createActionButton("🔽", "Aplicar\nFiltros", ACCENT_BLUE);
        btnAplicar.addActionListener(e -> aplicarFiltros());
        buttonsPanel.add(btnAplicar);

        JButton btnQuitar = createActionButton("⛔", "Quitar\nFiltros", ACCENT_RED);
        btnQuitar.addActionListener(e -> quitarFiltros());
        buttonsPanel.add(btnQuitar);

        root.add(formPanel, "grow");
        root.add(buttonsPanel, "top, w 100!");

        setContentPane(root);

        // Cargar criterios anteriores si existen
        if (criteriaAnterior != null) {
            cargarCriteria(criteriaAnterior);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Acciones
    // ═══════════════════════════════════════════════════════════════

    private void aplicarFiltros() {
        resultado = new FiltrosCriteria();

        resultado.setCodigo(txtCodigo.getText());
        resultado.setDescripcion(txtDescripcion.getText());
        resultado.setReferencia(txtReferencia.getText());
        resultado.setCodigoBarra(txtCodBarra.getText());
        resultado.setMarca(txtMarca.getText());
        resultado.setModelo(txtModelo.getText());
        resultado.setUbicacion(txtUbicacion.getText());
        resultado.setCampo1(txtCampo1.getText());
        resultado.setCampo2(txtCampo2.getText());
        resultado.setProveedor(txtProveedor.getText());

        resultado.setMoneda(getComboText(cmbMoneda));
        resultado.setGrupo(getComboText(cmbGrupo));
        resultado.setSubGrupo(getComboText(cmbSubGrupo));
        resultado.setAlmacen(getComboText(cmbAlmacen));

        resultado.setMostrarInactivos(chkMostrarInactivos.isSelected());
        resultado.setCualquierPosicion(chkCualquierPosicion.isSelected());
        resultado.setSoloPrecioMenorCosto(chkSoloPrecioMenorCosto.isSelected());
        resultado.setDiferenteUbicacion(chkDiferente.isSelected());

        // Costo
        if (rbCostoSinCosto.isSelected()) {
            resultado.setFiltroCosto(FiltroCosto.SIN_COSTO);
        } else if (rbCostoConCosto.isSelected()) {
            resultado.setFiltroCosto(FiltroCosto.CON_COSTO);
        } else {
            resultado.setFiltroCosto(FiltroCosto.TODOS);
        }

        // Precio
        if (rbPrecioSinPrecio.isSelected()) {
            resultado.setFiltroPrecio(FiltroPrecio.SIN_PRECIO);
        } else if (rbPrecioConPrecio.isSelected()) {
            resultado.setFiltroPrecio(FiltroPrecio.CON_PRECIO);
        } else {
            resultado.setFiltroPrecio(FiltroPrecio.TODOS);
        }

        // Stock
        if (rbStockConStock.isSelected()) {
            resultado.setFiltroStock(FiltroStock.CON_STOCK);
        } else if (rbStockSinStock.isSelected()) {
            resultado.setFiltroStock(FiltroStock.SIN_STOCK);
        } else {
            resultado.setFiltroStock(FiltroStock.TODOS);
        }

        dispose();
    }

    private void quitarFiltros() {
        // Señal para eliminar todos los filtros (resultado null + flag)
        resultado = null;
        filtrosEliminados = true;
        dispose();
    }

    /**
     * Carga los campos del diálogo desde un FiltrosCriteria existente.
     */
    private void cargarCriteria(FiltrosCriteria c) {
        txtCodigo.setText(c.getCodigo());
        txtDescripcion.setText(c.getDescripcion());
        txtReferencia.setText(c.getReferencia());
        txtCodBarra.setText(c.getCodigoBarra());
        txtMarca.setText(c.getMarca());
        txtModelo.setText(c.getModelo());
        txtUbicacion.setText(c.getUbicacion());
        txtCampo1.setText(c.getCampo1());
        txtCampo2.setText(c.getCampo2());
        txtProveedor.setText(c.getProveedor());

        setComboText(cmbMoneda, c.getMoneda());
        setComboText(cmbGrupo, c.getGrupo());
        setComboText(cmbSubGrupo, c.getSubGrupo());
        setComboText(cmbAlmacen, c.getAlmacen());

        chkMostrarInactivos.setSelected(c.isMostrarInactivos());
        chkCualquierPosicion.setSelected(c.isCualquierPosicion());
        chkSoloPrecioMenorCosto.setSelected(c.isSoloPrecioMenorCosto());
        chkDiferente.setSelected(c.isDiferenteUbicacion());

        switch (c.getFiltroCosto()) {
            case SIN_COSTO -> rbCostoSinCosto.setSelected(true);
            case CON_COSTO -> rbCostoConCosto.setSelected(true);
            default -> rbCostoTodos.setSelected(true);
        }
        switch (c.getFiltroPrecio()) {
            case SIN_PRECIO -> rbPrecioSinPrecio.setSelected(true);
            case CON_PRECIO -> rbPrecioConPrecio.setSelected(true);
            default -> rbPrecioTodos.setSelected(true);
        }
        switch (c.getFiltroStock()) {
            case CON_STOCK -> rbStockConStock.setSelected(true);
            case SIN_STOCK -> rbStockSinStock.setSelected(true);
            default -> rbStockTodos.setSelected(true);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  API Pública
    // ═══════════════════════════════════════════════════════════════

    /**
     * Retorna los criterios seleccionados, o null si el diálogo fue cerrado sin aplicar.
     */
    public FiltrosCriteria getResultado() {
        return resultado;
    }

    /**
     * Indica si el usuario presionó "Quitar Filtros" (distinto de cerrar sin acción).
     */
    public boolean isFiltrosEliminados() {
        return filtrosEliminados;
    }

    // ═══════════════════════════════════════════════════════════════
    //  Builders de componentes
    // ═══════════════════════════════════════════════════════════════

    private JPanel buildFechaPanel() {
        JPanel p = new JPanel(new MigLayout("insets 0, gap 2", "[]2[]2[]2[]2[]2[]4[]", "[]"));
        p.setOpaque(false);

        JTextField dia = styledField(24);
        dia.setHorizontalAlignment(SwingConstants.CENTER);
        JTextField mes = styledField(24);
        mes.setHorizontalAlignment(SwingConstants.CENTER);
        JTextField anio = styledField(24);
        anio.setHorizontalAlignment(SwingConstants.CENTER);

        p.add(dia);
        p.add(styledLabel("/"));
        p.add(mes);
        p.add(styledLabel("/"));
        p.add(anio);

        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(0, 0, 23, 1);
        JSpinner spinner = new JSpinner(spinnerModel);
        spinner.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        spinner.setPreferredSize(new Dimension(50, 24));
        p.add(spinner);

        p.putClientProperty("dia", dia);
        p.putClientProperty("mes", mes);
        p.putClientProperty("anio", anio);
        p.putClientProperty("spinner", spinner);

        return p;
    }

    private JButton createActionButton(String icon, String text, Color bgColor) {
        JPanel content = new JPanel(new MigLayout("insets 0, wrap, align center", "[center]", "[]4[]"));
        content.setOpaque(false);

        JLabel lblIcon = new JLabel(icon);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        lblIcon.setForeground(TEXT_VALUE);
        lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
        content.add(lblIcon);

        // Texto con salto de línea
        String[] lines = text.split("\n");
        for (String line : lines) {
            JLabel lblText = new JLabel(line);
            lblText.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lblText.setForeground(TEXT_VALUE);
            lblText.setHorizontalAlignment(SwingConstants.CENTER);
            content.add(lblText);
        }

        JButton btn = new JButton();
        btn.setLayout(new BorderLayout());
        btn.add(content, BorderLayout.CENTER);
        btn.setBackground(bgColor);
        btn.setForeground(TEXT_VALUE);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        btn.setPreferredSize(new Dimension(90, 80));

        return btn;
    }

    // ═══════════════════════════════════════════════════════════════
    //  Factory helpers (estilo consistente con TasaCambioDialog)
    // ═══════════════════════════════════════════════════════════════

    private JLabel styledLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(TEXT_LABEL);
        return lbl;
    }

    private JTextField styledField(int width) {
        JTextField tf = new JTextField();
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tf.setBackground(BG_FIELD);
        tf.setForeground(TEXT_VALUE);
        tf.setCaretColor(CARET);
        tf.setPreferredSize(new Dimension(width, 26));
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        return tf;
    }

    private JComboBox<String> styledCombo(String[] items, int width) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        cb.setBackground(BG_FIELD);
        cb.setForeground(TEXT_VALUE);
        cb.setPreferredSize(new Dimension(width, 26));
        return cb;
    }

    private JCheckBox styledCheck(String text, boolean selected) {
        JCheckBox cb = new JCheckBox(text, selected);
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        cb.setForeground(TEXT_LABEL);
        cb.setOpaque(false);
        cb.setFocusPainted(false);
        return cb;
    }

    private JRadioButton styledRadio(String text, boolean selected) {
        JRadioButton rb = new JRadioButton(text, selected);
        rb.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        rb.setForeground(TEXT_LABEL);
        rb.setOpaque(false);
        rb.setFocusPainted(false);
        return rb;
    }

    private String getComboText(JComboBox<String> combo) {
        Object sel = combo.getSelectedItem();
        return sel != null ? sel.toString().trim() : "";
    }

    private void setComboText(JComboBox<String> combo, String text) {
        if (text == null || text.isEmpty()) {
            combo.setSelectedIndex(0);
        } else {
            combo.setSelectedItem(text);
        }
    }
}
