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
}
