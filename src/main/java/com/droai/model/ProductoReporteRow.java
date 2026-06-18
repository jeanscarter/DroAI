package com.droai.model;

/**
 * Modelo ligero para el Reporte de Productos.
 * Contiene únicamente los campos requeridos para la vista resumen:
 * Código, Descripción, Línea, Principio Activo (Sublínea), Categoría, Proveedor y Existencia.
 */
public class ProductoReporteRow {

    private String codigo;
    private String descripcion;
    private String codLinea;
    private String linea;
    private String codSubLinea;
    private String principioActivo;   // subl_des (sublínea)
    private String codCategoria;
    private String categoria;         // cat_des (saCatArticulo)
    private String codProveedor;
    private String proveedor;         // prov_des (saProveedor)
    private double existencia;

    public ProductoReporteRow() {}

    // --- Getters y Setters ---

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getCodLinea() { return codLinea; }
    public void setCodLinea(String codLinea) { this.codLinea = codLinea; }

    public String getLinea() { return linea; }
    public void setLinea(String linea) { this.linea = linea; }

    public String getCodSubLinea() { return codSubLinea; }
    public void setCodSubLinea(String codSubLinea) { this.codSubLinea = codSubLinea; }

    public String getPrincipioActivo() { return principioActivo; }
    public void setPrincipioActivo(String principioActivo) { this.principioActivo = principioActivo; }

    public String getCodCategoria() { return codCategoria; }
    public void setCodCategoria(String codCategoria) { this.codCategoria = codCategoria; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getCodProveedor() { return codProveedor; }
    public void setCodProveedor(String codProveedor) { this.codProveedor = codProveedor; }

    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }

    public double getExistencia() { return existencia; }
    public void setExistencia(double existencia) { this.existencia = existencia; }
}
