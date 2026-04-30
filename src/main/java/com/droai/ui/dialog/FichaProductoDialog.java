package com.droai.ui.dialog;

import com.droai.model.ArticuloRow;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Diálogo "Ficha Producto" — Vista detallada de un artículo del catálogo.
 * Emula la ventana del sistema Agata adaptada al estilo visual oscuro de DroAI.
 *
 * <p>Secciones:
 * <ul>
 *   <li>Identificación (Código, Descripción, Referencia, Cod.Barra, Marca, Ubicación)</li>
 *   <li>Clasificación (Grupo/Línea, Sub-Grupo/SubLínea)</li>
 *   <li>Inventario (Existencia, UdM, Destacado, Inactivo)</li>
 *   <li>Campos adicionales (Campo1–Campo6)</li>
 *   <li>Costos y Precios (4 niveles con utilidad %, Precio S/IVA, Precio C/IVA)</li>
 *   <li>IVA actual</li>
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
    private static final Color ACCENT_GREEN    = new Color(0, 210, 158);
    private static final Color TEXT_PRIMARY    = new Color(248, 250, 252);
    private static final Color TEXT_SECONDARY  = new Color(148, 163, 184);
    private static final Color TEXT_LABEL      = new Color(100, 116, 139);
    private static final Color HIGHLIGHT_GOLD  = new Color(255, 200, 80);
    private static final Color PRICE_HEADER_BG = new Color(42, 107, 255, 40);
    private static final Color ROW_ALT         = new Color(30, 38, 52);

    private final ArticuloRow articulo;

    public FichaProductoDialog(Frame owner, ArticuloRow articulo) {
        super(owner, "Ficha Producto", true);
        this.articulo = articulo;
        setSize(780, 720);
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel root = new GradientPanel();
        root.setLayout(new MigLayout(
            "insets 0, fill, wrap",
            "[grow]",
            "[]0[grow]"
        ));

        root.add(buildTitleBar(), "growx, h 52!");
        root.add(buildContent(), "grow");

        setContentPane(root);
    }

    // ═══════════════════════════════════════════════════════════════
    //  TITLE BAR
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new MigLayout("insets 12 20 12 20, fillx", "[]push[]", "[]"));
        bar.setBackground(ACCENT);

        JLabel title = new JLabel("📋  Ficha Producto");
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
        addLabelValue(grid, "Cod.Barra:", safe(articulo.getCodigoBarra()));

        addLabelValue(grid, "Marca:", safe(articulo.getModelo()));
        addLabelValue(grid, "Ubicación:", safe(articulo.getUbicacion()));

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
        addLabelValue(grid, "Proveedor:", safe(articulo.getNombreProveedor()));
        addLabelValue(grid, "Procedencia:", safe(articulo.getProcedencia()));

        section.add(grid, "growx");
        return section;
    }

    // ═══════════════════════════════════════════════════════════════
    //  SECCIÓN: Inventario
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildInventarioSection() {
        JPanel section = createSection("Inventario");

        JPanel grid = new JPanel(new MigLayout(
            "insets 12 16 12 16, fillx, gap 8 6, wrap 6",
            "[right]8[grow, fill]16[right]8[grow, fill]16[right]8[grow, fill]",
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

        // Status flags
        grid.add(fieldLabel("Destacado:"));
        grid.add(statusChip(articulo.isDestacado()), "growx, wrap");

        grid.add(fieldLabel("Peso:"));
        grid.add(valueField(fmt(articulo.getPeso())), "growx");
        grid.add(fieldLabel("Volumen:"));
        grid.add(valueField(fmt(articulo.getVolumen())), "growx");
        grid.add(fieldLabel("Inactivo:"));
        grid.add(statusChip(articulo.isAnulado()), "growx");

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

        // IVA — highlighted accent
        costos.add(fieldLabel("I.V.A.:"));
        JLabel valIva = valueField(String.format("%.2f%%", articulo.getIvaPct()));
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

        for (int i = 0; i < 4; i++) {
            double precioSiva = precios[i];
            double costoOm = articulo.getCostoOm();
            double utilPct = (costoOm > 0)
                ? ((precioSiva - costoOm) / costoOm) * 100.0
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

    private JLabel statusChip(boolean active) {
        JLabel chip = new JLabel(active ? "  Sí  " : "  No  ");
        chip.setFont(new Font("Segoe UI", Font.BOLD, 11));
        chip.setHorizontalAlignment(SwingConstants.CENTER);
        if (active) {
            chip.setForeground(ACCENT_GREEN);
            chip.setBackground(new Color(0, 210, 158, 30));
        } else {
            chip.setForeground(TEXT_SECONDARY);
            chip.setBackground(BG_FIELD);
        }
        chip.setOpaque(true);
        chip.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(active ? ACCENT_GREEN : BORDER, 1, true),
            BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        return chip;
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
