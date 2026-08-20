package com.droai.export;

import com.droai.model.ArticuloImportRow;
import com.droai.model.ArticuloRow;
import com.droai.model.CargaMasivaCostosPreciosRow;
import com.droai.model.ClienteMaestroRow;
import com.droai.model.ComisionRow;
import com.droai.model.CxCDocumentoRow;
import com.droai.model.DescuentoProductoRow;
import com.droai.model.DescuentoVolumenRow;
import com.droai.model.MatrizVentasRow;
import com.droai.model.ProductoReporteRow;
import com.droai.model.ResumenRow;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    /**
     * Exporta la Matriz de Ventas completa con las 40 columnas exactas del reporte de referencia (MatrizVentas_YYYYMMDD_HHmmss.xlsx).
     */
    public File exportMatrizVentas(List<MatrizVentasRow> listado, boolean isBs) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File file = new File(System.getProperty("user.dir"), "MatrizVentas_" + timestamp + ".xlsx");

        try (SXSSFWorkbook wb = new SXSSFWorkbook(100)) {
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle numberStyle = createNumberStyle(wb);
            CellStyle currencyStyle = createCurrencyStyle(wb);

            Sheet sheet = wb.createSheet("Matriz");
            String[] headers = {
                    "Numero", "Fecha", "Cod. Cliente", "CI / RIF", "Nombre o Razon Social",
                    "Grupo", "Condicion de Pago", "Zona", "Ciudad", "Cod.Ven.", "Nombre Vendedor",
                    "Origen", "Tasa",
                    "Cod. Prov.", "Nombre Proveedor", "Cod. Art. Prov.", "Codigo Art.", "Descripcion",
                    "Cantidad", "Precio", "DP", "DA", "DCT", "DC", "DV",
                    "Total Renglon", "% Desc. Glob.", "Renglon DG", "Monto I.V.A.", "% I.V.A.", "Tot. Renglon IVA",
                    "Costo Venta", "Total Costo Venta", "Tot. CV DP", "Monto Utilidad", "% Util.",
                    "Costo Actual", "Stock Actual", "Cod. Linea", "Linea", "Cod. Sub.", "Sub-Linea",
                    "Almacen", "Pedido Web", "Usuario Web"
            };

            Row hr = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hr.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (MatrizVentasRow r : listado) {
                Row row = sheet.createRow(rowIdx++);
                double tasa = r.getTasa() > 0 ? r.getTasa() : 1.0;
                double divFactor = isBs ? 1.0 : tasa;

                row.createCell(0).setCellValue(safeStr(r.getNumero()));
                row.createCell(1).setCellValue(safeStr(r.getFecha()));
                row.createCell(2).setCellValue(safeStr(r.getCodCliente()));
                row.createCell(3).setCellValue(safeStr(r.getCiRif()));
                row.createCell(4).setCellValue(safeStr(r.getNombreRazonSocial()));
                row.createCell(5).setCellValue(safeStr(r.getGrupoCliente()));
                row.createCell(6).setCellValue(safeStr(r.getCondicionPago()));
                row.createCell(7).setCellValue(safeStr(r.getZona()));
                row.createCell(8).setCellValue(safeStr(r.getCiudad()));
                row.createCell(9).setCellValue(safeStr(r.getCoVen()));
                row.createCell(10).setCellValue(safeStr(r.getNombreVendedor()));
                row.createCell(11).setCellValue(safeStr(r.getOrigen()));
                setCellNum(row, 12, r.getTasa(), numberStyle);
                row.createCell(13).setCellValue(safeStr(r.getCodProveedor()));
                row.createCell(14).setCellValue(safeStr(r.getNombreProveedor()));
                row.createCell(15).setCellValue(safeStr(r.getCodProv())); // Cod. Art. Prov. (a.campo1)
                row.createCell(16).setCellValue(safeStr(r.getCodigoArt()));
                row.createCell(17).setCellValue(safeStr(r.getDescripcion()));
                setCellNum(row, 18, r.getCantidad(), numberStyle);
                setCellNum(row, 19, r.getPrecio() / divFactor, currencyStyle);
                setCellNum(row, 20, r.getDp(), numberStyle);
                setCellNum(row, 21, r.getDa(), numberStyle);
                setCellNum(row, 22, r.getDct(), numberStyle);
                setCellNum(row, 23, r.getDc(), numberStyle);
                setCellNum(row, 24, r.getDv(), numberStyle);
                setCellNum(row, 25, (r.getPrecio() * r.getCantidad()) / divFactor, currencyStyle);
                setCellNum(row, 26, r.getDescPctGlobal(), numberStyle);
                setCellNum(row, 27, r.getRenglonDg() / divFactor, currencyStyle);
                setCellNum(row, 28, r.getMontoIva() / divFactor, currencyStyle);
                setCellNum(row, 29, r.getIvaPct(), numberStyle);
                setCellNum(row, 30, r.getTotRenglonIva() / divFactor, currencyStyle);
                setCellNum(row, 31, r.getCostoVenta() / divFactor, currencyStyle);
                setCellNum(row, 32, r.getTotalCostoVenta() / divFactor, currencyStyle);
                setCellNum(row, 33, r.getTotCvDp() / divFactor, currencyStyle);
                setCellNum(row, 34, r.getMontoUtilidad() / divFactor, currencyStyle);
                setCellNum(row, 35, r.getUtilPct(), numberStyle);
                setCellNum(row, 36, r.getCostoActual(), currencyStyle);
                setCellNum(row, 37, r.getStockActual(), numberStyle);
                row.createCell(38).setCellValue(safeStr(r.getCodLinea()));
                row.createCell(39).setCellValue(safeStr(r.getLinea()));
                row.createCell(40).setCellValue(safeStr(r.getCodSub()));
                row.createCell(41).setCellValue(safeStr(r.getSubLinea()));
                row.createCell(42).setCellValue(safeStr(r.getAlmacen()));
                row.createCell(43).setCellValue(safeStr(r.getPedidoWeb()));
                row.createCell(44).setCellValue(safeStr(r.getUsuarioWeb()));
            }

            sheet.createFreezePane(0, 1);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
        return file;
    }

    public File exportProductosFacturados(List<MatrizVentasRow> listado) throws IOException {
        return exportMatrizVentas(listado, true);
    }

    /**
     * Exporta el reporte de Unidades y Valores por Proveedor con formato oficial de DroActiva (18 columnas).
     */
    public File exportUnidadesValores(List<MatrizVentasRow> listado, boolean isBs) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File file = new File(System.getProperty("user.dir"), "UnidadesValores_" + timestamp + ".xlsx");

        // Ordenar copia del listado por Fecha asc y Numero asc
        List<MatrizVentasRow> sorted = new ArrayList<>(listado != null ? listado : List.of());
        sorted.sort((a, b) -> {
            int c = safeStr(a.getFecha()).compareTo(safeStr(b.getFecha()));
            if (c != 0) return c;
            return safeStr(a.getNumero()).compareTo(safeStr(b.getNumero()));
        });

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Unidades y Valores");
            sheet.setDisplayGridlines(true);

            // Intentar cargar e insertar logo DroActiva en A1:C4 desde C:\Users\jeancarlos\Documents\Proyectos\dro-ai\Logo.png
            try {
                byte[] bytes = null;
                File externalLogo = new File("C:\\Users\\jeancarlos\\Documents\\Proyectos\\dro-ai\\Logo.png");
                if (!externalLogo.exists()) {
                    externalLogo = new File(System.getProperty("user.dir"), "Logo.png");
                }
                if (externalLogo.exists()) {
                    try (FileInputStream fis = new FileInputStream(externalLogo)) {
                        bytes = fis.readAllBytes();
                    }
                } else {
                    try (InputStream is = getClass().getResourceAsStream("/images/logo.png")) {
                        if (is != null) {
                            bytes = is.readAllBytes();
                        }
                    }
                }

                if (bytes != null && bytes.length > 0) {
                    int pictureIdx = wb.addPicture(bytes, Workbook.PICTURE_TYPE_PNG);
                    CreationHelper helper = wb.getCreationHelper();
                    Drawing<?> drawing = sheet.createDrawingPatriarch();
                    ClientAnchor anchor = helper.createClientAnchor();
                    anchor.setCol1(0);
                    anchor.setRow1(0);
                    anchor.setCol2(3);
                    anchor.setRow2(4);
                    drawing.createPicture(anchor, pictureIdx);
                }
            } catch (Exception e) {
                // Si ocurre algún inconveniente leyendo el logo, continuar sin interrumpir la exportación
            }

            // Título de factura en fila 1 (index 1)
            Row titleRow = sheet.createRow(1);
            Cell titleCell = titleRow.createCell(5);
            String mesTitle = "FACTURACION";
            if (!sorted.isEmpty() && sorted.get(0).getMes() != null && !sorted.get(0).getMes().isBlank()) {
                mesTitle = "FACTURACION " + sorted.get(0).getMes();
            }
            titleCell.setCellValue(mesTitle);

            XSSFFont titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setColor(new XSSFColor(new java.awt.Color(0, 51, 102), null));
            CellStyle titleStyle = wb.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.LEFT);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            titleCell.setCellStyle(titleStyle);

            // Fila de resumen de totales superiores (Fila 5, index 5)
            // total de Cantidad en col 16 y total de Total Renglon en col 17
            Row totalSummaryRow = sheet.createRow(5);
            Cell cellSumCant = totalSummaryRow.createCell(16);
            Cell cellSumVal = totalSummaryRow.createCell(17);

            double totalCant = 0;
            double totalMonto = 0;
            for (MatrizVentasRow r : sorted) {
                double divFactor = isBs ? 1.0 : (r.getTasa() > 0 ? r.getTasa() : 1.0);
                totalCant += r.getCantidad();
                totalMonto += r.getRenglonDg() / divFactor;
            }

            cellSumCant.setCellValue(totalCant);
            cellSumVal.setCellValue(totalMonto);

            CellStyle sumCantStyle = wb.createCellStyle();
            Font sumFont = wb.createFont();
            sumFont.setBold(true);
            sumFont.setFontHeightInPoints((short) 11);
            sumCantStyle.setFont(sumFont);
            sumCantStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0"));
            sumCantStyle.setAlignment(HorizontalAlignment.RIGHT);
            cellSumCant.setCellStyle(sumCantStyle);

            CellStyle sumValStyle = wb.createCellStyle();
            sumValStyle.setFont(sumFont);
            sumValStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
            sumValStyle.setAlignment(HorizontalAlignment.RIGHT);
            cellSumVal.setCellStyle(sumValStyle);

            // Fila de Encabezados (Fila 6, index 6)
            Row hr = sheet.createRow(6);
            hr.setHeightInPoints(24);

            String[] headers = {
                    "Numero", "Mes", "Fecha", "Proveedor", "Cod Cliente", "Rif", "Razon Social",
                    "Grupo", "Condicion de Pago", "Origen", "Nombre Vendedor", "Zona", "Ciudad", "Cod Prov", "Cod Art",
                    "Descripcion Art", "Cantidad", "Total Renglon"
            };

            // Estilo Teal (#00A89D) para encabezados
            CellStyle headerStyle = wb.createCellStyle();
            XSSFFont headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 10);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(0, 168, 157), null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);

            for (int i = 0; i < headers.length; i++) {
                Cell c = hr.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            // Estilos para datos
            CellStyle textStyle = wb.createCellStyle();
            textStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle numStyle = wb.createCellStyle();
            numStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0"));
            numStyle.setAlignment(HorizontalAlignment.RIGHT);
            numStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle currencyStyle = wb.createCellStyle();
            currencyStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
            currencyStyle.setAlignment(HorizontalAlignment.RIGHT);
            currencyStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            int rowIdx = 7;
            for (MatrizVentasRow r : sorted) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(19);

                double divFactor = isBs ? 1.0 : (r.getTasa() > 0 ? r.getTasa() : 1.0);

                Cell c0 = row.createCell(0); c0.setCellValue(safeStr(r.getNumero())); c0.setCellStyle(textStyle);
                Cell c1 = row.createCell(1); c1.setCellValue(safeStr(r.getMes())); c1.setCellStyle(textStyle);
                Cell c2 = row.createCell(2); c2.setCellValue(safeStr(r.getFecha())); c2.setCellStyle(textStyle);
                Cell c3 = row.createCell(3); c3.setCellValue(safeStr(r.getNombreProveedor())); c3.setCellStyle(textStyle);
                Cell c4 = row.createCell(4); c4.setCellValue(safeStr(r.getCodCliente())); c4.setCellStyle(textStyle);
                Cell c5 = row.createCell(5); c5.setCellValue(safeStr(r.getCiRif())); c5.setCellStyle(textStyle);
                Cell c6 = row.createCell(6); c6.setCellValue(safeStr(r.getNombreRazonSocial())); c6.setCellStyle(textStyle);
                Cell c7 = row.createCell(7); c7.setCellValue(safeStr(r.getGrupoCliente())); c7.setCellStyle(textStyle);
                Cell c8 = row.createCell(8); c8.setCellValue(safeStr(r.getCondicionPago())); c8.setCellStyle(textStyle);
                Cell c9 = row.createCell(9); c9.setCellValue(safeStr(r.getOrigen())); c9.setCellStyle(textStyle);
                Cell c10 = row.createCell(10); c10.setCellValue(safeStr(r.getNombreVendedor())); c10.setCellStyle(textStyle);
                Cell c11 = row.createCell(11); c11.setCellValue(safeStr(r.getZona())); c11.setCellStyle(textStyle);
                Cell c12 = row.createCell(12); c12.setCellValue(safeStr(r.getCiudad())); c12.setCellStyle(textStyle);
                Cell c13 = row.createCell(13); c13.setCellValue(safeStr(r.getCodProv())); c13.setCellStyle(textStyle);
                Cell c14 = row.createCell(14); c14.setCellValue(safeStr(r.getCodigoArt())); c14.setCellStyle(textStyle);
                Cell c15 = row.createCell(15); c15.setCellValue(safeStr(r.getDescripcion())); c15.setCellStyle(textStyle);

                Cell c16 = row.createCell(16); c16.setCellValue(r.getCantidad()); c16.setCellStyle(numStyle);
                Cell c17 = row.createCell(17); c17.setCellValue(r.getRenglonDg() / divFactor); c17.setCellStyle(currencyStyle);
            }

            // Inmovilizar paneles (Encabezado fijo)
            sheet.createFreezePane(0, 7);

            // Auto-filtro en la fila de encabezados
            if (rowIdx > 7) {
                sheet.setAutoFilter(new CellRangeAddress(6, rowIdx - 1, 0, 17));
            }

            // Ajustar anchos de columnas
            for (int col = 0; col < headers.length; col++) {
                sheet.autoSizeColumn(col);
                int width = sheet.getColumnWidth(col) + 1000;
                sheet.setColumnWidth(col, Math.min(Math.max(width, 2500), 15000));
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
        return file;
    }

    private String safeStr(String str) {
        return str != null ? str.trim() : "";
    }

    /**
     * Exporta la relación de comisiones quincenal individual para un vendedor siguiendo la plantilla oficial.
     */
    public File exportRelacionComisiones(List<ComisionRow> listado, String relacionNum,
                                          java.time.LocalDate fechaRelacion, String coVen,
                                          String nombreVendedor, File fileDestino) throws IOException {

        File file = fileDestino != null ? fileDestino : new File(System.getProperty("user.dir"), "COMISIONES_" + coVen + ".xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Font fontBold = wb.createFont(); fontBold.setFontName("Calibri"); fontBold.setFontHeightInPoints((short) 11); fontBold.setBold(true);
            Font fontTitle = wb.createFont(); fontTitle.setFontName("Calibri"); fontTitle.setFontHeightInPoints((short) 14); fontTitle.setBold(true);
            Font fontRegular = wb.createFont(); fontRegular.setFontName("Calibri"); fontRegular.setFontHeightInPoints((short) 10);

            CellStyle titleStyle = wb.createCellStyle(); titleStyle.setFont(fontTitle);

            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(fontBold);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle textStyle = wb.createCellStyle(); textStyle.setFont(fontRegular);
            CellStyle centerStyle = wb.createCellStyle(); centerStyle.setFont(fontRegular); centerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle numberStyle = wb.createCellStyle(); numberStyle.setFont(fontRegular);
            DataFormat df = wb.createDataFormat();
            numberStyle.setDataFormat(df.getFormat("#,##0.00"));

            CellStyle pctStyle = wb.createCellStyle(); pctStyle.setFont(fontRegular);
            pctStyle.setDataFormat(df.getFormat("0.00")); pctStyle.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle boldNumStyle = wb.createCellStyle(); boldNumStyle.setFont(fontBold);
            boldNumStyle.setDataFormat(df.getFormat("#,##0.00"));
            boldNumStyle.setBorderTop(BorderStyle.DOUBLE); boldNumStyle.setBorderBottom(BorderStyle.DOUBLE);

            writeComisionesSheet(wb, "Hoja1", listado, relacionNum, fechaRelacion, coVen, nombreVendedor,
                    titleStyle, headerStyle, textStyle, centerStyle, numberStyle, pctStyle, boldNumStyle);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
        return file;
    }

    /**
     * Exporta un archivo ÚNICO maestro de comisiones que contiene:
     * - Hoja 1: "GENERAL" (Todos los vendedores consolidados en un solo listado)
     * - Hojas adicionales: Una pestaña por cada vendedor dentro del mismo archivo Excel.
     */
    public File exportRelacionComisionesMaestro(List<ComisionRow> listadoCompleto, String relacionNum,
                                                java.time.LocalDate fechaRelacion, File fileDestino) throws IOException {

        File file = fileDestino != null ? fileDestino : new File(System.getProperty("user.dir"), "RELACION_DE_COMISIONES_GENERAL.xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Font fontBold = wb.createFont(); fontBold.setFontName("Calibri"); fontBold.setFontHeightInPoints((short) 11); fontBold.setBold(true);
            Font fontTitle = wb.createFont(); fontTitle.setFontName("Calibri"); fontTitle.setFontHeightInPoints((short) 14); fontTitle.setBold(true);
            Font fontRegular = wb.createFont(); fontRegular.setFontName("Calibri"); fontRegular.setFontHeightInPoints((short) 10);

            CellStyle titleStyle = wb.createCellStyle(); titleStyle.setFont(fontTitle);

            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(fontBold);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle textStyle = wb.createCellStyle(); textStyle.setFont(fontRegular);
            CellStyle centerStyle = wb.createCellStyle(); centerStyle.setFont(fontRegular); centerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle numberStyle = wb.createCellStyle(); numberStyle.setFont(fontRegular);
            DataFormat df = wb.createDataFormat();
            numberStyle.setDataFormat(df.getFormat("#,##0.00"));

            CellStyle pctStyle = wb.createCellStyle(); pctStyle.setFont(fontRegular);
            pctStyle.setDataFormat(df.getFormat("0.00")); pctStyle.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle boldNumStyle = wb.createCellStyle(); boldNumStyle.setFont(fontBold);
            boldNumStyle.setDataFormat(df.getFormat("#,##0.00"));
            boldNumStyle.setBorderTop(BorderStyle.DOUBLE); boldNumStyle.setBorderBottom(BorderStyle.DOUBLE);

            // 1. Hoja "GENERAL" (Consolidado)
            writeComisionesSheet(wb, "GENERAL", listadoCompleto, relacionNum, fechaRelacion, "GENERAL", "GENERAL",
                    titleStyle, headerStyle, textStyle, centerStyle, numberStyle, pctStyle, boldNumStyle);

            // 2. Pestañas individuales por vendedor en el mismo libro
            java.util.Map<String, List<ComisionRow>> mapByVen = new java.util.LinkedHashMap<>();
            for (ComisionRow r : listadoCompleto) {
                String cv = r.getCodigoVendedor() != null && !r.getCodigoVendedor().isBlank() ? r.getCodigoVendedor().trim() : "VARIOS";
                mapByVen.computeIfAbsent(cv, k -> new ArrayList<>()).add(r);
            }

            for (java.util.Map.Entry<String, List<ComisionRow>> entry : mapByVen.entrySet()) {
                String cv = entry.getKey();
                List<ComisionRow> listVen = entry.getValue();
                String nomVen = listVen.get(0).getNombreVendedor();
                writeComisionesSheet(wb, cv, listVen, relacionNum, fechaRelacion, cv, nomVen,
                        titleStyle, headerStyle, textStyle, centerStyle, numberStyle, pctStyle, boldNumStyle);
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }

        return file;
    }

    private void writeComisionesSheet(Workbook wb, String sheetName, List<ComisionRow> listado,
                                       String relacionNum, java.time.LocalDate fechaRelacion,
                                       String coVen, String nombreVendedor,
                                       CellStyle titleStyle, CellStyle headerStyle, CellStyle textStyle,
                                       CellStyle centerStyle, CellStyle numberStyle, CellStyle pctStyle,
                                       CellStyle boldNumStyle) {

        Sheet sheet = wb.createSheet(sheetName);

        // Fila 7: DROGUERIA ACTIVA, C.A.
        Row r7 = sheet.createRow(6);
        Cell c7 = r7.createCell(3);
        c7.setCellValue("DROGUERIA ACTIVA, C.A.");
        c7.setCellStyle(titleStyle);

        Cell c7n = r7.createCell(13);
        c7n.setCellValue("RELACION #");
        c7n.setCellStyle(titleStyle);

        Cell c7v = r7.createCell(15);
        c7v.setCellValue(relacionNum != null ? relacionNum : "00000160");
        c7v.setCellStyle(titleStyle);

        // Fila 8: FECHA
        Row r8 = sheet.createRow(7);
        Cell c8l = r8.createCell(14);
        c8l.setCellValue("FECHA:");
        c8l.setCellStyle(titleStyle);

        Cell c8v = r8.createCell(15);
        c8v.setCellValue(fechaRelacion != null ? fechaRelacion.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
        c8v.setCellStyle(titleStyle);

        // Fila 10: RELACION DE COMISIONES
        Row r10 = sheet.createRow(9);
        Cell c10 = r10.createCell(3);
        c10.setCellValue("RELACION DE COMISIONES");
        c10.setCellStyle(titleStyle);

        // Fila 12: VENDEDOR
        Row r12 = sheet.createRow(11);
        Cell c12l = r12.createCell(0);
        c12l.setCellValue("VENDEDOR:");
        c12l.setCellStyle(titleStyle);

        Cell c12v = r12.createCell(2);
        c12v.setCellValue(coVen != null ? coVen : "");
        c12v.setCellStyle(titleStyle);

        Cell c12m = r12.createCell(11);
        c12m.setCellValue("MONEDA BS");
        c12m.setCellStyle(titleStyle);

        // Fila 13: Encabezados de tabla
        String[] headers = {
                "#", "TIPO DOC.", "NUMERO DOCUMENTO", "CLASE", "FECHA DE EMISION",
                "FECHA DE VENCIMIENTO", "FECHA DE COBRO", "NUMERO COBRO", "DIAS  CALLE",
                "CODIGO CLIENTE", "NOMBRE CLIENTE", "MONTO DOCUMENTO", "% DESC",
                "MONTO COBRADO", "BASE COMISION", "% COMISION", "MONTO COMISION", "VENDEDOR:", "PSICO"
        };

        Row headerRow = sheet.createRow(12);
        headerRow.setHeightInPoints(24);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Datos
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        int rowIndex = 13;
        int sec = 1;

        double sumDoc = 0;
        double sumCob = 0;
        double sumBase = 0;
        double sumCom = 0;

        for (ComisionRow r : listado) {
            Row row = sheet.createRow(rowIndex++);
            row.setHeightInPoints(18);

            Cell cell0 = row.createCell(0); cell0.setCellValue(sec++); cell0.setCellStyle(centerStyle);
            Cell cell1 = row.createCell(1); cell1.setCellValue(safeStr(r.getTipoDoc())); cell1.setCellStyle(centerStyle);
            Cell cell2 = row.createCell(2); cell2.setCellValue(safeStr(r.getNumeroDocumento())); cell2.setCellStyle(centerStyle);
            Cell cell3 = row.createCell(3); cell3.setCellValue(safeStr(r.getClase())); cell3.setCellStyle(centerStyle);
            Cell cell4 = row.createCell(4); cell4.setCellValue(r.getFechaEmision() != null ? r.getFechaEmision().format(dateFmt) : ""); cell4.setCellStyle(centerStyle);
            Cell cell5 = row.createCell(5); cell5.setCellValue(r.getFechaVencimiento() != null ? r.getFechaVencimiento().format(dateFmt) : ""); cell5.setCellStyle(centerStyle);
            Cell cell6 = row.createCell(6); cell6.setCellValue(r.getFechaCobro() != null ? r.getFechaCobro().format(dateFmt) : ""); cell6.setCellStyle(centerStyle);
            Cell cell7 = row.createCell(7); cell7.setCellValue(safeStr(r.getNumeroCobro())); cell7.setCellStyle(centerStyle);
            Cell cell8 = row.createCell(8); cell8.setCellValue(r.getDiasCalle()); cell8.setCellStyle(centerStyle);
            Cell cell9 = row.createCell(9); cell9.setCellValue(safeStr(r.getCodigoCliente())); cell9.setCellStyle(centerStyle);
            Cell cell10 = row.createCell(10); cell10.setCellValue(safeStr(r.getNombreCliente())); cell10.setCellStyle(textStyle);

            Cell cell11 = row.createCell(11); cell11.setCellValue(r.getMontoDocumento()); cell11.setCellStyle(numberStyle);
            Cell cell12 = row.createCell(12); cell12.setCellValue(r.getPorcDesc()); cell12.setCellStyle(pctStyle);
            Cell cell13 = row.createCell(13); cell13.setCellValue(r.getMontoCobrado()); cell13.setCellStyle(numberStyle);
            Cell cell14 = row.createCell(14); cell14.setCellValue(r.getBaseComision()); cell14.setCellStyle(numberStyle);

            Cell cell15 = row.createCell(15);
            if (r.getPorcComision() > 0) {
                cell15.setCellValue(r.getPorcComision());
                cell15.setCellStyle(pctStyle);
            } else {
                cell15.setCellValue("-");
                cell15.setCellStyle(centerStyle);
            }

            Cell cell16 = row.createCell(16); cell16.setCellValue(r.getMontoComision()); cell16.setCellStyle(numberStyle);
            Cell cell17 = row.createCell(17); cell17.setCellValue(safeStr(r.getNombreVendedor() != null ? r.getNombreVendedor() : nombreVendedor)); cell17.setCellStyle(textStyle);
            Cell cell18 = row.createCell(18); cell18.setCellValue(safeStr(r.getPsico())); cell18.setCellStyle(centerStyle);

            sumDoc += r.getMontoDocumento();
            sumCob += r.getMontoCobrado();
            sumBase += r.getBaseComision();
            sumCom += r.getMontoComision();
        }

        // Fila Totales
        Row totRow = sheet.createRow(rowIndex);
        totRow.setHeightInPoints(20);
        Cell totLbl = totRow.createCell(10);
        totLbl.setCellValue("TOTALES:");
        totLbl.setCellStyle(headerStyle);

        Cell cTotDoc = totRow.createCell(11); cTotDoc.setCellValue(sumDoc); cTotDoc.setCellStyle(boldNumStyle);
        Cell cTotCob = totRow.createCell(13); cTotCob.setCellValue(sumCob); cTotCob.setCellStyle(boldNumStyle);
        Cell cTotBase = totRow.createCell(14); cTotBase.setCellValue(sumBase); cTotBase.setCellStyle(boldNumStyle);
        Cell cTotCom = totRow.createCell(16); cTotCom.setCellValue(sumCom); cTotCom.setCellStyle(boldNumStyle);

        // Ajustar anchos de columna
        for (int col = 0; col < headers.length; col++) {
            sheet.autoSizeColumn(col);
            int width = sheet.getColumnWidth(col) + 800;
            sheet.setColumnWidth(col, Math.min(Math.max(width, 2400), 14000));
        }
    }

    /**
     * Exporta el Maestro General de Clientes con todos sus campos comerciales, fiscales y logísticos.
     */
    public File exportMaestroClientes(List<ClienteMaestroRow> listado) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File file = new File(System.getProperty("user.dir"), "Maestro_Clientes_" + timestamp + ".xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle currencyStyle = createCurrencyStyle(wb);

            Sheet sheet = wb.createSheet("Maestro Clientes");
            String[] headers = {
                "Código", "R.I.F", "Nombres / Razón Social", "NIT", "Fecha Registro",
                "Contribuyente", "Tipo", "País", "Zona", "Ciudad",
                "Segmento", "Inactivo", "Vendedor", "Cod. Postal", "Cond. de Pago",
                "Email", "Crédito", "Teléfono", "Límite Crédito ($)", "Ruta",
                "Tipo de Persona", "Contacto", "Dirección"
            };

            Row hr = sheet.createRow(0);
            hr.setHeightInPoints(24);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hr.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (ClienteMaestroRow r : listado) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(18);

                row.createCell(0).setCellValue(safeStr(r.getCodigo()));
                row.createCell(1).setCellValue(safeStr(r.getRif()));
                row.createCell(2).setCellValue(safeStr(r.getNombre()));
                row.createCell(3).setCellValue(safeStr(r.getNit()));
                row.createCell(4).setCellValue(safeStr(r.getFechaRegistro()));
                row.createCell(5).setCellValue(safeStr(r.getContribuyente()));
                row.createCell(6).setCellValue(safeStr(r.getTipoCliente()));
                row.createCell(7).setCellValue(safeStr(r.getPais()));
                row.createCell(8).setCellValue(safeStr(r.getZona()));
                row.createCell(9).setCellValue(safeStr(r.getCiudad()));
                row.createCell(10).setCellValue(safeStr(r.getSegmento()));
                row.createCell(11).setCellValue(safeStr(r.getInactivo()));
                row.createCell(12).setCellValue(safeStr(r.getVendedor()));
                row.createCell(13).setCellValue(safeStr(r.getCodPostal()));
                row.createCell(14).setCellValue(safeStr(r.getCondPago()));
                row.createCell(15).setCellValue(safeStr(r.getEmail()));
                row.createCell(16).setCellValue(safeStr(r.getCredito()));
                row.createCell(17).setCellValue(safeStr(r.getTelefono()));

                Cell cLim = row.createCell(18);
                cLim.setCellValue(r.getLimiteCredito());
                cLim.setCellStyle(currencyStyle);

                row.createCell(19).setCellValue(safeStr(r.getRuta()));
                row.createCell(20).setCellValue(safeStr(r.getTipoPersona()));
                row.createCell(21).setCellValue(safeStr(r.getContacto()));
                row.createCell(22).setCellValue(safeStr(r.getDireccion()));
            }

            sheet.createFreezePane(0, 1);

            // Anchos automáticos
            for (int col = 0; col < headers.length; col++) {
                sheet.autoSizeColumn(col);
                int width = sheet.getColumnWidth(col) + 600;
                sheet.setColumnWidth(col, Math.min(Math.max(width, 2400), 12000));
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
        return file;
    }

    /**
     * Exporta el reporte de Estado de Cuentas por Cobrar (CxC) con pestaña de detalle y resúmenes.
     */
    public File exportCxCDocumentos(List<CxCDocumentoRow> listado, File fileToSave) throws IOException {
        return exportCxCDocumentos(listado, fileToSave, false);
    }

    public File exportCxCDocumentos(List<CxCDocumentoRow> listado, File fileToSave, boolean isBs) throws IOException {
        File file = (fileToSave != null) ? fileToSave
                : new File(System.getProperty("user.dir"), "EDC_Maestro_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = wb.createCellStyle();
            Font hFont = wb.createFont();
            hFont.setBold(true);
            hFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(hFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle currencyStyle = wb.createCellStyle();
            DataFormat df = wb.createDataFormat();
            currencyStyle.setDataFormat(df.getFormat("#,##0.00"));

            CellStyle tasaStyle = wb.createCellStyle();
            tasaStyle.setDataFormat(df.getFormat("#,##0.0000"));

            CellStyle centerStyle = wb.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle boldCurrencyStyle = wb.createCellStyle();
            Font bFont = wb.createFont();
            bFont.setBold(true);
            boldCurrencyStyle.setFont(bFont);
            boldCurrencyStyle.setDataFormat(df.getFormat("#,##0.00"));

            // ── HOJA 1: Detalle de Facturas ──
            Sheet sheet = wb.createSheet("Detalle Facturas");
            String[] headers = {
                    "CODIGO CLIENTE", "GRUPO CLIENTE", "CLIENTE", "FACT", "TIPO", "F-I", "EMISIÓN", "VENCIM", "DIAS DE VENC",
                    isBs ? "NETO (Bs)" : "NETO ($)",
                    isBs ? "IVA (Bs)" : "IVA ($)",
                    isBs ? "SALDO (Bs)" : "SALDO ($)",
                    "TASA",
                    isBs ? "TOTAL ($)" : "TOTAL Bs.",
                    isBs ? "POR VENCER (Bs)" : "POR VENCER ($)",
                    isBs ? "1-30 (Bs)" : "1-30 ($)",
                    isBs ? "31-60 (Bs)" : "31-60 ($)",
                    isBs ? "61-90 (Bs)" : "61-90 ($)",
                    isBs ? ">=91 (Bs)" : ">=91 ($)",
                    "cod. Vnd", "VEND.", "ANALISTA", "PEDIDO"
            };

            Row hr = sheet.createRow(0);
            hr.setHeightInPoints(24);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hr.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            int rowIdx = 1;
            if (listado != null) {
                listado.sort(java.util.Comparator
                        .comparing(CxCDocumentoRow::getFechaVencimiento, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                        .thenComparing(r -> r.getFactura() != null ? r.getFactura() : "", String.CASE_INSENSITIVE_ORDER)
                );
            }
            for (CxCDocumentoRow r : listado) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(18);

                double factor = isBs ? (r.getTasa() > 0 ? r.getTasa() : 1.0) : 1.0;

                row.createCell(0).setCellValue(safeStr(r.getCodigoCliente()));
                row.createCell(1).setCellValue(safeStr(r.getGrupoCliente()));
                row.createCell(2).setCellValue(safeStr(r.getCliente()));
                row.createCell(3).setCellValue(safeStr(r.getFactura()));
                
                Cell cTipo = row.createCell(4);
                cTipo.setCellValue(safeStr(r.getTipoDoc()));
                cTipo.setCellStyle(centerStyle);

                Cell cFi = row.createCell(5);
                cFi.setCellValue(safeStr(r.getFacturaImpaga()));
                cFi.setCellStyle(centerStyle);

                Cell cEmis = row.createCell(6);
                cEmis.setCellValue(r.getFechaEmision() != null ? r.getFechaEmision().format(dateFmt) : "");
                cEmis.setCellStyle(centerStyle);

                Cell cVenc = row.createCell(7);
                cVenc.setCellValue(r.getFechaVencimiento() != null ? r.getFechaVencimiento().format(dateFmt) : "");
                cVenc.setCellStyle(centerStyle);

                Cell cDias = row.createCell(8);
                cDias.setCellValue(r.getDiasVencimiento());
                cDias.setCellStyle(centerStyle);

                Cell cNeto = row.createCell(9);
                cNeto.setCellValue(r.getNeto() * factor);
                cNeto.setCellStyle(currencyStyle);

                Cell cIva = row.createCell(10);
                cIva.setCellValue(r.getIva() * factor);
                cIva.setCellStyle(currencyStyle);

                Cell cSaldo = row.createCell(11);
                cSaldo.setCellValue(isBs ? r.getTotalBs() : r.getSaldo());
                cSaldo.setCellStyle(currencyStyle);

                Cell cTasa = row.createCell(12);
                cTasa.setCellValue(r.getTasa());
                cTasa.setCellStyle(tasaStyle);

                Cell cAlt = row.createCell(13);
                cAlt.setCellValue(isBs ? r.getSaldo() : r.getTotalBs());
                cAlt.setCellStyle(currencyStyle);

                Cell cPv = row.createCell(14);
                cPv.setCellValue(r.getPorVencer() * factor);
                cPv.setCellStyle(currencyStyle);

                Cell c1a30 = row.createCell(15);
                c1a30.setCellValue(r.getVencido1a30() * factor);
                c1a30.setCellStyle(currencyStyle);

                Cell c31a60 = row.createCell(16);
                c31a60.setCellValue(r.getVencido31a60() * factor);
                c31a60.setCellStyle(currencyStyle);

                Cell c61a90 = row.createCell(17);
                c61a90.setCellValue(r.getVencido61a90() * factor);
                c61a90.setCellStyle(currencyStyle);

                Cell cMas91 = row.createCell(18);
                cMas91.setCellValue(r.getVencidoMas91() * factor);
                cMas91.setCellStyle(currencyStyle);

                Cell cCodV = row.createCell(19);
                cCodV.setCellValue(safeStr(r.getCodVendedor()));
                cCodV.setCellStyle(centerStyle);

                row.createCell(20).setCellValue(safeStr(r.getNombreVendedor()));
                
                Cell cAna = row.createCell(21);
                cAna.setCellValue(safeStr(r.getAnalista()));
                cAna.setCellStyle(centerStyle);

                row.createCell(22).setCellValue(safeStr(r.getPedido()));
            }

            sheet.createFreezePane(0, 1);
            for (int col = 0; col < headers.length; col++) {
                sheet.autoSizeColumn(col);
                int width = sheet.getColumnWidth(col) + 600;
                sheet.setColumnWidth(col, Math.min(Math.max(width, 2400), 12000));
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
        return file;
    }
}
