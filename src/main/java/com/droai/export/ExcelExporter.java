package com.droai.export;

import com.droai.model.FacturaRow;
import com.droai.model.ResumenRow;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exporta a Excel (.xlsx) con 5 hojas:
 * 1. Listado Completo
 * 2. Simulador
 * 3. Descuentos x Volumen
 * 4. Descuento x Producto
 * 5. Resumen General
 */
public class ExcelExporter {

    public File export(List<FacturaRow> listado, List<ResumenRow> dctoVol,
                       List<ResumenRow> dctoProd, double tasa) throws IOException {

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File file = new File(System.getProperty("user.dir"), "ListadoPrecios_" + timestamp + ".xlsx");

        try (SXSSFWorkbook wb = new SXSSFWorkbook(100)) {

            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle numberStyle = createNumberStyle(wb);
            CellStyle currencyStyle = createCurrencyStyle(wb);

            // Sheet 1: Listado Completo
            writeListado(wb, headerStyle, numberStyle, currencyStyle, listado, tasa);

            // Sheet 2: Simulador (same data, placeholder)
            writeListado(wb, headerStyle, numberStyle, currencyStyle, listado, tasa);
            wb.setSheetName(1, "Simulador");
            wb.setSheetName(0, "Listado Completo");

            // Sheet 3: Descuentos x Volumen
            writeResumen(wb, "Descuentos x Volumen", headerStyle, numberStyle, currencyStyle, dctoVol);

            // Sheet 4: Descuento x Producto
            writeResumen(wb, "Descuento x Producto", headerStyle, numberStyle, currencyStyle, dctoProd);

            // Sheet 5: Resumen General
            writeResumenGeneral(wb, headerStyle, numberStyle, currencyStyle, listado, tasa);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
        return file;
    }

    // ---------- Hojas ----------

    private void writeListado(SXSSFWorkbook wb, CellStyle hs, CellStyle ns,
                              CellStyle cs, List<FacturaRow> data, double tasa) {
        Sheet sheet = wb.createSheet();
        String[] headers = {"Codigo", "Descripcion", "Referencia", "Existencia", "UdM",
                "Costo Fabrica", "Arancel%", "Costo OM", "Util %", "Precio1", "%IVA", "Precio C/IVA"};

        // Header row
        Row hr = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell c = hr.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(hs);
        }

        // Data rows
        int rowIdx = 1;
        for (FacturaRow r : data) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(r.getCodigo());
            row.createCell(1).setCellValue(r.getDescripcion());
            row.createCell(2).setCellValue(r.getReferencia());
            setCellNum(row, 3, r.getExistencia(), ns);
            row.createCell(4).setCellValue(r.getUdm());
            setCellNum(row, 5, r.getCostoFabrica(), cs);
            setCellNum(row, 6, r.getArancelPct(), ns);
            setCellNum(row, 7, r.getCostoOM(), cs);
            setCellNum(row, 8, r.getUtilPct(), ns);
            setCellNum(row, 9, r.getPrecio1(), cs);
            setCellNum(row, 10, r.getIvaPct(), ns);
            setCellNum(row, 11, r.getPrecioCIVA(), cs);
        }

        sheet.createFreezePane(0, 1);
    }

    private void writeResumen(SXSSFWorkbook wb, String name, CellStyle hs,
                              CellStyle ns, CellStyle cs, List<ResumenRow> data) {
        Sheet sheet = wb.createSheet(name);
        String[] headers = {"Codigo", "Descripcion", "Total", "Descuento", "Neto", "% Descuento"};

        Row hr = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell c = hr.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(hs);
        }

        int rowIdx = 1;
        for (ResumenRow r : data) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(r.getClave());
            row.createCell(1).setCellValue(r.getDescripcion());
            setCellNum(row, 2, r.getTotal(), cs);
            setCellNum(row, 3, r.getDescuento(), cs);
            setCellNum(row, 4, r.getNeto(), cs);
            setCellNum(row, 5, r.getPorcentaje(), ns);
        }

        sheet.createFreezePane(0, 1);
    }

    private void writeResumenGeneral(SXSSFWorkbook wb, CellStyle hs, CellStyle ns,
                                     CellStyle cs, List<FacturaRow> data, double tasa) {
        Sheet sheet = wb.createSheet("Resumen General");
        String[] headers = {"Concepto", "Valor"};

        Row hr = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell c = hr.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(hs);
        }

        double totalPrecios = data.stream().mapToDouble(FacturaRow::getPrecio1).sum();
        double totalCIVA    = data.stream().mapToDouble(FacturaRow::getPrecioCIVA).sum();

        addSummaryRow(sheet, 1, "Total Registros", data.size(), ns);
        addSummaryRow(sheet, 2, "Suma Precio1", totalPrecios, cs);
        addSummaryRow(sheet, 3, "Suma Precio C/IVA", totalCIVA, cs);
        addSummaryRow(sheet, 4, "Tasa de Cambio", tasa, ns);
        addSummaryRow(sheet, 5, "Total USD (Precio1 / Tasa)",
                tasa > 0 ? totalPrecios / tasa : 0, cs);
    }

    // ---------- Helpers ----------

    private void setCellNum(Row row, int col, double val, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(val);
        c.setCellStyle(style);
    }

    private void addSummaryRow(Sheet sheet, int idx, String label, double val, CellStyle style) {
        Row row = sheet.createRow(idx);
        row.createCell(0).setCellValue(label);
        Cell c = row.createCell(1);
        c.setCellValue(val);
        c.setCellStyle(style);
    }

    // ---------- Styles ----------

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 11);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        return s;
    }

    private CellStyle createNumberStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        s.setAlignment(HorizontalAlignment.RIGHT);
        return s;
    }

    private CellStyle createCurrencyStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        s.setAlignment(HorizontalAlignment.RIGHT);
        Font f = wb.createFont();
        f.setColor(IndexedColors.DARK_TEAL.getIndex());
        s.setFont(f);
        return s;
    }
}
