package com.droai.model;

/**
 * Fila de datos extraída de la hoja de importación Excel.
 * Cada instancia representa un artículo a actualizar en saArticulo.
 *
 * <p>Los campos se mapean dinámicamente según la configuración de la hoja
 * "Config" del archivo de importación.
 *
 * <p><b>Mapeo Excel → saArticulo (Profit Plus):</b>
 * <ul>
 *   <li>{@code codigo}   → {@code co_art}</li>
 *   <li>{@code descri}   → {@code art_des}</li>
 *   <li>{@code ref}      → {@code ref}</li>
 *   <li>{@code marca}    → {@code campo4}</li>
 *   <li>{@code grupo}    → {@code co_lin} (línea de artículo)</li>
 *   <li>{@code sgrupo}   → {@code co_subl} (sublínea)</li>
 *   <li>{@code cat}      → {@code co_cat} (categoría)</li>
 *   <li>{@code co_color} → {@code co_color}</li>
 *   <li>{@code co_prov}  → informativo / campo auxiliar</li>
 *   <li>{@code unidad}   → informativo</li>
 * </ul>
 */
public class ArticuloImportRow {

    private String codigo;
    private String tipo;
    private String descripcion;
    private String marca;
    private double impuesto;       // pimp: tasa de impuesto (ej. 16 → tipo_imp='1')
    private String referencia;

    // ── Catálogos Profit Plus ──
    private String grupo;          // co_lin (línea de artículo)
    private String sgrupo;         // co_subl (sublínea)
    private String cat;            // co_cat (categoría de artículo)
    private String coColor;        // co_color
    private String coProv;         // co_prov (proveedor, informativo)
    private String unidad;         // unidad (informativo)

    // ── Campos libres ──
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

    public String getGrupo() { return grupo; }
    public void setGrupo(String grupo) { this.grupo = grupo; }

    public String getSgrupo() { return sgrupo; }
    public void setSgrupo(String sgrupo) { this.sgrupo = sgrupo; }

    public String getCat() { return cat; }
    public void setCat(String cat) { this.cat = cat; }

    public String getCoColor() { return coColor; }
    public void setCoColor(String coColor) { this.coColor = coColor; }

    public String getCoProv() { return coProv; }
    public void setCoProv(String coProv) { this.coProv = coProv; }

    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }

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

    /**
     * Obtiene el tipo de artículo validado para Profit Plus.
     * <p>CHECK constraint: {@code tipo IN ('V','F','C','S','M','N','E')}
     * <p>Valores conocidos:
     * <ul>
     *   <li><b>V</b> = Venta (99.97% de los artículos)</li>
     *   <li><b>S</b> = Servicio</li>
     *   <li><b>F</b> = Fabricado, <b>C</b> = Compuesto, <b>M</b> = Materia prima</li>
     *   <li><b>N</b> = No inventariable, <b>E</b> = Ensamblado</li>
     * </ul>
     * Si no se especifica o no es válido, retorna 'V' como default seguro.
     */
    public String getTipoValidado() {
        if (tipo != null && !tipo.isBlank()) {
            String t = tipo.trim().toUpperCase();
            if (t.length() == 1 && "VFCSMNE".contains(t)) {
                return t;
            }
        }
        return "V"; // Default: artículo de Venta
    }
}
