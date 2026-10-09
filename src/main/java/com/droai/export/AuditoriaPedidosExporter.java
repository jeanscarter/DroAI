package com.droai.export;

import com.droai.model.AuditoriaPedidoRenglonRow;
import com.droai.model.AuditoriaPedidoRow;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Exportador oficial a Excel para la Auditoría de Pedidos Web vs. Facturación SIAD.
 */
public class AuditoriaPedidosExporter {

    public static void exportar(List<AuditoriaPedidoRow> pedidos, File destino) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheetPedidos = wb.createSheet("Resumen Pedidos");
            XSSFSheet sheetDetalle = wb.createSheet("Detalle Renglones");

            // ── Estilos ──
            XSSFCellStyle headerStyle = wb.createCellStyle();
            XSSFFont headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(30, 41, 59), null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            XSSFCellStyle alertStyle = wb.createCellStyle();
            XSSFFont alertFont = wb.createFont();
            alertFont.setBold(true);
            alertFont.setColor(new XSSFColor(new java.awt.Color(239, 68, 68), null));
            alertStyle.setFont(alertFont);

            XSSFCellStyle moneyStyle = wb.createCellStyle();
            DataFormat df = wb.createDataFormat();
            moneyStyle.setDataFormat(df.getFormat("$#,##0.00"));

            XSSFCellStyle numStyle = wb.createCellStyle();
            numStyle.setDataFormat(df.getFormat("#,##0"));

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

            // ── Hoja 1: Resumen Pedidos ──
            String[] headersPedidos = {
                    "Estado", "Diagnóstico", "Pedido Web", "Nota Entrega", "Factura(s)",
                    "Fecha Emisión", "Código Cliente", "Cliente", "Vendedor",
                    "Reng Web", "Reng Nota", "Reng Fact", "Reng Faltantes",
                    "Und Web", "Und Nota", "Und Fact", "Und Faltantes",
                    "Monto Total ($)", "Monto Facturado ($)", "Monto Pendiente ($)"
            };

            Row hRow1 = sheetPedidos.createRow(0);
            hRow1.setHeightInPoints(24);
            for (int i = 0; i < headersPedidos.length; i++) {
                Cell cell = hRow1.createCell(i);
                cell.setCellValue(headersPedidos[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx1 = 1;
            for (AuditoriaPedidoRow p : pedidos) {
                Row r = sheetPedidos.createRow(rowIdx1++);
                int col = 0;
                r.createCell(col++).setCellValue(p.getEstado());
                r.createCell(col++).setCellValue(p.getDiagnostico());
                r.createCell(col++).setCellValue(p.getPedidoWeb());
                r.createCell(col++).setCellValue(p.getNotaEntrega());
                r.createCell(col++).setCellValue(p.getFacturas());
                r.createCell(col++).setCellValue(p.getFecEmis() != null ? sdf.format(p.getFecEmis()) : "");
                r.createCell(col++).setCellValue(p.getCoCli());
                r.createCell(col++).setCellValue(p.getCliDes());
                r.createCell(col++).setCellValue(p.getVenDes());

                Cell c;
                (c = r.createCell(col++)).setCellValue(p.getTotalRengWeb()); c.setCellStyle(numStyle);
                (c = r.createCell(col++)).setCellValue(p.getTotalRengNota()); c.setCellStyle(numStyle);
                (c = r.createCell(col++)).setCellValue(p.getTotalRengFact()); c.setCellStyle(numStyle);
                (c = r.createCell(col++)).setCellValue(p.getTotalRengFaltantes()); c.setCellStyle(numStyle);

                (c = r.createCell(col++)).setCellValue(p.getTotalUndWeb()); c.setCellStyle(numStyle);
                (c = r.createCell(col++)).setCellValue(p.getTotalUndNota()); c.setCellStyle(numStyle);
                (c = r.createCell(col++)).setCellValue(p.getTotalUndFact()); c.setCellStyle(numStyle);
                (c = r.createCell(col++)).setCellValue(p.getTotalUndFaltantes()); c.setCellStyle(numStyle);

                (c = r.createCell(col++)).setCellValue(p.getTotalMontoNota()); c.setCellStyle(moneyStyle);
                (c = r.createCell(col++)).setCellValue(p.getTotalMontoFact()); c.setCellStyle(moneyStyle);
                (c = r.createCell(col++)).setCellValue(p.getTotalMontoPend()); c.setCellStyle(moneyStyle);
            }

            for (int i = 0; i < headersPedidos.length; i++) {
                sheetPedidos.autoSizeColumn(i);
            }

            // ── Hoja 2: Detalle Renglones ──
            String[] headersDetalle = {
                    "Pedido Web", "Nota Entrega", "Estado Línea", "Reng#", "Código Art",
                    "Descripción", "Cant Web", "Cant Nota", "Cant Facturada", "Cant Faltante",
                    "Precio ($)", "Descuento", "Neto ($)", "Lote", "Factura"
            };

            Row hRow2 = sheetDetalle.createRow(0);
            hRow2.setHeightInPoints(24);
            for (int i = 0; i < headersDetalle.length; i++) {
                Cell cell = hRow2.createCell(i);
                cell.setCellValue(headersDetalle[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx2 = 1;
            for (AuditoriaPedidoRow p : pedidos) {
                if (p.getRenglones() == null) continue;
                for (AuditoriaPedidoRenglonRow d : p.getRenglones()) {
                    Row r = sheetDetalle.createRow(rowIdx2++);
                    int col = 0;
                    r.createCell(col++).setCellValue(p.getPedidoWeb());
                    r.createCell(col++).setCellValue(p.getNotaEntrega());
                    r.createCell(col++).setCellValue(d.getEstado());
                    r.createCell(col++).setCellValue(d.getRengNum());
                    r.createCell(col++).setCellValue(d.getCoArt());
                    r.createCell(col++).setCellValue(d.getArtDes());

                    Cell c;
                    (c = r.createCell(col++)).setCellValue(d.getCantWeb()); c.setCellStyle(numStyle);
                    (c = r.createCell(col++)).setCellValue(d.getCantNota()); c.setCellStyle(numStyle);
                    (c = r.createCell(col++)).setCellValue(d.getCantFact()); c.setCellStyle(numStyle);
                    (c = r.createCell(col++)).setCellValue(d.getCantFaltante()); c.setCellStyle(numStyle);

                    (c = r.createCell(col++)).setCellValue(d.getPrecVta()); c.setCellStyle(moneyStyle);
                    r.createCell(col++).setCellValue(d.getPorcDesc());
                    (c = r.createCell(col++)).setCellValue(d.getRengNeto()); c.setCellStyle(moneyStyle);
                    r.createCell(col++).setCellValue(d.getNroLote());
                    r.createCell(col++).setCellValue(d.getFacturaDoc());
                }
            }

            for (int i = 0; i < headersDetalle.length; i++) {
                sheetDetalle.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(destino)) {
                wb.write(fos);
            }
        }
    }
}
