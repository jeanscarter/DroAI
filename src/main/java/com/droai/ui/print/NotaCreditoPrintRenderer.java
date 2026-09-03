package com.droai.ui.print;

import com.droai.model.NotaCreditoModel;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Renderizador de impresión vectorial para Notas de Crédito de Profit Plus.
 * Reproduce con máxima fidelidad el formato oficial preimpreso/fiscal de la empresa.
 */
public class NotaCreditoPrintRenderer implements Printable {

    private final NotaCreditoModel model;
    private final boolean enDolares;
    private static final DecimalFormat DF;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.of("es", "VE"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        DF = new DecimalFormat("#,##0.00", symbols);
    }

    public NotaCreditoPrintRenderer(NotaCreditoModel model) {
        this(model, false);
    }

    public NotaCreditoPrintRenderer(NotaCreditoModel model, boolean enDolares) {
        this.model = model;
        this.enDolares = enDolares;
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex > 0) {
            return NO_SUCH_PAGE;
        }

        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        paintDocument(g2d, (int) pageFormat.getImageableWidth(), (int) pageFormat.getImageableHeight());

        return PAGE_EXISTS;
    }

    /**
     * Dibuja el formato completo sobre el Graphics2D dado.
     * Puede ser usado tanto para imprimir directamente como para renderizar la vista previa en pantalla.
     */
    public void paintDocument(Graphics2D g2, int width, int height) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        // Fondo blanco
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);

        // Tipografías oficiales Profit Plus (Familia Arial con jerarquía exacta)
        Font fontRegular = new Font("Arial", Font.PLAIN, 8);
        Font fontBold = new Font("Arial", Font.BOLD, 8);
        Font fontHeaderBold = new Font("Arial", Font.BOLD, 9);
        Font fontLargeBold = new Font("Arial", Font.BOLD, 11);
        Font fontAddress = new Font("Arial", Font.PLAIN, 7);

        g2.setColor(Color.BLACK);

        int startX = 28;
        int totalW = 732; // Ancho oficial Carta Horizontal de RepFormatoDocumentoVentaNCR-DRO.rpt (732 pt)

        // ═══════════════════════════════════════════════════════════
        // 1. RECUADRO SUPERIOR DERECHO: DATOS DE CONTROL Y EMISIÓN
        // ═══════════════════════════════════════════════════════════
        int topBoxW = 222;
        int topBoxH = 76;
        int topBoxX = startX + totalW - topBoxW;
        int topBoxY = 162;

        g2.draw(new RoundRectangle2D.Float(topBoxX, topBoxY, topBoxW, topBoxH, 8, 8));

        int rX = topBoxX + 10;
        int vX = topBoxX + topBoxW - 10;
        int rY = topBoxY + 17;

        g2.setFont(fontBold);
        g2.drawString("NOTA DE CREDITO Nº:", rX, rY);
        g2.setFont(fontLargeBold);
        drawRightAlignedString(g2, model.getNumeroImpresion(), vX, rY);

        rY += 15;
        g2.setFont(fontBold);
        g2.drawString("CONTROL Nº:", rX, rY);
        g2.setFont(fontRegular);
        drawRightAlignedString(g2, model.getNControl() != null ? model.getNControl() : "", vX, rY);

        rY += 14;
        g2.setFont(fontBold);
        g2.drawString("Fecha Emisión:", rX, rY);
        g2.setFont(fontRegular);
        String fecEmisStr = model.getFecEmis() != null ? model.getFecEmis().format(DTF) : "";
        drawRightAlignedString(g2, fecEmisStr, vX, rY);

        rY += 14;
        g2.setFont(fontBold);
        g2.drawString("Vendedor:", rX, rY);
        g2.setFont(fontRegular);
        drawRightAlignedString(g2, model.getVenDes() != null ? model.getVenDes() : "", vX, rY);

        // ═══════════════════════════════════════════════════════════
        // 2. BLOQUE SUPERIOR IZQUIERDO: DATOS DEL CLIENTE
        // ═══════════════════════════════════════════════════════════
        int cX = startX + 6;
        int cY = 168;
        int vClientX = startX + 62;

        g2.setFont(fontBold);
        g2.drawString("Cliente :", cX, cY);
        g2.setFont(fontRegular);
        g2.drawString(model.getCliDes() != null ? model.getCliDes() : "", vClientX, cY);

        cY += 14;
        g2.setFont(fontBold);
        g2.drawString("R.I.F.:", cX, cY);
        g2.setFont(fontRegular);
        g2.drawString(model.getRif() != null ? model.getRif() : "", vClientX, cY);

        cY += 14;
        g2.setFont(fontBold);
        g2.drawString("Dirección:", cX, cY);
        g2.setFont(fontAddress);
        g2.drawString(model.getDireccion() != null ? model.getDireccion() : "", vClientX, cY);

        cY += 14;
        g2.setFont(fontBold);
        g2.drawString("Teléfonos:", cX, cY);
        g2.setFont(fontRegular);
        g2.drawString(model.getTelefonos() != null ? model.getTelefonos() : "", vClientX, cY);

        // ═══════════════════════════════════════════════════════════
        // 3. BARRA GRIS SUPERIOR: POR CONCEPTO DE
        // ═══════════════════════════════════════════════════════════
        int bannerY = 246;
        int bannerH = 18;
        int centerBoxY = bannerY + bannerH;
        int centerBoxH = 122;

        g2.setColor(new Color(228, 228, 228));
        g2.fill(new RoundRectangle2D.Float(startX, bannerY, totalW, bannerH + 8, 8, 8));
        g2.fillRect(startX, bannerY + 8, totalW, bannerH);
        g2.setColor(Color.BLACK);
        g2.draw(new RoundRectangle2D.Float(startX, bannerY, totalW, bannerH + 8, 8, 8));
        g2.drawLine(startX, bannerY + bannerH, startX + totalW, bannerY + bannerH);

        g2.setFont(fontBold);
        drawCenteredString(g2, "POR CONCEPTO DE:", startX + (totalW / 2), bannerY + 13);

        // ═══════════════════════════════════════════════════════════
        // 4. RECUADRO CENTRAL: INFORMACIÓN DOCUMENTO AFECTADO
        // ═══════════════════════════════════════════════════════════
        g2.draw(new RoundRectangle2D.Float(startX, centerBoxY, totalW, centerBoxH, 8, 8));

        // Concepto / Descripción en negrita centrado
        g2.setFont(fontHeaderBold);
        String desc = model.getDescripcion() != null ? model.getDescripcion().toUpperCase() : "";
        drawCenteredString(g2, desc, startX + (totalW / 2), centerBoxY + 19);

        // Subtítulo
        g2.setFont(fontBold);
        drawCenteredString(g2, "Información del Documento afectado :", startX + (totalW / 2), centerBoxY + 36);

        // Detalle Factura Afectada (Izquierda)
        int col1LblX = startX + 115;
        int col1ValX = startX + 245;
        int factY = centerBoxY + 56;

        g2.setFont(fontBold);
        g2.drawString("Factura Afectada:", col1LblX + 45, factY);

        factY += 15;
        g2.drawString("Numero.", col1LblX, factY);
        g2.setFont(fontRegular);
        drawRightAlignedString(g2, model.getNroOrig() != null ? model.getNroOrig() : "", col1ValX, factY);

        factY += 14;
        g2.setFont(fontBold);
        g2.drawString("Subtotal:", col1LblX, factY);
        g2.setFont(fontRegular);
        double subOrig = enDolares ? (model.getSubtotalOrigBs() / model.getTasa()) : model.getSubtotalOrigBs();
        drawRightAlignedString(g2, DF.format(subOrig), col1ValX, factY);

        factY += 14;
        g2.setFont(fontBold);
        g2.drawString("I.V.A.:", col1LblX, factY);
        g2.setFont(fontRegular);
        double ivaOrig = enDolares ? (model.getIvaOrigBs() / model.getTasa()) : model.getIvaOrigBs();
        drawRightAlignedString(g2, DF.format(ivaOrig), col1ValX, factY);

        factY += 14;
        g2.setFont(fontBold);
        g2.drawString("Total:", col1LblX, factY);
        g2.setFont(fontRegular);
        double totOrig = enDolares ? (model.getTotalOrigBs() / model.getTasa()) : model.getTotalOrigBs();
        drawRightAlignedString(g2, DF.format(totOrig), col1ValX, factY);

        // Detalle Factura Afectada (Derecha: Fecha)
        int col2CenterX = startX + totalW - 200;
        int fecFactY = centerBoxY + 56;
        g2.setFont(fontBold);
        String lblFecFact = "Fecha Factura Afectada :";
        drawCenteredString(g2, lblFecFact, col2CenterX, fecFactY);
        g2.setFont(fontRegular);
        String fecOrigStr = model.getFecEmisOrig() != null ? model.getFecEmisOrig().format(DTF) : "";
        drawCenteredString(g2, fecOrigStr, col2CenterX, fecFactY + 16);

        // ═══════════════════════════════════════════════════════════
        // 5. RECUADRO INFERIOR DERECHO: TOTALES Y ALÍCUOTAS
        // ═══════════════════════════════════════════════════════════
        int totBoxW = 260;
        int totBoxH = 72;
        int totBoxX = startX + totalW - totBoxW;
        int totBoxY = centerBoxY + centerBoxH + 18;

        g2.draw(new RoundRectangle2D.Float(totBoxX, totBoxY, totBoxW, totBoxH, 8, 8));

        int tRowX = totBoxX + 12;
        int tValX = totBoxX + totBoxW - 12;
        int tRowY = totBoxY + 17;

        // Base Imponible
        g2.setFont(fontBold);
        g2.drawString("Base Imponible:", tRowX, tRowY);
        g2.setFont(fontRegular);
        double baseImp = enDolares ? model.getBaseImponibleUsd() : model.getBaseImponibleBs();
        if (baseImp == 0.0 && (enDolares ? model.getMontoExentoUsd() : model.getMontoExentoBs()) > 0) {
            baseImp = enDolares ? model.getMontoExentoUsd() : model.getMontoExentoBs();
        }
        drawRightAlignedString(g2, DF.format(baseImp), tValX, tRowY);

        // I.V.A.
        tRowY += 14;
        g2.setFont(fontBold);
        g2.drawString("I.V.A.:", tRowX, tRowY);
        g2.setFont(fontRegular);
        double ivaVal = enDolares ? model.getIvaUsd() : model.getIvaBs();
        drawRightAlignedString(g2, DF.format(ivaVal), tValX, tRowY);

        // Monto Exento
        tRowY += 14;
        g2.setFont(fontBold);
        g2.drawString("Monto Exento:", tRowX, tRowY);
        g2.setFont(fontRegular);
        double exentoVal = enDolares ? model.getMontoExentoUsd() : model.getMontoExentoBs();
        drawRightAlignedString(g2, DF.format(exentoVal), tValX, tRowY);

        // Neto
        tRowY += 15;
        g2.setFont(fontHeaderBold);
        g2.drawString("Neto:", tRowX, tRowY);
        double netoVal = enDolares ? model.getMontoNetoUsd() : model.getMontoNetoBs();
        drawRightAlignedString(g2, DF.format(netoVal), tValX, tRowY);

        // Marca de moneda si es USD
        if (enDolares) {
            g2.setFont(new Font("Arial", Font.ITALIC, 7));
            g2.drawString("* Valores expresados en Dólares Americanos (USD) a tasa " + DF.format(model.getTasa()), startX, totBoxY + totBoxH + 12);
        }
    }

    private void drawCenteredString(Graphics2D g, String text, int centerX, int y) {
        if (text == null) return;
        FontMetrics fm = g.getFontMetrics();
        int x = centerX - (fm.stringWidth(text) / 2);
        g.drawString(text, x, y);
    }

    private void drawRightAlignedString(Graphics2D g, String text, int rightX, int y) {
        if (text == null) return;
        FontMetrics fm = g.getFontMetrics();
        int x = rightX - fm.stringWidth(text);
        g.drawString(text, x, y);
    }
}
