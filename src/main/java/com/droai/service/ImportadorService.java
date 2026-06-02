package com.droai.service;

import com.droai.dao.ImportacionDAO;
import com.droai.dao.ImportacionDAO.ImportResult;
import com.droai.dao.ImportacionDAO.ValidationResult;
import com.droai.model.ArticuloImportRow;
import com.droai.model.ImportConfig;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para importación masiva de datos desde un archivo Excel
 * con estructura de configuración dinámica (hoja Config + hoja de datos).
 *
 * <p>
 * Flujo completo:
 * <ol>
 * <li>{@link #leerConfiguracion(File)} → Parse de hoja Config</li>
 * <li>{@link #previsualizarDatos(File, ImportConfig, int)} → Preview en
 * JTable</li>
 * <li>{@link #validarEnMemoria(File)} → Validación de catálogos en BD</li>
 * <li>{@link #procesarImportacion(File, ProgressCallback)} → UPSERT masivo</li>
 * </ol>
 */
public class ImportadorService {

    private static final int BATCH_SIZE = 200;

    private final ImportacionDAO dao;

    public ImportadorService() {
        this.dao = new ImportacionDAO();
    }

    // ═══════════════════════════════════════════════════════════════
    // 1. Lectura de la Hoja Config
    // ═══════════════════════════════════════════════════════════════

    /**
     * Lee la hoja "Config" del archivo Excel y construye un {@link ImportConfig}
     * con el nombre de hoja destino, rango de filas y mapeo de columnas.
     *
     * <p>
     * Estructura esperada de la hoja Config:
     * 
     * <pre>
     *   Fila 1: Input | Hoja1
     *   Fila 2: fila inicio | 2 | 4541 | (SQL template)
     *   Fila 3+: nombre_campo | letra_columna
     * </pre>
     */
    public ImportConfig leerConfiguracion(File file) throws IOException {
        ImportConfig config = new ImportConfig();

        try (FileInputStream fis = new FileInputStream(file);
                Workbook wb = new XSSFWorkbook(fis)) {

            Sheet configSheet = wb.getSheet("Config");
            if (configSheet == null) {
                for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                    String name = wb.getSheetName(i);
                    if (name.equalsIgnoreCase("Config") || name.equalsIgnoreCase("Configuracion")) {
                        configSheet = wb.getSheetAt(i);
                        break;
                    }
                }
            }
            if (configSheet == null) {
                throw new IOException("No se encontró la hoja 'Config' en el archivo Excel.");
            }

            // Fila 1: "Input" | "Hoja1"
            Row row0 = configSheet.getRow(0);
            if (row0 != null) {
                config.setHojaInput(getCellString(row0, 1));
            }
            if (config.getHojaInput() == null || config.getHojaInput().isBlank()) {
                config.setHojaInput("Hoja1");
            }

            // Fila 2: "fila inicio" | inicio | fin | sql_template
            Row row1 = configSheet.getRow(1);
            if (row1 != null) {
                config.setFilaInicio(getCellInt(row1, 1));
                config.setFilaFin(getCellInt(row1, 2));

                StringBuilder sql = new StringBuilder();
                for (int c = 3; c <= row1.getLastCellNum(); c++) {
                    String val = getCellString(row1, c);
                    if (val != null && !val.isBlank()) {
                        if (!sql.isEmpty())
                            sql.append(" ");
                        sql.append(val);
                    }
                }
                config.setSqlTemplate(sql.toString());
            }

            // Filas 3+: mapeo de campos (nombre, letra_columna)
            for (int r = 2; r <= configSheet.getLastRowNum(); r++) {
                Row mapRow = configSheet.getRow(r);
                if (mapRow == null)
                    continue;

                String fieldName = getCellString(mapRow, 0);
                String colLetter = getCellString(mapRow, 1);

                if (fieldName != null && !fieldName.isBlank()
                        && colLetter != null && !colLetter.isBlank()) {
                    int colIdx = ImportConfig.letterToIndex(colLetter);
                    if (colIdx >= 0) {
                        config.putColumn(fieldName, colIdx);
                    }
                }
            }
        }

        // ── Auto-detección de columnas desde la fila de encabezados ──
        // Sobreescribe los mapeos de Config si los headers reales no coinciden.
        autoDetectFromHeaders(file, config);

        System.out.println("[ImportadorService] Config leída: " + config);
        return config;
    }

    /**
     * Lee los encabezados reales de la hoja de datos y sobreescribe el mapeo
     * de Config con las columnas correctas. Esto corrige desfases entre la
     * hoja Config y la estructura real del Excel.
     */
    private void autoDetectFromHeaders(File file, ImportConfig config) {
        try (FileInputStream fis = new FileInputStream(file);
                Workbook wb = new XSSFWorkbook(fis)) {

            Sheet dataSheet = wb.getSheet(config.getHojaInput());
            if (dataSheet == null)
                return;

            int headerRowIdx = Math.max(0, config.getFilaInicio() - 2);
            Row headerRow = dataSheet.getRow(headerRowIdx);
            if (headerRow == null)
                return;

            // Mapeo: nombre de encabezado Excel (lowercase) → nombre de campo lógico
            java.util.Map<String, String> headerToField = java.util.Map.ofEntries(
                    java.util.Map.entry("codigo", "codigo"),
                    java.util.Map.entry("tipo", "tipo"),
                    java.util.Map.entry("descripcion", "descri"),
                    java.util.Map.entry("referencia", "ref"),
                    java.util.Map.entry("marca", "marca"),
                    java.util.Map.entry("stock", "campo1"),
                    java.util.Map.entry("color", "co_color"),
                    java.util.Map.entry("unidad", "unidad"),
                    java.util.Map.entry("grupo", "grupo"),
                    java.util.Map.entry("sgrupo", "sgrupo"),
                    java.util.Map.entry("categoria", "cat"),
                    java.util.Map.entry("proveedor", "co_prov"),
                    java.util.Map.entry("procede", "procede"),
                    java.util.Map.entry("ubicacion", "ubic"));

            boolean detected = false;
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                String header = getCellString(headerRow, c);
                if (header == null || header.isBlank())
                    continue;
                String normalized = header.trim().toLowerCase();
                String fieldName = headerToField.get(normalized);
                if (fieldName != null) {
                    config.putColumn(fieldName, c);
                    detected = true;
                }
            }

            if (detected) {
                System.out.println("[ImportadorService] Auto-detección de headers aplicada: " + config.getColumnMap());
            }

        } catch (IOException e) {
            System.err.println("[ImportadorService] No se pudo auto-detectar headers: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. Previsualización de Datos
    // ═══════════════════════════════════════════════════════════════

    /**
     * Lee hasta {@code limit} filas de la hoja de datos para previsualización.
     */
    public PreviewResult previsualizarDatos(File file, ImportConfig config, int limit) throws IOException {
        List<ArticuloImportRow> rows = new ArrayList<>();
        String[] headers = null;

        try (FileInputStream fis = new FileInputStream(file);
                Workbook wb = new XSSFWorkbook(fis)) {

            Sheet dataSheet = wb.getSheet(config.getHojaInput());
            if (dataSheet == null) {
                throw new IOException("No se encontró la hoja '" + config.getHojaInput() + "'.");
            }

            int headerRowIdx = Math.max(0, config.getFilaInicio() - 2);
            Row headerRow = dataSheet.getRow(headerRowIdx);
            if (headerRow != null) {
                int maxCol = headerRow.getLastCellNum();
                headers = new String[maxCol];
                for (int c = 0; c < maxCol; c++) {
                    headers[c] = getCellString(headerRow, c);
                    if (headers[c] == null)
                        headers[c] = "Col " + (c + 1);
                }
            }

            int startRow = Math.max(0, config.getFilaInicio() - 1);
            int endRow = config.getFilaFin() > 0
                    ? Math.min(config.getFilaFin() - 1, dataSheet.getLastRowNum())
                    : dataSheet.getLastRowNum();
            int count = 0;

            for (int r = startRow; r <= endRow && count < limit; r++) {
                Row row = dataSheet.getRow(r);
                if (row == null)
                    continue;

                ArticuloImportRow importRow = parseDataRow(row, config);
                if (importRow.getCodigo() != null && !importRow.getCodigo().isBlank()) {
                    rows.add(importRow);
                    count++;
                }
            }
        }

        return new PreviewResult(headers, rows);
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. Validación en Memoria + BD
    // ═══════════════════════════════════════════════════════════════

    /**
     * Lee todas las filas del Excel y las valida contra la BD.
     * Verifica existencia de catálogos dependientes.
     *
     * @return resultado con errores, conteo de existentes y nuevos.
     */
    public ValidationResult validarEnMemoria(File file) throws Exception {
        ImportConfig config = leerConfiguracion(file);
        List<ArticuloImportRow> allRows = leerTodasLasFilas(file, config);
        return dao.validarCatalogos(allRows);
    }

    // ═══════════════════════════════════════════════════════════════
    // 4. Importación Masiva (UPSERT)
    // ═══════════════════════════════════════════════════════════════

    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(int current, int total, String message);
    }

    /**
     * Lee todo el archivo, valida, y procesa UPSERT por lotes.
     *
     * @param file     archivo Excel.
     * @param callback callback de progreso (invocado fuera del EDT).
     * @return total de registros procesados (actualizados + insertados).
     */
    public int procesarImportacion(File file, ProgressCallback callback) throws Exception {
        ImportConfig config = leerConfiguracion(file);

        // --- Fase 1: Lectura ---
        if (callback != null)
            callback.onProgress(0, 0, "Leyendo archivo Excel...");
        List<ArticuloImportRow> allRows = leerTodasLasFilas(file, config);

        int total = allRows.size();
        if (total == 0) {
            if (callback != null)
                callback.onProgress(0, 0, "No se encontraron filas válidas.");
            return 0;
        }

        // --- Fase 2: Validación ---
        if (callback != null)
            callback.onProgress(0, total, "Validando catálogos dependientes...");
        ValidationResult validation = dao.validarCatalogos(allRows);

        if (!validation.valid()) {
            String errMsg = String.join("\n", validation.errores());
            throw new IllegalStateException(
                    "Validación fallida — importación abortada:\n" + errMsg);
        }

        // --- Fase 3: UPSERT por lotes ---
        int totalActualizados = 0, totalInsertados = 0, totalOmitidos = 0;

        for (int i = 0; i < total; i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, total);
            List<ArticuloImportRow> lote = allRows.subList(i, end);

            ImportResult batchResult = dao.procesarLote(lote);
            totalActualizados += batchResult.actualizados();
            totalInsertados += batchResult.insertados();
            totalOmitidos += batchResult.omitidos();

            if (callback != null) {
                callback.onProgress(end, total,
                        "Lote %d/%d — %d/%d filas (Act: %d | Ins: %d | Om: %d)"
                                .formatted(
                                        (i / BATCH_SIZE) + 1,
                                        (int) Math.ceil((double) total / BATCH_SIZE),
                                        end, total,
                                        totalActualizados, totalInsertados, totalOmitidos));
            }
        }

        String finalMsg = "✔ Importación completada: %d actualizados, %d insertados, %d omitidos."
                .formatted(totalActualizados, totalInsertados, totalOmitidos);
        if (callback != null) {
            callback.onProgress(total, total, finalMsg);
        }

        return totalActualizados + totalInsertados;
    }

    // ═══════════════════════════════════════════════════════════════
    // Helpers internos
    // ═══════════════════════════════════════════════════════════════

    private List<ArticuloImportRow> leerTodasLasFilas(File file, ImportConfig config) throws IOException {
        List<ArticuloImportRow> allRows = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
                Workbook wb = new XSSFWorkbook(fis)) {

            Sheet dataSheet = wb.getSheet(config.getHojaInput());
            if (dataSheet == null) {
                throw new IOException("No se encontró la hoja '" + config.getHojaInput() + "'.");
            }

            int startRow = Math.max(0, config.getFilaInicio() - 1);
            int endRow = config.getFilaFin() > 0
                    ? Math.min(config.getFilaFin() - 1, dataSheet.getLastRowNum())
                    : dataSheet.getLastRowNum();

            for (int r = startRow; r <= endRow; r++) {
                Row row = dataSheet.getRow(r);
                if (row == null)
                    continue;

                ArticuloImportRow importRow = parseDataRow(row, config);
                if (importRow.getCodigo() != null && !importRow.getCodigo().isBlank()) {
                    allRows.add(importRow);
                }
            }
        }

        return allRows;
    }

    private ArticuloImportRow parseDataRow(Row row, ImportConfig config) {
        ArticuloImportRow r = new ArticuloImportRow();

        int maxCol = row.getLastCellNum();
        String[] raw = new String[maxCol];
        for (int c = 0; c < maxCol; c++) {
            raw[c] = getCellString(row, c);
        }
        r.setRawValues(raw);

        r.setCodigo(getField(raw, config, "codigo"));
        r.setTipo(getField(raw, config, "tipo"));
        r.setDescripcion(getField(raw, config, "descri"));
        r.setMarca(getField(raw, config, "marca"));
        r.setReferencia(getField(raw, config, "ref"));

        // ── Catálogos Profit Plus ──
        r.setGrupo(getField(raw, config, "grupo")); // co_lin
        r.setSgrupo(getField(raw, config, "sgrupo")); // co_subl
        r.setCat(getField(raw, config, "cat")); // co_cat
        r.setCoColor(getField(raw, config, "co_color")); // co_color
        r.setCoProv(getField(raw, config, "co_prov")); // proveedor
        r.setUnidad(getField(raw, config, "unidad")); // unidad (informativo)

        // ── Campos libres ──
        r.setCampo1(getField(raw, config, "campo1"));
        r.setCampo2(getField(raw, config, "campo2"));
        r.setCampo3(getField(raw, config, "campo3"));
        r.setCampo4(getField(raw, config, "campo4"));
        r.setCampo5(getField(raw, config, "campo5"));
        r.setCampo6(getField(raw, config, "campo6"));

        // ── Procedencia ──
        r.setProcede(getField(raw, config, "procede"));

        String pimpStr = getField(raw, config, "pimp");
        if (pimpStr != null && !pimpStr.isBlank()) {
            try {
                r.setImpuesto(Double.parseDouble(pimpStr.trim()));
            } catch (NumberFormatException ignored) {
            }
        }

        return r;
    }

    private String getField(String[] raw, ImportConfig config, String fieldName) {
        int idx = config.getColumnIndex(fieldName);
        if (idx >= 0 && idx < raw.length && raw[idx] != null) {
            String val = raw[idx].trim();
            if (val.startsWith("'")) {
                val = val.substring(1).trim();
            }
            return val;
        }
        return null;
    }

    private String getCellString(Row row, int col) {
        if (row == null)
            return null;
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null)
            return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        yield String.valueOf(cell.getNumericCellValue());
                    } catch (Exception e2) {
                        yield cell.getCellFormula();
                    }
                }
            }
            default -> null;
        };
    }

    private int getCellInt(Row row, int col) {
        String val = getCellString(row, col);
        if (val == null || val.isBlank())
            return 0;
        try {
            return (int) Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Contiene los headers detectados y las filas leídas para previsualización.
     */
    public record PreviewResult(String[] headers, List<ArticuloImportRow> rows) {
    }
}
