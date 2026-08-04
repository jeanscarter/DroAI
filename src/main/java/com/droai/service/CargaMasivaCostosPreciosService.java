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
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                throw new IllegalArgumentException("El archivo Excel no contiene filas de datos.");
            }

            Row row0 = sheet.getRow(0);
            if (row0 == null) {
                throw new IllegalArgumentException("El archivo Excel no posee fila inicial.");
            }

            int colCodigo = -1;
            int colCosto = -1;
            int colPrecio = -1;
            boolean tieneEncabezado = false;

            // Evaluar si la fila 0 contiene palabras clave de encabezado
            for (Cell cell : row0) {
                String val = getCellValueAsString(cell).toLowerCase().trim();
                if (val.contains("cod") || val.contains("co_art") || val.contains("código") || val.contains("codigo") || val.contains("articulo")) {
                    if (colCodigo == -1) colCodigo = cell.getColumnIndex();
                    tieneEncabezado = true;
                } else if (val.contains("costo")) {
                    if (colCosto == -1) colCosto = cell.getColumnIndex();
                    tieneEncabezado = true;
                } else if (val.contains("precio")) {
                    if (colPrecio == -1) colPrecio = cell.getColumnIndex();
                    tieneEncabezado = true;
                }
            }

            int startRow;
            if (tieneEncabezado) {
                startRow = 1;
                if (colCodigo == -1) colCodigo = 0;
                if (colCosto == -1) colCosto = 1;
                if (colPrecio == -1) colPrecio = 2;
            } else {
                startRow = 0;
                colCodigo = 0;
                colCosto = 1;
                colPrecio = 2;
            }

            for (int r = startRow; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String codigo = getCellValueAsString(row.getCell(colCodigo)).trim();
                if (codigo.endsWith(".0")) {
                    codigo = codigo.substring(0, codigo.length() - 2);
                }
                if (codigo.isEmpty()) continue;

                double costoUsd = parseDoubleSafe(getCellValueAsString(row.getCell(colCosto)));
                double precioUsd = parseDoubleSafe(getCellValueAsString(row.getCell(colPrecio)));

                CargaMasivaCostosPreciosRow item = new CargaMasivaCostosPreciosRow();
                item.setCoArt(codigo);
                item.setCostoNuevoUsd(costoUsd);
                item.setPrecio1NuevoUsd(precioUsd);

                filas.add(item);
            }
        }

        // Consultar los datos actuales en BD (DROA_A) y aplicar conversión con tasa de Profit
        dao.enriquecerConDatosBd(filas);
        return filas;
    }

    /**
     * Aplica la carga masiva en la base de datos (DROA_A).
     */
    public int ejecutarCargaMasiva(List<CargaMasivaCostosPreciosRow> filas, boolean forzar) throws SQLException {
        return dao.ejecutarCargaMasiva(filas, forzar);
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
