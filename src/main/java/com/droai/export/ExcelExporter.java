package com.droai.export;

import com.droai.model.ArticuloRow;
import com.droai.model.DescuentoVolumenRow;
import com.droai.model.ProductoReporteRow;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExcelExporter {

    /**
     * Exporta el catálogo completo de artículos con todas las columnas disponibles.
     */
    public File exportCatalogo(List<ArticuloRow> listado, double tasa) throws IOException {

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File file = new File(System.getProperty("user.dir"), "Catalogo_" + timestamp + ".xlsx");

        try (SXSSFWorkbook wb = new SXSSFWorkbook(100)) {

            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle numberStyle = createNumberStyle(wb);
            CellStyle currencyStyle = createCurrencyStyle(wb);

            writeCatalogo(wb, headerStyle, numberStyle, currencyStyle, listado, tasa);
            wb.setSheetName(0, "Catalogo");

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
        return file;
    }

    private void writeCatalogo(SXSSFWorkbook wb, CellStyle hs, CellStyle ns,
            CellStyle cs, List<ArticuloRow> data, double tasa) {
        Sheet sheet = wb.createSheet();
        String[] headers = {
                "Codigo", "Descripcion", "Marca", "Existencia", "UdM",
                "Costo Fabrica", "Arancel%", "Costo OM", "Util %",
                "Precio1", "Precio2", "Precio3", "%IVA", "Precio C/IVA",
                "Cod.Linea", "Linea", "Cod.Sub.", "SubLinea",
                "Cod.Proveedor", "Nombre Proveedor",
                "Referencia", "Modelo", "Procedencia", "Peso", "Volumen"
        };

        Row hr = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell c = hr.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(hs);
        }

        int rowIdx = 1;
        for (ArticuloRow r : data) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(r.getCodigo());
            row.createCell(1).setCellValue(r.getDescripcion());
            row.createCell(2).setCellValue(r.getMarca());
            setCellNum(row, 3, r.getExistencia(), ns);
            row.createCell(4).setCellValue(r.getUdm());
            setCellNum(row, 5, r.getCostoFabrica(), cs);
            setCellNum(row, 6, r.getArancelPct(), ns);
            setCellNum(row, 7, r.getCostoOm(), cs);
            setCellNum(row, 8, r.getUtilPct(), ns);
            setCellNum(row, 9, r.getPrecio1(), cs);
            setCellNum(row, 10, r.getPrecio2(), cs);
            setCellNum(row, 11, r.getPrecio3(), cs);
            setCellNum(row, 12, r.getIvaPct(), ns);
            setCellNum(row, 13, r.getPrecioCiva(), cs);
            row.createCell(14).setCellValue(r.getCodLinea());
            row.createCell(15).setCellValue(r.getLinea());
            row.createCell(16).setCellValue(r.getCodSub());
            row.createCell(17).setCellValue(r.getSubLinea());
            row.createCell(18).setCellValue(r.getCodProveedor());
            row.createCell(19).setCellValue(r.getNombreProveedor());
            row.createCell(20).setCellValue(r.getReferencia());
            row.createCell(21).setCellValue(r.getModelo());
            row.createCell(22).setCellValue(r.getProcedencia());
            setCellNum(row, 23, r.getPeso(), ns);
            setCellNum(row, 24, r.getVolumen(), ns);
        }

        sheet.createFreezePane(0, 1);
    }

    private void setCellNum(Row row, int col, double val, CellStyle style) {
        Cell c = row.createCell(col);
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

    /**
     * Exporta el reporte de productos con las columnas mostradas en la interfaz.
     */
    public File exportReporteProductos(List<ProductoReporteRow> listado) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File file = new File(System.getProperty("user.dir"), "ReporteProductos_" + timestamp + ".xlsx");

        try (SXSSFWorkbook wb = new SXSSFWorkbook(100)) {
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle numberStyle = createNumberStyle(wb);

            Sheet sheet = wb.createSheet();
            String[] headers = {
                    "Código", "Código de Barra", "Descripción", "Marca",
                    "Línea", "Principio Activo", "Categoría", "Proveedor",
                    "Existencia", "Impuesto"
            };

            Row hr = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hr.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (ProductoReporteRow r : listado) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.getCodigo());
                row.createCell(1).setCellValue(r.getCodigoBarra());
                row.createCell(2).setCellValue(r.getDescripcion());
                row.createCell(3).setCellValue(r.getMarca());
                row.createCell(4).setCellValue(r.getLinea());
                row.createCell(5).setCellValue(r.getPrincipioActivo());
                row.createCell(6).setCellValue(r.getCategoria());
                row.createCell(7).setCellValue(r.getProveedor());
                setCellNum(row, 8, r.getExistencia(), numberStyle);
                setCellNum(row, 9, r.getImpuesto(), numberStyle);
            }

            sheet.createFreezePane(0, 1);
            wb.setSheetName(0, "Reporte Productos");

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
        return file;
    }

    /**
     * Exporta el reporte de descuentos por volumen.
     */
    public File exportDescuentosVolumen(List<DescuentoVolumenRow> listado) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File file = new File(System.getProperty("user.dir"), "DescuentosVolumen_" + timestamp + ".xlsx");

        try (SXSSFWorkbook wb = new SXSSFWorkbook(100)) {
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle numberStyle = createNumberStyle(wb);
            CellStyle currencyStyle = createCurrencyStyle(wb);

            Sheet sheet = wb.createSheet();
            String[] headers = {
                    "Codigo", "Descripcion", "Marca", "Codigo de Barra", "Precio", "Descuento DV",
                    "Fecha Inicio", "Fecha Fin"
            };

            Row hr = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hr.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (DescuentoVolumenRow r : listado) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.getCodigo());
                row.createCell(1).setCellValue(r.getDescripcion());
                row.createCell(2).setCellValue(r.getMarca());
                row.createCell(3).setCellValue(r.getCodigoBarra());
                setCellNum(row, 4, r.getPrecio1(), currencyStyle);
                setCellNum(row, 5, r.getDescuentoDV(), numberStyle);
                row.createCell(6).setCellValue(r.getFechaIni());
                row.createCell(7).setCellValue(r.getFechaFin());
            }

            sheet.createFreezePane(0, 1);
            wb.setSheetName(0, "Descuentos Volumen");

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
        return file;
    }
}