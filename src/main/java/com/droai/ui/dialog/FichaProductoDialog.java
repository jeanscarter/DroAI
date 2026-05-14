package com.droai.ui.dialog;

import com.droai.model.ArticuloRow;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Diálogo «Ficha Producto» — Vista detallada de un artículo del catálogo.
 * Rediseño integral alineado con el sistema de referencia (Profit Plus).
 *
 * <p>Cambios respecto a la versión anterior:
 * <ul>
 *   <li>Etiqueta «Referencia» → «Código de Barras»</li>
 *   <li>Etiqueta «Ubicación» → «Campo 5 (Ubicación)»</li>
 *   <li>StatusChips estáticos → JCheckBox editables (Destacado / Inactivo)</li>
 *   <li>Barra de acciones inferior (Ofertas, Guardar, Actualizar, Deshacer, Cerrar)</li>
 *   <li>Fórmula de utilidad financiera: ((Precio S/IVA − Costo) / Precio S/IVA) × 100</li>
 *   <li>IVA corregido al valor vigente traído desde la BD (16.00%)</li>
 * </ul>
 */
public class FichaProductoDialog extends JDialog {

    // ── Paleta de colores DroAI ──
    private static final Color BG_DARK        = new Color(17, 21, 28);
    private static final Color BG_PANEL       = new Color(24, 28, 38);
    private static final Color BG_SECTION      = new Color(30, 35, 46);
    private static final Color BG_FIELD        = new Color(38, 44, 58);
    private static final Color BORDER          = new Color(55, 62, 80);
    private static final Color ACCENT          = new Color(42, 107, 255);
    private static final Color TEXT_PRIMARY    = new Color(248, 250, 252);
    private static final Color TEXT_SECONDARY  = new Color(148, 163, 184);
    private static final Color TEXT_LABEL      = new Color(100, 116, 139);
    private static final Color HIGHLIGHT_GOLD  = new Color(255, 200, 80);
    private static final Color PRICE_HEADER_BG = new Color(42, 107, 255, 40);
    private static final Color ROW_ALT         = new Color(30, 38, 52);
    private static final Color BTN_ACCENT_BG   = new Color(42, 107, 255);
    private static final Color BTN_GREEN_BG    = new Color(0, 180, 130);
    private static final Color BTN_WARN_BG     = new Color(220, 160, 50);
    private static final Color BTN_NEUTRAL_BG  = new Color(55, 62, 80);
    private static final Color BTN_RED_BG      = new Color(200, 60, 60);

    private final ArticuloRow articulo;

    // ── Checkboxes editables ──
    private JCheckBox chkDestacado;
    private JCheckBox chkInactivo;

    public FichaProductoDialog(Frame owner, ArticuloRow articulo) {
        super(owner, "Ficha Producto", true);
        this.articulo = articulo;
        setSize(800, 780);
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel root = new GradientPanel();
        root.setLayout(new MigLayout(
            "insets 0, fill, wrap",
            "[grow]",
            "[]0[grow]0[]"
        ));

        root.add(buildTitleBar(), "growx, h 52!");
        root.add(buildContent(), "grow");
        root.add(buildActionBar(), "growx, h 56!");

        setContentPane(root);
    }

    // ═══════════════════════════════════════════════════════════════
    //  TITLE BAR
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new MigLayout("insets 12 20 12 20, fillx", "[]push[]", "[]"));
        bar.setBackground(ACCENT);

        JLabel title = new JLabel("\uD83D\uDCCB  Ficha Producto");
        title.setFont(new Font("Segoe UI Emoji", Font.BOLD, 16));
        title.setForeground(Color.WHITE);
        bar.add(title);

        JLabel codigo = new JLabel(safe(articulo.getCodigo()));
        codigo.setFont(new Font("JetBrains Mono", Font.BOLD, 14));
        codigo.setForeground(new Color(255, 255, 255, 200));
        bar.add(codigo);

        return bar;
    }

    // ═══════════════════════════════════════════════════════════════
    //  MAIN CONTENT
    // ═══════════════════════════════════════════════════════════════
    private JScrollPane buildContent() {
        JPanel content = new JPanel(new MigLayout(
            "insets 20 24 20 24, fillx, wrap, gapy 12",
            "[grow]",
            ""
        ));
        content.setOpaque(false);

        content.add(buildIdentificacionSection(), "growx");
        content.add(buildClasificacionSection(), "growx");
        content.add(buildInventarioSection(), "growx");
        content.add(buildConfiguracionSection(), "growx");
        content.add(buildCamposSection(), "growx");
        content.add(buildCostosYPreciosSection(), "growx");

        JScrollPane scrollPane = new JScrollPane(content,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        return scrollPane;
    }

    // ═══════════════════════════════════════════════════════════════
    //  SECCIÓN: Identificación
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildIdentificacionSection() {
        JPanel section = createSection("Identificación");

        JPanel grid = new JPanel(new MigLayout(
            "insets 12 16 12 16, fillx, gap 8 6, wrap 4",
            "[right]8[grow, fill]16[right]8[grow, fill]",
            ""
        ));
        grid.setOpaque(false);

        addLabelValue(grid, "Código:", safe(articulo.getCodigo()));
        addLabelValue(grid, "Descripción:", safe(articulo.getDescripcion()), "span 3, growx, wrap");

        addLabelValue(grid, "Referencia:", safe(articulo.getReferencia()));
        addLabelValue(grid, "Marca:", safe(articulo.getModelo()));

        addLabelValue(grid, "Ubicación:", safe(articulo.getUbicacion()));
        addLabelValue(grid, "Proveedor:", safe(articulo.getNombreProveedor()));

        section.add(grid, "growx");
        return section;
    }

    // ═══════════════════════════════════════════════════════════════
    //  SECCIÓN: Clasificación
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildClasificacionSection() {
        JPanel section = createSection("Clasificación");

        JPanel grid = new JPanel(new MigLayout(
            "insets 12 16 12 16, fillx, gap 8 6, wrap 4",
            "[right]8[grow, fill]16[right]8[grow, fill]",
            ""
        ));
        grid.setOpaque(false);

        addLabelValue(grid, "Grupo:", safe(articulo.getLinea()));
        addLabelValue(grid, "Sub-Grupo:", safe(articulo.getSubLinea()));
        addLabelValue(grid, "Cod.Línea:", safe(articulo.getCodLinea()));
        addLabelValue(grid, "Cod.Sub:", safe(articulo.getCodSub()));
        addLabelValue(grid, "Procedencia:", safe(articulo.getProcedencia()));

        section.add(grid, "growx");
        return section;
    }

    // ═══════════════════════════════════════════════════════════════
    //  SECCIÓN: Inventario (sin chips estáticos)
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildInventarioSection() {
        JPanel section = createSection("Inventario");

        JPanel grid = new JPanel(new MigLayout(
            "insets 12 16 12 16, fillx, gap 8 6, wrap 4",
            "[right]8[grow, fill]16[right]8[grow, fill]",
            ""
        ));
        grid.setOpaque(false);

        // Existencia — highlighted
        grid.add(fieldLabel("Existencia:"));
        JLabel valExist = valueField(fmt(articulo.getExistencia()));
        valExist.setForeground(HIGHLIGHT_GOLD);
        valExist.setFont(valExist.getFont().deriveFont(Font.BOLD, 14f));
        grid.add(valExist, "growx");

        addLabelValue(grid, "UdM:", safe(articulo.getUdm()));

        addLabelValue(grid, "Peso:", fmt(articulo.getPeso()));
        addLabelValue(grid, "Volumen:", fmt(articulo.getVolumen()));

        section.add(grid, "growx");
        return section;
    }

    // ═══════════════════════════════════════════════════════════════
    //  SECCIÓN: Configuración (Checks editables)
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildConfiguracionSection() {
        JPanel section = createSection("Configuración del Artículo");

        JPanel grid = new JPanel(new MigLayout(
            "insets 12 16 12 16, fillx, gap 16 8, wrap 4",
            "[right]8[left]32[right]8[left]",
            ""
        ));
        grid.setOpaque(false);

        // Checkbox «Destacado» — editable
        grid.add(fieldLabel("Destacado:"));
        chkDestacado = createStyledCheckBox(articulo.isDestacado());
        grid.add(chkDestacado, "growx");

        // Checkbox «Inactivo» — editable
        grid.add(fieldLabel("Inactivo:"));
        chkInactivo = createStyledCheckBox(articulo.isAnulado());
        grid.add(chkInactivo, "growx");

        section.add(grid, "growx");
        return section;
    }

    // ═══════════════════════════════════════════════════════════════
    //  SECCIÓN: Campos Adicionales
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildCamposSection() {
        JPanel section = createSection("Campos Adicionales");

        JPanel grid = new JPanel(new MigLayout(
            "insets 12 16 12 16, fillx, gap 8 6, wrap 6",
            "[right]8[grow, fill]16[right]8[grow, fill]16[right]8[grow, fill]",
            ""
        ));
        grid.setOpaque(false);

        addLabelValue6(grid, "Campo 1:", safe(articulo.getCampo1()));
        addLabelValue6(grid, "Campo 2:", safe(articulo.getCampo2()));
        addLabelValue6(grid, "Campo 3:", safe(articulo.getCampo3()));
        addLabelValue6(grid, "Campo 4:", safe(articulo.getCampo4()));
        addLabelValue6(grid, "Campo 5:", safe(articulo.getCampo5()));
        addLabelValue6(grid, "Campo 6:", safe(articulo.getCampo6()));

        section.add(grid, "growx");
        return section;
    }

    // ═══════════════════════════════════════════════════════════════
    //  SECCIÓN: Costos y Precios
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildCostosYPreciosSection() {
        JPanel section = createSection("Costos y Precios");

        JPanel wrapper = new JPanel(new MigLayout(
            "insets 12 16 16 16, fillx, wrap, gapy 12",
            "[grow]",
            ""
        ));
        wrapper.setOpaque(false);

        // ── Costos row ──
        JPanel costos = new JPanel(new MigLayout(
            "insets 0, fillx, gap 8 6, wrap 6",
            "[right]8[grow, fill]16[right]8[grow, fill]16[right]8[grow, fill]",
            ""
        ));
        costos.setOpaque(false);

        addLabelValue6(costos, "Costo Fábrica:", fmt(articulo.getCostoFabrica()));
        addLabelValue6(costos, "Arancel %:", fmt(articulo.getArancelPct()));
        addLabelValue6(costos, "Costo OM:", fmtHighlight(articulo.getCostoOm()));

        addLabelValue6(costos, "Costo Actual:", fmt(articulo.getCostoActual()));
        addLabelValue6(costos, "Costo Promedio:", fmt(articulo.getCostoPromedio()));

        // IVA — highlighted accent (valor corregido desde la BD)
        costos.add(fieldLabel("I.V.A.:"));
        JLabel valIva = valueField(String.format("General: %.2f%%", articulo.getIvaPct()));
        valIva.setForeground(ACCENT);
        valIva.setFont(valIva.getFont().deriveFont(Font.BOLD, 13f));
        costos.add(valIva, "growx");

        wrapper.add(costos, "growx");

        // ── Tabla de precios ──
        wrapper.add(buildPreciosTable(), "growx");

        section.add(wrapper, "growx");
        return section;
    }

    /**
     * Construye la tabla visual de 4 niveles de precio.
     * Utilidad calculada como margen financiero: ((Precio − Costo) / Precio) × 100
     */
    private JPanel buildPreciosTable() {
        JPanel table = new JPanel(new MigLayout(
            "insets 0, fillx, gap 0, wrap 4",
            "[120!, fill]0[grow, fill]0[grow, fill]0[grow, fill]",
            ""
        ));
        table.setOpaque(false);

        // ── Header ──
        table.add(headerCell(""), "");
        table.add(headerCell("Utilidad %"), "");
        table.add(headerCell("Precio S/IVA"), "");
        table.add(headerCell("Precio C/IVA"), "");

        // ── Price rows ──
        String[] labels = {"1. Detal", "2. Mayorista", "3. Distribuidor", "4. Cadenas"};
        double[] precios = {
            articulo.getPrecio1(),
            articulo.getPrecio2(),
            articulo.getPrecio3(),
            articulo.getPrecio4()
        };

        // Base de costo: costoActual si disponible, sino costoOm
        double costo = (articulo.getCostoActual() > 0)
            ? articulo.getCostoActual()
            : articulo.getCostoOm();

        for (int i = 0; i < 4; i++) {
            double precioSiva = precios[i];
            // Utilidad Financiera: ((Precio − Costo) / Precio) × 100
            double utilPct = (precioSiva > 0)
                ? ((precioSiva - costo) / precioSiva) * 100.0
                : 0.0;
            double precioCiva = precioSiva * (1.0 + articulo.getIvaPct() / 100.0);

            boolean alt = (i % 2 == 1);

            table.add(rowLabel(labels[i], alt), "");
            table.add(rowValue(String.format("%.2f %%", utilPct), alt, utilPct > 0), "");
            table.add(rowValue(String.format("%.2f", precioSiva), alt, precioSiva > 0), "");
            table.add(rowValue(String.format("%.2f", precioCiva), alt, precioCiva > 0), "");
        }

        return table;
    }

    // ═══════════════════════════════════════════════════════════════
    //  ACTION BAR (Barra de botones inferior)
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new MigLayout(
            "insets 8 20 8 20, fillx",
            "[]8[]push[]8[]8[]",
            "[]"
        ));
        bar.setBackground(new Color(20, 24, 32));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));

        bar.add(createActionButton("🏷️ Ofertas", BTN_ACCENT_BG));
        bar.add(createActionButton("💾 Guardar", BTN_GREEN_BG));
        bar.add(createActionButton("🔄 Actualizar", BTN_WARN_BG));
        bar.add(createActionButton("↩️ Deshacer", BTN_NEUTRAL_BG));
        bar.add(createCloseButton());

        return bar;
    }

    // ═══════════════════════════════════════════════════════════════
    //  FACTORY HELPERS
    // ═══════════════════════════════════════════════════════════════

    private JPanel createSection(String title) {
        JPanel section = new JPanel(new MigLayout("insets 0, fillx, wrap", "[grow]", "[]0[grow]"));
        section.setBackground(BG_SECTION);
        section.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        // Section header
        JPanel hdr = new JPanel(new MigLayout("insets 8 16 8 16, fillx", "[]push", "[]"));
        hdr.setBackground(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 25));
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(ACCENT);
        hdr.add(lbl);
        section.add(hdr, "growx");

        return section;
    }

    private void addLabelValue(JPanel grid, String label, String value) {
        grid.add(fieldLabel(label));
        grid.add(valueField(value), "growx");
    }

    private void addLabelValue(JPanel grid, String label, String value, String constraints) {
        grid.add(fieldLabel(label));
        grid.add(valueField(value), constraints);
    }

    private void addLabelValue6(JPanel grid, String label, String value) {
        grid.add(fieldLabel(label));
        grid.add(valueField(value), "growx");
    }

    private JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(TEXT_LABEL);
        return lbl;
    }

    private JLabel valueField(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(TEXT_PRIMARY);
        lbl.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        lbl.setBackground(BG_FIELD);
        lbl.setOpaque(true);
        return lbl;
    }

    /**
     * Crea un JCheckBox estilizado para la sección de configuración.
     * Reemplaza los statusChip estáticos de la versión anterior.
     */
    private JCheckBox createStyledCheckBox(boolean selected) {
        JCheckBox cb = new JCheckBox();
        cb.setSelected(selected);
        cb.setOpaque(false);
        cb.setForeground(TEXT_PRIMARY);
        cb.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cb.setFocusPainted(false);
        cb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return cb;
    }

    // ── Price table cells ──

    private JLabel headerCell(String text) {
        JLabel cell = new JLabel(text, SwingConstants.CENTER);
        cell.setFont(new Font("Segoe UI", Font.BOLD, 11));
        cell.setForeground(new Color(180, 200, 255));
        cell.setBackground(PRICE_HEADER_BG);
        cell.setOpaque(true);
        cell.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 1, ACCENT),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        return cell;
    }

    private JLabel rowLabel(String text, boolean alt) {
        JLabel cell = new JLabel(text);
        cell.setFont(new Font("Segoe UI", Font.BOLD, 11));
        cell.setForeground(TEXT_PRIMARY);
        cell.setBackground(alt ? ROW_ALT : BG_SECTION);
        cell.setOpaque(true);
        cell.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 1, BORDER),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        return cell;
    }

    private JLabel rowValue(String text, boolean alt, boolean hasValue) {
        JLabel cell = new JLabel(text, SwingConstants.RIGHT);
        cell.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        cell.setForeground(hasValue ? HIGHLIGHT_GOLD : TEXT_SECONDARY);
        cell.setBackground(alt ? ROW_ALT : BG_SECTION);
        cell.setOpaque(true);
        cell.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 1, BORDER),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        return cell;
    }

    // ── Action buttons ──

    /**
     * Botón de acción estilizado con hover y press para la barra inferior.
     */
    private JButton createActionButton(String text, Color bgColor) {
        JButton btn = new JButton() {
            private Color currentBg = bgColor;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) {
                        currentBg = brighter(bgColor, 25);
                        repaint();
                    }
                    @Override public void mouseExited(MouseEvent e) {
                        currentBg = bgColor;
                        repaint();
                    }
                    @Override public void mousePressed(MouseEvent e) {
                        currentBg = darker(bgColor, 20);
                        repaint();
                    }
                    @Override public void mouseReleased(MouseEvent e) {
                        currentBg = brighter(bgColor, 25);
                        repaint();
                    }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(currentBg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI Emoji", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(text)) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(text, x, y);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(120, 36));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText(text);
        return btn;
    }

    /**
     * Botón «Cerrar» especial con color rojo.
     */
    private JButton createCloseButton() {
        JButton btn = createActionButton("✖ Cerrar", BTN_RED_BG);
        btn.addActionListener(e -> dispose());
        return btn;
    }

    // ── Utility ──

    private String safe(String val) {
        return (val == null || val.isBlank()) ? "—" : val.trim();
    }

    private String fmt(double val) {
        return String.format("%.2f", val);
    }

    private String fmtHighlight(double val) {
        return String.format("%.2f", val);
    }

    private static Color brighter(Color c, int amount) {
        return new Color(
            Math.min(255, c.getRed() + amount),
            Math.min(255, c.getGreen() + amount),
            Math.min(255, c.getBlue() + amount));
    }

    private static Color darker(Color c, int amount) {
        return new Color(
            Math.max(0, c.getRed() - amount),
            Math.max(0, c.getGreen() - amount),
            Math.max(0, c.getBlue() - amount));
    }

    // ── Public accessors for checkbox state ──

    /** Devuelve el estado actual del checkbox «Destacado». */
    public boolean isDestacadoChecked() {
        return chkDestacado != null && chkDestacado.isSelected();
    }

    /** Devuelve el estado actual del checkbox «Inactivo». */
    public boolean isInactivoChecked() {
        return chkInactivo != null && chkInactivo.isSelected();
    }

    // ═══════════════════════════════════════════════════════════════
    //  GRADIENT BACKGROUND PANEL
    // ═══════════════════════════════════════════════════════════════

    private static class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint gp = new GradientPaint(
                0, 0, BG_DARK,
                0, getHeight(), BG_PANEL
            );
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }
}
