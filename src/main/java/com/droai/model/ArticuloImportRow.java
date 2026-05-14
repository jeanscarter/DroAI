package com.droai.model;

/**
 * Fila de datos extraída de la hoja de importación Excel.
 * Cada instancia representa un artículo a actualizar en saArticulo.
 *
 * <p>Los campos se mapean dinámicamente según la configuración de la hoja
 * "Config" del archivo de importación.
 */
public class ArticuloImportRow {

    private String codigo;
    private String tipo;
    private String descripcion;
    private String marca;
    private double impuesto;       // pimp: tasa de impuesto (ej. 16 → tipo_imp='1')
    private String referencia;
    private String campo1;
    private String campo2;
    private String campo3;
    private String campo4;
    private String campo5;
    private String campo6;

    /** Todos los valores crudos de la fila, indexados por columna. */
    private String[] rawValues;

    public ArticuloImportRow() {}

    // --- Getters / Setters ---

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public double getImpuesto() { return impuesto; }
    public void setImpuesto(double impuesto) { this.impuesto = impuesto; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public String getCampo1() { return campo1; }
    public void setCampo1(String campo1) { this.campo1 = campo1; }

    public String getCampo2() { return campo2; }
    public void setCampo2(String campo2) { this.campo2 = campo2; }

    public String getCampo3() { return campo3; }
    public void setCampo3(String campo3) { this.campo3 = campo3; }

    public String getCampo4() { return campo4; }
    public void setCampo4(String campo4) { this.campo4 = campo4; }

    public String getCampo5() { return campo5; }
    public void setCampo5(String campo5) { this.campo5 = campo5; }

    public String getCampo6() { return campo6; }
    public void setCampo6(String campo6) { this.campo6 = campo6; }

    public String[] getRawValues() { return rawValues; }
    public void setRawValues(String[] rawValues) { this.rawValues = rawValues; }

    /**
     * Calcula el tipo de impuesto para Profit Plus.
     * Si pimp == 16 → '1' (gravado), de lo contrario → '7' (exento).
     */
    public String getTipoImpCalculado() {
        return impuesto == 16.0 ? "1" : "7";
    }
}
