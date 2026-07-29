package com.droai.service;

import com.droai.dao.CargaMasivaCostosPreciosDAO;
import com.droai.model.CargaMasivaCostosPreciosRow;

import org.apache.poi.ss.usermodel.*;
import java.io.File;
import java.io.FileInputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CargaMasivaCostosPreciosService {

    private final CargaMasivaCostosPreciosDAO dao;

    public CargaMasivaCostosPreciosService() {
        this.dao = new CargaMasivaCostosPreciosDAO();
    }

    /**
     * Lee un archivo Excel (.xlsx), extrae código interno, costo y precio,
     * y consulta la BD para armar el modelo de vista previa.
     */
    public List<CargaMasivaCostosPreciosRow> cargarDesdeExcel(File file) throws Exception {
        List<CargaMasivaCostosPreciosRow> filas = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() < 2) {
                throw new IllegalArgumentException("El archivo Excel no contiene filas de datos.");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("El archivo Excel no posee fila de encabezados.");
            }

            int colCodigo = -1;
            int colCosto = -1;
            int colPrecio = -1;

            for (Cell cell : headerRow) {
                String val = getCellValueAsString(cell).toLowerCase().trim();
                if (val.contains("cod") || val.contains("co_art") || val.contains("código") || val.contains("codigo") || val.contains("articulo")) {
                    if (colCodigo == -1) colCodigo = cell.getColumnIndex();
                } else if (val.contains("costo")) {
                    if (colCosto == -1) colCosto = cell.getColumnIndex();
                } else if (val.contains("precio")) {
                    if (colPrecio == -1) colPrecio = cell.getColumnIndex();
                }
            }

            // Si no se detectaron por encabezado exacto, asumir orden por defecto: Col 0: Código, Col 1: Costo, Col 2: Precio
            if (colCodigo == -1) colCodigo = 0;
            if (colCosto == -1) colCosto = 1;
            if (colPrecio == -1) colPrecio = 2;

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String codigo = getCellValueAsString(row.getCell(colCodigo)).trim();
                if (codigo.isEmpty()) continue;

                double costo = parseDoubleSafe(getCellValueAsString(row.getCell(colCosto)));
                double precio = parseDoubleSafe(getCellValueAsString(row.getCell(colPrecio)));

                CargaMasivaCostosPreciosRow item = new CargaMasivaCostosPreciosRow();
                item.setCoArt(codigo);
                item.setCostoNuevo(costo);
                item.setPrecio1Nuevo(precio);

                filas.add(item);
            }
        }

        // Consultar los datos actuales en BD (DROA_A) para enriquecer la tabla de vista previa
        dao.enriquecerConDatosBd(filas);
        return filas;
    }

    /**
     * Aplica la carga masiva en la base de datos (DROA_A).
     */
    public int ejecutarCargaMasiva(List<CargaMasivaCostosPreciosRow> filas) throws SQLException {
        return dao.ejecutarCargaMasiva(filas);
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        CellType type = cell.getCellType();
        if (type == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (type == CellType.NUMERIC) {
            double val = cell.getNumericCellValue();
            if (val == (long) val) {
                return String.valueOf((long) val);
            } else {
                return String.valueOf(val);
            }
        } else if (type == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        } else if (type == CellType.FORMULA) {
            try {
                return String.valueOf(cell.getNumericCellValue());
            } catch (Exception e) {
                try {
                    return cell.getStringCellValue();
                } catch (Exception ex) {
                    return "";
                }
            }
        }
        return "";
    }

    private double parseDoubleSafe(String text) {
        if (text == null || text.isBlank()) return 0.0;
        try {
            String clean = text.replace(",", ".").replaceAll("[^0-9.]", "").trim();
            if (clean.isEmpty()) return 0.0;
            return Double.parseDouble(clean);
        } catch (Exception e) {
            return 0.0;
        }
    }
}
