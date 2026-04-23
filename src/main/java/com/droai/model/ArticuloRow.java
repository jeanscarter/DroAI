package com.droai.model;

/**
 * Modelo de datos para el Catálogo de Productos (basado en saArticulo).
 * Contiene los campos visibles en la UI (12 columnas) y campos adicionales para exportación Excel.
 */
public class ArticuloRow {

    // === Campos visibles en la UI (12 columnas) ===
    private String codigo;
    private String descripcion;
    private String marca;          // mapeado desde prov_des (Proveedor)
    private double existencia;     // stock total
    private String udm;            // unidad de medida
    private double costoFabrica;
    private double arancelPct;
    private double costoOm;
    private double utilPct;        // calculado
    private double precio1;
    private double ivaPct;
    private double precioCiva;     // calculado: precio1 * (1 + ivaPct/100)

    // === Campos adicionales para exportación Excel ===
    private double precio2;
    private double precio3;
    private String codLinea;
    private String linea;
    private String codSub;
    private String subLinea;
    private String codProveedor;
    private String nombreProveedor;
    private String referencia;
    private String modelo;
    private String procedencia;
    private double peso;
    private double volumen;

    public ArticuloRow() {}

    // --- Getters y Setters ---

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public double getExistencia() { return existencia; }
    public void setExistencia(double existencia) { this.existencia = existencia; }

    public String getUdm() { return udm; }
    public void setUdm(String udm) { this.udm = udm; }

    public double getCostoFabrica() { return costoFabrica; }
    public void setCostoFabrica(double costoFabrica) { this.costoFabrica = costoFabrica; }

    public double getArancelPct() { return arancelPct; }
    public void setArancelPct(double arancelPct) { this.arancelPct = arancelPct; }

    public double getCostoOm() { return costoOm; }
    public void setCostoOm(double costoOm) { this.costoOm = costoOm; }

    public double getUtilPct() { return utilPct; }
    public void setUtilPct(double utilPct) { this.utilPct = utilPct; }

    public double getPrecio1() { return precio1; }
    public void setPrecio1(double precio1) { this.precio1 = precio1; }

    public double getIvaPct() { return ivaPct; }
    public void setIvaPct(double ivaPct) { this.ivaPct = ivaPct; }

    public double getPrecioCiva() { return precioCiva; }
    public void setPrecioCiva(double precioCiva) { this.precioCiva = precioCiva; }

    public double getPrecio2() { return precio2; }
    public void setPrecio2(double precio2) { this.precio2 = precio2; }

    public double getPrecio3() { return precio3; }
    public void setPrecio3(double precio3) { this.precio3 = precio3; }

    public String getCodLinea() { return codLinea; }
    public void setCodLinea(String codLinea) { this.codLinea = codLinea; }

    public String getLinea() { return linea; }
    public void setLinea(String linea) { this.linea = linea; }

    public String getCodSub() { return codSub; }
    public void setCodSub(String codSub) { this.codSub = codSub; }

    public String getSubLinea() { return subLinea; }
    public void setSubLinea(String subLinea) { this.subLinea = subLinea; }

    public String getCodProveedor() { return codProveedor; }
    public void setCodProveedor(String codProveedor) { this.codProveedor = codProveedor; }

    public String getNombreProveedor() { return nombreProveedor; }
    public void setNombreProveedor(String nombreProveedor) { this.nombreProveedor = nombreProveedor; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getProcedencia() { return procedencia; }
    public void setProcedencia(String procedencia) { this.procedencia = procedencia; }

    public double getPeso() { return peso; }
    public void setPeso(double peso) { this.peso = peso; }

    public double getVolumen() { return volumen; }
    public void setVolumen(double volumen) { this.volumen = volumen; }
}
