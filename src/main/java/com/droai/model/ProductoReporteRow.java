package com.droai.model;

/**
 * Modelo de datos para las filas del visor de reportes de productos.
 */
public class ProductoReporteRow {

    private String codigo;
    private String codigoBarra;
    private String descripcion;
    private String marca;
    private String linea;
    private String principioActivo;
    private String categoria;
    private String proveedor;
    private Double existencia;
    private Double impuesto;

    public ProductoReporteRow() {}

    public ProductoReporteRow(String codigo, String codigoBarra, String descripcion, String marca, String linea,
                              String principioActivo, String categoria, String proveedor, Double existencia, Double impuesto) {
        this.codigo = codigo;
        this.codigoBarra = codigoBarra;
        this.descripcion = descripcion;
        this.marca = marca;
        this.linea = linea;
        this.principioActivo = principioActivo;
        this.categoria = categoria;
        this.proveedor = proveedor;
        this.existencia = existencia;
        this.impuesto = impuesto;
    }

    // --- Getters y Setters ---

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigoBarra() {
        return codigoBarra;
    }

    public void setCodigoBarra(String codigoBarra) {
        this.codigoBarra = codigoBarra;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getLinea() {
        return linea;
    }

    public void setLinea(String linea) {
        this.linea = linea;
    }

    public String getPrincipioActivo() {
        return principioActivo;
    }

    public void setPrincipioActivo(String principioActivo) {
        this.principioActivo = principioActivo;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public Double getExistencia() {
        return existencia;
    }

    public void setExistencia(Double existencia) {
        this.existencia = existencia;
    }

    public Double getImpuesto() {
        return impuesto;
    }

    public void setImpuesto(Double impuesto) {
        this.impuesto = impuesto;
    }
}
