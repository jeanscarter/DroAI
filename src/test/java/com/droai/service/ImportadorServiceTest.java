package com.droai.service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ImportadorServiceTest {

    @Test
    public void testCargaEstructuraDescuentoVolumenEnDA() throws Exception {
        File tempFile = File.createTempFile("test_dv_structure", ".xlsx");
        tempFile.deleteOnExit();

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Descuentos");
            // Encabezados con la estructura de Descuento por Volumen
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("COD INT");
            header.createCell(1).setCellValue("DESCRIPCION");
            header.createCell(2).setCellValue("DESCUENTO DV");
            header.createCell(3).setCellValue("FECHA INICIAL");
            header.createCell(4).setCellValue("FECHA FINAL");

            // Fila 1
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("000101");
            r1.createCell(1).setCellValue("PRODUCTO 1");
            r1.createCell(2).setCellValue(12.5);
            r1.createCell(3).setCellValue("2026-10-01");
            r1.createCell(4).setCellValue("2026-10-31");

            // Fila 2: código como número y descuento con coma en texto
            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue(102.0); // POI numeric
            r2.createCell(1).setCellValue("PRODUCTO 2");
            r2.createCell(2).setCellValue("8,5%");
            r2.createCell(3).setCellValue("01/10/2026");
            r2.createCell(4).setCellValue("31/10/2026");

            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                wb.write(fos);
            }
        }

        ImportadorService service = new ImportadorService();

        // 1. Probar que leerExcelDescuentoDAItems lee perfectamente la estructura del archivo DV
        List<ImportadorService.DescuentoDAImportItem> daItems = service.leerExcelDescuentoDAItems(tempFile);
        assertNotNull(daItems);
        assertEquals(2, daItems.size());

        assertEquals("000101", daItems.get(0).getCodigoArticulo());
        assertEquals(12.5, daItems.get(0).getPorcentaje(), 0.001);
        assertEquals(java.sql.Date.valueOf("2026-10-01"), daItems.get(0).getFechaDesde());
        assertEquals(java.sql.Date.valueOf("2026-10-31"), daItems.get(0).getFechaHasta());

        assertEquals("102", daItems.get(1).getCodigoArticulo());
        assertEquals(8.5, daItems.get(1).getPorcentaje(), 0.001);
        assertEquals(java.sql.Date.valueOf("2026-10-01"), daItems.get(1).getFechaDesde());
        assertEquals(java.sql.Date.valueOf("2026-10-31"), daItems.get(1).getFechaHasta());

        // 2. Probar que leerExcelDescuentoDVItems también lo lee
        List<ImportadorService.DescuentoDVImportItem> dvItems = service.leerExcelDescuentoDVItems(tempFile);
        assertNotNull(dvItems);
        assertEquals(2, dvItems.size());
        assertEquals("000101", dvItems.get(0).getCodigoArticulo());
        assertEquals(12.5, dvItems.get(0).getPorcentaje(), 0.001);
        assertEquals("102", dvItems.get(1).getCodigoArticulo());
        assertEquals(8.5, dvItems.get(1).getPorcentaje(), 0.001);
    }

    @Test
    public void testExactColumnsPromocionFlashValmorca() throws Exception {
        File tempFile = File.createTempFile("test_valmorca_flash", ".xlsx");
        tempFile.deleteOnExit();

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sheet1");
            // Encabezados exactos del usuario:
            // COD INT, Descripcion de Articulo, Codigo de Barra, Descuento, Fecha ini, Fecha fin
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("COD INT");
            header.createCell(1).setCellValue("Descripcion de Articulo");
            header.createCell(2).setCellValue("Codigo de Barra");
            header.createCell(3).setCellValue("Descuento");
            header.createCell(4).setCellValue("Fecha ini");
            header.createCell(5).setCellValue("Fecha fin");

            // Fila de prueba
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue(6930.0); // Número de COD INT
            r1.createCell(1).setCellValue("ACEITE DE ALMENDRAS 60ML");
            r1.createCell(2).setCellValue(7.591020005296E12); // Codigo de Barra
            r1.createCell(3).setCellValue(0.05); // 5% decimal
            r1.createCell(4).setCellValue("2026-10-07");
            r1.createCell(5).setCellValue("2026-10-08");

            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                wb.write(fos);
            }
        }

        ImportadorService service = new ImportadorService();
        List<ImportadorService.DescuentoDAImportItem> items = service.leerExcelDescuentoDAItems(tempFile);

        assertNotNull(items);
        assertEquals(1, items.size());

        ImportadorService.DescuentoDAImportItem item = items.get(0);
        // Debe priorizar COD INT (6930) sobre Codigo de Barra
        assertEquals("6930", item.getCodigoArticulo());
        assertEquals(5.0, item.getPorcentaje(), 0.001);
        // Debe reconocer 'Fecha ini' y 'Fecha fin'
        assertNotNull(item.getFechaDesde(), "Fecha ini debe ser reconocida");
        assertNotNull(item.getFechaHasta(), "Fecha fin debe ser reconocida");
        assertEquals(java.sql.Date.valueOf("2026-10-07"), item.getFechaDesde());
        assertEquals(java.sql.Date.valueOf("2026-10-08"), item.getFechaHasta());
    }
}

