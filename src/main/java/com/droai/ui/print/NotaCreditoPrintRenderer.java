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
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Fondo blanco
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);

        // Tipografías
        Font fontRegular = new Font("Arial", Font.PLAIN, 8);
        Font fontBold = new Font("Arial", Font.BOLD, 8);
        Font fontHeaderBold = new Font("Arial", Font.BOLD, 9);
        Font fontTitleBold = new Font("Arial", Font.BOLD, 10);
        Font fontNumbers = new Font("Arial", Font.PLAIN, 8);
        Font fontNumbersBold = new Font("Arial", Font.BOLD, 8);

        g2.setColor(Color.BLACK);

        // ═══════════════════════════════════════════════════════════
        // 1. BLOQUE SUPERIOR IZQUIERDO: DATOS DEL CLIENTE
        // ═══════════════════════════════════════════════════════════
        int startX = 32;
        int clientY = 195;

        g2.setFont(fontBold);
        g2.drawString("Cliente :", startX, clientY);
        g2.setFont(fontRegular);
        g2.drawString(model.getCliDes() != null ? model.getCliDes() : "", startX + 55, clientY);

        clientY += 14;
        g2.setFont(fontBold);
        g2.drawString("R.I.F.:", startX, clientY);
        g2.setFont(fontRegular);
        g2.drawString(model.getRif() != null ? model.getRif() : "", startX + 55, clientY);

        clientY += 14;
        g2.setFont(fontBold);
        g2.drawString("Dirección:", startX, clientY);
        g2.setFont(fontRegular);
        
        // Manejar salto de línea en dirección solo si excede el ancho disponible antes del recuadro de control
        String dir = model.getDireccion() != null ? model.getDireccion() : "";
        int maxDirWidth = (width - startX - 225) - (startX + 55);
        FontMetrics fmReg = g2.getFontMetrics(fontRegular);
        if (fmReg.stringWidth(dir) > maxDirWidth && dir.length() > 60) {
            int splitIdx = 60;
            int spaceIdx = dir.lastIndexOf(' ', splitIdx);
            if (spaceIdx > 35) splitIdx = spaceIdx;
            String l1 = dir.substring(0, splitIdx).trim();
            String l2 = dir.substring(splitIdx).trim();
            g2.drawString(l1, startX + 55, clientY);
            clientY += 11;
            g2.drawString(l2, startX + 55, clientY);
        } else {
            g2.drawString(dir, startX + 55, clientY);
        }

        clientY += 14;
        g2.setFont(fontBold);
        g2.drawString("Teléfonos:", startX, clientY);
        g2.setFont(fontRegular);
        g2.drawString(model.getTelefonos() != null ? model.getTelefonos() : "", startX + 55, clientY);

        // ═══════════════════════════════════════════════════════════
        // 2. RECUADRO SUPERIOR DERECHO: DATOS DE CONTROL Y EMISIÓN
        // ═══════════════════════════════════════════════════════════
        int boxRightW = 215;
        int boxRightH = 74;
        int boxRightX = width - startX - boxRightW;
        int boxRightY = 185;

        g2.draw(new RoundRectangle2D.Float(boxRightX, boxRightY, boxRightW, boxRightH, 12, 12));

        int innerX = boxRightX + 10;
        int rightValX = boxRightX + boxRightW - 10;
        int innerY = boxRightY + 16;

        g2.setFont(fontBold);
        g2.drawString("NOTA DE CREDITO Nº:", innerX, innerY);
        g2.setFont(fontTitleBold);
        String numNota = model.getNumeroImpresion();
        drawRightAlignedString(g2, numNota, rightValX, innerY);

        innerY += 14;
        g2.setFont(fontBold);
        g2.drawString("CONTROL Nº:", innerX, innerY);
        g2.setFont(fontRegular);
        drawRightAlignedString(g2, model.getNControl() != null ? model.getNControl() : "", rightValX, innerY);

        innerY += 14;
        g2.setFont(fontBold);
        g2.drawString("Fecha Emisión:", innerX, innerY);
        g2.setFont(fontRegular);
        String fecEmisStr = model.getFecEmis() != null ? model.getFecEmis().format(DTF) : "";
        drawRightAlignedString(g2, fecEmisStr, rightValX, innerY);

        innerY += 14;
        g2.setFont(fontBold);
        g2.drawString("Vendedor:", innerX, innerY);
        g2.setFont(fontRegular);
        drawRightAlignedString(g2, model.getVenDes() != null ? model.getVenDes() : "", rightValX, innerY);

        // ═══════════════════════════════════════════════════════════
        // 3. BARRA GRIS: POR CONCEPTO DE
        // ═══════════════════════════════════════════════════════════
        int bannerX = startX;
        int bannerY = 270;
        int bannerW = width - (startX * 2);
        int bannerH = 16;

        g2.setColor(new Color(228, 228, 228));
        g2.fill(new RoundRectangle2D.Float(bannerX, bannerY, bannerW, bannerH, 8, 8));

        g2.setColor(Color.BLACK);
        g2.draw(new RoundRectangle2D.Float(bannerX, bannerY, bannerW, bannerH, 8, 8));

        g2.setFont(fontHeaderBold);
        drawCenteredString(g2, "POR CONCEPTO DE:", bannerX + (bannerW / 2), bannerY + 11);

        // ═══════════════════════════════════════════════════════════
        // 4. RECUADRO CENTRAL: INFORMACIÓN DOCUMENTO AFECTADO
        // ═══════════════════════════════════════════════════════════
        int centerBoxX = startX;
        int centerBoxY = bannerY + bannerH;
        int centerBoxW = bannerW;
        int centerBoxH = 118;

        g2.draw(new RoundRectangle2D.Float(centerBoxX, centerBoxY, centerBoxW, centerBoxH, 8, 8));

        // Concepto / Descripción en negrita centrado
        g2.setFont(fontTitleBold);
        int descY = centerBoxY + 19;
        String desc = model.getDescripcion() != null ? model.getDescripcion().toUpperCase() : "";
        drawCenteredString(g2, desc, centerBoxX + (centerBoxW / 2), descY);

        // Subtítulo
        g2.setFont(fontHeaderBold);
        int subY = descY + 15;
        drawCenteredString(g2, "Información del Documento afectado :", centerBoxX + (centerBoxW / 2), subY);

        // Detalle Factura Afectada (Izquierda)
        int col1X = centerBoxX + 130;
        int factY = subY + 20;

        g2.setFont(fontBold);
        g2.drawString("Factura Afectada:", col1X, factY);

        int col1LabelX = col1X - 50;
        int col1ValX = col1X + 115;

        factY += 14;
        g2.setFont(fontBold);
        g2.drawString("Numero.", col1LabelX, factY);
        g2.setFont(fontRegular);
        drawRightAlignedString(g2, model.getNroOrig() != null ? model.getNroOrig() : "", col1ValX, factY);

        factY += 13;
        g2.setFont(fontBold);
        g2.drawString("Subtotal:", col1LabelX, factY);
        g2.setFont(fontNumbers);
        double subOrig = enDolares ? (model.getSubtotalOrigBs() / model.getTasa()) : model.getSubtotalOrigBs();
        drawRightAlignedString(g2, DF.format(subOrig), col1ValX, factY);

        factY += 13;
        g2.setFont(fontBold);
        g2.drawString("I.V.A.:", col1LabelX, factY);
        g2.setFont(fontNumbers);
        double ivaOrig = enDolares ? (model.getIvaOrigBs() / model.getTasa()) : model.getIvaOrigBs();
        drawRightAlignedString(g2, DF.format(ivaOrig), col1ValX, factY);

        factY += 13;
        g2.setFont(fontBold);
        g2.drawString("Total:", col1LabelX, factY);
        g2.setFont(fontNumbers);
        double totOrig = enDolares ? (model.getTotalOrigBs() / model.getTasa()) : model.getTotalOrigBs();
        drawRightAlignedString(g2, DF.format(totOrig), col1ValX, factY);

        // Detalle Factura Afectada (Derecha: Fecha)
        int col2X = centerBoxX + (centerBoxW / 2) + 60;
        int fecFactY = subY + 20;
        g2.setFont(fontBold);
        String lblFecFact = "Fecha Factura Afectada :";
        g2.drawString(lblFecFact, col2X, fecFactY);
        g2.setFont(fontRegular);
        String fecOrigStr = model.getFecEmisOrig() != null ? model.getFecEmisOrig().format(DTF) : "";
        if (!fecOrigStr.isEmpty()) {
            FontMetrics fmBold = g2.getFontMetrics(fontBold);
            int lblW = fmBold.stringWidth(lblFecFact);
            FontMetrics fmDate = g2.getFontMetrics(fontRegular);
            int dateW = fmDate.stringWidth(fecOrigStr);
            int dateX = col2X + (lblW / 2) - (dateW / 2);
            g2.drawString(fecOrigStr, dateX, fecFactY + 14);
        }

        // ═══════════════════════════════════════════════════════════
        // 5. RECUADRO INFERIOR DERECHO: TOTALES Y ALÍCUOTAS
        // ═══════════════════════════════════════════════════════════
        int totBoxW = 255;
        int totBoxH = 68;
        int totBoxX = width - startX - totBoxW;
        int totBoxY = centerBoxY + centerBoxH + 16;

        g2.draw(new RoundRectangle2D.Float(totBoxX, totBoxY, totBoxW, totBoxH, 10, 10));

        int tRowX = totBoxX + 12;
        int tRowY = totBoxY + 15;
        int tValX = totBoxX + totBoxW - 12;

        // Base Imponible
        g2.setFont(fontBold);
        g2.drawString("Base Imponible:", tRowX, tRowY);
        g2.setFont(fontNumbers);
        double baseImp = enDolares ? model.getBaseImponibleUsd() : model.getBaseImponibleBs();
        drawRightAlignedString(g2, DF.format(baseImp), tValX, tRowY);

        // I.V.A.
        tRowY += 13;
        g2.setFont(fontBold);
        g2.drawString("I.V.A.:", tRowX, tRowY);
        g2.setFont(fontNumbers);
        double ivaVal = enDolares ? model.getIvaUsd() : model.getIvaBs();
        drawRightAlignedString(g2, DF.format(ivaVal), tValX, tRowY);

        // Monto Exento
        tRowY += 13;
        g2.setFont(fontBold);
        g2.drawString("Monto Exento:", tRowX, tRowY);
        g2.setFont(fontNumbers);
        double exentoVal = enDolares ? model.getMontoExentoUsd() : model.getMontoExentoBs();
        drawRightAlignedString(g2, DF.format(exentoVal), tValX, tRowY);

        // Neto
        tRowY += 14;
        g2.setFont(fontBold);
        g2.drawString("Neto:", tRowX, tRowY);
        g2.setFont(fontNumbersBold);
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
