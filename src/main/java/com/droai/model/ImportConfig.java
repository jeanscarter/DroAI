package com.droai.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuración extraída de la hoja "Config" del archivo Excel de importación.
 * Almacena: hoja de datos destino, rango de filas, mapeo columna→índice,
 * y la plantilla SQL original (informativo).
 */
public class ImportConfig {

    /** Nombre de la hoja que contiene los datos (ej. "Hoja1"). */
    private String hojaInput;

    /** Fila de inicio (1-based en Excel, se convierte a 0-based para POI). */
    private int filaInicio;

    /** Última fila a leer (1-based). */
    private int filaFin;

    /** Plantilla SQL original leída de la hoja Config (solo informativo). */
    private String sqlTemplate;

    /**
     * Mapeo: nombre lógico del campo → índice de columna en la hoja de datos.
     * Ejemplo: {"codigo" → 0, "descri" → 1, "marca" → 3, "pimp" → 4}
     */
    private final Map<String, Integer> columnMap = new LinkedHashMap<>();

    public ImportConfig() {}

    // --- Getters / Setters ---

    public String getHojaInput() { return hojaInput; }
    public void setHojaInput(String hojaInput) { this.hojaInput = hojaInput; }

    public int getFilaInicio() { return filaInicio; }
    public void setFilaInicio(int filaInicio) { this.filaInicio = filaInicio; }

    public int getFilaFin() { return filaFin; }
    public void setFilaFin(int filaFin) { this.filaFin = filaFin; }

    public String getSqlTemplate() { return sqlTemplate; }
    public void setSqlTemplate(String sqlTemplate) { this.sqlTemplate = sqlTemplate; }

    public Map<String, Integer> getColumnMap() { return columnMap; }

    public void putColumn(String fieldName, int colIndex) {
        columnMap.put(fieldName.trim().toLowerCase(), colIndex);
    }

    /**
     * Obtiene el índice de columna para un campo lógico dado.
     * @return índice 0-based, o -1 si no existe.
     */
    public int getColumnIndex(String fieldName) {
        return columnMap.getOrDefault(fieldName.trim().toLowerCase(), -1);
    }

    /**
     * Convierte una letra de columna Excel (A, B, ..., Z, AA, AB...) a índice 0-based.
     */
    public static int letterToIndex(String letter) {
        if (letter == null || letter.isBlank()) return -1;
        letter = letter.trim().toUpperCase();
        int index = 0;
        for (int i = 0; i < letter.length(); i++) {
            index = index * 26 + (letter.charAt(i) - 'A' + 1);
        }
        return index - 1;
    }

    @Override
    public String toString() {
        return "ImportConfig{hoja='%s', filas=%d→%d, campos=%s}"
                .formatted(hojaInput, filaInicio, filaFin, columnMap);
    }
}
