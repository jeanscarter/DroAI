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

public class ExcelExporter {

    public File export(List<FacturaRow> listado, List<ResumenRow> dctoVol,
                       List<ResumenRow> dctoProd, double tasa) throws IOException {

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File file = new File(System.getProperty("user.dir"), "MatrizVentas_" + timestamp + ".xlsx");

        try (SXSSFWorkbook wb = new SXSSFWorkbook(100)) {

            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle numberStyle = createNumberStyle(wb);
            CellStyle currencyStyle = createCurrencyStyle(wb);

            // Sheet 1: Matriz Original
            writeListado(wb, headerStyle, numberStyle, currencyStyle, listado, tasa);
            wb.setSheetName(0, "Matriz");

            // Sheet 2: Descuentos x Volumen
            writeResumen(wb, "Descuentos x Volumen", headerStyle, numberStyle, currencyStyle, dctoVol);

            // Sheet 3: Descuento x Producto
            writeResumen(wb, "Descuento x Producto", headerStyle, numberStyle, currencyStyle, dctoProd);

            // Sheet 4: Resumen General
            writeResumenGeneral(wb, headerStyle, numberStyle, currencyStyle, listado, tasa);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
        return file;
    }

    private void writeListado(SXSSFWorkbook wb, CellStyle hs, CellStyle ns,
                              CellStyle cs, List<FacturaRow> data, double tasa) {
        Sheet sheet = wb.createSheet();
        String[] headers = {
            "Numero", "Fecha", "CI/Rif", "Nombre o Razon Social", "Vendedor", "Nombre Vendedor",
            "Tasa", "codigo art", "Descripcion", "Cantidad", "Precio", "DP", "DCT", "DA", "DV",
            "Desc.%", "Total Renglon", "Desc.%Global", "Renglon-DG", "Monto IVA", "Tot.Renglon+IVA",
            "Costo de Venta", "Total Costo Venta", "Tot.CV-DP", "Monto Utilidad", "% Utilidad",
            "Costo Actual", "Stock Actual", "Cod.Linea", "Linea"
        };

        Row hr = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell c = hr.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(hs);
        }

        int rowIdx = 1;
        for (FacturaRow r : data) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(r.getNumero());
            row.createCell(1).setCellValue(r.getFecha());
            row.createCell(2).setCellValue(r.getCiRif());
            row.createCell(3).setCellValue(r.getNombreRazonSocial());
            row.createCell(4).setCellValue(r.getCoVen());
            row.createCell(5).setCellValue(r.getNombreVendedor());
            setCellNum(row, 6, r.getTasa(), ns);
            row.createCell(7).setCellValue(r.getCodigoArt());
            row.createCell(8).setCellValue(r.getDescripcionArt());
            setCellNum(row, 9, r.getCantidad(), ns);
            setCellNum(row, 10, r.getPrecio(), cs);
            setCellNum(row, 11, r.getDp(), ns);
            setCellNum(row, 12, r.getDct(), ns);
            setCellNum(row, 13, r.getDa(), ns);
            setCellNum(row, 14, r.getDv(), ns);
            setCellNum(row, 15, r.getDescPct(), ns);
            setCellNum(row, 16, r.getTotalRenglon(), cs);
            setCellNum(row, 17, r.getDescPctGlobal(), ns);
            setCellNum(row, 18, r.getRenglonDg(), cs);
            setCellNum(row, 19, r.getMontoIva(), cs);
            setCellNum(row, 20, r.getTotRenglonIva(), cs);
            setCellNum(row, 21, r.getCostoVenta(), cs);
            setCellNum(row, 22, r.getTotalCostoVenta(), cs);
            setCellNum(row, 23, r.getTotCvDp(), cs);
            setCellNum(row, 24, r.getMontoUtilidad(), cs);
            setCellNum(row, 25, r.getUtilPct(), ns);
            setCellNum(row, 26, r.getCostoActual(), cs);
            setCellNum(row, 27, r.getStockActual(), ns);
            row.createCell(28).setCellValue(r.getCodLinea());
            row.createCell(29).setCellValue(r.getLinea());
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

        double totalPrecios = data.stream().mapToDouble(FacturaRow::getPrecio).sum();
        double totalIva     = data.stream().mapToDouble(FacturaRow::getMontoIva).sum();
        double totalCIVA    = data.stream().mapToDouble(FacturaRow::getTotRenglonIva).sum();

        addSummaryRow(sheet, 1, "Total Registros/Ventas", data.size(), ns);
        addSummaryRow(sheet, 2, "Suma Total Precio (Base)", totalPrecios, cs);
        addSummaryRow(sheet, 3, "Suma Mto IVA", totalIva, cs);
        addSummaryRow(sheet, 4, "Suma Total C/IVA", totalCIVA, cs);
        addSummaryRow(sheet, 5, "Tasa de Cambio Gral.", tasa, ns);
    }

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
