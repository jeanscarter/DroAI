package com.droai.export;

import com.droai.model.CapaCostoDTO;
import com.droai.model.CostoVentaRow;
import com.droai.model.CostoVentasPeriodoResult;
import com.droai.model.InventarioValoradoRow;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exportador oficial a Excel para:
 * 1. Valuación de Inventario y Capas FIFO (con vista dual: Costo Histórico vs. Valor de Reposición).
 * 2. Costo de Ventas y Margen del Período (desglose por venta firme y consignación).
 */
public class InventarioValoracionExcelExporter {

    private static final DateTimeFormatter DF_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static void exportar(List<InventarioValoradoRow> rows,
                                LocalDate fechaCorte,
                                double tasaUSD,
                                File destino) throws IOException {

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheetResumen = wb.createSheet("Resumen Ejecutivo");
            XSSFSheet sheetInventario = wb.createSheet("Inventario Valorado");
            XSSFSheet sheetCapas = wb.createSheet("Desglose Capas FIFO");

            DataFormat df = wb.createDataFormat();

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

            XSSFCellStyle titleStyle = wb.createCellStyle();
            XSSFFont titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setColor(new XSSFColor(new java.awt.Color(15, 23, 42), null));
            titleStyle.setFont(titleFont);

            XSSFCellStyle boldStyle = wb.createCellStyle();
            XSSFFont boldFont = wb.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);

            XSSFCellStyle moneyUsdStyle = wb.createCellStyle();
            moneyUsdStyle.setDataFormat(df.getFormat("$#,##0.00"));

            XSSFCellStyle moneyBsStyle = wb.createCellStyle();
            moneyBsStyle.setDataFormat(df.getFormat("Bs. #,##0.00"));

            XSSFCellStyle numDecStyle = wb.createCellStyle();
            numDecStyle.setDataFormat(df.getFormat("#,##0.00"));

            XSSFCellStyle numIntStyle = wb.createCellStyle();
            numIntStyle.setDataFormat(df.getFormat("#,##0"));

            // ═══════════════════════════════════════════════════════════
            // HOJA 1: RESUMEN EJECUTIVO
            // ═══════════════════════════════════════════════════════════
            Row rTitle = sheetResumen.createRow(1);
            Cell cTitle = rTitle.createCell(1);
            cTitle.setCellValue("DROGUERÍA DROACTIVA, C.A. — INVENTARIO VALORADO AL CORTE");
            cTitle.setCellStyle(titleStyle);

            Row rFecha = sheetResumen.createRow(2);
            rFecha.createCell(1).setCellValue("Fecha de corte de inventario: " + (fechaCorte != null ? fechaCorte.format(DF_FECHA) : ""));
            Row rTasa = sheetResumen.createRow(3);
            rTasa.createCell(1).setCellValue(String.format("Tasa oficial aplicada (BCV): Bs. %,.4f / USD", tasaUSD));
            Row rDb = sheetResumen.createRow(4);
            String dbConsultada = (fechaCorte != null && !fechaCorte.isAfter(LocalDate.of(2026, 7, 27)))
                    ? "DROA2_A (Histórico ≤ 27/07/2026)" : "DROA_A (Producción Actual)";
            rDb.createCell(1).setCellValue("Base de Datos consultada: " + dbConsultada);

            // Totales acumulados
            int itemsFirme = 0;
            double undsFirme = 0;
            double totalUsdFirme = 0;
            double totalBsFirme = 0;
            double totalReposicionBsFirme = 0;

            int itemsConsig = 0;
            double undsConsig = 0;
            double totalUsdConsig = 0;
            double totalBsConsig = 0;
            double totalReposicionBsConsig = 0;

            for (InventarioValoradoRow row : rows) {
                if ("CONSIGNACION".equalsIgnoreCase(row.getTipoMercancia())) {
                    itemsConsig++;
                    undsConsig += row.getExistencia();
                    totalUsdConsig += row.getTotalValorizadoUSD();
                    totalBsConsig += row.getTotalValorizadoBS();
                    totalReposicionBsConsig += row.getTotalReposicionBS();
                } else {
                    itemsFirme++;
                    undsFirme += row.getExistencia();
                    totalUsdFirme += row.getTotalValorizadoUSD();
                    totalBsFirme += row.getTotalValorizadoBS();
                    totalReposicionBsFirme += row.getTotalReposicionBS();
                }
            }

            int itemsTotal = itemsFirme + itemsConsig;
            double undsTotal = undsFirme + undsConsig;
            double totalUsdGlobal = totalUsdFirme + totalUsdConsig;
            double totalBsGlobal = totalBsFirme + totalBsConsig;
            double totalReposicionBsGlobal = totalReposicionBsFirme + totalReposicionBsConsig;
            double difCambiarioGlobal = totalReposicionBsGlobal - totalBsGlobal;

            String[] resHeaders = {
                    "Categoría de Inventario", "Cant. Productos", "Total Unidades",
                    "Total Valorado (USD $)", "Costo Histórico (Bs.)", "Valor Reposición (Bs.)", "Dif. Cambiario (Bs.)"
            };
            Row rHRes = sheetResumen.createRow(5);
            rHRes.setHeightInPoints(22);
            for (int i = 0; i < resHeaders.length; i++) {
                Cell c = rHRes.createCell(i + 1);
                c.setCellValue(resHeaders[i]);
                c.setCellStyle(headerStyle);
            }

            // Fila Venta Firme
            Row rFirme = sheetResumen.createRow(6);
            rFirme.createCell(1).setCellValue("Mercancía Venta Firme (Capas FIFO)");
            Cell c1;
            (c1 = rFirme.createCell(2)).setCellValue(itemsFirme); c1.setCellStyle(numIntStyle);
            (c1 = rFirme.createCell(3)).setCellValue(undsFirme); c1.setCellStyle(numDecStyle);
            (c1 = rFirme.createCell(4)).setCellValue(totalUsdFirme); c1.setCellStyle(moneyUsdStyle);
            (c1 = rFirme.createCell(5)).setCellValue(totalBsFirme); c1.setCellStyle(moneyBsStyle);
            (c1 = rFirme.createCell(6)).setCellValue(totalReposicionBsFirme); c1.setCellStyle(moneyBsStyle);
            (c1 = rFirme.createCell(7)).setCellValue(totalReposicionBsFirme - totalBsFirme); c1.setCellStyle(moneyBsStyle);

            // Fila Consignación
            Row rConsig = sheetResumen.createRow(7);
            rConsig.createCell(1).setCellValue("Mercancía en Consignación (Costo Proveedor Reportado)");
            (c1 = rConsig.createCell(2)).setCellValue(itemsConsig); c1.setCellStyle(numIntStyle);
            (c1 = rConsig.createCell(3)).setCellValue(undsConsig); c1.setCellStyle(numDecStyle);
            (c1 = rConsig.createCell(4)).setCellValue(totalUsdConsig); c1.setCellStyle(moneyUsdStyle);
            (c1 = rConsig.createCell(5)).setCellValue(totalBsConsig); c1.setCellStyle(moneyBsStyle);
            (c1 = rConsig.createCell(6)).setCellValue(totalReposicionBsConsig); c1.setCellStyle(moneyBsStyle);
            (c1 = rConsig.createCell(7)).setCellValue(0.0); c1.setCellStyle(moneyBsStyle);

            // Fila Total General
            Row rTot = sheetResumen.createRow(8);
            Cell cTot = rTot.createCell(1); cTot.setCellValue("TOTAL GENERAL EMPRESA"); cTot.setCellStyle(boldStyle);
            (c1 = rTot.createCell(2)).setCellValue(itemsTotal); c1.setCellStyle(boldStyle);
            (c1 = rTot.createCell(3)).setCellValue(undsTotal); c1.setCellStyle(boldStyle);
            (c1 = rTot.createCell(4)).setCellValue(totalUsdGlobal); c1.setCellStyle(moneyUsdStyle);
            (c1 = rTot.createCell(5)).setCellValue(totalBsGlobal); c1.setCellStyle(moneyBsStyle);
            (c1 = rTot.createCell(6)).setCellValue(totalReposicionBsGlobal); c1.setCellStyle(moneyBsStyle);
            (c1 = rTot.createCell(7)).setCellValue(difCambiarioGlobal); c1.setCellStyle(moneyBsStyle);

            for (int i = 1; i <= 7; i++) {
                sheetResumen.autoSizeColumn(i);
            }

            // ═══════════════════════════════════════════════════════════
            // HOJA 2: INVENTARIO VALORADO DETALLADO
            // ═══════════════════════════════════════════════════════════
            String[] invHeaders = {
                    "Código", "Descripción", "Línea", "Categoría", "Proveedor",
                    "Almacén", "Nombre Almacén", "Tipo Mercancía", "Existencia al Corte",
                    "Costo Promedio ($)", "Costo Promedio (Bs.)",
                    "Último Costo ($)", "Último Costo (Bs.)",
                    "Costo Proveedor ($)", "Costo Proveedor (Bs.)",
                    "Total Valorado ($)", "Costo Histórico (Bs.)",
                    "Valor Reposición (Bs.)", "Dif. Cambiario (Bs.)",
                    "Cant. Capas FIFO"
            };

            Row rHInv = sheetInventario.createRow(0);
            rHInv.setHeightInPoints(24);
            for (int i = 0; i < invHeaders.length; i++) {
                Cell c = rHInv.createCell(i);
                c.setCellValue(invHeaders[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx2 = 1;
            for (InventarioValoradoRow r : rows) {
                Row row = sheetInventario.createRow(rowIdx2++);
                int col = 0;
                row.createCell(col++).setCellValue(r.getCoArt());
                row.createCell(col++).setCellValue(r.getArtDes());
                row.createCell(col++).setCellValue(r.getLinea());
                row.createCell(col++).setCellValue(r.getCategoria());
                row.createCell(col++).setCellValue(r.getNomProv());
                row.createCell(col++).setCellValue(r.getCoAlma());
                row.createCell(col++).setCellValue(r.getDesAlma());
                row.createCell(col++).setCellValue(r.getTipoMercancia());

                Cell c;
                (c = row.createCell(col++)).setCellValue(r.getExistencia()); c.setCellStyle(numDecStyle);
                (c = row.createCell(col++)).setCellValue(r.getCostoPromedioUSD()); c.setCellStyle(moneyUsdStyle);
                (c = row.createCell(col++)).setCellValue(r.getCostoPromedioBS()); c.setCellStyle(moneyBsStyle);
                (c = row.createCell(col++)).setCellValue(r.getUltimoCostoUSD()); c.setCellStyle(moneyUsdStyle);
                (c = row.createCell(col++)).setCellValue(r.getUltimoCostoBS()); c.setCellStyle(moneyBsStyle);
                (c = row.createCell(col++)).setCellValue(r.getCostoProveedorUSD()); c.setCellStyle(moneyUsdStyle);
                (c = row.createCell(col++)).setCellValue(r.getCostoProveedorBS()); c.setCellStyle(moneyBsStyle);
                (c = row.createCell(col++)).setCellValue(r.getTotalValorizadoUSD()); c.setCellStyle(moneyUsdStyle);
                (c = row.createCell(col++)).setCellValue(r.getTotalValorizadoBS()); c.setCellStyle(moneyBsStyle);
                (c = row.createCell(col++)).setCellValue(r.getTotalReposicionBS()); c.setCellStyle(moneyBsStyle);
                (c = row.createCell(col++)).setCellValue(r.getDiferencialCambiarioBS()); c.setCellStyle(moneyBsStyle);
                (c = row.createCell(col++)).setCellValue(r.getCantCapas()); c.setCellStyle(numIntStyle);
            }

            for (int i = 0; i < invHeaders.length; i++) {
                sheetInventario.autoSizeColumn(i);
            }

            // ═══════════════════════════════════════════════════════════
            // HOJA 3: DESGLOSE DE CAPAS FIFO
            // ═══════════════════════════════════════════════════════════
            String[] capasHeaders = {
                    "Código Art.", "Descripción", "Almacén", "Tipo Doc", "Nro. Documento",
                    "Fecha Recepción", "Cant. Total Compra", "Cant. en Capa (Saldo)",
                    "Costo Unitario ($)", "Costo Unitario (Bs.)", "Tasa Compra",
                    "Subtotal Capa ($)", "Subtotal Capa (Bs.)", "Proveedor"
            };

            Row rHCapas = sheetCapas.createRow(0);
            rHCapas.setHeightInPoints(24);
            for (int i = 0; i < capasHeaders.length; i++) {
                Cell c = rHCapas.createCell(i);
                c.setCellValue(capasHeaders[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx3 = 1;
            for (InventarioValoradoRow r : rows) {
                if (r.getCapas() != null && !r.getCapas().isEmpty()) {
                    for (CapaCostoDTO capa : r.getCapas()) {
                        Row row = sheetCapas.createRow(rowIdx3++);
                        int col = 0;
                        row.createCell(col++).setCellValue(r.getCoArt());
                        row.createCell(col++).setCellValue(r.getArtDes());
                        row.createCell(col++).setCellValue(r.getCoAlma());
                        row.createCell(col++).setCellValue(capa.getTipoDoc());
                        row.createCell(col++).setCellValue(capa.getDocNum());
                        row.createCell(col++).setCellValue(capa.getFecha());

                        Cell c;
                        (c = row.createCell(col++)).setCellValue(capa.getCantidadTotal()); c.setCellStyle(numDecStyle);
                        (c = row.createCell(col++)).setCellValue(capa.getCantidadEnCapa()); c.setCellStyle(numDecStyle);
                        (c = row.createCell(col++)).setCellValue(capa.getCostUnitUSD()); c.setCellStyle(moneyUsdStyle);
                        (c = row.createCell(col++)).setCellValue(capa.getCostUnitBS()); c.setCellStyle(moneyBsStyle);
                        (c = row.createCell(col++)).setCellValue(capa.getTasaCambio()); c.setCellStyle(numDecStyle);
                        (c = row.createCell(col++)).setCellValue(capa.getSubtotalUSD()); c.setCellStyle(moneyUsdStyle);
                        (c = row.createCell(col++)).setCellValue(capa.getSubtotalBS()); c.setCellStyle(moneyBsStyle);
                        row.createCell(col++).setCellValue(capa.getNomProv());
                    }
                }
            }

            for (int i = 0; i < capasHeaders.length; i++) {
                sheetCapas.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(destino)) {
                wb.write(fos);
            }
        }
    }

    /**
     * Exporta el reporte de Costo de Ventas y Margen del Período a Excel.
     */
    public static void exportarCostoVentas(CostoVentasPeriodoResult result,
                                           LocalDate desde,
                                           LocalDate hasta,
                                           File destino) throws IOException {

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheetResumen = wb.createSheet("Resumen Costo de Ventas");
            XSSFSheet sheetDetalle = wb.createSheet("Detalle Renglones");

            DataFormat df = wb.createDataFormat();

            // Estilos
            XSSFCellStyle headerStyle = wb.createCellStyle();
            XSSFFont headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(30, 41, 59), null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            XSSFCellStyle titleStyle = wb.createCellStyle();
            XSSFFont titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setColor(new XSSFColor(new java.awt.Color(15, 23, 42), null));
            titleStyle.setFont(titleFont);

            XSSFCellStyle boldStyle = wb.createCellStyle();
            XSSFFont boldFont = wb.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);

            XSSFCellStyle moneyUsdStyle = wb.createCellStyle();
            moneyUsdStyle.setDataFormat(df.getFormat("$#,##0.00"));

            XSSFCellStyle moneyBsStyle = wb.createCellStyle();
            moneyBsStyle.setDataFormat(df.getFormat("Bs. #,##0.00"));

            XSSFCellStyle numDecStyle = wb.createCellStyle();
            numDecStyle.setDataFormat(df.getFormat("#,##0.00"));

            XSSFCellStyle numPctStyle = wb.createCellStyle();
            numPctStyle.setDataFormat(df.getFormat("0.00\"%\""));

            // ═══════════════════════════════════════════════════════════
            // HOJA 1: RESUMEN EJECUTIVO DE COSTO DE VENTAS
            // ═══════════════════════════════════════════════════════════
            Row rTitle = sheetResumen.createRow(1);
            Cell cTitle = rTitle.createCell(1);
            cTitle.setCellValue("DROGUERÍA DROACTIVA, C.A. — COSTO DE VENTAS Y MARGEN BRUTO");
            cTitle.setCellStyle(titleStyle);

            Row rPeriodo = sheetResumen.createRow(2);
            rPeriodo.createCell(1).setCellValue(String.format("Período analizado: %s al %s",
                    (desde != null ? desde.format(DF_FECHA) : ""),
                    (hasta != null ? hasta.format(DF_FECHA) : "")));

            Row rDb = sheetResumen.createRow(3);
            rDb.createCell(1).setCellValue("Base de Datos consultada: DROA_A (Producción Actual)");

            String[] resHeaders = {"Indicador Financiero / Contable", "Monto USD ($)", "Monto Bs. (VES)"};
            Row rHRes = sheetResumen.createRow(5);
            rHRes.setHeightInPoints(22);
            for (int i = 0; i < resHeaders.length; i++) {
                Cell c = rHRes.createCell(i + 1);
                c.setCellValue(resHeaders[i]);
                c.setCellStyle(headerStyle);
            }

            int rowNum = 6;
            rowNum = addKpiRow(sheetResumen, rowNum, "Ventas Netas Totales", result.getTotalVentaUSD(), result.getTotalVentaBS(), moneyUsdStyle, moneyBsStyle, false, boldStyle);
            rowNum = addKpiRow(sheetResumen, rowNum, "Costo de Ventas — Venta Firme (Costo Promedio Ponderado)", result.getTotalCostoFirmeUSD(), result.getTotalCostoFirmeBS(), moneyUsdStyle, moneyBsStyle, false, boldStyle);
            rowNum = addKpiRow(sheetResumen, rowNum, "Costo de Ventas — Consignación (Último Costo Proveedor)", result.getTotalCostoConsigUSD(), result.getTotalCostoConsigBS(), moneyUsdStyle, moneyBsStyle, false, boldStyle);
            rowNum = addKpiRow(sheetResumen, rowNum, "TOTAL COSTO DE VENTAS", result.getTotalCostoUSD(), result.getTotalCostoBS(), moneyUsdStyle, moneyBsStyle, true, boldStyle);
            rowNum = addKpiRow(sheetResumen, rowNum, "UTILIDAD BRUTA EN VENTAS", result.getUtilidadUSD(), result.getUtilidadBS(), moneyUsdStyle, moneyBsStyle, true, boldStyle);

            // Margen %
            Row rMargen = sheetResumen.createRow(rowNum++);
            Cell cMLbl = rMargen.createCell(1); cMLbl.setCellValue("MARGEN BRUTO GLOBAL (%)"); cMLbl.setCellStyle(boldStyle);
            Cell cMVal = rMargen.createCell(2); cMVal.setCellValue(result.getMargenPct()); cMVal.setCellStyle(numPctStyle);

            // Unidades
            Row rUnds = sheetResumen.createRow(rowNum++);
            Cell cULbl = rUnds.createCell(1); cULbl.setCellValue("Total Unidades Vendidas"); cULbl.setCellStyle(boldStyle);
            Cell cUVal = rUnds.createCell(2); cUVal.setCellValue(result.getTotalUnidades()); cUVal.setCellStyle(numDecStyle);

            for (int i = 1; i <= 3; i++) {
                sheetResumen.autoSizeColumn(i);
            }

            // ═══════════════════════════════════════════════════════════
            // HOJA 2: DETALLE DE RENGLONES DE VENTA
            // ═══════════════════════════════════════════════════════════
            String[] detHeaders = {
                    "Tipo Doc", "Nro. Documento", "Fecha Emisión", "Cód. Cliente", "Nombre Cliente",
                    "Cód. Artículo", "Descripción Artículo", "Almacén", "Tipo Mercancía",
                    "Cantidad", "Precio Vta. ($)", "Total Vta. ($)",
                    "Costo Unit. ($)", "Total Costo ($)",
                    "Utilidad ($)", "Margen (%)", "Tasa Doc (Bs.)",
                    "Total Vta. (Bs.)", "Total Costo (Bs.)", "Utilidad (Bs.)"
            };

            Row rHDet = sheetDetalle.createRow(0);
            rHDet.setHeightInPoints(24);
            for (int i = 0; i < detHeaders.length; i++) {
                Cell c = rHDet.createCell(i);
                c.setCellValue(detHeaders[i]);
                c.setCellStyle(headerStyle);
            }

            int detIdx = 1;
            for (CostoVentaRow r : result.getItems()) {
                Row row = sheetDetalle.createRow(detIdx++);
                int col = 0;
                row.createCell(col++).setCellValue(r.getTipoDoc());
                row.createCell(col++).setCellValue(r.getDocNum());
                row.createCell(col++).setCellValue(r.getFecha());
                row.createCell(col++).setCellValue(r.getCoCli());
                row.createCell(col++).setCellValue(r.getCliDes());
                row.createCell(col++).setCellValue(r.getCoArt());
                row.createCell(col++).setCellValue(r.getArtDes());
                row.createCell(col++).setCellValue(r.getCoAlma());
                row.createCell(col++).setCellValue(r.getTipoMercancia());

                Cell c;
                (c = row.createCell(col++)).setCellValue(r.getCantidad()); c.setCellStyle(numDecStyle);
                (c = row.createCell(col++)).setCellValue(r.getPrecioVentaUSD()); c.setCellStyle(moneyUsdStyle);
                (c = row.createCell(col++)).setCellValue(r.getTotalVentaUSD()); c.setCellStyle(moneyUsdStyle);
                (c = row.createCell(col++)).setCellValue(r.getCostoUnitUSD()); c.setCellStyle(moneyUsdStyle);
                (c = row.createCell(col++)).setCellValue(r.getTotalCostoUSD()); c.setCellStyle(moneyUsdStyle);
                (c = row.createCell(col++)).setCellValue(r.getUtilidadUSD()); c.setCellStyle(moneyUsdStyle);
                (c = row.createCell(col++)).setCellValue(r.getMargenPct()); c.setCellStyle(numPctStyle);
                (c = row.createCell(col++)).setCellValue(r.getTasaDoc()); c.setCellStyle(numDecStyle);
                (c = row.createCell(col++)).setCellValue(r.getTotalVentaBS()); c.setCellStyle(moneyBsStyle);
                (c = row.createCell(col++)).setCellValue(r.getTotalCostoBS()); c.setCellStyle(moneyBsStyle);
                (c = row.createCell(col++)).setCellValue(r.getUtilidadBS()); c.setCellStyle(moneyBsStyle);
            }

            for (int i = 0; i < detHeaders.length; i++) {
                sheetDetalle.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(destino)) {
                wb.write(fos);
            }
        }
    }

    private static int addKpiRow(XSSFSheet sheet, int rowNum, String label, double valUsd, double valBs,
                                 XSSFCellStyle usdStyle, XSSFCellStyle bsStyle, boolean bold, XSSFCellStyle boldStyle) {
        Row r = sheet.createRow(rowNum++);
        Cell cLbl = r.createCell(1);
        cLbl.setCellValue(label);
        if (bold) cLbl.setCellStyle(boldStyle);

        Cell cUsd = r.createCell(2);
        cUsd.setCellValue(valUsd);
        cUsd.setCellStyle(usdStyle);

        Cell cBs = r.createCell(3);
        cBs.setCellValue(valBs);
        cBs.setCellStyle(bsStyle);

        return rowNum;
    }
}
