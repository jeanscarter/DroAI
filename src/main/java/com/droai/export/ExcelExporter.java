package com.droai.export;

import com.droai.model.ArticuloImportRow;
import com.droai.model.ArticuloRow;
import com.droai.model.CargaMasivaCostosPreciosRow;
import com.droai.model.DescuentoProductoRow;
import com.droai.model.DescuentoVolumenRow;
import com.droai.model.ProductoReporteRow;
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

    /**
     * Exporta los Descuentos Adicionales (Simulador / Resumen).
     */
    public File exportDescuentosAdicionales(List<ResumenRow> listado) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File file = new File(System.getProperty("user.dir"), "DescuentosAdicionales_" + timestamp + ".xlsx");

        try (SXSSFWorkbook wb = new SXSSFWorkbook(100)) {
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle numberStyle = createNumberStyle(wb);

            Sheet sheet = wb.createSheet();
            String[] headers = {
                    "Clave", "Descripción", "Total", "Descuento", "Neto", "% Descuento", "Costo OM", "Util %"
            };

            Row hr = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hr.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (ResumenRow r : listado) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.getClave());
                row.createCell(1).setCellValue(r.getDescripcion());
                setCellNum(row, 2, r.getTotal(), numberStyle);
                setCellNum(row, 3, r.getDescuento(), numberStyle);
                setCellNum(row, 4, r.getNeto(), numberStyle);
                setCellNum(row, 5, r.getPorcentaje(), numberStyle);
                setCellNum(row, 6, r.getCostoOm(), numberStyle);
                setCellNum(row, 7, r.getUtilPct(), numberStyle);
            }

            sheet.createFreezePane(0, 1);
            wb.setSheetName(0, "Descuentos Adicionales");

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
        return file;
    }

    /**
     * Exporta el reporte de Descuento por Producto (DP).
     */
    public File exportDescuentosProducto(List<DescuentoProductoRow> listado) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File file = new File(System.getProperty("user.dir"), "DescuentoPorProducto_" + timestamp + ".xlsx");

        try (SXSSFWorkbook wb = new SXSSFWorkbook(100)) {
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle numberStyle = createNumberStyle(wb);
            CellStyle currencyStyle = createCurrencyStyle(wb);

            Sheet sheet = wb.createSheet();
            String[] headers = {
                    "Código", "Código de Barra", "Descripción", "Marca", "Costo Fábrica",
                    "Arancel%", "Costo Actual", "Precio1", "Utilidad%", "% Dcto", "Precio c/Dcto"
            };

            Row hr = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hr.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (DescuentoProductoRow r : listado) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.getCodigo());
                row.createCell(1).setCellValue(r.getCodigoBarra());
                row.createCell(2).setCellValue(r.getDescripcion());
                row.createCell(3).setCellValue(r.getMarca());
                setCellNum(row, 4, r.getCostoFabrica(), currencyStyle);
                setCellNum(row, 5, r.getArancelPct(), numberStyle);
                setCellNum(row, 6, r.getCostoActual(), currencyStyle);
                setCellNum(row, 7, r.getPrecio1(), currencyStyle);
                setCellNum(row, 8, r.getUtilidadPct(), numberStyle);
                setCellNum(row, 9, r.getDctoPct(), numberStyle);
                setCellNum(row, 10, r.getPrecioDcto(), currencyStyle);
            }

            sheet.createFreezePane(0, 1);
            wb.setSheetName(0, "Descuento x Producto");

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
        return file;
    }

    /**
     * Exporta los datos de Carga Masiva Costos/Precios.
     */
    public File exportCargaMasiva(List<CargaMasivaCostosPreciosRow> listado) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File file = new File(System.getProperty("user.dir"), "CargaMasivaCostosPrecios_" + timestamp + ".xlsx");

        try (SXSSFWorkbook wb = new SXSSFWorkbook(100)) {
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle currencyStyle = createCurrencyStyle(wb);

            Sheet sheet = wb.createSheet();
            String[] headers = {
                    "Código", "Descripción", "Costo Actual (USD)", "Costo Actual (Bs)",
                    "Costo Nuevo (USD)", "Costo Nuevo (Bs)", "Precio1 Actual (USD)",
                    "Precio1 Actual (Bs)", "Precio1 Nuevo (USD)", "Precio1 Nuevo (Bs)", "Estado"
            };

            Row hr = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hr.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (CargaMasivaCostosPreciosRow r : listado) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.getCoArt());
                row.createCell(1).setCellValue(r.getDescripcion());
                setCellNum(row, 2, r.getCostoActualUsd(), currencyStyle);
                setCellNum(row, 3, r.getCostoActualBs(), currencyStyle);
                setCellNum(row, 4, r.getCostoNuevoUsd(), currencyStyle);
                setCellNum(row, 5, r.getCostoNuevoBs(), currencyStyle);
                setCellNum(row, 6, r.getPrecio1ActualUsd(), currencyStyle);
                setCellNum(row, 7, r.getPrecio1ActualBs(), currencyStyle);
                setCellNum(row, 8, r.getPrecio1NuevoUsd(), currencyStyle);
                setCellNum(row, 9, r.getPrecio1NuevoBs(), currencyStyle);
                row.createCell(10).setCellValue(r.getEstado());
            }

            sheet.createFreezePane(0, 1);
            wb.setSheetName(0, "Carga Masiva");

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
        return file;
    }

    /**
     * Exporta la vista previa de Importación desde Excel.
     */
    public File exportImportarPreview(List<ArticuloImportRow> listado) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File file = new File(System.getProperty("user.dir"), "ImportarExcelPreview_" + timestamp + ".xlsx");

        try (SXSSFWorkbook wb = new SXSSFWorkbook(100)) {
            CellStyle headerStyle = createHeaderStyle(wb);

            Sheet sheet = wb.createSheet();
            String[] headers = {
                    "Código", "Tipo", "Descripción", "Referencia", "Marca", "Línea", "SubLínea", "Categoría"
            };

            Row hr = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hr.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (ArticuloImportRow r : listado) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.getCodigo());
                row.createCell(1).setCellValue(r.getTipo());
                row.createCell(2).setCellValue(r.getDescripcion());
                row.createCell(3).setCellValue(r.getReferencia());
                row.createCell(4).setCellValue(r.getMarca());
                row.createCell(5).setCellValue(r.getGrupo());
                row.createCell(6).setCellValue(r.getSgrupo());
                row.createCell(7).setCellValue(r.getCat());
            }

            sheet.createFreezePane(0, 1);
            wb.setSheetName(0, "Vista Previa Importación");

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
        return file;
    }
}